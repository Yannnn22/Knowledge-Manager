package zjc.knowledgemanager.config.ai;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import zjc.knowledgemanager.advisor.LongTermMemoryRetrievalAdvisor;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AppConfiguration {

    @Value("${spring.ai.memory.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.ai.memory.redis.port:6379}")
    private int redisPort;

    @Value("${spring.ai.memory.redis.password:}")
    private String redisPassword;

    @Value("${spring.ai.memory.redis.timeout:2000}")
    private int redisTimeout;

    private final MemoryProperties memoryProperties;

    public AppConfiguration(MemoryProperties memoryProperties) {
        this.memoryProperties = memoryProperties;
    }

    // === 异步线程池（记忆提取用） ===

    @Bean("memoryExecutor")
    public Executor memoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("memory-extract-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // === ChatMemory（短期滑动窗口） ===

    @Bean
    public ChatMemory redisChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(memoryProperties.getShortTerm().getWindowSize())
                .chatMemoryRepository(RedisChatMemoryRepository.builder()
                        .host(redisHost)
                        .port(redisPort)
                        .password(redisPassword.isEmpty() ? null : redisPassword)
                        .timeout(redisTimeout)
                        .build())
                .build();
    }

    // === Advisors ===

    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor(100);
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(
            ChatMemory redisChatMemory
    ) {
        return MessageChatMemoryAdvisor.builder(redisChatMemory).build();
    }

    // 说明：openAiChatModel 由 spring-ai-starter-model-openai 自动配置提供，此处不再手动声明

    // 说明：EmbeddingModel 由 spring-ai-starter-model-openai 自动配置提供，此处不再手动声明

    // 说明：LongTermMemoryRetrievalAdvisor 由 @Component 自动扫描，此处不重复声明

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    // === OpenAI (SiliconFlow) HTTP 超时配置 ===

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(10));
            factory.setReadTimeout(Duration.ofSeconds(60));
            builder.requestFactory(factory);
        };
    }

    @Bean
    public WebClientCustomizer webClientCustomizer() {
        return builder -> {
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
            connector.setReadTimeout(Duration.ofSeconds(60));
            builder.clientConnector(connector);
        };
    }
}
