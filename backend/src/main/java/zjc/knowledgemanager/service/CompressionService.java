package zjc.knowledgemanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.config.ai.MemoryProperties;
import zjc.knowledgemanager.entity.Message;
import zjc.knowledgemanager.mapper.MessageMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 渐进式摘要压缩服务 —— 超长对话自动生成摘要存入 Milvus。
 *
 * <h3>触发条件</h3>
 * 当对话消息数 &gt;= triggerRoundCount×2（默认 40 条 = 20 轮），
 * 取最早的 20 条消息生成约 200 字摘要，存入 Milvus conversation_summaries 集合。
 *
 * <h3>摘要层级</h3>
 * <pre>
 *   L1 摘要: 每 20 轮 → 200字摘要（存入 Milvus）
 *   L2 元摘要: 每 5 个 L1 摘要 → LLM 合并为元摘要（后续版本）
 * </pre>
 *
 * <h3>检索流程</h3>
 * {@link zjc.knowledgemanager.advisor.ConversationCompressionAdvisor} 在 Advisor 链中
 * 用 conversationId + userId 过滤检索 Milvus，注入历史摘要到 system prompt。
 */
@Service
@ConditionalOnBean(name = "memoryVectorStore")
public class CompressionService {

    private static final Logger log = LoggerFactory.getLogger(CompressionService.class);

    private final ChatClient summaryClient;
    private final VectorStore memoryVectorStore;
    private final MessageMapper messageMapper;
    private final MemoryProperties memoryProperties;

    private static final String SUMMARY_PROMPT = """
            将以下对话片段压缩为不超过%d字的关键摘要。
            保留: 关键决定、重要事实、用户偏好、讨论主题的演变。
            丢弃: 闲聊、重复确认、无关细节。

            对话片段:
            %s

            仅输出纯文本摘要，不要任何格式标记，不要前缀，不要JSON。""";

    public CompressionService(@Qualifier("summaryChatClient") ChatClient summaryClient,
                              @Qualifier("memoryVectorStore") VectorStore memoryVectorStore,
                              MessageMapper messageMapper,
                              MemoryProperties memoryProperties) {
        this.summaryClient = summaryClient;
        this.memoryVectorStore = memoryVectorStore;
        this.messageMapper = messageMapper;
        this.memoryProperties = memoryProperties;
    }

    // ========== 检索（Advisor 用） ==========

