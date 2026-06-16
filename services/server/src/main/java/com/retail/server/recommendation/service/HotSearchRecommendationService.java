package com.retail.server.recommendation.service;

import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.UserBehaviorEventMapper;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.semantic.SearchKeywordExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HotSearchRecommendationService {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_TOP_M = 30;
    private static final int MAX_TOP_M = 80;
    private static final int MAX_TOP_K = 20;

    private final UserBehaviorEventMapper userBehaviorEventMapper;
    private final GoodsMapper goodsMapper;
    private final SearchKeywordExtractor keywordExtractor;
    private final RandomTopSelector randomTopSelector;

    public HotSearchRecommendationService(
            UserBehaviorEventMapper userBehaviorEventMapper,
            GoodsMapper goodsMapper,
            SearchKeywordExtractor keywordExtractor,
            RandomTopSelector randomTopSelector) {
        this.userBehaviorEventMapper = userBehaviorEventMapper;
        this.goodsMapper = goodsMapper;
        this.keywordExtractor = keywordExtractor;
        this.randomTopSelector = randomTopSelector;
    }

    public List<String> hotKeywords(Integer k, Integer m) {
        int topK = Math.min(Math.max(k == null ? 8 : k, 1), MAX_TOP_K);
        int topM = Math.min(Math.max(m == null ? DEFAULT_TOP_M : m, topK), MAX_TOP_M);

        List<GoodsSearchDocument> hotGoods = safeRecentPurchasedGoodsDocuments(topM);
        if (CollectionUtils.isEmpty(hotGoods)) {
            hotGoods = safeActiveSearchDocuments();
        }

        List<String> rankedKeywords = aggregateKeywords(hotGoods);
        return randomTopSelector.select(rankedKeywords, topM, topK);
    }

    private List<GoodsSearchDocument> safeRecentPurchasedGoodsDocuments(int limit) {
        try {
            return userBehaviorEventMapper.selectRecentPurchasedGoodsDocuments(DEFAULT_DAYS, limit);
        } catch (DataAccessException ex) {
            log.warn("Load hot search purchase candidates failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<GoodsSearchDocument> safeActiveSearchDocuments() {
        try {
            return goodsMapper.selectActiveSearchDocuments(null);
        } catch (DataAccessException ex) {
            log.warn("Load hot search fallback goods failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<String> aggregateKeywords(List<GoodsSearchDocument> documents) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        int descendingWeight = documents.size();
        for (GoodsSearchDocument document : documents) {
            String keyword = keywordExtractor.fromGoodsName(document.getName(), document.getCategoryName());
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            weights.merge(keyword, Math.max(1, descendingWeight), Integer::sum);
            descendingWeight--;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(weights.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey));
        return entries.stream().map(Map.Entry::getKey).toList();
    }
}
