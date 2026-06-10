package com.retail.server.dto;

/**
 * 管理端反馈状态更新请求。
 */
public record FeedbackStatusUpdateRequest(
        String status,
        String reply
) {
}
