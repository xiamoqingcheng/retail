package com.retail.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.retail.server.entity.Goods;
import com.retail.server.entity.InventoryLog;
import com.retail.server.entity.Order;
import com.retail.server.entity.OrderItem;
import com.retail.server.entity.Warning;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.InventoryLogMapper;
import com.retail.server.mapper.OrderItemMapper;
import com.retail.server.mapper.OrderMapper;
import com.retail.server.service.AsyncCartService;
import com.retail.server.service.CartService;
import com.retail.server.service.OrderService;
import com.retail.server.service.WarningService;
import com.retail.server.vo.CartVO;
import com.retail.server.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单业务实现。
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CartService cartService;
    private final AsyncCartService asyncCartService;
    private final GoodsMapper goodsMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final OrderItemMapper orderItemMapper;
    private final WarningService warningService;

    public OrderServiceImpl(
            CartService cartService,
            AsyncCartService asyncCartService,
            GoodsMapper goodsMapper,
            InventoryLogMapper inventoryLogMapper,
            OrderItemMapper orderItemMapper,
            WarningService warningService) {
        this.cartService = cartService;
        this.asyncCartService = asyncCartService;
        this.goodsMapper = goodsMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.orderItemMapper = orderItemMapper;
        this.warningService = warningService;
    }

    /**
     * 订单结算核心流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkout(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "用户未登录");
        }

        // 1. 从 Redis 获取购物车。
        return checkout(userId, cartService.getCartList(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkout(Long userId, List<CartVO> cartItems) {
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "user not logged in");
        }
        if (CollectionUtils.isEmpty(cartItems)) {
            throw new BusinessException(400, "购物车为空，无法结算");
        }

        List<CartVO> normalizedItems = new ArrayList<>();
        for (CartVO item : cartItems) {
            if (item == null || item.getGoodsId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(400, "购物车商品数据非法");
            }
            normalizedItems.add(item);
        }
        if (CollectionUtils.isEmpty(normalizedItems)) {
            throw new BusinessException(400, "购物车为空，无法结算");
        }

        // 按商品 ID 排序后逐条加锁，减少并发结算时的死锁概率。
        normalizedItems.sort(Comparator.comparing(CartVO::getGoodsId));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> itemList = new ArrayList<>();

        // 2-3. 行级锁校验库存并扣减库存。
        for (CartVO item : normalizedItems) {
            Goods goods = goodsMapper.selectByIdForUpdate(item.getGoodsId());
            if (goods == null) {
                throw new BusinessException(404, "商品不存在或已下架");
            }

            if (goods.getStock() == null || goods.getStock() < item.getQuantity()) {
                throw new BusinessException("库存不足");
            }

            int affected = goodsMapper.decreaseStock(goods.getId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException("库存不足");
            }

            int remainStock = goods.getStock() - item.getQuantity();

            InventoryLog inventoryLog = InventoryLog.builder()
                    .goodsId(goods.getId())
                    .changeAmount(-item.getQuantity())
                    .currentStock(remainStock)
                    .type("SALE")
                    .build();
            if (inventoryLogMapper.insert(inventoryLog) != 1) {
                throw new BusinessException(500, "记录库存日志失败");
            }

            int safeThreshold = goods.getSafeStock() != null ? goods.getSafeStock() : 10;
            if (remainStock < safeThreshold) {
                String goodsName = StringUtils.hasText(goods.getName()) ? goods.getName() : "未知商品";
                Warning warning = Warning.builder()
                        .goodsId(goods.getId())
                        .warningType(remainStock == 0 ? "OUT_OF_STOCK" : "LOW_STOCK")
                        .warningMsg("商品" + goodsName + "库存不足，当前剩余:" + remainStock)
                        .status(0)
                        .build();
                boolean warningSaved = warningService.save(warning);
                if (!warningSaved) {
                    throw new BusinessException(500, "生成库存告警失败");
                }
            }

            BigDecimal currentPrice = goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice();
            totalAmount = totalAmount.add(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())));

            itemList.add(OrderItem.builder()
                    .goodsId(goods.getId())
                    .goodsName(goods.getName())
                    .price(currentPrice)
                    .quantity(item.getQuantity())
                    .build());
        }

        // 4. 生成订单主记录。
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .status("PENDING")
                .build();
        boolean orderSaved = this.save(order);
        if (!orderSaved || order.getId() == null) {
            throw new BusinessException(500, "创建订单失败");
        }

        // 5. 批量插入订单明细。
        for (OrderItem item : itemList) {
            item.setOrderId(order.getId());
        }
        int inserted = orderItemMapper.insertBatch(itemList);
        if (inserted != itemList.size()) {
            throw new BusinessException(500, "创建订单明细失败");
        }

        // 6. 事务提交后异步清空 Redis 购物车。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncCartService.clearUserCart(userId);
                }
            });
        } else {
            asyncCartService.clearUserCart(userId);
        }

        return order.getId();
    }

    @Override
    public List<OrderVO> listOrdersByUserId(Long userId) {
        List<Order> orders = this.lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .list();

        if (CollectionUtils.isEmpty(orders)) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem> itemQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        itemQuery.in(OrderItem::getOrderId, orderIds);
        List<OrderItem> allItems = orderItemMapper.selectList(itemQuery);

        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, LinkedHashMap::new, Collectors.toList()));

        // Collect goods ids for image lookup
        List<Long> goodsIds = allItems.stream()
                .map(OrderItem::getGoodsId)
                .distinct()
                .toList();
        Map<Long, Goods> goodsMap = new LinkedHashMap<>();
        if (!goodsIds.isEmpty()) {
            List<Goods> goodsList = goodsMapper.selectBatchIds(goodsIds);
            if (goodsList != null) {
                for (Goods g : goodsList) {
                    goodsMap.put(g.getId(), g);
                }
            }
        }

        List<OrderVO> result = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
            List<OrderVO.OrderGoodsVO> goodsVOs = items.stream()
                    .map(item -> OrderVO.OrderGoodsVO.from(item, goodsMap.get(item.getGoodsId())))
                    .toList();

            result.add(OrderVO.builder()
                    .id(order.getId())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .createTime(order.getCreateTime())
                    .goods(goodsVOs)
                    .build());
        }
        return result;
    }
}
