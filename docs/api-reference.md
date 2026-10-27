# 知识管理系统 API 接口文档1这是权限导致的“伪不存在”。现在列表用 includeAll=true 能拿到“别人的知识库”，但后端在更新/删除时按“仅 owner 可操作”校验，于是把非 owner 的操作打回“知识库不存在”（为了隐藏资源存在性），所以前端对这些条目操作就会出现你看到的两条 500（或应为 403）。

建议改动
- 后端（推荐二选一，或同时做）
  - 放权给管理员：若当前用户是 ADMIN，则允许更新/删除任意知识库；否则仍仅限 owner。
  - 返回明确错误码：对非 owner 返回 403 Forbidden，message 提示“无权限”，而不是“知识库不存在”。这样前端能正确提示并禁用按钮。
- 列表补充权限字段（前端可据此禁用编辑/删除）
  - 在 includeAll=true 的返回每条记录中增加：
    - ownerId: string
    - canEdit: boolean
    - canDelete: boolean
  - 或最少返回 ownerId，由前端用当前登录用户 id 判断是否显示编辑/删除（若有角色也可返回 roles/canXXX）。

前端配合
- 我会按以下逻辑禁用按钮：
  - 若返回了 canEdit/canDelete，则直接据此控制按钮显隐；
  - 否则用 ownerId === authStore.user?.id 判断，非 owner 隐藏“编辑/删除”。
- 对 403 的响应，弹出“无权限操作该知识库”；对 409（仍有关联处理中数据）给出明确提示。

你确认采用哪种后端方案（ADMIN 放权或仅返回 403 和 ownerId/canEdit/canDelete），我就把前端按钮权限控制加上去。

> 说明
- 统一响应：`ApiResponse<T>`，字段：`code`(int) `message`(string) `data`(泛型)
- 需认证接口在请求头加入：`Authorization: Bearer <JWT_TOKEN>`
- 分页参数通用：`page`(默认1) `size`(默认10)

---

## 认证 Auth

- POST `/api/v1/auth/login`
  - 描述：用户登录
  - 请求体 `LoginRequest`
    - username: string 必填
    - password: string 必填
  - 响应体 `LoginResponse`
    - token: string（JWT）
    - userId: long
    - username: string（其余字段以实现为准）

示例
```json
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "123456"
}
```

---

## 用户 Users

- POST `/api/v1/users` 需要认证
  - 描述：创建用户
  - 请求体 `CreateUserRequest`
    - username: string 必填 3-50
    - password: string 必填 6-100
    - email: string 必填 Email
    - realName: string 必填
    - department: string 必填
  - 响应体：long（新用户ID）

- GET `/api/v1/users/page` 需要认证
  - 描述：用户分页查询
  - Query：page size department status
  - 响应体 `UserPageResponse`

- PUT `/api/v1/users/{id}` 需要认证
  - 描述：更新用户
  - 路径参数：id: long
  - 请求体 `UpdateUserRequest`
    - email: string 可选 Email
    - realName: string 可选
    - department: string 可选
    - status: int 必填
  - 响应体：string “更新成功”

- DELETE `/api/v1/users/{id}` 需要认证
  - 描述：删除用户（逻辑删除）
  - 路径参数：id: long
  - 响应体：string “删除成功”

- GET `/api/v1/users/profile` 需要认证
  - 描述：获取当前用户个人资料
  - 响应体 `UserProfileDTO`
    - userId: long
    - realName: string ≤50
    - email: string ≤100 Email
    - department: string ≤50
    - avatarUrl: string ≤500
    - phone: string ≤20
    - bio: string ≤500
    - location: string ≤100
    - website: string ≤200
    - notificationEmail: boolean
    - notificationSystem: boolean

- PUT `/api/v1/users/profile` 需要认证
  - 描述：更新当前用户个人资料
  - 请求体：`UserProfileDTO`（后端会覆盖 userId 为当前登录用户）
  - 响应体：string “个人资料更新成功”

- PUT `/api/v1/users/password` 需要认证
  - 描述：修改密码
  - 请求体 `UpdatePasswordRequest`（参见 DTO）
    - currentPassword: string
    - newPassword: string
  - 响应体：string “密码修改成功”

---

## 知识库 Knowledge Bases

Base：`/api/v1/knowledge-bases`

- POST `/api/v1/knowledge-bases` 需要认证
  - 描述：创建知识库
  - 请求体 `CreateKnowledgeBaseRequest`（name/description?/department）
  - 响应：long（knowledgeBaseId）

- GET `/api/v1/knowledge-bases` 需要认证
  - 描述：知识库列表（Query：department 可选）
  - 响应 `KnowledgeBaseListResponse`

- GET `/api/v1/knowledge-bases/{id}` 需要认证
  - 描述：知识库详情
  - 响应 `KnowledgeBaseDetailResponse`

