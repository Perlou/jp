package com.example.jp_user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JpUserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpUserServiceApplication.class, args);
		System.out.println("\n============================================");
		System.out.println("🚀 启动成功!");
		System.out.println("============================================");
		System.out.println("API 文档: http://localhost:8080");
		System.out.println("H2 控制台: http://localhost:8080/h2-console");
		System.out.println("健康检查: http://localhost:8080/actuator/health");
		System.out.println("============================================\n");
	}

}
