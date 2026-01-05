package com.example.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQConfig {

    // 秒杀队列
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    // 死信队列（用于处理失败的消息）
    public static final String SECKILL_DLQ = "seckill.order.dlq";
    public static final String SECKILL_DLX = "seckill.order.dlx";

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        return factory;
    }

    // ============ 秒杀队列配置 ============

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_DLX)
                .withArgument("x-dead-letter-routing-key", SECKILL_DLQ)
                .build();
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    @Bean
    public Binding seckillBinding(Queue seckillQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(SECKILL_ROUTING_KEY);
    }

    // ============ 死信队列配置 ============

    @Bean
    public Queue seckillDlq() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public DirectExchange seckillDlx() {
        return new DirectExchange(SECKILL_DLX);
    }

    @Bean
    public Binding seckillDlqBinding(Queue seckillDlq, DirectExchange seckillDlx) {
        return BindingBuilder.bind(seckillDlq).to(seckillDlx).with(SECKILL_DLQ);
    }
}
