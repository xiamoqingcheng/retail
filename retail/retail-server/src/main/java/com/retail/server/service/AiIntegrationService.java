package com.retail.server.service;

import com.retail.server.dto.AppletScanGoodsDTO;

import java.util.List;

/**
 * AI 微服务聚合业务。
 */
public interface AiIntegrationService {

    /**
     * 调用 Python AI 服务并返回 Top-K 商品聚合结果。
     */
    List<AppletScanGoodsDTO> getTopKGoodsFromImage(String imageBase64, int k);
}