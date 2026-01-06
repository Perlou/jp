package com.example.seckill.domain.event;

import java.math.BigDecimal;

/**
 * 秒杀相关的领域事件集合
 */
public class SeckillEvents {

    private SeckillEvents() {
    }

    // ==================== 秒杀开始事件 ====================

    /**
     * 秒杀活动开始事件
     */
    public static class SeckillStartedEvent extends DomainEvent {
        private final Long goodsId;
        private final Integer stock;
        private final BigDecimal seckillPrice;

        public SeckillStartedEvent(String seckillId, Long goodsId, Integer stock, BigDecimal seckillPrice) {
            super(seckillId);
            this.goodsId = goodsId;
            this.stock = stock;
            this.seckillPrice = seckillPrice;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public Integer getStock() {
            return stock;
        }

        public BigDecimal getSeckillPrice() {
            return seckillPrice;
        }
    }

    // ==================== 秒杀请求事件 ====================

    /**
     * 用户发起秒杀请求事件
     */
    public static class SeckillRequestedEvent extends DomainEvent {
        private final Long userId;
        private final Long goodsId;

        public SeckillRequestedEvent(String requestId, Long userId, Long goodsId) {
            super(requestId);
            this.userId = userId;
            this.goodsId = goodsId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getGoodsId() {
            return goodsId;
        }
    }

    // ==================== 秒杀成功事件 ====================

    /**
     * 秒杀成功事件（库存扣减成功）
     */
    public static class SeckillSucceededEvent extends DomainEvent {
        private final Long userId;
        private final Long goodsId;
        private final Long orderId;

        public SeckillSucceededEvent(String seckillId, Long userId, Long goodsId, Long orderId) {
            super(seckillId);
            this.userId = userId;
            this.goodsId = goodsId;
            this.orderId = orderId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public Long getOrderId() {
            return orderId;
        }
    }

    // ==================== 秒杀失败事件 ====================

    /**
     * 秒杀失败事件
     */
    public static class SeckillFailedEvent extends DomainEvent {
        private final Long userId;
        private final Long goodsId;
        private final String reason;

        public SeckillFailedEvent(String seckillId, Long userId, Long goodsId, String reason) {
            super(seckillId);
            this.userId = userId;
            this.goodsId = goodsId;
            this.reason = reason;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public String getReason() {
            return reason;
        }
    }

    // ==================== 库存变更事件 ====================

    /**
     * 库存变更事件
     */
    public static class StockChangedEvent extends DomainEvent {
        private final Long goodsId;
        private final Integer previousStock;
        private final Integer currentStock;
        private final String changeType; // DEDUCT, RESTORE

        public StockChangedEvent(String goodsId, Integer previousStock, Integer currentStock, String changeType) {
            super(goodsId);
            this.goodsId = Long.parseLong(goodsId);
            this.previousStock = previousStock;
            this.currentStock = currentStock;
            this.changeType = changeType;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public Integer getPreviousStock() {
            return previousStock;
        }

        public Integer getCurrentStock() {
            return currentStock;
        }

        public String getChangeType() {
            return changeType;
        }
    }
}
