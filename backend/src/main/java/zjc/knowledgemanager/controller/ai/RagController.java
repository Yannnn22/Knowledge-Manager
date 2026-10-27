package zjc.knowledgemanager.controller.ai;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zjc.knowledgemanager.middleware.MiddlewareContext;
import zjc.knowledgemanager.middleware.PromptPipeline;
import zjc.knowledgemanager.service.BaseService;
import zjc.knowledgemanager.service.CompressionService;
import zjc.knowledgemanager.service.ConversationService;
import zjc.knowledgemanager.service.CacheService;
import zjc.knowledgemanager.service.LlmMetricsService;
import zjc.knowledgemanager.service.MemoryExtractionService;
import zjc.knowledgemanager.service.ModelFallBackService;
import zjc.knowledgemanager.dto.AssistantStatsDTO;
import zjc.knowledgemanager.entity.UserProfile;
import zjc.knowledgemanager.mapper.ConversationMapper;
import zjc.knowledgemanager.mapper.UserProfileMapper;
import zjc.knowledgemanager.service.UserProfileService;
import zjc.knowledgemanager.service.retrieval.HybridRetriever;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/ai")
@Tag(name = "RAG智能问答", description = "基于向量检索的智能问答相关接口")
public class RagController {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final String GROUNDING_INSTRUCTION =
            "优先依据下方检索到的文档片段作答，可结合用户画像和历史记忆补充上下文；"
                    + "不要使用任何外部常识或推断；只输出与文档一致的结论，不要扩展说明；"
                    + "若文档未包含答案，可基于用户画像和历史记忆回答；"
                    + "若文档和记忆都未包含答案，仅回复：'无法从提供的文档中找到答案'。";

    private static final int REQUEST_TIMEOUT_SECONDS = 60;

    private static final Logger logger = LoggerFactory.getLogger(RagController.class);

    private final PromptPipeline promptPipeline;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final BaseService baseService;
    private final ConversationService conversationService;
    private final MemoryExtractionService memoryExtractionService;
    private final UserProfileService userProfileService;
    private final CompressionService compressionService;
    private final HybridRetriever hybridRetriever;
    private final java.util.concurrent.Executor memoryExecutor;
    private final ModelFallBackService modelFallBackService;
    private final LlmMetricsService llmMetricsService;
    private final CacheService cacheService;
    private final UserProfileMapper userProfileMapper;
    private final ConversationMapper conversationMapper;

