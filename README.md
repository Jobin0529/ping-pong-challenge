# Ping-Pong Challenge

基于 **Spring WebFlux + RocketMQ + MongoDB** 的跨进程限流微服务实战项目。

## 项目简介

本项目实现了一个 Ping-Pong 挑战系统，核心特性：

- **跨进程速率限制**：使用 Java `FileLock` 实现多个 Ping 实例间的跨进程限流（合计 2 RPS）
- **Pong 端限流**：Pong 服务内置 `ThrottleFilter`，限制 1 RPS
- **RocketMQ 审计**：Pong 服务通过 RocketMQ 发送审计消息，Ping 服务消费
- **MongoDB 日志持久化**：所有请求日志异步写入 MongoDB
- **响应式架构**：基于 Spring WebFlux + Reactor 非阻塞模型

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.0 + WebFlux |
| 消息队列 | RocketMQ 5.3.1 |
| 数据库 | MongoDB |
| 构建工具 | Maven |
| 测试 | Groovy + Spock |
| Java | 21 |

## 系统架构

```
                    ┌─────────────────────────────────────────┐
                    │         RocketMQ (Proxy:8081)            │
                    │  NameServer:9876 ←→ Broker+Proxy:8081   │
                    └──────────┬──────────────────┬────────────┘
                               │ 生产审计消息      │ 消费审计消息
                    ┌──────────▼──────────┐  ┌─────┴───────────────┐
                    │   Pong Service      │  │  Ping Service ×3    │
                    │   port: 8082        │  │  8080/8090/8091     │
                    │   (限流: 1 RPS)     │  │  (跨进程限流: 2 RPS)│
                    └──────────┬──────────┘  └─────┬───────────────┘
                               │                    │
                    ┌──────────▼──────────────────────▼──────────┐
                    │              MongoDB :27017                 │
                    │         (pingpong / request_logs)           │
                    └────────────────────────────────────────────┘
```

## 项目结构

```
ping-pong-challenge/
├── common/                    # 公共模块
│   └── src/main/java/
│       └── com/example/common/
│           ├── model/         # PingPongMessage, RequestResult
│           └── ratelimit/     # FileLockRateLimiter
├── ping-service/              # Ping 服务（调用方）
│   └── src/main/java/
│       └── com/example/ping/
│           ├── config/        # RateLimiter, RocketMQ, MongoDB, WebClient 配置
│           ├── mongo/         # RequestLog 实体, Repository, MongoLogService
│           ├── mq/            # PongAuditConsumer
│           ├── scheduler/     # PingScheduler 定时调度
│           └── service/       # PingService 核心业务
├── pong-service/              # Pong 服务（被调用方）
│   └── src/main/java/
│       └── com/example/pong/
│           ├── config/        # RocketMQ 配置
│           ├── controller/    # PongController REST 接口
│           ├── mq/            # PongAuditProducer 审计消息生产者
│           └── throttle/      # ThrottleFilter 限流过滤器
├── scripts/                   # 启动/停止脚本
│   ├── start-all.sh
│   └── stop-all.sh
└── pom.xml                    # 父 POM
```

## 快速启动

### 前置条件

- Java 21+
- Maven 3.8+
- MongoDB（端口 27017）
- RocketMQ 5.3.1

### 1. 编译项目

```bash
export JAVA_HOME=/path/to/java21
mvn clean package -DskipTests
```

### 2. 启动 MongoDB

```bash
mongod --dbpath /usr/local/var/mongodb --fork
```

### 3. 启动 RocketMQ

```bash
# 启动 NameServer
sh bin/mqnamesrv

# 启动 Broker + Proxy（新终端）
sh bin/mqbroker -n localhost:9876 --enable-proxy

# 创建 Topic（新终端）
sh bin/mqadmin updateTopic -n localhost:9876 -b localhost:10911 -t pong-audit
```

### 4. 启动服务

```bash
# 启动 Pong 服务
java -jar pong-service/target/pong-service-1.0.0-SNAPSHOT.jar

# 启动 Ping 服务（多个实例，不同端口）
java -jar ping-service/target/ping-service-1.0.0-SNAPSHOT.jar --server.port=8080 &
java -jar ping-service/target/ping-service-1.0.0-SNAPSHOT.jar --server.port=8090 &
java -jar ping-service/target/ping-service-1.0.0-SNAPSHOT.jar --server.port=8091 &
```

### 5. 一键启动（推荐）

```bash
bash scripts/start-all.sh
```

## 端口分配

| 组件 | 端口 | 说明 |
|------|------|------|
| MongoDB | 27017 | 日志存储 |
| RocketMQ NameServer | 9876 | 注册中心 |
| RocketMQ Proxy | 8081 | gRPC 代理 |
| Pong Service | 8082 | 被调用方 |
| Ping Service ×3 | 8080/8090/8091 | 调用方 |

## 核心设计

### 跨进程限流 (FileLockRateLimiter)

使用 Java NIO `FileLock` 实现跨进程互斥，确保所有 Ping 实例合计不超过 2 RPS：

1. 所有 Ping 实例共享同一个锁文件 `/tmp/ping-pong-rate.lock`
2. 每次请求前获取文件锁，检查当前秒内的请求计数
3. 超过阈值则拒绝，未超过则放行并更新计数

### Pong 端限流 (ThrottleFilter)

基于 WebFlux `WebFilter` 实现，限制 Pong 服务最多处理 1 RPS。

### RocketMQ 审计

- Pong 服务处理请求后，通过 `PongAuditProducer` 发送审计消息到 `pong-audit` Topic
- Ping 服务通过 `PongAuditConsumer` 拉取并处理审计消息

### MongoDB 日志

每个请求的结果（成功/限流/超时/错误）异步写入 MongoDB `pingpong.request_logs` 集合。

## 测试

```bash
# 运行所有测试
mvn test

# 运行单个模块测试
mvn test -pl common
mvn test -pl ping-service
mvn test -pl pong-service
```

## 停止服务

```bash
bash scripts/stop-all.sh
```
