package zjc.knowledgemanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.config.ai.MemoryProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户画像服务 —— Redis Hash 直查（O(1), &lt;1ms）+ LLM 异步更新。
 *
 * <h3>存储策略</h3>
 * <pre>
 *   Redis Hash: user_profile:{userId}
 *     response_style  → "简洁" / "详细" / "技术导向"
 *     expertise_level → "初级" / "中级" / "高级"
 *     known_topics    → "Spring AI,Milvus,Redis"
 *     language_pref   → "zh" / "en"
 *     last_updated    → epoch秒
 * </pre>
 *
 * <h3>更新策略</h3>
 * 每轮对话后 LLM 异步分析本轮对话，输出 profile_updates，
 * 增量合并到 Redis Hash（不覆盖未变更字段）。
 *
 * <h3>Advisor 注入</h3>
 * {@link zjc.knowledgemanager.advisor.UserProfileAdvisor} 在每次 LLM 调用前
 * 读取画像并注入 system prompt 最前面（order=-200）。
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final String KEY_PREFIX = "user_profile:";
    private static final long TTL_HOURS = 720; // 30 天

    private final StringRedisTemplate redis;
    private final ChatClient profileClient;
    private final MemoryProperties memoryProperties;
    private final ObjectMapper objectMapper;
    private final LlmMetricsService llmMetricsService;

    private static final String EXTRACTION_PROMPT = """
            严格遵守：仅输出JSON对象，不要markdown代码块标记，不要任何前缀或解释，不要注释。

            分析本轮对话，提取用户画像的变更。无变更返回{}。

            用户消息: %s
            助手回复: %s

            可更新的画像字段（仅输出有变化的字段）:
            - response_style: 用户偏好的回复风格（简洁/详细/技术导向/故事化）
            - expertise_level: 用户展现的专业水平（初级/中级/高级）
            - known_topics: 用户关注或了解的技术主题（逗号分隔，追加而非覆盖）
            - language_pref: 语言偏好（zh/en）

            输出格式示例: {"response_style":"简洁","known_topics":"Spring AI,Redis"}
            无变更输出: {}""";

    public UserProfileService(StringRedisTemplate stringRedisTemplate,
                              @Qualifier("profileChatClient") ChatClient profileClient,
                              MemoryProperties memoryProperties,
                              LlmMetricsService llmMetricsService) {
        this.redis = stringRedisTemplate;
        this.profileClient = profileClient;
        this.memoryProperties = memoryProperties;
        this.objectMapper = new ObjectMapper();
        this.llmMetricsService = llmMetricsService;
    }

    // ========== 读取 ==========

    /**
     * 读取用户画像（Redis Hash → Map）。
     * @return 空 Map 表示无画像数据
     */
    public Map<String, String> getProfile(Long userId) {
        if (userId == null) return Map.of();
        try {
            String key = KEY_PREFIX + userId;
            Map<Object, Object> raw = redis.opsForHash().entries(key);
            if (raw.isEmpty()) return Map.of();

            Map<String, String> profile = new HashMap<>();
            raw.forEach((k, v) -> profile.put(String.valueOf(k), String.valueOf(v)));
            return profile;
        } catch (Exception e) {
            log.warn("读取用户画像失败: userId={}, error={}", userId, e.getMessage());
            return Map.of();
        }
    }

    /**
     * 将画像格式化为可注入 system prompt 的文本。
     */
    public String formatForPrompt(Map<String, String> profile) {
        if (profile.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("[用户画像]\n");

        String style = profile.get("response_style");
        if (style != null && !style.isBlank()) {
            sb.append("- 回复风格偏好: ").append(style).append("\n");
        }
        String level = profile.get("expertise_level");
        if (level != null && !level.isBlank()) {
            sb.append("- 技术水平: ").append(level).append("\n");
        }
        String topics = profile.get("known_topics");
        if (topics != null && !topics.isBlank()) {
            sb.append("- 关注主题: ").append(topics).append("\n");
        }
        String lang = profile.get("language_pref");
        if (lang != null && !lang.isBlank()) {
            sb.append("- 语言偏好: ").append(lang).append("\n");
        }
        return sb.toString();
    }

    // ========== 异步更新 ==========

    /**
     * 从一轮对话中提取画像变更并合并到 Redis Hash。
     * 在 RagController 流式响应结束后异步调用。
     */
    public void extractAndUpdateProfile(Long userId, String userQuery, String assistantResponse) {
        MemoryProperties.UserProfile config = memoryProperties.getUserProfile();
        if (!config.isEnabled() || userId == null) return;

        try {
            Map<String, String> updates = extract(userQuery, assistantResponse, config);
            if (updates.isEmpty()) return;

            mergeProfile(userId, updates);
            log.debug("用户画像更新: userId={}, fields={}", userId, updates.keySet());

        } catch (Exception e) {
            log.warn("画像提取失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ========== 内部方法 ==========

    /**
     * 调用 LLM 提取画像变更。
     */
    private Map<String, String> extract(String userQuery, String assistantResponse,
                                        MemoryProperties.UserProfile config) {
        long startTime = System.currentTimeMillis();
        try {
            String prompt = String.format(EXTRACTION_PROMPT, userQuery, assistantResponse);
            String raw = profileClient.prompt()
                    .system("你是一个用户画像分析助手，只输出JSON。")
                    .user(prompt)
                    .call()
                    .content();

            if (raw == null || raw.isBlank() || raw.trim().equals("{}")) {
                llmMetricsService.recordProfileUpdate(
                        config.getExtractionModel(),
                        System.currentTimeMillis() - startTime,
                        true,
                        null);
                return Map.of();
            }

            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf('\n') + 1);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.lastIndexOf("```")).trim();
                }
            }

            Map<String, String> result = objectMapper.readValue(json, 
                    new TypeReference<Map<String, String>>() {});
            
            llmMetricsService.recordProfileUpdate(
                    config.getExtractionModel(),
                    System.currentTimeMillis() - startTime,
                    true,
                    null);
            
            return result;

        } catch (Exception e) {
            log.debug("画像提取无变更或无有效输出: {}", e.getMessage());
            llmMetricsService.recordProfileUpdate(
                    config.getExtractionModel(),
                    System.currentTimeMillis() - startTime,
                    false,
                    e.getClass().getSimpleName());
            return Map.of();
        }
    }

    /**
     * 增量合并画像到 Redis Hash + 刷新 TTL。
     */
    private void mergeProfile(Long userId, Map<String, String> updates) {
        String key = KEY_PREFIX + userId;
        long now = System.currentTimeMillis() / 1000;

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String field = entry.getKey();
            String newValue = entry.getValue();
            if (newValue == null || newValue.isBlank()) continue;

            // known_topics 特殊处理：追加而非覆盖
            if ("known_topics".equals(field)) {
                String existing = (String) redis.opsForHash().get(key, field);
                if (existing != null && !existing.isBlank()) {
                    // 合并去重
                    StringBuilder merged = new StringBuilder(existing);
                    for (String topic : newValue.split(",")) {
                        String t = topic.trim();
                        if (!t.isBlank() && !existing.contains(t)) {
                            merged.append(",").append(t);
                        }
                    }
                    newValue = merged.toString();
                }
            }

            redis.opsForHash().put(key, field, newValue);
        }

        redis.opsForHash().put(key, "last_updated", String.valueOf(now));
        redis.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }
}
