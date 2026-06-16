package com.retail.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.common.Result;
import com.retail.server.dto.AdminFeedbackDTO;
import com.retail.server.dto.FeedbackStatusUpdateRequest;
import com.retail.server.service.FeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端反馈控制器。
 */
@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/page")
    public Result<Page<AdminFeedbackDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feedbackType) {
        return Result.success(feedbackService.pageAdminFeedback(page, size, status, feedbackType));
    }

    @PutMapping("/{id}/status")
    public Result<AdminFeedbackDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody FeedbackStatusUpdateRequest request) {
        String status = request == null ? null : request.status();
        String reply = request == null ? null : request.reply();
        AdminFeedbackDTO result = feedbackService.updateFeedbackStatus(id, status, reply);
        return Result.success("反馈状态已更新", result);
    }
}
