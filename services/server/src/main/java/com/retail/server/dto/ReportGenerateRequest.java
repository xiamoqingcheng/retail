package com.retail.server.dto;

/**
 * 手动生成报表请求。start/end 接受 ISO（2026-06-10T00:00:00）或 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd"。
 */
public record ReportGenerateRequest(
        String start,
        String end
) {
}
