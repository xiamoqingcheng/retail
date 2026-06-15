package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.KeywordAggregate;
import com.retail.server.recommendation.semantic.GoodsSemanticVectorizer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RecommendationScoreContext(
        Long userId,
        LocalDateTime now,
        Map<Long, GoodsBehaviorAggregate> userGoodsStats,
        Map<Long, Double> globalPurchaseScore,
        List<KeywordAggregate> userKeywords,
        GoodsSemanticVectorizer vectorizer
) {
    public GoodsBehaviorAggregate stats(Long goodsId) {
        return userGoodsStats.get(goodsId);
    }

    public double globalPurchase(Long goodsId) {
        return globalPurchaseScore.getOrDefault(goodsId, 0.0);
    }
}
