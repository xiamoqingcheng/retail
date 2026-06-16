package com.retail.server.service;

import com.retail.server.vo.CartVO;

import java.util.List;

/**
 * 购物车业务层。
 */
public interface CartService {

    /**
     * 加入或修改购物车。
     */
    void addCart(Long userId, Long goodsId, Integer quantity);

    /**
     * 查询当前用户购物车列表。
     */
    List<CartVO> getCartList(Long userId);

    /**
     * 清空当前用户购物车。
     */
    void clearCart(Long userId);
}