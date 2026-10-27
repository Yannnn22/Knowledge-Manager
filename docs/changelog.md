# 知识管理系统 - 更新日志

## 版本 v1.4.0 (2025-09-06)

### 🎉 功能增强

#### ✅ 操作日志统计与高级筛选
- **新增功能**: 实现了完整的操作日志统计和高级筛选功能。
- **统计看板**:
  - `GET /api/v1/operation-logs/statistics` - 提供今日日志数、敏感操作数、活跃用户数和错误记录数。
- **高级筛选**:
  - `GET /api/v1/operation-logs/filter` - 支持按 **用户名**、**操作类型**、**目标类型**、**目标ID** 和 **时间范围** 进行组合筛选。
- **DTO**: 创建了 `OperationLogStatsDTO` 和 `PageResult` DTO 用于标准化响应。

### 🐛 Bug 修复

#### ⚙️ 修复了系统性的筛选逻辑问题
- **问题描述**: `/api/v1/operation-logs` 和 `/api/v1/operation-logs/filter` 端点的筛选参数（如 `username`, `targetType`）被忽略。
- **根本原因**: 服务层在调用 Mapper 方法时，参数传递顺序错误且缺失，导致筛选条件失效。
- **解决方案**:
  - 修正了 `OperationLogServiceImpl` 中 `selectByPageWithFilters` 方法的参数映射。
  - 为 `OperationLogQueryRequest` DTO 添加了缺失的 `targetType` 和 `targetId` 字段。
  - 确保了 `JOIN` 查询在按用户名筛选时能被正确应用。

### 🗄️ 数据库优化

- **索引建议**: 在 `database_indexes.sql` 文件中为 `operation_log` 表提供了详细的索引创建建议，以提升查询性能。

### 📝 文档更新

- **API测试文档**: 创建了 `operation_log_api_test.md`，详细说明了新接口的使用方法和测试用例。

---

## 版本 v1.3.0 (2025-08-21)

### 🤖 大模型与 RAG 能力

#### ✅ 集成大模型与检索增强生成（RAG）
- **新增模块**: 基于 Spring AI + DashScope 的智能问答
- **对话记忆**: 基于 Redis 的 `ChatMemory`，支持 `chatId` 会话隔离
- **模型控制**: 通过请求头 `model`、`temperature` 控制模型与采样温度
- **检索过滤**: 支持按 `fileId` 过滤向量检索范围

#### ✅ 向量化文件管理与检索
- **支持格式**: PDF、Word、Excel、TXT 等（PDF 使用分页 Reader，其它用 Tika 提取）
- **切分策略**: Token 级文本分块（TokenTextSplitter）
- **向量存储**: Spring AI `VectorStore` 相似度搜索与删除

#### ✅ 对话历史管理
- 会话列表、详情、删除、清空、统计全套接口，便于审计与运维

### 🧭 新增 API 接口

1) RAG 智能问答
- `GET /ai/chat-gated`（SSE 流式）：参数 `prompt`、`topK`、`threshold`、`fileId`；请求头 `model`、`temperature`、`chatId`
- `GET /ai/chat-once`（非流式）：参数与请求头同上，返回一次性文本
- `GET /ai/select`：向量数据查询测试

2) 文件上传与向量存储
- `POST /ingest/upload`（multipart）：上传并向量化，返回 `fileId`
- `GET /ingest/search`：基于 `q` 与 `fileId` 的相似度检索，支持 `topK`
- `POST /ingest/delete`：根据 `fileId` 删除对应向量数据

3) 对话历史管理
- `GET /api/v1/conversation/history`：获取会话列表
- `GET /api/v1/conversation/history/{chatId}`：获取会话详情
- `DELETE /api/v1/conversation/history/{chatId}`：删除指定会话
- `DELETE /api/v1/conversation/history`：清空全部会话
- `GET /api/v1/conversation/stats`：获取会话统计

### 🏗️ 技术实现
- Spring AI ChatClient + DashScope（通义千问）
- Redis `StringRedisTemplate` 记录会话与记忆
- 向量检索：`VectorStore.similaritySearch` + 阈值 `threshold` 决策是否走 RAG
- 文档解析：`PagePdfDocumentReader` / `TikaDocumentReader` + `TokenTextSplitter`
- 运行时参数校验：`model` / `temperature` 由服务校验与注入

