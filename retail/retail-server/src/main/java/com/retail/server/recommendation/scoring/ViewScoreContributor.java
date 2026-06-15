package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.springframework.stereotype.Component;

@Component
public class ViewScoreContributor implements GoodsScoreContributor {
    @Override
    public double score(GoodsSearchDocument document, RecommendationScoreContext context) {
        GoodsBehaviorAggregate stats = context.stats(document.getId());
        double views = stats == null ? 0.0 : stats.viewCountValue();
        return Math.log1p(views) * 2.2;
    }
}
