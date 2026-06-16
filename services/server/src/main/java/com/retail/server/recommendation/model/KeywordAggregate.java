package com.retail.server.recommendation.model;

import lombok.Data;

@Data
public class KeywordAggregate {
    private String keyword;
    private Double countValue;

    public double count() {
        return countValue == null ? 0.0 : countValue;
    }
}
