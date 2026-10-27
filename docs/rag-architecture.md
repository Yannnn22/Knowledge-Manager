# RAG 架构设计文档

## 一、概述

本项目基于 Spring AI 框架实现了一套完整的检索增强生成（RAG）系统，涵盖文档分块、向量存储、混合检索、LLM重排、Prompt工程等核心模块。系统采用模块化设计，各组件职责清晰，支持灵活的策略配置和扩展。

---

## 二、文件入库流程

### 2.1 整体数据流

```
文件上传 → 文档解析 → 智能分块 → 元数据注入 → 批量向量化 → 混合索引存储
    ↓           ↓          ↓            ↓            ↓            ↓
  MultipartFile  Tika/PDF  ChunkStrategy  fileId等    Embedding   Milvus+BM25
```

### 2.2 文件上传控制器 (FileIngestController)

**核心代码**：

```java
@RestController
@RequestMapping("/ingest")
public class FileIngestController {
    private static final int EMBEDDING_BATCH_LIMIT = 5;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    private final VectorStore vectorStore;
    private final ChunkStrategyFactory chunkStrategyFactory;
    private final HybridRetriever hybridRetriever;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestPart("file") MultipartFile file) {
        // 文件校验
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("必须上传非空的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body("文件大小超过限制，最大支持10MB");
        }

        try {
            List<Document> docs;
            String filename = file.getOriginalFilename();
            
            // 根据文件类型选择解析器
            if (StringUtils.hasText(filename) && filename.toLowerCase().endsWith(".pdf")) {
                docs = new PagePdfDocumentReader(file.getResource()).get();
            } else {
                docs = new TikaDocumentReader(file.getResource()).get();
            }
            
            // 智能分块
            List<Document> splitDocs = chunkStrategyFactory.smartChunk(docs);
            
            // 分块数量限制
            if (splitDocs.size() > 100) {
                return ResponseEntity.badRequest().body("文件内容过多，分块后超过100个片段");
            }

            // 注入元数据
            String fileId = UUID.randomUUID().toString();
            for (Document doc : splitDocs) {
                doc.getMetadata().put("fileId", fileId);
                doc.getMetadata().put("originalFilename", filename);
            }

            // 批量向量化存储
            for (int i = 0; i < splitDocs.size(); i += EMBEDDING_BATCH_LIMIT) {
                int end = Math.min(i + EMBEDDING_BATCH_LIMIT, splitDocs.size());
                List<Document> batch = splitDocs.subList(i, end);
                hybridRetriever.addDocuments(batch);
                System.gc(); // 释放内存
            }

            return ResponseEntity.ok("successfully inserted " + splitDocs.size() + " text fragments, fileId: " + fileId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("文件上传失败: " + e.getMessage());
        }
    }
}
```

**文件位置**：[FileIngestController.java](../backend/src/main/java/zjc/knowledgemanager/controller/ai/FileIngestController.java)

---

## 三、文档分块策略

### 3.1 分块策略接口

```java
public interface ChunkStrategy {
    String getName();
    List<Document> chunk(List<Document> documents);
}
```

### 3.2 段落分块策略 (ParagraphChunkStrategy)

**策略说明**：基于段落边界进行分块，以空行作为段落分隔符。每个段落最大512 token（约2048字符）。

**核心代码**：

```java
@Component
public class ParagraphChunkStrategy implements ChunkStrategy {
    private static final int MAX_CHUNK_TOKENS = 512;
    private static final int ESTIMATED_CHARS_PER_TOKEN = 4;

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;
            
            String[] paragraphs = content.split("\n\\s*\n");
            
            for (int i = 0; i < paragraphs.length; i++) {
                String paragraph = paragraphs[i].trim();
                if (paragraph.isEmpty()) continue;
                
                List<String> paragraphChunks = splitBySize(paragraph, MAX_CHUNK_TOKENS * ESTIMATED_CHARS_PER_TOKEN);
                
                for (int j = 0; j < paragraphChunks.size(); j++) {
                    Document chunk = new Document(paragraphChunks.get(j));
                    chunk.getMetadata().putAll(doc.getMetadata());
                    chunk.getMetadata().put("chunkType", "paragraph");
                    chunk.getMetadata().put("paragraphIndex", i);
                    chunk.getMetadata().put("chunkIndex", j);
                    chunk.getMetadata().put("totalChunks", paragraphChunks.size());
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }
    
    private List<String> splitBySize(String content, int maxSize) {
        List<String> chunks = new ArrayList<>();
        if (content.length() <= maxSize) {
            chunks.add(content);
            return chunks;
        }
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxSize, content.length());
            if (end < content.length()) {
                int lastSentenceEnd = findLastSentenceEnd(content, start, end);
                if (lastSentenceEnd > start) {
                    end = lastSentenceEnd;
                } else {
                    int lastSpace = content.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace;
                    }
                }
            }
            chunks.add(content.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }
}
```

