package com.retail.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.retail.server.dto.AdminFeedbackDTO;
import com.retail.server.dto.AppletFeedbackRequest;
import com.retail.server.entity.Feedback;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.FeedbackMapper;
import com.retail.server.service.FeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * 用户反馈业务实现。
 */
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            STATUS_PENDING,
            "PROCESSING",
            STATUS_RESOLVED,
            "CLOSED"
    );

    @Override
    public Long submitAppletFeedback(AppletFeedbackRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new BusinessException(400, "反馈内容不能为空");
        }

        String content = request.content().trim();
        if (content.length() < 6) {
            throw new BusinessException(400, "反馈内容请不少于 6 个字");
        }
        if (content.length() > 1000) {
            throw new BusinessException(400, "反馈内容不能超过 1000 个字");
        }

        Feedback feedback = Feedback.builder()
                .feedbackType(limit(defaultValue(request.feedbackType(), "OTHER"), 32))
                .content(content)
                .contact(limit(request.contact(), 128))
                .apiBaseUrl(limit(request.apiBaseUrl(), 255))
                .systemInfo(limit(request.systemInfo(), 255))
                .diagnosticInfo(limit(request.diagnosticInfo(), 1000))
                .status(STATUS_PENDING)
                .build();

        if (!this.save(feedback) || feedback.getId() == null) {
            throw new BusinessException(500, "反馈提交失败");
        }
        return feedback.getId();
    }

    @Override
    public Page<AdminFeedbackDTO> pageAdminFeedback(int page, int size, String status, String feedbackType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Feedback::getStatus, normalizeStatus(status));
        }
        if (StringUtils.hasText(feedbackType)) {
            queryWrapper.eq(Feedback::getFeedbackType, feedbackType.trim());
        }
        queryWrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> feedbackPage = this.page(new Page<>(safePage, safeSize), queryWrapper);
        Page<AdminFeedbackDTO> dtoPage = new Page<>(feedbackPage.getCurrent(), feedbackPage.getSize(), feedbackPage.getTotal());
        dtoPage.setRecords(feedbackPage.getRecords().stream().map(this::toDTO).toList());
        return dtoPage;
    }

    @Override
    public AdminFeedbackDTO updateFeedbackStatus(Long id, String status, String reply) {
        if (id == null || id < 1) {
            throw new BusinessException(400, "反馈 ID 无效");
        }

        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new BusinessException(404, "反馈不存在");
        }

        String nextStatus = normalizeStatus(status);
        feedback.setStatus(nextStatus);
        feedback.setReply(limit(reply, 500));
        feedback.setUpdateTime(LocalDateTime.now());
        feedback.setResolvedTime(STATUS_RESOLVED.equals(nextStatus) || "CLOSED".equals(nextStatus)
                ? LocalDateTime.now()
                : null);

        if (!this.updateById(feedback)) {
            throw new BusinessException(500, "反馈状态更新失败");
        }
        return toDTO(feedback);
    }

    private String normalizeStatus(String status) {
        String value = defaultValue(status, STATUS_PENDING).trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(value)) {
            throw new BusinessException(400, "反馈状态无效");
        }
        return value;
    }

    private AdminFeedbackDTO toDTO(Feedback feedback) {
        return new AdminFeedbackDTO(
                feedback.getId(),
                feedback.getUserId(),
                feedback.getFeedbackType(),
                feedback.getContent(),
                feedback.getContact(),
                feedback.getApiBaseUrl(),
                feedback.getSystemInfo(),
                feedback.getDiagnosticInfo(),
                feedback.getStatus(),
                feedback.getReply(),
                feedback.getCreateTime(),
                feedback.getUpdateTime(),
                feedback.getResolvedTime()
        );
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
