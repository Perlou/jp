package com.example.seckill.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 多级缓存服务
 * 
 * L1: Caffeine 本地缓存 (热点数据，毫秒级访问)
 * L2: Redis 分布式缓存 (共享数据，跨节点)
 * L3: 数据库 (持久化存储)
 * 
 * 读取流程:
 * 1. 查 L1 Caffeine，命中则返回
 * 2. 查 L2 Redis，命中则回填 L1 并返回
 * 3. 查 L3 数据库，命中则回填 L2、L1 并返回
 * 
 * 写入流程 (Cache-Aside):
 * 1. 更新数据库
 * 2. 删除 L2 Redis
 * 3. 删除 L1 Caffeine
 * 4. （可选）延迟双删保证一致性
 */
@Service
public class MultiLevelCacheService {

    private static final Logger log = LoggerFactory.getLogger(MultiLevelCacheService.class);

    private final StringRedisTemplate redisTemplate;

    // L1: Caffeine 本地缓存
    private final Cache<String, String> localCache;

    // 缓存统计
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong dbHits = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    // Redis 缓存前缀
    private static final String CACHE_PREFIX = "ml:cache:";

    // 默认过期时间
    private static final Duration L1_EXPIRE = Duration.ofMinutes(5);
    private static final Duration L2_EXPIRE = Duration.ofMinutes(30);

    public MultiLevelCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // 初始化 Caffeine 本地缓存
        this.localCache = Caffeine.newBuilder()
                .maximumSize(10_000) // 最大条目数
                .expireAfterWrite(L1_EXPIRE) // 写入后 5 分钟过期
                .recordStats() // 记录统计信息
                .build();

        log.info("多级缓存服务初始化完成 - L1: Caffeine(5分钟), L2: Redis(30分钟)");
    }

    /**
     * 多级缓存读取 (Cache-Aside 模式)
     * 
     * @param key      缓存键
     * @param dbLoader 数据库加载函数
     * @return 缓存值或数据库值
     */
    public String get(String key, Function<String, String> dbLoader) {
        totalRequests.incrementAndGet();
        String cacheKey = CACHE_PREFIX + key;

        // Step 1: 查询 L1 Caffeine
        String value = localCache.getIfPresent(cacheKey);
        if (value != null) {
            l1Hits.incrementAndGet();
            log.debug("[L1 命中] key={}", key);
            return value;
        }

        // Step 2: 查询 L2 Redis
        value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            l2Hits.incrementAndGet();
            // 回填 L1
            localCache.put(cacheKey, value);
            log.debug("[L2 命中] key={}, 回填 L1", key);
            return value;
        }

        // Step 3: 查询数据库
        value = dbLoader.apply(key);
        if (value != null) {
            dbHits.incrementAndGet();
            // 回填 L2 和 L1
            redisTemplate.opsForValue().set(cacheKey, value, L2_EXPIRE);
            localCache.put(cacheKey, value);
            log.debug("[DB 命中] key={}, 回填 L1 和 L2", key);
        }

        return value;
    }

    /**
     * 缓存写入并失效 (Cache-Aside 写模式)
     * 
     * 推荐流程:
     * 1. 业务层先更新数据库
     * 2. 调用此方法删除缓存
     * 
     * @param key 缓存键
     */
    public void invalidate(String key) {
        String cacheKey = CACHE_PREFIX + key;

        // 删除 L2 Redis
        redisTemplate.delete(cacheKey);

        // 删除 L1 Caffeine
        localCache.invalidate(cacheKey);

        log.info("[缓存失效] key={}", key);
    }

    /**
     * 延迟双删策略 (解决缓存数据库不一致问题)
     * 
     * 场景: 写请求与读请求并发时可能导致脏数据
     * 解决: 先删缓存 -> 更新DB -> 延迟再删缓存
     * 
     * @param key     缓存键
     * @param delayMs 延迟时间 (毫秒)
     */
    public void invalidateWithDelay(String key, long delayMs) {
        String cacheKey = CACHE_PREFIX + key;

        // 第一次删除
        invalidate(key);

        // 延迟双删
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                redisTemplate.delete(cacheKey);
                localCache.invalidate(cacheKey);
                log.info("[延迟双删] key={}, delay={}ms", key, delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 直接写入缓存 (Write-Through 模式的缓存写入部分)
     */
    public void put(String key, String value) {
        String cacheKey = CACHE_PREFIX + key;
        redisTemplate.opsForValue().set(cacheKey, value, L2_EXPIRE);
        localCache.put(cacheKey, value);
        log.debug("[缓存写入] key={}", key);
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        long total = totalRequests.get();
        long l1 = l1Hits.get();
        long l2 = l2Hits.get();
        long db = dbHits.get();

        stats.put("totalRequests", total);
        stats.put("l1Hits", l1);
        stats.put("l2Hits", l2);
        stats.put("dbHits", db);

        if (total > 0) {
            stats.put("l1HitRate", String.format("%.2f%%", l1 * 100.0 / total));
            stats.put("l2HitRate", String.format("%.2f%%", l2 * 100.0 / total));
            stats.put("overallHitRate", String.format("%.2f%%", (l1 + l2) * 100.0 / total));
        }

        // Caffeine 内部统计
        var caffeineStats = localCache.stats();
        stats.put("caffeine", Map.of(
                "hitCount", caffeineStats.hitCount(),
                "missCount", caffeineStats.missCount(),
                "evictionCount", caffeineStats.evictionCount(),
                "hitRate", String.format("%.2f%%", caffeineStats.hitRate() * 100)));

        stats.put("localCacheSize", localCache.estimatedSize());

        return stats;
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        localCache.invalidateAll();
        log.info("[缓存清空] L1 本地缓存已清空");
    }
}
