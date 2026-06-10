package com.retail.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 摄像头货架绑定更新请求。
 */
public record CameraUpdateRequest(
        Long id,
        @JsonAlias({"shelf_id", "shelfId"})
        String shelfId
) {
}