# Spring Boot Demo - 用户管理 API

一个完整的 Spring Boot RESTful API 示例项目。

## 项目结构

```
spring-boot-demo/
├── pom.xml
└── src/main/java/com/example/demo/
    ├── DemoApplication.java      # 启动类
    ├── controller/
    │   └── UserController.java   # 控制器
    ├── service/
    │   ├── UserService.java      # 服务接口
    │   └── UserServiceImpl.java  # 服务实现
    ├── repository/
    │   └── UserRepository.java   # 数据访问
    ├── entity/
    │   └── User.java             # 实体类
    ├── dto/
    │   ├── UserRequest.java      # 请求 DTO
    │   └── UserResponse.java     # 响应 DTO
    └── exception/
        ├── ResourceNotFoundException.java
        ├── DuplicateResourceException.java
        └── GlobalExceptionHandler.java
```

## 启动项目

```bash
cd spring-boot-demo
mvn spring-boot:run
```

## API 文档

### 1. 创建用户

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"张三","email":"zhangsan@example.com"}'
```

### 2. 查询所有用户

```bash
curl http://localhost:8080/api/users
```

### 3. 查询指定用户

```bash
curl http://localhost:8080/api/users/1
```

### 4. 更新用户

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"张三更新","email":"zhangsan@example.com"}'
```

### 5. 删除用户

```bash
curl -X DELETE http://localhost:8080/api/users/1
```

## 其他端点

- **H2 控制台**: http://localhost:8080/h2-console
- **健康检查**: http://localhost:8080/actuator/health
- **指标**: http://localhost:8080/actuator/metrics

## 技术栈

- Spring Boot 3.2
- Spring Data JPA
- H2 Database
- Spring Validation
- Spring Actuator
