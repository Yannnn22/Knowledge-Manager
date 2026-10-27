package zjc.knowledgemanager.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import zjc.knowledgemanager.config.ai.MemoryProperties;
import zjc.knowledgemanager.service.AbExperimentService;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 长期记忆检索 Advisor —— 中间件链 order=-100。
 *
 * <p>每次 LLM 调用前:
 * <ol>
 *   <li>从 ThreadLocal 读取当前 userId（由 {@code PromptPipeline} 注入）</li>
 *   <li>从 advisor context 读取实验分组（A/B）</li>
 *   <li>按分组调整 topK + 相似度阈值</li>
 *   <li>用 userId 过滤 + 语义检索 Milvus long_term_memory 集合</li>
 *   <li>客户端 TTL 过滤：剔除超过 ttlDays 的过期记忆</li>
 *   <li>按 importance×0.6 + freshness×0.4 排序</li>
 *   <li>将格式化后的记忆注入 system prompt</li>
 * </ol>
 *
 * <h3>多租户隔离</h3>
 * 通过 Milvus 标量过滤 {@code user_id == 当前userId} 确保 A 用户的
 * 记忆不会被 B 用户检索到。userId 为 null 时降级为全量检索（兼容旧数据）。
 *
 * <h3>A/B 实验分组</h3>
 * PromptPipeline 通过 advisor param {@code "experiment_group"} 传入分组。
 * B 组使用更大的 topK 和更低的阈值，召回更多记忆进行对比实验。
 *
 * <h3>客户端 TTL 过滤</h3>
 * 检索后，在 Java 侧过滤掉 {@code created_at + ttlDays * 86400 < now} 的记忆。
 * 这是一种软过期，避免 Milvus 中堆积大量陈旧记忆。
 */
