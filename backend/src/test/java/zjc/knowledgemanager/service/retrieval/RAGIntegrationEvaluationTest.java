package zjc.knowledgemanager.service.retrieval;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RAGIntegrationEvaluationTest {

    private static final Logger logger = LoggerFactory.getLogger(RAGIntegrationEvaluationTest.class);

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private BM25Retriever bm25Retriever;

    private List<TestQuery> testQueries;

    private void initTestQueries() {
        testQueries = new ArrayList<>();

        testQueries.add(new TestQuery("拼多多的特点", Arrays.asList("拼多多"), "电商平台特性"));
        testQueries.add(new TestQuery("淘宝电商平台", Arrays.asList("淘宝"), "电商平台特性"));
        testQueries.add(new TestQuery("京东物流", Arrays.asList("京东"), "电商平台特性"));
        testQueries.add(new TestQuery("抖音直播", Arrays.asList("抖音"), "电商平台特性"));
        testQueries.add(new TestQuery("微信小程序", Arrays.asList("微信"), "电商平台特性"));

        testQueries.add(new TestQuery("电商平台对比分析", Arrays.asList("拼多多", "淘宝", "京东"), "多文档匹配"));
        testQueries.add(new TestQuery("社交电商模式", Arrays.asList("拼多多", "抖音", "微信"), "多文档匹配"));

        testQueries.add(new TestQuery("拼多多和淘宝区别", Arrays.asList("拼多多", "淘宝"), "对比查询"));
        testQueries.add(new TestQuery("京东和拼多多对比", Arrays.asList("京东", "拼多多"), "对比查询"));

        testQueries.add(new TestQuery("电商运营策略", Arrays.asList("运营"), "语义检索"));
        testQueries.add(new TestQuery("用户增长方法", Arrays.asList("增长"), "语义检索"));
        testQueries.add(new TestQuery("数据分析应用", Arrays.asList("数据"), "语义检索"));
    }

    @Test
    void testEndToEndEvaluation() {
        logger.info("========== RAG集成测试：端到端召回率与命中率评估 ==========");

        initTestQueries();

        int[] topKValues = {1, 3, 5, 10};

        logger.info("\n测试查询总数: {}", testQueries.size());

        for (int topK : topKValues) {
            logger.info("\n" + "=".repeat(60));
            logger.info("TopK = {}", topK);
            logger.info("=".repeat(60));

            evaluateAndReport(topK);
        }
    }

    @Test
    void testRetrievalMethodComparison() {
        logger.info("========== 检索方法对比测试 ==========");

        initTestQueries();
        int topK = 5;

        logger.info("\n| 检索方法 | Recall@{} | HitRate@{} | MRR | 平均耗时(ms) |", topK, topK);
        logger.info("|----------|-----------|------------|-----|--------------|");

        EvaluationResult vectorResult = evaluateRetrievalMethod(
            "纯向量检索",
            query -> vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build()),
            topK);

        EvaluationResult bm25Result = evaluateRetrievalMethod(
            "纯BM25检索",
            query -> bm25Retriever.search(query, topK, null).stream()
                .map(BM25Retriever.ScoredDocument::document)
                .collect(Collectors.toList()),
            topK);

        EvaluationResult hybridResult = evaluateRetrievalMethod(
            "混合检索",
            query -> hybridRetriever.search(query, topK, null),
            topK);

        logger.info("| 纯向量检索 | {:.2f}% | {:.2f}% | {:.4f} | {} |",
            vectorResult.recallRate * 100, vectorResult.hitRate * 100, vectorResult.mrr, vectorResult.avgLatencyMs);
        logger.info("| 纯BM25检索 | {:.2f}% | {:.2f}% | {:.4f} | {} |",
            bm25Result.recallRate * 100, bm25Result.hitRate * 100, bm25Result.mrr, bm25Result.avgLatencyMs);
        logger.info("| 混合检索 | {:.2f}% | {:.2f}% | {:.4f} | {} |",
            hybridResult.recallRate * 100, hybridResult.hitRate * 100, hybridResult.mrr, hybridResult.avgLatencyMs);
    }

    @Test
    void testQueryAnalysis() {
        logger.info("========== 查询分析测试 ==========");

        initTestQueries();
        int topK = 5;

        logger.info("\n详细查询结果分析 (TopK={}):", topK);
        logger.info("-" + "-".repeat(100));
        logger.info(String.format("%-4s | %-35s | %-20s | %-10s | %-10s | %-10s",
            "序号", "查询", "类型", "Recall", "Hit", "MRR"));
        logger.info("-" + "-".repeat(100));

        int idx = 1;
        for (TestQuery query : testQueries) {
            List<Document> results = hybridRetriever.search(query.query(), topK, null);

            boolean hit = false;
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
                            hit = true;
                            break;
                        }
                    }
                }
            }

            double recall = hits > 0 ? 1.0 : 0.0;
            double mrr = rank > 0 ? 1.0 / rank : 0.0;

            logger.info(String.format("%-4d | %-35s | %-20s | %-10.2f | %-10s | %-10.4f",
                idx++, query.query(), query.queryType(), recall, hit ? "命中" : "未命中", mrr));
        }
    }

    private void evaluateAndReport(int topK) {
        EvaluationResult result = evaluate(testQueries, topK);

        logger.info("\n评估结果:");
        logger.info("  Recall@{} = {:.2f}%", topK, result.recallRate * 100);
        logger.info("  HitRate@{} = {:.2f}%", topK, result.hitRate * 100);
        logger.info("  MRR = {:.4f}", result.mrr);
        logger.info("  平均延迟 = {}ms", result.avgLatencyMs);
    }

    private EvaluationResult evaluateRetrievalMethod(String methodName,
                                                     RetrievalFunction retrievalFunc,
                                                     int topK) {
        double totalRecall = 0;
        double totalHit = 0;
        double totalMRR = 0;
        long totalTime = 0;

        for (TestQuery query : testQueries) {
            long start = System.currentTimeMillis();
            List<Document> results = retrievalFunc.retrieve(query.query());
            long end = System.currentTimeMillis();
            totalTime += (end - start);

            boolean hit = false;
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
                            hit = true;
                            break;
                        }
                    }
                }
            }

            double recall = hits > 0 ? 1.0 : 0.0;
            totalRecall += recall;
            totalHit += hit ? 1 : 0;
            totalMRR += rank > 0 ? 1.0 / rank : 0;
        }

        return new EvaluationResult(
            totalRecall / testQueries.size(),
            totalHit / testQueries.size(),
            totalMRR / testQueries.size(),
            testQueries.size(),
            totalTime / testQueries.size()
        );
    }

    private EvaluationResult evaluate(List<TestQuery> queries, int topK) {
        double totalRecall = 0;
        double totalHit = 0;
        double totalMRR = 0;
        long totalTime = 0;

        for (TestQuery query : queries) {
            long start = System.currentTimeMillis();
            List<Document> results = hybridRetriever.search(query.query(), topK, null);
            long end = System.currentTimeMillis();
            totalTime += (end - start);

            boolean hit = false;
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
                            hit = true;
                            break;
                        }
                    }
                }
            }

            double recall = hits > 0 ? 1.0 : 0.0;
            totalRecall += recall;
            totalHit += hit ? 1 : 0;
            totalMRR += rank > 0 ? 1.0 / rank : 0;
        }

        return new EvaluationResult(
            totalRecall / queries.size(),
            totalHit / queries.size(),
            totalMRR / queries.size(),
            queries.size(),
            totalTime / queries.size()
        );
    }

    @FunctionalInterface
    private interface RetrievalFunction {
        List<Document> retrieve(String query);
    }

    private record TestQuery(String query, List<String> expectedKeywords, String queryType) {}

    private record EvaluationResult(
        double recallRate, 
        double hitRate, 
        double mrr, 
        int queryCount,
        long avgLatencyMs) {
        
        public EvaluationResult(double recallRate, double hitRate, double mrr, int queryCount) {
            this(recallRate, hitRate, mrr, queryCount, 0);
        }
    }
}
