package zjc.knowledgemanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.config.ai.MemoryProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆提取服务
 * 每轮对话结束后，用 LLM 异步提取关键信息（事实/决策/偏好/实体），
 * 经过去重合并后写入 Milvus 长期记忆。
 */
@Service
@ConditionalOnBean(name = "memoryVectorStore")
public class MemoryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractionService.class);

    private final ChatClient extractionClient;
    private final MemoryMergeService memoryMergeService;
    private final MemoryProperties memoryProperties;
    private final MemoryLifecycleService memoryLifecycleService;
    private final ObjectMapper objectMapper;
    private final LlmMetricsService llmMetricsService;

    // 记忆提取 prompt 模板（严格JSON约束）
    private static final String EXTRACTION_PROMPT = """
            严格遵守：仅输出JSON数组，不要markdown代码块标记，不要任何前缀或解释，不要注释。

            从以下对话中提取值得长期记住的关键信息。

            用户消息: %s
            助手回复: %s

            记忆类型:
            - fact: 客观事实
            - decision: 用户做出的决定或选择
            - preference: 用户偏好或习惯
            - entity: 关键实体信息

            要求:
            1. 每条记忆必须独立可理解，不依赖对话上下文
            2. 简洁，每条不超过50字
            3. 最多%d条，宁缺毋滥
            4. 评估重要性(1-5): 5=必须记住, 1=可能有用

            输出格式示例: [{"type":"fact","content":"知识库包含Spring AI文档","importance":4}]
            无值得记录则返回: []""";

    public MemoryExtractionService(@Qualifier("extractionChatClient") ChatClient extractionClient,
                                   MemoryMergeService memoryMergeService,
                                   MemoryProperties memoryProperties,
                                   MemoryLifecycleService memoryLifecycleService,
                                   LlmMetricsService llmMetricsService) {
        this.extractionClient = extractionClient;
        this.memoryMergeService = memoryMergeService;
        this.memoryProperties = memoryProperties;
        this.memoryLifecycleService = memoryLifecycleService;
        this.objectMapper = new ObjectMapper();
        this.llmMetricsService = llmMetricsService;
    }

    /**
     * 从一轮对话中提取记忆并存储
     * 在 RagController 的流式响应结束后异步调用
     *
     * @param conversationId    对话ID
     * @param userId            用户ID（用于多租户过滤）
     * @param userQuery         用户输入
     * @param assistantResponse 助手回复
     */
    public void extractAndStore(String conversationId, Long userId, String userQuery,
                                String assistantResponse) {
        MemoryProperties.LongTerm config = memoryProperties.getLongTerm();
        if (!config.isEnabled()) return;

        try {
            // ① LLM 提取
            List<Map<String, Object>> entries = extract(userQuery, assistantResponse,
                    config.getExtractionMaxPerTurn());

            if (entries.isEmpty()) {
                log.debug("本轮无值得记录的记忆, conversationId={}", conversationId);
                return;
            }

            // ② 逐条存储（带合并去重）
            long now = System.currentTimeMillis() / 1000;
            int storedCount = 0;
            for (Map<String, Object> entry : entries) {
                String type = (String) entry.get("type");
                String content = (String) entry.get("content");
                int importance = entry.get("importance") instanceof Number
                        ? ((Number) entry.get("importance")).intValue() : 3;

                if (content == null || content.isBlank()) continue;

                // 写时容量检查：超上限时淘汰低分旧记忆
                memoryLifecycleService.enforceCapacityBeforeStore(userId);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("memory_type", type != null ? type : "fact");
                metadata.put("conversation_id", conversationId);
                metadata.put("user_id", userId);
                metadata.put("importance", importance);
                metadata.put("created_at", now);

                memoryMergeService.storeWithMerge(content, metadata,
                        config.getMergeThreshold());
                storedCount++;
            }

            // 递增 Redis 计数器
            if (storedCount > 0) {
                memoryLifecycleService.incrementCount(userId);
            }

            log.info("提取并存储 {} 条记忆, userId={}, conversationId={}",
                    storedCount, userId, conversationId);

        } catch (Exception e) {
            log.warn("记忆提取失败, conversationId={}: {}", conversationId, e.getMessage());
        }
    }

    /**
     * 调用 LLM 从对话中提取关键记忆
     * @return JSON解析后的记忆列表
     */
    private List<Map<String, Object>> extract(String userQuery, String assistantResponse,
                                              int maxEntries) {
        long startTime = System.currentTimeMillis();
        try {
            String prompt = String.format(EXTRACTION_PROMPT, userQuery, assistantResponse, maxEntries);
            String rawJson = extractionClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String json = rawJson.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.lastIndexOf("```")).trim();
                }
            }

            List<Map<String, Object>> result = objectMapper.readValue(json, 
                    new TypeReference<List<Map<String, Object>>>() {});
            
            llmMetricsService.recordMemoryExtraction(
                    memoryProperties.getLongTerm().getExtractionModel(),
                    System.currentTimeMillis() - startTime,
                    true,
                    null);
            
            return result;

        } catch (Exception e) {
            log.warn("LLM 提取或JSON解析失败: {}", e.getMessage());
            llmMetricsService.recordMemoryExtraction(
                    memoryProperties.getLongTerm().getExtractionModel(),
                    System.currentTimeMillis() - startTime,
                    false,
                    e.getClass().getSimpleName());
            return List.of();
        }
    }
}
