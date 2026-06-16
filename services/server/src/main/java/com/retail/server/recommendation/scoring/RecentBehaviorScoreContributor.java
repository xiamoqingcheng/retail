package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RecentBehaviorScoreContributor implements GoodsScoreContributor {
    @Override
    public double score(GoodsSearchDocument document, RecommendationScoreContext context) {
        GoodsBehaviorAggregate stats = context.stats(document.getId());
        if (stats == null || stats.getLatestEventTime() == null) {
            return 0.0;
        }

        long hours = Math.max(0, Duration.between(stats.getLatestEventTime(), context.now()).toHours());
        if (hours >= 24 * 30) {
            return 0.0;
        }
        return 1.5 * (1.0 - (hours / (24.0 * 30.0)));
    }
}
