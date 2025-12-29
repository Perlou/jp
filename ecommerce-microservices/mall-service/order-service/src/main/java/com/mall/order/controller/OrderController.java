package com.mall.order.controller;

import com.mall.common.result.Result;
import com.mall.order.dto.CreateOrderDTO;
import com.mall.order.entity.Order;
import com.mall.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public Result<Order> findById(@PathVariable Long id) {
        return Result.success(orderService.findById(id));
    }

    @GetMapping("/no/{orderNo}")
    public Result<Order> findByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.findByOrderNo(orderNo));
    }

    @GetMapping("/user/{userId}")
    public Result<List<Order>> findByUserId(@PathVariable Long userId) {
        return Result.success(orderService.findByUserId(userId));
    }

    @GetMapping
    public Result<List<Order>> findAll() {
        return Result.success(orderService.findAll());
    }

    @PostMapping("/{id}/cancel")
    public Result<Order> cancel(@PathVariable Long id) {
        return Result.success(orderService.cancel(id));
    }
}
