package com.retail.server.controller;

import com.retail.server.common.Result;
import com.retail.server.dto.AdminWarningDTO;
import com.retail.server.service.WarningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端库存告警控制器。
 */
@RestController
@RequestMapping("/api/admin/warning")
public class AdminWarningController {

    private final WarningService warningService;

    public AdminWarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    /**
     * 查询所有未处理告警。
     */
    @GetMapping("/list")
    public Result<List<AdminWarningDTO>> listPendingWarnings() {
        return Result.success(warningService.listPendingWarnings());
    }
}