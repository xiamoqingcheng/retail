package com.retail.server.recommendation.semantic;

import com.retail.server.recommendation.model.GoodsScoreCandidate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Component
public class SemanticGoodsSearchEngine {

    public static final double DEFAULT_MIN_SCORE = 0.24;
    private static final double EXACT_NAME_MATCH_SCORE = 0.45;
    private static final double PARTIAL_NAME_MATCH_MAX_SCORE = 0.32;

    private final GoodsSemanticVectorizer vectorizer;

    public SemanticGoodsSearchEngine(GoodsSemanticVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    public List<GoodsScoreCandidate> search(
            String query,
            List<GoodsSearchDocument> documents,
            Long categoryId,
            int limit,
            double minScore) {
        String normalizedQuery = vectorizer.normalize(query);
        if (!StringUtils.hasText(normalizedQuery) || CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        TextVector queryVector = vectorizer.vectorizeQuery(normalizedQuery);
        int resolvedLimit = Math.max(1, limit);
        double effectiveMinScore = effectiveMinScore(normalizedQuery, minScore);

        return documents.stream()
                .filter(document -> categoryId == null || categoryId <= 0 || categoryId.equals(document.getCategoryId()))
                .map(document -> new GoodsScoreCandidate(document.getId(), score(normalizedQuery, queryVector, document)))
                .filter(candidate -> candidate.score() >= effectiveMinScore)
                .sorted(Comparator.comparingDouble(GoodsScoreCandidate::score).reversed()
                        .thenComparing(GoodsScoreCandidate::goodsId))
                .limit(resolvedLimit)
                .toList();
    }

    private double score(String normalizedQuery, TextVector queryVector, GoodsSearchDocument document) {
        TextVector goodsVector = vectorizer.vectorizeGoods(document);
        double score = queryVector.cosine(goodsVector);

        String name = vectorizer.normalize(document.getName());
        String category = vectorizer.normalize(document.getCategoryName());
        int queryLength = vectorizer.termLength(normalizedQuery);
        if (StringUtils.hasText(name) && name.contains(normalizedQuery)) {
            score += queryLength <= 1 ? 0.04 : EXACT_NAME_MATCH_SCORE;
        } else if (StringUtils.hasText(name) && queryLength >= 2) {
            score += partialNameMatchScore(normalizedQuery, name);
        }
        if (StringUtils.hasText(category) && category.contains(normalizedQuery)) {
            score += queryLength <= 1 ? 0.03 : 0.15;
        }
        return Math.min(score, 1.0);
    }

    private double effectiveMinScore(String normalizedQuery, double requestedMinScore) {
        if (vectorizer.isShortQuery(normalizedQuery)) {
            return Math.min(requestedMinScore, 0.14);
        }
        if (vectorizer.isBroadIntentQuery(normalizedQuery)) {
            return Math.min(requestedMinScore, 0.18);
        }
        return requestedMinScore;
    }

    private double partialNameMatchScore(String normalizedQuery, String normalizedName) {
        int overlap = longestCommonSubstringLength(normalizedQuery, normalizedName);
        if (overlap < 2) {
            return 0.0;
        }
        int queryLength = Math.max(vectorizer.termLength(normalizedQuery), 1);
        double ratio = Math.min(1.0, (double) overlap / queryLength);
        return PARTIAL_NAME_MATCH_MAX_SCORE * ratio;
    }

    private int longestCommonSubstringLength(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        int[] previous = new int[rightLength + 1];
        int best = 0;
        for (int i = 1; i <= leftLength; i++) {
            int[] current = new int[rightLength + 1];
            for (int j = 1; j <= rightLength; j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    current[j] = previous[j - 1] + 1;
                    best = Math.max(best, current[j]);
                }
            }
            previous = current;
        }
        return best;
    }
}
