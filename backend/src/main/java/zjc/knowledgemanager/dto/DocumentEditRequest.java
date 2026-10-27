package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 文档编辑请求DTO
 */
public class DocumentEditRequest {
    
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    @NotBlank(message = "文档内容不能为空")
    private String content;
    
    private String description; // 文档描述
    
    public DocumentEditRequest() {}
    
    public DocumentEditRequest(Long documentId, String content, String description) {
        this.documentId = documentId;
        this.content = content;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "DocumentEditRequest{" +
                "documentId=" + documentId +
                ", content='" + content + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

