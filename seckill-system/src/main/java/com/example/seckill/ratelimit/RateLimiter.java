package com.example.seckill.ratelimit;

/**
 * 限流器接口
 * 
 * 定义限流器的通用行为
 */
public interface RateLimiter {

    /**
     * 尝试获取一个许可
     * 
     * @return true 如果获取成功，false 如果被限流
     */
    boolean tryAcquire();

    /**
     * 尝试获取指定数量的许可
     * 
     * @param permits 需要的许可数量
     * @return true 如果获取成功
     */
    boolean tryAcquire(int permits);

    /**
     * 获取限流器名称
     */
    String getName();

    /**
     * 获取限流器统计信息
     */
    RateLimiterStats getStats();
}
