package com.example.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.order.common.Result;
import com.example.order.dto.CreateOrderDTO;
import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单
     */
    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    /**
     * 查询订单详情 (带订单项)
     */
    @GetMapping("/{id}")
    public Result<Order> findById(@PathVariable Long id) {
        return Result.success(orderService.findOrderWithItems(id));
    }

    /**
     * 根据订单号查询
     */
    @GetMapping("/no/{orderNo}")
    public Result<Order> findByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.findByOrderNo(orderNo));
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<Order>> findByUserId(@PathVariable Long userId) {
        return Result.success(orderService.findByUserId(userId));
    }

    /**
     * 分页查询所有订单
     */
    @GetMapping("/page")
    public Result<Page<Order>> findPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.findPage(pageNum, pageSize));
    }

    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    public Result<Order> pay(@PathVariable Long id) {
        return Result.success(orderService.payOrder(id));
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result<Order> cancel(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(id));
    }
}
