package zjc.knowledgemanager.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiModelChatConfig {

    @Value("${spring.ai.openai.chat.options.model:Qwen/Qwen3.5-27B}")
    private String defaultModel;

    @Value("${app.ai.memory.long-term.extraction-model:Qwen/Qwen3.5-9B}")
    private String extractionModel;

    @Value("${app.ai.memory.user-profile.extraction-model:Qwen/Qwen3.5-9B}")
    private String profileModel;

    @Value("${app.ai.memory.compression.summary-model:Qwen/Qwen3.5-4B}")
    private String summaryModel;

    @Value("${app.ai.default-temperature:0.7}")
    private double defaultTemperature;

    @Primary
    @Bean("mainChatClient")
    public ChatClient mainChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(defaultModel)
                        .temperature(defaultTemperature)
                        .build())
                .build();
    }

    @Bean("extractionChatClient")
    public ChatClient extractionChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(extractionModel)
                        .temperature(0.1)
                        .build())
                .build();
    }

    @Bean("profileChatClient")
    public ChatClient profileChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(profileModel)
                        .temperature(0.1)
                        .build())
                .build();
    }

    @Bean("summaryChatClient")
    public ChatClient summaryChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(summaryModel)
                        .temperature(0.3)
                        .build())
                .build();
    }
}
