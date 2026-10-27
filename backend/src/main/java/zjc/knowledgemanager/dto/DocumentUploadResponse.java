package zjc.knowledgemanager.dto;

/**
 * 文档上传响应DTO
 */
public class DocumentUploadResponse {
    
    private Long documentId;
    private String statusUrl;

    public DocumentUploadResponse() {}

    public DocumentUploadResponse(Long documentId, String statusUrl) {
        this.documentId = documentId;
        this.statusUrl = statusUrl;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }
} 