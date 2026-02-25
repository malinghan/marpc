# sorpc - Lightweight Java RPC Framework

## Project Overview

**sorpc** is an educational RPC (Remote Procedure Call) framework built with Spring Boot 3.2.3 and Java 17. It demonstrates core distributed system concepts including service discovery, load balancing, fault tolerance, and traffic management.

**Maven Coordinates:** `io.github.malinghan:sorpc:0.0.3`

## Project Structure

```
sorpc/
├── sorpc-core/              # Core RPC framework (~2,792 LOC)
├── sorpc-demo-api/          # Shared service interfaces
├── sorpc-demo-provider/     # Provider demo application
├── sorpc-demo-consumer/     # Consumer demo application
└── pom.xml                  # Parent POM
```

## Technology Stack

- **Java 17** with Spring Boot 3.2.3
- **Serialization:** Fastjson 1.2.83 & Fastjson2 2.0.47
- **HTTP Client:** OkHttp 4.5.0
- **Service Registry:** Apache Curator 5.1.0 (Zookeeper) + Custom HTTP Registry
- **Config Center:** Apollo Client 2.2.0
- **Build:** Maven with flatten-maven-plugin

## Core Components

### Provider Side
- **ProviderBootStrap**: Service registration and lifecycle management
- **ProviderInvoker**: Request handling with traffic control (sliding time window)
- **SpringBootTransport**: HTTP endpoint (`/sorpc`) for RPC requests

### Consumer Side
- **ConsumerBootStrap**: Dynamic proxy creation for RPC interfaces
- **SoInvocationHandler**: Proxy handler with retry, circuit breaker, and fault isolation
- **Filter Chain**: Pre/post processing (Cache, Mock, Context Parameters)

### Service Governance
- **Registry Centers**: ZkRegistryCenter (Zookeeper), SoRegistryCenter (HTTP)
- **Load Balancers**: Random, RoundRobin
- **Routers**: GrayRouter (gray release with configurable ratio)
- **Circuit Breaker**: Half-open recovery with sliding time window
- **Traffic Control**: Provider-side TPS limiting

## Key Annotations

- `@EnableSorpc`: Enable RPC framework
- `@SoRpcProvider`: Mark service implementations
- `@SoRpcConsumer`: Inject RPC client proxies
- `@SoRpcScan`: Custom package scanning

## Configuration

### Provider Configuration
```yaml
sorpc:
  app:
    id: soapp1
    env: dev
    namespace: public
  provider:
    metas:
      dc: bj
      gray: false
      tc: 20  # Traffic control: 20 req/30s
```

### Consumer Configuration
```yaml
sorpc:
  consumer:
    retries: 1
    timeout: 1000
    grayRatio: 33  # 33% gray traffic
    faultLimit: 10  # Circuit breaker threshold
    halfOpenInitialDelay: 10000
    halfOpenDelay: 60000
```

## Important Conventions

1. **Method Signatures**: Overloaded methods use signature format: `methodName@paramCount_type1_type2`
2. **Registry Paths**: `/{app}_{namespace}_{env}_{serviceName}/{host}_{port}`
3. **Context Parameters**: Thread-local implicit parameters via `RpcContext`
4. **Traffic Control**: Sliding time window (default 30s) for rate limiting and fault detection

## Development Workflow

### Running Provider
```bash
cd sorpc-demo-provider
mvn spring-boot:run
```

### Running Consumer
```bash
cd sorpc-demo-consumer
mvn spring-boot:run
```

### Running Tests
```bash
mvn test
```

### Building
```bash
mvn clean install
```

## Key Files Reference

**Core Entry Points:**
- Provider: `sorpc-core/src/main/java/com/so/sorpc/core/provider/ProviderBootStrap.java`
- Consumer: `sorpc-core/src/main/java/com/so/sorpc/core/consumer/ConsumerBootStrap.java`
- Transport: `sorpc-core/src/main/java/com/so/sorpc/core/transport/SpringBootTransport.java`

**Registry Implementations:**
- Zookeeper: `sorpc-core/src/main/java/com/so/sorpc/core/registry/zk/ZkRegistryCenter.java`
- HTTP: `sorpc-core/src/main/java/com/so/sorpc/core/registry/so/SoRegistryCenter.java`

**Configuration:**
- Provider Config: `sorpc-core/src/main/java/com/so/sorpc/core/config/ProviderConfig.java`
- Consumer Config: `sorpc-core/src/main/java/com/so/sorpc/core/config/ConsumerConfig.java`

## Design Patterns

- **Strategy Pattern**: LoadBalancer, Router, Filter
- **Proxy Pattern**: Consumer-side dynamic proxies
- **Chain of Responsibility**: Filter chain
- **Observer Pattern**: Registry change notifications
- **Sliding Window Algorithm**: Traffic control and fault detection

## Version History

- **v1.0-v4.0**: Basic RPC, method overloading, type conversion, load balancing
- **v5.0**: Code refactoring
- **v6.0**: Filter mechanism
- **v7.0**: Dependency optimization, unit testing
- **v8.0**: Exception handling, retry
- **v9.0**: Circuit breaker
- **v10.0**: Gray routing
- **v11.0**: Context parameters
- **v12.0**: Unified config, traffic control, Apollo integration, Maven Central publishing
- **v13.0 (Planned)**: so-registry integration, Netty transport

## Working with This Project

When making changes:
1. Core framework code is in `sorpc-core/`
2. Test changes using demo applications in `sorpc-demo-provider/` and `sorpc-demo-consumer/`
3. Follow existing patterns for filters, load balancers, and routers
4. Update configuration properties in `AppConfigProperties`, `ProviderConfigProperties`, `ConsumerConfigProperties`
5. Run tests with `mvn test` before committing