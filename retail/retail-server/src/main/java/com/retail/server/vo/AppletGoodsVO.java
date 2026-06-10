package com.retail.server.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 小程序商品详情返回对象。
 */
public record AppletGoodsVO(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String shelfId,
        List<String> shelfIds
) {
}
