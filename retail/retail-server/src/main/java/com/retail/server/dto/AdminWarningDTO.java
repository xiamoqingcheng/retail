package com.retail.server.dto;

import java.time.LocalDateTime;

/**
 * 管理端告警列表 DTO（Java Record）。
 */
public record AdminWarningDTO(
        Long id,
        Long goodsId,
        Integer currentStock,
        String warningMsg,
        Integer status,
        LocalDateTime createTime
) {
}