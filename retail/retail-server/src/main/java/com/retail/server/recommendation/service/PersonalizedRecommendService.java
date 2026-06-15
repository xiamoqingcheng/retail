package com.retail.server.recommendation.service;

import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.UserBehaviorEventMapper;
import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.model.KeywordAggregate;
import com.retail.server.recommendation.scoring.GoodsScoreContributor;
import com.retail.server.recommendation.scoring.RecommendationScoreContext;
import com.retail.server.recommendation.semantic.GoodsSemanticVectorizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PersonalizedRecommendService {

    private static final int BEHAVIOR_DAYS = 90;
    private static final int GLOBAL_DAYS = 30;
    private static final int KEYWORD_LIMIT = 20;
    private static final int MAX_POOL = 80;

    private final GoodsMapper goodsMapper;
    private final UserBehaviorEventMapper userBehaviorEventMapper;
    private final GoodsSemanticVectorizer vectorizer;
    private final List<GoodsScoreContributor> scoreContributors;
    private final RandomTopSelector randomTopSelector;

    public PersonalizedRecommendService(
            GoodsMapper goodsMapper,
            UserBehaviorEventMapper userBehaviorEventMapper,
            GoodsSemanticVectorizer vectorizer,
            List<GoodsScoreContributor> scoreContributors,
            RandomTopSelector randomTopSelector) {
        this.goodsMapper = goodsMapper;
        this.userBehaviorEventMapper = userBehaviorEventMapper;
        this.vectorizer = vectorizer;
        this.scoreContributors = scoreContributors;
        this.randomTopSelector = randomTopSelector;
    }

    public List<Long> recommendGoodsIds(Long userId, int count) {
        if (count <= 0) {
            return List.of();
        }

        List<GoodsSearchDocument> documents = safeActiveSearchDocuments().stream()
                .filter(document -> document.getId() != null)
                .filter(document -> document.getStock() == null || document.getStock() > 0)
                .toList();
        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        Map<Long, GoodsBehaviorAggregate> userStats = new LinkedHashMap<>();
        List<KeywordAggregate> keywords = List.of();
        if (userId != null && userId > 0) {
            mergeStats(userStats, safeUserGoodsAggregates(userId));
            mergeStats(userStats, safeUserOrderGoodsAggregates(userId));
            keywords = safeUserKeywordAggregates(userId);
        }
        Map<Long, Double> globalPurchaseScore = globalPurchaseScore();

        RecommendationScoreContext context = new RecommendationScoreContext(
                userId,
                LocalDateTime.now(),
                userStats,
                globalPurchaseScore,
                keywords,
                vectorizer);

        List<ScoredDocument> ranked = new ArrayList<>();
        for (GoodsSearchDocument document : documents) {
            double score = 0.0;
            for (GoodsScoreContributor contributor : scoreContributors) {
                score += contributor.score(document, context);
            }
            ranked.add(new ScoredDocument(document.getId(), score));
        }

        ranked.sort(Comparator.comparingDouble(ScoredDocument::score).reversed()
                .thenComparing(ScoredDocument::goodsId));

        boolean hasPersonalScore = ranked.stream().anyMatch(item -> item.score() > 0.0);
        List<Long> rankedIds = hasPersonalScore
                ? ranked.stream().map(ScoredDocument::goodsId).toList()
                : fallbackRankedIds(documents);

        int topM = Math.min(MAX_POOL, Math.max(count * 3, 30));
        return randomTopSelector.select(rankedIds, topM, count);
    }

    private Map<Long, Double> globalPurchaseScore() {
        List<GoodsSearchDocument> hotGoods = safeRecentPurchasedGoodsDocuments(GLOBAL_DAYS, MAX_POOL);
        Map<Long, Double> score = new HashMap<>();
        double value = hotGoods.size();
        for (GoodsSearchDocument document : hotGoods) {
            if (document.getId() != null) {
                score.put(document.getId(), value);
                value = Math.max(0.0, value - 1.0);
            }
        }
        return score;
    }

    private List<GoodsSearchDocument> safeActiveSearchDocuments() {
        try {
            return goodsMapper.selectActiveSearchDocuments(null);
        } catch (DataAccessException ex) {
            log.warn("Load active goods for recommendation failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<GoodsBehaviorAggregate> safeUserGoodsAggregates(Long userId) {
        try {
            return userBehaviorEventMapper.selectUserGoodsAggregates(userId, BEHAVIOR_DAYS);
        } catch (DataAccessException ex) {
            log.warn("Load user behavior aggregates failed: userId={}, error={}", userId, ex.getMessage());
            return List.of();
        }
    }

    private List<GoodsBehaviorAggregate> safeUserOrderGoodsAggregates(Long userId) {
        try {
            return userBehaviorEventMapper.selectUserOrderGoodsAggregates(userId, BEHAVIOR_DAYS);
        } catch (DataAccessException ex) {
            log.warn("Load user order aggregates failed: userId={}, error={}", userId, ex.getMessage());
            return List.of();
        }
    }

    private List<KeywordAggregate> safeUserKeywordAggregates(Long userId) {
        try {
            return userBehaviorEventMapper.selectUserKeywordAggregates(userId, BEHAVIOR_DAYS, KEYWORD_LIMIT);
        } catch (DataAccessException ex) {
            log.warn("Load user keyword aggregates failed: userId={}, error={}", userId, ex.getMessage());
            return List.of();
        }
    }

    private List<GoodsSearchDocument> safeRecentPurchasedGoodsDocuments(int days, int limit) {
        try {
            return userBehaviorEventMapper.selectRecentPurchasedGoodsDocuments(days, limit);
        } catch (DataAccessException ex) {
            log.warn("Load global purchase recommendation failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private void mergeStats(Map<Long, GoodsBehaviorAggregate> target, List<GoodsBehaviorAggregate> incoming) {
        if (CollectionUtils.isEmpty(incoming)) {
            return;
        }
        for (GoodsBehaviorAggregate item : incoming) {
            if (item == null || item.getGoodsId() == null) {
                continue;
            }
            GoodsBehaviorAggregate exists = target.get(item.getGoodsId());
            if (exists == null) {
                target.put(item.getGoodsId(), item);
                continue;
            }
            exists.setViewCount(exists.viewCountValue() + item.viewCountValue());
            exists.setPurchaseQty(exists.purchaseQtyValue() + item.purchaseQtyValue());
            exists.setCancelQty(exists.cancelQtyValue() + item.cancelQtyValue());
            if (exists.getLatestEventTime() == null ||
                    (item.getLatestEventTime() != null && item.getLatestEventTime().isAfter(exists.getLatestEventTime()))) {
                exists.setLatestEventTime(item.getLatestEventTime());
            }
        }
    }

    private List<Long> fallbackRankedIds(List<GoodsSearchDocument> documents) {
        return documents.stream()
                .sorted(Comparator.comparing((GoodsSearchDocument document) -> document.getStock() == null ? 0 : document.getStock()).reversed()
                        .thenComparing(GoodsSearchDocument::getId))
                .map(GoodsSearchDocument::getId)
                .toList();
    }

    private record ScoredDocument(Long goodsId, double score) {
    }
}
