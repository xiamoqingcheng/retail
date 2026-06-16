package com.retail.server.recommendation.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GoodsBehaviorAggregate {
    private Long goodsId;
    private Double viewCount;
    private Double purchaseQty;
    private Double cancelQty;
    private LocalDateTime latestEventTime;

    public double viewCountValue() {
        return viewCount == null ? 0.0 : viewCount;
    }

    public double purchaseQtyValue() {
        return purchaseQty == null ? 0.0 : purchaseQty;
    }

    public double cancelQtyValue() {
        return cancelQty == null ? 0.0 : cancelQty;
    }
}
