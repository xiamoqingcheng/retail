package com.retail.server.service;

import com.retail.server.entity.OrderItem;

import java.util.List;

public interface UserBehaviorEventService {
    void recordSearch(Long userId, String keyword);

    void recordView(Long userId, Long goodsId);

    void recordPurchase(Long userId, Long orderId, List<OrderItem> items);

    void recordCancel(Long userId, Long orderId, List<OrderItem> items);
}
