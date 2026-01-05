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
    // TODO
    private static final Logger log = LoggerFactory.getLogger(MultiLevelCacheService.class);

    private final StringRedisTemplate redisTemplate;

    private final Cache<String, String> localCache;

    // 缓存统计
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong dbHits = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    private static final String CACHE_PREFIX = "ml:cache:";

    private static final Duration L1_EXPIRE = Duration.ofMinutes(5);
    private static final Duration L2_EXPIRE = Duration.ofMinutes(30);

    public MultiLevelCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.localCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(L1_EXPIRE)
                .recordStats()
                .build();
        log.info("多级缓存服务初始化完成 - L1: Caffeine(5分钟), L2: Redis(30分钟)");
    }

    // public String get()

}
