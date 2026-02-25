# marpc

基于 Spring Boot 4.0 + Java 17 的轻量级教学型 RPC 框架。

## 功能清单

### 核心传输
| 功能 | 状态 |
|------|------|
| JDK 动态代理 + OkHttp HTTP 调用 | ✅ |
| Netty 长连接传输（自定义二进制协议） | ✅ |
| OkHttp / Netty 可配置切换 | ✅ |
| 方法重载支持（方法签名规范化） | ✅ |
| 复杂类型序列化/反序列化 | ✅ |

### 服务注册与发现
| 功能 | 状态 |
|------|------|
| 抽象 RegistryCenter 接口 | ✅ |
| Zookeeper 注册中心（Curator） | ✅ |
| Provider 启动注册 / 关闭注销 | ✅ |
| Consumer 订阅变更，动态刷新实例列表 | ✅ |
| 自研 HTTP 注册中心（maregistry） | 🔲 |

### 负载均衡
| 功能 | 状态 |
|------|------|
| 随机负载均衡（RandomLoadBalancer） | ✅ |
| 轮询负载均衡（RoundRobinLoadBalancer） | ✅ |
| 权重轮询（WeightedRoundRobin） | 🔲 |

### 容错与可靠性
| 功能 | 状态 |
|------|------|
| 可配置重试次数 + 超时 | ✅ |
| 重试时自动切换节点 | ✅ |
| 熔断器（滑动窗口 + 状态机） | ✅ |
| 优雅停机 | 🔲 |

### 流量管理
| 功能 | 状态 |
|------|------|
| 灰度路由（按比例路由到灰度节点） | ✅ |
| RpcContext 隐式传参（ThreadLocal 透传） | ✅ |
| Provider TPS 限流（滑动时间窗口） | 🔲 |
| 权重路由 | 🔲 |
| 服务分组与版本隔离 | 🔲 |

### Filter 机制
| 功能 | 状态 |
|------|------|
| 可插拔 Filter 链（preFilter / postFilter） | ✅ |
| CacheFilter（Consumer 端结果缓存） | ✅ |
| MockFilter（接口 Mock 返回） | ✅ |

### 异常处理
| 功能 | 状态 |
|------|------|
| 统一异常体系（业务 / 框架 / 网络） | ✅ |
| 异常信息透传到 Consumer 端 | ✅ |

### 配置
| 功能 | 状态 |
|------|------|
| YAML 配置驱动 | ✅ |
| Apollo 配置中心接入 | 🔲 |
| 自研配置中心（maconfig）接入 | 🔲 |

### 可观测性
| 功能 | 状态 |
|------|------|
| 调用日志（重试、熔断、路由） | ✅ |
| RpcContext 链路标记（grayId、traceId） | ✅ |
| Micrometer / OpenTelemetry 接入 | 🔲 |
| QPS / P99 / 熔断次数指标暴露 | 🔲 |

---

## 快速开始

**启动 Provider：**
```bash
cd marpc-demo-provider && mvn spring-boot:run
```

**启动 Consumer：**
```bash
cd marpc-demo-consumer && mvn spring-boot:run
```

**切换 Netty 传输（Provider + Consumer 均需配置）：**
```yaml
marpc:
  transport: netty
  netty:
    port: 9090
    server:
      enabled: true   # 仅 Provider 开启
```

---

## 版本历史

| 版本 | 主要内容 |
|------|---------|
| v1.0 | 基础 RPC 骨架，动态代理 + OkHttp |
| v2.0 | 方法重载、类型转换、注册中心、负载均衡 |
| v3.0 | 多模块拆分、统一异常体系 |
| v4.0 | Filter 机制、Testcontainers 测试隔离 |
| v5.0 | 重试、熔断、灰度路由 |
| v6.0 | RpcContext 隐式传参、Netty 传输层 |
