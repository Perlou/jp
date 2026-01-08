package com.example.seckill.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 滑动窗口限流器
 * 
 * 核心思想：
 * 1. 将时间窗口划分为多个小的时间槽
 * 2. 每个槽记录该时间段内的请求数
 * 3. 窗口随时间滑动，过期的槽被清除
 * 
 * 优点：解决固定窗口的边界问题，限流更精确
 * 适用场景：需要精确限流的场景
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    /**
     * 每秒最大请求数
     */
    private final int limit;

    /**
     * 时间槽数量
     */
    private final int slotCount;

    /**
     * 每个时间槽的大小（毫秒）
     */
    private final int slotSizeMs;

    /**
     * 时间槽数组
     */
    private final AtomicInteger[] slots;

    /**
     * 窗口开始时间
     */
    private volatile long windowStart;

    /**
     * 同步锁
     */
    private final Object lock = new Object();

    /**
     * 创建滑动窗口限流器
     * 
     * @param limit        每秒最大请求数
     * @param windowSizeMs 窗口大小（毫秒）
     * @param slotCount    时间槽数量
     */
    public SlidingWindowRateLimiter(int limit, int windowSizeMs, int slotCount) {
        this.limit = limit;
        this.slotCount = slotCount;
        this.slotSizeMs = windowSizeMs / slotCount;
        this.slots = new AtomicInteger[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slots[i] = new AtomicInteger(0);
        }
        this.windowStart = System.currentTimeMillis();
    }

    /**
     * 创建默认配置的滑动窗口限流器（1秒窗口，10个槽）
     */
    public SlidingWindowRateLimiter(int limit) {
        this(limit, 1000, 10);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(int permits) {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            cleanExpiredSlots(now);

            // 计算当前窗口内的总请求数
            int total = 0;
            for (AtomicInteger slot : slots) {
                total += slot.get();
            }

            // 判断是否超过限制
            if (total + permits <= limit) {
                // 获取当前时间槽的索引
                int currentSlot = getCurrentSlotIndex(now);
                slots[currentSlot].addAndGet(permits);
                return true;
            }
            return false;
        }
    }

    /**
     * 清理过期的时间槽
     */
    private void cleanExpiredSlots(long now) {
        long elapsed = now - windowStart;

        if (elapsed >= slotSizeMs) {
            int slotsToClean = (int) (elapsed / slotSizeMs);
            slotsToClean = Math.min(slotsToClean, slotCount);

            // 滑动窗口
            windowStart += (long) slotsToClean * slotSizeMs;

            // 清理过期槽（简化实现：直接重置）
            for (int i = 0; i < slotsToClean; i++) {
                int idx = i % slotCount;
                slots[idx].set(0);
            }
        }
    }

    /**
     * 获取当前时间槽索引
     */
    private int getCurrentSlotIndex(long now) {
        return (int) ((now - windowStart) / slotSizeMs) % slotCount;
    }

    /**
     * 获取当前窗口内的请求数
     */
    public int getCurrentCount() {
        synchronized (lock) {
            cleanExpiredSlots(System.currentTimeMillis());
            int total = 0;
            for (AtomicInteger slot : slots) {
                total += slot.get();
            }
            return total;
        }
    }

    /**
     * 获取限流阈值
     */
    public int getLimit() {
        return limit;
    }

    @Override
    public String getName() {
        return "SlidingWindow";
    }

    @Override
    public RateLimiterStats getStats() {
        return new RateLimiterStats(getName(), limit, limit - getCurrentCount(), limit);
    }
}