**文件位置**：[ParagraphChunkStrategy.java](../backend/src/main/java/zjc/knowledgemanager/service/chunk/ParagraphChunkStrategy.java)

### 3.3 Markdown分块策略 (MarkdownChunkStrategy)

**策略说明**：基于Markdown标题结构进行分块，每个标题下的内容作为一个独立块。支持H1-H6标题级别。

**核心代码**：

```java
@Component
public class MarkdownChunkStrategy implements ChunkStrategy {
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final int MAX_CHUNK_TOKENS = 512;

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;
            
            List<String> sections = parseMarkdownSections(content);
            
            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i);
                String title = extractTitle(section);
                
                List<String> sectionChunks = splitBySize(section, MAX_CHUNK_TOKENS * ESTIMATED_CHARS_PER_TOKEN);
                
                for (int j = 0; j < sectionChunks.size(); j++) {
                    Document chunk = new Document(sectionChunks.get(j));
                    chunk.getMetadata().putAll(doc.getMetadata());
                    chunk.getMetadata().put("chunkType", "markdown");
                    chunk.getMetadata().put("sectionTitle", title);
                    chunk.getMetadata().put("sectionIndex", i);
                    chunk.getMetadata().put("chunkIndex", j);
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }
}
```

**文件位置**：[MarkdownChunkStrategy.java](../backend/src/main/java/zjc/knowledgemanager/service/chunk/MarkdownChunkStrategy.java)

### 3.4 大小块分块策略 (SmallLargeChunkStrategy)

**策略说明**：同时生成小粒度块（256 token）和大粒度块（512 token），小块用于精确匹配，大块用于上下文保留。块间重叠64 token。

**核心代码**：

```java
@Component
public class SmallLargeChunkStrategy implements ChunkStrategy {
    private static final int SMALL_CHUNK_TOKENS = 256;
    private static final int LARGE_CHUNK_TOKENS = 512;
    private static final int OVERLAP_TOKENS = 64;

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;
            
            List<String> smallChunks = splitContent(content, SMALL_CHUNK_TOKENS * 4, OVERLAP_TOKENS * 4);
            List<String> largeChunks = splitContent(content, LARGE_CHUNK_TOKENS * 4, OVERLAP_TOKENS * 4);
            
            for (int i = 0; i < smallChunks.size(); i++) {
                Document chunk = new Document(smallChunks.get(i));
                chunk.getMetadata().putAll(doc.getMetadata());
                chunk.getMetadata().put("chunkType", "small");
                chunk.getMetadata().put("chunkIndex", i);
                chunks.add(chunk);
            }
            
            for (int i = 0; i < largeChunks.size(); i++) {
                Document chunk = new Document(largeChunks.get(i));
                chunk.getMetadata().putAll(doc.getMetadata());
                chunk.getMetadata().put("chunkType", "large");
                chunk.getMetadata().put("chunkIndex", i);
                chunks.add(chunk);
            }
        }
        return chunks;
    }
}
```

**文件位置**：[SmallLargeChunkStrategy.java](../backend/src/main/java/zjc/knowledgemanager/service/chunk/SmallLargeChunkStrategy.java)

### 3.5 智能分块工厂 (ChunkStrategyFactory)

**策略说明**：根据文档类型智能选择分块策略。优先使用Markdown策略，否则使用Small-Large策略，最后回退到段落策略。

**核心代码**：

