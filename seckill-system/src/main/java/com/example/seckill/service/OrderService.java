package com.example.seckill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.seckill.common.SeckillException;
import com.example.seckill.dto.CreateOrderDTO;
import com.example.seckill.dto.OrderItemDTO;
import com.example.seckill.entity.Order;
import com.example.seckill.entity.OrderItem;
import com.example.seckill.entity.Product;
import com.example.seckill.mapper.OrderItemMapper;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 订单服务
 * 
 * 核心功能：
 * - 创建订单 (支持多商品)
 * - 支付订单
 * - 取消订单 (恢复库存)
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
            ProductMapper productMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    /**
     * 创建订单 - 核心业务逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderDTO dto) {
        log.info("创建订单: userId={}, items={}", dto.getUserId(), dto.getItems().size());

        // 1. 验证用户
        if (userMapper.selectById(dto.getUserId()) == null) {
            throw new SeckillException("用户不存在");
        }

        // 2. 生成订单号
        String orderNo = generateOrderNo();

        // 3. 计算总金额并扣减库存
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO itemDTO : dto.getItems()) {
            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null || product.getStatus() != Product.STATUS_ON) {
                throw new SeckillException("商品不存在或已下架");
            }

            // 扣减库存 (原子操作，防止超卖)
            int rows = productMapper.deductStock(itemDTO.getProductId(), itemDTO.getQuantity());
            if (rows == 0) {
                throw new SeckillException("商品 [" + product.getName() + "] 库存不足");
            }

            // 计算金额
            BigDecimal amount = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(amount);

            // 构建订单项
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(itemDTO.getQuantity());
            orderItems.add(item);
        }

        // 4. 保存订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(dto.getUserId());
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.STATUS_PENDING);
        orderMapper.insert(order);

        // 5. 保存订单项
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        log.info("订单创建成功: orderNo={}, totalAmount={}", orderNo, totalAmount);

        order.setItems(orderItems);
        return order;
    }

    /**
     * 支付订单
     */
    @Transactional
    public Order payOrder(Long orderId) {
        Order order = findById(orderId);
        if (order.getStatus() != Order.STATUS_PENDING) {
            throw new SeckillException("订单状态不允许支付");
        }
        order.setStatus(Order.STATUS_PAID);
        order.setPaymentTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单支付成功: {}", order.getOrderNo());
        return order;
    }

    /**
     * 取消订单 (恢复库存)
     */
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrder(Long orderId) {
        Order order = findById(orderId);
        if (order.getStatus() != Order.STATUS_PENDING) {
            throw new SeckillException("订单状态不允许取消");
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(Order.STATUS_CANCELLED);
        orderMapper.updateById(order);
        log.info("订单取消成功: {}", order.getOrderNo());
        return order;
    }

    public Order findById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new SeckillException("订单不存在");
        }
        return order;
    }

    public Order findByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo)
                .orElseThrow(() -> new SeckillException("订单不存在"));
    }

    public Order findOrderWithItems(Long id) {
        Order order = orderMapper.findOrderWithItems(id);
        if (order == null) {
            throw new SeckillException("订单不存在");
        }
        return order;
    }

    public List<Order> findByUserId(Long userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        // 加载订单项
        for (Order order : orders) {
            order.setItems(orderItemMapper.findByOrderId(order.getId()));
        }
        return orders;
    }

    public Page<Order> findPage(int pageNum, int pageSize) {
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", new Random().nextInt(1000000));
        return "ORD" + datePart + randomPart;
    }
}
