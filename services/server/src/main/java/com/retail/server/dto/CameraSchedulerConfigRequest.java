package com.retail.server.dto;

/**
 * 摄像头定时巡检配置请求。
 */
public record CameraSchedulerConfigRequest(
        Integer intervalMinutes,
        Integer batchSize
) {
}