package zjc.knowledgemanager.service.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class BM25Retriever {

    private static final Logger logger = LoggerFactory.getLogger(BM25Retriever.class);
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]|[a-zA-Z0-9]+");
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private Map<String, Map<String, Integer>> documentTermFrequencies = new ConcurrentHashMap<>();
    private Map<String, Integer> documentLengths = new ConcurrentHashMap<>();
    private Map<String, Set<String>> documentTerms = new ConcurrentHashMap<>();
    private Map<String, Integer> inverseDocumentFrequencies = new ConcurrentHashMap<>();
    private Map<String, Document> documentStore = new ConcurrentHashMap<>();
    private int totalDocuments = 0;
    private double averageDocumentLength = 0;

    public void addDocuments(List<Document> documents) {
        for (Document doc : documents) {
            String docId = doc.getId();
            String content = doc.getText();
            
            Map<String, Integer> termFreq = computeTermFrequency(content);
            documentTermFrequencies.put(docId, termFreq);
            documentLengths.put(docId, content.length());
            documentTerms.put(docId, termFreq.keySet());
            documentStore.put(docId, doc);
            
            for (String term : termFreq.keySet()) {
                inverseDocumentFrequencies.merge(term, 1, Integer::sum);
            }
            
            totalDocuments++;
        }
        
        updateAverageDocumentLength();
        logger.info("BM25 index updated: {} documents added, total: {}", documents.size(), totalDocuments);
    }

    public List<ScoredDocument> search(String query, int topK, Set<String> fileIds) {
        if (totalDocuments == 0) {
            return List.of();
        }
        
        Map<String, Double> scores = new HashMap<>();
        Map<String, Integer> queryTermFreq = computeTermFrequency(query);
        
        for (String docId : documentTermFrequencies.keySet()) {
            Document doc = getDocumentById(docId);
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
                .map(e -> new ScoredDocument(getDocumentById(e.getKey()), e.getValue(), "bm25"))
                .toList();
    }

    private double calculateBM25Score(String docId, Map<String, Integer> queryTermFreq) {
        double score = 0;
        Map<String, Integer> docTermFreq = documentTermFrequencies.get(docId);
        int docLength = documentLengths.get(docId);
        
        for (Map.Entry<String, Integer> entry : queryTermFreq.entrySet()) {
            String term = entry.getKey();
            int queryFreq = entry.getValue();
            
            if (!docTermFreq.containsKey(term)) {
                continue;
            }
            
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
        WORD_PATTERN.matcher(text).results()
                .map(m -> m.group())
                .forEach(term -> termFreq.merge(term, 1, Integer::sum));
        return termFreq;
    }

    private void updateAverageDocumentLength() {
        if (totalDocuments == 0) {
            averageDocumentLength = 0;
            return;
        }
        
        int totalLength = documentLengths.values().stream().mapToInt(Integer::intValue).sum();
        averageDocumentLength = (double) totalLength / totalDocuments;
    }

    private Document getDocumentById(String docId) {
        return documentStore.get(docId);
    }

    public void clear() {
        documentTermFrequencies.clear();
        documentLengths.clear();
        documentTerms.clear();
        inverseDocumentFrequencies.clear();
        documentStore.clear();
        totalDocuments = 0;
        averageDocumentLength = 0;
    }

    public record ScoredDocument(Document document, double score, String source) {}
}