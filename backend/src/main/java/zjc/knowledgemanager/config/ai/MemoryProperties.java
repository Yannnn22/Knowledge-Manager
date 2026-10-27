package zjc.knowledgemanager.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 记忆体系配置属性
 * 前缀: app.ai.memory
 */
@Component
@ConfigurationProperties(prefix = "app.ai.memory")
public class MemoryProperties {

    private LongTerm longTerm = new LongTerm();
    private ShortTerm shortTerm = new ShortTerm();
    private UserProfile userProfile = new UserProfile();
    private Compression compression = new Compression();
    private TokenBudget tokenBudget = new TokenBudget();
    private Experiment experiment = new Experiment();

    public LongTerm getLongTerm() { return longTerm; }
    public void setLongTerm(LongTerm longTerm) { this.longTerm = longTerm; }

    public ShortTerm getShortTerm() { return shortTerm; }
    public void setShortTerm(ShortTerm shortTerm) { this.shortTerm = shortTerm; }

    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }

    public Compression getCompression() { return compression; }
    public void setCompression(Compression compression) { this.compression = compression; }

    public TokenBudget getTokenBudget() { return tokenBudget; }
    public void setTokenBudget(TokenBudget tokenBudget) { this.tokenBudget = tokenBudget; }

    public Experiment getExperiment() { return experiment; }
    public void setExperiment(Experiment experiment) { this.experiment = experiment; }

    /**
     * 长期记忆配置 — Milvus 语义检索
     */
    public static class LongTerm {
        private boolean enabled = true;
        private String collectionName = "long_term_memory";
        private int retrievalTopK = 5;
        private double retrievalThreshold = 0.7;
        private double mergeThreshold = 0.85;
        private int extractionMaxPerTurn = 5;
        private String extractionModel = "Qwen/Qwen3.5-9B";
        /** 记忆 TTL（天），超过后检索时客户端过滤 */
        private int ttlDays = 90;
        /** 每用户最大记忆条数，超过触发淘汰 */
        private int maxMemoriesPerUser = 500;
        /** 淘汰时 importance 低于此值的优先删除 */
        private double evictionImportanceThreshold = 2.0;
        /**
         * 冲突检测模式: "similarity"=仅相似度触发 / "llm"=LLM 判断是否矛盾
         * similarity 模式: 语义相似度超 mergeThreshold 即视为冲突，旧记忆 expired
         * llm 模式: 语义相似度超 mergeThreshold 后，再用 LLM 判断是否真实矛盾
         */
        private String conflictDetectionMode = "similarity";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }

        public int getRetrievalTopK() { return retrievalTopK; }
        public void setRetrievalTopK(int retrievalTopK) { this.retrievalTopK = retrievalTopK; }

        public double getRetrievalThreshold() { return retrievalThreshold; }
        public void setRetrievalThreshold(double retrievalThreshold) { this.retrievalThreshold = retrievalThreshold; }

        public double getMergeThreshold() { return mergeThreshold; }
        public void setMergeThreshold(double mergeThreshold) { this.mergeThreshold = mergeThreshold; }

        public int getExtractionMaxPerTurn() { return extractionMaxPerTurn; }
        public void setExtractionMaxPerTurn(int extractionMaxPerTurn) { this.extractionMaxPerTurn = extractionMaxPerTurn; }

        public String getExtractionModel() { return extractionModel; }
        public void setExtractionModel(String extractionModel) { this.extractionModel = extractionModel; }

        public int getTtlDays() { return ttlDays; }
        public void setTtlDays(int ttlDays) { this.ttlDays = ttlDays; }

        public int getMaxMemoriesPerUser() { return maxMemoriesPerUser; }
        public void setMaxMemoriesPerUser(int maxMemoriesPerUser) { this.maxMemoriesPerUser = maxMemoriesPerUser; }

        public double getEvictionImportanceThreshold() { return evictionImportanceThreshold; }
        public void setEvictionImportanceThreshold(double evictionImportanceThreshold) { this.evictionImportanceThreshold = evictionImportanceThreshold; }

        public String getConflictDetectionMode() { return conflictDetectionMode; }
        public void setConflictDetectionMode(String conflictDetectionMode) { this.conflictDetectionMode = conflictDetectionMode; }
    }

    /**
     * 短期记忆配置 — Redis 滑动窗口
     */
    public static class ShortTerm {
        private int windowSize = 30;

        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
    }

    /**
     * 用户画像配置 — Redis Hash 存储 + LLM 异步更新
     */
    public static class UserProfile {
        private boolean enabled = true;
        private String extractionModel = "Qwen/Qwen3.5-9B";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getExtractionModel() { return extractionModel; }
        public void setExtractionModel(String extractionModel) { this.extractionModel = extractionModel; }
    }

    /**
     * 渐进式摘要压缩配置 — 超长对话自动压缩为摘要存入 Milvus
     */
    public static class Compression {
        private boolean enabled = true;
        /** 触发压缩的轮数阈值（每轮 = user + assistant 两条消息） */
        private int triggerRoundCount = 20;
        /** 每条摘要的最大字符数 */
        private int summaryMaxLength = 200;
        private String summaryModel = "Qwen/Qwen3.5-4B";
        private String collectionName = "conversation_summaries";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getTriggerRoundCount() { return triggerRoundCount; }
        public void setTriggerRoundCount(int triggerRoundCount) { this.triggerRoundCount = triggerRoundCount; }

        public int getSummaryMaxLength() { return summaryMaxLength; }
        public void setSummaryMaxLength(int summaryMaxLength) { this.summaryMaxLength = summaryMaxLength; }

        public String getSummaryModel() { return summaryModel; }
        public void setSummaryModel(String summaryModel) { this.summaryModel = summaryModel; }

        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    }

    /**
     * Token 预算控制配置 — 防止上下文溢出
     */
    public static class TokenBudget {
        private boolean enabled = true;
        /** system prompt 最大 token 数（含注入的画像/记忆/摘要/文档） */
        private int maxSystemTokens = 3000;
        /** 用户消息预留 token 数 */
        private int userMessageReserve = 2000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMaxSystemTokens() { return maxSystemTokens; }
        public void setMaxSystemTokens(int maxSystemTokens) { this.maxSystemTokens = maxSystemTokens; }

        public int getUserMessageReserve() { return userMessageReserve; }
        public void setUserMessageReserve(int userMessageReserve) { this.userMessageReserve = userMessageReserve; }
    }

    /**
     * A/B 实验分组配置 — 不同分组使用不同检索策略
     */
    public static class Experiment {
        private boolean enabled = false;
        /** 默认分组（未开启实验时所有用户归入此组） */
        private String defaultGroup = "A";
        /** A 组检索 topK */
        private int groupATopK = 5;
        /** B 组检索 topK（实验组：检索更多记忆） */
        private int groupBTopK = 8;
        /** B 组相似度阈值（实验组：降低阈值召回更多） */
        private double groupBThreshold = 0.6;
        /** B 组实验比例（0.0-1.0，哈希分配） */
        private double groupBRatio = 0.5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getDefaultGroup() { return defaultGroup; }
        public void setDefaultGroup(String defaultGroup) { this.defaultGroup = defaultGroup; }

        public int getGroupATopK() { return groupATopK; }
        public void setGroupATopK(int groupATopK) { this.groupATopK = groupATopK; }

        public int getGroupBTopK() { return groupBTopK; }
        public void setGroupBTopK(int groupBTopK) { this.groupBTopK = groupBTopK; }

        public double getGroupBThreshold() { return groupBThreshold; }
        public void setGroupBThreshold(double groupBThreshold) { this.groupBThreshold = groupBThreshold; }

        public double getGroupBRatio() { return groupBRatio; }
        public void setGroupBRatio(double groupBRatio) { this.groupBRatio = groupBRatio; }
    }
}
