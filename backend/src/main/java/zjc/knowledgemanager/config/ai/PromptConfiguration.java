package zjc.knowledgemanager.config.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * AI 功能总开关配置。
 *
 * <p>当 {@code app.ai.enabled=false} 时，此配置类 + 所有
 * 受 {@code @ConditionalOnProperty} 保护的 AI 组件将不加载，
 * 系统退化为无 AI 模式（仅提供文件管理、向量存储等基础能力）。
 *
 * <p>ChatClient 不再由此处声明为 Bean，改由
 * {@link zjc.knowledgemanager.middleware.PromptPipeline} 内部创建，
 * 避免 {@code defaultAdvisors} 与中间件链的排列冲突。
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class PromptConfiguration {
    // 仅作为条件开关，实际 Bean 在 AppConfiguration / PromptPipeline 中声明
}
