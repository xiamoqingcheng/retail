package com.retail.server.exception;

import lombok.Getter;

/**
 * 自定义业务异常，用于向上层返回可读的业务错误信息。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
