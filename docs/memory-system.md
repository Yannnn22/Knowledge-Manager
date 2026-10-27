# 第一阶段——记忆体系设计与优化文档

> **项目**：知识管理系统（Spring Boot 3.5.4 + Vue 3 + Spring AI 1.0.0 + Milvus 2.5.8 + Redis + MySQL + OpenAI 兼容模型服务（SiliconFlow / DashScope））  
> **阶段目标**：从零构建企业级对话记忆体系，覆盖长期/短期/持久三层存储，逐步扩展为完整的 7 层中间件管道  
> **最后更新**：2026-07-07

---

## 目录

1. [初始架构：三层存储体系](#1-初始架构三层存储体系)
2. [优化轮次 1：PromptPipeline 中间件统一重构](#2-优化轮次-1promptpipeline-中间件统一重构)
3. [优化轮次 2：用户画像 + 摘要压缩 + 可观测性](#3-优化轮次-2用户画像--摘要压缩--可观测性)
4. [优化轮次 3：Token 预算 + 生命周期 + A/B 实验](#4-优化轮次-3token-预算--生命周期--ab-实验)
5. [优化轮次 4：双时间冲突检测机制](#5-优化轮次-4双时间冲突检测机制)
6. [最终中间件管道全景](#6-最终中间件管道全景)
7. [完整配置清单](#7-完整配置清单)
8. [代码变更统计](#8-代码变更统计)
9. [企业级能力清单](#9-企业级能力清单)

---

## 1. 初始架构：三层存储体系

### 1.1 设计动机

Spring AI 默认的 `InMemoryChatMemory` 只提供进程内短期记忆，存在三个根本问题：

| 问题 | 后果 |
|------|------|
| 服务重启记忆丢失 | 用户换话题后无法引用历史知识 |
| 无法跨会话复用记忆 | 每次新对话从零开始，LLM 反复追问 |
| 无语义检索能力 | 只能按时间顺序取最近 N 条，无法"找相关内容"|

### 1.2 三层存储职责

```
┌─────────────────────────────────────────────────────────────┐
│                     三层记忆存储架构                           │
├───────────────┬─────────────────┬─────────────────────────────┤
│  L1 长期记忆   │ Milvus          │ LLM 提取的关键事实/决策/偏好   │
│               │ long_term_memory│ 语义检索 topK=5，相似度≥0.7   │
├───────────────┼─────────────────┼─────────────────────────────┤
│  L2 短期记忆   │ Redis           │ 最近 30 轮原始对话消息          │
│               │ ChatMemory      │ 滑动窗口，保证当前对话流畅       │
├───────────────┼─────────────────┼─────────────────────────────┤
│  L3 持久存储   │ MySQL           │ conversation + message 两张表  │
│               │                 │ 所有对话永久落盘，支持审计/回溯  │
└───────────────┴─────────────────┴─────────────────────────────┘
```

**数据流**：

```
用户输入
   ↓
Controller 接收
   ↓
Advisor 链注入 prompt
   ├─ LongTermMemoryRetrievalAdvisor  → Milvus 检索长期记忆
   ├─ SimpleLoggerAdvisor             → 日志
   ├─ MessageChatMemoryAdvisor        → Redis 短期窗口
   ├─ QuestionAnswerAdvisor           → RAG 文档检索
   └─ LLM 生成回复 → SSE 流式输出
                       ↓
               Flux.cache() 异步后处理
                       ├─ MySQL 持久化消息
                       └─ MemoryExtractionService 提取长期记忆
```

### 1.3 记忆提取与合并策略

**记忆提取**（`MemoryExtractionService`）：

- 每轮对话后，`@Async` 异步调用 `qwen-turbo` 模型
- LLM 从对话中提取结构化的关键信息，JSON 格式返回：

```json
[
  {"type": "preference", "content": "用户偏好简洁的代码风格", "importance": 4},
  {"type": "fact", "content": "用户正在学习 Spring AI 1.0.0", "importance": 5}
]
```

- 每轮最多提取 5 条记忆

**记忆合并**（`MemoryMergeService`）：

- 存储前在 Milvus 检索相似记忆（阈值 0.85，topK=3）
- 有命中：LLM 将新旧记忆融合为更完整的表述，upsert 覆盖（保留旧 ID）
- 无命中：直接 insert 新记忆

### 1.4 新建与修改文件

| 类型 | 文件 | 说明 |
|------|------|------|
| 新建 | `sql/conversation.sql` | conversation + message 表结构 |
| 新建 | `entity/Conversation.java` | 对话实体 |
| 新建 | `entity/Message.java` | 消息实体 |
| 新建 | `config/ai/MemoryProperties.java` | 记忆配置类（`@ConfigurationProperties`）|
| 新建 | `config/ai/LongTermMemoryConfig.java` | Milvus 独立集合 Bean |
| 新建 | `advisor/LongTermMemoryRetrievalAdvisor.java` | 实现 `BaseAdvisor`，语义检索 + 注入 |
| 新建 | `service/MemoryStoreService.java` | Milvus 存储封装 |
| 新建 | `service/MemoryMergeService.java` | LLM 合并去重 |
| 新建 | `service/MemoryExtractionService.java` | @Async 异步提取 |
| 修改 | `config/ai/AppConfiguration.java` | +@EnableAsync + memoryExecutor + windowSize |
| 修改 | `config/ai/PromptConfiguration.java` | defaultAdvisors 链 |
| 修改 | `controller/ai/RagController.java` | 注入 + 异步提取调用 |
| 修改 | `application.properties` | +app.ai.memory.* 配置 |

**编译错误与修复**：

1. **Spring AI 1.0.0 API 变更**：
   - `Document.getContent()` → `Document.getText()`
   - `UserMessage.getContent()` → `UserMessage.getText()`
   - `CallAroundAdvisor` 接口已移除 → 改用 `BaseAdvisor`
   - Prompt 注入方式：`request.prompt().augmentSystemMessage(String)` 追加 system 消息

2. **Milvus VectorStore Builder 枚举类型**：需使用 Milvus SDK 的 `IndexType`、`MetricType` 而非字符串

---

## 2. 优化轮次 1：PromptPipeline 中间件统一重构

### 2.1 问题诊断

| 问题 | 严重程度 | 描述 |
|------|---------|------|
| **Advisor 两条链混用** | 高 | `PromptConfiguration.defaultAdvisors`（3 个全局 Advisor）+ `RagController` 手工 `.advisors(QuestionAnswerAdvisor)` 是两套独立的链，执行顺序不透明 |
| **多租户隔离缺失** | 高 | `LongTermMemoryRetrievalAdvisor` 检索 Milvus 时没有带 userId 过滤，所有用户的记忆混在一起 |

### 2.2 解决方案

**核心思路**：将 Advisor 链从"全局静态 + 动态拼接"的混用模式，改造为统一的责任链中间件。

**新建 `MiddlewareContext`**：不可变运行时上下文，承载请求全生命周期参数：

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | Long | JWT 注入，透传全链（多租户隔离）|
| `conversationId` | String | ChatMemory 隔离 key |
| `query` | String | 用户原始输入 |
| `model` | String | 运行时模型（qwen-plus/qwen-turbo）|
| `temperature` | Double | 运行时温度参数 |
| `systemPrompt` | String | RAG 模式下注入"只依据文档作答"|
| `searchRequest` | SearchRequest | 非 null = RAG 模式，null = 纯对话 |
| `tokenBudget` | Integer | system prompt 最大 token 数预留位 |
| `attributes` | Map<String,Object> | 可扩展：用户画像/traceId/AB 分组 |
| `timings` | Map<String,Long> | 预留各中间件耗时记录 |

**新建 `PromptPipeline`**：统一入口，封装 ChatClient 调用：

```java
// 流式
Flux<String> stream(MiddlewareContext ctx);

// 同步
String call(MiddlewareContext ctx);
```

**多租户升级**（`LongTermMemoryRetrievalAdvisor`）：

- `ThreadLocal<Long> CURRENT_USER_ID`：PromptPipeline 调用前 set，调用后 clear
- Milvus 标量过滤：`FilterExpressionBuilder().eq("user_id", userId)`
- userId 为 null 时降级为全量检索（兼容旧数据）

### 2.3 效果对比

| 维度 | 重构前 | 重构后 |
|------|-------|-------|
| Advisor 管理 | 两套链混用 | 单链有序 |
| 多租户隔离 | ❌ | ✅ userId 标量过滤 |
| Context 透传 | ❌ 无法跨 Advisor | ✅ MiddlewareContext |
| Controller 复杂度 | 460 行 | 200 行（-56%）|
| 条件注入 | Controller 硬编码 | Pipeline shouldApply |

### 2.4 变更文件

| 操作 | 文件 |
|------|------|
| 新建 | `middleware/MiddlewareContext.java` |
| 新建 | `middleware/PromptPipeline.java` |
| 修改 | `advisor/LongTermMemoryRetrievalAdvisor.java` — ThreadLocal userId |
| 修改 | `config/ai/PromptConfiguration.java` — 精简为条件开关 |
| 修改 | `controller/ai/RagController.java` — 从 460 行瘦身为 200 行 |
| 修改 | `service/MemoryExtractionService.java` — extractAndStore +userId 参数 |

---

## 3. 优化轮次 2：用户画像 + 摘要压缩 + 可观测性

### 3.1 用户画像注入（UserProfileAdvisor，order=-200）

**存储**：Redis Hash `user_profile:{userId}`

| 字段 | 说明 | 示例 |
|------|------|------|
| `response_style` | 回复风格偏好 | "简洁" |
| `expertise_level` | 技术水平 | "高级" |
| `known_topics` | 已知话题列表 | "Spring AI,Milvus,Redis,RAG" |
| `language_pref` | 语言偏好 | "zh" |

**特点**：
- O(1) Redis Hash 直查，<1ms
- 每轮对话后 LLM 异步提取画像变更
- `known_topics` 追加合并不覆盖
- TTL 30 天

**Advisor 位置**：order=-200，在链最前面。后续所有中间件（包括长期记忆检索）都能利用画像信息做更精准的匹配。

**文件**：`service/UserProfileService.java` + `advisor/UserProfileAdvisor.java`

### 3.2 渐进式摘要压缩（ConversationCompressionAdvisor，order=-50）

**问题**：对话过长时，Redis 滑动窗口只保留最近 30 轮，早期内容丢失。

**方案**：当消息数达到阈值（默认 40 条 = 20 轮对话）时触发压缩：

```
消息 1-40   → LLM 生成 200 字摘要 → 存入 Milvus conversation_summaries（index=1）
消息 41-80  → LLM 生成 200 字摘要 → 存入 Milvus（index=2）
消息 81-120 → ...
```

**检索**：`ConversationCompressionAdvisor` 在每次 LLM 调用前，用 conversationId 过滤检索历史摘要，按 `summary_index` 排序注入 system prompt。

**文件**：`service/CompressionService.java` + `advisor/ConversationCompressionAdvisor.java`  
**关键配置**：`trigger-round-count=20`, `summary-max-length=200`

### 3.3 Micrometer 可观测性

| 指标 | 类型 | 说明 |
|------|------|------|
| `pipeline.request.duration` | Timer | 请求耗时 p50/p95/p99 |
| `pipeline.request.count{rag=}` | Counter | 按 RAG/非RAG 分组计数 |

**文件**：`middleware/PromptContextHolder.java`（ThreadLocal 透传 conversationId）  
**端点**：`/actuator/prometheus`

---

## 4. 优化轮次 3：Token 预算 + 生命周期 + A/B 实验

### 4.1 Token 预算强制截断（TokenBudgetAdvisor，order=+50）

**位置**：在所有内容注入 Advisor 之后、LLM 调用之前执行。

**优先级截断顺序**（先截 → 后截）：

```
[文档上下文] → [对话历史摘要] → [相关历史记忆] → [用户画像] → [原始prompt 永不截]
```

**Token 估算算法**：

```java
int tokens = 0;
for (char c : text.toCharArray()) {
    if (c < 128) tokens++;       // ASCII: 1 字 = 1 token
    else tokens += 1.5;          // 中文: ~1.5 字/token
}
```

**配置**：`max-system-tokens=3000`

### 4.2 记忆生命周期管理（MemoryLifecycleService）

**双层淘汰机制**：

| 机制 | 触发时机 | 逻辑 |
|------|---------|------|
| 写时容量检查 | 每次存储新记忆前 | Redis 计数器 > 500 → 检索最低分旧记忆 → 删除（限 importance<2.0）|
| 客户端 TTL 过滤 | 每次检索记忆后 | `created_at + 90天 < now` 在 Java 侧过滤 |
| 定时清理 | 每天凌晨 3:00（`@Scheduled`）| 彻底清理过期记忆 |

**淘汰评分公式**：

```java
score = importance - daysAgo * 0.01
// expired 记忆直接返回 -1000（最高优先级删除）
```

**配置**：`ttl-days=90`, `max-memories-per-user=500`, `eviction-importance-threshold=2.0`

### 4.3 A/B 实验分组（AbExperimentService）

**分组分配算法**：

```java
String group = (Math.abs(userId.hashCode()) % 10000 / 10000.0) < 0.5 ? "A" : "B";
// Redis 持久化: experiment_group:{userId} → "A"/"B"，TTL 30 天
// 确定性 sticky 分配，同一用户永远进入同一分组
```

**实验维度对比**：

| 配置项 | A 组（对照） | B 组（实验）|
|--------|------------|------------|
| 检索 topK | 5 | 8 |
| 相似度阈值 | 0.7 | 0.6 |
| 效果 | 精准少召回 | 宽松多召回 |

**数据流**：

```
PromptPipeline → abExperimentService.getGroup(userId)
  → .advisors(longTermMemoryAdvisor, a -> a.param("experiment_group", group))
  → LongTermMemoryRetrievalAdvisor.before()
    → request.context().get("experiment_group") → 动态调整 topK/threshold
```

**配置**：`experiment.enabled=false`（默认关闭）  
**可观测**：`pipeline.request.count{rag=,group=}` 按分组统计

---

## 5. 优化轮次 4：双时间冲突检测机制

### 5.1 设计动机

旧版 LLM 合并策略存在的问题：

- 新旧记忆 merge 后直接 upsert，旧事实的完整内容丢失
- 无法追溯"用户从 A 变成 B"的演变过程
- 合并质量依赖 LLM 表现，不稳定

### 5.2 双时间机制（expire-and-create）

**核心设计**：每条记忆拥有两条时间线——

```
时间轴 ───────────────────────────────────────────>

旧记忆: "用户住在北京"
  ├─ status = "active"
  ├─ created_at = T1
  └─ expired_at = null

              ↓ 新事实出现，冲突检测触发 ↓

旧记忆: "用户住在北京"
  ├─ status = "expired"     ← 被标记为失效，保留
  ├─ created_at = T1
  └─ expired_at = T2

新记忆: "用户住在上海"
  ├─ status = "active"
  ├─ created_at = T2
  └─ supersedes = "<旧记忆 docId>"   ← 事实链追溯
```

**新增元数据字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | String | "active" / "expired" |
| `expired_at` | Long | Unix 时间戳（null = 仍有效）|
| `superseded_by` | String | 新记忆的 docId（旧→新追溯）|
| `supersedes` | String | 旧记忆的 docId（新→旧追溯）|

### 5.3 冲突检测模式

**similarity 模式**（默认，零额外 LLM 成本）：

```
语义相似度 > 0.85 → 直接视为冲突 → expire 旧记忆 + store 新记忆
```

**llm 模式**（精确但多一次 LLM 调用）：

```python
# 配置: conflict-detection-mode=llm
语义相似度 > 0.85 → LLM 判断是否真正矛盾
  ├─ YES（矛盾）→ expire 旧记忆 + store 新记忆
  └─ NO（补充）→ 新旧共存（如 "会 Python" + "会 Java"）
```

**llm 安全兜底**：LLM 调用失败时默认按冲突处理（expire 旧记忆），宁可多失效一条也不漏掉真实矛盾。

### 5.4 检索端：只注入 active 记忆

`LongTermMemoryRetrievalAdvisor.searchMemories()` 增加过滤表达式：

```java
f.and(f.eq("user_id", userId), f.eq("status", "active")).build()
```

expired 记忆不会被注入 LLM 提示词。

### 5.5 淘汰端：expired 记忆优先删除

`MemoryLifecycleService.evictionScore()` 中 expired 记忆返回 -1000（最高优先级淘汰），确保失效事实在容量压力下首先被清理。

### 5.6 两条记忆的生命周期对比

| 维度 | active 记忆 | expired 记忆 |
|------|-----------|------------|
| LLM 注入 | ✅ 检索时匹配 | ❌ 被过滤掉 |
| 审计回溯 | ✅ 当前事实 | ✅ 历史事实，含 `superseded_by` 指针 |
| 容量淘汰 | 受 importance 保护 | 优先删除 |
| 事实链追溯 | 含 `supersedes` 指针 | 含 `superseded_by` 指针 |

### 5.7 变更文件

| 操作 | 文件 |
|------|------|
| 重写 | `service/MemoryMergeService.java` — LLM 合并 → expire-and-create |
| 修改 | `advisor/LongTermMemoryRetrievalAdvisor.java` — 检索过滤 status=active |
| 修改 | `service/MemoryLifecycleService.java` — 淘汰评分 expired 优先 |
| 修改 | `config/ai/MemoryProperties.java` — +conflictDetectionMode |
| 修改 | `application.properties` — +conflict-detection-mode=similarity |

---

## 6. 最终中间件管道全景

经过全部四轮优化后，最终的 Prompt 中间件链为 7 层：

```
请求进入
  │
  ├─ -200  UserProfileAdvisor              → Redis Hash 用户画像
  │         ├─ O(1) 直查，<1ms
  │         └─ 画像字段注入 system prompt 最前面
  │
  ├─ -100  LongTermMemoryRetrievalAdvisor   → Milvus 长期记忆
  │         ├─ userId 标量过滤（多租户隔离）
  │         ├─ status=active 过滤（双时间机制）
  │         ├─ A/B 分组动态调整 topK/threshold
  │         └─ 客户端 TTL 过滤（>90 天剔除）
  │
  ├─  -50  ConversationCompressionAdvisor   → Milvus 对话摘要
  │         └─ 超长对话历史摘要注入
  │
  ├─    0  QuestionAnswerAdvisor           → RAG 文档检索
  │         └─ 仅 RAG 模式注入
  │
  ├─    0  MessageChatMemoryAdvisor         → Redis 短期记忆
  │         └─ 滑动窗口 30 轮
  │
  ├─  +50  TokenBudgetAdvisor              → Token 预算截断
  │         └─ 按优先级截断超预算内容
  │
  ├─ +100  SimpleLoggerAdvisor             → 日志
  │
  └→ LLM (qwen-plus) → SSE 流式响应
                │
     Flux.cache() → 后台异步:
         ├─ MySQL 持久化消息
         ├─ MemoryExtractionService → 写时容量检查 → storeWithMerge
         ├─ MemoryLifecycleService → 计数器更新 + 定时清理
         ├─ UserProfileService → Redis 画像更新
         └─ CompressionService → Milvus 摘要生成
```

---

## 7. 完整配置清单

```properties
# ===== AI 功能开关 =====
app.ai.enabled=true

# ===== 长期记忆 =====
app.ai.memory.long-term.enabled=true
app.ai.memory.long-term.collection-name=long_term_memory
app.ai.memory.long-term.retrieval-top-k=5
app.ai.memory.long-term.retrieval-threshold=0.7
app.ai.memory.long-term.merge-threshold=0.85
app.ai.memory.long-term.extraction-max-per-turn=5
app.ai.memory.long-term.extraction-model=qwen-turbo
app.ai.memory.long-term.ttl-days=90
app.ai.memory.long-term.max-memories-per-user=500
app.ai.memory.long-term.eviction-importance-threshold=2.0
app.ai.memory.long-term.conflict-detection-mode=similarity   # or llm

# ===== 短期记忆 =====
app.ai.memory.short-term.window-size=30

# ===== 用户画像 =====
app.ai.memory.user-profile.enabled=true
app.ai.memory.user-profile.extraction-model=qwen-turbo

# ===== 渐进式摘要压缩 =====
app.ai.memory.compression.enabled=true
app.ai.memory.compression.trigger-round-count=20
app.ai.memory.compression.summary-max-length=200
app.ai.memory.compression.summary-model=qwen-turbo
app.ai.memory.compression.collection-name=conversation_summaries

# ===== Token 预算控制 =====
app.ai.memory.token-budget.enabled=true
app.ai.memory.token-budget.max-system-tokens=3000
app.ai.memory.token-budget.user-message-reserve=2000

# ===== A/B 实验分组 =====
app.ai.memory.experiment.enabled=false
app.ai.memory.experiment.default-group=A
app.ai.memory.experiment.group-a-top-k=5
app.ai.memory.experiment.group-b-top-k=8
app.ai.memory.experiment.group-b-threshold=0.6
app.ai.memory.experiment.group-b-ratio=0.5

# ===== 可观测性 =====
management.endpoints.web.exposure.include=health,info,env,metrics,prometheus
management.metrics.tags.application=knowledge-manager
```

---

## 8. 代码变更统计

| 轮次 | 新建文件 | 修改文件 | 净增代码 |
|------|---------|---------|---------|
| 初始三层存储 | 8 | 4 | ~800 行 |
| PromptPipeline 重构 | 2 | 4 | +200 / -260 |
| 画像+压缩+监控 | 4 | 6 | ~600 行 |
| 预算+淘汰+AB | 3 | 6 | ~550 行 |
| 双时间冲突检测 | 0 | 5 | +120 / -80 |
| **合计** | **17** | **25** | **~1,900 行** |

---

## 9. 企业级能力清单

| 能力 | 实现方式 | 状态 |
|------|---------|------|
| 三层存储（Milvus + Redis + MySQL）| Spring AI + MyBatis + Redis | ✅ |
| 多租户隔离 | Milvus userId 标量过滤 + ThreadLocal 透传 | ✅ |
| 统一中间件管道 | PromptPipeline + getOrder() 排序 | ✅ |
| 用户画像 | Redis Hash O(1) 直查 + LLM 异步更新 | ✅ |
| 渐进式摘要压缩 | LLM 生成摘要 + Milvus 存储 + 按索引检索 | ✅ |
| Token 预算控制 | 按优先级截断（文档→摘要→记忆→画像）| ✅ |
| 记忆生命周期管理 | 写时淘汰 + 定时清理 + 客户端 TTL 过滤 | ✅ |
| 双时间冲突检测 | expire-and-create + 事实链追溯 | ✅ |
| A/B 实验能力 | 哈希分组 sticky 分配 + 策略路由 + 分组 metrics | ✅ |
| 可观测性 | Micrometer Timer + Counter + Prometheus 端点 | ✅ |
| 优雅降级 | 全链路 try-catch，单点失败不阻断对话 | ✅ |
| 配置外化 | @ConfigurationProperties 统一管理 | ✅ |
| 开关控制 | @ConditionalOnProperty 灰度控制 | ✅ |

---

> **后续阶段预告**：
> - 阶段二 —— RAG 调优（混合检索、查询改写、引用溯源）
> - 阶段三 —— LangChain4j 集成
> - 阶段四 —— ReAct Agent
