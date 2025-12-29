# 微服务电商系统

> Spring Boot 3 + Spring Cloud 微服务学习项目

---

## 🎯 项目简介

基于 Spring Cloud 的微服务电商系统，演示服务注册发现、服务调用、API 网关等核心概念。

## 🛠️ 技术栈

| 组件     | 技术                         | 版本       |
| -------- | ---------------------------- | ---------- |
| 核心框架 | Spring Boot                  | 3.2.0      |
| 微服务   | Spring Cloud                 | 2023.0.0   |
| 服务注册 | Nacos (Spring Cloud Alibaba) | 2022.0.0.0 |
| 服务调用 | OpenFeign                    | -          |
| 网关     | Spring Cloud Gateway         | -          |
| 数据库   | H2 (内存)                    | -          |
| 容器     | Docker                       | -          |

---

## 📁 项目结构

```
ecommerce-microservices/
├── pom.xml                      # 父 POM
├── docker-compose.yml           # Docker 配置 (Nacos)
├── mall-common/                 # 公共模块
│   └── src/.../common/
│       ├── entity/BaseEntity.java
│       ├── result/Result.java
│       └── exception/
├── mall-gateway/                # API 网关 (:8080)
└── mall-service/
    ├── user-service/            # 用户服务 (:8081)
    ├── product-service/         # 商品服务 (:8082)
    ├── inventory-service/       # 库存服务 (:8083)
    └── order-service/           # 订单服务 (:8084)
```

---

## 🚀 快速开始

### 1. 编译安装

```bash
cd projects/ecommerce-microservices

# 首次运行必须执行 - 安装所有模块到本地仓库
mvn clean install -DskipTests
```

### 2. 启动 Nacos (需要 Docker)

```bash
# Apple Silicon Mac 已适配
docker-compose up -d nacos

# 等待启动完成后访问控制台
# http://localhost:8848/nacos
# 账号/密码: nacos/nacos
```

### 3. 启动微服务

恢复 Nacos 配置后，在不同终端分别启动：

```bash
# 1. 网关
cd mall-gateway && mvn spring-boot:run

# 2. 用户服务
cd mall-service/user-service && mvn spring-boot:run

# 3. 商品服务
cd mall-service/product-service && mvn spring-boot:run

# 4. 库存服务
cd mall-service/inventory-service && mvn spring-boot:run

# 5. 订单服务
cd mall-service/order-service && mvn spring-boot:run
```

### 4. 独立运行 (无需 Docker)

当前配置默认禁用 Nacos，可直接启动各服务进行本地测试：

```bash
cd mall-service/user-service
mvn spring-boot:run
# 访问: http://localhost:8081/users
```

---

## 📖 API 接口

### 用户服务 (localhost:8081)

```bash
# 注册用户
curl -X POST http://localhost:8081/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","nickname":"测试用户"}'

# 查询所有用户
curl http://localhost:8081/users

# 查询单个用户
curl http://localhost:8081/users/1
```

### 商品服务 (localhost:8082)

```bash
# 创建商品
curl -X POST http://localhost:8082/products \
  -H "Content-Type: application/json" \
  -d '{"name":"iPhone 15","price":8999,"category":"手机"}'

# 查询商品
curl http://localhost:8082/products
```

### 库存服务 (localhost:8083)

```bash
# 初始化库存
curl -X POST "http://localhost:8083/inventory/init?productId=1&stock=100"

# 查询库存
curl http://localhost:8083/inventory/1
```

### 订单服务 (localhost:8084)

```bash
# 创建订单 (需要其他服务运行)
curl -X POST http://localhost:8084/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"quantity":2}'
```

---

## 🔧 服务端口

| 服务      | 端口 | 说明         |
| --------- | ---- | ------------ |
| Nacos     | 8848 | 服务注册中心 |
| Gateway   | 8080 | API 网关入口 |
| User      | 8081 | 用户服务     |
| Product   | 8082 | 商品服务     |
| Inventory | 8083 | 库存服务     |
| Order     | 8084 | 订单服务     |

---

## ⚠️ 注意事项

1. **首次运行**: 必须先执行 `mvn clean install -DskipTests`
2. **Apple Silicon**: docker-compose.yml 已适配 ARM64 架构
3. **独立模式**: 当前配置禁用了 Nacos，可独立运行各服务
4. **完整模式**: 启用 Nacos 后需修改 `application.yml` 中的 `spring.cloud.nacos.discovery.enabled: true`

---
