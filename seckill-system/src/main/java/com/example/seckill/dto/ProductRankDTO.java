package com.example.seckill.dto;

import java.math.BigDecimal;

/**
 * 热销商品排行 DTO
 */
public class ProductRankDTO {

    private String productName;
    private Long totalQuantity;
    private BigDecimal totalSales;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
}