```java
@Component
public class ChunkStrategyFactory {
    @Autowired
    private MarkdownChunkStrategy markdownStrategy;
    
    @Autowired
    private SmallLargeChunkStrategy smallLargeStrategy;
    
    @Autowired
    private ParagraphChunkStrategy paragraphStrategy;

    public List<Document> smartChunk(List<Document> documents) {
        return documents.stream()
                .flatMap(doc -> chunkDocument(doc).stream())
                .collect(Collectors.toList());
    }
    
    private List<Document> chunkDocument(Document document) {
        String content = document.getText();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        
        boolean isMarkdown = content.contains("# ") || content.contains("**") || 
                            content.contains("* ") || content.contains("- ");
        
        if (isMarkdown) {
            List<Document> markdownChunks = markdownStrategy.chunk(List.of(document));
            if (markdownChunks.size() > 1) {
                return markdownChunks;
            }
        }
        
        List<Document> smallLargeChunks = smallLargeStrategy.chunk(List.of(document));
        
        if (smallLargeChunks.isEmpty()) {
            return paragraphStrategy.chunk(List.of(document));
        }
        
        return smallLargeChunks;
    }
}
```

**文件位置**：[ChunkStrategyFactory.java](../backend/src/main/java/zjc/knowledgemanager/service/chunk/ChunkStrategyFactory.java)

---

## 四、检索策略

### 4.1 混合检索器 (HybridRetriever)

**策略说明**：同时执行向量检索和BM25检索，使用RRF（Reciprocal Rank Fusion）算法融合结果。

**核心参数**：
- `RRF_K = 60` - RRF平滑参数
- `VECTOR_WEIGHT = 0.6` - 向量检索权重
- `BM25_WEIGHT = 0.4` - BM25检索权重
- `EXPANSION_FACTOR = 2` - 扩展因子（实际检索数量 = topK × 2）

**核心代码**：

```java
@Component
public class HybridRetriever {
    private static final int RRF_K = 60;
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double BM25_WEIGHT = 0.4;
    private static final int SEARCH_TIMEOUT_SECONDS = 8;

    private final VectorStore vectorStore;
    private final BM25Retriever bm25Retriever;
    private final ExecutorService executorService;
    private final CacheService cacheService;

    public List<Document> search(String query, int topK, Set<String> fileIds) {
        // 优先查本地缓存
        List<Document> cachedResults = getLocalCache(query, fileIds);
        if (cachedResults != null) {
            return cachedResults;
        }
        
        int expandedTopK = topK * EXPANSION_FACTOR;
        
        // 并行执行两种检索
        CompletableFuture<List<BM25Retriever.ScoredDocument>> bm25Future = CompletableFuture.supplyAsync(
                () -> bm25Retriever.search(query, expandedTopK, fileIds), executorService);
        
        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(
                () -> searchVector(query, expandedTopK, fileIds), executorService);
        
        // 等待结果
        CompletableFuture.allOf(bm25Future, vectorFuture)
                .get(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // RRF融合
        List<Document> fusedResults = fuseWithRRF(bm25Results, vectorResults, topK);
        putLocalCache(query, fileIds, fusedResults);
        
        return fusedResults;
    }
    
    private List<Document> fuseWithRRF(List<BM25Retriever.ScoredDocument> bm25Results,
                                        List<Document> vectorResults, int topK) {
        Map<String, Double> scores = new HashMap<>();
        
        // BM25结果打分：RRF = 1 / (K + rank)
        for (int i = 0; i < bm25Results.size(); i++) {
            BM25Retriever.ScoredDocument scoredDoc = bm25Results.get(i);
            if (scoredDoc.document() != null) {
                double rrfScore = 1.0 / (RRF_K + i + 1);
                scores.merge(scoredDoc.document().getId(), rrfScore * BM25_WEIGHT, Double::sum);
            }
        }
        
        // 向量检索结果打分
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            scores.merge(doc.getId(), rrfScore * VECTOR_WEIGHT, Double::sum);
        }
        
        // 按分数排序去重
        Set<String> seenIds = new HashSet<>();
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    Document doc = findDocumentById(e.getKey(), bm25Results, vectorResults);
                    if (doc != null && seenIds.add(doc.getId())) {
                        doc.getMetadata().put("hybridScore", e.getValue());
                        return doc;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
```

**文件位置**：[HybridRetriever.java](../backend/src/main/java/zjc/knowledgemanager/service/retrieval/HybridRetriever.java)

### 4.2 BM25检索器 (BM25Retriever)

