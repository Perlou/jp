package com.example.seckill.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 熔断器实现
 * 
 * 基于状态机的熔断器模式：
 * - CLOSED: 正常状态，请求正常通过
 * - OPEN: 熔断状态，请求快速失败
 * - HALF_OPEN: 半开状态，允许部分请求尝试
 * 
 * 状态转换：
 * CLOSED -> (失败率超阈值) -> OPEN
 * OPEN -> (等待超时) -> HALF_OPEN
 * HALF_OPEN -> (尝试成功) -> CLOSED
 * HALF_OPEN -> (尝试失败) -> OPEN
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    /**
     * 熔断器状态
     */
    public enum State {
        CLOSED("关闭", "正常运行"),
        OPEN("打开", "熔断中"),
        HALF_OPEN("半开", "尝试恢复");

        private final String name;
        private final String description;

        State(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getDisplayName() {
            return name + " - " + description;
        }
    }

    private final String name;
    private final int failureThreshold; // 触发熔断的失败次数阈值
    private final int successThreshold; // 恢复的成功次数阈值
    private final long openTimeoutMs; // 熔断持续时间（毫秒）

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private volatile long openTimestamp = 0;

    // 统计数据
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger rejectedRequests = new AtomicInteger(0);

    /**
     * 创建熔断器
     * 
     * @param name             熔断器名称
     * @param failureThreshold 失败阈值
     * @param successThreshold 恢复阈值
     * @param openTimeoutMs    熔断超时时间
     */
    public CircuitBreaker(String name, int failureThreshold, int successThreshold, long openTimeoutMs) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.openTimeoutMs = openTimeoutMs;
    }

    /**
     * 使用默认配置创建熔断器
     */
    public CircuitBreaker(String name) {
        this(name, 5, 3, 10000); // 5次失败熔断，3次成功恢复，10秒超时
    }

    /**
     * 检查是否允许请求通过
     */
    public boolean allowRequest() {
        totalRequests.incrementAndGet();
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                // 检查是否超时，可以转为半开状态
                if (System.currentTimeMillis() - openTimestamp >= openTimeoutMs) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        log.info("🔄 熔断器 [{}] 状态: OPEN -> HALF_OPEN (尝试恢复)", name);
                        failureCount.set(0);
                        successCount.set(0);
                    }
                    return true;
                }
                rejectedRequests.incrementAndGet();
                return false;

            case HALF_OPEN:
                // 半开状态允许请求通过尝试
                return true;

            default:
                return true;
        }
    }

    /**
     * 记录请求成功
     */
    public void recordSuccess() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                failureCount.set(0); // 重置失败计数
                break;

            case HALF_OPEN:
                int successes = successCount.incrementAndGet();
                if (successes >= successThreshold) {
                    if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                        log.info("✅ 熔断器 [{}] 状态: HALF_OPEN -> CLOSED (恢复正常)", name);
                        reset();
                    }
                }
                break;

            default:
                break;
        }
    }

    /**
     * 记录请求失败
     */
    public void recordFailure() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                int failures = failureCount.incrementAndGet();
                if (failures >= failureThreshold) {
                    if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                        openTimestamp = System.currentTimeMillis();
                        log.warn("⚠️ 熔断器 [{}] 状态: CLOSED -> OPEN (触发熔断)", name);
                    }
                }
                break;

            case HALF_OPEN:
                if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                    openTimestamp = System.currentTimeMillis();
                    log.warn("⚠️ 熔断器 [{}] 状态: HALF_OPEN -> OPEN (恢复失败)", name);
                }
                break;

            default:
                break;
        }
    }

    /**
     * 执行受保护的操作
     */
    public <T> T execute(ProtectedAction<T> action, FallbackAction<T> fallback) {
        if (!allowRequest()) {
            log.debug("🚫 熔断器 [{}] 拒绝请求", name);
            return fallback.execute();
        }

        try {
            T result = action.execute();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            log.warn("❌ 熔断器 [{}] 请求失败: {}", name, e.getMessage());
            return fallback.execute();
        }
    }

    /**
     * 重置熔断器
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        openTimestamp = 0;
    }

    // Getters
    public String getName() {
        return name;
    }

    public State getState() {
        return state.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getRejectedRequests() {
        return rejectedRequests.get();
    }

    /**
     * 获取熔断器统计信息
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
                name,
                state.get(),
                failureCount.get(),
                successCount.get(),
                totalRequests.get(),
                rejectedRequests.get());
    }

    /**
     * 受保护的操作接口
     */
    @FunctionalInterface
    public interface ProtectedAction<T> {
        T execute() throws Exception;
    }

    /**
     * 降级操作接口
     */
    @FunctionalInterface
    public interface FallbackAction<T> {
        T execute();
    }

    /**
     * 熔断器统计信息
     */
    public static class CircuitBreakerStats {
        private final String name;
        private final State state;
        private final int failureCount;
        private final int successCount;
        private final int totalRequests;
        private final int rejectedRequests;

        public CircuitBreakerStats(String name, State state, int failureCount,
                int successCount, int totalRequests, int rejectedRequests) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.totalRequests = totalRequests;
            this.rejectedRequests = rejectedRequests;
        }

        public String getName() {
            return name;
        }

        public State getState() {
            return state;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getRejectedRequests() {
            return rejectedRequests;
        }

        public double getRejectionRate() {
            return totalRequests > 0 ? (double) rejectedRequests / totalRequests * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("%s{state=%s, failures=%d, total=%d, rejected=%d (%.1f%%)}",
                    name, state, failureCount, totalRequests, rejectedRequests, getRejectionRate());
        }
    }
}
