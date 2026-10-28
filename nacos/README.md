# Nacos 学习资料：按 Spring Cloud Alibaba 课程实践

> 本资料按你提供的课程路线学习：使用 Nacos 作为注册中心和配置中心，创建 `nacos-user-service`、`nacos-remote-service`、`nacos-config-client` 三个模块。

## 1. Nacos 是什么

Nacos（Dynamic Naming and Configuration Service）用于发现、配置和管理微服务。Spring Cloud Alibaba 中，它最常见的两个角色是：

| 角色 | 解决的问题 | 你将在课程中看到的结果 |
|---|---|---|
| 注册中心 | 调用方不应写死服务 IP、端口；实例扩容、下线后地址会变化 | `nacos-remote-service` 通过服务名调用多个 `nacos-user-service` 实例 |
| 配置中心 | 配置分散在各服务中，修改后往往需要重新部署 | 修改 Nacos 控制台中的配置后，`nacos-config-client` 不重启就能读到新值 |

Nacos 不是网关，也不负责真正的 HTTP 负载均衡选择。它保存服务实例、健康状态、配置和元数据；Spring Cloud 客户端获得实例列表后再通过负载均衡组件选择实例。

## 2. Nacos 的功能

1. **服务发现和健康检查**：服务注册后，消费者按服务名发现实例；Nacos 实时检查健康状态，避免请求不健康实例。
2. **动态配置服务**：以中心化、外部化、动态化方式管理不同环境的应用与服务配置。
3. **动态 DNS 服务**：支持权重路由、灵活路由、流量控制，以及数据中心内网 DNS 解析。
4. **服务和元数据管理**：统一管理服务、实例、集群和元数据。

## 3. 本课程的实验拓扑

```text
                         ┌───────────────────┐
                         │ Nacos :8848       │
                         │ 注册中心 + 配置中心 │
                         └───────▲───────────┘
                 注册/发现      │       配置读取/监听
        ┌────────────────────────┼──────────────────────────┐
        │                        │                          │
┌───────┴──────────┐    ┌────────┴─────────┐     ┌──────────┴──────────┐
│ nacos-user-service│    │nacos-remote-service│     │nacos-config-client │
│ :8206 / :8207     │◄───│ :8308              │     │ :9101              │
└──────────────────┘    └────────────────────┘     └─────────────────────┘
```

- 启动两个 `nacos-user-service` 实例，端口分别使用 `8206`、`8207`；
- `nacos-remote-service` 使用服务名 `nacos-user-service` 发起远程调用；
- `nacos-config-client` 从 Nacos 读取 dev 环境配置，并用 `@RefreshScope` 接收变更。

## 4. 搭建 Nacos Server

