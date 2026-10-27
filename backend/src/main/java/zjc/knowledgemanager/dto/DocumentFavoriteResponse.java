package zjc.knowledgemanager.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档收藏响应DTO
 */
public class DocumentFavoriteResponse {
    
    private Long total;
    private List<FavoriteDocumentItem> documents;

    // 构造函数
    public DocumentFavoriteResponse() {}

    public DocumentFavoriteResponse(Long total, List<FavoriteDocumentItem> documents) {
        this.total = total;
        this.documents = documents;
    }

    // Getter和Setter方法
    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<FavoriteDocumentItem> getDocuments() {
        return documents;
    }

    public void setDocuments(List<FavoriteDocumentItem> documents) {
        this.documents = documents;
    }

    /**
     * 收藏文档项
     */
    public static class FavoriteDocumentItem {
        private Long documentId;
        private String originalFilename;
        private String fileType;
        private Long fileSize;
        private Integer status;
        private String statusText;
        private Long knowledgeBaseId;
        private String knowledgeBaseName;
        private LocalDateTime favoriteTime;
        private List<TagDTO> tags;

        // 构造函数
        public FavoriteDocumentItem() {}

        // Getter和Setter方法
        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getStatusText() {
            return statusText;
        }

        public void setStatusText(String statusText) {
            this.statusText = statusText;
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

        public LocalDateTime getFavoriteTime() {
            return favoriteTime;
        }

        public void setFavoriteTime(LocalDateTime favoriteTime) {
            this.favoriteTime = favoriteTime;
        }

        public List<TagDTO> getTags() {
            return tags;
        }

        public void setTags(List<TagDTO> tags) {
            this.tags = tags;
        }

        @Override
        public String toString() {
            return "FavoriteDocumentItem{" +
                    "documentId=" + documentId +
                    ", originalFilename='" + originalFilename + '\'' +
                    ", fileType='" + fileType + '\'' +
                    ", fileSize=" + fileSize +
                    ", status=" + status +
                    ", statusText='" + statusText + '\'' +
                    ", knowledgeBaseId=" + knowledgeBaseId +
                    ", knowledgeBaseName='" + knowledgeBaseName + '\'' +
                    ", favoriteTime=" + favoriteTime +
                    ", tags=" + tags +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DocumentFavoriteResponse{" +
                "total=" + total +
                ", documents=" + documents +
                '}';
    }
}