### ✅ 测试验证
- 流式与非流式问答均可用
- 文件上传 / 检索 / 删除链路打通
- 会话列表、详情、删除、清空、统计接口通过

---

## 版本 v1.2.0 (2025-08-19)

### 🎉 重大功能更新

#### ✅ 用户个人资料管理功能
- **新增功能**: 完整的用户个人资料管理系统
- **API接口**: 
  - `GET /api/v1/users/profile` - 获取当前用户个人资料
  - `PUT /api/v1/users/profile` - 更新个人资料
  - `PUT /api/v1/users/password` - 修改密码
- **功能特性**: 
  - 支持头像URL、手机号、个人简介、所在地、个人网站管理
  - 邮件通知和系统通知开关设置
  - 安全的密码修改（需验证当前密码）
  - 自动创建个人资料记录

#### ✅ 文档收藏和标签管理功能
- **新增功能**: 完整的文档收藏和标签系统
- **API接口**: 
  - `POST /api/v1/documents/{id}/favorite` - 收藏文档
  - `DELETE /api/v1/documents/{id}/favorite` - 取消收藏
  - `GET /api/v1/documents/favorites` - 获取收藏列表（分页）
  - `POST /api/v1/documents/{id}/tags` - 给文档添加标签
  - `DELETE /api/v1/documents/{id}/tags/{tagId}` - 移除文档标签
  - `GET /api/v1/documents/{id}/tags` - 获取文档标签
  - `GET /api/v1/documents/tags` - 获取所有标签
  - `POST /api/v1/documents/tags` - 创建新标签
- **功能特性**: 
  - 灵活的标签系统（创建、关联、移除、查询）
  - 标签使用次数统计
  - 收藏列表包含文档详情和关联标签
  - 支持自定义标签颜色和描述

### 🗄️ 数据库更新

#### 新增数据表
1. **user_profile** - 用户个人资料扩展表
   - 支持头像、联系方式、个人简介等信息
   - 通知设置管理

2. **tag** - 标签表
   - 标签名称、颜色、描述
   - 使用次数统计
   - 创建者信息

3. **document_tag** - 文档标签关联表
   - 文档与标签的多对多关系
   - 关联创建者记录

4. **document_favorite** - 文档收藏表
   - 用户收藏文档记录
   - 收藏时间记录

#### 预置测试数据
- 6个预定义标签（技术文档、产品需求、会议纪要、培训资料、重要、草稿）
- 为现有用户自动创建个人资料记录
- 测试收藏和标签关联数据

### 🏗️ 技术实现

#### 新增文件统计
- **实体类**: 4个（UserProfile, Tag, DocumentTag, DocumentFavorite）
- **DTO类**: 4个（UserProfileDTO, UpdatePasswordRequest, TagDTO, DocumentFavoriteResponse）
- **Mapper接口**: 4个（UserProfileMapper, TagMapper, DocumentTagMapper, DocumentFavoriteMapper）
- **XML映射文件**: 4个对应的MyBatis映射文件
- **Controller扩展**: UserController和DocumentController
- **Service扩展**: UserService和DocumentService
- **数据库脚本**: user_profile_and_favorites.sql

### ✅ 测试验证

#### 功能测试结果
- ✅ 个人资料管理：获取、更新功能正常
- ✅ 文档收藏：收藏、取消收藏、收藏列表功能正常
- ✅ 标签管理：创建、关联、查询功能正常
- ✅ JWT认证：所有接口认证正常
- ✅ Swagger文档：API文档完整可用

---

## 版本 v1.1.0 (2025-08-18)

### 🎉 主要更新

#### ✅ SpringDoc OpenAPI 集成成功
- **新增功能**: 成功集成 SpringDoc OpenAPI 3.x
- **版本**: 使用 SpringDoc 2.7.0 版本解决与 Spring Boot 3.5.4 的兼容性问题
- **功能**: 自动生成 API 文档，支持交互式测试

