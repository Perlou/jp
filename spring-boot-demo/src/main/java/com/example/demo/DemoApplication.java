package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用启动类
 * 
 * @SpringBootApplication 是一个组合注解，包含：
 *                        - @Configuration: 标识这是一个配置类
 *                        - @EnableAutoConfiguration: 启用自动配置
 *                        - @ComponentScan: 扫描当前包及子包下的组件
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("\n============================================");
        System.out.println("🚀 Spring Boot Demo 启动成功!");
        System.out.println("============================================");
        System.out.println("API 文档: http://localhost:8080");
        System.out.println("H2 控制台: http://localhost:8080/h2-console");
        System.out.println("健康检查: http://localhost:8080/actuator/health");
        System.out.println("============================================\n");
    }
}