**策略说明**：基于BM25算法实现的关键词检索，使用TF-IDF变体计算文档相关性。

**核心代码**：

```java
@Component
public class BM25Retriever {
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\w]+");
    
    private Map<String, Map<String, Integer>> documentTermFrequencies = new ConcurrentHashMap<>();
    private Map<String, Integer> documentLengths = new ConcurrentHashMap<>();
    private Map<String, Integer> inverseDocumentFrequencies = new ConcurrentHashMap<>();
    private Map<String, Document> documentStore = new ConcurrentHashMap<>();
    private int totalDocuments = 0;
    private double averageDocumentLength = 0;

    public record ScoredDocument(Document document, double score, String source) {}

    public void addDocuments(List<Document> documents) {
        for (Document doc : documents) {
            String docId = doc.getId();
            String content = doc.getText();
            
            Map<String, Integer> termFreq = computeTermFrequency(content);
            documentTermFrequencies.put(docId, termFreq);
            documentLengths.put(docId, content.length());
            documentStore.put(docId, doc);
            
            for (String term : termFreq.keySet()) {
                inverseDocumentFrequencies.merge(term, 1, Integer::sum);
            }
            
            totalDocuments++;
        }
        updateAverageDocumentLength();
    }

    public List<ScoredDocument> search(String query, int topK, Set<String> fileIds) {
        if (totalDocuments == 0) return List.of();
        
        Map<String, Double> scores = new HashMap<>();
        Map<String, Integer> queryTermFreq = computeTermFrequency(query);
        
        for (String docId : documentTermFrequencies.keySet()) {
            Document doc = documentStore.get(docId);
            
            // 文件ID过滤
            if (doc != null && fileIds != null && !fileIds.isEmpty()) {
                String docFileId = (String) doc.getMetadata().get("fileId");
                if (docFileId == null || !fileIds.contains(docFileId)) {
                    continue;
                }
            }
            
            double score = calculateBM25Score(docId, queryTermFreq);
            scores.put(docId, score);
        }
        
        return scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new ScoredDocument(documentStore.get(e.getKey()), e.getValue(), "bm25"))
                .toList();
    }
    
    private double calculateBM25Score(String docId, Map<String, Integer> queryTermFreq) {
        double score = 0;
        Map<String, Integer> docTermFreq = documentTermFrequencies.get(docId);
        int docLength = documentLengths.get(docId);
        
        for (Map.Entry<String, Integer> entry : queryTermFreq.entrySet()) {
            String term = entry.getKey();
            if (!docTermFreq.containsKey(term)) continue;
            
            int docFreq = docTermFreq.get(term);
            int docCount = inverseDocumentFrequencies.getOrDefault(term, 0);
            
            double idf = Math.log((totalDocuments - docCount + 0.5) / (docCount + 0.5) + 1);
            double tf = docFreq * (K1 + 1);
            double denominator = docFreq + K1 * (1 - B + B * docLength / averageDocumentLength);
            
            score += idf * tf / denominator;
        }
        return score;
    }
    
    private Map<String, Integer> computeTermFrequency(String text) {
        Map<String, Integer> termFreq = new HashMap<>();
        WORD_PATTERN.matcher(text.toLowerCase()).results()
                .map(m -> m.group())
                .forEach(term -> termFreq.merge(term, 1, Integer::sum));
        return termFreq;
    }
}
```

**文件位置**：[BM25Retriever.java](../backend/src/main/java/zjc/knowledgemanager/service/retrieval/BM25Retriever.java)

### 4.3 向量检索配置

**配置说明**：使用Milvus作为向量数据库，配置如下：

```java
@Configuration
public class MilvusConfig {
    @Value("${spring.ai.vectorstore.milvus.collectionName:vector_store_v6}")
    private String collectionName;
    
    @Value("${spring.ai.vectorstore.milvus.embeddingDimension:2560}")
    private int embeddingDimension;
    
    @Value("${spring.ai.vectorstore.milvus.indexType:IVF_FLAT}")
    private String indexType;
    
    @Value("${spring.ai.vectorstore.milvus.metricType:COSINE}")
    private String metricType;

    @Bean
    public VectorStore vectorStore(MilvusServiceClient milvusServiceClient,
                                   EmbeddingModel embeddingModel) {
        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(collectionName)
                .embeddingDimension(embeddingDimension)
                .indexType(IndexType.valueOf(indexType))
                .metricType(MetricType.valueOf(metricType))
                .initializeSchema(true)
                .build();
    }
}
```

