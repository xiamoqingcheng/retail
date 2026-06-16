package com.retail.server.recommendation.scoring;

import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.model.KeywordAggregate;
import com.retail.server.recommendation.semantic.TextVector;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SearchAffinityScoreContributor implements GoodsScoreContributor {
    @Override
    public double score(GoodsSearchDocument document, RecommendationScoreContext context) {
        if (CollectionUtils.isEmpty(context.userKeywords())) {
            return 0.0;
        }

        TextVector goodsVector = context.vectorizer().vectorizeGoods(document);
        double score = 0.0;
        for (KeywordAggregate keyword : context.userKeywords()) {
            if (keyword == null || keyword.getKeyword() == null || keyword.getKeyword().isBlank()) {
                continue;
            }
            double similarity = context.vectorizer().vectorizeQuery(keyword.getKeyword()).cosine(goodsVector);
            if (similarity < 0.18) {
                continue;
            }
            score += similarity * Math.log1p(keyword.count()) * 2.0;
        }
        return score;
    }
}
