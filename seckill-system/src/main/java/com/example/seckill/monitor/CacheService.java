package com.example.seckill.monitor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存服务 - 多级缓存实现
 * 
 * 演示缓存架构：
 * 1. 本地缓存 (Caffeine) - 微秒级访问
 * 2. 分布式缓存 (Redis) - 毫秒级访问
 * 3. 数据库 - 毫秒级访问
 * 
 * 缓存问题解决方案：
 * - 缓存穿透：布隆过滤器 / 空值缓存
 * - 缓存击穿：互斥锁 / 逻辑过期
 * - 缓存雪崩：随机过期时间 / 多级缓存
 */
@Service
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    // Caffeine 本地缓存 - 存储商品信息
    private final Cache<String, String> localCache;

    // 模拟数据库存储
    private final Map<String, String> mockDatabase = new ConcurrentHashMap<>();

    // 空值缓存标记 (解决缓存穿透)
    private static final String NULL_VALUE = "__NULL__";

    public CacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // 初始化 Caffeine 缓存
        // 最佳实践配置
        this.localCache = Caffeine.newBuilder()
                .maximumSize(10_000) // 最大缓存条目数
                .expireAfterWrite(Duration.ofMinutes(5)) // 写入后 5 分钟过期
                .expireAfterAccess(Duration.ofMinutes(2))// 2 分钟未访问过期
                .recordStats() // 记录统计信息
                .build();

        // 初始化模拟数据
        initMockData();
    }

    private void initMockData() {
        mockDatabase.put("goods:1", "{\"id\":1,\"name\":\"iPhone 15\",\"price\":5999}");
        mockDatabase.put("goods:2", "{\"id\":2,\"name\":\"MacBook Pro\",\"price\":12999}");
        mockDatabase.put("goods:3", "{\"id\":3,\"name\":\"iPad Air\",\"price\":4799}");
    }

    /**
     * 多级缓存读取示例 (Cache-Aside 模式)
     * 
     * 读流程：
     * 1. 先查本地缓存 (Caffeine)
     * 2. 未命中则查 Redis
     * 3. 仍未命中则查数据库
     * 4. 查到后回填到所有缓存层
     */
    public Map<String, Object> getWithMultiLevelCache(String key) {
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.nanoTime();
        String value = null;
        String hitLevel = null;

        // 1. 查本地缓存
        value = localCache.getIfPresent(key);
        if (value != null) {
            hitLevel = "LOCAL_CACHE (Caffeine)";
            if (NULL_VALUE.equals(value)) {
                value = null; // 空值缓存，返回 null
                hitLevel += " [空值缓存]";
            }
        } else {
            // 2. 查 Redis
            try {
                value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    hitLevel = "REDIS";
                    // 回填本地缓存
                    localCache.put(key, value);
                }
            } catch (Exception e) {
                result.put("redis_error", e.getMessage());
            }
        }

        // 3. 查数据库
        if (value == null && hitLevel == null) {
            value = mockDatabase.get(key);
            if (value != null) {
                hitLevel = "DATABASE (模拟)";
                // 回填到所有缓存层
                localCache.put(key, value);
                try {
                    redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
                } catch (Exception e) {
                    // Redis 不可用时降级
                }
            } else {
                hitLevel = "NOT_FOUND";
                // 缓存空值，防止缓存穿透
                localCache.put(key, NULL_VALUE);
            }
        }

        long duration = System.nanoTime() - startTime;

        result.put("key", key);
        result.put("value", value);
        result.put("hit_level", hitLevel);
        result.put("duration_ns", duration);
        result.put("duration_readable", formatDuration(duration));

        return result;
    }

    /**
     * 获取缓存统计信息
     * 
     * 关键指标：
     * - hitRate: 命中率 (越高越好)
     * - missRate: 未命中率
     * - evictionCount: 驱逐次数 (缓存满时移除的条目)
     */
    public Map<String, Object> getCacheStats() {
        CacheStats stats = localCache.stats();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cache_type", "Caffeine");
        result.put("estimated_size", localCache.estimatedSize());
        result.put("hit_count", stats.hitCount());
        result.put("miss_count", stats.missCount());
        result.put("hit_rate", String.format("%.2f%%", stats.hitRate() * 100));
        result.put("miss_rate", String.format("%.2f%%", stats.missRate() * 100));
        result.put("load_count", stats.loadCount());
        result.put("eviction_count", stats.evictionCount());
        result.put("average_load_penalty_ms", String.format("%.2f", stats.averageLoadPenalty() / 1_000_000));

        // 性能建议
        if (stats.hitRate() < 0.8 && stats.requestCount() > 100) {
            result.put("suggestion", "⚠️ 缓存命中率低于 80%，建议检查缓存策略");
        } else if (stats.hitRate() >= 0.9) {
            result.put("suggestion", "✅ 缓存命中率良好 (≥90%)");
        }

        return result;
    }

    /**
     * 压力测试：演示缓存效果
     */
    public Map<String, Object> runCacheStressTest(int iterations) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] keys = { "goods:1", "goods:2", "goods:3", "goods:999" };
        Random random = new Random();

        // 清空缓存统计
        localCache.invalidateAll();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            String key = keys[random.nextInt(keys.length)];
            getWithMultiLevelCache(key);
        }
        long duration = System.currentTimeMillis() - startTime;

        result.put("iterations", iterations);
        result.put("total_time_ms", duration);
        result.put("avg_time_per_request_ms", String.format("%.3f", (double) duration / iterations));
        result.put("requests_per_second", (int) (iterations * 1000.0 / duration));
        result.put("cache_stats", getCacheStats());

        return result;
    }

    /**
     * 手动清除缓存
     */
    public Map<String, Object> clearCache(String key) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (key == null || key.isEmpty()) {
            localCache.invalidateAll();
            result.put("action", "清除所有本地缓存");
        } else {
            localCache.invalidate(key);
            redisTemplate.delete(key);
            result.put("action", "清除缓存: " + key);
        }
        result.put("current_size", localCache.estimatedSize());
        return result;
    }

    private String formatDuration(long nanos) {
        if (nanos < 1_000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2f μs", nanos / 1000.0);
        } else {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        }
    }
}
