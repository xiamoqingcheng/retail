package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.common.Result;
import com.retail.server.entity.InventoryLog;
import com.retail.server.mapper.InventoryLogMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory-log")
public class InventoryLogController {

    private final InventoryLogMapper inventoryLogMapper;

    public InventoryLogController(InventoryLogMapper inventoryLogMapper) {
        this.inventoryLogMapper = inventoryLogMapper;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<InventoryLog> qw = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            qw.eq(InventoryLog::getType, type);
        }
        qw.orderByDesc(InventoryLog::getCreateTime);
        Page<InventoryLog> result = inventoryLogMapper.selectPage(new Page<>(page, size), qw);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }
}
