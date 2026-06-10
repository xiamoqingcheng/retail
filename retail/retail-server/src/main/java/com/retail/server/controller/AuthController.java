package com.retail.server.controller;

import com.retail.server.common.Result;
import com.retail.server.context.UserContext;
import com.retail.server.dto.LoginRequest;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户鉴权控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthController(UserService userService, StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 登录接口，成功后返回 JWT Token。
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }

        String token = userService.login(request.getUsername(), request.getPassword());
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("token", token);
        resultData.put("tokenType", "Bearer");
        return Result.success("登录成功", resultData);
    }

    /**
     * 退出登录接口：从 Redis 中删除 Token 以使其失效。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentRole();
        if (userId != null) {
            try {
                String redisKey = TOKEN_KEY_PREFIX + (role != null ? role + ":" : "") + userId;
                stringRedisTemplate.delete(redisKey);
            } catch (Exception e) {
                log.warn("退出登录时清理 Redis Token 失败: userId={}", userId, e);
            }
        }
        return Result.success("退出登录成功", null);
    }
}
