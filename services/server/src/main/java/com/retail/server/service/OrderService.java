package com.retail.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.retail.server.entity.Order;
import com.retail.server.vo.CartVO;
import com.retail.server.vo.OrderVO;

import java.util.List;

/**
 * 订单主表业务层。
 */
public interface OrderService extends IService<Order> {

    /**
     * 订单结算。
     */
    Long checkout(Long userId);

    Long checkout(Long userId, List<CartVO> cartItems);

    /**
     * 查询用户订单列表（含明细）。
     */
    List<OrderVO> listOrdersByUserId(Long userId);
}
