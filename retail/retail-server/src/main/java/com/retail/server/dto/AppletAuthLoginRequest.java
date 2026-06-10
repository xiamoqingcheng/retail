package com.retail.server.dto;

import lombok.Data;

/**
 * 小程序登录请求参数。
 */
@Data
public class AppletAuthLoginRequest {

    /**
     * 微信登录凭证。
     */
    private String code;
}
