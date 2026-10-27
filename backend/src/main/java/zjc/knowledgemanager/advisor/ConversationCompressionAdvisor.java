package zjc.knowledgemanager.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import zjc.knowledgemanager.config.ai.MemoryProperties;
import zjc.knowledgemanager.middleware.PromptContextHolder;
import zjc.knowledgemanager.service.CompressionService;

import java.util.List;

/**
 * 渐进式摘要压缩 Advisor —— 中间件链 order=-50。
 *
 * <p>在长期记忆检索之后、RAG 文档检索之前执行:
 * <ol>
 *   <li>从 ThreadLocal 读取 userId + conversationId</li>
 *   <li>检索 Milvus conversation_summaries 集合中该对话的所有摘要</li>
 *   <li>格式化后注入 system prompt</li>
 * </ol>
 *
 * <h3>中间件顺序全景</h3>
 * <pre>
 *   -200  UserProfileAdvisor              → 用户画像
 *   -100  LongTermMemoryRetrievalAdvisor  → 长期记忆
 *    -50  ConversationCompressionAdvisor  → 对话摘要压缩  ← 新增
 *      0  QuestionAnswerAdvisor           → RAG 文档检索
 *      0  MessageChatMemoryAdvisor        → Redis 短期记忆
 *   +100  SimpleLoggerAdvisor             → 日志
 * </pre>
 */
@Component
public class ConversationCompressionAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ConversationCompressionAdvisor.class);

    private final ObjectProvider<CompressionService> compressionServiceProvider;
    private final MemoryProperties memoryProperties;

    public ConversationCompressionAdvisor(ObjectProvider<CompressionService> compressionServiceProvider,
                                          MemoryProperties memoryProperties) {
        this.compressionServiceProvider = compressionServiceProvider;
        this.memoryProperties = memoryProperties;
    }

    @Override
    public String getName() {
        return "conversation_compression";
    }

    @Override
    public int getOrder() {
        return -50;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        long start = System.nanoTime();
        MemoryProperties.Compression config = memoryProperties.getCompression();
        if (!config.isEnabled()) {
            return request;
        }

        Long userId = LongTermMemoryRetrievalAdvisor.getCurrentUserId();
        String conversationId = PromptContextHolder.getConversationId();
        if (userId == null || conversationId == null) {
            return request;
        }

        String userText = extractUserText(request);
        if (userText == null || userText.isBlank()) {
            return request;
        }

        try {
            CompressionService compressionService = compressionServiceProvider.getIfAvailable();
            if (compressionService == null) {
                return request;
            }

            List<Document> summaries = compressionService.retrieveSummaries(
                    conversationId, userId, userText);

            if (summaries.isEmpty()) {
                return request;
            }

            String context = compressionService.formatSummaries(summaries);
            log.debug("对话摘要注入: convId={}, count={}", conversationId, summaries.size());

            return request.mutate()
                    .prompt(request.prompt().augmentSystemMessage("\n\n" + context))
                    .build();

        } catch (Exception e) {
            log.warn("对话摘要检索失败: convId={}, error={}", conversationId, e.getMessage());
            return request;
        } finally {
            log.debug("[conversation_compression] took {}ms", (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    // ========== 工具方法 ==========

    private String extractUserText(ChatClientRequest request) {
        try {
            var userMsg = request.prompt().getUserMessage();
            if (userMsg != null) return userMsg.getText();
        } catch (Exception ignored) {
        }
        return null;
    }
}
