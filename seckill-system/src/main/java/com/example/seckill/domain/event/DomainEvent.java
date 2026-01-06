package com.example.seckill.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 所有领域事件的抽象基类，包含事件通用属性：
 * - 事件ID
 * - 事件发生时间
 * - 聚合根ID
 * - 事件类型
 */
public abstract class DomainEvent {

    /**
     * 事件唯一标识
     */
    private final String eventId;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredAt;

    /**
     * 聚合根ID
     */
    private final String aggregateId;

    /**
     * 事件版本（用于事件溯源）
     */
    private final int version;

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.version = 1;
    }

    protected DomainEvent(String aggregateId, int version) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.version = version;
    }

    /**
     * 获取事件类型名称
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', aggregateId='%s', occurredAt=%s}",
                getEventType(), eventId, aggregateId, occurredAt);
    }
}