#### 🔧 Swagger UI 恢复
- **问题**: 之前因兼容性问题移除了 SpringDoc 依赖
- **解决方案**: 升级到 SpringDoc 2.7.0，成功解决兼容性问题
- **访问地址**: 
  - Swagger UI: http://localhost:8080/swagger-ui/index.html
  - OpenAPI JSON: http://localhost:8080/v3/api-docs
  - 根路径自动重定向到 Swagger UI

### 🐛 Bug 修复

#### 🔐 登录认证问题修复
- **问题描述**: 用户无法使用 README.md 中提供的测试账号登录
- **根本原因**: 
  - 数据库中 admin 用户密码实际为 "123456" 的 BCrypt 哈希
  - README.md 中错误标注为 "password"
- **解决方案**:
  - 创建新的测试用户 "newuser" (密码: password)
  - 更新 README.md 中的测试账号信息
  - 验证 BCrypt 密码加密和验证流程

#### 🏗️ 代码结构优化
- **异常处理**: 优化 GlobalExceptionHandler，避免与 SpringDoc 冲突
- **注解恢复**: 重新添加 SpringDoc 注解 (@Tag, @Operation, @ApiResponses)
- **路由配置**: 更新 HomeController 重定向路径

### 🧪 测试验证

#### API 接口全面测试
1. **认证接口** ✅
   - POST `/api/v1/auth/login` - 用户登录
   - JWT token 生成和验证正常

2. **用户管理接口** ✅
   - GET `/api/v1/users/page` - 分页查询用户
   - POST `/api/v1/test/create-user` - 创建用户（测试用）
   - 返回 5 个用户记录，JWT 认证正常

3. **知识库管理接口** ✅
   - GET `/api/v1/knowledge-bases` - 获取知识库列表
   - 返回技术文档库，包含 3 个文档

4. **操作日志接口** ✅
   - GET `/api/v1/operation-logs` - 查询操作日志
   - 返回 10 条历史操作记录，分页正常

5. **测试接口** ✅
   - GET `/api/v1/test/hello` - 基础连通性测试
   - 新增 TestController 用于无认证测试

### 📝 文档更新

#### README.md 修正
- **测试账号信息更新**:
  - admin / 123456 (管理员账号)
  - newuser / password (新创建测试账号)
  - testuser / password (数据库预置账号)

#### 新增文件
- **TestController.java**: 提供测试接口，支持无认证的基础功能测试
- **更新日志.md**: 本文档，记录所有更新内容

### 🔧 技术栈信息

#### 依赖版本
- Spring Boot: 3.5.4
- SpringDoc OpenAPI: 2.7.0
- MySQL: 8.0.33
- JWT: 0.11.5
- MyBatis: 3.0.3

#### 数据库
- MySQL 8.0+
- 数据库名: knowledge_manager
- 包含用户、知识库、文档、操作日志等表

---

## 项目简介

### 📚 知识管理系统 (Knowledge Manager)

这是一个基于 Spring Boot 的企业级知识管理系统，旨在帮助组织高效地管理、存储和检索知识资源。

#### 🎯 核心功能

1. **用户管理**
   - 用户注册、登录、权限管理
   - JWT 令牌认证
   - 部门和状态管理

2. **知识库管理**
   - 创建和管理知识库
   - 按部门分类管理
   - 权限控制和访问管理

3. **文档管理**
   - 文档上传和存储
   - 文档状态跟踪（上传中、处理中、完成、失败、已归档）
   - 全文搜索功能
   - 文档归档管理

4. **操作日志**
   - 完整的操作审计跟踪
   - 敏感操作记录
   - 日志导出功能

#### 🏗️ 技术架构

- **后端框架**: Spring Boot 3.5.4
- **数据库**: MySQL 8.0 + MyBatis
- **认证**: JWT + Spring Security
- **API 文档**: SpringDoc OpenAPI 3.x + Swagger UI
-[object Object] **完善的异常处理**: 全局异常处理机制
4. **参数校验**: 使用 Bean Validation 进行参数验证
5. **交互式文档**: Swagger UI 支持在线 API 测试
6. **安全认证**: JWT 令牌 + BCrypt 密码加密
7. **操作审计**: 完整的操作日志记录

