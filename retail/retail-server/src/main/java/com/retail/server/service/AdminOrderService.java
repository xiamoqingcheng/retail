package com.retail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.entity.Goods;
import com.retail.server.entity.Order;
import com.retail.server.entity.OrderItem;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.OrderItemMapper;
import com.retail.server.mapper.OrderMapper;
import com.retail.server.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;

    public AdminOrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper, GoodsMapper goodsMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.goodsMapper = goodsMapper;
    }

    public Map<String, Object> pageOrders(long page, long size, String status) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq(Order::getStatus, status);
        }
        qw.orderByDesc(Order::getCreateTime);
        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), qw);

        List<Order> orders = orderPage.getRecords();
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, List<OrderItem>> itemMap;
        Set<Long> goodsIds = new HashSet<>();
        if (!orderIds.isEmpty()) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds));
            itemMap = items.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
            items.forEach(it -> goodsIds.add(it.getGoodsId()));
        } else {
            itemMap = Map.of();
        }
        Map<Long, Goods> goodsMap;
        if (!goodsIds.isEmpty()) {
            goodsMap = goodsMapper.selectBatchIds(goodsIds).stream()
                    .collect(Collectors.toMap(Goods::getId, g -> g));
        } else {
            goodsMap = Map.of();
        }

        List<OrderVO> voList = orders.stream().map(o -> {
            List<OrderItem> items = itemMap.getOrDefault(o.getId(), List.of());
            List<OrderVO.OrderGoodsVO> goodsVOList = items.stream()
                    .map(it -> OrderVO.OrderGoodsVO.from(it, goodsMap.get(it.getGoodsId())))
                    .collect(Collectors.toList());
            return OrderVO.builder()
                    .id(o.getId())
                    .status(o.getStatus())
                    .totalAmount(o.getTotalAmount())
                    .createTime(o.getCreateTime())
                    .goods(goodsVOList)
                    .build();
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", voList);
        result.put("total", orderPage.getTotal());
        result.put("page", orderPage.getCurrent());
        result.put("size", orderPage.getSize());
        return result;
    }

    public OrderVO getOrderDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) return null;
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        Map<Long, Goods> goodsMap;
        if (!items.isEmpty()) {
            Set<Long> gids = items.stream().map(OrderItem::getGoodsId).collect(Collectors.toSet());
            goodsMap = goodsMapper.selectBatchIds(gids).stream()
                    .collect(Collectors.toMap(Goods::getId, g -> g));
        } else {
            goodsMap = Map.of();
        }
        List<OrderVO.OrderGoodsVO> goodsVOList = items.stream()
                .map(it -> OrderVO.OrderGoodsVO.from(it, goodsMap.get(it.getGoodsId())))
                .collect(Collectors.toList());
        return OrderVO.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createTime(order.getCreateTime())
                .goods(goodsVOList)
                .build();
    }
}
