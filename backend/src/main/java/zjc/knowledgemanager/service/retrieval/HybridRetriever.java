package zjc.knowledgemanager.service.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zjc.knowledgemanager.service.CacheService;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class HybridRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HybridRetriever.class);
    private static final int RRF_K = 60;
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double BM25_WEIGHT = 0.4;
    private static final int SEARCH_TIMEOUT_SECONDS = 8;
    private static final int EXPANSION_FACTOR = 2;

    private final VectorStore vectorStore;
    private final BM25Retriever bm25Retriever;
    private final ExecutorService executorService;
    private final CacheService cacheService;
    private RerankService rerankService;
    
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private static final int LOCAL_CACHE_TTL_SECONDS = 30;

    @Autowired(required = false)
    public void setRerankService(RerankService rerankService) {
        this.rerankService = rerankService;
    }

    @Autowired
    public HybridRetriever(VectorStore vectorStore, BM25Retriever bm25Retriever,
                          CacheService cacheService) {
        this.vectorStore = vectorStore;
        this.bm25Retriever = bm25Retriever;
        this.cacheService = cacheService;
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    public HybridRetriever(VectorStore vectorStore, BM25Retriever bm25Retriever) {
        this(vectorStore, bm25Retriever, null);
    }
    
    public HybridRetriever(VectorStore vectorStore, BM25Retriever bm25Retriever,
                          CacheService cacheService, RerankService rerankService) {
        this.vectorStore = vectorStore;
        this.bm25Retriever = bm25Retriever;
        this.cacheService = cacheService;
        this.rerankService = rerankService;
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    @PreDestroy
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private record CacheEntry(List<Document> results, long timestamp) {}
    
    private List<Document> getLocalCache(String query, Set<String> fileIds) {
        String cacheKey = query + ":" + (fileIds != null ? String.join(",", fileIds) : "all");
        CacheEntry entry = localCache.get(cacheKey);
        if (entry != null && System.currentTimeMillis() - entry.timestamp() < LOCAL_CACHE_TTL_SECONDS * 1000) {
            logger.debug("Local cache hit for query: {}", query);
            return entry.results();
        }
        return null;
    }
    
    private void putLocalCache(String query, Set<String> fileIds, List<Document> results) {
        String cacheKey = query + ":" + (fileIds != null ? String.join(",", fileIds) : "all");
        localCache.put(cacheKey, new CacheEntry(results, System.currentTimeMillis()));
        if (localCache.size() > 100) {
            localCache.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue((a, b) -> Long.compare(a.timestamp(), b.timestamp())))
                    .limit(localCache.size() - 50)
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(localCache::remove);
        }
    }

    public List<Document> search(String query, int topK, Set<String> fileIds) {
        long startTime = System.currentTimeMillis();
        
        List<Document> cachedResults = getLocalCache(query, fileIds);
        if (cachedResults != null) {
            return cachedResults;
        }
        
        int expandedTopK = topK * EXPANSION_FACTOR;
        
        CompletableFuture<List<BM25Retriever.ScoredDocument>> bm25Future = CompletableFuture.supplyAsync(
                () -> bm25Retriever.search(query, expandedTopK, fileIds), executorService);
        
        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(
                () -> searchVector(query, expandedTopK, fileIds), executorService);
        
        List<BM25Retriever.ScoredDocument> bm25Results;
        List<Document> vectorResults;
        
        try {
            CompletableFuture.allOf(bm25Future, vectorFuture)
                    .get(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            bm25Results = bm25Future.get();
            vectorResults = vectorFuture.get();
        } catch (TimeoutException e) {
            logger.warn("Hybrid search timeout after {}s for query: {}", SEARCH_TIMEOUT_SECONDS, query);
            bm25Results = bm25Future.join();
            vectorResults = vectorFuture.join();
        } catch (Exception e) {
            logger.error("Hybrid search failed for query: {}", query, e);
            bm25Results = bm25Retriever.search(query, topK * 2, fileIds);
            vectorResults = searchVector(query, topK * 2, fileIds);
        }
        
        List<Document> fusedResults = fuseWithRRF(bm25Results, vectorResults, expandedTopK);
        
        if (rerankService != null && !fusedResults.isEmpty()) {
            fusedResults = rerankService.rerank(query, fusedResults, topK);
        } else {
            fusedResults = fusedResults.stream().limit(topK).collect(Collectors.toList());
        }
        
        putLocalCache(query, fileIds, fusedResults);
        
        logger.debug("Hybrid search completed in {}ms, bm25={}, vector={}, fused={}",
                System.currentTimeMillis() - startTime, bm25Results.size(), vectorResults.size(), fusedResults.size());
        
        return fusedResults;
    }

    private List<Document> searchVector(String query, int topK, Set<String> fileIds) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);
            
            if (fileIds != null && !fileIds.isEmpty()) {
                FilterExpressionBuilder f = new FilterExpressionBuilder();
                Filter.Expression expression = f.in("fileId", fileIds.toArray()).build();
                builder = builder.filterExpression(expression);
            }
            
            return vectorStore.similaritySearch(builder.build());
        } catch (Exception e) {
            logger.error("Vector search failed", e);
            return List.of();
        }
    }

    private List<Document> fuseWithRRF(List<BM25Retriever.ScoredDocument> bm25Results,
                                        List<Document> vectorResults, int topK) {
        Map<String, Double> scores = new HashMap<>();
        
        for (int i = 0; i < bm25Results.size(); i++) {
            BM25Retriever.ScoredDocument scoredDoc = bm25Results.get(i);
            if (scoredDoc.document() != null) {
                double rrfScore = 1.0 / (RRF_K + i + 1);
                scores.merge(scoredDoc.document().getId(), rrfScore * BM25_WEIGHT, Double::sum);
            }
        }
        
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            scores.merge(doc.getId(), rrfScore * VECTOR_WEIGHT, Double::sum);
        }
        
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

    private Document findDocumentById(String id, List<BM25Retriever.ScoredDocument> bm25Results,
                                      List<Document> vectorResults) {
        for (BM25Retriever.ScoredDocument scoredDoc : bm25Results) {
            if (scoredDoc.document() != null && id.equals(scoredDoc.document().getId())) {
                return scoredDoc.document();
            }
        }
        
        for (Document doc : vectorResults) {
            if (id.equals(doc.getId())) {
                return doc;
            }
        }
        
        return null;
    }

    public void addDocuments(List<Document> documents) {
        bm25Retriever.addDocuments(documents);
        vectorStore.add(documents);
        logger.info("Documents added to hybrid index: {}", documents.size());
    }
}