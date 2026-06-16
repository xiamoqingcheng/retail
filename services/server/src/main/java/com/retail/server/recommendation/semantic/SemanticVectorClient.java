package com.retail.server.recommendation.semantic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 语义向量检索客户端 —— 调用 Python AI 服务的 /api/ai/semantic/* 接口。
 * 任何调用失败都【不抛异常】，返回空/-1，使上层混合检索可优雅退化为纯关键词检索。
 */
@Slf4j
@Component
public class SemanticVectorClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public SemanticVectorClient(@Qualifier("aiRecognitionRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 送入索引的商品文档：id + 用于向量化的文本（名称 + 分类等）。 */
    public record IndexDoc(long id, String text) {
    }

    /** 检索结果：商品 id + 余弦分数。 */
    public record ScoredId(
            @JsonProperty("id") long id,
            @JsonProperty("score") double score) {
    }

    /**
     * 把活跃商品推送到 AI 重建语义索引。
     *
     * @return 入库条数；调用失败返回 -1。
     */
    public int indexGoods(List<IndexDoc> docs) {
        String url = aiServiceUrl + "/api/ai/semantic/index";
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, jsonEntity(Map.of("docs", docs)),
                    new ParameterizedTypeReference<>() {
                    });
            Object indexed = resp.getBody() == null ? null : resp.getBody().get("indexed");
            return indexed instanceof Number num ? num.intValue() : 0;
        } catch (RestClientException ex) {
            log.warn("语义索引构建失败（检索将退化为纯关键词）: {} - {}", url, ex.getMessage());
            return -1;
        }
    }

    /**
     * 向量检索 Top-K。
     *
     * @return (goodsId, 余弦分数) 列表；AI 不可用时返回空列表。
     */
    public List<ScoredId> search(String query, int topk) {
        String url = aiServiceUrl + "/api/ai/semantic/search";
        try {
            ResponseEntity<List<ScoredId>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, jsonEntity(Map.of("query", query, "topk", topk)),
                    new ParameterizedTypeReference<>() {
                    });
            return resp.getBody() == null ? List.of() : resp.getBody();
        } catch (RestClientException ex) {
            log.warn("语义检索失败（退化为纯关键词）: {} - {}", url, ex.getMessage());
            return List.of();
        }
    }

    private HttpEntity<Object> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
