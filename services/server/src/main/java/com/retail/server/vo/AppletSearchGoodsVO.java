package com.retail.server.vo;

import java.math.BigDecimal;

public record AppletSearchGoodsVO(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String shelfId,
        Long categoryId,
        String categoryName,
        Double relevance
) {
}
