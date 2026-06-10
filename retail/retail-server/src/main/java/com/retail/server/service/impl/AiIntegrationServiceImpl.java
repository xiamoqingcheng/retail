package com.retail.server.service.impl;

import com.retail.server.dto.AppletScanGoodsDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.server.entity.Goods;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.AiIntegrationService;
import com.retail.server.service.GoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 聚合业务实现。
 */
@Slf4j
@Service
public class AiIntegrationServiceImpl implements AiIntegrationService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final RestTemplate restTemplate;
    private final GoodsService goodsService;

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public AiIntegrationServiceImpl(RestTemplate restTemplate, GoodsService goodsService) {
        this.restTemplate = restTemplate;
        this.goodsService = goodsService;
    }

    @Override
    public List<AppletScanGoodsDTO> getTopKGoodsFromImage(String imageBase64, int k) {
        if (!StringUtils.hasText(imageBase64)) {
            throw new BusinessException(400, "图片不能为空");
        }
        if (k < 1) {
            throw new BusinessException(400, "k 必须大于 0");
        }

        List<PythonCenterItem> aiItems = callPythonCenterApi(imageBase64, k);
        if (aiItems.isEmpty()) {
            return List.of();
        }

        Set<Long> seen = new HashSet<>();
        SequencedCollection<Long> orderedGoodsIds = new ArrayList<>();
        Map<String, Long> aiIdToGoodsId = new HashMap<>();

        for (PythonCenterItem item : aiItems) {
            if (item == null || !StringUtils.hasText(item.goodsId())) {
                continue;
            }
            Long parsedGoodsId = parseGoodsId(item.goodsId());
            aiIdToGoodsId.put(item.goodsId(), parsedGoodsId);
            if (parsedGoodsId != null && seen.add(parsedGoodsId)) {
                orderedGoodsIds.add(parsedGoodsId);
            }
        }

        Map<Long, Goods> goodsMap = new LinkedHashMap<>();
        if (!orderedGoodsIds.isEmpty()) {
            List<Goods> goodsList = goodsService.listByIds(new ArrayList<>(orderedGoodsIds));
            for (Goods goods : goodsList) {
                if (goods != null && goods.getId() != null) {
                    goodsMap.put(goods.getId(), goods);
                }
            }
        }

        SequencedCollection<AppletScanGoodsDTO> result = new ArrayList<>();
        Set<Long> addedToResult = new HashSet<>();
        for (PythonCenterItem item : aiItems) {
            if (item == null || !StringUtils.hasText(item.goodsId())) {
                continue;
            }

            Long goodsId = aiIdToGoodsId.get(item.goodsId());
            if (goodsId != null && !addedToResult.add(goodsId)) {
                continue;
            }

            Goods goods = goodsId == null ? null : goodsMap.get(goodsId);

            result.add(new AppletScanGoodsDTO(
                    item.goodsId(),
                    goodsId,
                    goods == null ? null : goods.getName(),
                    goods == null ? null : goods.getPrice(),
                    goods == null ? null : goods.getStock(),
                    goods == null ? null : goods.getShelfId(),
                    goods == null ? null : goods.getImageUrl(),
                    item.boxCenter() == null ? List.of() : item.boxCenter(),
                    item.distance(),
                    item.box() == null ? List.of() : item.box()
            ));
        }

        return List.copyOf(result);
    }

    private List<PythonCenterItem> callPythonCenterApi(String imageBase64, int k) {
        String url = aiServiceUrl + "/api/ai/recognize/center";

        Map<String, Object> payload = new HashMap<>();
        payload.put("image_base64", imageBase64);
        payload.put("k", k);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        int maxRetries = 2;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<List<PythonCenterItem>> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<>() {
                        }
                );

                List<PythonCenterItem> body = response.getBody();
                return body == null ? List.of() : body;
            } catch (RestClientException ex) {
                if (attempt < maxRetries) {
                    log.warn("调用 Python AI 服务失败 (第{}次重试): {} - {}", attempt + 1, url, ex.getMessage());
                    try {
                        Thread.sleep(1000L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("调用 Python AI 服务失败，已达最大重试次数: {}", url, ex);
                    throw new BusinessException(502, "AI 识别服务繁忙，请稍后重试");
                }
            }
        }
        throw new BusinessException(502, "AI 识别服务不可用");
    }

    private Long parseGoodsId(String aiGoodsId) {
        if (!StringUtils.hasText(aiGoodsId)) {
            return null;
        }

        try {
            return Long.parseLong(aiGoodsId);
        } catch (NumberFormatException ignore) {
            log.debug("AI 返回 goods_id 非纯数字: {}", aiGoodsId);
        }

        Matcher matcher = NUMBER_PATTERN.matcher(aiGoodsId);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record PythonCenterItem(
            @JsonProperty("goods_id") String goodsId,
            @JsonProperty("box_center") List<Double> boxCenter,
            @JsonProperty("distance") Double distance,
            @JsonProperty("box") List<Double> box
    ) {
    }
}