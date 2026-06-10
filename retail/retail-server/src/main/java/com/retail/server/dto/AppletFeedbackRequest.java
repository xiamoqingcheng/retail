package com.retail.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 小程序反馈提交请求。
 */
public record AppletFeedbackRequest(
        @JsonAlias({"type", "feedback_type"})
        String feedbackType,
        String content,
        String contact,
        @JsonAlias({"apiBaseUrl", "api_base_url"})
        String apiBaseUrl,
        @JsonAlias({"systemInfo", "system_info"})
        String systemInfo,
        @JsonAlias({"diagnosticInfo", "diagnostic_info"})
        String diagnosticInfo
) {
}
