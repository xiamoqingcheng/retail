package com.retail.server.dto;

/**
 * 定时报表调度配置请求（天:时:分 间隔）。
 */
public record ReportScheduleRequest(
        Integer enabled,
        Integer intervalDays,
        Integer intervalHours,
        Integer intervalMinutes
) {
}
