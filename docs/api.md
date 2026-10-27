# 知识管理系统 API 文档

## 概述

知识管理系统提供了完整的RESTful API接口，支持用户认证、文档管理、知识库管理等功能。

**基础URL**: `http://localhost:8080/api/v1`

**认证方式**: JWT Bearer Token

## 通用响应格式

所有API接口都使用统一的响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码，200表示成功，其他表示错误 |
| message | String | 响应消息 |
| data | Object | 响应数据，可为null |

### 常见状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或token无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

---

## 1. 认证管理

### 1.1 用户登录

**接口**: `POST /auth/login`

**描述**: 用户登录获取JWT token

**请求参数**:

```json
{
  "username": "string",  // 用户名，必填，不能为空
  "password": "string"   // 密码，必填，不能为空
}
```

**响应数据**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "string",   // JWT token
    "userId": 1          // 用户ID
  }
}
```

**错误响应**:

- `401`: 用户名或密码错误
- `400`: 请求参数错误

---

## 2. 用户管理

### 2.1 创建用户

**接口**: `POST /users`

**描述**: 创建新用户

**认证**: 需要JWT token

**请求参数**:

```json
{
  "username": "string",    // 用户名，必填，3-50字符
  "password": "string",    // 密码，必填，6-100字符
  "email": "string",       // 邮箱，必填，格式正确
  "realName": "string",    // 真实姓名，必填
  "department": "string"   // 部门，必填
}
```

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 123  // 新创建的用户ID
}
```

### 2.2 获取用户列表

**接口**: `GET /users/page`

**描述**: 分页获取用户列表

**认证**: 需要JWT token

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页大小 |
| department | String | 否 | - | 部门筛选 |
| status | Integer | 否 | - | 状态筛选 |

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "users": [
      {
        "id": 1,
        "username": "string",
        "email": "string",
        "realName": "string",
        "department": "string",
        "status": 1,
        "createdAt": "2025-09-05T10:00:00"
      }
    ]
  }
}
```

### 2.3 更新用户

**接口**: `PUT /users/{id}`

**描述**: 更新指定用户信息

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |

**请求参数**:

```json
{
  "email": "string",       // 邮箱，可选
  "realName": "string",    // 真实姓名，可选
  "department": "string",  // 部门，可选
  "status": 1              // 状态，可选
}
```

### 2.4 删除用户

**接口**: `DELETE /users/{id}`

**描述**: 删除指定用户

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |

### 2.5 获取个人资料

**接口**: `GET /users/profile`

**描述**: 获取当前登录用户的个人资料

**认证**: 需要JWT token

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "string",
    "email": "string",
    "realName": "string",
    "department": "string",
    "avatar": "string"
  }
}
```

### 2.6 更新个人资料

**接口**: `PUT /users/profile`

**描述**: 更新当前登录用户的个人资料

**认证**: 需要JWT token

**请求参数**:

```json
{
  "email": "string",       // 邮箱，可选
  "realName": "string",    // 真实姓名，可选
  "department": "string",  // 部门，可选
  "avatar": "string"       // 头像URL，可选
}
```

### 2.7 修改密码

**接口**: `PUT /users/password`

**描述**: 修改当前登录用户的密码

**认证**: 需要JWT token

**请求参数**:

```json
{
  "oldPassword": "string",  // 旧密码，必填
  "newPassword": "string"   // 新密码，必填，6-100字符
}
```

---

## 3. 知识库管理

### 3.1 创建知识库

**接口**: `POST /knowledge-bases`

**描述**: 创建新的知识库

**认证**: 需要JWT token

**请求参数**:

```json
{
  "name": "string",        // 知识库名称，必填
  "description": "string", // 描述，可选
  "department": "string"   // 部门，可选
}
```

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 123  // 新创建的知识库ID
}
```

### 3.2 获取知识库列表

**接口**: `GET /knowledge-bases`

**描述**: 获取用户有权限的知识库列表

**认证**: 需要JWT token

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| department | String | 否 | - | 部门筛选 |
| includeAll | Boolean | 否 | false | 是否包含所有知识库 |

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "knowledgeBases": [
      {
        "id": 1,
        "name": "string",
        "owner": "string",
        "ownerId": 1,
        "department": "string",
        "createdAt": "2025-09-05T10:00:00",
        "updatedAt": "2025-09-05T10:00:00",
        "documentCount": 10,
        "canEdit": true,
        "canDelete": true
      }
    ]
  }
}
```

### 3.3 获取知识库详情

**接口**: `GET /knowledge-bases/{id}`

**描述**: 获取指定知识库的详细信息

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 知识库ID |

### 3.4 获取知识库统计

**接口**: `GET /knowledge-bases/{id}/stats`

**描述**: 获取指定知识库的统计信息

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 知识库ID |

### 3.5 更新知识库

