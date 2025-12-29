package com.example.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单管理系统 - 启动类
 * Phase 11 实战项目
 */
@SpringBootApplication
@MapperScan("com.example.order.mapper")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════════════╗
                ║         订单管理系统启动成功!                              ║
                ║                                                          ║
                ║  H2 控制台: http://localhost:8080/h2-console             ║
                ║  API 文档:  http://localhost:8080/                       ║
                ║                                                          ║
                ║  用户 API:   /api/users                                  ║
                ║  商品 API:   /api/products                               ║
                ║  订单 API:   /api/orders                                 ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }
}
