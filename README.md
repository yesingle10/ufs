# UFS Core · 公共文件服务核心

UFS（Unified File Service）是面向跨境电商的**多云对象存储抽象层**，统一屏蔽阿里云 OSS / 七牛 Kodo / 火山 TOS 的差异，对外提供一致的桶、对象、策略、回收站、监控、审计能力。

> 本工程是 **Java 后端脚手架（架子）**，按《UFS 研发规范手册》7 份规范编写：Java 编码、SQL/DDL、接口契约、前端 TS、安全、监控告警、提交规范。

- 语言/框架：**Java 17 + Spring Boot 3.2.5**
- 构建：**Maven 3.9.x**（本仓库未强制携带 `mvnw`，可用本地 `mvn` 或见下方“本地构建”）
- 包根：`com.ufs.core`

---

## 1. 分层架构（api → service → spi → adapter → repository）

```
com.ufs.core
├── api/            ① 接入层：REST 控制器，返回统一信封 ApiResponse<T>
├── service/        ② 业务层：接口 + impl，编排 domain / spi / repository
├── spi/            ③ 存储厂商 SPI：StorageProvider 接口 + Factory 路由
├── adapter/        ④ 厂商适配：local(内存可跑) / oss / kodo / tos(待配置)
├── repository/     ⑤ 持久化：entity(record) + MyBatis mapper + Flyway DDL
├── model/dto/      传输对象（全部 record，不序列化 Entity 出层）
├── common/         统一响应 ApiResponse / 分页 PageResult / 错误码 / 工具
├── auth/           双鉴权：HMAC-SHA256(服务端) + JWT(前端) + 拦截器
├── tenant/         多租户隔离：TenantContextHolder(ThreadLocal) + SQL 拦截器
├── config/         异步线程池(MDC) / 幂等 / 缓存请求体 / MyBatis 租户拦截
├── audit/          审计发布：Logging(默认) / RocketMQ(prod)
└── scheduler/      上传任务清理定时任务
```

设计原则：依赖方向 `api → service → {spi, repository}`，`common/auth/tenant/config` 为横切能力；`spi` 与 `adapter` 解耦，新增厂商只需实现 `StorageProvider` 并在 `StorageProviderFactory` 注册。

---

## 2. 快速开始

### 本地构建（无需联网安装 Maven）

若本地未安装 Maven，可用仓库内置的本地启动方式（首次需下载，已置于 `target/maven`，被 `.gitignore` 忽略）：

```bash
# 等价 mvn compile
bash mvn-local.sh compile
# 或启动应用
bash mvn-local.sh spring-boot:run
```

> 或直接用系统 `mvn`：`mvn -B compile` / `mvn spring-boot:run`

### 运行

```bash
mvn spring-boot:run
# 默认端口 8080，dev profile 已内置 6 个演示桶 + local 内存存储可直接联调
```

启动后健康检查：`GET /v1/health`；鉴权令牌：`POST /v1/auth/token`（HMAC 签名，见 §4）。

---

## 3. 统一契约

- 响应信封：`{ code, message, requestId, timestamp, data }`，成功 `code="0000"`。
- 错误码：`UfsErrorCode`（UFS.4000~UFS.5041），对齐《UFS 接口契约》§5。
- 分页：offset（`total`）+ cursor（`nextCursor`+`hasMore`）双模式，见 `PageResult`。
- 全局异常：`GlobalExceptionHandler` 统一包裹；厂商异常 `VendorException` 透传 `vendorCode` 映射为 UFS.5020/5040。

---

## 4. 鉴权（双模式）

### 服务端：HMAC-SHA256

请求头：`X-Ufs-AppKey` / `X-Ufs-Timestamp`（±300s） / `X-Ufs-Nonce`（防重放） / `X-Ufs-Sign`。
签名串 `canonical = method\npath\ntimestamp\nnonce\nbodySha256`，`sign = HMAC-SHA256(secret, canonical)`，头与时间戳/随机数经 Redis SETNX 做重放防护。

最小调用示例（Java）：

```java
String appKey = "app_demo", secret = "<from KMS>";
long ts = Instant.now().getEpochSecond();
String nonce = IdGenerator.newId();
String canonical = method + "\n" + path + "\n" + ts + "\n" + nonce + "\n" + sha256Hex(body);
String sign = HmacSigner.sha256Hex(secret, canonical);
// Header: X-Ufs-AppKey / X-Ufs-Timestamp / X-Ufs-Nonce / X-Ufs-Sign
```

### 前端：Bearer JWT（HS256，≤2h）

`POST /v1/auth/token` 换取 `Authorization: Bearer <jwt>`；密钥来自 KMS，不落库、不打印。

---

## 5. 多租户隔离

- `TenantContextHolder`（ThreadLocal）在 `UfsAuthInterceptor` 中由鉴权信息写入。
- `TenantSqlInterceptor`（MyBatis 拦截器）对业务表强制注入 `tenant_id`；系统上下文（`TenantContextHolder.system()`，如定时任务/迁移）豁免。
- Entity 实现 `TenantScoped`，所有 mapper 查询均携带 `tenant_id`。

---

## 6. 当前桩实现（stub）状态

| 能力 | 状态 | 说明 |
|---|---|---|
| `LocalStorageProvider` | ✅ 可跑 | 内存实现，dev 直接联调对象上传/列举/下载 |
| `OssStorageProvider` | 🚧 桩 | 继承 `AbstractVendorStorageProvider`，未配置时抛 `notConfigured` |
| `KodoStorageProvider` | 🚧 桩 | 同上 |
| `TosStorageProvider` | 🚧 桩 | 同上 |
| 持久化 | ✅ DDL | Flyway `V1__init.sql`：file_meta / upload_task / storage_policy / audit_log |
| 审计落盘 | ✅ 默认 | `LoggingAuditPublisher` 写库 + 日志；`RocketMqAuditPublisher` 仅 prod 生效 |
| 监控指标 | 🔶 合成 | `MonitorServiceImpl` 聚合元数据 + 指标，真实数据源为 Prometheus |

---

## 7. 对齐《UFS 研发规范手册》（7 份）

| 规范 | 落点 |
|---|---|
| Java 编码 | `common/error`、`config`、`service/impl` 命名/分层/异常处理/异步 MDC |
| SQL/DDL | `db/migration/V1__init.sql`：utf8mb4_0900_ai_ci、DATETIME(3)、租户分片键 |
| 接口契约 | `model/dto/*`、`api/*` 与 OpenAPI 28 个 operation 一一对应 |
| 前端 TS | 见手册独立文档（前端消费本后端） |
| 安全 | `auth/*`、`tenant/*`、密钥 KMS 注入、重放防护、审计 |
| 监控告警 | `micrometer-registry-prometheus` + Actuator，暴露 `/actuator/prometheus` |
| 提交规范 | Conventional Commits；见仓库 git 配置与手册 |

---

## 8. 下一批待办（建议）

1. 落地 oss/kodo/tos 三家 SDK 适配（实现 `StorageProvider` 全量方法）。
2. 接入真实 MySQL/Redis/RocketMQ（当前 dev 用 local 存储 + 内存）。
3. 补充单测（starter-test 已引入），覆盖鉴权/租户拦截/幂等。
4. 接入 `mvnw`（Maven Wrapper）以便零安装构建。
