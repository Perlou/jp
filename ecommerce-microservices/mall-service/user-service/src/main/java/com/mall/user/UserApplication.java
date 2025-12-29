package com.mall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类
 */
@SpringBootApplication(scanBasePackages = { "com.mall.user", "com.mall.common" })
@EnableDiscoveryClient
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
        System.out.println("=".repeat(50));
        System.out.println("  User Service 启动成功!");
        System.out.println("  端口: 8081");
        System.out.println("=".repeat(50));
    }
}
