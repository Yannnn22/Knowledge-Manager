package zjc.knowledgemanager.service.retrieval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RAGEvaluationTest {

    private static final Logger logger = LoggerFactory.getLogger(RAGEvaluationTest.class);

    private BM25Retriever bm25Retriever;
    private MockVectorStore mockVectorStore;
    private HybridRetriever hybridRetriever;

    private List<TestQuery> testQueries;
    private Map<String, Document> testDocuments;

    @BeforeEach
    void setUp() {
        initTestDocuments();
        initTestQueries();
        initRetrievers();
    }

    private void initTestDocuments() {
        testDocuments = new ConcurrentHashMap<>();

        Document doc1 = new Document("拼多多的特点和优势包括社交电商模式、低价策略、拼团玩法等。拼多多通过社交裂变快速获取用户，主打性价比商品。");
        doc1.getMetadata().put("fileId", "file-001");
        doc1.getMetadata().put("originalFilename", "拼多多的特点.txt");
        doc1.getMetadata().put("knowledgeBaseName", "电商知识库");
        testDocuments.put("拼多多", doc1);

        Document doc2 = new Document("淘宝是阿里巴巴旗下的电商平台，拥有庞大的商品种类和商家资源。淘宝提供C2C和B2C两种模式。");
        doc2.getMetadata().put("fileId", "file-002");
        doc2.getMetadata().put("originalFilename", "淘宝介绍.docx");
        doc2.getMetadata().put("knowledgeBaseName", "电商知识库");
        testDocuments.put("淘宝", doc2);

        Document doc3 = new Document("京东以自营模式为主，提供正品保障和快速配送服务。京东物流是其核心竞争力之一。");
        doc3.getMetadata().put("fileId", "file-003");
        doc3.getMetadata().put("originalFilename", "京东运营模式.txt");
        doc3.getMetadata().put("knowledgeBaseName", "电商知识库");
        testDocuments.put("京东", doc3);

        Document doc4 = new Document("抖音电商通过短视频和直播形式进行商品推广，依托推荐算法实现精准营销。");
        doc4.getMetadata().put("fileId", "file-004");
        doc4.getMetadata().put("originalFilename", "抖音电商模式.md");
        doc4.getMetadata().put("knowledgeBaseName", "电商知识库");
        testDocuments.put("抖音", doc4);

        Document doc5 = new Document("微信小程序电商依托微信生态，提供便捷的购物入口，无需下载APP即可完成购物。");
        doc5.getMetadata().put("fileId", "file-005");
        doc5.getMetadata().put("originalFilename", "微信电商策略.pdf");
        doc5.getMetadata().put("knowledgeBaseName", "电商知识库");
        testDocuments.put("微信", doc5);

        Document doc6 = new Document("电商平台的用户增长策略包括拉新、留存、转化三个核心环节，需要结合数据分析进行优化。");
        doc6.getMetadata().put("fileId", "file-006");
        doc6.getMetadata().put("originalFilename", "用户增长策略.docx");
        doc6.getMetadata().put("knowledgeBaseName", "运营知识库");
        testDocuments.put("增长", doc6);

        Document doc7 = new Document("电商营销活动策划需要考虑活动目标、预算、时间节点和用户触达渠道等因素。");
        doc7.getMetadata().put("fileId", "file-007");
        doc7.getMetadata().put("originalFilename", "营销活动策划.txt");
        doc7.getMetadata().put("knowledgeBaseName", "运营知识库");
        testDocuments.put("营销", doc7);

        Document doc8 = new Document("数据分析在电商运营中的应用包括用户行为分析、销售数据分析、竞品分析等。");
        doc8.getMetadata().put("fileId", "file-008");
        doc8.getMetadata().put("originalFilename", "数据分析应用.md");
        doc8.getMetadata().put("knowledgeBaseName", "运营知识库");
        testDocuments.put("数据", doc8);
    }

    private void initTestQueries() {
        testQueries = new ArrayList<>();

        testQueries.add(new TestQuery("拼多多的社交电商模式", Arrays.asList("拼多多"), "精准匹配"));
        testQueries.add(new TestQuery("淘宝平台有哪些特点", Arrays.asList("淘宝"), "精准匹配"));
        testQueries.add(new TestQuery("京东物流配送服务", Arrays.asList("京东"), "精准匹配"));
        testQueries.add(new TestQuery("抖音直播带货", Arrays.asList("抖音"), "精准匹配"));
        testQueries.add(new TestQuery("微信小程序购物", Arrays.asList("微信"), "精准匹配"));
        testQueries.add(new TestQuery("电商用户增长方法", Arrays.asList("增长"), "精准匹配"));
        testQueries.add(new TestQuery("营销活动策划方案", Arrays.asList("营销"), "精准匹配"));
        testQueries.add(new TestQuery("电商数据分析", Arrays.asList("数据"), "精准匹配"));

        testQueries.add(new TestQuery("电商平台对比", Arrays.asList("拼多多", "淘宝", "京东"), "多文档匹配"));
        testQueries.add(new TestQuery("社交电商平台", Arrays.asList("拼多多", "抖音", "微信"), "多文档匹配"));
        testQueries.add(new TestQuery("电商运营策略", Arrays.asList("增长", "营销", "数据"), "多文档匹配"));

        testQueries.add(new TestQuery("拼多多和淘宝有什么区别", Arrays.asList("拼多多", "淘宝"), "对比查询"));
        testQueries.add(new TestQuery("京东和拼多多哪个好", Arrays.asList("京东", "拼多多"), "对比查询"));

        testQueries.add(new TestQuery("短视频电商怎么做", Arrays.asList("抖音"), "语义相似"));
        testQueries.add(new TestQuery("如何提高电商用户活跃度", Arrays.asList("增长"), "语义相似"));
        testQueries.add(new TestQuery("电商活动促销方案", Arrays.asList("营销"), "语义相似"));
    }

    private void initRetrievers() {
        bm25Retriever = new BM25Retriever();
        mockVectorStore = new MockVectorStore(testDocuments);
        hybridRetriever = new HybridRetriever(mockVectorStore, bm25Retriever);

        bm25Retriever.addDocuments(new ArrayList<>(testDocuments.values()));
    }

    @Test
    void testRecallAndHitRate() {
        logger.info("========== RAG 召回率与命中率测试 ==========");

        int[] topKValues = {1, 3, 5, 10};

        for (int topK : topKValues) {
            logger.info("--- TopK = {} ---", topK);
            
            EvaluationResult vectorResult = evaluateRetrieval(
                query -> mockVectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).build()), 
                topK);
            
            EvaluationResult bm25Result = evaluateRetrieval(
                query -> bm25Retriever.search(query, topK, null).stream()
                    .map(BM25Retriever.ScoredDocument::document)
                    .collect(Collectors.toList()), 
                topK);

            EvaluationResult hybridResult = evaluateRetrieval(
                query -> hybridRetriever.search(query, topK, null), 
                topK);

            printComparisonTable(topK, vectorResult, bm25Result, hybridResult);
        }
    }

    @Test
    void testQueryTypePerformance() {
        logger.info("========== 查询类型性能测试 ==========");

        Map<String, List<TestQuery>> queriesByType = testQueries.stream()
            .collect(Collectors.groupingBy(TestQuery::queryType));

        int topK = 5;

        for (Map.Entry<String, List<TestQuery>> entry : queriesByType.entrySet()) {
            String queryType = entry.getKey();
            List<TestQuery> queries = entry.getValue();

            EvaluationResult vectorResult = evaluateQueries(queries, 
                query -> mockVectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).build()), topK);

            EvaluationResult bm25Result = evaluateQueries(queries,
                query -> bm25Retriever.search(query, topK, null).stream()
                    .map(BM25Retriever.ScoredDocument::document)
                    .collect(Collectors.toList()), topK);

            EvaluationResult hybridResult = evaluateQueries(queries,
                query -> hybridRetriever.search(query, topK, null), topK);

            logger.info("查询类型 [{}] ({}条):", queryType, queries.size());
            logger.info("  纯向量检索 → Recall@{}={}%, HitRate@{}={}%, MRR={}",
                topK, String.format("%.2f", vectorResult.recallRate * 100), 
                topK, String.format("%.2f", vectorResult.hitRate * 100), 
                String.format("%.4f", vectorResult.mrr));
            logger.info("  纯BM25检索 → Recall@{}={}%, HitRate@{}={}%, MRR={}",
                topK, String.format("%.2f", bm25Result.recallRate * 100), 
                topK, String.format("%.2f", bm25Result.hitRate * 100), 
                String.format("%.4f", bm25Result.mrr));
            logger.info("  混合检索 → Recall@{}={}%, HitRate@{}={}%, MRR={}",
                topK, String.format("%.2f", hybridResult.recallRate * 100), 
                topK, String.format("%.2f", hybridResult.hitRate * 100), 
                String.format("%.4f", hybridResult.mrr));
        }
    }

    @Test
    void testDocumentRelevanceDistribution() {
        logger.info("========== 文档相关性分布测试 ==========");

        int topK = 5;

        evaluateDistribution("BM25", topK, 
            query -> bm25Retriever.search(query, topK, null).stream()
                .map(BM25Retriever.ScoredDocument::document)
                .collect(Collectors.toList()));

        evaluateDistribution("向量检索", topK, 
            query -> mockVectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build()));

        evaluateDistribution("混合检索", topK, 
            query -> hybridRetriever.search(query, topK, null));
    }

    private void evaluateDistribution(String methodName, int topK, RetrievalFunction retrievalFunc) {
        Map<String, Integer> hitCount = new HashMap<>();

        for (TestQuery query : testQueries) {
            List<Document> results = retrievalFunc.retrieve(query.query());

            for (String expectedKeyword : query.expectedKeywords()) {
                boolean hit = results.stream().anyMatch(doc -> {
                    String filename = (String) doc.getMetadata().get("originalFilename");
                    return filename != null && filename.contains(expectedKeyword);
                });
                if (hit) {
                    hitCount.merge(expectedKeyword, 1, Integer::sum);
                }
            }
        }

        logger.info("文档命中次数分布 ({}):", methodName);
        hitCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                Document doc = testDocuments.get(entry.getKey());
                String filename = (String) doc.getMetadata().get("originalFilename");
                logger.info("  {} ({}): {}次", entry.getKey(), filename, entry.getValue());
            });
    }

    private EvaluationResult evaluateRetrieval(RetrievalFunction retrievalFunc, int topK) {
        return evaluateQueries(testQueries, retrievalFunc, topK);
    }

    private EvaluationResult evaluateQueries(List<TestQuery> queries, RetrievalFunction retrievalFunc, int topK) {
        double totalRecall = 0;
        double totalHit = 0;
        double totalMRR = 0;

        for (TestQuery query : queries) {
            List<Document> results = retrievalFunc.retrieve(query.query());

            int hits = 0;
            int rank = -1;
            
            for (int i = 0; i < results.size() && i < topK; i++) {
                Document doc = results.get(i);
                String filename = (String) doc.getMetadata().get("originalFilename");
                
                if (filename != null) {
                    for (String expectedKeyword : query.expectedKeywords()) {
                        if (filename.contains(expectedKeyword)) {
                            hits++;
                            if (rank == -1) {
                                rank = i + 1;
                            }
                            break;
                        }
                    }
                }
            }

            double recall = (double) hits / query.expectedKeywords().size();
            double hit = hits > 0 ? 1 : 0;
            double mrr = rank > 0 ? 1.0 / rank : 0;

            totalRecall += recall;
            totalHit += hit;
            totalMRR += mrr;
        }

        return new EvaluationResult(
            totalRecall / queries.size(),
            totalHit / queries.size(),
            totalMRR / queries.size(),
            queries.size()
        );
    }

    private void printComparisonTable(int topK, EvaluationResult vector, EvaluationResult bm25, EvaluationResult hybrid) {
        logger.info("| 检索方法 | Recall@{} | HitRate@{} | MRR |", topK, topK);
        logger.info("|----------|-----------|------------|-----|");
        logger.info("| 纯向量检索 | {}% | {}% | {} |", 
            String.format("%.2f", vector.recallRate * 100),
            String.format("%.2f", vector.hitRate * 100),
            String.format("%.4f", vector.mrr));
        logger.info("| 纯BM25检索 | {}% | {}% | {} |", 
            String.format("%.2f", bm25.recallRate * 100),
            String.format("%.2f", bm25.hitRate * 100),
            String.format("%.4f", bm25.mrr));
        logger.info("| 混合检索 | {}% | {}% | {} |", 
            String.format("%.2f", hybrid.recallRate * 100),
            String.format("%.2f", hybrid.hitRate * 100),
            String.format("%.4f", hybrid.mrr));
    }

    @FunctionalInterface
    private interface RetrievalFunction {
        List<Document> retrieve(String query);
    }

    private record TestQuery(String query, List<String> expectedKeywords, String queryType) {}

    private record EvaluationResult(double recallRate, double hitRate, double mrr, int queryCount) {}

    private static class MockVectorStore implements VectorStore {
        private final Map<String, Document> documents;
        private final Map<String, List<Document>> mockSimilarityCache;

        public MockVectorStore(Map<String, Document> documents) {
            this.documents = documents;
            this.mockSimilarityCache = new ConcurrentHashMap<>();
            initMockSimilarity();
        }

        private void initMockSimilarity() {
            mockSimilarityCache.put("拼多多的社交电商模式", Arrays.asList(
                documents.get("拼多多"), documents.get("微信"), documents.get("抖音")));
            mockSimilarityCache.put("淘宝平台有哪些特点", Arrays.asList(
                documents.get("淘宝"), documents.get("拼多多"), documents.get("京东")));
            mockSimilarityCache.put("京东物流配送服务", Arrays.asList(
                documents.get("京东"), documents.get("淘宝")));
            mockSimilarityCache.put("抖音直播带货", Arrays.asList(
                documents.get("抖音"), documents.get("微信")));
            mockSimilarityCache.put("微信小程序购物", Arrays.asList(
                documents.get("微信"), documents.get("抖音"), documents.get("拼多多")));
            mockSimilarityCache.put("电商用户增长方法", Arrays.asList(
                documents.get("增长"), documents.get("营销")));
            mockSimilarityCache.put("营销活动策划方案", Arrays.asList(
                documents.get("营销"), documents.get("增长")));
            mockSimilarityCache.put("电商数据分析", Arrays.asList(
                documents.get("数据"), documents.get("增长")));
            mockSimilarityCache.put("电商平台对比", Arrays.asList(
                documents.get("拼多多"), documents.get("淘宝"), documents.get("京东"), documents.get("抖音")));
            mockSimilarityCache.put("社交电商平台", Arrays.asList(
                documents.get("拼多多"), documents.get("抖音"), documents.get("微信")));
            mockSimilarityCache.put("电商运营策略", Arrays.asList(
                documents.get("增长"), documents.get("营销"), documents.get("数据")));
            mockSimilarityCache.put("拼多多和淘宝有什么区别", Arrays.asList(
                documents.get("拼多多"), documents.get("淘宝")));
            mockSimilarityCache.put("京东和拼多多哪个好", Arrays.asList(
                documents.get("京东"), documents.get("拼多多")));
            mockSimilarityCache.put("短视频电商怎么做", Arrays.asList(
                documents.get("抖音"), documents.get("营销")));
            mockSimilarityCache.put("如何提高电商用户活跃度", Arrays.asList(
                documents.get("增长"), documents.get("数据")));
            mockSimilarityCache.put("电商活动促销方案", Arrays.asList(
                documents.get("营销"), documents.get("增长")));
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            String query = request.getQuery();
            int topK = request.getTopK();
            
            List<Document> results = mockSimilarityCache.getOrDefault(query, 
                new ArrayList<>(documents.values()));
            
            return results.stream().limit(topK).collect(Collectors.toList());
        }

        @Override
        public void add(List<Document> documents) {}

        @Override
        public void delete(String id) {}

        @Override
        public void delete(List<String> ids) {}

        @Override
        public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression filterExpression) {}
    }
}
