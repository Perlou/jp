package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.dto.ProductRankDTO;
import com.example.order.dto.SalesReportDTO;
import com.example.order.entity.Order;
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

        /**
         * 查询订单带明细 (一对多关联)
         */
        @Select("SELECT o.*, u.username FROM orders o " +
                        "LEFT JOIN users u ON o.user_id = u.id " +
                        "WHERE o.id = #{id}")
        @Results(id = "orderWithItems", value = {
                        @Result(property = "id", column = "id", id = true),
                        @Result(property = "orderNo", column = "order_no"),
                        @Result(property = "userId", column = "user_id"),
                        @Result(property = "totalAmount", column = "total_amount"),
                        @Result(property = "status", column = "status"),
                        @Result(property = "paymentTime", column = "payment_time"),
                        @Result(property = "createdAt", column = "created_at"),
                        @Result(property = "username", column = "username"),
                        @Result(property = "items", column = "id", many = @Many(select = "com.example.order.mapper.OrderItemMapper.findByOrderId"))
        })
        Order findOrderWithItems(Long id);

        /**
         * 销售报表 - 按日统计
         */
        @Select("""
                        SELECT DATE(created_at) as date,
                               COUNT(*) as orderCount,
                               SUM(total_amount) as totalSales
                        FROM orders
                        WHERE status IN (1, 2, 3)
                          AND created_at >= #{startDate}
                          AND created_at < #{endDate}
                        GROUP BY DATE(created_at)
                        ORDER BY date
                        """)
        List<SalesReportDTO> dailySalesReport(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * 热销商品 TOP N
         */
        @Select("""
                        SELECT p.id, p.name, p.price,
                               SUM(oi.quantity) as totalSold
                        FROM order_items oi
                        JOIN products p ON oi.product_id = p.id
                        JOIN orders o ON oi.order_id = o.id
                        WHERE o.status IN (1, 2, 3)
                          AND o.created_at >= #{startDate}
                        GROUP BY p.id, p.name, p.price
                        ORDER BY totalSold DESC
                        LIMIT #{limit}
                        """)
        List<ProductRankDTO> topProducts(@Param("startDate") LocalDateTime startDate,
                        @Param("limit") int limit);
}
