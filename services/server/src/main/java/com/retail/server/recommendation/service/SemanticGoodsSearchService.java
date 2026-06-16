package com.retail.server.recommendation.service;

import com.retail.server.mapper.GoodsMapper;
import com.retail.server.recommendation.model.GoodsScoreCandidate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.semantic.SemanticGoodsSearchEngine;
import com.retail.server.vo.AppletSearchGoodsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SemanticGoodsSearchService {

    private static final int MAX_SEARCH_SIZE = 60;

    private final GoodsMapper goodsMapper;
    private final SemanticGoodsSearchEngine searchEngine;

    public SemanticGoodsSearchService(GoodsMapper goodsMapper, SemanticGoodsSearchEngine searchEngine) {
        this.goodsMapper = goodsMapper;
        this.searchEngine = searchEngine;
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
        List<GoodsScoreCandidate> candidates = searchEngine.search(
                query,
                documents,
                categoryId,
                limit,
                SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE);
        if (CollectionUtils.isEmpty(candidates)) {
            return List.of();
        }

        Map<Long, GoodsSearchDocument> documentsById = documents.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(GoodsSearchDocument::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        return candidates.stream()
                .map(candidate -> toVo(documentsById.get(candidate.goodsId()), candidate.score()))
                .filter(Objects::nonNull)
                .toList();
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
