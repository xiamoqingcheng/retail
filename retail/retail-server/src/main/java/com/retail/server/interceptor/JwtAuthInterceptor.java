package com.retail.server.interceptor;

import com.retail.server.context.UserContext;
import com.retail.server.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器：解析 Token 并将当前 userId 写入 UserContext。
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final StringRedisTemplate stringRedisTemplate;

    public JwtAuthInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = null;

        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        }

        // 回退：支持 query 参数传递 token（用于 <img src> 加载视频流等场景）
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }

        if (!StringUtils.hasText(token) && isOptionalAuthPath(request)) {
            return true;
        }

        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未登录或Token缺失");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            var claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();

            if (!StringUtils.hasText(subject)) {
                throw new BusinessException(401, "Token无效");
            }

            String role = claims.get("role", String.class);
            String redisKey = TOKEN_KEY_PREFIX + (role != null ? role + ":" : "") + subject;

            // 检查 Token 是否已在 Redis 中被撤销（登出/踢下线场景）
            String storedToken = null;
            try {
                storedToken = stringRedisTemplate.opsForValue().get(redisKey);
            } catch (Exception e) {
                log.debug("Redis 不可用，跳过 Token 撤销检查", e);
            }
            if (storedToken != null && !storedToken.equals(token)) {
                if (isOptionalAuthPath(request)) {
                    return true;
                }
                throw new BusinessException(401, "Token已失效，请重新登录");
            }

            UserContext.setCurrentUserId(Long.parseLong(subject));
            UserContext.setCurrentRole(role);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            if (isOptionalAuthPath(request)) {
                return true;
            }
            throw new BusinessException(401, "Token无效或已过期");
        }
    }

    private boolean isOptionalAuthPath(HttpServletRequest request) {
        return "/api/applet/home/recommend".equals(request.getRequestURI());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
