package com.retail.server.controller;

import com.retail.server.common.Result;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.AdminOrderService;
import com.retail.server.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String status) {
        if (page < 1 || size < 1) {
            throw new BusinessException(400, "分页参数必须大于 0");
        }
        return Result.success(adminOrderService.pageOrders(page, size, status));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(400, "订单 ID 非法");
        }
        OrderVO vo = adminOrderService.getOrderDetail(id);
        if (vo == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return Result.success(vo);
    }
}
