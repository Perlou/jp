package com.example.seckill.ratelimit;

/**
 * 限流器统计信息
 */
public class RateLimiterStats {

    private final String name;
    private final long capacity;
    private final long available;
    private final double rate;

    public RateLimiterStats(String name, long capacity, long available, double rate) {
        this.name = name;
        this.capacity = capacity;
        this.available = available;
        this.rate = rate;
    }

    public String getName() {
        return name;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getAvailable() {
        return available;
    }

    public double getRate() {
        return rate;
    }

    public double getUsagePercent() {
        return capacity > 0 ? (double) (capacity - available) / capacity * 100 : 0;
    }

    @Override
    public String toString() {
        return String.format("%s{capacity=%d, available=%d, rate=%.2f/s, usage=%.1f%%}",
                name, capacity, available, rate, getUsagePercent());
    }
}
