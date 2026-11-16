# JWT 学习：短期访问令牌、刷新令牌与 Gateway

## 学习边界

本模块学习自建认证服务的双令牌会话：**短期 JWT access token + 不透明 refresh token + Redis 会话状态**。它不是 OAuth 2.0 / OpenID Connect 授权服务器，也不替代 Spring Authorization Server。

本仓库现有 Gateway 使用 Java 17、Spring Boot 2.7.18、Spring Cloud 2021.0.8。`jwt/demo/` 仅是 Maven 骨架；没有生成启动类、接口、YAML 或业务代码。完整的可复制实验代码在[实验手册](./实验手册.md)。

## JWT 是什么，不是什么

JWT 是承载声明的紧凑格式。常见 JWS 形态为：

```text
base64url(header).base64url(payload).base64url(signature)
```

签名保护完整性和来源，**不加密 payload**。因此 JWT 中不能放密码、身份证、完整权限列表等敏感或频繁变化的数据。最小 access token 通常只含：

```json
{
  "iss": "jwt-auth-service",
  "aud": "gateway-service",
  "sub": "user-42",
  "scope": "profile.read",
  "sid": "refresh-family-id",
  "jti": "access-token-id",
  "iat": 1760000000,
  "nbf": 1760000000,
  "exp": 1760000600
}
```

校验不仅是验签：资源服务器必须限制允许算法，并验证 `iss`、`aud`、`exp`、`nbf`、令牌类型和权限范围。JWT 的 `alg` 头是攻击者可控输入，不能据此自由选择验签算法。

## 你记得的“双 Token + Redis”到底是什么

JWT **不要求**双 token 或 Redis。双令牌是为兼顾短期 API 凭证与长期登录体验的会话设计：

| 项目 | access token | refresh token |
|---|---|---|
| 形式 | 签名 JWT | 高熵随机、不透明字符串 |
| 发给谁 | Gateway / API | 仅认证服务的刷新端点 |
| 有效期 | 5–15 分钟 | 7–30 天，另设闲置与绝对期限 |
| 作用 | 每个受保护 API 的认证 | 换取一对新 token |
| 服务端状态 | 可近似无状态；登出时可查 `jti` 拒绝表 | 必须有状态，保存当前值、家族和过期时间 |

推荐将 refresh token 设计成 `familyId.secret`，只把 `SHA-256(secret)` 存在 Redis：

```text
客户端持有: familyId.rawRandomSecret
Redis:
  auth:refresh:{familyId} = SHA-256(rawRandomSecret)
  TTL = refresh 绝对期限
  auth:deny:{access-jti} = 1
  TTL = access token 剩余寿命
```

刷新时必须原子完成：比较当前 hash → 替换为新 hash → 返回新 access token 和新 refresh token。旧 refresh token 再次出现说明可能被窃取；删除整个 family，要求用户重新登录。不要把旧 refresh token 加入“已用列表”后无限保留。

```text
登录
  → 认证服务签发 AT-1(JWT) 与 RT-1(opaque)
  → Redis 保存 RT-1 hash
API
  → Gateway 校验 AT-1：签名、iss、aud、exp、deny:jti
  → lb:// 服务名 → 下游业务
刷新
  → 认证服务原子兑换 RT-1
  → AT-2 + RT-2，Redis 只保留 RT-2 hash
重放 RT-1
  → 撤销 family，返回 401，重新登录
```

## Gateway 的责任边界

```text
客户端 Authorization: Bearer <access JWT>
  → Gateway 全局过滤器验证 access JWT
  → 401：缺失、格式错误、过期、验签失败、撤销
  → 路由 Path=/mall-user/**
  → StripPrefix=1
  → lb://mall-user，经 Nacos + LoadBalancer 选实例
  → 下游 /user/**
```

- **认证服务**：校验凭证、签发、刷新、撤销 refresh family。
- **Gateway**：验证 access token、在可信边界内添加用户上下文头、阻断未认证请求。
- **下游服务**：仍应验证来自 Gateway 的内部身份凭证或只允许内网访问；不能因为“Gateway 做过鉴权”而对公网裸露。
- **Redis**：刷新令牌状态、撤销状态和可选会话元数据；不是 JWT 的签名密钥仓库。

不要把 refresh token 转发给业务服务，也不要在 Gateway 自动刷新 token：刷新请求应显式发送给认证服务，避免在代理层隐式改写响应和 Cookie。

## 本仓库现状

| 项目 | 状态 | 证据 |
|---|---|---|
| Spring Cloud Gateway | 已实现 | `gateway/demo/gateway-service/pom.xml` 含 `spring-cloud-starter-gateway`，并有路由配置 |
| Nacos + LoadBalancer 路由 | 已实现 | Gateway POM 与 `application-nacos.yml` |
| Redis 限流 | 已实现 | Gateway POM 含 reactive Redis，`RedisRateLimiterConfig` 与限流 profile |
| JWT 认证、签发、刷新、撤销 | 未实现 | 现有 Gateway POM 未含 JWT 库；未发现认证过滤器或 token 端点 |
| Redis 认证会话 | 未实现 | 当前 Redis 用途是 Gateway 限流，不是身份会话 |

## 参考

- [RFC 7519 — JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519.html)：令牌结构与注册声明。
- [RFC 8725 — JWT Best Current Practices](https://www.rfc-editor.org/rfc/rfc8725.html)：算法限制、令牌类型、输入验证。
- [RFC 9700 — OAuth 2.0 Security BCP](https://www.rfc-editor.org/rfc/rfc9700.html)：公共客户端 refresh token 重放防护和 rotation 要求。
- [Spring Cloud Gateway 术语表](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/glossary.html)：Route、Predicate、Filter 边界。
