package com.example.order.dto;

import java.math.BigDecimal;

/**
 * 热销商品排行 DTO
 */
public class ProductRankDTO {

    private Long id;
    private String name;
    private BigDecimal price;
    private Long totalSold;

    public ProductRankDTO() {
    }

    public ProductRankDTO(Long id, String name, BigDecimal price, Long totalSold) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.totalSold = totalSold;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getTotalSold() {
        return totalSold;
    }

    public void setTotalSold(Long totalSold) {
        this.totalSold = totalSold;
    }
}
