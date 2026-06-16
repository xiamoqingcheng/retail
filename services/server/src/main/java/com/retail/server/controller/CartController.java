package com.retail.server.controller;

import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.dto.CartDTO;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.CartService;
import com.retail.server.vo.CartVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端购物车控制器。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 加入或修改购物车。
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CartDTO cartDTO) {
        if (cartDTO == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }

        Long userId = currentUserId();
        cartService.addCart(userId, cartDTO.getGoodsId(), cartDTO.getQuantity());
        return Result.success("操作成功", null);
    }

    /**
     * 查询购物车列表。
     */
    @GetMapping("/list")
    public Result<List<CartVO>> list() {
        Long userId = currentUserId();
        return Result.success(cartService.getCartList(userId));
    }

    /**
     * 清空购物车。
     */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        Long userId = currentUserId();
        cartService.clearCart(userId);
        return Result.success("清空成功", null);
    }

    /**
     * 从上下文读取当前登录用户 ID（由拦截器提前解析 Token 写入）。
     */
    private Long currentUserId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || userId < 1) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return userId;
    }
}
