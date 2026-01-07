package com.example.seckill.mq;

import com.example.seckill.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息生产者
 */
@Component
public class SeckillMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(SeckillMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public SeckillMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送秒杀订单消息
     */
    public void sendSeckillMessage(Long userId, Long goodsId) {
        SeckillMessage message = new SeckillMessage(userId, goodsId);
        log.info("发送秒杀消息: {}", message);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ROUTING_KEY,
                message);
    }
}
