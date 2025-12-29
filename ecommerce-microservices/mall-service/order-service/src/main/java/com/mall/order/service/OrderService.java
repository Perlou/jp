package com.mall.order.service;

import com.mall.common.exception.BusinessException;
import com.mall.common.result.Result;
import com.mall.order.dto.CreateOrderDTO;
import com.mall.order.entity.Order;
import com.mall.order.feign.*;
import com.mall.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final AtomicLong orderSequence = new AtomicLong(1);

    public OrderService(OrderRepository orderRepository, UserClient userClient,
            ProductClient productClient, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public Order createOrder(CreateOrderDTO dto) {
        log.info("创建订单: userId={}, productId={}, quantity={}",
                dto.getUserId(), dto.getProductId(), dto.getQuantity());

        // 1. 查询用户
        Result<UserDTO> userResult = userClient.findById(dto.getUserId());
        if (!userResult.isSuccess()) {
            throw BusinessException.of("用户服务异常: " + userResult.getMessage());
        }
        UserDTO user = userResult.getData();
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        log.info("用户信息: {}", user.getUsername());

        // 2. 查询商品
        Result<ProductDTO> productResult = productClient.findById(dto.getProductId());
        if (!productResult.isSuccess()) {
            throw BusinessException.of("商品服务异常: " + productResult.getMessage());
        }
        ProductDTO product = productResult.getData();
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        log.info("商品信息: {} - ¥{}", product.getName(), product.getPrice());

        // 3. 扣减库存
        Result<Boolean> inventoryResult = inventoryClient.deduct(dto.getProductId(), dto.getQuantity());
        if (!inventoryResult.isSuccess()) {
            throw BusinessException.of("库存扣减失败: " + inventoryResult.getMessage());
        }
        log.info("库存扣减成功");

        // 4. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(dto.getUserId());
        order.setProductId(dto.getProductId());
        order.setProductName(product.getName());
        order.setQuantity(dto.getQuantity());
        order.setUnitPrice(product.getPrice());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setRemark(dto.getRemark());
        order.setStatus(Order.OrderStatus.CREATED);

        Order saved = orderRepository.save(order);
        log.info("订单创建成功: {}", saved.getOrderNo());

        return saved;
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
    }

    public Order findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
    }

    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order cancel(Long id) {
        Order order = findById(id);
        if (order.getStatus() != Order.OrderStatus.CREATED) {
            throw BusinessException.of("订单状态不允许取消");
        }
        inventoryClient.restore(order.getProductId(), order.getQuantity());
        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String seqPart = String.format("%06d", orderSequence.getAndIncrement() % 1000000);
        return "ORD" + datePart + seqPart;
    }
}
