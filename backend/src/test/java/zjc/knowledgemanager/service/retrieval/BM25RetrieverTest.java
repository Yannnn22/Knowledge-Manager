package zjc.knowledgemanager.service.retrieval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BM25RetrieverTest {

    private static final Logger logger = LoggerFactory.getLogger(BM25RetrieverTest.class);

    private BM25Retriever bm25Retriever;

    @BeforeEach
    void setUp() {
        bm25Retriever = new BM25Retriever();
        
        List<Document> documents = new ArrayList<>();
        
        Document doc1 = new Document("拼多多的特点和优势包括社交电商模式、低价策略、拼团玩法等。");
        doc1.getMetadata().put("originalFilename", "拼多多的特点.txt");
        documents.add(doc1);
        
        Document doc2 = new Document("淘宝是阿里巴巴旗下的电商平台，拥有庞大的商品种类和商家资源。");
        doc2.getMetadata().put("originalFilename", "淘宝介绍.docx");
        documents.add(doc2);
        
        Document doc3 = new Document("京东以自营模式为主，提供正品保障和快速配送服务。");
        doc3.getMetadata().put("originalFilename", "京东运营模式.txt");
        documents.add(doc3);
        
        bm25Retriever.addDocuments(documents);
    }

    @Test
    void testChineseTokenization() {
        logger.info("测试中文分词...");
        
        List<BM25Retriever.ScoredDocument> results = bm25Retriever.search("拼多多", 5, null);
        
        logger.info("BM25搜索结果数量: {}", results.size());
        for (BM25Retriever.ScoredDocument scoredDoc : results) {
            Document doc = scoredDoc.document();
            String filename = (String) doc.getMetadata().get("originalFilename");
            logger.info("  文档: {}, 评分: {}", filename, scoredDoc.score());
        }
        
        assertFalse(results.isEmpty(), "BM25搜索结果不应为空");
        
        boolean found = results.stream().anyMatch(sd -> {
            String filename = (String) sd.document().getMetadata().get("originalFilename");
            return filename != null && filename.contains("拼多多");
        });
        
        assertTrue(found, "应该能找到包含'拼多多'的文档");
    }

    @Test
    void testScoreCalculation() {
        logger.info("测试评分计算...");
        
        List<BM25Retriever.ScoredDocument> results = bm25Retriever.search("拼多多", 5, null);
        
        for (BM25Retriever.ScoredDocument scoredDoc : results) {
            assertTrue(scoredDoc.score() > 0, "评分应该大于0");
            String filename = (String) scoredDoc.document().getMetadata().get("originalFilename");
            logger.info("  {}: score={}", filename, scoredDoc.score());
        }
    }

    @Test
    void testRanking() {
        logger.info("测试排序...");
        
        List<BM25Retriever.ScoredDocument> results = bm25Retriever.search("电商平台", 5, null);
        
        logger.info("搜索'电商平台'的结果:");
        for (BM25Retriever.ScoredDocument scoredDoc : results) {
            String filename = (String) scoredDoc.document().getMetadata().get("originalFilename");
            logger.info("  {}: score={}", filename, scoredDoc.score());
        }
        
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i-1).score() >= results.get(i).score(), 
                "结果应该按评分降序排列");
        }
    }
}
