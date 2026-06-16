package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.common.Result;
import com.retail.server.entity.Warning;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.WarningService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 库存告警控制器。
 */
@RestController
@RequestMapping("/api/warning")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    /**
     * 分页查询告警列表。
     */
    @GetMapping("/page")
    public Result<Page<Warning>> page(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        if (page < 1 || size < 1) {
            throw new BusinessException(400, "分页参数必须大于 0");
        }

        Page<Warning> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Warning> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(Warning::getStatus, status);
        }
        queryWrapper.orderByDesc(Warning::getCreateTime);

        warningService.page(pageParam, queryWrapper);
        return Result.success(pageParam);
    }

    /**
     * 将告警标记为已处理。
     */
    @PutMapping("/{id}/resolve")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resolve(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(400, "告警 ID 非法");
        }

        Warning warning = warningService.getById(id);
        if (warning == null) {
            throw new BusinessException(404, "告警不存在");
        }

        warning.setStatus(1);
        warning.setResolveTime(LocalDateTime.now());
        boolean updated = warningService.updateById(warning);
        if (!updated) {
            throw new BusinessException(500, "处理告警失败");
        }
        return Result.success("处理成功", null);
    }
}
