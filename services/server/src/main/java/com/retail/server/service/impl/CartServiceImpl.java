package com.retail.server.service.impl;

import com.retail.server.entity.Goods;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.CartService;
import com.retail.server.service.GoodsService;
import com.retail.server.vo.CartVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车业务实现。
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "cart:user:";

    private final StringRedisTemplate stringRedisTemplate;
    private final GoodsService goodsService;

    public CartServiceImpl(StringRedisTemplate stringRedisTemplate, GoodsService goodsService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.goodsService = goodsService;
    }

    /**
     * 加入或修改购物车。
     */
    @Override
    public void addCart(Long userId, Long goodsId, Integer quantity) {
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "用户未登录");
        }
        if (goodsId == null) {
            throw new BusinessException(400, "商品 ID 不能为空");
        }
        if (quantity == null || quantity < 0) {
            throw new BusinessException(400, "商品数量不能小于 0");
        }

        // 仅在写入数量大于 0 时校验商品是否存在。
        if (quantity > 0 && goodsService.getById(goodsId) == null) {
            throw new BusinessException(404, "商品不存在");
        }

        // 选择 Hash 结构是因为购物车读写频繁，且常按 goodsId 更新单个字段，
        // Hash 能做到单字段原子写入和删除，避免整对象反序列化带来的开销。
        String cartKey = buildCartKey(userId);
        try {
            if (quantity == 0) {
                stringRedisTemplate.opsForHash().delete(cartKey, goodsId.toString());
            } else {
                stringRedisTemplate.opsForHash().put(cartKey, goodsId.toString(), quantity.toString());
            }
        } catch (Exception ex) {
            log.warn("Cart cache write skipped: userId={}, goodsId={}, error={}",
                    userId, goodsId, ex.getMessage());
        }
    }

    /**
     * 查询购物车列表并补全商品最新信息。
     */
    @Override
    public List<CartVO> getCartList(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "用户未登录");
        }

        String cartKey = buildCartKey(userId);
        Map<Object, Object> redisEntries;
        try {
            redisEntries = stringRedisTemplate.opsForHash().entries(cartKey);
        } catch (Exception ex) {
            log.warn("Cart cache read failed, return empty list: userId={}, error={}",
                    userId, ex.getMessage());
            return Collections.emptyList();
        }

        if (redisEntries == null || redisEntries.isEmpty()) {
            return Collections.emptyList();
        }

        // 先整理 Redis 中的商品数量，非法数据自动跳过。
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : redisEntries.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            try {
                Long goodsId = Long.parseLong(entry.getKey().toString());
                Integer quantity = Integer.parseInt(entry.getValue().toString());
                if (quantity > 0) {
                    quantityMap.put(goodsId, quantity);
                }
            } catch (NumberFormatException ignore) {
                log.debug("忽略购物车异常数据: key={}", entry.getKey());
            }
        }

        if (quantityMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> goodsIds = new ArrayList<>(quantityMap.keySet());
        List<Goods> goodsList = goodsService.listByIds(goodsIds);
        if (goodsList == null || goodsList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Goods> goodsMap = new HashMap<>();
        for (Goods goods : goodsList) {
            if (goods != null && goods.getId() != null) {
                goodsMap.put(goods.getId(), goods);
            }
        }

        List<CartVO> result = new ArrayList<>();
        for (Long goodsId : goodsIds) {
            Goods goods = goodsMap.get(goodsId);
            Integer quantity = quantityMap.get(goodsId);
            if (goods == null || quantity == null) {
                continue;
            }

            result.add(CartVO.builder()
                    .goodsId(goods.getId())
                    .goodsName(goods.getName())
                    .price(goods.getPrice())
                    .quantity(quantity)
                    .imageUrl(goods.getImageUrl())
                    .build());
        }

        return result;
    }

    /**
     * 清空购物车。
     */
    @Override
    public void clearCart(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "用户未登录");
        }

        try {
            stringRedisTemplate.delete(buildCartKey(userId));
        } catch (Exception ex) {
            log.warn("Cart cache clear skipped: userId={}, error={}", userId, ex.getMessage());
        }
    }

    private String buildCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
}