@Component
public class LongTermMemoryRetrievalAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryRetrievalAdvisor.class);

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private final ObjectProvider<VectorStore> memoryVectorStoreProvider;
    private final MemoryProperties memoryProperties;
    private final AbExperimentService abExperimentService;

    public LongTermMemoryRetrievalAdvisor(
            @Qualifier("memoryVectorStore") ObjectProvider<VectorStore> memoryVectorStoreProvider,
            MemoryProperties memoryProperties,
            AbExperimentService abExperimentService) {
        this.memoryVectorStoreProvider = memoryVectorStoreProvider;
        this.memoryProperties = memoryProperties;
        this.abExperimentService = abExperimentService;
    }

    // ========== ThreadLocal 管理（兼容旧代码） ==========

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clearUserId() {
        CURRENT_USER_ID.remove();
    }

    // ========== 记忆检索门控 ==========

    /**
     * 判断是否需要检索长期记忆。
     * <p>企业级策略：
     * <ul>
     *   <li>强制检索：包含记忆指代关键词（之前、上次、记得、我的、曾经等）</li>
     *   <li>条件检索：消息长度>4且不是简单问候</li>
     *   <li>跳过检索：简单问候、确认、单一字符等</li>
     * </ul>
     */
    private boolean shouldRetrieveMemory(String userText) {
        String text = userText.trim();
        
        // 强制检索关键词（指代记忆）
        String[] forceRetrieveKeywords = {
            "之前", "上次", "刚才", "刚才说", "之前说", "上次说",
            "记得", "还记得", "我的", "我是", "我叫",
            "曾经", "之前提到", "上次提到", "之前讨论", "上次讨论",
            "之前的", "上次的", "刚才的", "之前做的", "上次做的",
            "之前的问题", "上次的问题", "之前的回答", "上次的回答"
        };
        
        for (String keyword : forceRetrieveKeywords) {
            if (text.contains(keyword)) {
                log.debug("命中强制检索关键词: {}", keyword);
                return true;
            }
        }
        
        // 跳过检索：简单问候
        String[] skipKeywords = {
            "你好", "您好", "hello", "hi", "哈喽", "嗨",
            "在吗", "在不在", "有人吗", "请问", "请问在吗",
            "谢谢", "谢谢你", "感谢", "辛苦了", "拜拜", "再见",
            "ok", "好的", "是的", "对的", "没错", "嗯嗯", "嗯",
            "哈哈", "呵呵", "好", "行", "可以", "没问题"
        };
        
        String lowerText = text.toLowerCase();
        for (String keyword : skipKeywords) {
            if (lowerText.equals(keyword) || 
                (lowerText.length() <= 8 && lowerText.contains(keyword))) {
                log.debug("命中跳过关键词: {}", keyword);
                return false;
            }
        }
        
        // 条件检索：消息长度>4且不是单一字符
        if (text.length() <= 2) {
            return false;
        }
        
        // 默认执行检索
        return true;
    }

    /**
     * 从advisor context读取userId（优先），fallback到ThreadLocal。
     */
    private Long extractUserId(ChatClientRequest request) {
        try {
            Object userIdObj = request.context().get("userId");
            if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
        } catch (Exception ignored) {
        }
        return CURRENT_USER_ID.get();
    }

    // ========== Advisor 接口 ==========

    @Override
    public String getName() {
        return "long_term_memory_retrieval";
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        long start = System.nanoTime();
        MemoryProperties.LongTerm config = memoryProperties.getLongTerm();
        if (!config.isEnabled()) {
            return request;
        }

        String userText = extractUserText(request);
        if (userText == null || userText.isBlank()) {
            return request;
        }

        Long userId = extractUserId(request);
        if (userId == null) {
            log.debug("无用户ID，跳过长期记忆检索");
            return request;
        }

        // 记忆检索门控：判断是否需要检索
        if (!shouldRetrieveMemory(userText)) {
            log.debug("门控判定无需检索记忆: query={}", truncate(userText, 40));
            return request;
        }

        String experimentGroup = extractExperimentGroup(request);
        int topK = abExperimentService.getTopK(experimentGroup);
        double threshold = abExperimentService.getThreshold(experimentGroup);

        try {
            List<Document> memories = searchMemories(userText, userId, topK, threshold);

            // 客户端 TTL 过滤
            memories = filterExpiredMemories(memories, config.getTtlDays());

            // 语义去重：去除相似度>0.85的重复记忆
            memories = deduplicateMemories(memories);

            if (memories.isEmpty()) {
                log.debug("长期记忆检索无结果: userId={}, group={}, query={}",
                        userId, experimentGroup, truncate(userText, 40));
                return request;
            }

            // 按 importance×0.6 + freshness×0.4 排序
            memories.sort(Comparator.comparingDouble(this::memoryScore).reversed());

            String memoryContext = formatMemories(memories);
            String augmentedContext = "\n\n[相关历史记忆]\n" + memoryContext;

            log.debug("长期记忆注入: userId={}, group={}, count={}, topK={}, threshold={}",
                    userId, experimentGroup, memories.size(), topK, threshold);

            return request.mutate()
                    .prompt(request.prompt().augmentSystemMessage(augmentedContext))
                    .build();

        } catch (Exception e) {
            log.warn("长期记忆检索失败, 回退到普通对话: userId={}, error={}",
                    userId, e.getMessage());
            return request;
        } finally {
            log.debug("[long_term_memory] took {}ms", (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    // ========== 检索逻辑 ==========

    /**
     * 用 userId + status=active 过滤 + 语义检索 Milvus。
     * 只检索当前有效（未被新事实取代）的记忆。
     * userId 为 null 时降级为无用户过滤全量检索。
     */
    private List<Document> searchMemories(String query, Long userId, int topK, double threshold) {
        VectorStore vs = memoryVectorStoreProvider.getIfAvailable();
        if (vs == null) {
            log.debug("长期记忆向量存储不可用，跳过检索");
            return List.of();
        }

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold);

        try {
            FilterExpressionBuilder f = new FilterExpressionBuilder();

            if (userId != null) {
                Filter.Expression filter = f.and(
                        f.eq("user_id", userId),
                        f.eq("status", "active")).build();
                builder.filterExpression(filter);
            } else {
                builder.filterExpression(f.eq("status", "active").build());
            }
        } catch (Exception e) {
            log.warn("添加检索过滤条件失败, 降级为无过滤检索: {}", e.getMessage());
        }

        try {
            return CompletableFuture.supplyAsync(() -> vs.similaritySearch(builder.build()))
                    .get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("长期记忆检索超时(5s): userId={}, query={}", userId, truncate(query, 40));
            return List.of();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("长期记忆检索失败: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    // ========== 客户端 TTL 过滤 ==========

    /**
     * 过滤掉超过 TTL 的过期记忆（时间维度）。
     * 此方法作为 Milvus status=active 过滤的补充——理论上 status=active 的记忆
     * 不会超 TTL（MemoryLifecycleService 会清理），但作为防御性编程保留。
     *
     * @param memories  检索到的记忆列表
     * @param ttlDays   TTL 天数（<=0 表示不过滤）
     * @return 过滤后的列表
     */
    private List<Document> filterExpiredMemories(List<Document> memories, int ttlDays) {
        if (ttlDays <= 0 || memories.isEmpty()) {
            return memories;
        }

        long now = System.currentTimeMillis() / 1000;
        long ttlSeconds = (long) ttlDays * 86400;

        return memories.stream()
                .filter(doc -> {
                    Object created = doc.getMetadata().get("created_at");
                    if (created instanceof Number) {
                        long createdAt = ((Number) created).longValue();
                        return (now - createdAt) < ttlSeconds;
                    }
                    return true; // 无时间戳的记忆不过滤
                })
                .collect(Collectors.toList());
    }

    // ========== 评分 & 格式化 ==========

    private double memoryScore(Document doc) {
        double importance = 3.0;
        Object imp = doc.getMetadata().get("importance");
        if (imp instanceof Number) {
            importance = ((Number) imp).doubleValue();
        }

        double daysAgo = 0;
        Object created = doc.getMetadata().get("created_at");
        if (created instanceof Number) {
            long createdAt = ((Number) created).longValue();
            long now = System.currentTimeMillis() / 1000;
            daysAgo = (now - createdAt) / 86400.0;
        }

        double freshness = Math.max(0, 1.0 - daysAgo / 90.0);
        return importance * 0.6 + freshness * 0.4;
    }

    private String formatMemories(List<Document> memories) {
        return memories.stream()
                .map(doc -> {
                    String type = (String) doc.getMetadata().getOrDefault("memory_type", "fact");
                    return "- [" + type + "] " + doc.getText();
                })
                .collect(Collectors.joining("\n"));
    }

    // ========== 语义去重 ==========

    /**
     * 语义去重：去除相似度>0.85的重复记忆，保留importance最高的。
     * 使用简单的Jaccard相似度作为快速去重。
     */
    private List<Document> deduplicateMemories(List<Document> memories) {
        if (memories.size() <= 1) {
            return memories;
        }

        List<Document> result = new java.util.ArrayList<>();
        for (int i = 0; i < memories.size(); i++) {
            Document doc = memories.get(i);
            boolean isDuplicate = false;
            
            for (Document existing : result) {
                double similarity = computeSimilarity(doc.getText(), existing.getText());
                if (similarity > 0.85) {
                    // 重复：保留importance更高的
                    double docImportance = getImportance(doc);
                    double existingImportance = getImportance(existing);
                    if (docImportance > existingImportance) {
                        result.remove(existing);
                    } else {
                        isDuplicate = true;
                    }
                    break;
                }
            }
            
            if (!isDuplicate) {
                result.add(doc);
            }
        }
        
        if (result.size() < memories.size()) {
            log.debug("记忆去重: 原始{}条 → 去重后{}条", memories.size(), result.size());
        }
        
        return result;
    }

    /**
     * 计算文本相似度（Jaccard相似度）。
     */
    private double computeSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        java.util.Set<String> words1 = tokenize(text1);
        java.util.Set<String> words2 = tokenize(text2);
        
        if (words1.isEmpty() && words2.isEmpty()) return 1.0;
        if (words1.isEmpty() || words2.isEmpty()) return 0.0;
        
        java.util.Set<String> intersection = new java.util.HashSet<>(words1);
        intersection.retainAll(words2);
        
        java.util.Set<String> union = new java.util.HashSet<>(words1);
        union.addAll(words2);
        
        return (double) intersection.size() / union.size();
    }

    /**
     * 简单分词：按字分词（中文）+ 按空白分词（英文）。
     */
    private java.util.Set<String> tokenize(String text) {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                tokens.add(String.valueOf(c));
            }
        }
        
        for (String word : text.split("\\s+")) {
            if (word.length() > 1) {
                tokens.add(word.toLowerCase());
            }
        }
        
        return tokens;
    }

    private double getImportance(Document doc) {
        Object imp = doc.getMetadata().get("importance");
        if (imp instanceof Number) {
            return ((Number) imp).doubleValue();
        }
        return 3.0;
    }

    // ========== 工具方法 ==========

    /**
     * 从 advisor context 读取实验分组。
     */
    private String extractExperimentGroup(ChatClientRequest request) {
        try {
            Object group = request.context().get("experiment_group");
            if (group instanceof String) {
                return (String) group;
            }
        } catch (Exception ignored) {
        }
        return memoryProperties.getExperiment().getDefaultGroup();
    }

    private String extractUserText(ChatClientRequest request) {
        try {
            var userMsg = request.prompt().getUserMessage();
            if (userMsg != null) {
                return userMsg.getText();
            }
        } catch (Exception ignored) {
            // fallback
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
