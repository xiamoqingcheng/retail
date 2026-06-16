package com.retail.server.recommendation;

import com.retail.server.recommendation.model.GoodsScoreCandidate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.semantic.GoodsSemanticVectorizer;
import com.retail.server.recommendation.semantic.SemanticGoodsSearchEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticGoodsSearchEngineTest {

    private final GoodsSemanticVectorizer vectorizer = new GoodsSemanticVectorizer();
    private final SemanticGoodsSearchEngine engine = new SemanticGoodsSearchEngine(vectorizer);

    @Test
    void beverageQueryReturnsBeveragesButNotPuffedSnacks() {
        List<GoodsSearchDocument> documents = List.of(
                document(73L, "可口可乐零度500ml", 7L, "饮料"),
                document(78L, "雪碧500ml", 7L, "饮料"),
                document(1L, "上好佳荷兰豆55g", 1L, "膨化食品"),
                document(5L, "妙脆角魔力炭烧味65g", 1L, "膨化食品")
        );

        List<Long> resultIds = engine.search(
                        "饮料",
                        documents,
                        null,
                        10,
                        SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)
                .stream()
                .map(GoodsScoreCandidate::goodsId)
                .toList();

        assertTrue(resultIds.contains(73L));
        assertTrue(resultIds.contains(78L));
        assertFalse(resultIds.contains(1L));
        assertFalse(resultIds.contains(5L));
    }

    @Test
    void cokeQueryDoesNotLeakUnrelatedStationery() {
        List<GoodsSearchDocument> documents = List.of(
                document(74L, "可口可乐500ml", 7L, "饮料"),
                document(78L, "雪碧500ml", 7L, "饮料"),
                document(190L, "晨光圆珠笔", 17L, "文具")
        );

        List<Long> resultIds = engine.search(
                        "可乐",
                        documents,
                        null,
                        10,
                        SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)
                .stream()
                .map(GoodsScoreCandidate::goodsId)
                .toList();

        assertTrue(resultIds.contains(74L));
        assertFalse(resultIds.contains(190L));
    }

    @Test
    void waterQueryFindsBeveragesButNotHygieneGoods() {
        List<GoodsSearchDocument> documents = List.of(
                document(201L, "农夫山泉矿泉水550ml", 7L, "饮料"),
                document(202L, "可口可乐500ml", 7L, "饮料"),
                document(203L, "飘柔洗发水400ml", 13L, "个人卫生"),
                document(204L, "晨光圆珠笔", 17L, "文具")
        );

        List<Long> resultIds = engine.search(
                        "水",
                        documents,
                        null,
                        10,
                        SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)
                .stream()
                .map(GoodsScoreCandidate::goodsId)
                .toList();

        assertTrue(resultIds.contains(201L));
        assertTrue(resultIds.contains(202L));
        assertFalse(resultIds.contains(203L));
        assertFalse(resultIds.contains(204L));
    }

    @Test
    void snackQueryExpandsToSnackFamiliesButNotBeverages() {
        List<GoodsSearchDocument> documents = List.of(
                document(301L, "乐事原味薯片70g", 1L, "膨化食品"),
                document(302L, "奥利奥夹心饼干97g", 8L, "饼干糕点"),
                document(303L, "开心果120g", 9L, "坚果炒货"),
                document(304L, "可口可乐500ml", 7L, "饮料"),
                document(305L, "晨光圆珠笔", 17L, "文具")
        );

        List<Long> resultIds = engine.search(
                        "零食",
                        documents,
                        null,
                        10,
                        SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)
                .stream()
                .map(GoodsScoreCandidate::goodsId)
                .toList();

        assertTrue(resultIds.contains(301L));
        assertTrue(resultIds.contains(302L));
        assertTrue(resultIds.contains(303L));
        assertFalse(resultIds.contains(304L));
        assertFalse(resultIds.contains(305L));
    }

    @Test
    void brandNameOverlapKeepsGoodsInSearchResult() {
        List<GoodsSearchDocument> documents = List.of(
                document(401L, "优乐美原味奶茶80g", 18L, "冲调食品"),
                document(402L, "香飘飘珍珠奶茶80g", 18L, "冲调食品"),
                document(403L, "晨光优品圆珠笔", 17L, "文具")
        );

        List<Long> resultIds = engine.search(
                        "优乐美",
                        documents,
                        null,
                        10,
                        SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)
                .stream()
                .map(GoodsScoreCandidate::goodsId)
                .toList();

        assertTrue(resultIds.contains(401L));
        assertFalse(resultIds.contains(403L));
    }

    @Test
    void sausageQueryFindsLunchMeatButNotInstantNoodles() {
        List<GoodsSearchDocument> documents = List.of(
                document(501L, "梅林午餐肉340g", 12L, "罐头"),
                document(502L, "五谷道场红烧牛肉面100g", 7L, "方便面"),
                document(503L, "盼盼烧烤牛排味块105g", 1L, "膨化食品")
        );

        List<Long> resultIds = engine.search(
                        "火腿肠",
                        documents,
                        null,
                        10,
                        SemanticGoodsSearchEngine.DEFAULT_MIN_SCORE)
                .stream()
                .map(GoodsScoreCandidate::goodsId)
                .toList();

        assertTrue(resultIds.contains(501L));
        assertFalse(resultIds.contains(502L));
        assertFalse(resultIds.contains(503L));
    }

    private GoodsSearchDocument document(Long id, String name, Long categoryId, String categoryName) {
        GoodsSearchDocument document = new GoodsSearchDocument();
        document.setId(id);
        document.setName(name);
        document.setCategoryId(categoryId);
        document.setCategoryName(categoryName);
        document.setStock(100);
        return document;
    }
}