    @Value("${app.ai.default-model:Qwen/Qwen3.5-27B}")
    private String defaultModel;

    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2);

    public RagController(PromptPipeline promptPipeline,
                         @Qualifier("vectorStore") ObjectProvider<VectorStore> vectorStoreProvider,
                         StringRedisTemplate stringRedisTemplate,
                         BaseService baseService,
                         ConversationService conversationService,
                         ObjectProvider<MemoryExtractionService> memoryExtractionService,
                         UserProfileService userProfileService,
                         ObjectProvider<CompressionService> compressionService,
                         HybridRetriever hybridRetriever,
                         @Qualifier("memoryExecutor") java.util.concurrent.Executor memoryExecutor,
                         ModelFallBackService modelFallBackService,
                         LlmMetricsService llmMetricsService,
                         CacheService cacheService,
                         UserProfileMapper userProfileMapper,
                         ConversationMapper conversationMapper) {
        this.promptPipeline = promptPipeline;
        this.vectorStoreProvider = vectorStoreProvider;
        this.stringRedisTemplate = stringRedisTemplate;
        this.baseService = baseService;
        this.conversationService = conversationService;
        this.memoryExtractionService = memoryExtractionService.getIfAvailable();
        this.userProfileService = userProfileService;
        this.compressionService = compressionService.getIfAvailable();
        this.hybridRetriever = hybridRetriever;
        this.memoryExecutor = memoryExecutor;
        this.modelFallBackService = modelFallBackService;
        this.llmMetricsService = llmMetricsService;
        this.cacheService = cacheService;
        this.userProfileMapper = userProfileMapper;
        this.conversationMapper = conversationMapper;
    }

    @GetMapping(value = "/chat-gated", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "智能问答聊天（流式）",
            description = "通过 PromptPipeline 统一中间件链进行 RAG 问答，支持阈值控制、文件过滤和对话记忆"
    )
    @ApiResponses(@ApiResponse(responseCode = "200",
            description = "SSE 流式响应",
            content = @Content(mediaType = "text/plain")))
    public Flux<String> chatWithThreshold(
            @Parameter(description = "用户输入的问题", required = true, example = "什么是Spring AI?")
            @RequestParam("prompt") String userInput,
            @Parameter(description = "检索返回的文档数量", example = "5")
            @RequestParam(value = "topK", defaultValue = "8") int topK,
            @Parameter(description = "相似度阈值，低于此值不使用RAG", example = "0.3")
            @RequestParam(value = "threshold", defaultValue = "0.1") double threshold,
            @Parameter(description = "文件ID，用于过滤检索范围")
            @RequestParam(value = "fileId", required = false) String fileId,
            @Parameter(description = "AI模型名称", example = "Qwen/Qwen3.5-27B")
            @RequestHeader(value = "model", required = false) String headerModel,
            @RequestParam(value = "model", required = false) String paramModel,
            @Parameter(description = "温度参数", example = "0.8")
            @RequestHeader(value = "temperature", required = false) Double headerTemperature,
            @RequestParam(value = "temperature", required = false) Double paramTemperature,
            @Parameter(description = "对话ID")
            @RequestHeader(value = "chatId", required = false) String headerChatId,
            @RequestParam(value = "chatId", required = false) String paramChatId,
            @Parameter(description = "用户ID（JWT注入）", hidden = true)
            @RequestAttribute(value = "userId", required = false) Long userId,
            HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");

        RequestContext ctx = buildRequestContext(userInput, topK, threshold, fileId,
                headerModel, paramModel, headerTemperature, paramTemperature,
                headerChatId, paramChatId, userId);

        persistUserMessage(ctx.normalizedChatId, userId, userInput, ctx.normalizedModel);

        MiddlewareContext middlewareCtx = buildMiddlewareContext(ctx, userInput, userId);

        return Flux.<String>create(sink -> {
            ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(
                    () -> sink.next("\n"), 0, 5000, TimeUnit.MILLISECONDS);

            CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                String answer;
                boolean success = true;
                try {
                    answer = promptPipeline.call(middlewareCtx);
                } catch (Exception e) {
                    logger.error("LLM调用失败: {}", e.getMessage());
                    modelFallBackService.recordFailure(ctx.normalizedModel);
                    llmMetricsService.recordMainChat(ctx.normalizedModel,
                            System.currentTimeMillis() - startTime, false, e.getClass().getSimpleName());
                    answer = "请求失败：" + e.getMessage();
                    success = false;
                }

                if (success) {
                    modelFallBackService.recordSuccess(ctx.normalizedModel);
                    llmMetricsService.recordMainChat(ctx.normalizedModel,
                            System.currentTimeMillis() - startTime, true, null);
                }

                if (userId != null) {
                    final String finalAnswer = answer;
                    CompletableFuture.runAsync(() ->
                            handlePostProcessing(ctx.normalizedChatId, userId, userInput, finalAnswer, ctx.normalizedModel),
                            memoryExecutor);
                }

                return answer;
            }, memoryExecutor)
            .thenAccept(answer -> {
                heartbeat.cancel(true);
                answer.chars().forEach(c -> sink.next(String.valueOf((char) c)));
                sink.complete();
            })
            .exceptionally(e -> {
                heartbeat.cancel(true);
                logger.error("流式响应失败: {}", e.getMessage());
                sink.error(e);
                return null;
            });
        }).delayElements(Duration.ofMillis(30));
    }

    @GetMapping(value = "/chat-once", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "一次性回答（非流式）",
            description = "与 /ai/chat-gated 相同的逻辑，返回一次性文本，便于 Swagger UI 测试"
    )
    @ApiResponses(@ApiResponse(responseCode = "200",
            description = "纯文本回答",
            content = @Content(mediaType = "text/plain")))
    public ResponseEntity<String> chatOnce(
            @Parameter(description = "用户输入的问题", required = true)
            @RequestParam("prompt") String userInput,
            @Parameter(description = "检索返回的文档数量", example = "5")
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @Parameter(description = "相似度阈值，低于此值不使用RAG", example = "0.5")
            @RequestParam(value = "threshold", defaultValue = "0.5") double threshold,
            @Parameter(description = "文件ID")
            @RequestParam(value = "fileId", required = false) String fileId,
            @Parameter(description = "AI模型名称")
            @RequestHeader(value = "model", required = false) String headerModel,
            @RequestParam(value = "model", required = false) String paramModel,
            @Parameter(description = "温度参数")
            @RequestHeader(value = "temperature", required = false) Double headerTemperature,
            @RequestParam(value = "temperature", required = false) Double paramTemperature,
            @Parameter(description = "对话ID")
            @RequestHeader(value = "chatId", required = false) String headerChatId,
            @RequestParam(value = "chatId", required = false) String paramChatId,
            @Parameter(description = "用户ID（JWT注入）", hidden = true)
            @RequestAttribute(value = "userId", required = false) Long userId) {

        long startTime = System.currentTimeMillis();

        RequestContext ctx = buildRequestContext(userInput, topK, threshold, fileId,
                headerModel, paramModel, headerTemperature, paramTemperature,
                headerChatId, paramChatId, userId);

        persistUserMessage(ctx.normalizedChatId, userId, userInput, ctx.normalizedModel);

        MiddlewareContext middlewareCtx = buildMiddlewareContext(ctx, userInput, userId);

        String answer;
        try {
            answer = CompletableFuture.supplyAsync(() -> promptPipeline.call(middlewareCtx))
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("RAG request timeout after {}s for prompt: {}", REQUEST_TIMEOUT_SECONDS, userInput);
            answer = "请求超时，请稍后重试";
        } catch (InterruptedException | ExecutionException e) {
            logger.error("RAG request failed", e);
            answer = "请求失败：" + e.getMessage();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("chatOnce completed in {}ms, rag={}, docs={}, model={}",
                totalTime, ctx.hasUsefulContext, ctx.retrievedDocs.size(), ctx.normalizedModel);

        if (userId != null) {
            final Long finalUserId = userId;
            final String finalChatId = ctx.normalizedChatId;
            final String finalModel = ctx.normalizedModel;
            final String finalUserInput = userInput;
            final String finalAnswer = answer;
            CompletableFuture.runAsync(() ->
                    handlePostProcessing(finalChatId, finalUserId, finalUserInput, finalAnswer, finalModel),
                    memoryExecutor);
        }

        return ResponseEntity.ok(answer);
    }

    @PostMapping(value = "/chat-once", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "一次性回答（非流式，POST方式）",
            description = "与 /ai/chat-once GET 相同的逻辑，使用POST支持中文查询"
    )
    @ApiResponses(@ApiResponse(responseCode = "200",
            description = "纯文本回答",
            content = @Content(mediaType = "text/plain")))
    public ResponseEntity<String> chatOncePost(
            @Parameter(description = "用户输入的问题", required = true)
            @RequestBody ChatRequest request,
            @Parameter(description = "AI模型名称")
            @RequestHeader(value = "model", required = false) String headerModel,
            @Parameter(description = "温度参数")
            @RequestHeader(value = "temperature", required = false) Double headerTemperature,
            @Parameter(description = "对话ID")
            @RequestHeader(value = "chatId", required = false) String headerChatId,
            @Parameter(description = "用户ID（JWT注入）", hidden = true)
            @RequestAttribute(value = "userId", required = false) Long userId) {

        return chatOnce(
                request.prompt(),
                request.topK() != null ? request.topK() : 5,
                request.threshold() != null ? request.threshold() : 0.5,
                request.fileId(),
                headerModel,
                null,
                headerTemperature,
                null,
                headerChatId,
                null,
                userId
        );
    }

    private record ChatRequest(
            String prompt,
            Integer topK,
            Double threshold,
            String fileId
    ) {}

    // ================================================================
    //  向量查询测试
    // ================================================================

    @GetMapping(value = "/select", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "向量数据查询测试", description = "测试向量存储功能")
    @ApiResponses(@ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Document.class))))
    public List<Document> search() {
        VectorStore vs = vectorStoreProvider.getIfAvailable();
        if (vs == null) {
            return List.of();
        }
        return vs.similaritySearch(
                SearchRequest.builder().query("SpringAIAlibaba")
                        .topK("SpringAIAlibaba".length()).build());
    }

    private record RequestContext(
            String normalizedChatId,
            int effectiveTopK,
            double effectiveThreshold,
            String normalizedModel,
            String userApiKey,
            double validatedTemperature,
            Set<String> boundFileIds,
            List<Document> retrievedDocs,
            SearchRequest searchRequest,
            boolean hasUsefulContext
    ) {}

    private RequestContext buildRequestContext(String userInput, int topK, double threshold, String fileId,
                                               String headerModel, String paramModel,
                                               Double headerTemperature, Double paramTemperature,
                                               String headerChatId, String paramChatId, Long userId) {
        String normalizedChatId = normalizeChatId(resolveParam(headerChatId, paramChatId));
        int effectiveTopK = Math.min(Math.max(topK, 3), 10);
        double effectiveThreshold = Math.max(0.3, Math.min(threshold, 0.9));
        String normalizedModel = resolveModel(resolveParam(headerModel, paramModel), userId);
        String userApiKey = resolveUserApiKey(userId);
        String normalizedFileId = extractUuid(fileId);
        double validatedTemperature = baseService.validateAndGetTemperature(
                resolveParam(headerTemperature, paramTemperature));
        Set<String> boundFileIds = resolveBoundFileIds(normalizedChatId, normalizedFileId);

        boolean needsRag = !boundFileIds.isEmpty();
        List<Document> retrievedDocs = List.of();
        SearchRequest searchRequest = null;

        if (needsRag) {
            logger.info("使用混合检索: query='{}', fileIds={}, topK={}", userInput, boundFileIds.size(), effectiveTopK);
            List<Document> cachedDocs = cacheService.getCachedRetrievalResult(normalizedChatId, userInput);
            if (cachedDocs != null) {
                retrievedDocs = cachedDocs;
            } else {
                retrievedDocs = hybridRetriever.search(userInput, effectiveTopK, boundFileIds);
                cacheService.cacheRetrievalResult(normalizedChatId, userInput, retrievedDocs);
            }
            searchRequest = buildSearchRequest(userInput, effectiveTopK, effectiveThreshold, boundFileIds);
        }

        return new RequestContext(
                normalizedChatId, effectiveTopK, effectiveThreshold, normalizedModel, userApiKey,
                validatedTemperature, boundFileIds, retrievedDocs, searchRequest, !retrievedDocs.isEmpty()
        );
    }

    private MiddlewareContext buildMiddlewareContext(RequestContext ctx, String userInput, Long userId) {
        return MiddlewareContext.builder()
                .userId(userId)
                .conversationId(ctx.normalizedChatId())
                .query(userInput)
                .model(ctx.normalizedModel())
                .temperature(ctx.validatedTemperature())
                .systemPrompt(ctx.hasUsefulContext() ? GROUNDING_INSTRUCTION : null)
                .searchRequest(ctx.hasUsefulContext() ? ctx.searchRequest() : null)
                .preRetrievedDocs(ctx.hasUsefulContext() ? ctx.retrievedDocs() : null)
                .customApiKey(ctx.userApiKey())
                .build();
    }

    private void handlePostProcessing(String chatId, Long userId, String userInput, String assistantContent, String modelName) {
        try {
            persistAssistantMessage(chatId, userId, assistantContent, modelName);
            CompletableFuture.runAsync(() -> extractMemories(chatId, userId, userInput, assistantContent), memoryExecutor);
            CompletableFuture.runAsync(() -> updateProfile(userId, userInput, assistantContent), memoryExecutor);
            compressConversation(chatId, userId);
        } catch (Exception e) {
            logger.error("Async post-processing failed", e);
        }
    }

    private String normalizeChatId(String chatId) {
        return chatId == null || chatId.isBlank() ? UUID.randomUUID().toString() : chatId;
    }

    private String extractUuid(String input) {
        if (input == null || input.isBlank()) return null;
        var m = UUID_PATTERN.matcher(input);
        return m.find() ? m.group() : null;
    }

    private Set<String> resolveBoundFileIds(String chatId, String fileId) {
        Set<String> bound = new HashSet<>();
        if (chatId != null) {
            try {
                Set<String> existing = stringRedisTemplate.opsForSet().members("chat_files:" + chatId);
                if (existing != null) bound.addAll(existing);
            } catch (Exception ignored) {
            }
        }
        if (fileId != null) {
            bound.add(fileId);
            try {
                stringRedisTemplate.opsForSet().add("chat_files:" + chatId, fileId);
            } catch (Exception ignored) {
            }
        }
        return bound;
    }

    private SearchRequest buildSearchRequest(String query, int topK, double threshold, Set<String> fileIds) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query).topK(topK).similarityThreshold(threshold);

        if (!fileIds.isEmpty()) {
            FilterExpressionBuilder f = new FilterExpressionBuilder();
            Filter.Expression expr = fileIds.size() == 1
                    ? f.eq("fileId", fileIds.iterator().next()).build()
                    : f.in("fileId", fileIds.stream().toList()).build();
            builder.filterExpression(expr);
        }
        return builder.build();
    }

    // ================================================================
    //  私有方法: 持久化
    // ================================================================

    private void persistUserMessage(String chatId, Long userId,
                                    String content, String modelName) {
        if (userId == null) return;
        try {
            conversationService.createConversation(userId, chatId);
            conversationService.saveMessage(chatId, userId, "user", content, modelName);
            stringRedisTemplate.opsForSet().add("all_chat_ids", chatId);
        } catch (Exception ignored) {
            // 持久化失败不中断聊天
        }
    }

    private void persistAssistantMessage(String chatId, Long userId,
                                         String content, String modelName) {
        if (userId == null) return;
        try {
            conversationService.saveMessage(chatId, userId, "assistant", content, modelName);
        } catch (Exception ignored) {
        }
    }

    private void extractMemories(String chatId, Long userId,
                                 String userQuery, String assistantContent) {
        if (memoryExtractionService == null) return;
        try {
            memoryExtractionService.extractAndStore(chatId, userId, userQuery, assistantContent);
        } catch (Exception ignored) {
        }
    }

    private void updateProfile(Long userId, String userQuery, String assistantContent) {
        try {
            userProfileService.extractAndUpdateProfile(userId, userQuery, assistantContent);
        } catch (Exception ignored) {
        }
    }

    private void compressConversation(String chatId, Long userId) {
        if (compressionService == null) return;
        try {
            compressionService.compressIfNeeded(chatId, userId);
        } catch (Exception ignored) {
        }
    }

    // ================================================================
    //  私有方法: 用户模型配置
    // ================================================================

    /**
     * 解析使用的模型名称。优先级：HTTP 请求头 > 用户个人配置 > 全局默认值。
     */
    private String resolveModel(String headerModel, Long userId) {
        String resolvedModel;
        
        if (headerModel != null && !headerModel.isBlank() && !"default".equalsIgnoreCase(headerModel)) {
            resolvedModel = headerModel;
        } else if (userId != null) {
            try {
                UserProfile profile = userProfileMapper.findByUserId(userId);
                if (profile != null && profile.getPreferredModel() != null
                        && !profile.getPreferredModel().isBlank()) {
                    resolvedModel = profile.getPreferredModel();
                } else {
                    resolvedModel = defaultModel;
                }
            } catch (Exception ignored) {
                resolvedModel = defaultModel;
            }
        } else {
            resolvedModel = defaultModel;
        }
        
        return modelFallBackService.getModel(resolvedModel);
    }

    /**
     * 读取用户自定义的 SiliconFlow API Key。未配置时返回 null（使用系统全局 Key）。
     */
    private String resolveUserApiKey(Long userId) {
        if (userId == null) return null;
        try {
            UserProfile profile = userProfileMapper.findByUserId(userId);
            if (profile != null && profile.getCustomApiKey() != null
                    && !profile.getCustomApiKey().isBlank()) {
                return profile.getCustomApiKey();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ================================================================
    //  私有方法: 参数合并
    // ================================================================

    /** 合并 Header 和 Query Param 传递的 String 参数，Header 优先 */
    private String resolveParam(String headerValue, String paramValue) {
        if (headerValue != null && !headerValue.isBlank()) return headerValue;
        if (paramValue != null && !paramValue.isBlank()) return paramValue;
        return null;
    }

    /** 合并 Header 和 Query Param 传递的 Double 参数，Header 优先 */
    private Double resolveParam(Double headerValue, Double paramValue) {
        return headerValue != null ? headerValue : paramValue;
    }

    @GetMapping("/stats")
    @Operation(summary = "智能秘书统计数据", description = "获取当前用户的智能秘书使用统计数据")
    public ResponseEntity<AssistantStatsDTO> getAssistantStats(
            @RequestAttribute(value = "userId", required = false) Long userId) {
        
        AssistantStatsDTO stats = new AssistantStatsDTO();
        
        if (userId != null) {
            stats.setTodayChatCount(conversationMapper.countTodayByUserId(userId));
            stats.setTodayMessageCount(conversationMapper.countTodayMessagesByUserId(userId));
            stats.setMonthChatCount(conversationMapper.countMonthByUserId(userId));
            stats.setTotalChatCount(conversationMapper.countByUserId(userId));
        } else {
            stats.setTodayChatCount(conversationMapper.countTodayAll());
            stats.setTotalChatCount(conversationMapper.countAll());
            stats.setMonthChatCount(0);
            stats.setTodayMessageCount(0);
        }
        
        stats.setAccuracyRate(92);
        stats.setSatisfactionRate(94);
        stats.setTaskCompleted(18);
        stats.setTaskInProgress(3);
        
        return ResponseEntity.ok(stats);
    }
}
