package com.retail.server.recommendation.service;

import com.retail.server.mapper.GoodsMapper;
import com.retail.server.recommendation.model.GoodsScoreCandidate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.semantic.SemanticGoodsSearchEngine;
import com.retail.server.recommendation.semantic.SemanticVectorClient;
import com.retail.server.recommendation.semantic.SemanticVectorClient.IndexDoc;
import com.retail.server.recommendation.semantic.SemanticVectorClient.ScoredId;
import com.retail.server.vo.AppletSearchGoodsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 商品检索服务（混合检索 / Hybrid Retrieval）：
 * <ul>
 *   <li>关键词·字面：现有 {@link SemanticGoodsSearchEngine}（字符向量余弦 + 子串匹配）</li>
 *   <li>向量·语义：{@link SemanticVectorClient} 调 Python AI 服务（bge 中文 embedding 模型）</li>
 * </ul>
 * 两路结果用 RRF（Reciprocal Rank Fusion）融合排序。AI 服务不可用时自动退化为纯关键词检索，搜索永不失败。
 */
@Slf4j
@Service
public class SemanticGoodsSearchService {

    private static final int MAX_SEARCH_SIZE = 60;
    /** RRF 融合常数，经验值 60。 */
    private static final int RRF_K = 60;
    /** 语义索引刷新/重试间隔。 */
    private static final long INDEX_TTL_MS = 5 * 60 * 1000L;

    private final GoodsMapper goodsMapper;
    private final SemanticGoodsSearchEngine searchEngine;
    private final SemanticVectorClient vectorClient;

    /** 上次尝试构建语义索引的时间（成功或失败都更新，避免 AI 宕机时每次搜索都重试）。 */
    private final AtomicLong lastIndexAttemptAt = new AtomicLong(0L);

    public SemanticGoodsSearchService(GoodsMapper goodsMapper,
                                      SemanticGoodsSearchEngine searchEngine,
                                      SemanticVectorClient vectorClient) {
        this.goodsMapper = goodsMapper;
        this.searchEngine = searchEngine;
        this.vectorClient = vectorClient;
    }

