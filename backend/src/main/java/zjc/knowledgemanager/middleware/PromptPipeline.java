package zjc.knowledgemanager.middleware;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import zjc.knowledgemanager.advisor.ConversationCompressionAdvisor;
import zjc.knowledgemanager.advisor.LongTermMemoryRetrievalAdvisor;
import zjc.knowledgemanager.advisor.TokenBudgetAdvisor;
import zjc.knowledgemanager.advisor.UserProfileAdvisor;
import zjc.knowledgemanager.service.AbExperimentService;
import zjc.knowledgemanager.service.intent.IntentRecognitionService;
import zjc.knowledgemanager.service.tool.DocumentTool;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PromptPipeline {

    private static final Logger log = LoggerFactory.getLogger(PromptPipeline.class);

    private static final String DEFAULT_SYSTEM =
            "你是一名贴心的智能秘书，语气亲切友好，使用自然的口语化表达，适当添加语气词（如：哦、呢、呀、哈、嗯）。\n\n" +
            "思考流程：\n" +
            "1. Thought（思考）：分析用户请求的意图\n" +
            "2. Action（行动）：根据意图决定是否调用工具\n" +
            "3. Observation（观察）：根据工具返回结果，用友好自然的语言总结给用户\n\n" +
            "意图分类与处理规则：\n" +
            "- 普通问答：用户询问知识、信息、建议等（如'什么是Java'、'天气怎么样'），直接回答，不需要调用工具\n" +
            "- RAG检索：用户询问已上传文档中的内容，系统会自动提供相关文档，直接基于文档内容回答\n" +
            "- 文档搜索：用户想要搜索或查找文档（包含关键词：搜索文档、查找文档、帮我找个文档）\n" +
            "- 文档查看：用户想要查看某个文档的内容（包含关键词：查看文档、文档内容）\n" +
            "- 文档列表：用户想要列出所有文档（包含关键词：文档列表、所有文档）\n" +
            "- 文档创建：用户想要创建新文档（包含关键词：创建文档、新建文档、生成文档）- 仅管理员可用\n" +
            "- 文档编辑：用户想要编辑现有文档（包含关键词：编辑文档、修改文档）- 仅管理员可用\n" +
            "- 文档删除：用户想要删除文档（包含关键词：删除文档）- 仅管理员可用\n" +
            "- 知识库搜索：用户想要在知识库中搜索（包含关键词：知识库搜索、在知识库中搜索）\n" +
            "- 知识库列表：用户想要列出所有知识库（包含关键词：知识库列表）\n" +
            "- 数据查询：用户想要通过自然语言查询数据库（包含关键词：查询、统计、有多少、数量）- 仅管理员可用\n\n" +
            "可用工具：\n" +
            "- search_documents: 搜索文档列表，需要参数 query(搜索关键词)、top_k(返回数量)\n" +
            "- search_document_content: 搜索文档内容，需要参数 query(搜索关键词)、top_k(返回数量)\n" +
            "- get_document: 获取文档详情，需要参数 document_id(文档ID)\n" +
            "- list_documents: 列出所有文档，需要参数 page(页码)、size(每页数量)\n" +
            "- search_in_knowledge_base: 在指定知识库中搜索文档，需要参数 knowledge_base_id(知识库ID)、query(搜索关键词)、top_k(返回数量)\n" +
            "- manage_knowledge_base: 知识库管理，需要参数 operation(操作类型：create/list/delete)、name(名称)、description(描述)、knowledge_base_id(知识库ID)\n" +
            "- query_data: 数据查询（仅管理员），需要参数 query(查询内容)\n" +
            "- create_document: 创建文档（仅管理员），需要参数 title(标题)、content(内容)、description(描述)\n" +
            "- edit_document: 编辑文档（仅管理员），需要参数 document_id(文档ID)、new_content(新内容)、new_description(新描述)\n" +
            "- delete_document: 删除文档（仅管理员），需要参数 document_id(文档ID)\n" +
            "- create_pdf_document: 创建PDF文档（仅管理员），需要参数 title(标题)、content(内容)、description(描述)\n" +
            "- create_word_document: 创建Word文档（仅管理员），需要参数 title(标题)、content(内容)、description(描述)\n" +
            "- summarize_document: 生成文档摘要，需要参数 document_id(文档ID)、summary_length(摘要长度)\n" +
            "\n" +
            "重要规则：\n" +
            "- 普通用户只能执行只读操作：搜索文档、查看文档、列出文档、知识库搜索、知识库列表\n" +
            "- 管理员可以执行所有操作：创建、编辑、删除文档，创建、删除知识库，数据查询\n" +
            "- 如果用户的问题是普通问答或知识检索，请直接回答，不要调用工具\n" +
            "- 如果用户明确请求文档操作（创建、编辑、删除、列出、搜索、查看），才调用相应工具\n" +
            "- 如果用户请求在知识库中查找文档，请先列出知识库获取知识库ID，然后再进行搜索\n" +
            "- 如果用户请求数据查询，请确认用户是否为管理员\n" +
            "- 不要为了调用工具而强行将普通问答转换为文档操作\n" +
            "- 不要自动创建文档来记录对话内容\n" +
            "- 禁止执行任何删除、修改数据的高危操作，除非用户是管理员且明确请求\n" +
            "\n" +
            "输出格式要求：\n" +
            "- 禁止在回答前添加数字编号（如1.、2.、3.）\n" +
            "- 禁止使用任何列表格式\n" +
            "- 直接输出回答内容，不要任何前缀\n" +
            "- 只有当回答确实包含多个独立要点时，才用阿拉伯数字分点（1、2、3...）\n" +
            "- 去掉所有Markdown格式符号\n" +
            "- 回复简洁明了\n" +
            "- 回复要有亲和力，使用自然的口语化表达，适当添加语气词（如：哦、呢、呀、哈、嗯）\n" +
            "- 当搜索文档时，先用友好的语气告知用户找到的文档数量（如：找到了2个相关文档哦！），然后每个文档单独占一行展示信息\n" +
            "- 文档信息格式：文档名(文档ID) 所属知识库：xxx 匹配度：xx%\n" +
            "- 每个文档必须单独占一行，文档之间必须有换行符\n" +
            "- 不要直接输出工具返回的原始格式，要用自然语言重新组织内容，但必须保留文档ID以便用户可以点击查看";


    private final ChatClient chatClient;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final LongTermMemoryRetrievalAdvisor longTermMemoryAdvisor;
    private final UserProfileAdvisor userProfileAdvisor;
    private final ConversationCompressionAdvisor compressionAdvisor;
    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;
    private final TokenBudgetAdvisor tokenBudgetAdvisor;
    private final AbExperimentService abExperimentService;
    private final DocumentTool documentTool;
    private final IntentRecognitionService intentRecognitionService;

    private final Timer pipelineTimer;
    private final MeterRegistry meterRegistry;

    private final ObjectProvider<RestClient.Builder> restClientBuilderProvider;
    private final ObjectProvider<WebClient.Builder> webClientBuilderProvider;

    @Value("${spring.ai.openai.base-url}")
    private String openaiBaseUrl;

    public PromptPipeline(ChatClient.Builder builder,
                          @Qualifier("vectorStore") ObjectProvider<VectorStore> vectorStoreProvider,
                          ObjectProvider<LongTermMemoryRetrievalAdvisor> longTermMemoryAdvisor,
                          UserProfileAdvisor userProfileAdvisor,
                          ObjectProvider<ConversationCompressionAdvisor> compressionAdvisor,
                          MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                          TokenBudgetAdvisor tokenBudgetAdvisor,
                          AbExperimentService abExperimentService,
                          DocumentTool documentTool,
                          IntentRecognitionService intentRecognitionService,
                          MeterRegistry meterRegistry,
                          ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                          ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        this.chatClient = builder.defaultSystem(DEFAULT_SYSTEM).build();
        this.vectorStoreProvider = vectorStoreProvider;
        this.longTermMemoryAdvisor = longTermMemoryAdvisor.getIfAvailable();
        this.userProfileAdvisor = userProfileAdvisor;
        this.compressionAdvisor = compressionAdvisor.getIfAvailable();
        this.messageChatMemoryAdvisor = messageChatMemoryAdvisor;
        this.tokenBudgetAdvisor = tokenBudgetAdvisor;
        this.abExperimentService = abExperimentService;
        this.documentTool = documentTool;
        this.intentRecognitionService = intentRecognitionService;
        this.meterRegistry = meterRegistry;
        this.restClientBuilderProvider = restClientBuilderProvider;
        this.webClientBuilderProvider = webClientBuilderProvider;
        this.pipelineTimer = Timer.builder("pipeline.request.duration")
                .description("PromptPipeline 请求耗时")
                .register(meterRegistry);

        log.info("Pipeline init: restClientBuilder available={}, webClientBuilder available={}, documentTool={}",
                restClientBuilderProvider.getIfAvailable() != null,
                webClientBuilderProvider.getIfAvailable() != null,
                documentTool != null);
    }

    public Flux<String> stream(MiddlewareContext ctx) {
        long start = System.nanoTime();

        LongTermMemoryRetrievalAdvisor.setUserId(ctx.getUserId());
        PromptContextHolder.setConversationId(ctx.getConversationId());

        String experimentGroup = abExperimentService.getGroup(ctx.getUserId());
        var spec = buildSpec(ctx, experimentGroup);

        meterRegistry.counter("pipeline.request.count",
                "rag", String.valueOf(ctx.hasRagContext()),
                "group", experimentGroup).increment();

        AtomicBoolean firstToken = new AtomicBoolean(true);
        return spec.stream()
                .content()
                .doOnNext(token -> {
                    if (firstToken.compareAndSet(true, false)) {
                        log.debug("First token latency: {}ms", 
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                    }
                })
                .doFinally(signal -> {
                    pipelineTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                    LongTermMemoryRetrievalAdvisor.clearUserId();
                    PromptContextHolder.clear();
                });
    }

    private boolean requiresToolCall(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return false;
        }
        
        if (intentRecognitionService != null) {
            var intentResult = intentRecognitionService.recognize(userQuery);
            log.debug("意图识别结果: {} - {}", intentResult.getIntent().getCode(), userQuery);
            return intentResult.isRequiresTool();
        }
        
        String[] TOOL_INTENT_KEYWORDS = {
            "创建文档", "新建文档", "写文档", "保存文档",
            "编辑文档", "修改文档", "更新文档",
            "删除文档", "移除文档",
            "列出文档", "文档列表", "所有文档",
            "搜索文档", "查找文档", "检索文档", "找文档", "找个文档", "找一下文档",
            "查看文档", "获取文档", "文档内容",
            "生成文档", "生成word", "生成pdf", "word文档", "pdf文档",
            "document", "知识库", "查询", "统计",
            "帮我找", "有没有", "关于.*文档"
        };
        
        String lowerQuery = userQuery.toLowerCase();
        for (String keyword : TOOL_INTENT_KEYWORDS) {
            if (userQuery.contains(keyword) || lowerQuery.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private ChatClient.ChatClientRequestSpec buildSpec(MiddlewareContext ctx, String experimentGroup) {
        var spec = buildBaseSpec(ctx);

        spec = spec.advisors(userProfileAdvisor);

        if (longTermMemoryAdvisor != null && ctx.getUserId() != null) {
            spec = spec.advisors(longTermMemoryAdvisor)
                    .advisors(a -> a.param("experiment_group", experimentGroup))
                    .advisors(a -> a.param("userId", ctx.getUserId()));
        }

        if (compressionAdvisor != null && ctx.getUserId() != null) {
            spec = spec.advisors(compressionAdvisor);
        }

        if (ctx.hasRagContext() && (ctx.getPreRetrievedDocs() == null || ctx.getPreRetrievedDocs().isEmpty())) {
            VectorStore vs = vectorStoreProvider.getIfAvailable();
            if (vs != null && ctx.getSearchRequest() != null) {
                spec = spec.advisors(QuestionAnswerAdvisor.builder(vs)
                        .searchRequest(ctx.getSearchRequest()).build());
            }
        }

        spec = spec.advisors(messageChatMemoryAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ctx.getConversationId()))
                .advisors(tokenBudgetAdvisor)
                .advisors(new SimpleLoggerAdvisor(100));

        if (requiresToolCall(ctx.getQuery())) {
            spec = spec.tools(documentTool);
            log.debug("注册文档工具: query='{}'", ctx.getQuery());
        }

        return spec;
    }

    public String call(MiddlewareContext ctx) {
        long start = System.nanoTime();

        LongTermMemoryRetrievalAdvisor.setUserId(ctx.getUserId());
        PromptContextHolder.setConversationId(ctx.getConversationId());
        documentTool.setCurrentUserId(ctx.getUserId());

        String experimentGroup = abExperimentService.getGroup(ctx.getUserId());
        var spec = buildSpec(ctx, experimentGroup);

        meterRegistry.counter("pipeline.request.count",
                "rag", String.valueOf(ctx.hasRagContext()),
                "group", experimentGroup).increment();

        try {
            return spec.call().content();
        } catch (Exception e) {
            log.error("AI调用失败", e);
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getSimpleName();
                if (e.getCause() != null) {
                    errorMsg += ": " + e.getCause().getMessage();
                }
            }
            if (errorMsg.contains("application/octet-stream")) {
                return "AI服务暂时不可用，请稍后重试";
            }
            if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
                return "API密钥无效，请检查配置";
            }
            if (errorMsg.contains("429") || errorMsg.contains("rate limit")) {
                return "请求过于频繁，请稍后重试";
            }
            if (errorMsg.contains("SocketTimeout") || errorMsg.contains("timeout")) {
                return "请求超时，请稍后重试";
            }
            if (errorMsg.contains("ArrayIndexOutOfBounds")) {
                return "工具调用参数解析失败，请尝试重新提问";
            }
            return "请求失败：" + errorMsg;
        } finally {
            pipelineTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            LongTermMemoryRetrievalAdvisor.clearUserId();
            PromptContextHolder.clear();
        }
    }

    private ChatClient.ChatClientRequestSpec buildBaseSpec(MiddlewareContext ctx) {
        var options = OpenAiChatOptions.builder()
                .model(ctx.getModel())
                .temperature(ctx.getTemperature())
                .build();

        String sysPrompt = ctx.getSystemPrompt() != null
                ? ctx.getSystemPrompt() : DEFAULT_SYSTEM;

        if (ctx.getPreRetrievedDocs() != null && !ctx.getPreRetrievedDocs().isEmpty()) {
            sysPrompt += formatRetrievedDocs(ctx.getPreRetrievedDocs());
        }

        ChatClient client = resolveChatClient(ctx);

        return client.prompt()
                .options(options)
                .system(sysPrompt)
                .user(ctx.getQuery());
    }

    private String formatRetrievedDocs(List<Document> docs) {
        StringBuilder sb = new StringBuilder("\n\n[文档上下文]\n");
        for (int i = 0; i < docs.size(); i++) {
            sb.append(i + 1).append(". ").append(docs.get(i).getText()).append("\n");
        }
        return sb.toString();
    }

    private ChatClient resolveChatClient(MiddlewareContext ctx) {
        String customKey = ctx.getCustomApiKey();
        if (customKey != null && !customKey.isBlank()) {
            try {
                OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                        .apiKey(customKey)
                        .baseUrl(openaiBaseUrl);

                RestClient.Builder restBuilder = restClientBuilderProvider.getIfAvailable();
                if (restBuilder != null) apiBuilder.restClientBuilder(restBuilder);

                WebClient.Builder webBuilder = webClientBuilderProvider.getIfAvailable();
                if (webBuilder != null) apiBuilder.webClientBuilder(webBuilder.clone());

                OpenAiChatModel customModel = OpenAiChatModel.builder()
                        .openAiApi(apiBuilder.build())
                        .build();
                return ChatClient.builder(customModel)
                        .defaultSystem(DEFAULT_SYSTEM)
                        .build();
            } catch (Exception e) {
                log.warn("使用用户自定义 API Key 失败，回退到全局配置: {}", e.getMessage());
            }
        }
        return chatClient;
    }
}