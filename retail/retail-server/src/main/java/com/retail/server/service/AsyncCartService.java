package com.retail.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 购物车异步后置处理。
 */
@Slf4j
@Service
public class AsyncCartService {

    private final CartService cartService;

    public AsyncCartService(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 异步清理用户购物车，避免阻塞结算主流程。
     */
    @Async
    public void clearUserCart(Long userId) {
        try {
            cartService.clearCart(userId);
        } catch (Exception ex) {
            log.warn("异步清空购物车失败, userId={}", userId, ex);
        }
    }
}