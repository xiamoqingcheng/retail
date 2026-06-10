package com.retail.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购物车操作请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {

    /**
     * 商品 ID。
     */
    private Long goodsId;

    /**
     * 购买数量。
     */
    private Integer quantity;
}