**文件位置**：[MilvusConfig.java](../backend/src/main/java/zjc/knowledgemanager/config/ai/MilvusConfig.java)

---

## 五、LLM重排策略

### 5.1 重排服务 (RerankService)

**策略说明**：利用LLM对检索结果进行语义级重排，提升相关性排序准确性。

**注意**：当前RerankService已实现但尚未集成到检索链路中，可根据业务需求在HybridRetriever之后调用。

**核心代码**：

```java
@Component
public class RerankService {
    private static final String RERANK_PROMPT = """
            你是一个专业的文档重排助手。请根据以下查询和文档片段，对文档片段进行相关性排序。
            
            查询：{query}
            
            文档片段列表：
            {documents}
            
            请按照与查询的相关性从高到低排序，输出文档的编号（从0开始），用逗号分隔。
            只输出排序后的编号列表，不要输出其他内容。
            
            示例输出：2,0,1,3,4
            """;

    private final ChatClient chatClient;

    public List<Document> rerank(String query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }
        
        StringBuilder docsBuilder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            String content = documents.get(i).getText();
            String preview = content.length() > 300 ? content.substring(0, 300) + "..." : content;
            docsBuilder.append(i).append(". ").append(preview).append("\n\n");
        }
        
        String prompt = RERANK_PROMPT
                .replace("{query}", query)
                .replace("{documents}", docsBuilder.toString());
        
        String result = chatClient.prompt().user(prompt).call().content();
        List<Integer> indices = parseRerankResult(result, documents.size());
        
        List<Document> reranked = new ArrayList<>();
        for (Integer index : indices) {
            if (index >= 0 && index < documents.size()) {
                reranked.add(documents.get(index));
            }
        }
        
        return reranked;
    }
}
```

**文件位置**：[RerankService.java](../backend/src/main/java/zjc/knowledgemanager/service/retrieval/RerankService.java)

---

## 六、Prompt工程

### 6.1 中间件管道 (PromptPipeline)

**策略说明**：构建包含多个Advisor的Prompt处理管道，实现多阶段上下文注入。

**中间件顺序**：

| Order | Advisor | 功能 |
|-------|---------|------|
| -200 | UserProfileAdvisor | 用户画像注入 |
| -100 | LongTermMemoryRetrievalAdvisor | 长期记忆检索 |
| -50 | ConversationCompressionAdvisor | 对话摘要压缩 |
| 0 | QuestionAnswerAdvisor | RAG文档检索 |
| 0 | MessageChatMemoryAdvisor | Redis短期记忆 |
| +50 | TokenBudgetAdvisor | Token预算控制 |
| +100 | SimpleLoggerAdvisor | 日志记录 |

**核心代码**：

```java
@Component
public class PromptPipeline {
    private static final String DEFAULT_SYSTEM =
            "你是一名贴心的智能秘书，语气亲切友好，使用自然的口语化表达，适当添加语气词。\n\n" +
            "思考流程：\n" +
            "1. Thought（思考）：分析用户请求的意图\n" +
            "2. Action（行动）：根据意图决定是否调用工具\n" +
            "3. Observation（观察）：根据工具返回结果，用友好自然的语言总结给用户\n\n" +
            "意图分类与处理规则：\n" +
            "- 普通问答：用户询问知识、信息、建议等，直接回答，不需要调用工具\n" +
            "- RAG检索：用户询问已上传文档中的内容，系统会自动提供相关文档\n" +
            "- 文档搜索：用户想要搜索或查找文档\n" +
            "...\n" +
            "输出格式要求：\n" +
            "- 回复要有亲和力，使用自然的口语化表达\n" +
            "- 当搜索文档时，先用友好的语气告知用户找到的文档数量\n" +
            "- 文档信息格式：文档名(文档ID) 所属知识库：xxx 匹配度：xx%\n";

    public String call(MiddlewareContext ctx) {
        var spec = buildSpec(ctx, experimentGroup);
        try {
            return spec.call().content();
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("application/octet-stream")) {
                return "AI服务暂时不可用，请稍后重试";
            }
            if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
                return "API密钥无效，请检查配置";
            }
            if (errorMsg.contains("429") || errorMsg.contains("rate limit")) {
                return "请求过于频繁，请稍后重试";
            }
            return "请求失败：" + errorMsg;
        }
    }
}
```

