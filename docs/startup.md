# 本地启动与排错

> 快速开始请看根目录 [README](../README.md)。本文补充依赖细节、环境变量与常见问题。

## 1. 目录约定

```text
knowledge-manager/
├── backend/     # Spring Boot 3 后端
├── frontend/    # Vue 3 + Vite 前端
├── docs/        # 设计文档与 schema.sql
└── docker-compose.yml
```

## 2. 环境依赖

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17+ | 后端编译与运行 |
| Maven | 3.9+（或使用 `./mvnw`） | 后端构建 |
| Node.js | 20.19+ 或 22.12+ | 前端构建 |
| MySQL | 8.0+ | 业务数据 |
| Redis | 7+ | 会话、缓存、短期记忆 |
| Milvus | 2.5（可选） | 向量检索、长期记忆、文件入库 |
| Docker | 可选 | 一键启动 MySQL / Redis / Milvus |

## 3. 启动基础设施

推荐使用 Docker Compose：

```bash
# MySQL + Redis
docker compose up -d

# 需要 RAG / 向量检索时，再启动 Milvus（etcd + MinIO + Milvus）
docker compose --profile ai up -d
```

MySQL 首次创建数据卷时会自动执行 [`schema.sql`](schema.sql)。如果使用本机已有 MySQL，请手动导入：

```bash
mysql -uroot -p < docs/schema.sql
```

Compose 默认端口：MySQL `3306`、Redis `6379`、Milvus `19530`、MinIO `9000/9001`。
如端口被占用，可通过 `MYSQL_PORT`、`REDIS_PORT`、`MILVUS_PORT` 覆盖。

## 4. 启动后端

```bash
cd backend

export DB_PASSWORD=km123456
export SILICONFLOW_API_KEY=sk-你的Key
export JWT_SECRET=请替换为至少64位随机字符串

./mvnw spring-boot:run
```

默认地址 `http://localhost:8080`。首次启动时 `DataInitializer` 会自动创建管理员：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 管理员 |

## 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:3000`。开发环境下 Vite 会把 `/api`、`/ai`、`/ingest`、`/uploads` 代理到 `http://127.0.0.1:8080`，可通过 `VITE_PROXY_TARGET` 修改。

## 6. 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/knowledge_manager...` | MySQL 连接串 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 连接 |
| `MILVUS_HOST` / `MILVUS_PORT` | `localhost` / `19530` | Milvus 连接 |
| `SILICONFLOW_API_KEY` | `not-configured` | 大模型 / Embedding Key |
| `SILICONFLOW_BASE_URL` | `https://api.siliconflow.cn` | OpenAI 兼容服务地址 |
| `JWT_SECRET` | 开发默认值 | 生产环境必须替换 |
| `FILE_UPLOAD_DIR` | `./uploads` | 文件存储目录 |
| `APP_AI_ENABLED` | `true` | 设为 `false` 可关闭 AI 接口 |

## 7. 常见问题

### MySQL 连接失败

- 检查容器：`docker compose ps`、`docker compose logs -f mysql`。
- 本机已有 MySQL 占用 `3306` 时，改用 `MYSQL_PORT=3307`，并同步修改 `DB_URL`。
- 确认 `knowledge_manager` 库已创建，且 `docs/schema.sql` 已执行。

### Redis 连接失败

```bash
redis-cli ping
# 期望输出 PONG
```

若设置了密码，请同步配置 `REDIS_PASSWORD`。

### Milvus 启动慢 / 不可达

```bash
docker compose --profile ai ps
curl http://localhost:9091/healthz
```

Milvus 首次启动需要较长时间。未就绪时后端仍可启动，但向量检索、长期记忆与文件入库会自动跳过；知识库、文档、审计功能不受影响。

### AI 接口返回 401 / 模型不可用

- 确认 `SILICONFLOW_API_KEY` 已设置为真实 Key，而非默认的 `not-configured`。
- 确认服务器可访问 `https://api.siliconflow.cn`。
- 只想使用知识库功能时，可设置 `APP_AI_ENABLED=false`。

### 端口被占用

- 后端：`SERVER_PORT=8081 ./mvnw spring-boot:run`。
- 前端：`npm run dev -- --port 3001`，同时把 `VITE_PROXY_TARGET` 指向后端实际端口。

### 上传文件失败

`FILE_UPLOAD_DIR` 必须是可写目录。相对路径以**后端启动目录**为基准；Docker 部署时建议挂载绝对路径。

### 生产环境注意事项

- 修改默认管理员密码。
- 替换 `JWT_SECRET`，不要使用仓库中的开发默认值。
- 关闭或限制 Actuator 暴露端点。
- 为 MySQL / Redis / Milvus 配置独立账号与网络访问控制。
