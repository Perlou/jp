package com.example.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售报表 DTO
 */
public class SalesReportDTO {

    private LocalDate date;
    private Integer orderCount;
    private BigDecimal totalSales;

    public SalesReportDTO() {
    }

    public SalesReportDTO(LocalDate date, Integer orderCount, BigDecimal totalSales) {
        this.date = date;
        this.orderCount = orderCount;
        this.totalSales = totalSales;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
}
