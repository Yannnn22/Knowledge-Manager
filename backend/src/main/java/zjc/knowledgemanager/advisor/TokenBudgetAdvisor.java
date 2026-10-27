package zjc.knowledgemanager.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import zjc.knowledgemanager.config.ai.MemoryProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Token 预算截断 Advisor —— 中间件链 order=+50。
 *
 * <p>在所有内容注入 Advisor（画像/记忆/摘要/文档）执行完毕后、LLM 调用前运行:
 * <ol>
 *   <li>读取当前 system prompt 全文</li>
 *   <li>估算 token 数（中文 ~1.5 字/token，英文 ~4 字/token）</li>
 *   <li>若超过 {@code maxSystemTokens}，按优先级从低到高截断注入段落</li>
 * </ol>
 *
 * <h3>截断优先级（从先截断到最后截断）</h3>
 * <ol>
 *   <li>[文档上下文] — RAG 检索结果（最容易冗余）</li>
 *   <li>[对话历史摘要] — 压缩摘要（历史信息）</li>
 *   <li>[相关历史记忆] — 长期记忆（跨对话）</li>
 *   <li>[用户画像] — 画像信息（每轮必须）</li>
 *   <li>[原始 system prompt] — 永不截断</li>
 * </ol>
 *
 * <h3>中间件顺序全景</h3>
 * <pre>
 *   -200  UserProfileAdvisor
 *   -100  LongTermMemoryRetrievalAdvisor
 *    -50  ConversationCompressionAdvisor
 *      0  QuestionAnswerAdvisor / MessageChatMemoryAdvisor
 *    +50  TokenBudgetAdvisor       ← 本中间件
 *   +100  SimpleLoggerAdvisor
 * </pre>
 */
@Component
public class TokenBudgetAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenBudgetAdvisor.class);

    /** 注入段落的标记前缀，按截断优先级从高（先截）到低排列 */
    private static final String[] SECTION_MARKERS = {
            "[文档上下文]",      // RAG 文档检索结果，最先截断
            "[对话历史摘要]",    // 压缩摘要
            "[相关历史记忆]",    // 长期记忆
            "[用户画像]"         // 用户画像，最后截断
    };

    private final MemoryProperties memoryProperties;

    public TokenBudgetAdvisor(MemoryProperties memoryProperties) {
        this.memoryProperties = memoryProperties;
    }

    @Override
    public String getName() {
        return "token_budget";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        long start = System.nanoTime();
        MemoryProperties.TokenBudget config = memoryProperties.getTokenBudget();
        if (!config.isEnabled()) {
            return request;
        }

        try {
            String systemPrompt = request.prompt().getSystemMessage().getText();
            if (systemPrompt == null || systemPrompt.isEmpty()) {
                return request;
            }

            int estimatedTokens = estimateTokens(systemPrompt);
            int budget = config.getMaxSystemTokens();

            if (estimatedTokens <= budget) {
                return request; // 未超预算，无需截断
            }

            // 超预算：按优先级截断
            String trimmed = trimToBudget(systemPrompt, estimatedTokens, budget);

            if (trimmed.equals(systemPrompt)) {
                return request; // 无法进一步截断
            }

            int trimmedTokens = estimateTokens(trimmed);
            log.info("Token 预算截断: {} → {} tokens (budget={}), 截断 {} 字符",
                    estimatedTokens, trimmedTokens, budget,
                    systemPrompt.length() - trimmed.length());

            // 用截断后的内容替换 system message，保留 user message 和 options
            List<Message> newMessages = new ArrayList<>();
            newMessages.add(new SystemMessage(trimmed));
            for (Message msg : request.prompt().getInstructions()) {
                if (!(msg instanceof SystemMessage)) {
                    newMessages.add(msg);
                }
            }
            Prompt newPrompt = new Prompt(newMessages, request.prompt().getOptions());
            return request.mutate().prompt(newPrompt).build();

        } catch (Exception e) {
            log.warn("Token 预算检查失败, 跳过截断: {}", e.getMessage());
            return request;
        } finally {
            log.debug("[token_budget] took {}ms", (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    // ========== Token 估算 ==========

    /**
     * 粗略估算 token 数。
     * 中文字符 ~1.5 字/token，ASCII 字符 ~4 字/token。
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;

        int cjkChars = 0;
        int asciiChars = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                cjkChars++;
            } else {
                asciiChars++;
            }
        }

        return (int) Math.ceil(cjkChars / 1.5 + asciiChars / 4.0);
    }

    // ========== 截断逻辑 ==========

    /**
     * 按优先级截断 system prompt，使其 token 数 <= budget。
     */
    private String trimToBudget(String systemPrompt, int currentTokens, int budget) {
        String result = systemPrompt;

        for (String marker : SECTION_MARKERS) {
            if (estimateTokens(result) <= budget) {
                break;
            }

            int markerIndex = result.indexOf(marker);
            if (markerIndex < 0) {
                continue; // 该段落不存在
            }

            // 找到下一个段落标记或文本末尾
            int nextMarkerIndex = findNextMarker(result, markerIndex + marker.length());
            int sectionEnd = nextMarkerIndex > 0 ? nextMarkerIndex : result.length();

            // 截断该段落
            result = result.substring(0, markerIndex) + result.substring(sectionEnd);
            log.debug("截断段落: {}", marker);
        }

        return result;
    }

    /**
     * 从指定位置开始，查找下一个段落标记的位置。
     */
    private int findNextMarker(String text, int fromIndex) {
        int earliest = -1;
        for (String marker : SECTION_MARKERS) {
            int idx = text.indexOf(marker, fromIndex);
            if (idx >= 0 && (earliest < 0 || idx < earliest)) {
                earliest = idx;
            }
        }
        // 也检查原始 system prompt 的开头标记
        int sysIdx = text.indexOf("你将作为一名", fromIndex);
        if (sysIdx >= 0 && (earliest < 0 || sysIdx < earliest)) {
            earliest = sysIdx;
        }
        return earliest;
    }
}
