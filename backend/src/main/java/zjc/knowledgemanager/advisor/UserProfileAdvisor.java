package zjc.knowledgemanager.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;
import zjc.knowledgemanager.config.ai.MemoryProperties;
import zjc.knowledgemanager.service.UserProfileService;

import java.util.Map;

/**
 * 用户画像注入 Advisor —— 中间件链最前面（order=-200）。
 *
 * <p>每次 LLM 调用前:
 * <ol>
 *   <li>从 ThreadLocal 读取 userId（由 PromptPipeline 注入）</li>
 *   <li>读 Redis Hash {@code user_profile:{userId}}</li>
 *   <li>格式化画像文本注入 system prompt 最前面</li>
 * </ol>
 *
 * <h3>注入示例</h3>
 * <pre>
 *   [用户画像]
 *   - 回复风格偏好: 简洁
 *   - 技术水平: 高级
 *   - 关注主题: Spring AI, Milvus, Redis
 *   - 语言偏好: zh
 *
 *   [原始 system prompt]
 *   你将作为一名知识库管理员...
 * </pre>
 *
 * <h3>设计考量</h3>
 * <ul>
 *   <li>order=-200: 在所有 Advisor 之前执行，画像信息对后续中间件可见</li>
 *   <li>无画像数据时直接返回原始 request，不注入任何内容</li>
 *   <li>读取失败时优雅降级，不中断对话</li>
 * </ul>
 */
@Component
public class UserProfileAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(UserProfileAdvisor.class);

    private final UserProfileService userProfileService;
    private final MemoryProperties memoryProperties;

    public UserProfileAdvisor(UserProfileService userProfileService,
                              MemoryProperties memoryProperties) {
        this.userProfileService = userProfileService;
        this.memoryProperties = memoryProperties;
    }

    @Override
    public String getName() {
        return "user_profile";
    }

    @Override
    public int getOrder() {
        return -200;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        long start = System.nanoTime();
        MemoryProperties.UserProfile config = memoryProperties.getUserProfile();
        if (!config.isEnabled()) {
            return request;
        }

        Long userId = LongTermMemoryRetrievalAdvisor.getCurrentUserId();
        if (userId == null) {
            return request;
        }

        try {
            Map<String, String> profile = userProfileService.getProfile(userId);
            if (profile.isEmpty()) {
                return request;
            }

            String profileText = userProfileService.formatForPrompt(profile);
            String augmented = profileText + "\n" + request.prompt().getSystemMessage().getText();

            log.debug("用户画像注入: userId={}, fields={}", userId, profile.keySet());

            return request.mutate()
                    .prompt(request.prompt().augmentSystemMessage(profileText))
                    .build();

        } catch (Exception e) {
            log.warn("用户画像注入失败, 回退: userId={}, error={}", userId, e.getMessage());
            return request;
        } finally {
            log.debug("[user_profile] took {}ms", (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }
}
