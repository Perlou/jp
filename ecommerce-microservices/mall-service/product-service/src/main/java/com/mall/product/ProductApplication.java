package com.mall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商品服务启动类
 */
@SpringBootApplication(scanBasePackages = { "com.mall.product", "com.mall.common" })
@EnableDiscoveryClient
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
        System.out.println("=".repeat(50));
        System.out.println("  Product Service 启动成功!");
        System.out.println("  端口: 8082");
        System.out.println("=".repeat(50));
    }
}