**文件位置**：[PromptPipeline.java](../backend/src/main/java/zjc/knowledgemanager/middleware/PromptPipeline.java)

### 6.2 Token预算控制 (TokenBudgetAdvisor)

**策略说明**：按优先级截断上下文，确保Prompt不超过模型token限制。

**截断优先级（从先截断到最后截断）**：

1. [文档上下文] — RAG检索结果（最容易冗余）
2. [对话历史摘要] — 压缩摘要（历史信息）
3. [相关历史记忆] — 长期记忆（跨对话）
4. [用户画像] — 画像信息（每轮必须）
5. [原始system prompt] — 永不截断

**核心代码**：

```java
@Component
public class TokenBudgetAdvisor implements BaseAdvisor {
    private static final String[] SECTION_MARKERS = {
            "[文档上下文]",      // RAG 文档检索结果，最先截断
            "[对话历史摘要]",    // 压缩摘要
            "[相关历史记忆]",    // 长期记忆
            "[用户画像]"         // 用户画像，最后截断
    };

    private final MemoryProperties memoryProperties;

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        MemoryProperties.TokenBudget config = memoryProperties.getTokenBudget();
        if (!config.isEnabled()) {
            return request;
        }

        String systemPrompt = request.prompt().getSystemMessage().getText();
        int estimatedTokens = estimateTokens(systemPrompt);
        int budget = config.getMaxSystemTokens();

        if (estimatedTokens <= budget) {
            return request; // 未超预算，无需截断
        }

        // 超预算：按优先级截断
        String trimmed = trimToBudget(systemPrompt, estimatedTokens, budget);
        
        // 用截断后的内容替换 system message
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(new SystemMessage(trimmed));
        for (Message msg : request.prompt().getInstructions()) {
            if (!(msg instanceof SystemMessage)) {
                newMessages.add(msg);
            }
        }
        Prompt newPrompt = new Prompt(newMessages, request.prompt().getOptions());
        return request.mutate().prompt(newPrompt).build();
    }
    
    /**
     * 粗略估算 token 数。中文字符 ~1.5 字/token，ASCII 字符 ~4 字/token。
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cjkChars = 0;
        int asciiChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                cjkChars++;
            } else {
                asciiChars++;
            }
        }
        return (int) Math.ceil(cjkChars / 1.5 + asciiChars / 4.0);
    }
    
    private String trimToBudget(String systemPrompt, int currentTokens, int budget) {
        String result = systemPrompt;
        for (String marker : SECTION_MARKERS) {
            if (estimateTokens(result) <= budget) break;
            
            int markerIndex = result.indexOf(marker);
            if (markerIndex < 0) continue;
            
            int nextMarkerIndex = findNextMarker(result, markerIndex + marker.length());
            int sectionEnd = nextMarkerIndex > 0 ? nextMarkerIndex : result.length();
            
            result = result.substring(0, markerIndex) + result.substring(sectionEnd);
        }
        return result;
    }
}
```

**文件位置**：[TokenBudgetAdvisor.java](../backend/src/main/java/zjc/knowledgemanager/advisor/TokenBudgetAdvisor.java)

---

## 七、意图识别

### 7.1 意图识别服务 (IntentRecognitionService)

**策略说明**：基于关键词和正则表达式进行意图分类，支持多种意图类型。

**意图类型**：

| 意图类型 | 说明 | 关键词示例 |
|---------|------|-----------|
| GENERAL_QA | 普通问答 | "什么是Java" |
| RAG_RETRIEVAL | RAG检索 | 询问文档内容 |
| DOCUMENT_SEARCH | 文档搜索 | "搜索文档"、"找文档" |
| DOCUMENT_VIEW | 文档查看 | "查看文档"、"文档内容" |
| DOCUMENT_CREATE | 文档创建 | "创建文档"、"新建文档" |
| KNOWLEDGE_BASE_SEARCH | 知识库搜索 | "知识库搜索" |
| NL2SQL_QUERY | NL2SQL查询 | "查询"、"统计" |

