# Spring Boot 用户管理服务 (jp-user-service)

一个基于 Spring Boot 3.2 的完整 RESTful API 示例项目，实现了用户管理的核心功能。

## 🚀 技术栈

- **核心框架**: Spring Boot 3.2.0
- **开发语言**: Java 17
- **持久层**: Spring Data JPA
- **数据库**: H2 Database (内存数据库)
- **API 文档**: SpringDoc OpenAPI (Swagger UI)
- **校验**: Spring Boot Validation
- **监控**: Spring Boot Actuator

## ✨ 核心功能

- **用户 CRUD**: 完整的创建、查询、更新和删除功能。
- **数据校验**: 实体和 DTO 层的入参验证（用户名长度、邮箱格式等）。
- **统一异常处理**: 全局异常拦截，返回标准化的错误响应。
- **DTO 映射**: 隔离数据库实体 (Entity) 与视图层 (DTO/Response)。
- **OpenAPI 文档**: 自动生成的在线交互式 API 文档。
- **JPA 审计**: 自动填充 `createdAt` 和 `updatedAt` 时间戳。

## 📂 项目结构

```text
src/main/java/com/example/jp_user_service/
├── config/             # 配置类 (OpenAPI 等)
├── controller/         # REST 控制器 (API 入口)
├── dto/                # 数据传输对象 (Request/Response)
├── entity/             # JPA 数据库实体
├── exception/          # 异常定义及全局异常处理器
├── repository/         # 数据库访问层 (JPA)
└── service/            # 业务逻辑层 (接口与实现)
```

## 🛠️ 如何启动

### 1. 运行项目

在项目根目录下，使用 Maven Wrapper 启动：

```bash
./mvnw spring-boot:run
```

### 2. 访问服务

项目启动后，默认监听 **8080** 端口。

- **API 基础路径**: `http://localhost:8080/api/users`
- **在线 API 文档 (Swagger)**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **H2 数据库控制台**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - _JDBC URL_: `jdbc:h2:mem:testdb`
  - _User_: `sa` / _Password_: (空)
- **应用健康检查**: `http://localhost:8080/actuator/health`

## 📝 常用 API 示例

### 创建用户

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan", "email":"zhangsan@example.com"}'
```

### 查询所有用户

```bash
curl http://localhost:8080/api/users
```

### 更新用户

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan_updated", "email":"zhangsan@example.com"}'
```
