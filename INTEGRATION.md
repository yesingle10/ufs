# UFS 联调验证指南

## 全链路验证清单

| 层 | 组件 | 状态 | 验证方式 |
|---|------|------|----------|
| 后端 | ufs-core | 65 tests ✅ | `mvn test` |
| SDK | ufs-client | 23 tests ✅ | `mvn test` (MockWebServer) |
| 前端 | ufs-console | BUILD ✅ (4.7s) | `npm run build` |
| 示例 | ufs-sdk-example | COMPILE ✅ | `mvn compile` |

---

## 方式一：Docker 一键启动（推荐）

```bash
# 1. 克隆仓库
git clone https://github.com/JESAN/ufs-core.git && cd ufs-core

# 2. 配置环境变量（可选，有默认值）
cp .env.example .env
# 编辑 .env 设置数据库密码、JWT 密钥等

# 3. 一键启动（MySQL 8 + Redis 7 + ufs-core）
docker compose up -d

# 4. 查看日志
docker compose logs -f ufs-core

# 5. 验证
curl http://localhost:8080/api/v1/health
# → {"status":"UP","version":"1.0.0"}
```

## 方式二：本地 IDE 启动

### 前提

- JDK 17+
- MySQL 8.0 运行在 `localhost:3306`，数据库 `ufs_dev`
- Redis 7 运行在 `localhost:6379`

### 步骤

```bash
# 1. 启动 ufs-core（dev profile）
cd ufs-core
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. 启动前端（另一个终端）
cd ufs-console
npm run dev
# → http://localhost:5173  （Vite 代理到 :8080）

# 3. 运行 SDK 示例（第三个终端）
cd ufs-sdk-example
mvn compile exec:java
```

---

## 验证端点速查

| 端点 | 方法 | 用途 |
|------|------|------|
| `/api/v1/health` | GET | 健康检查（免鉴权） |
| `/api/v1/buckets` | GET/POST | 桶列表 / 创建 |
| `/api/v1/buckets/{name}` | GET/PUT/DELETE | 桶详情/更新/删除 |
| `/api/v1/policies/active` | GET | 活跃策略列表 |
| `/api/v1/policies` | POST | 创建策略 |
| `/api/v1/objects/presign-upload` | POST | 预签名上传 |
| `/api/v1/objects/presign-download` | POST | 预签名下载 |
| `/api/v1/objects/presign-multipart` | POST | 分片上传预签名 |
| `/api/v1/objects/complete-upload` | POST | 完成分片上传 |
| `/api/v1/objects` | GET | 对象列表（分页） |
| `/api/v1/objects/head` | GET | 对象元数据 |
| `/api/v1/monitor/stats` | GET | 监控统计 |
| `/api/v1/monitor/audit` | GET | 审计日志 |
| `/api/v1/monitor/recycle` | GET/POST | 回收站 |
| `/swagger-ui.html` | GET | Swagger API 文档 |

### 鉴权头

```
Authorization: Bearer <jwt_token>
X-App-Id: app_demo
X-Tenant-Id: t_demo
```

---

## SDK 示例项目结构

```
ufs-sdk-example/
├── pom.xml                    # 依赖 ufs-client:1.0.0
└── src/main/java/com/ufs/example/
    └── QuickStart.java        # 完整 CRUD 演示
```

### QuickStart 覆盖的 API

1. **健康检查** — 获取服务状态和版本
2. **桶管理** — 列举 / 创建 / 查询 / 更新 / 删除
3. **存储策略** — 列举活跃 / 创建 / 查询 / 更新 / 删除
4. **对象操作** — 预签名上传 / 分片上传 / 预签名下载 / 列表 / 元数据 / 删除
5. **错误处理** — 不存在的资源 → `UfsClientException`