- GET `/api/v1/knowledge-bases/{id}/stats` 需要认证
  - 描述：知识库统计
  - 响应 `KnowledgeBaseStatsResponse`

---

## 文档 Documents

Base：`/api/v1/documents`

- POST `/api/v1/documents/upload` 需要认证
  - 描述：上传文档
  - Form-Data：file 必填、knowledgeBaseId 必填、description 可选
  - 响应 `DocumentUploadResponse`

- GET `/api/v1/documents/{id}/status` 需要认证
  - 描述：获取文档处理状态
  - 响应 `DocumentStatusResponse`

- POST `/api/v1/documents/search` 需要认证
  - 描述：全文搜索
  - 请求体 `DocumentSearchRequest`（query、knowledgeBaseIds[]）
  - 响应 `DocumentSearchResponse`

- PUT `/api/v1/documents/{id}/archive` 需要认证
  - 描述：归档文档
  - 响应：string "文档归档成功"

- DELETE `/api/v1/documents/{id}` 需要认证
  - 描述：删除文档（软删除）
  - 路径参数：id: long
  - 权限：需要是文档上传者或知识库拥有者
  - 响应体：string "文档删除成功"

收藏与标签（v1.2.0）

- POST `/api/v1/documents/{id}/favorite` 需要认证
  - 描述：收藏文档
  - 响应：string “收藏成功”

- DELETE `/api/v1/documents/{id}/favorite` 需要认证
  - 描述：取消收藏
  - 响应：string “取消收藏成功”

- GET `/api/v1/documents/favorites` 需要认证
  - 描述：收藏列表（Query：page/size）
  - 响应 `DocumentFavoriteResponse`

- POST `/api/v1/documents/{id}/tags` 需要认证
  - 描述：添加标签（请求体：tagIds: long[]）
  - 响应：string “标签添加成功”

- DELETE `/api/v1/documents/{id}/tags/{tagId}` 需要认证
  - 描述：移除指定标签
  - 响应：string “标签移除成功”

- GET `/api/v1/documents/{id}/tags` 需要认证
  - 描述：获取文档标签
  - 响应：`TagDTO[]`

- GET `/api/v1/documents/tags` 需要认证
  - 描述：获取所有标签
  - 响应：`TagDTO[]`

- POST `/api/v1/documents/tags` 需要认证
  - 描述：创建标签
  - 请求体 `TagDTO`（name、color?、description?）
  - 响应：创建后的 `TagDTO`

---

## 操作日志 Operation Logs

Base：`/api/v1/operation-logs`

- GET `/api/v1/operation-logs` 需要认证（可能需 admin）
  - 描述：查询操作日志
  - 请求：`OperationLogQueryRequest`（page/size/过滤条件等）
  - 响应 `OperationLogListResponse`

- GET `/api/v1/operation-logs/export` 需要认证（可能需 admin）
  - 描述：导出 CSV
  - Query：startTime/endTime（yyyy-MM-dd HH:mm:ss）
  - 响应：文件流

- GET `/api/v1/operation-logs/sensitive` 需要认证（可能需 admin）
  - 描述：敏感操作
  - 响应 `SensitiveOperationLogResponse`

---

## 对话历史 Conversation（RAG 会话）

Base：`/api/v1/conversation`

- GET `/history`：会话列表 → Map<String,Object>
- GET `/history/{chatId}`：会话详情 → Map<String,Object>
- DELETE `/history/{chatId}`：删除会话 → Map<String,Object>
- DELETE `/history`：清空全部会话 → Map<String,Object>
- GET `/stats`：会话统计 → Map<String,Object>

---

## AI / RAG

### 向量化文件与检索（示例环境可能无需 JWT，视部署策略）
Base：`/ingest`

- POST `/ingest/upload`
  - Form-Data：file 必填（PDF分页，其它Tika提取）
  - 响应：text/plain（含 `fileId` 和片段计数）

- GET `/ingest/search`
  - Query：q 必填、fileId 必填、topK 默认5
  - 响应：`org.springframework.ai.document.Document[]`

- POST `/ingest/delete`
  - Form/Query：fileId 必填
  - 响应：text/plain “successfully deleted”

### RAG 智能问答
Base：`/ai`

- GET `/ai/chat-gated`（SSE 流式）
  - Query：prompt 必填、topK 默认5、threshold 默认0.3、fileId 可选
  - Headers：model 可选、temperature 可选、chatId 可选
  - 响应：text/event-stream（流式文本）

- GET `/ai/chat-once`（非流式）
  - 同上参数与头
  - 响应：text/plain（一次性回答）

- GET `/ai/select`
  - 描述：向量查询测试
  - 响应：`Document[]`

---

## 常见问题
- 404：检查前端请求是否出现 `api/api/...` 重复前缀；确认后端已启动且端口正确
- 401：缺少或错误的 Authorization 头
- 500：查看后端日志与请求参数
- RAG 相关：确保 Milvus/Redis/模型 Key 已配置