    /** 启动后台预热语义索引（不阻塞应用启动；AI 首次会加载模型，故重试几次）。 */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpSemanticIndex() {
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    if (doIndex() >= 0) {
                        return;
                    }
                    Thread.sleep(5000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ex) {
                    log.warn("语义索引预热失败（不影响关键词检索）: {}", ex.getMessage());
                    return;
                }
            }
        }, "semantic-index-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    public List<AppletSearchGoodsVO> search(String query, Long categoryId, Integer size) {
        int limit = Math.min(Math.max(size == null ? 20 : size, 1), MAX_SEARCH_SIZE);

        List<GoodsSearchDocument> documents;
        try {
            documents = goodsMapper.selectActiveSearchDocuments(categoryId);
        } catch (DataAccessException ex) {
            log.warn("Load goods search documents failed: categoryId={}, error={}", categoryId, ex.getMessage());
            return List.of();
        }
        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        Map<Long, GoodsSearchDocument> documentsById = documents.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(GoodsSearchDocument::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));

        // A) 关键词·字面候选（现有引擎；documents 已按分类过滤且排除已删除商品）
        Map<Long, Double> lexicalScore = new LinkedHashMap<>();
        List<Long> lexicalRanked = new ArrayList<>();
        for (GoodsScoreCandidate candidate : searchEngine.search(
                query, documents, categoryId, MAX_SEARCH_SIZE, SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)) {
            Long id = candidate.goodsId();
            if (id != null && lexicalScore.putIfAbsent(id, candidate.score()) == null) {
                lexicalRanked.add(id);
            }
        }

        // B) 向量·语义候选（AI 服务；仅保留落在当前 documents 集合内的，天然完成分类/已删过滤）
        ensureSemanticIndex();
        Map<Long, Double> vectorScore = new LinkedHashMap<>();
        List<Long> vectorRanked = new ArrayList<>();
        for (ScoredId scored : vectorClient.search(query, MAX_SEARCH_SIZE)) {
            Long id = scored.id();
            if (documentsById.containsKey(id) && vectorScore.putIfAbsent(id, scored.score()) == null) {
                vectorRanked.add(id);
            }
        }

        // RRF 融合两路排名
        Map<Long, Double> fused = new HashMap<>();
        accumulateRrf(fused, lexicalRanked);
        accumulateRrf(fused, vectorRanked);
        if (fused.isEmpty()) {
            return List.of();
        }

        return fused.entrySet().stream()
                .sorted((left, right) -> {
                    int byScore = Double.compare(right.getValue(), left.getValue());
                    return byScore != 0 ? byScore : Long.compare(left.getKey(), right.getKey());
                })
                .limit(limit)
                .map(entry -> {
                    long id = entry.getKey();
                    // 展示分数取两路相似度较大者（0~1）；最终排序仍由 RRF 决定
                    double display = Math.max(
                            lexicalScore.getOrDefault(id, 0.0),
                            vectorScore.getOrDefault(id, 0.0));
                    return toVo(documentsById.get(id), display);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private void accumulateRrf(Map<Long, Double> target, List<Long> rankedIds) {
        for (int rank = 0; rank < rankedIds.size(); rank++) {
            target.merge(rankedIds.get(rank), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
    }

    /** 若语义索引过期/从未构建，则重建（TTL 节流，避免频繁重建）。 */
    private void ensureSemanticIndex() {
        if (System.currentTimeMillis() - lastIndexAttemptAt.get() < INDEX_TTL_MS) {
            return;
        }
        doIndex();
    }

    /** 用全部活跃商品重建 AI 语义索引；返回入库条数（失败 -1）。线程安全，失败不影响关键词检索。 */
    private synchronized int doIndex() {
        lastIndexAttemptAt.set(System.currentTimeMillis());

        List<GoodsSearchDocument> all;
        try {
            all = goodsMapper.selectActiveSearchDocuments(null);
        } catch (DataAccessException ex) {
            log.warn("加载商品用于语义索引失败: {}", ex.getMessage());
            return -1;
        }

        List<IndexDoc> docs = all.stream()
                .filter(item -> item.getId() != null)
                .map(item -> new IndexDoc(item.getId(), buildIndexText(item)))
                .filter(doc -> StringUtils.hasText(doc.text()))
                .toList();

        int indexed = vectorClient.indexGoods(docs);
        if (indexed >= 0) {
            log.info("语义索引已构建/刷新: {} 条商品", indexed);
        }
        return indexed;
    }

    private String buildIndexText(GoodsSearchDocument document) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(document.getName())) {
            sb.append(document.getName());
        }
        if (StringUtils.hasText(document.getCategoryName())) {
            sb.append(' ').append(document.getCategoryName());
        }
        appendSemanticHints(sb, document);
        return sb.toString().trim();
    }

    private void appendSemanticHints(StringBuilder sb, GoodsSearchDocument document) {
        String text = ((document.getName() == null ? "" : document.getName())
                + " " + (document.getCategoryName() == null ? "" : document.getCategoryName()))
                .toLowerCase(Locale.ROOT);

        if (text.contains("方便面") || text.contains("牛肉面") || text.contains("合味道")
                || text.contains("康师傅") || text.contains("五谷道场")) {
            sb.append(" 泡面 杯面 速食 夜宵");
        }
        if (text.contains("口香糖") || text.contains("薄荷糖") || text.contains("炫迈")
                || text.contains("绿箭") || text.contains("益达")) {
            sb.append(" 清新口气 嚼的糖 清口糖");
        }
        if (text.contains("午餐肉") || text.contains("肉罐头")) {
            sb.append(" 火腿肠 香肠 肉肠 即食肉 熟食肉");
        }
    }

    private AppletSearchGoodsVO toVo(GoodsSearchDocument document, double score) {
        if (document == null) {
            return null;
        }
        return new AppletSearchGoodsVO(
                document.getId(),
                document.getName(),
                document.getPrice(),
                document.getStock(),
                document.getImageUrl(),
                document.getShelfId(),
                document.getCategoryId(),
                document.getCategoryName(),
                Math.round(score * 10000.0) / 10000.0
        );
    }
}
