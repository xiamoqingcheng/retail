package com.retail.server.recommendation.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsSearchDocument {
    private Long id;
    private String name;
    private String barcode;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer stock;
    private String shelfId;
    private String imageUrl;
}
