package com.example.seckill.mq;

import com.example.seckill.config.RabbitMQConfig;
import com.example.seckill.entity.SeckillGoods;
import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.mapper.SeckillGoodsMapper;
import com.example.seckill.mapper.SeckillOrderMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 秒杀消息消费者
 * 异步处理订单创建
 */
@Component
public class SeckillMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillMessageConsumer.class);

    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;

    public SeckillMessageConsumer(SeckillGoodsMapper goodsMapper,
            SeckillOrderMapper orderMapper,
            StringRedisTemplate redisTemplate) {
        this.goodsMapper = goodsMapper;
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    @Transactional
    public void handleSeckillMessage(SeckillMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();

        log.info("收到秒杀消息: userId={}, goodsId={}", userId, goodsId);

        try {
            // 1. 检查是否已有订单（防止重复消费）
            SeckillOrder existing = orderMapper.selectByUserAndGoods(userId, goodsId);
            if (existing != null) {
                log.warn("订单已存在，跳过: userId={}, goodsId={}", userId, goodsId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 查询商品信息
            SeckillGoods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                log.error("商品不存在: goodsId={}", goodsId);
                handleOrderFail(userId, goodsId, channel, deliveryTag);
                return;
            }

            // 3. 扣减数据库库存（乐观锁）
            int rows = goodsMapper.deductStock(goodsId);
            if (rows == 0) {
                log.warn("数据库库存不足: goodsId={}", goodsId);
                handleOrderFail(userId, goodsId, channel, deliveryTag);
                return;
            }

            // 4. 创建秒杀订单
            SeckillOrder order = new SeckillOrder();
            order.setUserId(userId);
            order.setGoodsId(goodsId);
            order.setGoodsName(goods.getGoodsName());
            order.setSeckillPrice(goods.getSeckillPrice());
            order.setStatus(SeckillOrder.STATUS_SUCCESS);
            orderMapper.insert(order);

            // 5. 设置秒杀结果到 Redis（供前端轮询）
            String resultKey = "seckill:result:" + userId + ":" + goodsId;
            redisTemplate.opsForValue().set(resultKey, "SUCCESS");

            log.info("秒杀订单创建成功: userId={}, goodsId={}, orderId={}", userId, goodsId, order.getId());

            // 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理秒杀消息异常", e);
            // 消息重新入队
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 处理订单失败
     */
    private void handleOrderFail(Long userId, Long goodsId, Channel channel, long deliveryTag) throws IOException {
        // 恢复 Redis 库存
        String stockKey = "seckill:stock:" + goodsId;
        redisTemplate.opsForValue().increment(stockKey);

        // 从已购买集合移除
        String boughtKey = "seckill:bought:" + goodsId;
        redisTemplate.opsForSet().remove(boughtKey, userId.toString());

        // 设置失败结果
        String resultKey = "seckill:result:" + userId + ":" + goodsId;
        redisTemplate.opsForValue().set(resultKey, "FAIL");

        // 确认消息（不重试）
        channel.basicAck(deliveryTag, false);
    }
}
