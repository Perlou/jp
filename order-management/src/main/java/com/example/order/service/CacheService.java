package com.example.order.service;

import com.example.order.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务 - 商品缓存
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String CACHE_KEY_PRODUCT = "product:";
    private static final long CACHE_TTL_SECONDS = 3600; // 1小时

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存商品
     */
    public void cacheProduct(Product product) {
        if (product == null || product.getId() == null)
            return;
        try {
            String key = CACHE_KEY_PRODUCT + product.getId();
            redisTemplate.opsForValue().set(key, product, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("商品已缓存: {}", key);
        } catch (Exception e) {
            log.warn("商品缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取商品缓存
     */
    public Product getProductCache(Long productId) {
        if (productId == null)
            return null;
        try {
            String key = CACHE_KEY_PRODUCT + productId;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Product) {
                log.debug("命中商品缓存: {}", key);
                return (Product) cached;
            }
        } catch (Exception e) {
            log.warn("商品缓存读取失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 删除商品缓存
     */
    public void evictProductCache(Long productId) {
        if (productId == null)
            return;
        try {
            String key = CACHE_KEY_PRODUCT + productId;
            redisTemplate.delete(key);
            log.debug("商品缓存已删除: {}", key);
        } catch (Exception e) {
            log.warn("删除商品缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除所有商品缓存
     */
    public void clearProductCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(CACHE_KEY_PRODUCT + "*"));
            log.info("所有商品缓存已清除");
        } catch (Exception e) {
            log.warn("清除商品缓存失败: {}", e.getMessage());
        }
    }
}
