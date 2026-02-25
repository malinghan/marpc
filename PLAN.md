# marpc — 轻量级 Java RPC 框架

基于 Spring Boot 4.0 + Java 17 构建的教学型 RPC 框架，逐步实现分布式系统核心特性。

---

## 版本迭代计划

### ✅ v1.0 — 基础 RPC 骨架

**核心目标：** 跑通最简单的远程调用

**实现内容：**
- 核心数据结构：`RpcRequest` / `RpcResponse`
- Provider 端：HTTP 接口 + 反射调用
- Consumer 端：JDK 动态代理 + OkHttp
- 注解：`@MarpcProvider` / `@MarpcConsumer`
- Spring Boot 自动装配：`@EnableMarpc`

---

### ✅ v1.1 — 基础优化

**核心目标：** 提升性能与健壮性

**实现内容：**
- 基本类型直接返回，跳过不必要的 JSON 转换
- 非用户接口过滤（`java.*` / `javax.*` / `org.springframework.*`）
- 异常信息封装透传（类名 + message）

---

### ✅ v2.0 — 序列化 & 注册中心 & 负载均衡

**核心目标：** 解决方法重载、类型转换、动态服务发现

**实现内容：**

**1. 序列化优化**
- 方法签名规范：`methodName@paramCount_type1_type2`，解决重载歧义
- JSON 反序列化到正确 Java 类型（基本类型、包装类、List、Map、自定义对象）
- 返回值类型推断与转换

**2. 注册中心接入**
- 抽象 `RegistryCenter` 接口：`register` / `unregister` / `fetchAll` / `subscribe`
- 实现 `ZkRegistryCenter`（Zookeeper + Curator）
- 注册路径规范：`/{app}_{env}_{serviceName}/{host}_{port}`
- Provider 启动时注册，优雅关闭时注销
- Consumer 订阅变更，动态刷新服务列表

**3. 负载均衡**
- 抽象 `LoadBalancer` 接口
- 实现 `RandomLoadBalancer`（随机）
- 实现 `RoundRobinLoadBalancer`（轮询）
- 配置项切换策略

**测试：** 构造测试场景，输出直观结果验证功能

---

### ✅ v3.0 — 模块拆分 & 统一异常体系

**核心目标：** 整理架构，为后续扩展打好基础

**实现内容：**

**1. 多模块拆分**
- `marpc-core`：核心 RPC 结构体和协议定义
- `marpc-demo-api`：Demo 程序的 Service Bean 定义
- `marpc-demo-provider`：Provider 服务启动
- `marpc-demo-consumer`：Consumer 服务启动

**2. 统一异常体系**
- `MarpcException`：区分业务异常 / 框架异常 / 网络异常

**3. Bean 生命周期规范**
- 清理启动流程，新增测试日志

**测试：** 构造测试场景验证功能

---

### ✅ v4.0 — Filter 机制 & Testcontainers 测试隔离

**核心目标：** 可插拔的请求/响应处理链

**实现内容：**

**1. Filter 机制**
- 抽象 `Filter` 接口（`preFilter` / `postFilter`）
- 实现 `CacheFilter`：Consumer 端结果缓存
- 实现 `MockFilter`：接口 Mock 返回
- Filter 链组装与执行顺序控制

**2. 测试隔离**
- 引入 Testcontainers 支持 Zookeeper 测试环境隔离

**文档：** 生成 v4.0.md

---

### ✅ v5.0 — 重试 & 熔断 & 灰度路由

**核心目标：** 提升调用可靠性，防止雪崩，支持灰度发布

**实现内容：**

**1. 重试机制**
- 异常分类：业务异常 / 框架异常 / 网络异常
- Consumer 端可配置重试次数（`retries`）
- 超时配置（`timeout`，单位 ms）
- 异常信息透传到 Consumer 端并还原
- 重试时自动切换节点，避免打到同一故障实例

**2. 熔断机制**
- 滑动时间窗口统计失败率
- 熔断状态机：`Closed → Open → Half-Open → Closed`
- 可配置：`faultLimit`（触发阈值）、`halfOpenInitialDelay`、`halfOpenDelay`
- Half-Open 状态下探测请求，成功则自动恢复

**3. 灰度路由**
- 抽象 `Router` 接口
- 实现 `GrayRouter`：按比例将流量路由到灰度节点
- Provider 元数据标记：`gray: true/false`
- Consumer 配置灰度比例：`grayRatio`（0-100）

**测试：** 构造测试场景验证功能
**文档：** 生成 v5.0.md

---

### ✅ v6.0 — RpcContext 隐式传参 & Netty 传输层

**核心目标：** 跨调用链传递上下文，优化传输性能

**实现内容：**

**1. RpcContext 隐式传参**
- `RpcContext`：ThreadLocal 存储隐式参数
- 请求头携带 context 参数透传到 Provider
- 支持 `grayId` 等流量染色标记，用于链路监控和日志追踪
- 调用结束后自动清理 ThreadLocal，防止内存泄漏

**2. Netty 传输层**
- OkHttp → Netty 长连接传输，降低连接建立开销
- 保留 OkHttp 传输方式，通过配置切换（`marpc.transport: okhttp|netty`）
- Netty 连接池优化，复用 Channel
- 自定义二进制协议设计（魔数 + 版本 + 类型 + 序列号 + 长度 + JSON body）
- 解决粘包问题（`MarpcFrameDecoder` / `MarpcFrameEncoder`）

**3. 配置化**
- `marpc.transport`：okhttp / netty（默认 okhttp）
- `marpc.netty.port`：Netty 服务端口（默认 9090）
- `marpc.netty.server.enabled`：是否启动 Netty 服务端（默认 false，只在 Provider 侧开启）

**测试：** Scene10（RpcContext）、Scene11（Netty 传输）
**文档：** 生成 v6.0.md

---

## 后续规划

### v7.0 — 配置中心 & 注册中心可插拔

**目标1：配置中心接入**
- 统一配置类：`AppConfigProperties` / `ProviderConfigProperties` / `ConsumerConfigProperties`
- Provider 端 TPS 限流（滑动时间窗口，`tc` 参数，默认 30s 窗口）
- 接入 Apollo 配置中心，支持动态推送配置变更
- 自研配置中心 `maconfig` 接入（待 maconfig 开发完成）

**目标2：注册中心可插拔**
- 实现 `MarpcRegistryCenter`（轻量 HTTP 注册中心）
- 替换 Zookeeper 依赖为自研 `maregistry`（待 maregistry 开发完成）

---

### Maven Central 发布

**目标：** 将 marpc 发布到 Maven 中央仓库

**实现内容：**
- Maven Central 发布流程（flatten-maven-plugin）
- POM 规范化（licenses、developers、scm）
- GPG 签名配置

---

### 可观测性（规划中）

**目标：** 线上问题可追踪、可度量

**实现内容：**
- 集成 Micrometer / OpenTelemetry 调用链追踪
- 指标暴露：QPS、P99 延迟、熔断次数、重试次数
- 健康检查端点（`/marpc/health`）
- MDC 注入 traceId，日志与链路打通

---

### 服务治理增强（规划中）

**目标：** 对齐生产级服务治理能力

**实现内容：**
- 权重路由（`WeightedRoundRobinLoadBalancer`）
- 服务分组与版本隔离（`group`、`version` 元数据）
- 优雅停机：等待在途请求完成后再注销
- Provider 限流升级：令牌桶算法替换滑动窗口