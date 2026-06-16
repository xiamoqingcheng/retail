package com.retail.server.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 小程序扫码返回项：商品详情 + AI 坐标信息。
 */
public record AppletScanGoodsDTO(
        String aiGoodsId,
        Long goodsId,
        String goodsName,
        BigDecimal price,
        Integer stock,
        String shelfId,
        String imageUrl,
        List<Double> boxCenter,
        Double distance,
        List<Double> box
) {
}