package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.dto.ProductRankDTO;
import com.example.seckill.dto.SalesReportDTO;
import com.example.seckill.entity.Order;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Optional<Order> findByOrderNo(String orderNo);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Order> findByUserId(Long userId);

    @Select("""
                SELECT o.*, u.username
                FROM orders o
                LEFT JOIN users u ON o.user_id = u.id
                WHERE o.id = #{id}
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "items", column = "id", many = @Many(select = "com.example.seckill.mapper.OrderItemMapper.findByOrderId"))
    })
    Order findOrderWithItems(Long id);

    /**
     * 按日销售统计
     */
    @Select("""
                SELECT DATE(created_at) as sale_date,
                       COUNT(*) as order_count,
                       SUM(total_amount) as total_sales
                FROM orders
                WHERE status >= 1 AND created_at BETWEEN #{start} AND #{end}
                GROUP BY DATE(created_at)
                ORDER BY sale_date
            """)
    List<SalesReportDTO> dailySalesReport(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 热销商品 TOP N
     */
    @Select("""
                SELECT oi.product_name as product_name,
                       SUM(oi.quantity) as total_quantity,
                       SUM(oi.unit_price * oi.quantity) as total_sales
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                WHERE o.status >= 1 AND o.created_at >= #{start}
                GROUP BY oi.product_id, oi.product_name
                ORDER BY total_quantity DESC
                LIMIT #{limit}
            """)
    List<ProductRankDTO> topProducts(@Param("start") LocalDateTime start,
            @Param("limit") int limit);
}
