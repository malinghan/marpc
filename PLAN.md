# marpc — 轻量级 Java RPC 框架

基于 Spring Boot 4.0 + Java 17 构建的教学型 RPC 框架，逐步实现分布式系统核心特性。

---

## 版本迭代计划

### v1.0 — 基础 RPC 骨架 ✅

**目标：** 跑通最简单的远程调用

- 定义核心请求、响应数据结构：`RpcRequest` / `RpcResponse`
- Provider 端：HTTP 接口接收请求，反射调用本地方法
- Consumer 端：JDK 动态代理 + OkHttp 发起 HTTP 调用
- 注解：`@MarpcProvider`、`@MarpcConsumer`
- Spring Boot 自动装配：`@EnableMarpc`

### v1.1 - 优化 — 基础 RPC 骨架 ✅
- **基本类型直接返回**：Consumer 侧对 `int/long/double/boolean` 等基本类型及其包装类，跳过 `JSON.to()` 转换，直接强转返回，避免不必要的序列化开销
- **非用户接口过滤**：Provider 注册时过滤掉 `java.*` / `javax.*` / `org.springframework.*` 等非用户自定义接口；Consumer 代理拦截时同样过滤，对来自这些包的方法直接本地执行
- **异常信息封装透传**：Provider 捕获业务异常后，将异常类名 + message 封装到 `RpcResponse.errorMessage`，Consumer 侧收到 `status=false` 时抛出携带完整信息的 `RuntimeException`

---

### v2.0 — 反序列化处理 & 方法重载 & 类型转换处理 & 注册中心接入 & 负载均衡处理
- 解决序列化和反序列表的过程中方法签名歧义和参数类型转换
  - 方法签名规范：`methodName@paramCount_type1_type2`，解决重载歧义(之前没有对方法签名进行规范化处理)
  - JSON 反序列化到正确 Java 类型（基本类型、包装类、List、Map、自定义对象）：之前provider反序列化会存在类型转换问题
  - 返回值类型推断与转换：返回的类型也会有转换问题
- 动态服务注册，脱离硬编码地址，可以接入zk或自定义的注册中心
  - 抽象 `RegistryCenter` 接口：`register` / `unregister` / `fetchAll` / `subscribe`
  - 实现 `ZkRegistryCenter`（Zookeeper + Curator）
  - 注册路径规范：`/{app}_{env}_{serviceName}/{host}_{port}`
  - Provider 启动时注册，优雅关闭时注销
  - Consumer 订阅变更，动态刷新服务列表
- 简单的负载均衡，实现多实例下的请求分发
  - 抽象 `LoadBalancer` 接口
  - 实现 `RandomLoadBalancer`（随机）
  - 实现 `RoundRobinLoadBalancer`（轮询）
  - 配置项切换策略
- 生成以上功能的测试方法，不用junit，简化测试流程，通过构造测试步骤，输出直观结果，来验证代码是否正确

---

### v3.0 — 模块拆分 & 统一异常体系 & 规范 Bean 生命周期

**目标：** 整理架构，为后续扩展打好基础

- 多模块拆分：`marpc-core` / `marpc-demo-api` / `marpc-demo-provider` / `marpc-demo-consumer`
  -    `marpc-core` 存放consumer、provider都需要的核心rpc结构体和协议定义
  -    `marpc-demo-api`: 存放demo程序的service bean定义，例如HelloService、UserService、OrderService
  -    `marpc-demo-provider`: 存放demo程序的service provider定义，作为一个provider服务启动
  -    `marpc-demo-consumer`: 存放demo程序的service consumer定义，作为一个consumer服务启动
  - 在marpc-demo-provider和marpc-demo-consumer中构造不同的使用场景，来测试marpc的功能

- 统一异常体系：`MarpcException`，区分业务异常 / 框架异常 / 网络异常

- 规范 Bean 生命周期，清理启动流程，在启动时新增测试日志

- 生成以上功能的测试方法，简化测试流程，通过构造测试步骤，输出直观结果，来验证代码是否正确

---

### v4.0 — 实现Rpc的Filter 机制

**目标：** 可插拔的请求/响应处理链

- 抽象 `Filter` 接口（`preFilter` / `postFilter`）
- 实现 `CacheFilter`: Consumer 端结果缓存, 节约流量开销
- 实现 `MockFilter`（接口 Mock 返回，用于测试）
- Filter 链组装与执行顺序控制

---

### v5.0 — 单元测试 & 工程质量

**目标：** 提升工程质量，建立测试基线

- 核心组件单元测试：动态代理、序列化、负载均衡
- Provider + Consumer 集成联调测试
- 引入 Testcontainers 支持 Zookeeper 测试环境隔离

---

### v6.0 — 异常处理 & 重试 & 熔断

**目标1：** 异常处理、重试机制，提升调用可靠性

- 异常分类：业务异常 vs 框架异常 vs 网络异常
- Consumer 端可配置重试次数（`retries`）
- 超时配置（`timeout`，单位 ms）
- 异常信息透传到 Consumer 端并还原
- 重试时自动切换节点，避免打到同一故障实例

**目标2：** 熔断机制：防止雪崩，快速失败

- 滑动时间窗口统计失败率
- 熔断状态机：`Closed → Open → Half-Open → Closed`
- 可配置：`faultLimit`（触发阈值）、`halfOpenInitialDelay`、`halfOpenDelay`
- Half-Open 状态下探测请求，成功则自动恢复

---

### v7.0 — 灰度路由

**目标：** 支持灰度发布

- 抽象 `Router` 接口
- 实现 `GrayRouter`：按比例将流量路由到灰度节点
- Provider 元数据标记：`gray: true/false`
- Consumer 配置灰度比例：`grayRatio`（0-100）

---

### v8.0 — 隐式传参（RpcContext）

**目标：** 跨调用链传递上下文，避免业务代码侵入

- `RpcContext`：ThreadLocal 存储隐式参数
- 请求头携带 context 参数透传到 Provider
- 支持 `grayId` 等流量染色标记
- 调用结束后自动清理 ThreadLocal，防止内存泄漏

---

### v9.0 — 配置中心接入 & 流量控制 

**目标：** 生产可用

- 统一配置类：`AppConfigProperties` / `ProviderConfigProperties` / `ConsumerConfigProperties`
- Provider 端 TPS 限流（滑动时间窗口，`tc` 参数，默认 30s 窗口）
- 接入 Apollo 配置中心，支持动态推送配置变更
- Maven Central 发布流程（flatten-maven-plugin）

---

### v13.0 — 自研注册中心 & Netty 传输

**目标：** 去除外部依赖，提升性能

- 实现 `MarpcRegistryCenter`（轻量 HTTP 注册中心，去除 Zookeeper 依赖）
- OkHttp → Netty 长连接传输，降低连接建立开销
- 连接池管理
- 自定义二进制协议设计（替换 HTTP+JSON，减少序列化开销）

---

### v14.0 — 可观测性（规划）

**目标：** 线上问题可追踪、可度量

- 集成 Micrometer / OpenTelemetry 调用链追踪
- 指标暴露：QPS、P99 延迟、熔断次数、重试次数
- 健康检查端点（`/marpc/health`）
- MDC 注入 traceId，日志与链路打通

---

### v15.0 — 服务治理增强（规划）

**目标：** 对齐生产级服务治理能力

- 权重路由（`WeightedRoundRobinLoadBalancer`）
- 服务分组与版本隔离（`group`、`version` 元数据）
- 优雅停机：等待在途请求完成后再注销
- Provider 限流升级：令牌桶算法替换滑动窗口