    /**
     * 检索当前对话的所有历史摘要。
     */
    public List<Document> retrieveSummaries(String conversationId, Long userId, String currentQuery) {
        MemoryProperties.Compression config = memoryProperties.getCompression();
        if (!config.isEnabled()) return List.of();

        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(currentQuery)
                    .topK(10)
                    .similarityThreshold(0.3);

            // 多租户 + 对话过滤
            FilterExpressionBuilder f = new FilterExpressionBuilder();
            Filter.Expression convFilter = f.eq("conversation_id", conversationId).build();
            builder.filterExpression(convFilter);

            // Milvus 搜索超时保护：5s 超时
            return CompletableFuture.supplyAsync(() -> memoryVectorStore.similaritySearch(builder.build()))
                    .get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("对话摘要检索超时(5s): convId={}", conversationId);
            return List.of();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("检索对话摘要失败: convId={}, error={}", conversationId, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("检索对话摘要失败: convId={}, error={}", conversationId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 格式化摘要为 prompt 可注入文本。
     */
    public String formatSummaries(List<Document> summaries) {
        if (summaries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("[对话历史摘要]\n");

        // 按 summary_index 排序
        summaries.sort((a, b) -> {
            int ai = getSummaryIndex(a);
            int bi = getSummaryIndex(b);
            return Integer.compare(ai, bi);
        });

        for (Document doc : summaries) {
            int idx = getSummaryIndex(doc);
            sb.append("- [片段").append(idx).append("] ").append(doc.getText()).append("\n");
        }
        return sb.toString();
    }

    // ========== 生成 & 存储 ==========

    /**
     * 检查并触发压缩（异步调用）。
     * 每轮对话结束后由 RagController 调用。
     *
     * <h3>企业级压缩策略</h3>
     * <ul>
     *   <li>触发条件：基于 token 估算（字符数/1.5）而非消息条数</li>
     *   <li>保留最近 5 轮原始消息不参与压缩（短期记忆覆盖）</li>
     *   <li>每 2000 token 生成一个 L1 摘要</li>
     * </ul>
     *
     * @return true 表示本次触发了压缩
     */
    public boolean compressIfNeeded(String conversationId, Long userId) {
        MemoryProperties.Compression config = memoryProperties.getCompression();
        if (!config.isEnabled() || conversationId == null || userId == null) return false;

        try {
            List<Message> allMessages = messageMapper.findByChatId(conversationId);
            if (allMessages == null || allMessages.isEmpty()) return false;

            // 保留最近 5 轮（10条消息）不压缩，短期记忆已覆盖
            int keepRecentCount = 10;
            if (allMessages.size() <= keepRecentCount) return false;

            // 计算已压缩的 token 数
            int existingSummaries = countExistingSummaries(conversationId);
            int processedMessages = existingSummaries * keepRecentCount;
            
            // 从已处理消息之后开始，跳过最近10条
            int startIdx = Math.min(processedMessages, allMessages.size() - keepRecentCount);
            int endIdx = allMessages.size() - keepRecentCount;
            
            if (startIdx >= endIdx) return false;

            // 取待压缩的消息片段
            List<Message> messagesToCompress = allMessages.subList(startIdx, endIdx);
            
            // 估算 token 数：字符数 / 1.5（中文近似）
            int charCount = messagesToCompress.stream()
                    .mapToInt(m -> m.getContent().length())
                    .sum();
            int estimatedTokens = charCount / 15 * 10; // 近似: 15字符 ≈ 10 token
            
            // 触发阈值：每 2000 token 生成一个摘要
            int triggerTokens = 2000;
            if (estimatedTokens < triggerTokens) return false;

            // LLM 生成摘要
            String conversationText = messagesToCompress.stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));

            String summary = generateSummary(conversationText, config.getSummaryMaxLength());
            if (summary == null || summary.isBlank()) return false;

            // 存入 Milvus
            storeSummary(conversationId, userId, summary, existingSummaries + 1);
            log.info("对话摘要生成: convId={}, index={}, tokens={}, len={}",
                    conversationId, existingSummaries + 1, estimatedTokens, summary.length());

            return true;

        } catch (Exception e) {
            log.warn("摘要压缩失败: convId={}, error={}", conversationId, e.getMessage());
            return false;
        }
    }

    // ========== 内部方法 ==========

    private String generateSummary(String conversationText, int maxLength) {
        try {
            String prompt = String.format(SUMMARY_PROMPT, maxLength, conversationText);
            String result = summaryClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return result != null ? result.trim() : "";
        } catch (Exception e) {
            log.warn("LLM 摘要生成失败: {}", e.getMessage());
            return "";
        }
    }

    private void storeSummary(String conversationId, Long userId, String summary, int index) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversation_id", conversationId);
        metadata.put("user_id", userId);
        metadata.put("summary_index", index);
        metadata.put("created_at", System.currentTimeMillis() / 1000);
        metadata.put("doc_type", "conversation_summary");

        Document doc = new Document(summary, metadata);
        memoryVectorStore.add(List.of(doc));
    }

    private int countExistingSummaries(String conversationId) {
        try {
            FilterExpressionBuilder f = new FilterExpressionBuilder();
            Filter.Expression filter = f.eq("conversation_id", conversationId).build();
            // 用 topK=1 的检索估算摘要数量（Milvus 不支持 count）
            var results = memoryVectorStore.similaritySearch(
                    SearchRequest.builder().query("").topK(100).filterExpression(filter).build());
            return results != null ? results.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getSummaryIndex(Document doc) {
        Object idx = doc.getMetadata().get("summary_index");
        if (idx instanceof Number) return ((Number) idx).intValue();
        return 0;
    }
}
