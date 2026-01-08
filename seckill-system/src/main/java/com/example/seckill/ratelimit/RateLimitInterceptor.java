package com.example.seckill.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 限流拦截器
 * 
 * 拦截带有 @RateLimit 注解的请求，执行限流检查
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        // 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取方法或类上的 @RateLimit 注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        // 没有注解，直接通过
        if (rateLimit == null) {
            return true;
        }

        // 获取限流器并尝试获取许可
        RateLimiter limiter = rateLimitService.getLimiter(rateLimit);
        if (limiter.tryAcquire()) {
            return true;
        }

        // 被限流，返回 429 状态码
        log.warn("🚫 请求被限流: {} {} (规则: {})",
                request.getMethod(), request.getRequestURI(), rateLimit.name());

        handleRateLimited(response, rateLimit);
        return false;
    }

    /**
     * 处理被限流的请求
     */
    private void handleRateLimited(HttpServletResponse response, RateLimit rateLimit)
            throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.qps()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("Retry-After", "1");

        String json = String.format(
                "{\"code\":429,\"message\":\"%s\",\"data\":null}",
                rateLimit.message());
        response.getWriter().write(json);
    }
}
