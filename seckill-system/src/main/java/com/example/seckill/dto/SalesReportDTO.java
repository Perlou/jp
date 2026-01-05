package com.example.seckill.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售报表 DTO
 */
public class SalesReportDTO {

    private LocalDate saleDate;
    private Long orderCount;
    private BigDecimal totalSales;

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
}
