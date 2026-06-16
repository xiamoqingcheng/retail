package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.springframework.stereotype.Component;

@Component
public class CancelPenaltyContributor implements GoodsScoreContributor {
    @Override
    public double score(GoodsSearchDocument document, RecommendationScoreContext context) {
        GoodsBehaviorAggregate stats = context.stats(document.getId());
        double cancelQty = stats == null ? 0.0 : stats.cancelQtyValue();
        return -Math.log1p(cancelQty) * 3.0;
    }
}
