package com.example.seckill.cqrs;

import com.example.seckill.domain.event.DomainEventPublisher;
import com.example.seckill.domain.event.OrderEvents.*;
import com.example.seckill.entity.Order;
import com.example.seckill.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CQRS 命令服务
 * 
 * 负责所有写入操作，与查询操作分离：
 * - 处理订单创建、支付、取消等命令
 * - 发布领域事件
 * - 保证数据一致性
 */
@Service
public class OrderCommandService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);

    private final OrderMapper orderMapper;
    private final DomainEventPublisher eventPublisher;

    public OrderCommandService(OrderMapper orderMapper,
            DomainEventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
    }

    // ==================== 创建订单命令 ====================

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Long userId, BigDecimal totalAmount) {
        log.info("📝 处理创建订单命令: userId={}, amount={}", userId, totalAmount);

        // 1. 创建订单实体
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // 2. 持久化
        orderMapper.insert(order);

        // 3. 发布领域事件
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId().toString(),
                userId,
                null, // Order 支持多商品，productId 在 OrderItem 中
                null,
                totalAmount);
        eventPublisher.publish(event);

        log.info("✅ 订单创建成功: orderId={}", order.getId());
        return order;
    }

    // ==================== 支付订单命令 ====================

    /**
     * 支付订单
     */
    @Transactional
    public Order payOrder(Long orderId, String paymentMethod) {
        log.info("💰 处理支付订单命令: orderId={}, paymentMethod={}", orderId, paymentMethod);

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        // 2. 验证状态
        if (order.getStatus() != Order.STATUS_PENDING) {
            throw new IllegalStateException("订单状态不允许支付: " + order.getStatusName());
        }

        // 3. 更新状态
        order.setStatus(Order.STATUS_PAID);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 发布领域事件
        OrderPaidEvent event = new OrderPaidEvent(
                orderId.toString(),
                order.getTotalAmount(),
                paymentMethod);
        eventPublisher.publish(event);

        log.info("✅ 订单支付成功: orderId={}", orderId);
        return order;
    }

    // ==================== 取消订单命令 ====================

    /**
     * 取消订单
     */
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        log.info("❌ 处理取消订单命令: orderId={}, reason={}", orderId, reason);

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        // 2. 验证状态（只有待支付订单可取消）
        if (order.getStatus() != Order.STATUS_PENDING) {
            throw new IllegalStateException("订单状态不允许取消: " + order.getStatusName());
        }

        // 3. 更新状态
        order.setStatus(Order.STATUS_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 发布领域事件
        OrderCancelledEvent event = new OrderCancelledEvent(orderId.toString(), reason);
        eventPublisher.publish(event);

        log.info("✅ 订单取消成功: orderId={}", orderId);
        return order;
    }

    // ==================== 发货命令 ====================

    /**
     * 订单发货
     */
    @Transactional
    public Order shipOrder(Long orderId, String trackingNumber, String carrier) {
        log.info("🚚 处理发货命令: orderId={}, trackingNumber={}", orderId, trackingNumber);

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        // 2. 验证状态
        if (order.getStatus() != Order.STATUS_PAID) {
            throw new IllegalStateException("订单状态不允许发货: " + order.getStatusName());
        }

        // 3. 更新状态
        order.setStatus(Order.STATUS_SHIPPED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 发布领域事件
        OrderShippedEvent event = new OrderShippedEvent(orderId.toString(), trackingNumber, carrier);
        eventPublisher.publish(event);

        log.info("✅ 订单发货成功: orderId={}", orderId);
        return order;
    }

    // ==================== 完成订单命令 ====================

    /**
     * 完成订单（确认收货）
     */
    @Transactional
    public Order completeOrder(Long orderId) {
        log.info("🎉 处理完成订单命令: orderId={}", orderId);

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        // 2. 验证状态
        if (order.getStatus() != Order.STATUS_SHIPPED) {
            throw new IllegalStateException("订单状态不允许完成: " + order.getStatusName());
        }

        // 3. 更新状态
        order.setStatus(Order.STATUS_COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 发布领域事件
        OrderCompletedEvent event = new OrderCompletedEvent(orderId.toString());
        eventPublisher.publish(event);

        log.info("✅ 订单完成: orderId={}", orderId);
        return order;
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
