package com.retail.server.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void defaultConstructorUsesBadRequestCode() {
        BusinessException exception = new BusinessException("参数错误");

        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).isEqualTo("参数错误");
    }

    @Test
    void customConstructorKeepsBusinessCode() {
        BusinessException exception = new BusinessException(403, "无权限");

        assertThat(exception.getCode()).isEqualTo(403);
        assertThat(exception.getMessage()).isEqualTo("无权限");
    }
}
