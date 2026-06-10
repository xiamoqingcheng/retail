package com.retail.server.controller.applet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.dto.AppletCheckoutRequest;
import com.retail.server.dto.AppletOrderCancelRequest;
import com.retail.server.dto.AppletOrderPayRequest;
import com.retail.server.entity.Goods;
import com.retail.server.entity.Order;
import com.retail.server.entity.OrderItem;
import com.retail.server.entity.WechatUser;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.OrderItemMapper;
import com.retail.server.mapper.WechatUserMapper;
import com.retail.server.service.CartService;
import com.retail.server.service.OrderService;
import com.retail.server.vo.OrderVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 小程序订单控制器。
 */
@RestController
@RequestMapping("/api/applet/order")
public class AppletOrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final WechatUserMapper wechatUserMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;

    public AppletOrderController(OrderService orderService, CartService cartService,
                                  WechatUserMapper wechatUserMapper, OrderItemMapper orderItemMapper,
                                  GoodsMapper goodsMapper) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.wechatUserMapper = wechatUserMapper;
        this.orderItemMapper = orderItemMapper;
        this.goodsMapper = goodsMapper;
    }

    /**
     * 小程序结算：接收客户端购物车商品列表，同步到 Redis 后执行结算。
     */
    @PostMapping("/checkout")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> checkout(@RequestBody AppletCheckoutRequest request) {
        Long userId = currentUserId();

        if (request != null && request.getItems() != null && !request.getItems().isEmpty()) {
            cartService.clearCart(userId);
            for (AppletCheckoutRequest.CartItem item : request.getItems()) {
                if (item.getId() != null && item.getCount() != null && item.getId() > 0 && item.getCount() > 0) {
                    cartService.addCart(userId, item.getId(), item.getCount());
                }
            }
        }

        Long orderId = orderService.checkout(userId);
        return Result.success("结算成功", orderId);
    }

    /**
     * 查询当前用户的订单列表。
     */
    @GetMapping("/list")
    public Result<List<OrderVO>> list() {
        Long userId = currentUserId();
        return Result.success(orderService.listOrdersByUserId(userId));
    }

    /**
     * 订单支付（将 PENDING 订单置为 PAID，若使用余额支付则扣减余额）。
     */
    @PostMapping("/pay")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> pay(@RequestBody AppletOrderPayRequest request) {
        Long userId = currentUserId();
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException(400, "缺少orderId");
        }
        Long orderId = request.getOrderId();

        String paymentMethod = request.getPaymentMethod() != null
                ? request.getPaymentMethod() : "balance";

        // 1. 先查询订单，校验归属和状态
        Order order = orderService.getById(orderId);
        if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }

        if ("PAID".equals(order.getStatus())) {
            return Result.success("该订单已支付", null);
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态异常，无法支付");
        }

        // 2. 余额支付：原子扣减，防止并发超扣
        if ("balance".equals(paymentMethod)) {
            BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "订单金额异常");
            }

            // UPDATE balance = balance - ? WHERE id = ? AND balance >= ?
            LambdaUpdateWrapper<WechatUser> balanceUpdate = new LambdaUpdateWrapper<WechatUser>()
                    .eq(WechatUser::getId, userId)
                    .ge(WechatUser::getBalance, totalAmount)
                    .setSql("balance = balance - {0}", totalAmount);
            int rows = wechatUserMapper.update(null, balanceUpdate);
            if (rows == 0) {
                throw new BusinessException(400, "余额不足");
            }
        }

        // 3. 更新订单状态 PENDING → PAID
        boolean updated = orderService.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, "PENDING")
                .set(Order::getStatus, "PAID"));
        if (!updated) {
            throw new BusinessException(400, "订单状态异常，支付失败");
        }
        return Result.success("支付成功", null);
    }

    /**
     * 取消订单（仅 PENDING 状态可取消，恢复库存）。
     */
    @PostMapping("/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(@RequestBody AppletOrderCancelRequest request) {
        Long userId = currentUserId();
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException(400, "缺少orderId");
        }
        Long orderId = request.getOrderId();

        Order order = orderService.getById(orderId);
        if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(400, "仅待付款订单可取消");
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            if (item.getGoodsId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                goodsMapper.update(null, new LambdaUpdateWrapper<Goods>()
                        .eq(Goods::getId, item.getGoodsId())
                        .setSql("stock = stock + {0}", item.getQuantity()));
            }
        }

        boolean updated = orderService.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, "PENDING")
                .set(Order::getStatus, "CANCELLED"));
        if (!updated) {
            throw new BusinessException(400, "取消订单失败");
        }
        return Result.success("订单已取消", null);
    }

    private Long currentUserId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return userId;
    }
}
