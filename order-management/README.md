# 订单管理系统

> Spring Boot + MyBatis-Plus + MySQL + Redis + Flyway

---

## 🎯 项目简介

综合运用 MySQL、MyBatis-Plus、Redis、Flyway 的订单管理系统，包含用户、商品、订单、报表统计等完整功能。

## 🛠️ 技术栈

| 技术         | 版本  | 说明       |
| ------------ | ----- | ---------- |
| Spring Boot  | 3.2.0 | 核心框架   |
| MyBatis-Plus | 3.5.5 | ORM 增强   |
| MySQL        | 8.0   | 生产数据库 |
| H2           | -     | 开发测试   |
| Redis        | 7.x   | 缓存       |
| Flyway       | 9.22  | 数据库迁移 |
| Swagger      | 2.3.0 | API 文档   |

---

## 📁 项目结构

```
order-management/
├── pom.xml
├── docker-compose.yml
├── src/main/java/com/example/order/
│   ├── OrderApplication.java
│   ├── config/
│   │   ├── MyBatisPlusConfig.java
│   │   ├── RedisConfig.java
│   │   └── SwaggerConfig.java
│   ├── entity/
│   ├── mapper/
│   ├── service/
│   │   ├── ReportService.java
│   │   └── CacheService.java
│   ├── controller/
│   │   └── ReportController.java
│   ├── dto/
│   └── exception/
└── src/main/resources/
    ├── application.yml
    └── db/
        └── migration/               # Flyway 迁移脚本
            ├── V1__Initial_schema.sql
            └── V2__Insert_initial_data.sql
```

---

## 🚀 快速开始

### 方式 1: H2 模式 (默认，无需外部依赖)

```bash
cd projects/order-management
mvn spring-boot:run
```

### 方式 2: MySQL + Redis 模式

```bash
# 1. 启动 MySQL 和 Redis
docker-compose up -d

# 2. 使用 mysql profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 访问地址

| 服务          | 地址                                  |
| ------------- | ------------------------------------- |
| 应用          | http://localhost:8080                 |
| Swagger UI    | http://localhost:8080/swagger-ui.html |
| H2 控制台     | http://localhost:8080/h2-console      |
| API 文档 JSON | http://localhost:8080/v3/api-docs     |

---

## 📖 API 接口

### 用户接口 `/api/users`

```bash
# 注册
curl -X POST "http://localhost:8080/api/users/register?username=demo&password=123456"

# 登录
curl -X POST "http://localhost:8080/api/users/login?username=admin&password=123456"
```

### 商品接口 `/api/products`

```bash
# 查询商品 (带 Redis 缓存)
curl http://localhost:8080/api/products/1

# 分页查询
curl "http://localhost:8080/api/products/page?pageNum=1&pageSize=5"
```

### 订单接口 `/api/orders`

```bash
# 创建订单
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"items":[{"productId":1,"quantity":2}]}'

# 支付订单
curl -X POST http://localhost:8080/api/orders/1/pay
```

### 报表接口 `/api/reports`

```bash
# 销售报表 (按日统计)
curl "http://localhost:8080/api/reports/sales?startDate=2024-01-01&endDate=2024-12-31"

# 热销商品 TOP 10
curl "http://localhost:8080/api/reports/top-products?days=30&limit=10"
```

---

## 🔧 核心功能

### 1. Flyway 数据库迁移

```
db/migration/
├── V1__Initial_schema.sql    # 表结构
└── V2__Insert_initial_data.sql  # 初始数据
```

### 2. Redis 缓存

- 商品详情缓存 (1 小时)
- 报表数据缓存 (5-10 分钟)

### 3. 报表统计

- 按日销售统计
- 热销商品排行

### 4. 库存扣减 (原子操作)

```sql
UPDATE products SET stock = stock - #{quantity}
WHERE id = #{id} AND stock >= #{quantity}
```

### 5. 乐观锁

```java
@Version
private Integer version;
```

---

## 📚 学习要点

- ✅ Flyway 数据库版本控制
- ✅ MySQL 索引与事务
- ✅ MyBatis-Plus CRUD
- ✅ 乐观锁防超卖
- ✅ Redis 缓存策略
- ✅ Swagger API 文档
- ✅ 报表统计查询

---

> 对应课程: Phase 11 数据库与 ORM
