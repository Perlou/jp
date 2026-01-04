# 🛒 秒杀商城系统

## 🛠️ 技术栈

| 核心技术        | 用途     |
| --------------- | -------- |
| Spring Boot 3.2 | 应用框架 |
| MyBatis-Plus    | ORM      |
| MySQL + Redis   | 存储     |
| RabbitMQ        | 消息队列 |
| Caffeine        | 本地缓存 |

## 🚀 快速开始

```bash
# 启动基础设施
docker-compose up -d

# 启动后端
mvn spring-boot:run

# 启动前端 Admin
cd frontend && pnpm install && pnpm dev:admin
```

**访问地址：**

- 前端: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Admin: http://localhost:5174

---
