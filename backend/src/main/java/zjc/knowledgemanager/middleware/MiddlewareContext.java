package zjc.knowledgemanager.middleware;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中间件上下文 —— 携带一次 LLM 调用所需的全部运行时元信息。
 *
 * <p>设计原则:
 * <ul>
 *   <li><b>不可变</b>: 通过 Builder 构建，线程安全</li>
 *   <li><b>可扩展</b>: attributes Map 承载自定义数据（用户画像、AB 实验分组等）</li>
 *   <li><b>可观测</b>: timings Map 记录各中间件耗时（ms）</li>
 *   <li><b>租户隔离</b>: userId 在中间件链间透传，用于 Milvus 过滤</li>
 * </ul>
 *
 * <p>典型用法:
 * <pre>{@code
 * MiddlewareContext ctx = MiddlewareContext.builder()
 *     .userId(1001L)
 *     .conversationId("chat-uuid-123")
 *     .query("Spring AI 怎么用")
 *     .model("Qwen/Qwen3.5-27B")
 *     .temperature(0.7)
 *     .searchRequest(searchRequest)  // null = 非 RAG 模式
 *     .tokenBudget(4096)
 *     .build();
 * }</pre>
 */
public class MiddlewareContext {

    /** 用户 ID（JWT 解析得到）,用于多租户隔离 */
    private final Long userId;

    /** 对话 ID,用于 ChatMemory 隔离和消息落库 */
    private final String conversationId;

    /** 用户原始查询文本 */
    private final String query;

    /** LLM 模型名称 */
    private final String model;

    /** 温度参数 (0.0–1.0) */
    private final double temperature;

    /** 自定义 system prompt（为 null 时使用默认值） */
    private final String systemPrompt;

    /** RAG 检索请求（为 null 表示纯对话/无 RAG） */
    private final SearchRequest searchRequest;

    /** 是否启用 RAG */
    private final boolean hasRagContext;

    /** system prompt 的 token 预算上限 */
    private final int tokenBudget;

    /** 用户自定义 API Key（为 null 时使用系统全局 Key） */
    private final String customApiKey;

    /** 预检索的 RAG 文档（由 RagController.checkContextRelevance 提供，避免 QuestionAnswerAdvisor 重复搜索） */
    private final List<Document> preRetrievedDocs;

    /** 可扩展属性（用户画像、实验分组、traceId 等） */
    private final Map<String, Object> attributes;

    /** 各中间件耗时记录（key=中间件名, value=ms） */
    private final Map<String, Long> timings;

    private MiddlewareContext(Builder builder) {
        this.userId = builder.userId;
        this.conversationId = builder.conversationId;
        this.query = builder.query;
        this.model = builder.model != null ? builder.model : "Qwen/Qwen3.5-27B";
        this.temperature = Math.max(0.0, Math.min(builder.temperature, 1.0));
        this.systemPrompt = builder.systemPrompt;
        this.searchRequest = builder.searchRequest;
        this.hasRagContext = builder.searchRequest != null || 
                (builder.preRetrievedDocs != null && !builder.preRetrievedDocs.isEmpty());
        this.tokenBudget = builder.tokenBudget > 0 ? builder.tokenBudget : 4096;
        this.customApiKey = builder.customApiKey;
        this.preRetrievedDocs = builder.preRetrievedDocs;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
        this.timings = new HashMap<>();
    }

    // ========== 耗时记录 ==========

    public long recordTiming(String middlewareName) {
        long now = System.currentTimeMillis();
        long elapsed = now - getTotalStart();
        timings.put(middlewareName, elapsed);
        return elapsed;
    }

    private long totalStart = System.currentTimeMillis();

    public long getTotalStart() {
        return totalStart;
    }

    // ========== Getters ==========

    public Long getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getQuery() {
        return query;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public boolean hasRagContext() {
        return hasRagContext;
    }

    public int getTokenBudget() {
        return tokenBudget;
    }

    public String getCustomApiKey() {
        return customApiKey;
    }

    public List<Document> getPreRetrievedDocs() {
        return preRetrievedDocs;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Long> getTimings() {
        return Collections.unmodifiableMap(timings);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    // ========== Builder ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String conversationId;
        private String query;
        private String model = "Qwen/Qwen3.5-27B";
        private double temperature = 0.7;
        private String systemPrompt;
        private SearchRequest searchRequest;
        private int tokenBudget = 4096;
        private String customApiKey;
        private List<Document> preRetrievedDocs;
        private final Map<String, Object> attributes = new HashMap<>();

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder searchRequest(SearchRequest searchRequest) {
            this.searchRequest = searchRequest;
            return this;
        }

        public Builder tokenBudget(int tokenBudget) {
            this.tokenBudget = tokenBudget;
            return this;
        }

        public Builder customApiKey(String customApiKey) {
            this.customApiKey = customApiKey;
            return this;
        }

        public Builder preRetrievedDocs(List<Document> docs) {
            this.preRetrievedDocs = docs;
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public MiddlewareContext build() {
            return new MiddlewareContext(this);
        }
    }
}
