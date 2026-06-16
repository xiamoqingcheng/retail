package com.retail.server.exception;

import com.retail.server.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

/**
 * 全局异常处理器，统一包装接口异常返回。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String traceId(HttpServletRequest request) {
        String tid = request.getHeader("X-Trace-Id");
        return tid != null ? tid : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        if (ex.getCode() >= 500) {
            log.error("[traceId={}] 业务异常: {}", traceId(request), ex.getMessage(), ex);
        }
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return Result.fail(404, "接口不存在，请检查请求路径");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return Result.fail(405, "请求方法不支持");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return Result.fail(400, "请求体 JSON 格式错误");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return Result.fail(415, "Content-Type 不支持，请使用 application/json");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("[traceId={}] 系统异常", traceId(request), ex);
        return Result.fail(500, "服务器内部异常");
    }
}
