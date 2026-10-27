package zjc.knowledgemanager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private static final String CONVERSATION_HISTORY_PREFIX = "conv_history:";
    private static final String RETRIEVAL_RESULT_PREFIX = "retrieval:";
    private static final String EMBEDDING_PREFIX = "embedding:";

    private static final int CONVERSATION_CACHE_MINUTES = 30;
    private static final int RETRIEVAL_CACHE_MINUTES = 10;
    private static final int EMBEDDING_CACHE_HOURS = 24;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate redis) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
    }

    // ========== 对话历史缓存 ==========

    public void cacheConversationHistory(String conversationId, List<Map<String, Object>> messages) {
        try {
            String key = CONVERSATION_HISTORY_PREFIX + conversationId;
            String value = objectMapper.writeValueAsString(messages);
            redis.opsForValue().set(key, value, CONVERSATION_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存对话历史失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCachedConversationHistory(String conversationId) {
        try {
            String key = CONVERSATION_HISTORY_PREFIX + conversationId;
            String value = redis.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, 
                        new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("读取对话历史缓存失败: {}", e.getMessage());
        }
        return null;
    }

    public void invalidateConversationHistory(String conversationId) {
        String key = CONVERSATION_HISTORY_PREFIX + conversationId;
        redis.delete(key);
    }

    // ========== 检索结果缓存 ==========

    public void cacheRetrievalResult(String conversationId, String query, List<Document> documents) {
        try {
            String key = RETRIEVAL_RESULT_PREFIX + conversationId + ":" + hash(query);
            List<Map<String, Object>> docMaps = documents.stream()
                    .map(doc -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("content", doc.getText());
                        map.put("metadata", doc.getMetadata());
                        return map;
                    })
                    .collect(Collectors.toList());
            String value = objectMapper.writeValueAsString(docMaps);
            redis.opsForValue().set(key, value, RETRIEVAL_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存检索结果失败: {}", e.getMessage());
        }
    }

    public List<Document> getCachedRetrievalResult(String conversationId, String query) {
        try {
            String key = RETRIEVAL_RESULT_PREFIX + conversationId + ":" + hash(query);
            String value = redis.opsForValue().get(key);
            if (value != null) {
                log.debug("命中检索缓存: conversationId={}, query={}", conversationId, query);
                List<Map<String, Object>> docMaps = objectMapper.readValue(value, 
                        new TypeReference<List<Map<String, Object>>>() {});
                return docMaps.stream()
                        .map(map -> new Document((String) map.get("content"), (Map<String, Object>) map.get("metadata")))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("读取检索缓存失败: {}", e.getMessage());
        }
        return null;
    }

    public void invalidateRetrievalCache(String conversationId) {
        var keys = redis.keys(RETRIEVAL_RESULT_PREFIX + conversationId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    // ========== Embedding缓存 ==========

    public void cacheEmbedding(String text, float[] embedding) {
        try {
            String key = EMBEDDING_PREFIX + hash(text);
            String value = objectMapper.writeValueAsString(embedding);
            redis.opsForValue().set(key, value, EMBEDDING_CACHE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("缓存Embedding失败: {}", e.getMessage());
        }
    }

    public float[] getCachedEmbedding(String text) {
        try {
            String key = EMBEDDING_PREFIX + hash(text);
            String value = redis.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, float[].class);
            }
        } catch (Exception e) {
            log.warn("读取Embedding缓存失败: {}", e.getMessage());
        }
        return null;
    }

    // ========== 工具方法 ==========

    private String hash(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}