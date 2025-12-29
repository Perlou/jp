package com.mall.inventory.entity;

import com.mall.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer lockedStock = 0;

    public Integer getAvailableStock() {
        return stock - lockedStock;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getLockedStock() {
        return lockedStock;
    }

    public void setLockedStock(Integer lockedStock) {
        this.lockedStock = lockedStock;
    }
}
