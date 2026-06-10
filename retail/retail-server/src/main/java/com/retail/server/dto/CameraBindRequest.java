package com.retail.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 摄像头绑定/新增请求。
 */
public record CameraBindRequest(
        @JsonAlias({"camera_no", "cameraNo"})
        String cameraNo,
        @JsonAlias({"shelf_id", "shelfId"})
        String shelfId
) {
}