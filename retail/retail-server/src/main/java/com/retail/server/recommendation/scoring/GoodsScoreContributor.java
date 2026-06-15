package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsSearchDocument;

public interface GoodsScoreContributor {
    double score(GoodsSearchDocument document, RecommendationScoreContext context);
}
