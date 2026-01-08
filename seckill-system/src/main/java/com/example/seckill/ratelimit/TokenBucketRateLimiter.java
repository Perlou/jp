package com.example.seckill.ratelimit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限流器
 * 
 * 核心思想：
 * 1. 以固定速率向桶中添加令牌
 * 2. 请求需要获取令牌才能被处理
 * 3. 桶有容量限制，满了之后新令牌被丢弃
 * 4. 允许一定程度的突发流量
 * 
 * 适用场景：需要允许突发流量的 API 限流
 */
public class TokenBucketRateLimiter implements RateLimiter {

    /**
     * 桶容量（允许的最大突发量）
     */
    private final long capacity;

    /**
     * 令牌生成速率（每秒生成的令牌数）
     */
    private final double refillRate;

    /**
     * 当前令牌数
     */
    private final AtomicLong tokens;

    /**
     * 上次填充时间（纳秒）
     */
    private volatile long lastRefillNanoTime;

    /**
     * 同步锁
     */
    private final Object lock = new Object();

    /**
     * 创建令牌桶限流器
     * 
     * @param capacity   桶容量
     * @param refillRate 每秒生成的令牌数
     */
    public TokenBucketRateLimiter(long capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillNanoTime = System.nanoTime();
    }

    /**
     * 尝试获取一个令牌
     */
    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * 尝试获取指定数量的令牌
     */
    @Override
    public boolean tryAcquire(int permits) {
        synchronized (lock) {
            refill();
            long currentTokens = tokens.get();
            if (currentTokens >= permits) {
                tokens.addAndGet(-permits);
                return true;
            }
            return false;
        }
    }

    /**
     * 阻塞等待获取令牌
     */
    public void acquire() throws InterruptedException {
        acquire(1);
    }

    /**
     * 阻塞等待获取指定数量的令牌
     */
    public void acquire(int permits) throws InterruptedException {
        while (true) {
            synchronized (lock) {
                refill();
                long currentTokens = tokens.get();
                if (currentTokens >= permits) {
                    tokens.addAndGet(-permits);
                    return;
                }
                // 计算需要等待的时间
                double tokensNeeded = permits - currentTokens;
                long waitMs = (long) Math.ceil(tokensNeeded / refillRate * 1000);
                lock.wait(Math.max(1, waitMs));
            }
        }
    }

    /**
     * 填充令牌
     */
    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanoTime) / 1_000_000_000.0;
        long tokensToAdd = (long) (elapsedSeconds * refillRate);

        if (tokensToAdd > 0) {
            long newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
            tokens.set(newTokens);
            lastRefillNanoTime = now;
        }
    }

    /**
     * 获取当前令牌数
     */
    public long getAvailableTokens() {
        synchronized (lock) {
            refill();
            return tokens.get();
        }
    }

    /**
     * 获取桶容量
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * 获取填充速率
     */
    public double getRefillRate() {
        return refillRate;
    }

    @Override
    public String getName() {
        return "TokenBucket";
    }

    @Override
    public RateLimiterStats getStats() {
        return new RateLimiterStats(getName(), capacity, getAvailableTokens(), refillRate);
    }
}
