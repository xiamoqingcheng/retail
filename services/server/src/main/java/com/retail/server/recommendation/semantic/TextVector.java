package com.retail.server.recommendation.semantic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TextVector {
    private final Map<String, Double> weights = new HashMap<>();

    public void add(String feature, double weight) {
        if (feature == null || feature.isBlank() || weight == 0.0) {
            return;
        }
        weights.merge(feature, weight, Double::sum);
    }

    public Map<String, Double> weights() {
        return Collections.unmodifiableMap(weights);
    }

    public double cosine(TextVector other) {
        if (other == null || weights.isEmpty() || other.weights.isEmpty()) {
            return 0.0;
        }

        double dot = 0.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            dot += entry.getValue() * other.weights.getOrDefault(entry.getKey(), 0.0);
        }

        double left = norm(weights);
        double right = norm(other.weights);
        if (left == 0.0 || right == 0.0) {
            return 0.0;
        }
        return dot / (left * right);
    }

    private double norm(Map<String, Double> value) {
        double sum = 0.0;
        for (double weight : value.values()) {
            sum += weight * weight;
        }
        return Math.sqrt(sum);
    }
}
