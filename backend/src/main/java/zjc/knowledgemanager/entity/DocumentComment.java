package zjc.knowledgemanager.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 文档评论实体类
 */
public class DocumentComment {
    
    private Long id;
    
    @JsonProperty("document_id")
    private Long documentId;
    
    @JsonProperty("user_id")
    private Long userId;
    
    private String content;
    
    @JsonProperty("parent_id")
    private Long parentId; // 父评论ID，用于回复功能
    
    @JsonProperty("comment_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime commentTime;
    
    @JsonProperty("is_deleted")
    private Integer isDeleted; // 0-未删除，1-已删除
    
    // 关联查询字段
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    public DocumentComment() {}
    
    public DocumentComment(Long documentId, Long userId, String content, Long parentId) {
        this.documentId = documentId;
        this.userId = userId;
        this.content = content;
        this.parentId = parentId;
        this.commentTime = LocalDateTime.now();
        this.isDeleted = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
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
    
    public LocalDateTime getCommentTime() {
        return commentTime;
    }
    
    public void setCommentTime(LocalDateTime commentTime) {
        this.commentTime = commentTime;
    }
    
    public Integer getIsDeleted() {
        return isDeleted;
    }
    
    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    @Override
    public String toString() {
        return "DocumentComment{" +
                "id=" + id +
                ", documentId=" + documentId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", parentId=" + parentId +
                ", commentTime=" + commentTime +
                ", isDeleted=" + isDeleted +
                ", user=" + user +
                '}';
    }
}

