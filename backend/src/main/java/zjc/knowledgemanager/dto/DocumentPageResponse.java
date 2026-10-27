package zjc.knowledgemanager.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档分页响应 DTO
 */
public class DocumentPageResponse {

    private Long total;
    private List<Item> documents;

    public DocumentPageResponse() {}

    public DocumentPageResponse(Long total, List<Item> documents) {
        this.total = total;
        this.documents = documents;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<Item> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Item> documents) {
        this.documents = documents;
    }

    /**
     * 文档条目
     */
    public static class Item {
        private Long id;
        private Long knowledgeBaseId;
        private String originalFilename;
        private String fileType;
        private Long fileSize;
        private Integer status;
        private Long uploaderId;
        private LocalDateTime uploadTime;

        public Item() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getKnowledgeBaseId() {
            return knowledgeBaseId;
        }

        public void setKnowledgeBaseId(Long knowledgeBaseId) {
            this.knowledgeBaseId = knowledgeBaseId;
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

        public Long getUploaderId() {
            return uploaderId;
        }

        public void setUploaderId(Long uploaderId) {
            this.uploaderId = uploaderId;
        }

        public LocalDateTime getUploadTime() {
            return uploadTime;
        }

        public void setUploadTime(LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
        }
    }
}


