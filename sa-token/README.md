# mall-swarm 的 Sa-Token 与 Gateway

## 结论先行

`mall-swarm` 的 Gateway 用 **Sa-Token Reactor** 在网关统一做登录与权限检查，用 **Redis** 在网关和服务间共享登录状态，并通过 Spring Cloud Gateway 的 `lb://` 路由转发到业务服务。它不是“把 JWT 放进 Gateway 就完成鉴权”。

当前 `mall-swarm` `master` 是另一条版本线：Spring Boot 3.5 / Spring Cloud 2025，Gateway 使用 `spring-cloud-starter-gateway-server-webflux` 与 `sa-token-reactor-spring-boot3-starter`。本仓库是 Java 17 / Spring Boot 2.7.18 / Spring Cloud 2021.0.8，因此骨架使用 Boot 2 的 `sa-token-reactor-spring-boot-starter`；两者不能直接复制依赖名或 Gateway 配置前缀。

`sa-token/demo/` 只提供 Maven 骨架，不生成应用代码或配置。完整实验文件在[实验手册](./实验手册.md)。

## mall-swarm 的已验证结构

来自 `mall-swarm/master` 的 `mall-gateway/pom.xml`：

- `spring-cloud-starter-gateway-server-webflux`
- `sa-token-reactor-spring-boot3-starter`
- `sa-token-redis-jackson`
- `sa-token-jwt`
- Redis、Nacos Discovery / Config

`mall-gateway/src/main/resources/application.yml` 定义了：

```text
/mall-admin/**  → StripPrefix=1 → lb://mall-admin
/mall-portal/** → StripPrefix=1 → lb://mall-portal
/mall-auth/**   → StripPrefix=1 → lb://mall-auth
```

并配置 `Authorization: Bearer <token>`、30 天 `timeout`、Redis 和白名单。Gateway 对照代码使用 `SaReactorFilter`：管理端调用 `StpUtil.checkLogin()`，商城会员调用独立的 `StpMemberUtil.checkLogin()`，然后从 Redis 读取路径到权限资源的映射并执行权限校验。

> POM 依赖本身不能证明 token 模式；但 Gateway 源码中的 `StpMemberUtil` 明确创建了 `StpLogicJwtForSimple("memberLogin")`，所以商城会员使用 JWT **Simple** 模式，仍保留 Sa-Token DAO 状态。`token-style: uuid` 不是 Stateless JWT 的证据；是否无状态必须以实际 `StpLogic` 和运行配置判断。

## 请求链路

```text
GET /mall-admin/product/list
Authorization: Bearer <admin-token>
  → mall-gateway 的 SaReactorFilter
  → 白名单？否则 OPTIONS 放行或 StpUtil.checkLogin()
  → 查询该 path 的需要权限
  → StpUtil.checkPermissionOr(...)
  → Path=/mall-admin/** 匹配
  → StripPrefix=1，路径变为 /product/list
  → lb://mall-admin → Nacos 实例 → mall-admin
  → 响应返回
```

前台会员与后台管理员不能共用 login type：

| 访问者 | 路径 | Sa-Token 逻辑 |
|---|---|---|
| 后台管理员 | `/mall-admin/**` | `StpUtil` 默认账号体系 |
| 商城会员 | `/mall-portal/**` | `StpMemberUtil` 独立账号体系 |

否则相同用户 ID 可在两个身份域之间被误认为同一个登录会话。

## Redis、JWT 与 Sa-Token 的关系

Sa-Token 默认的 DAO token / session 模式依赖服务端状态，Redis 使多实例共享 token、会话、踢人和权限缓存。JWT 插件提供不同模式：

- **Simple**：JWT 仅承载 token 值，仍查询 Sa-Token DAO；保留强制下线等能力。
- **Mixin**：JWT 含 loginId，同时保存部分状态。
- **Stateless**：仅靠 JWT，注销、踢人、并发会话和权限即时变更会受限，必须自行设计撤销策略。

对电商后台，优先理解并使用 Redis session 模式；不要因为“JWT 无状态”而失去主动登出、踢人和权限变更后的即时失效能力。

## 本仓库现状

| 项目 | 状态 | 证据 |
|---|---|---|
| Gateway 路由、Nacos、Redis 限流 | 已实现 | `gateway/demo/gateway-service` POM、配置和 Java 类 |
| Sa-Token 依赖与 `SaReactorFilter` | 未实现 | 当前 Gateway POM 没有 `cn.dev33` 依赖，源码没有 Sa-Token 配置 |
| JWT 自建认证 | 未实现 | 当前 Gateway 无 JWT 验签过滤器与签发端点 |
| mall-swarm 运行代码 | 文档对照 | 本仓库未包含 `mall-swarm` 源码；分析引用远程 `master` 源 |

## 参考

- [mall-swarm Gateway POM（master）](https://raw.githubusercontent.com/macrozheng/mall-swarm/master/mall-gateway/pom.xml)
- [mall-swarm Gateway 配置（master）](https://raw.githubusercontent.com/macrozheng/mall-swarm/master/mall-gateway/src/main/resources/application.yml)
- [mall-swarm `StpMemberUtil`（JWT Simple 模式）](https://raw.githubusercontent.com/macrozheng/mall-swarm/master/mall-gateway/src/main/java/com/macro/mall/util/StpMemberUtil.java)
- [mall-swarm Sa-Token 升级说明](https://www.macrozheng.com/roadmap/mall_swarm_update_30.html)
- [Sa-Token：Gateway 统一鉴权](https://sa-token.com/micro/gateway-auth.html)
- [Sa-Token：内部服务外网隔离 / Same-Token](https://sa-token.com/micro/same-token.html)
- [Spring Cloud Gateway 过滤器](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories.html)