#### 🚀 快速开始

1. **环境要求**:
   - JDK 17+
   - MySQL 8.0+
   - Maven 3.6+

2. **启动步骤**:
   ```bash
   # 1. 克隆项目
   git clone <repository-url>
   
   # 2. 配置数据库
   # 修改 application.properties 中的数据库配置
   
   # 3. 初始化数据库
   mysql -u root -p < src/main/resources/sql/init.sql
   
   # 4. 启动应用
   mvn spring-boot:run
   
   # 5. 访问 Swagger UI
   # http://localhost:8080/swagger-ui/index.html
   ```

3. **测试账号**:
   - 管理员: admin / 123456
   - 测试用户: newuser / password

#### 📊 项目统计

- **代码文件**: 46 个 Java 源文件
- **API 接口**: 15+ 个 REST 接口
- **数据表**: 5 个核心业务表
- **功能模块**: 4 个主要业务模块

#### 🔮 未来规划

1. **短期目标**:
   - 集成 MinIO 或阿里云 OSS 文件存储
   - 增强搜索功能（Elasticsearch 集成）
   - 添加文档版本控制

2. **长期目标**:
   - 微服务架构改造
   - 前端 Vue.js 界面开发
   - 移动端 APP 支持
   - AI 智能推荐功能

---

## 开发团队

- **开发者**: ZJC
- **技术栈**: Spring Boot + MySQL + JWT
- **开发时间**: 2025年1月 - 2025年8月

## 许可证

MIT License

---

## 🚀 快速运行指南

### 环境要求
- **JDK**: 17+
- **MySQL**: 8.0+
- **Maven**: 3.6+

### 第一次运行

#### 1. 数据库初始化
```bash
# 登录MySQL
mysql -u root -p

# 执行初始化脚本（按顺序）
source src/main/resources/sql/init.sql
source src/main/resources/sql/knowledge_base.sql
source src/main/resources/sql/document.sql
source src/main/resources/sql/operation_log.sql
source src/main/resources/sql/user_profile_and_favorites.sql
```

#### 2. 配置数据库连接
编辑 `src/main/resources/application.properties`：
```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/knowledge_manager?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码
```

#### 3. 启动应用
```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

#### 4. 访问应用
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API文档**: http://localhost:8080/v3/api-docs
- **根路径**: http://localhost:8080/ （自动跳转到Swagger UI）

### 测试账号
- **管理员**: admin / 123456
- **测试用户**: newuser / password
- **普通用户**: testuser / password

### 快速测试新功能

#### 1. 获取JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password"}'
```

#### 2. 测试个人资料
```bash
# 获取个人资料
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_TOKEN"

# 更新个人资料
curl -X PUT http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bio":"我的新简介","location":"北京"}'
```

#### 3. 测试文档收藏
```bash
# 收藏文档
curl -X POST http://localhost:8080/api/v1/documents/1/favorite \
  -H "Authorization: Bearer YOUR_TOKEN"

# 获取收藏列表
curl -X GET http://localhost:8080/api/v1/documents/favorites \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 4. 测试标签管理
```bash
# 获取所有标签
curl -X GET http://localhost:8080/api/v1/documents/tags \
  -H "Authorization: Bearer YOUR_TOKEN"

# 创建新标签
curl -X POST http://localhost:8080/api/v1/documents/tags \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"我的标签","color":"#52c41a","description":"测试标签"}'
```

### 常见问题

**Q: 启动时提示数据库连接失败？**
A: 检查MySQL服务是否启动，数据库配置是否正确。

**Q: 接口返回401错误？**
A: 检查JWT token是否正确，是否在请求头中正确设置Authorization。

**Q: Swagger UI无法访问？**
A: 确认应用已启动，端口8080未被占用。

**Q: 如何重置数据库？**
A: 重新执行所有SQL初始化脚本即可。

### 开发建议

1. **使用Swagger UI**: 最方便的API测试方式
2. **查看日志**: 应用启动时会显示详细的初始化信息
3. **数据库工具**: 推荐使用Navicat或DBeaver查看数据
4. **API测试**: 可以使用Postman导入OpenAPI文档进行测试

---

*最后更新: 2025-08-21*