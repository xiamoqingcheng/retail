package com.retail.server.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successUsesStandardCodeAndMessage() {
        Result<String> result = Result.success("payload");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    void failKeepsBusinessCodeAndMessage() {
        Result<Object> result = Result.fail(401, "未登录");

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("未登录");
        assertThat(result.getData()).isNull();
    }
}
