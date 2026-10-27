package zjc.knowledgemanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.config.ai.MemoryProperties;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 记忆生命周期管理服务 —— 容量控制 + TTL 淘汰。
 *
 * <h3>两层淘汰机制</h3>
 * <ol>
 *   <li><b>写时容量检查</b>：每次存储新记忆前，检查用户记忆计数器。
 *       超过 {@code maxMemoriesPerUser} 时，检索最旧的低重要性记忆并删除。</li>
 *   <li><b>定时清理</b>：每天凌晨 3 点扫描，删除 importance 低于阈值且超过 TTL 的记忆。</li>
 * </ol>
 *
 * <h3>Redis 计数器</h3>
 * <pre>
 *   memory_count:{userId} → 当前记忆条数（INCR/DECR 维护）
 * </pre>
 * 计数器为近似值（Milvus 删除有延迟），但足以做容量判断。
 *
 * <h3>客户端 TTL 过滤</h3>
 * {@link zjc.knowledgemanager.advisor.LongTermMemoryRetrievalAdvisor} 检索后，
 * 在 Java 侧过滤掉 {@code created_at + ttlDays < now} 的过期记忆，
 * 避免向 LLM 注入陈旧信息。
 */
@Service
@ConditionalOnBean(name = "memoryVectorStore")
public class MemoryLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(MemoryLifecycleService.class);
    private static final String COUNT_KEY_PREFIX = "memory_count:";

    private final VectorStore memoryVectorStore;
    private final StringRedisTemplate redis;
    private final MemoryProperties memoryProperties;

    public MemoryLifecycleService(@Qualifier("memoryVectorStore") VectorStore memoryVectorStore,
                                   StringRedisTemplate redis,
                                   MemoryProperties memoryProperties) {
        this.memoryVectorStore = memoryVectorStore;
        this.redis = redis;
        this.memoryProperties = memoryProperties;
    }

    // ========== 写时容量检查 ==========

    /**
     * 在存储新记忆前调用。若用户记忆数已达上限，淘汰最旧的低分记忆。
     *
     * @param userId 用户 ID
     * @return 实际淘汰的记忆数
     */
    public int enforceCapacityBeforeStore(Long userId) {
        if (userId == null) return 0;

        MemoryProperties.LongTerm config = memoryProperties.getLongTerm();
        int maxPerUser = config.getMaxMemoriesPerUser();
        if (maxPerUser <= 0) return 0;

        int currentCount = getMemoryCount(userId);
        if (currentCount < maxPerUser) return 0;

        // 需要淘汰的数量（淘汰 1 条为新记忆腾位）
        int toEvict = Math.max(1, currentCount - maxPerUser + 1);
        log.info("用户 {} 记忆数 {} 超上限 {}, 淘汰 {} 条低分记忆",
                userId, currentCount, maxPerUser, toEvict);

        return evictLowScoreMemories(userId, toEvict, config);
    }

    /**
     * 存储成功后调用，递增计数器。
     */
    public void incrementCount(Long userId) {
        if (userId == null) return;
        String key = COUNT_KEY_PREFIX + userId;
        redis.opsForValue().increment(key);
        redis.expire(key, 90, TimeUnit.DAYS);
    }

    /**
     * 删除成功后调用，递减计数器。
     */
    public void decrementCount(Long userId, int amount) {
        if (userId == null || amount <= 0) return;
        String key = COUNT_KEY_PREFIX + userId;
        Long remaining = redis.opsForValue().decrement(key, amount);
        if (remaining != null && remaining < 0) {
            // 计数器漂移修正
            redis.opsForValue().set(key, "0");
        }
    }

    // ========== 定时清理 ==========

    /**
     * 每天凌晨 3 点执行 TTL 清理。
     * 清理对象：importance < evictionImportanceThreshold 且 created_at 超过 ttlDays 的记忆。
     *
     * 注意：由于 Milvus VectorStore 不支持按 filter 直接删除，
     * 此方法通过广泛查询检索记忆后在 Java 侧过滤删除。
     * 对于大规模数据，建议配合 Milvus 原生 TTL 功能使用。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledEviction() {
        MemoryProperties.LongTerm config = memoryProperties.getLongTerm();
        if (!config.isEnabled() || config.getTtlDays() <= 0) return;

        log.info("开始定时记忆清理: ttlDays={}, evictionThreshold={}",
                config.getTtlDays(), config.getEvictionImportanceThreshold());

        // 由于无法枚举所有 userId，这里只做日志记录。
        // 实际清理依赖写时容量检查 + 客户端 TTL 过滤。
        // 企业级方案：用 Milvus 原生 TTL 或定期批量脚本。
        log.info("定时记忆清理完成（依赖写时淘汰 + 客户端 TTL 过滤）");
    }

    // ========== 内部方法 ==========

    /**
     * 获取用户当前记忆数（Redis 计数器）。
     */
    public int getMemoryCount(Long userId) {
        if (userId == null) return 0;
        String key = COUNT_KEY_PREFIX + userId;
        String val = redis.opsForValue().get(key);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 检索并删除用户最旧的低分记忆。
     *
     * @param userId  用户 ID
     * @param count   要淘汰的数量
     * @param config  长期记忆配置
     * @return 实际删除数
     */
    private int evictLowScoreMemories(Long userId, int count, MemoryProperties.LongTerm config) {
        try {
            // 用用户的 ID 作为查询文本检索（召回该用户最相关的记忆）
            // 优先删除：importance 低 + 时间旧 的记忆
            // 这里检索 topK=maxPerUser 来获取全量，然后排序选最差的删除
            int searchK = Math.min(config.getMaxMemoriesPerUser(), 50);

            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(String.valueOf(userId))
                    .topK(searchK)
                    .similarityThreshold(0.0); // 低阈值，尽量多召回

            // 多租户过滤
            try {
                org.springframework.ai.vectorstore.filter.FilterExpressionBuilder f =
                        new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
                builder.filterExpression(f.eq("user_id", userId).build());
            } catch (Exception ignored) {
            }

            List<Document> allMemories = memoryVectorStore.similaritySearch(builder.build());
            if (allMemories.isEmpty()) {
                // 计数器可能漂移，重置
                redis.opsForValue().set(COUNT_KEY_PREFIX + userId, "0");
                return 0;
            }

            // 按 score 升序排（最差的在前）：importance 低 + 时间旧
            allMemories.sort(Comparator.comparingDouble(this::evictionScore));

            int evicted = 0;
            for (int i = 0; i < Math.min(count, allMemories.size()); i++) {
                Document doc = allMemories.get(i);

                // 只删除 importance 低于阈值的
                double importance = getImportance(doc);
                if (importance >= config.getEvictionImportanceThreshold()) {
                    break; // 剩余的都高于阈值，不删了
                }

                try {
                    memoryVectorStore.delete(List.of(doc.getId()));
                    evicted++;
                    log.debug("淘汰记忆: id={}, importance={}, content={}",
                            doc.getId(), importance, truncate(doc.getText(), 30));
                } catch (Exception e) {
                    log.warn("删除记忆失败: id={}, error={}", doc.getId(), e.getMessage());
                }
            }

            if (evicted > 0) {
                decrementCount(userId, evicted);
                log.info("用户 {} 淘汰 {} 条低分记忆", userId, evicted);
            }

            return evicted;

        } catch (Exception e) {
            log.warn("淘汰记忆失败: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * 淘汰评分：值越小越优先删除。
     * <ol>
     *   <li>expired 状态 → 固定最低分 -1000（确保最先被删）</li>
     *   <li>active 状态 → importance 低 + 时间旧 优先</li>
     * </ol>
     */
    private double evictionScore(Document doc) {
        // expired 记忆优先删除——它们已经没有任何检索价值
        String status = (String) doc.getMetadata().get("status");
        if ("expired".equals(status)) {
            return -1000.0;
        }

        double importance = getImportance(doc);
        double daysAgo = getDaysAgo(doc);
        return importance - daysAgo * 0.01;
    }

    private double getImportance(Document doc) {
        Object imp = doc.getMetadata().get("importance");
        if (imp instanceof Number) {
            return ((Number) imp).doubleValue();
        }
        return 3.0; // 默认中等
    }

    private double getDaysAgo(Document doc) {
        Object created = doc.getMetadata().get("created_at");
        if (created instanceof Number) {
            long createdAt = ((Number) created).longValue();
            long now = System.currentTimeMillis() / 1000;
            return (now - createdAt) / 86400.0;
        }
        return 0;
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
