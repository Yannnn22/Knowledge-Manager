package zjc.knowledgemanager.dto;

import java.util.List;

/**
 * 文档搜索响应DTO
 */
public class DocumentSearchResponse {
    
    private List<SearchResult> results;

    public DocumentSearchResponse() {}

    public DocumentSearchResponse(List<SearchResult> results) {
        this.results = results;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

    /**
     * 搜索结果DTO
     */
    public static class SearchResult {
        private Long documentId;
        private String title;
        private String snippet;
        private Double score;
        private Long knowledgeBaseId;
        private String knowledgeBaseName;

        public SearchResult() {}

        public SearchResult(Long documentId, String title, String snippet, Double score, Long knowledgeBaseId, String knowledgeBaseName) {
            this.documentId = documentId;
            this.title = title;
            this.snippet = snippet;
            this.score = score;
            this.knowledgeBaseId = knowledgeBaseId;
            this.knowledgeBaseName = knowledgeBaseName;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Long getKnowledgeBaseId() {
            return knowledgeBaseId;
        }

        public void setKnowledgeBaseId(Long knowledgeBaseId) {
            this.knowledgeBaseId = knowledgeBaseId;
        }

        public String getKnowledgeBaseName() {
            return knowledgeBaseName;
        }

        public void setKnowledgeBaseName(String knowledgeBaseName) {
            this.knowledgeBaseName = knowledgeBaseName;
        }
    }
} 