**接口**: `PUT /knowledge-bases/{id}`

**描述**: 更新指定知识库信息

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 知识库ID |

**请求参数**:

```json
{
  "name": "string",        // 知识库名称，可选
  "description": "string", // 描述，可选
  "department": "string"   // 部门，可选
}
```

### 3.6 删除知识库

**接口**: `DELETE /knowledge-bases/{id}`

**描述**: 删除指定知识库

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 知识库ID |

### 3.7 批量删除知识库

**接口**: `POST /knowledge-bases/batch-delete`

**描述**: 批量删除知识库

**认证**: 需要JWT token

**请求参数**:

```json
{
  "ids": [1, 2, 3]  // 知识库ID数组
}
```

**响应数据**:

```json
{
  "code": 200,
  "message": "删除完成",
  "data": {
    "successIds": [1, 2],     // 成功删除的ID
    "failed": ["ID 3: 权限不足"] // 失败的原因
  }
}
```

---

## 4. 文档管理

### 4.1 上传文档

**接口**: `POST /documents/upload`

**描述**: 上传文档到指定知识库

**认证**: 需要JWT token

**请求类型**: `multipart/form-data`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文档文件 |
| knowledgeBaseId | Long | 是 | 知识库ID |
| description | String | 否 | 文档描述 |

**响应数据**:

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "documentId": 123,
    "statusUrl": "/api/v1/documents/123/status"
  }
}
```

### 4.2 获取文档列表

**接口**: `GET /documents/page`

**描述**: 分页获取用户有权限的文档列表，支持筛选

**认证**: 需要JWT token

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页大小 |
| sortBy | String | 否 | uploadtime | 排序字段：uploadtime/createdat/filename/filesize/status |
| order | String | 否 | desc | 排序方向：asc/desc |
| knowledgeBaseIds | List<Long> | 否 | - | 知识库ID列表筛选 |
| status | Integer | 否 | - | 文档状态筛选：1=处理中，2=完成，3=失败 |

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": "100",
    "documents": [
      {
        "id": "19346709468680192",
        "knowledgeBaseId": "1",
        "originalFilename": "接口文档模板.pdf",
        "fileType": "pdf",
        "fileSize": "115885",
        "status": 1,
        "uploaderId": "1",
        "uploadTime": "2025-09-05T17:01:19"
      }
    ]
  }
}
```

### 4.3 获取文档状态

**接口**: `GET /documents/{id}/status`

**描述**: 获取文档处理状态

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "documentId": 123,
    "status": 1,           // 1=处理中，2=完成，3=失败
    "progress": 50,        // 处理进度百分比
    "message": "正在处理中..."
  }
}
```

### 4.4 预览文档

**接口**: `GET /documents/{id}/preview`

**描述**: 预览文档内容

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

### 4.5 下载文档

**接口**: `GET /documents/{id}/download`

**描述**: 下载原始文档文件

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

### 4.6 编辑文档

**接口**: `PUT /documents/{id}/edit`

**描述**: 编辑文档内容和描述

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

**请求参数**:

```json
{
  "content": "string",     // 文档内容，可选
  "description": "string"  // 文档描述，可选
}
```

### 4.7 删除文档

**接口**: `DELETE /documents/{id}`

**描述**: 软删除文档（标记为已删除）

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

### 4.8 物理删除文档

**接口**: `DELETE /documents/{id}/physical`

**描述**: 物理删除文档（彻底删除）

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

### 4.9 批量删除文档

**接口**: `POST /documents/batch-delete`

**描述**: 批量删除文档

**认证**: 需要JWT token

**请求参数**:

```json
{
  "documentIds": [1, 2, 3]  // 文档ID数组
}
```

### 4.10 搜索文档

**接口**: `POST /documents/search`

**描述**: 搜索文档内容

**认证**: 需要JWT token

**请求参数**:

```json
{
  "query": "string",           // 搜索关键词，必填
  "knowledgeBaseIds": [1, 2],  // 知识库ID列表，可选
  "fileTypes": ["pdf", "docx"], // 文件类型列表，可选
  "page": 1,                   // 页码，可选，默认1
  "size": 10                   // 每页大小，可选，默认10
}
```

### 4.11 文档评论管理

#### 4.11.1 添加评论

**接口**: `POST /documents/{id}/comments`

**描述**: 为文档添加评论

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

**请求参数**:

```json
{
  "content": "string",  // 评论内容，必填
  "parentId": 1         // 父评论ID，可选，用于回复评论
}
```

#### 4.11.2 获取评论列表

**接口**: `GET /documents/{id}/comments`

**描述**: 获取文档的评论列表

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页大小 |

#### 4.11.3 更新评论

**接口**: `PUT /documents/comments/{commentId}`

**描述**: 更新评论内容

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| commentId | Long | 评论ID |

**请求参数**:

```json
{
  "content": "string"  // 新的评论内容，必填
}
```

#### 4.11.4 删除评论

**接口**: `DELETE /documents/comments/{commentId}`

**描述**: 删除评论

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| commentId | Long | 评论ID |

### 4.12 标签管理

#### 4.12.1 创建标签

**接口**: `POST /documents/tags`

**描述**: 创建新标签

**认证**: 需要JWT token

**请求参数**:

```json
{
  "name": "string",        // 标签名称，必填
  "color": "string",       // 标签颜色，可选
  "description": "string"  // 标签描述，可选
}
```

#### 4.12.2 获取标签列表

**接口**: `GET /documents/tags`

**描述**: 获取标签列表

**认证**: 需要JWT token

#### 4.12.3 为文档添加标签

**接口**: `POST /documents/{id}/tags/{tagId}`

**描述**: 为文档添加标签

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |
| tagId | Long | 标签ID |

#### 4.12.4 移除文档标签

**接口**: `DELETE /documents/{id}/tags/{tagId}`

**描述**: 移除文档的标签

**认证**: 需要JWT token

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文档ID |
| tagId | Long | 标签ID |

---

## 5. 测试接口

### 5.1 健康检查

**接口**: `GET /test/hello`

**描述**: 测试接口，检查服务是否正常

**认证**: 无需认证

**响应数据**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "Hello, World!"
}
```

