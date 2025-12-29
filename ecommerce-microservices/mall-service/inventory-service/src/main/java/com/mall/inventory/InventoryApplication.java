package com.mall.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = { "com.mall.inventory", "com.mall.common" })
@EnableDiscoveryClient
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
        System.out.println("=".repeat(50));
        System.out.println("  Inventory Service 启动成功!");
        System.out.println("  端口: 8083");
        System.out.println("=".repeat(50));
    }
}
