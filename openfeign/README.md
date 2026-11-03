# Spring Cloud OpenFeign 学习资料

> 本资料按课程路线学习：`feign-service` 通过 OpenFeign 调用 `nacos-user-service`，借助 Nacos + LoadBalancer 负载均衡；使用 Spring Cloud Circuit Breaker + Resilience4j 实现服务降级；最后开启 Feign 日志。

## 1. OpenFeign 是什么

OpenFeign 是声明式 HTTP 服务调用工具。开发者定义一个带 Spring MVC 注解的 Java 接口，`@FeignClient` 把接口绑定到目标服务；运行时 Spring 创建动态代理，方法调用被转换为 HTTP 请求。

```java
@FeignClient(value = "nacos-user-service")
public interface UserService {

    @GetMapping("/user/{id}")
    CommonResult<User> getUser(@PathVariable("id") Long id);
}
```

业务代码调用：

```java
userService.getUser(1L);
```

而不是手写：

```java
restTemplate.getForObject(
    "http://nacos-user-service/user/{id}",
    CommonResult.class,
    id);
```

OpenFeign 简化的是客户端代码；网络超时、下游错误、幂等性、重试放大、降级策略仍需要明确设计。

## 2. 完整调用链

```text
GET feign-service:8701/user/1
  → UserFeignController.getUser(1)
  → UserService.getUser(1)
  → OpenFeign 动态代理根据 @GetMapping 构造 GET /user/1
  → LoadBalancer 查询 nacos-user-service 的实例列表
  → Nacos 返回健康实例 8206 / 8207
  → LoadBalancer 选择一个实例
  → 请求该实例的 /user/1
  → Decoder 解析 CommonResult<User>
  → feign-service 返回结果
```

### 组件职责

- **OpenFeign**：创建接口代理、依据注解构造 HTTP 请求、编码参数、解码响应。
- **Nacos**：提供 `nacos-user-service` 的健康实例列表。
- **LoadBalancer**：从实例列表选择一个 IP:Port。
- **Circuit Breaker**：在调用失败或断路时触发 fallback。
- **下游 User Service**：执行用户 CRUD 业务。

## 3. 课程模块与依赖

```text
springcloud-learn/openfeign/demo/
├── feign-service:8701            # OpenFeign + Resilience4j fallback
├── loadbalancer-service:8601     # @LoadBalanced RestTemplate
└── sentinel-feign-service:8801   # Sentinel + OpenFeign fallback
```

核心依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

LoadBalancer 是 OpenFeign 通过服务名调用多个 Nacos 实例时的选址组件；Circuit Breaker 是降级所需的容错组件。

## 4. 参数绑定规则

### 参数绑定

- **路径变量**：`@PathVariable("id") Long id`
- **查询参数**：`@RequestParam("username") String username`
- **JSON 请求体**：`@RequestBody User user`
- **请求头**：`@RequestHeader("Authorization") String token`，或使用 `RequestInterceptor` 统一注入。

`@PathVariable`、`@RequestParam` 的值不要省略。Feign 需要把 Java 参数名与 HTTP 模板变量、查询参数名称准确对应。

## 5. LoadBalancer 与 OpenFeign

OpenFeign 不保存服务实例。`@FeignClient(value = "nacos-user-service")` 中的 `value` 是服务名：

```text
nacos-user-service
```

它必须与提供者的：

```yaml
spring:
  application:
    name: nacos-user-service
```

一致。

当 Nacos 有两个实例时：

```text
nacos-user-service:8206
nacos-user-service:8207
```

连续请求 Feign 服务，LoadBalancer 会选择实例。默认策略通常是轮询；具体选择结果要通过提供者日志确认。

LoadBalancer 可以独立用于 `@LoadBalanced RestTemplate`，并不依赖 OpenFeign；但 OpenFeign 按服务名调用时需要它完成实例选择。

## 6. Circuit Breaker 服务降级

课程的 OpenFeign fallback 使用 Spring Cloud Circuit Breaker + Resilience4j：

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

Feign 接口指定 fallback：

```java
@FeignClient(
    value = "nacos-user-service",
    fallback = UserFallbackService.class
)
public interface UserService { }
```

`UserFallbackService` 必须实现 `UserService` 并注册为 Spring Bean。它应该返回可解释的降级结果，而不是把错误伪装为正常业务数据。

Circuit Breaker fallback 与 Sentinel fallback 不是同一机制：前者使用 Resilience4j，后者由 Sentinel 的 OpenFeign 适配启用。二者不要同时作为同一个 Feign Client 的降级方案。

## 7. Feign 日志

Feign 日志级别：

### 日志级别

- **`NONE`**：默认，不输出 Feign 日志。
- **`BASIC`**：方法、URL、响应状态码、耗时。
- **`HEADERS`**：BASIC 加请求/响应头。
- **`FULL`**：HEADERS 加请求/响应体与元数据。

启用 `FULL` 需要两步：定义 `Logger.Level.FULL` Bean，并将具体 Feign 接口日志级别设为 `DEBUG`。生产环境不要长期记录 FULL，避免 Token、密码和个人数据进入日志。

## 8. mall-swarm 对照

`mall-swarm/mall-demo`：

```java
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class MallDemoApplication { }
```

并声明：

```java
@FeignClient("mall-admin")
public interface FeignAdminService {
    @PostMapping("/admin/login")
    CommonResult login(@RequestBody UmsAdminLoginParam loginParam);
}
```

`mall-auth` 也通过 Feign 调用 `mall-admin`、`mall-portal` 的登录接口。项目当前未配置 Circuit Breaker fallback；课程实验会补齐这部分。

## 9. Sentinel 与 OpenFeign

Sentinel **不依赖** OpenFeign；它可以独立保护 Web、RPC 和任意资源。本学习路径将 Sentinel 放在 OpenFeign 后面，是为了用已有的 Feign 调用演示流控、熔断与 fallback。

```text
feign-service
  → Resilience4j Circuit Breaker fallback

sentinel-feign-service
  → Sentinel Flow / Degrade Rule fallback
```

Sentinel 实验使用：

```text
spring-cloud-starter-alibaba-sentinel
feign.sentinel.enabled=true
@FeignClient(fallback = UserFallbackService.class)
```

二者是两条独立实验路线。不要在同一个 Feign Client 同时启用 Resilience4j 与 Sentinel 的 fallback。

### Sentinel 要学习什么

- Resource：被保护的接口、方法或 Feign 调用；
- Flow Rule：基于 QPS 或并发线程流控；
- Degrade Rule：基于慢调用、异常比例、异常数熔断；
- Hotspot Rule：针对热点参数限流；
- System Rule：基于系统 load、CPU、线程数保护；
- Dashboard：观察资源和规则；生产规则应持久化到 Nacos。

完整代码、Dashboard 配置和验证步骤均在同一份 [`实验手册.md`](./实验手册.md) 后半部分。

## 10. 参考资料

- [Spring Cloud OpenFeign 官方参考](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)
- [Spring Cloud OpenFeign 配置属性](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/appendix.html)
- [OpenFeign 面试题来源](https://codewithvenu.com/blog/Interview-Tracks/framework-interview/Spring-Cloud/04-OpenFeign-QA)
- Macrozheng OpenFeign 课程（用户提供原文）
- `mall-swarm/mall-demo`、`mall-swarm/mall-auth`