课程使用 `nacos-server-2.3.0.zip`，可从 [Nacos Releases](https://github.com/alibaba/nacos/releases) 下载。

1. 安装并配置 JDK，设置 `JAVA_HOME`；
2. 解压 Nacos；
3. 在 Nacos 的 `bin` 目录以单机模式启动：

```bash
# Windows
startup.cmd -m standalone

# Linux / macOS
sh startup.sh -m standalone
```

4. 打开 <http://localhost:8848/nacos/index.html>。

检查点：Nacos 管理页面可以访问。课程的前半段不要求开启鉴权。

## 5. 注册中心：创建两个服务模块

完整的逐步动手说明在 [`实验手册.md`](./实验手册.md)。这里先掌握关键点。

### 5.1 公共 BOM 和服务发现依赖

父工程使用 Spring Cloud Alibaba BOM：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-alibaba-dependencies</artifactId>
      <version>${spring-cloud-alibaba.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

两个服务都添加：

```xml
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

### 5.2 `nacos-user-service` 注册配置

```yaml
server:
  port: 8206
spring:
  application:
    name: nacos-user-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
management:
  endpoints:
    web:
      exposure:
        include: '*'
```

复制一份配置并将端口改为 `8207`，启动第二个实例。Nacos 控制台应显示一个 `nacos-user-service`，其下有两个实例。

### 5.3 `nacos-remote-service` 通过服务名调用

`nacos-remote-service` 用 `RestTemplate` 访问：

```text
http://nacos-user-service/user/{id}
```

这个 URL 的主机名不是 DNS 域名，而是服务名。它必须与提供者的 `spring.application.name` 一致。为 `RestTemplate` 添加 `@LoadBalanced` 后，Spring Cloud 会从 Nacos 实例列表中选择一个目标实例。

多次访问远程服务接口，观察两个 user-service 的日志交替输出，即完成负载均衡验证。

## 6. 配置中心：创建 `nacos-config-client`

### 6.1 依赖

```xml
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<!-- 从 2021.0.5 起，Spring Cloud 不再默认包含 Bootstrap 支持 -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

### 6.2 两个配置文件

`application.yml` 选择 dev 环境：

```yaml
spring:
  profiles:
    active: dev
```

`bootstrap.yml` 在主应用配置前加载 Nacos 地址和配置格式：

```yaml
server:
  port: 9101
spring:
  application:
    name: nacos-config-client
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
```

### 6.3 Data ID 推导

课程的 Data ID 默认格式：

```text
${spring.application.name}-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
```

因此本实验必须在 Nacos 控制台的 `DEFAULT_GROUP` 创建：

```text
Data ID: nacos-config-client-dev.yaml
配置格式: YAML
```

内容：

```yaml
config:
  info: "config info for dev"
```

### 6.4 动态刷新

Controller 标注 `@RefreshScope`，用 `@Value("${config.info}")` 读取属性并暴露 `/configInfo`。第一次请求应返回 `config info for dev`。在控制台修改 `config.info` 并发布后，不重启客户端再次请求；应返回新值。

## 7. Nacos 开启鉴权

课程中可通过 Nacos `conf/application.properties` 开启认证：

```properties
nacos.core.auth.system.type=nacos
nacos.core.auth.enabled=true
nacos.core.auth.server.identity.key=example
nacos.core.auth.server.identity.value=example
nacos.core.auth.plugin.nacos.token.secret.key=SecretKey012345678901234567890123456789012345678901234567890123456789
```

重启 Nacos 后，课程版本使用 `nacos / nacos` 登录控制台。客户端连接 Nacos 时也要配置凭据：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos
```

生产环境必须使用自行生成的 Token、独立强密码、网络隔离和 HTTPS/TLS；示例凭据仅用于本地课程。

## 8. 常见问题

| 现象 | 排查顺序 |
|---|---|
| Nacos 页面打不开 | `JAVA_HOME`、启动命令、8848 端口、Nacos 日志 |
| 控制台没有服务 | `spring.application.name`、`server-addr`、discovery starter、应用启动日志 |
| 远程调用找不到 `nacos-user-service` | 服务名是否一致、提供者是否已注册、`RestTemplate` 是否 `@LoadBalanced` |
| 负载均衡没有轮换 | 是否实际启动了两个相同服务名但不同端口的实例；多次请求并观察两个实例日志 |
| 配置读取失败 | `bootstrap.yml`、dev profile、Data ID、Group、`file-extension` 是否完全匹配 |
| 修改配置未刷新 | 是否发布成功，Bean 是否 `@RefreshScope`，访问的是否为同一个 Data ID |

## 9. 参考资料

- [Nacos 官方：Nacos 融合 Spring Cloud](https://www.nacos.io/docs/v3.1/ecology/use-nacos-with-spring-cloud/)
- [Nacos 官方：配置管理概览](https://nacos.io/en/docs/next/manual/user/config/overview/)
- [Nacos 官方：服务发现概览](https://nacos.io/en/docs/latest/manual/user/naming/overview/)
- [Macrozheng 课程：Nacos 作为注册中心和配置中心](https://www.yuque.com/macrozheng/mall-swarm-learning/io0zoyrf5y0b2mf6)