**核心代码**：

```java
@Component
public class IntentRecognitionService {
    private static final Map<IntentType, List<String>> KEYWORD_MAP = new HashMap<>();
    private static final Map<IntentType, List<Pattern>> PATTERN_MAP = new HashMap<>();

    static {
        KEYWORD_MAP.put(IntentType.DOCUMENT_SEARCH, Arrays.asList(
            "搜索文档", "查找文档", "检索文档", "找文档", "搜索一下文档",
            "帮我找个文档", "帮我搜索文档", "查找一下文档",
            "找个关于", "找一下", "有没有.*文档", "帮我找",
            "关于.*的文档", "知识库里有什么", "知识库里的文档",
            "有什么文档", "什么文档", "文档有哪些"
        ));
        
        PATTERN_MAP.put(IntentType.DOCUMENT_VIEW, Arrays.asList(
            Pattern.compile("查看文档\\s*(\\d+)"),
            Pattern.compile("文档\\s*(\\d+)\\s*的内容"),
            Pattern.compile("文档ID\\s*(\\d+)")
        ));
    }

    public IntentResult recognize(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return new IntentResult(IntentType.UNKNOWN, false, null);
        }
        
        // 正则匹配（优先级更高）
        for (Map.Entry<IntentType, List<Pattern>> entry : PATTERN_MAP.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(userQuery).find()) {
                    return new IntentResult(entry.getKey(), requiresToolCall(entry.getKey()), entry.getKey());
                }
            }
        }
        
        // 关键词匹配
        for (Map.Entry<IntentType, List<String>> entry : KEYWORD_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (userQuery.contains(keyword)) {
                    return new IntentResult(entry.getKey(), requiresToolCall(entry.getKey()), entry.getKey());
                }
            }
        }
        
        return new IntentResult(IntentType.GENERAL_QA, false, null);
    }
}
```

**文件位置**：[IntentRecognitionService.java](../backend/src/main/java/zjc/knowledgemanager/service/intent/IntentRecognitionService.java)

---

## 八、完整RAG流程图

```
用户输入 → 意图识别 → 判断是否需要工具调用
                          ↓
              ┌───────────┴───────────┐
              ↓                       ↓
         需要工具调用              普通问答/RAG检索
              ↓                       ↓
         调用DocumentTool        混合检索(HybridRetriever)
              ↓                       ↓
         返回文档列表          BM25检索 + 向量检索
              ↓                       ↓
         LLM总结回复          RRF融合排序
                                    ↓
                            Prompt构建(多Advisor注入)
                                    ↓
                            LLM生成回答
```

---

## 九、关键配置参数

| 参数 | 配置位置 | 默认值 | 说明 |
|------|---------|--------|------|
| 向量维度 | application.properties | 2560 | Embedding模型输出维度 |
| 索引类型 | application.properties | IVF_FLAT | Milvus索引类型 |
| 距离度量 | application.properties | COSINE | 向量相似度度量方式 |
| 分块大小 | ChunkStrategy | 512 token | 文档分块的最大token数 |
| RRF_K | HybridRetriever | 60 | RRF融合平滑参数 |
| 向量权重 | HybridRetriever | 0.6 | 向量检索在融合中的权重 |
| BM25权重 | HybridRetriever | 0.4 | BM25检索在融合中的权重 |
| Token预算 | TokenBudgetAdvisor | 3000 | System Prompt最大token数 |
| 检索超时 | HybridRetriever | 8秒 | 混合检索超时时间 |

---

## 十、技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.2.x |
| AI框架 | Spring AI | 1.0.0 |
| 向量数据库 | Milvus | 2.x |
| 缓存 | Redis | 7.x |
| 模型 | Qwen/Qwen3.5-9B | - |
| Embedding模型 | Qwen/Qwen3-Embedding-4B | - |

---

## 十一、待优化项

| 优化项 | 描述 | 优先级 |
|--------|------|--------|
| RerankService集成 | 将LLM重排服务集成到检索链路中 | 中 |
| 增量索引更新 | 支持文档更新时的增量索引更新 | 中 |
| 多租户隔离 | 支持多租户场景下的向量检索隔离 | 低 |
| 检索日志分析 | 增加检索质量分析和日志记录 | 低 |
