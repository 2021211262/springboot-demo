# SpringBoot Demo

SpringBoot 练手项目，涵盖 MVC、依赖注入、事务、AOP、拦截器、数据库、Redis、RocketMQ 等核心知识点。

## 项目结构

springboot-demo/
├── pom.xml                                          # Maven 依赖配置
├── src/main/
│   ├── java/com/example/springbootdemo/
│   │   ├── SpringbootDemoApplication.java           # 启动类，@MapperScan 扫描 Mapper
│   │   │
│   │   ├── common/                                  # 通用组件
│   │   │   ├── Result.java                          # 统一响应封装（code/message/data）
│   │   │   ├── ResultCode.java                      # 响应状态码枚举
│   │   │   ├── BusinessException.java               # 业务异常，携带 code
│   │   │   ├── GlobalExceptionHandler.java          # 全局异常处理，映射 HTTP 状态码
│   │   │   └── CacheConstants.java                  # 缓存 Key 常量
│   │   │
│   │   ├── config/                                  # 配置类
│   │   │   ├── AppConfig.java                       # ObjectMapper Bean（JavaTimeModule）
│   │   │   └── WebConfig.java                       # 注册拦截器，拦截 /api/**
│   │   │
│   │   ├── interceptor/                             # 拦截器
│   │   │   └── AuthInterceptor.java                 # Token 校验，Header: X-Request-Token
│   │   │
│   │   ├── aop/                                     # 切面
│   │   │   └── OperationLogAspect.java              # Controller 方法耗时与参数日志
│   │   │
│   │   ├── entity/                                  # 实体类
│   │   │   ├── Product.java                         # 商品
│   │   │   ├── Order.java                           # 订单
│   │   │   └── OrderStatus.java                     # 订单状态枚举（PENDING/PAID/CANCELLED）
│   │   │
│   │   ├── dto/                                     # 请求参数
│   │   │   ├── ProductCreateRequest.java            # 创建商品（@NotBlank @Size @DecimalMin）
│   │   │   └── OrderCreateRequest.java              # 创建订单（@NotNull @Min）
│   │   │
│   │   ├── mapper/                                  # MyBatis Mapper 接口
│   │   │   ├── ProductMapper.java                   # 商品 CRUD + 乐观锁扣库存
│   │   │   └── OrderMapper.java                     # 订单 CRUD + 按商品统计
│   │   │
│   │   ├── service/                                 # 业务逻辑
│   │   │   ├── ProductService.java                  # 商品缓存（Redis）+ 名称唯一校验 + 删除关联检查
│   │   │   ├── OrderService.java                    # 下单入口：分布式锁 → 创建 → 发MQ消息
│   │   │   ├── OrderCreateService.java              # @Transactional 创建订单：校验→扣库存→插订单→清缓存
│   │   │   └── DistributedLockService.java          # Redis 分布式锁（SET NX + Lua 释放 + 重试）
│   │   │
│   │   ├── mq/                                      # 消息队列
│   │   │   ├── OrderMessageProducer.java            # 发送 ORDER_CREATED 消息（@Autowired required=false）
│   │   │   └── RocketMQConsumerConfig.java          # 消费者配置（@ConditionalOnProperty 按需启用）
│   │   │
│   │   └── controller/                              # REST 接口
│   │       ├── ProductController.java               # POST/GET/DELETE /api/products
│   │       └── OrderController.java                 # POST/GET /api/orders
│   │
│   └── resources/
│       ├── application.yml                          # 主配置（数据源/Redis/RocketMQ/MyBatis）
│       ├── sql/schema.sql                           # 建库建表脚本
│       └── mapper/
│           ├── ProductMapper.xml                    # 商品 SQL
│           └── OrderMapper.xml                      # 订单 SQL


## 功能设计

### 商品管理
- 创建商品（名称唯一校验，自动写入 Redis 缓存）
- 查询商品（先查 Redis 缓存，未命中再查数据库并回写缓存）
- 删除商品（有关联订单时拒绝删除，同时清除缓存）

### 订单管理
- 创建订单（分布式锁防并发 → 乐观锁扣库存 → 事务内创建订单 → 清除商品缓存 → 发送 RocketMQ 消息）
- 查询订单

### 通用能力
- **拦截器**：所有 `/api/**` 请求需携带 `X-Request-Token: Bearer-xxx`
- **AOP 日志**：自动记录 Controller 方法的入参和耗时
- **参数校验**：`@Valid` + Bean Validation 注解
- **全局异常处理**：BusinessException → 对应 HTTP 状态码，校验异常 → 400
- **分布式锁**：Redis SET NX + Lua 原子释放 + 重试机制
- **RocketMQ**：订单创建后异步发送消息，按需启用（未配置 name-server 时自动跳过）


## 启动与测试

### 前置条件
- JDK 17+
- MySQL（端口 3306）
- Redis（端口 6379）
- RocketMQ（端口 9876）

### 1. 启动MySQL数据库

### 2. 启动 RocketMQ

### 3. 启动应用
- 进入项目文件夹demo\springboot-demo打包
  mvn package -DskipTests

- 设置MySQL密码
  $env:MYSQL_PASSWORD = "你的MySQL密码"

- 启动生成的包体
  java -jar target/springboot-demo-1.0.0.jar

### 4. 功能测试

所有请求需携带认证头：`X-Request-Token: Bearer-test`

```powershell
# 创建商品
Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Post -ContentType "application/json" -Body '{"name":"iPhone 16","price":6999.00,"stock":100}' -Headers @{"X-Request-Token"="Bearer-test"}

# 查询所有商品
Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Headers @{"X-Request-Token"="Bearer-test"}

# 创建订单
Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -ContentType "application/json" -Body '{"productId":1,"quantity":2}' -Headers @{"X-Request-Token"="Bearer-test"}

# 查询订单
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/1" -Headers @{"X-Request-Token"="Bearer-test"}
```

### 5. 异常场景测试

```powershell
# 无 Token → 401
Invoke-RestMethod -Uri "http://localhost:8080/api/products"

# 商品不存在 → 404
Invoke-RestMethod -Uri "http://localhost:8080/api/products/999" -Headers @{"X-Request-Token"="Bearer-test"}

# 参数校验失败 → 400
Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Post -ContentType "application/json" -Body '{"name":"","price":-1,"stock":0}' -Headers @{"X-Request-Token"="Bearer-test"}
```