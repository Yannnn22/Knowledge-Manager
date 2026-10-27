package zjc.knowledgemanager.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 长期记忆存储服务
 * 封装 Milvus long_term_memory 集合的 CRUD 操作
 */
@Service
@ConditionalOnBean(name = "memoryVectorStore")
public class MemoryStoreService {

    private final VectorStore memoryVectorStore;

    public MemoryStoreService(@Qualifier("memoryVectorStore") VectorStore memoryVectorStore) {
        this.memoryVectorStore = memoryVectorStore;
    }

    /**
     * 存储一条记忆到 Milvus
     * @param content 记忆文本
     * @param metadata 元数据（memory_type, conversation_id, importance, version 等）
     */
    public void store(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        memoryVectorStore.add(List.of(doc));
    }

    /**
     * 搜索语义相似的记忆
     * @param query 查询文本
     * @param topK  返回数量
     * @param threshold 相似度阈值
     * @return 相似记忆列表
     */
    public List<Document> searchSimilar(String query, int topK, double threshold) {
        return memoryVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build());
    }

    /**
     * 用 upsert 更新一条已有记忆（合并场景用）
     * @param docId 已有记忆的 doc id
     * @param newContent 合并后的新内容
     * @param newMetadata 合并后的新元数据
     */
    public void upsert(String docId, String newContent, Map<String, Object> newMetadata) {
        Document doc = new Document(docId, newContent, newMetadata);
        memoryVectorStore.add(List.of(doc));
    }

    /**
     * 搜索指定用户的有效记忆（双时间线：仅 active）
     * @param query 查询文本
     * @param userId 用户ID
     * @param topK 返回数量
     * @param threshold 相似度阈值
     * @return 相似记忆列表
     */
    public List<Document> searchByUser(String query, Long userId, int topK, double threshold) {
        FilterExpressionBuilder f = new FilterExpressionBuilder();
        Filter.Expression filter = f.and(
                f.eq("user_id", userId),
                f.eq("status", "active")).build();

        return memoryVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .filterExpression(filter)
                        .build());
    }

    /**
     * 查询过期记忆（双时间线：仅 expired）
     * 用于回溯历史记忆，即使已被新事实取代
     * @param userId 用户ID
     * @param topK 返回数量
     * @return 过期记忆列表
     */
    public List<Document> queryExpiredMemories(Long userId, int topK) {
        FilterExpressionBuilder f = new FilterExpressionBuilder();
        Filter.Expression filter = f.and(
                f.eq("user_id", userId),
                f.eq("status", "expired")).build();

        return memoryVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("")
                        .topK(topK)
                        .similarityThreshold(0.0)
                        .filterExpression(filter)
                        .build());
    }

    /**
     * 查询用户的所有记忆（双时间线：active + expired）
     * @param userId 用户ID
     * @param topK 返回数量
     * @return 所有记忆列表
     */
    public List<Document> queryAllMemories(Long userId, int topK) {
        FilterExpressionBuilder f = new FilterExpressionBuilder();
        Filter.Expression filter = f.eq("user_id", userId).build();

        return memoryVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("")
                        .topK(topK)
                        .similarityThreshold(0.0)
                        .filterExpression(filter)
                        .build());
    }
}
