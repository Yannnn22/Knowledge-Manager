package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 文档评论请求DTO
 */
public class DocumentCommentRequest {
    
    private Long documentId; // 文档ID（从路径变量获取，不需要验证）
    
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    private Long parentId; // 父评论ID，用于回复功能
    
    public DocumentCommentRequest() {}
    
    public DocumentCommentRequest(Long documentId, String content, Long parentId) {
        this.documentId = documentId;
        this.content = content;
        this.parentId = parentId;
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
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    @Override
    public String toString() {
        return "DocumentCommentRequest{" +
                "documentId=" + documentId +
                ", content='" + content + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}
