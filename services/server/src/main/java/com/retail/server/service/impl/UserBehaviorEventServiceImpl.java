package com.retail.server.service.impl;

import com.retail.server.entity.OrderItem;
import com.retail.server.entity.UserBehaviorEvent;
import com.retail.server.mapper.UserBehaviorEventMapper;
import com.retail.server.recommendation.UserBehaviorEventType;
import com.retail.server.service.UserBehaviorEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserBehaviorEventServiceImpl implements UserBehaviorEventService {

    private final UserBehaviorEventMapper userBehaviorEventMapper;

    public UserBehaviorEventServiceImpl(UserBehaviorEventMapper userBehaviorEventMapper) {
        this.userBehaviorEventMapper = userBehaviorEventMapper;
    }

    @Override
    public void recordSearch(Long userId, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!validUser(userId) || !StringUtils.hasText(normalizedKeyword)) {
            return;
        }
        insert(userId, UserBehaviorEventType.SEARCH, null, normalizedKeyword, 1, null);
    }

    @Override
    public void recordView(Long userId, Long goodsId) {
        if (!validUser(userId) || goodsId == null || goodsId < 1) {
            return;
        }
        insert(userId, UserBehaviorEventType.VIEW, goodsId, null, 1, null);
    }

    @Override
    public void recordPurchase(Long userId, Long orderId, List<OrderItem> items) {
        recordOrderItems(userId, orderId, items, UserBehaviorEventType.PURCHASE);
    }

    @Override
    public void recordCancel(Long userId, Long orderId, List<OrderItem> items) {
        recordOrderItems(userId, orderId, items, UserBehaviorEventType.CANCEL);
    }

    private void recordOrderItems(Long userId, Long orderId, List<OrderItem> items, String eventType) {
        if (!validUser(userId) || CollectionUtils.isEmpty(items)) {
            return;
        }
        for (OrderItem item : items) {
            if (item == null || item.getGoodsId() == null || item.getGoodsId() < 1) {
                continue;
            }
            int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            insert(userId, eventType, item.getGoodsId(), null, quantity, orderId);
        }
    }

    private void insert(Long userId, String eventType, Long goodsId, String keyword, Integer quantity, Long orderId) {
        UserBehaviorEvent event = UserBehaviorEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .goodsId(goodsId)
                .keyword(keyword)
                .quantity(quantity == null || quantity < 1 ? 1 : quantity)
                .orderId(orderId)
                .createTime(LocalDateTime.now())
                .build();
        try {
            userBehaviorEventMapper.insert(event);
        } catch (DataAccessException ex) {
            log.warn("Skip user behavior event: userId={}, eventType={}, error={}",
                    userId, eventType, ex.getMessage());
        }
    }

    private boolean validUser(Long userId) {
        return userId != null && userId > 0;
    }
}
