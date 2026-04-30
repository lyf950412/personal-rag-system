package com.rag.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_CHAT_REQUESTS_PER_MINUTE = 10;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        long currentTime = System.currentTimeMillis();
        String key = clientIp + ":" + requestUri;
        RateLimitInfo rateLimitInfo = requestCounts.computeIfAbsent(key, k -> new RateLimitInfo());
        if (currentTime - rateLimitInfo.windowStart > 60000) {
            rateLimitInfo.reset(currentTime);
        }
        int maxRequests = requestUri.contains("/chat") ? MAX_CHAT_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
        if (rateLimitInfo.count.incrementAndGet() > maxRequests) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁\"}");
            return false;
        }
        return true;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        requestCounts.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue().windowStart > 120000);
    }
    
    private static class RateLimitInfo {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        public void reset(long currentTime) { count.set(0); windowStart = currentTime; }
    }
}
