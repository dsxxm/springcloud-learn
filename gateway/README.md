# Spring Cloud Gateway 学习资料

> 学习目标：理解 Gateway 如何以 Route、Predicate、Filter 处理请求；完成静态路由、Java 路由、路由过滤、Redis 限流、Nacos 服务发现路由五个实验。
>
> 本目录的实验骨架与已有 Nacos Demo 使用同一版本线：Java 17、Spring Boot 2.7.18、Spring Cloud 2021.0.8、Spring Cloud Alibaba 2021.0.5.0。该版本使用 `spring.cloud.gateway` 配置前缀和 `spring-cloud-starter-gateway` 依赖；不要混入新版本的 `spring.cloud.gateway.server.webflux` 前缀。

## 1. Gateway 是什么

Gateway 是基于 Spring WebFlux 与 Reactor 的 API 网关。它位于客户端和微服务之间，主要处理：

```text
客户端
  → 统一入口：路由匹配、鉴权、限流、路径改写、日志
  → 下游微服务
```

Gateway 不替代 Nacos、LoadBalancer 或业务服务：

| 组件 | 职责 |
|---|---|
| Gateway | 匹配外部请求、执行过滤器、把请求转发到下游 |
| Nacos | 提供服务名对应的健康实例列表 |
| LoadBalancer | 从实例列表中选择一个实例 |
| 下游服务 | 执行业务逻辑 |

当路由目标为 `lb://nacos-user-service` 时，完整链路是：

```text
GET /user/1
  → Gateway 匹配 Route
  → GatewayFilter 改写请求（如需要）
  → LoadBalancer 通过 Nacos 找到 nacos-user-service
  → 选择 8206 或 8207 实例
  → 转发到该实例的 /user/1
  → 响应经过后置过滤器返回客户端
```

## 2. 三个核心概念

官方定义：一条 Route 由 ID、目标 URI、Predicate 集合和 Filter 集合构成；所有 Predicate 聚合为真时才匹配路由。

### Route：一条完整的转发规则

```yaml
- id: user-route
  uri: http://localhost:8206
  predicates:
    - Path=/user/**
  filters:
    - AddRequestHeader=X-Gateway, gateway-service
```

| 字段 | 含义 |
|---|---|
| `id` | 路由唯一标识，便于诊断和 Actuator 管理 |
| `uri` | 目标地址；可为固定 HTTP 地址或 `lb://服务名` |
| `predicates` | 是否匹配请求的条件集合 |
| `filters` | 匹配成功后，对请求/响应的处理链 |

### Predicate：是否进入这条 Route

Predicate 基于请求的属性匹配；同一路由的多个 Predicate 为 **AND** 关系。

| Predicate | 例子 | 适用场景 |
|---|---|---|
| `Path` | `Path=/user/{id}` | 按 URI 路径路由，最常用 |
| `Method` | `Method=GET,POST` | 仅允许特定 HTTP 方法 |
| `Header` | `Header=X-Request-Id, \d+` | 请求头满足正则才路由 |
| `Query` | `Query=username` | 必须携带指定查询参数 |
| `Cookie` | `Cookie=username,macro` | Cookie 值满足正则 |
| `Host` | `Host=**.example.com` | 多域名入口路由 |
| `After` / `Before` / `Between` | `After=2026-01-01T00:00:00+08:00[Asia/Shanghai]` | 时间窗口控制 |
| `RemoteAddr` | `RemoteAddr=192.168.1.0/24` | 按来源网段匹配 |
| `Weight` | `Weight=canary,8` | 同组路由的比例分流 |

例如：

```yaml
predicates:
  - Path=/user/**
  - Method=GET
  - Header=X-Request-Id, \d+
```

只有路径、方法、请求头三个条件同时满足才会转发。

### Filter：匹配后如何处理请求或响应

Filter 分两类：

- **GatewayFilter**：只作用于声明它的路由；
- **GlobalFilter**：作用于所有经过网关的请求。

从处理时机看又分为：

```text
Pre Filter  → 发送下游请求 → Post Filter
```

#### 常用路由过滤器

- **`StripPrefix`：删除路径前缀**

  ```text
  进入 Gateway：/mall-user/user/1
  下游收到：    /user/1
  ```

- **`PrefixPath`：添加路径前缀**

  ```text
  进入 Gateway：/1
  下游收到：    /user/1
  ```

- **`RewritePath`：按正则表达式改写路径**

  ```text
  进入 Gateway：/api/user/1
  下游收到：    /user/1
  ```

