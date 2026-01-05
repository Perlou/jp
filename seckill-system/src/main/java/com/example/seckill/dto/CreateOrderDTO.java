package com.example.seckill.dto;

import java.util.List;

/**
 * 创建订单请求 DTO
 */
public class CreateOrderDTO {

    private Long userId;
    private List<OrderItemDTO> items;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}