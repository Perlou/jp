package com.example.seckill.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流管理服务
 * 
 * 统一管理所有限流器实例：
 * - 根据配置创建限流器
 * - 提供限流检查服务
 * - 管理熔断器
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * 限流器缓存
     */
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * 熔断器缓存
     */
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * 获取或创建令牌桶限流器
     */
    public RateLimiter getTokenBucketLimiter(String name, int qps) {
        return rateLimiters.computeIfAbsent(name + ":token_bucket",
                k -> {
                    log.info("📊 创建令牌桶限流器: {} (QPS={})", name, qps);
                    return new TokenBucketRateLimiter(qps * 2, qps);
                });
    }

    /**
     * 获取或创建滑动窗口限流器
     */
    public RateLimiter getSlidingWindowLimiter(String name, int qps) {
        return rateLimiters.computeIfAbsent(name + ":sliding_window",
                k -> {
                    log.info("📊 创建滑动窗口限流器: {} (QPS={})", name, qps);
                    return new SlidingWindowRateLimiter(qps);
                });
    }

    /**
     * 根据注解配置获取限流器
     */
    public RateLimiter getLimiter(RateLimit config) {
        return switch (config.algorithm()) {
            case TOKEN_BUCKET -> getTokenBucketLimiter(config.name(), config.qps());
            case SLIDING_WINDOW -> getSlidingWindowLimiter(config.name(), config.qps());
        };
    }

    /**
     * 尝试获取限流许可
     */
    public boolean tryAcquire(String name, int qps, RateLimit.Algorithm algorithm) {
        RateLimiter limiter = switch (algorithm) {
            case TOKEN_BUCKET -> getTokenBucketLimiter(name, qps);
            case SLIDING_WINDOW -> getSlidingWindowLimiter(name, qps);
        };
        return limiter.tryAcquire();
    }

    /**
     * 获取或创建熔断器
     */
    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(name,
                k -> {
                    log.info("🔌 创建熔断器: {}", name);
                    return new CircuitBreaker(name);
                });
    }

    /**
     * 获取或创建熔断器（自定义配置）
     */
    public CircuitBreaker getCircuitBreaker(String name, int failureThreshold,
            int successThreshold, long openTimeoutMs) {
        return circuitBreakers.computeIfAbsent(name,
                k -> {
                    log.info("🔌 创建熔断器: {} (失败阈值={}, 恢复阈值={}, 超时={}ms)",
                            name, failureThreshold, successThreshold, openTimeoutMs);
                    return new CircuitBreaker(name, failureThreshold, successThreshold, openTimeoutMs);
                });
    }

    /**
     * 执行受熔断保护的操作
     */
    public <T> T executeWithCircuitBreaker(String name,
            CircuitBreaker.ProtectedAction<T> action,
            CircuitBreaker.FallbackAction<T> fallback) {
        CircuitBreaker breaker = getCircuitBreaker(name);
        return breaker.execute(action, fallback);
    }

    /**
     * 获取所有限流器统计
     */
    public Map<String, RateLimiterStats> getAllRateLimiterStats() {
        Map<String, RateLimiterStats> stats = new ConcurrentHashMap<>();
        rateLimiters.forEach((name, limiter) -> stats.put(name, limiter.getStats()));
        return stats;
    }

    /**
     * 获取所有熔断器统计
     */
    public Map<String, CircuitBreaker.CircuitBreakerStats> getAllCircuitBreakerStats() {
        Map<String, CircuitBreaker.CircuitBreakerStats> stats = new ConcurrentHashMap<>();
        circuitBreakers.forEach((name, breaker) -> stats.put(name, breaker.getStats()));
        return stats;
    }

    /**
     * 重置指定熔断器
     */
    public void resetCircuitBreaker(String name) {
        CircuitBreaker breaker = circuitBreakers.get(name);
        if (breaker != null) {
            breaker.reset();
            log.info("🔄 熔断器 [{}] 已重置", name);
        }
    }

    /**
     * 清除所有限流器（用于测试）
     */
    public void clearAll() {
        rateLimiters.clear();
        circuitBreakers.clear();
        log.info("🗑️ 已清除所有限流器和熔断器");
    }
}
