package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.springframework.stereotype.Component;

@Component
public class PurchaseScoreContributor implements GoodsScoreContributor {
    @Override
    public double score(GoodsSearchDocument document, RecommendationScoreContext context) {
        GoodsBehaviorAggregate stats = context.stats(document.getId());
        double userPurchase = stats == null ? 0.0 : stats.purchaseQtyValue();
        return Math.log1p(userPurchase) * 4.0 + Math.log1p(context.globalPurchase(document.getId())) * 1.6;
    }
}
