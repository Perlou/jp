package com.example.seckill.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 领域事件处理器
 * 
 * 处理领域事件的响应逻辑：
 * - 更新缓存
 * - 发送通知
 * - 更新统计
 * - 触发后续流程
 */
@Component
public class DomainEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DomainEventHandler.class);

    private final StringRedisTemplate redisTemplate;

    public DomainEventHandler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== 订单事件处理 ====================

    @EventListener
    @Async
    public void handleOrderCreated(OrderEvents.OrderCreatedEvent event) {
        log.info("📥 处理订单创建事件: {}", event);

        // 1. 更新用户订单计数
        String userOrdersKey = "user:orders:count:" + event.getUserId();
        redisTemplate.opsForValue().increment(userOrdersKey);

        // 2. 更新商品销量统计
        String productSalesKey = "stats:product:sales:" + event.getProductId();
        redisTemplate.opsForValue().increment(productSalesKey, event.getQuantity());

        // 3. 更新热门商品排行
        redisTemplate.opsForZSet().incrementScore(
                "stats:top_products",
                event.getProductId().toString(),
                event.getQuantity());

        log.info("✅ 订单创建事件处理完成");
    }

    @EventListener
    @Async
    public void handleOrderPaid(OrderEvents.OrderPaidEvent event) {
        log.info("📥 处理订单支付事件: {}", event);

        // 1. 更新今日销售额
        String dailySalesKey = "stats:daily:sales:" + java.time.LocalDate.now();
        redisTemplate.opsForValue().increment(dailySalesKey,
                event.getPaidAmount().longValue());

        // 2. 可以触发发送支付成功通知等逻辑

        log.info("✅ 订单支付事件处理完成");
    }

    @EventListener
    @Async
    public void handleOrderCancelled(OrderEvents.OrderCancelledEvent event) {
        log.info("📥 处理订单取消事件: {}", event);

        // 1. 恢复库存（如果需要）
        // 2. 更新取消订单统计

        log.info("✅ 订单取消事件处理完成");
    }

    // ==================== 秒杀事件处理 ====================

    @EventListener
    @Async
    public void handleSeckillSucceeded(SeckillEvents.SeckillSucceededEvent event) {
        log.info("📥 处理秒杀成功事件: {}", event);

        // 1. 记录用户已秒杀标记
        String userSeckillKey = String.format("seckill:user:%d:goods:%d",
                event.getUserId(), event.getGoodsId());
        redisTemplate.opsForValue().set(userSeckillKey, "1");

        // 2. 更新秒杀统计
        String seckillStatsKey = "stats:seckill:" + event.getGoodsId();
        redisTemplate.opsForHash().increment(seckillStatsKey, "success_count", 1);

        log.info("✅ 秒杀成功事件处理完成");
    }

    @EventListener
    @Async
    public void handleSeckillFailed(SeckillEvents.SeckillFailedEvent event) {
        log.info("📥 处理秒杀失败事件: {}", event);

        // 更新秒杀失败统计
        String seckillStatsKey = "stats:seckill:" + event.getGoodsId();
        redisTemplate.opsForHash().increment(seckillStatsKey, "fail_count", 1);

        log.info("✅ 秒杀失败事件处理完成");
    }

    @EventListener
    @Async
    public void handleStockChanged(SeckillEvents.StockChangedEvent event) {
        log.info("📥 处理库存变更事件: {}", event);

        // 同步库存到缓存
        String stockKey = "seckill:stock:" + event.getGoodsId();
        redisTemplate.opsForValue().set(stockKey, event.getCurrentStock().toString());

        log.info("✅ 库存变更事件处理完成");
    }
}
