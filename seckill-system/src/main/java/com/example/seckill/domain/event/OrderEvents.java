package com.example.seckill.domain.event;

import java.math.BigDecimal;

/**
 * 订单相关的领域事件集合
 */
public class OrderEvents {

    private OrderEvents() {
    }

    // ==================== 订单创建事件 ====================

    /**
     * 订单创建事件
     */
    public static class OrderCreatedEvent extends DomainEvent {
        private final Long userId;
        private final Long productId;
        private final Integer quantity;
        private final BigDecimal totalAmount;

        public OrderCreatedEvent(String orderId, Long userId, Long productId,
                Integer quantity, BigDecimal totalAmount) {
            super(orderId);
            this.userId = userId;
            this.productId = productId;
            this.quantity = quantity;
            this.totalAmount = totalAmount;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    // ==================== 订单支付事件 ====================

    /**
     * 订单支付事件
     */
    public static class OrderPaidEvent extends DomainEvent {
        private final BigDecimal paidAmount;
        private final String paymentMethod;

        public OrderPaidEvent(String orderId, BigDecimal paidAmount, String paymentMethod) {
            super(orderId);
            this.paidAmount = paidAmount;
            this.paymentMethod = paymentMethod;
        }

        public BigDecimal getPaidAmount() {
            return paidAmount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }
    }

    // ==================== 订单取消事件 ====================

    /**
     * 订单取消事件
     */
    public static class OrderCancelledEvent extends DomainEvent {
        private final String reason;

        public OrderCancelledEvent(String orderId, String reason) {
            super(orderId);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    // ==================== 订单发货事件 ====================

    /**
     * 订单发货事件
     */
    public static class OrderShippedEvent extends DomainEvent {
        private final String trackingNumber;
        private final String carrier;

        public OrderShippedEvent(String orderId, String trackingNumber, String carrier) {
            super(orderId);
            this.trackingNumber = trackingNumber;
            this.carrier = carrier;
        }

        public String getTrackingNumber() {
            return trackingNumber;
        }

        public String getCarrier() {
            return carrier;
        }
    }

    // ==================== 订单完成事件 ====================

    /**
     * 订单完成事件
     */
    public static class OrderCompletedEvent extends DomainEvent {

        public OrderCompletedEvent(String orderId) {
            super(orderId);
        }
    }
}
