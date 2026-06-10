package com.retail.server.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车商品展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartVO {

    /**
     * 商品 ID。
     */
    private Long goodsId;

    /**
     * 商品名称。
     */
    private String goodsName;

    /**
     * 商品价格。
     */
    private BigDecimal price;

    /**
     * 购买数量。
     */
    private Integer quantity;

    /**
     * 商品图片 URL。
     */
    private String imageUrl;
}
