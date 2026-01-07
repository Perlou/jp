package com.example.seckill.service;

import com.example.seckill.cache.MultiLevelCacheService;
import com.example.seckill.entity.Product;
import com.example.seckill.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性模型服务
 * 分布式存储架构 - 不同一致性级别实现
 * 
 * 一致性级别:
 * 1. 强一致性 (Strong Consistency) - 直接读数据库
 * 2. 最终一致性 (Eventual Consistency) - 优先读缓存
 * 3. 读己之写 (Read Your Writes) - 同一客户端能读到自己的写入
 */
@Service
public class ConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyService.class);

    private final ProductMapper productMapper;
    private final MultiLevelCacheService cacheService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 用于模拟"读己之写"的会话缓存 (生产环境应使用 Session 或 Token 关联)
    private final Map<String, String> sessionCache = new ConcurrentHashMap<>();

    private static final String CONSISTENCY_CACHE_PREFIX = "consistency:product:";

    public ConsistencyService(ProductMapper productMapper,
            MultiLevelCacheService cacheService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ========== 强一致性读取 (Strong Consistency) ==========

    /**
     * 强一致性读取
     * 特点: 始终返回最新数据，但性能较低
     * 适用场景: 金融交易、库存扣减、订单创建
     */
    public Product readStrong(Long productId) {
        log.info("[强一致性] 直接读取数据库 productId={}", productId);

        // 直接查询数据库，绕过所有缓存
        Product product = productMapper.selectById(productId);

        if (product != null) {
            log.info("[强一致性] 读取成功: {}", product.getName());
        }

        return product;
    }

    // ========== 最终一致性读取 (Eventual Consistency) ==========

    /**
     * 最终一致性读取
     * 特点: 优先读缓存，可能返回短暂的过期数据
     * 适用场景: 商品浏览、文章阅读、社交动态
     */
    public Product readEventual(Long productId) {
        log.info("[最终一致性] 优先读取缓存 productId={}", productId);

        String key = CONSISTENCY_CACHE_PREFIX + productId;

        // 使用多级缓存读取
        String json = cacheService.get(key, k -> {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                try {
                    return objectMapper.writeValueAsString(product);
                } catch (JsonProcessingException e) {
                    log.error("序列化失败", e);
                }
            }
            return null;
        });

        if (json != null) {
            try {
                return objectMapper.readValue(json, Product.class);
            } catch (JsonProcessingException e) {
                log.error("反序列化失败", e);
            }
        }

        return null;
    }

    // ========== 读己之写一致性 (Read Your Writes) ==========

    /**
     * 带会话的写入
     * 写入后将数据缓存到会话级别，确保同一客户端能立即读到
     */
    public void writeWithSession(String sessionId, Long productId, String newName) {
        log.info("[读己之写] 写入 sessionId={}, productId={}, newName={}",
                sessionId, productId, newName);

        // 1. 更新数据库
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setName(newName);
            productMapper.updateById(product);
        }

        // 2. 更新会话缓存 (确保读己之写)
        String sessionKey = sessionId + ":" + productId;
        try {
            sessionCache.put(sessionKey, objectMapper.writeValueAsString(product));
        } catch (JsonProcessingException e) {
            log.error("序列化失败", e);
        }

        // 3. 失效分布式缓存 (最终其他客户端也会看到更新)
        cacheService.invalidateWithDelay(CONSISTENCY_CACHE_PREFIX + productId, 500);
    }

    /**
     * 读己之写的读取
     * 优先从会话缓存读取，确保能读到自己刚写入的数据
     */
    public Product readYourWrites(String sessionId, Long productId) {
        log.info("[读己之写] 读取 sessionId={}, productId={}", sessionId, productId);

        String sessionKey = sessionId + ":" + productId;

        // 1. 优先从会话缓存读取
        String cached = sessionCache.get(sessionKey);
        if (cached != null) {
            log.info("[读己之写] 会话缓存命中");
            try {
                return objectMapper.readValue(cached, Product.class);
            } catch (JsonProcessingException e) {
                log.error("反序列化失败", e);
            }
        }

        // 2. 回退到最终一致性读取
        log.info("[读己之写] 会话缓存未命中，使用最终一致性读取");
        return readEventual(productId);
    }

    // ========== 一致性对比演示 ==========

    /**
     * 一致性模型对比信息
     */
    public Map<String, Object> getConsistencyModelsInfo() {
        return Map.of(
                "strongConsistency", Map.of(
                        "name", "强一致性 (Strong Consistency)",
                        "description", "所有节点在任意时刻看到相同的数据",
                        "tradeoff", "牺牲延迟和可用性",
                        "useCase", "金融交易、库存扣减",
                        "implementation", "直接读数据库，绕过缓存"),
                "eventualConsistency", Map.of(
                        "name", "最终一致性 (Eventual Consistency)",
                        "description", "允许短暂不一致，最终达到一致状态",
                        "tradeoff", "牺牲即时一致性换取高可用",
                        "useCase", "商品浏览、社交动态",
                        "implementation", "优先读缓存，异步更新"),
                "readYourWrites", Map.of(
                        "name", "读己之写一致性 (Read Your Writes)",
                        "description", "写操作后，同一客户端能立即读到自己的写入",
                        "tradeoff", "介于强一致和最终一致之间",
                        "useCase", "用户资料更新、购物车",
                        "implementation", "使用会话级缓存"),
                "causalConsistency", Map.of(
                        "name", "因果一致性 (Causal Consistency)",
                        "description", "有因果关系的操作保持顺序",
                        "tradeoff", "比强一致弱，比最终一致强",
                        "useCase", "评论回复、消息链",
                        "implementation", "使用向量时钟或依赖跟踪"));
    }

    /**
     * 清理会话缓存
     */
    public void clearSessionCache(String sessionId) {
        sessionCache.entrySet().removeIf(e -> e.getKey().startsWith(sessionId + ":"));
        log.info("[读己之写] 会话缓存已清理 sessionId={}", sessionId);
    }
}