### 5.2 测试文档筛选

**接口**: `GET /test/documents`

**描述**: 测试文档筛选功能（无需认证）

**认证**: 无需认证

**查询参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| userId | Long | 否 | 1 | 用户ID |
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页大小 |
| sortBy | String | 否 | uploadtime | 排序字段 |
| order | String | 否 | desc | 排序方向 |
| knowledgeBaseIds | List<Long> | 否 | - | 知识库ID列表 |
| status | Integer | 否 | - | 文档状态 |

---

## 6. 错误处理

### 6.1 认证错误

当JWT token无效或过期时，会返回401错误：

```json
{
  "code": 401,
  "message": "未授权或token无效",
  "data": null
}
```

### 6.2 权限错误

当用户没有足够权限访问资源时，会返回403错误：

```json
{
  "code": 403,
  "message": "权限不足",
  "data": null
}
```

### 6.3 参数验证错误

当请求参数不符合要求时，会返回400错误：

```json
{
  "code": 400,
  "message": "请求参数错误: 用户名不能为空",
  "data": null
}
```

### 6.4 资源不存在错误

当请求的资源不存在时，会返回404错误：

```json
{
  "code": 404,
  "message": "资源不存在",
  "data": null
}
```

---

## 7. 使用示例

### 7.1 完整的用户登录和文档上传流程

```bash
# 1. 用户登录
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'

# 响应获得token
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1
  }
}

# 2. 获取知识库列表
curl -X GET "http://localhost:8080/api/v1/knowledge-bases" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 3. 上传文档
curl -X POST "http://localhost:8080/api/v1/documents/upload" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "file=@document.pdf" \
  -F "knowledgeBaseId=1" \
  -F "description=测试文档"

# 4. 获取文档列表
curl -X GET "http://localhost:8080/api/v1/documents/page?page=1&size=10&knowledgeBaseIds=1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 7.2 文档筛选示例

```bash
# 按知识库筛选
curl -X GET "http://localhost:8080/api/v1/documents/page?knowledgeBaseIds=1,2&page=1&size=10" \
  -H "Authorization: Bearer TOKEN"

# 按状态筛选
curl -X GET "http://localhost:8080/api/v1/documents/page?status=2&page=1&size=10" \
  -H "Authorization: Bearer TOKEN"

# 组合筛选和排序
curl -X GET "http://localhost:8080/api/v1/documents/page?knowledgeBaseIds=1&status=2&sortBy=uploadtime&order=desc" \
  -H "Authorization: Bearer TOKEN"
```

---

## 8. 配置说明

### 8.1 文件上传配置

系统支持以下文件上传配置：

- **最大文件大小**: 50MB
- **支持的文件类型**: PDF, DOC, DOCX, TXT, MD等
- **上传目录**: 可通过`file.upload.base-dir`配置
- **临时目录**: 可通过`spring.servlet.multipart.location`配置

### 8.2 JWT配置

- **Token有效期**: 24小时（可配置）
- **签名算法**: HS256
- **Header格式**: `Authorization: Bearer <token>`

### 8.3 数据库配置

- **数据库**: MySQL 5.7+
- **连接池**: HikariCP
- **事务管理**: Spring Transaction

---

## 9. 版本信息

- **API版本**: v1
- **Spring Boot版本**: 3.5.4
- **Java版本**: 17+
- **最后更新**: 2025-09-05

---

## 10. 联系方式

如有问题或建议，请联系开发团队。

**API文档地址**: http://localhost:8080/api-docs.html
