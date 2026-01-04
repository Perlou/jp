package com.example.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 秒杀系统启动类
 */
@SpringBootApplication
// @MapperScan("com.example.seckill.mapper")
public class SeckillApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeckillApplication.class, args);
		System.out.println("""

				============================================
				       秒杀系统启动成功!
				============================================

				Swagger 文档: http://localhost:8080/swagger-ui.html

				接口说明:
				- GET  /api/seckill/goods         获取秒杀商品列表
				- GET  /api/seckill/goods/{id}    获取商品详情
				- POST /api/seckill/do            执行秒杀
				- GET  /api/seckill/result        查询秒杀结果
				- POST /api/seckill/reset/{id}    重置秒杀（测试）

				============================================
				""");
	}
}