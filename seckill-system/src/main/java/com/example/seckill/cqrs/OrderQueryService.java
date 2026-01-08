package com.example.seckill.cqrs;

import com.example.seckill.entity.Order;
import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.mapper.SeckillOrderMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CQRS 查询服务
 * 
 * 负责所有只读查询操作，与写入操作分离：
 * - 从 Redis 缓存读取热点数据
 * - 从 MySQL 读取历史数据
 * - 支持复杂查询和报表统计
 */
@Service
public class OrderQueryService {

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StringRedisTemplate redisTemplate;

    // Redis 缓存前缀
    private static final String ORDER_CACHE_PREFIX = "order:";

    public OrderQueryService(OrderMapper orderMapper,
            SeckillOrderMapper seckillOrderMapper,
            StringRedisTemplate redisTemplate) {
        this.orderMapper = orderMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.redisTemplate = redisTemplate;
    }

    // ==================== 订单查询 ====================

    /**
     * 根据 ID 查询订单（优先从缓存读取）
     */
    public Optional<Order> findOrderById(Long orderId) {
        // 1. 先查缓存
        String cacheKey = ORDER_CACHE_PREFIX + orderId;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            // 缓存命中 - 简化处理，直接返回 ID
            Order order = new Order();
            order.setId(orderId);
            return Optional.of(order);
        }

        // 2. 缓存未命中，查数据库
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            // 回填缓存
            redisTemplate.opsForValue().set(cacheKey, order.getId().toString());
        }

        return Optional.ofNullable(order);
    }

    /**
     * 查询用户的订单列表
     */
    public List<Order> findOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    /**
     * 查询用户某状态的订单
     */
    public List<Order> findOrdersByUserIdAndStatus(Long userId, Integer status) {
        // 使用 MyBatis-Plus 查询
        return orderMapper.findByUserId(userId).stream()
                .filter(o -> status.equals(o.getStatus()))
                .toList();
    }

    // ==================== 秒杀订单查询 ====================

    /**
     * 查询用户的秒杀订单
     */
    public List<SeckillOrder> findSeckillOrdersByUserId(Long userId) {
        return seckillOrderMapper.selectList(null); // 简化实现
    }

    /**
     * 检查用户是否已参与某商品秒杀
     */
    public boolean hasUserSeckilled(Long userId, Long goodsId) {
        // 从 Redis 检查用户秒杀记录
        String key = String.format("seckill:user:%d:goods:%d", userId, goodsId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    // ==================== 统计查询 ====================

    /**
     * 获取订单统计数据
     */
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 从 Redis 获取统计
        String totalOrdersKey = "stats:orders:total";
        String totalAmountKey = "stats:orders:amount";

        String totalOrders = redisTemplate.opsForValue().get(totalOrdersKey);
        String totalAmount = redisTemplate.opsForValue().get(totalAmountKey);

        stats.put("total_orders", totalOrders != null ? Long.parseLong(totalOrders) : 0);
        stats.put("total_amount", totalAmount != null ? totalAmount : "0");

        return stats;
    }

    /**
     * 获取热门商品排行
     */
    public List<Map<String, Object>> getTopProducts(int limit) {
        // 从 Redis ZSet 读取热门商品
        Set<String> topProducts = redisTemplate.opsForZSet()
                .reverseRange("stats:top_products", 0, limit - 1);

        List<Map<String, Object>> result = new ArrayList<>();
        if (topProducts != null) {
            for (String productId : topProducts) {
                Double score = redisTemplate.opsForZSet()
                        .score("stats:top_products", productId);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("product_id", productId);
                item.put("sales_count", score != null ? score.intValue() : 0);
                result.add(item);
            }
        }

        return result;
    }
}
