package com.retail.server.config;

import com.retail.server.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${rate-limit.max-requests-per-window:240}")
    private int maxRequestsPerWindow;
    private static final long WINDOW_MS = 60_000;
    private final ConcurrentHashMap<String, RateLimitEntry> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/admin/camera/stream")) {
            return true;
        }
        String key = extractKey(request);
        RateLimitEntry entry = counters.computeIfAbsent(key, k -> new RateLimitEntry());
        long now = System.currentTimeMillis();
        if (now - entry.windowStart > WINDOW_MS) {
            entry.count.set(1);
            entry.windowStart = now;
        } else {
            long current = entry.count.incrementAndGet();
            if (current > maxRequestsPerWindow) {
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
        }
        return true;
    }

    private String extractKey(HttpServletRequest request) {
        String userId = request.getHeader("Authorization");
        if (userId != null && !userId.isEmpty()) {
            return "tok:" + userId.hashCode();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final AtomicLong count = new AtomicLong(1);
        volatile long windowStart = System.currentTimeMillis();
    }
}
