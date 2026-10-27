package zjc.knowledgemanager.dto;

/**
 * 知识库统计响应DTO
 */
public class KnowledgeBaseStatsResponse {
    
    private Integer totalDocuments;
    private Integer processing;
    private Integer completed;
    private Integer failed;

    public KnowledgeBaseStatsResponse() {}

    public KnowledgeBaseStatsResponse(Integer totalDocuments, Integer processing, Integer completed, Integer failed) {
        this.totalDocuments = totalDocuments;
        this.processing = processing;
        this.completed = completed;
        this.failed = failed;
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Integer getProcessing() {
        return processing;
    }

    public void setProcessing(Integer processing) {
        this.processing = processing;
    }

    public Integer getCompleted() {
        return completed;
    }

    public void setCompleted(Integer completed) {
        this.completed = completed;
    }

    public Integer getFailed() {
        return failed;
    }

    public void setFailed(Integer failed) {
        this.failed = failed;
    }
} 