- **`AddRequestParameter`：添加请求参数**

  ```text
  原始请求：/user/getByUsername
  下游请求：/user/getByUsername?username=macro
  ```

- **`AddRequestHeader`：添加请求头**

  例如给下游服务注入 `X-Gateway-Request: gateway-service` 或追踪 ID。

- **`RequestRateLimiter`：限流**

  常配合 Redis Token Bucket；请求超过令牌额度时，默认返回 HTTP `429 Too Many Requests`。

- **`Retry`：重试**

  对指定的异常或 HTTP 5xx 状态码重试，适合短暂网络失败或下游瞬时不可用，不能替代超时控制和熔断。

## 3. 固定 URI 与 `lb://` URI

### 固定 URI：先理解路由本身

```yaml
uri: http://localhost:8206
```

适合第一个实验。它直接转发到固定服务地址，但不具备服务发现与多实例负载均衡能力。

### `lb://`：与 Nacos 结合

```yaml
uri: lb://nacos-user-service
```

`lb` 表示交由 Spring Cloud LoadBalancer 解析服务名。前提：

1. Gateway 引入 Nacos Discovery 与 LoadBalancer；
2. Gateway 配置 Nacos 地址；
3. `nacos-user-service` 已注册至同一个 Nacos Namespace / Group；
4. Nacos 中至少有一个健康实例。

`lb://` 不等于 Nacos；它是 Gateway 将 Nacos 实例列表与 LoadBalancer 选择动作连接起来的 URI 协议。

## 4. Gateway 与 mall-swarm

`mall-swarm/mall-gateway` 是成熟对照案例。它使用：

```yaml
- id: mall-admin
  uri: lb://mall-admin
  predicates:
    - Path=/mall-admin/**
  filters:
    - StripPrefix=1
```

请求：

```text
/mall-admin/admin/login
```

处理过程：

```text
Path=/mall-admin/** 匹配
  → StripPrefix=1 删除 /mall-admin
  → 下游路径成为 /admin/login
  → lb://mall-admin 通过 Nacos + LoadBalancer 选择 mall-admin 实例
```

Gateway 还在该项目中承担 Sa-Token 全局鉴权与 Knife4j 文档聚合。学习时先分清路由、过滤、鉴权三个边界；不要把它们都看成“转发”。

## 5. Redis 限流

`RequestRateLimiter` 的 Redis 实现使用 Token Bucket：

| 参数 | 含义 |
|---|---|
| `replenishRate` | 每秒补充令牌数，即稳定速率 |
| `burstCapacity` | 桶最多可容纳的令牌数，即短时突发上限 |
| `requestedTokens` | 单个请求消耗的令牌数 |
| `key-resolver` | 如何划分限流桶，例如按 IP、用户或 API Key |

例如：

```yaml
filters:
  - name: RequestRateLimiter
    args:
      key-resolver: "#{@ipKeyResolver}"
      redis-rate-limiter.replenishRate: 1
      redis-rate-limiter.burstCapacity: 2
      redis-rate-limiter.requestedTokens: 1
```

这表示每个 IP 稳定每秒 1 个请求，最多允许积累 2 个令牌；超过限额默认返回 HTTP 429。`RequestRateLimiter` 必须使用 `name` / `args` 的完整形式，不能使用快捷写法。

部署在 Nginx、SLB 或 Kubernetes Ingress 后面时，不能直接把 TCP 连接的 remote address 当真实用户 IP；需先配置可信代理与转发头处理，否则多个用户可能被误判为同一个代理 IP。

## 6. 学习顺序

按 [`实验手册.md`](./实验手册.md) 完成：

1. 固定 URI 的 YAML 路由；
2. `RouteLocator` Java 路由；
3. Predicate 路由匹配；
4. `StripPrefix`、`PrefixPath`、`AddRequestParameter`；
5. Redis `RequestRateLimiter`；
6. Nacos Discovery + `lb://`；
7. 对照 `mall-swarm/mall-gateway` 的路由与 Sa-Token 全局鉴权。

## 7. 参考资料

- [Spring Cloud Gateway 官方：术语表](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/glossary.html)
- [Spring Cloud Gateway 官方：Route Predicate Factories](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html)
- [Spring Cloud Gateway 官方：Route 与 Filter 配置](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/configuring-route-predicate-factories-and-filter-factories.html)
- [Spring Cloud Gateway 官方：RequestRateLimiter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html)
- [Macrozheng Gateway 课程](https://www.yuque.com/macrozheng/mall-swarm-learning/io0zoyrf5y0b2mf6)
