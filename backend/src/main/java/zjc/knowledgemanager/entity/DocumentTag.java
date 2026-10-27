package zjc.knowledgemanager.entity;

import java.time.LocalDateTime;

/**
 * 文档标签关联实体类
 */
public class DocumentTag {
    
    private Long id;
    private Long documentId;
    private Long tagId;
    private Long creatorId;
    private LocalDateTime createTime;

    // 构造函数
    public DocumentTag() {}

    public DocumentTag(Long documentId, Long tagId, Long creatorId) {
        this.documentId = documentId;
        this.tagId = tagId;
        this.creatorId = creatorId;
    }

    // Getter和Setter方法
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

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "DocumentTag{" +
                "id=" + id +
                ", documentId=" + documentId +
                ", tagId=" + tagId +
                ", creatorId=" + creatorId +
                ", createTime=" + createTime +
                '}';
    }
}