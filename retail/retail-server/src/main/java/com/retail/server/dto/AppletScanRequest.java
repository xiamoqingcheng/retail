package com.retail.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 小程序扫码请求参数。
 */
public record AppletScanRequest(
        @JsonAlias({"image_base64", "imageBase64"})
        String imageBase64,
        Integer k
) {
}