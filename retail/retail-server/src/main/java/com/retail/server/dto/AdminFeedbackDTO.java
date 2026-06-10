package com.retail.server.dto;

import java.time.LocalDateTime;

/**
 * 管理端反馈列表项。
 */
public record AdminFeedbackDTO(
        Long id,
        Long userId,
        String feedbackType,
        String content,
        String contact,
        String apiBaseUrl,
        String systemInfo,
        String diagnosticInfo,
        String status,
        String reply,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime resolvedTime
) {
}
