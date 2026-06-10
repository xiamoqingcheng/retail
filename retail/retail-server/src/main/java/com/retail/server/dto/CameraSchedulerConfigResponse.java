package com.retail.server.dto;

/**
 * 摄像头定时巡检配置响应。
 */
public record CameraSchedulerConfigResponse(
        Integer intervalMinutes,
        Integer batchSize
) {
}