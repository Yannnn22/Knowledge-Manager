package zjc.knowledgemanager.entity;

import java.time.LocalDateTime;

/**
 * 文档收藏实体类
 */
public class DocumentFavorite {
    
    private Long id;
    private Long userId;
    private Long documentId;
    private LocalDateTime createTime;

    // 构造函数
    public DocumentFavorite() {}

    public DocumentFavorite(Long userId, Long documentId) {
        this.userId = userId;
        this.documentId = documentId;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "DocumentFavorite{" +
                "id=" + id +
                ", userId=" + userId +
                ", documentId=" + documentId +
                ", createTime=" + createTime +
                '}';
    }
}