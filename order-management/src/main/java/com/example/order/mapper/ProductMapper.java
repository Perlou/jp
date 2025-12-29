package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 原子操作扣减库存 (推荐方式)
     * 使用 SQL 条件判断确保库存充足
     */
    @Update("UPDATE products SET stock = stock - #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 恢复库存 (订单取消时)
     */
    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
