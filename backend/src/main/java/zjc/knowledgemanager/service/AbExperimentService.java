package zjc.knowledgemanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.config.ai.MemoryProperties;

import java.util.concurrent.TimeUnit;

/**
 * A/B 实验分组服务 —— Redis 持久化用户分组。
 *
 * <h3>分组策略</h3>
 * <ol>
 *   <li>首次请求：基于 userId 哈希 + 配置比例分配 A/B 组</li>
 *   <li>Redis 持久化：{@code experiment_group:{userId} → "A" / "B"}，TTL 30 天</li>
 *   <li>后续请求：直接读 Redis，保证分组稳定（sticky）</li>
 * </ol>
 *
 * <h3>实验维度（当前支持）</h3>
 * <ul>
 *   <li>长期记忆检索 topK：A 组 5 条 vs B 组 8 条</li>
 *   <li>相似度阈值：A 组 0.7 vs B 组 0.6（B 组召回更多）</li>
 * </ul>
 *
 * <h3>扩展方向</h3>
 * 通过 {@code MiddlewareContext.attribute("experiment_group")} 透传，
 * 任何 Advisor 都可以按分组调整策略（如不同 system prompt、不同压缩阈值）。
 */
@Service
public class AbExperimentService {

    private static final Logger log = LoggerFactory.getLogger(AbExperimentService.class);
    private static final String KEY_PREFIX = "experiment_group:";
    private static final long TTL_DAYS = 30;

    private final StringRedisTemplate redis;
    private final MemoryProperties memoryProperties;

    public AbExperimentService(StringRedisTemplate redis,
                                MemoryProperties memoryProperties) {
        this.redis = redis;
        this.memoryProperties = memoryProperties;
    }

    /**
     * 获取用户实验分组（Redis 持久化，sticky）。
     *
     * @param userId 用户 ID
     * @return "A" 或 "B"；实验未开启时返回默认组
     */
    public String getGroup(Long userId) {
        MemoryProperties.Experiment config = memoryProperties.getExperiment();
        if (!config.isEnabled() || userId == null) {
            return config.getDefaultGroup();
        }

        String key = KEY_PREFIX + userId;
        try {
            String existing = redis.opsForValue().get(key);
            if (existing != null) {
                return existing;
            }

            // 首次分配：基于 userId 哈希
            String group = assignGroup(userId, config.getGroupBRatio());
            redis.opsForValue().set(key, group, TTL_DAYS, TimeUnit.DAYS);
            log.info("用户 {} 首次分配实验分组: {}", userId, group);
            return group;

        } catch (Exception e) {
            log.warn("实验分组读取失败, 使用默认组: userId={}, error={}", userId, e.getMessage());
            return config.getDefaultGroup();
        }
    }

    /**
     * 获取当前分组应使用的检索 topK。
     */
    public int getTopK(String group) {
        MemoryProperties.Experiment config = memoryProperties.getExperiment();
        if ("B".equals(group)) {
            return config.getGroupBTopK();
        }
        return config.getGroupATopK();
    }

    /**
     * 获取当前分组应使用的相似度阈值。
     */
    public double getThreshold(String group) {
        MemoryProperties.Experiment config = memoryProperties.getExperiment();
        if ("B".equals(group)) {
            return config.getGroupBThreshold();
        }
        return memoryProperties.getLongTerm().getRetrievalThreshold();
    }

    /**
     * 手动设置用户分组（用于定向投放或调试）。
     */
    public void setGroup(Long userId, String group) {
        if (userId == null) return;
        String key = KEY_PREFIX + userId;
        redis.opsForValue().set(key, group, TTL_DAYS, TimeUnit.DAYS);
        log.info("手动设置用户 {} 实验分组: {}", userId, group);
    }

    // ========== 内部方法 ==========

    /**
     * 基于 userId 哈希 + 比例分配分组。
     * 同一 userId 永远得到同一结果（确定性分配）。
     */
    private String assignGroup(Long userId, double groupBRatio) {
        // 使用 userId 的 hashCode 做确定性分配
        int hash = Math.abs(userId.hashCode());
        double normalized = (hash % 10000) / 10000.0;
        return normalized < groupBRatio ? "B" : "A";
    }
}
