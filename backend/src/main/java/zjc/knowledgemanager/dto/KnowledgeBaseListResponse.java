package zjc.knowledgemanager.dto;

import java.util.List;

/**
 * 知识库列表响应DTO
 */
public class KnowledgeBaseListResponse {
    
    private List<KnowledgeBaseInfo> knowledgeBases;

    public KnowledgeBaseListResponse() {}

    public KnowledgeBaseListResponse(List<KnowledgeBaseInfo> knowledgeBases) {
        this.knowledgeBases = knowledgeBases;
    }

    public List<KnowledgeBaseInfo> getKnowledgeBases() {
        return knowledgeBases;
    }

    public void setKnowledgeBases(List<KnowledgeBaseInfo> knowledgeBases) {
        this.knowledgeBases = knowledgeBases;
    }

    /**
     * 知识库信息DTO
     */
    public static class KnowledgeBaseInfo {
        private Long id;
        private String name;
        private String owner;
        private Long ownerId;
        private String department;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private Integer documentCount;
        private Boolean canEdit;
        private Boolean canDelete;

        public KnowledgeBaseInfo() {}

        public KnowledgeBaseInfo(Long id, String name, String owner, Long ownerId, String department,
                                 java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
                                 Integer documentCount, Boolean canEdit, Boolean canDelete) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.ownerId = ownerId;
            this.department = department;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.documentCount = documentCount;
            this.canEdit = canEdit;
            this.canDelete = canDelete;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public Integer getDocumentCount() {
            return documentCount;
        }

        public void setDocumentCount(Integer documentCount) {
            this.documentCount = documentCount;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public java.time.LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Long getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(Long ownerId) {
            this.ownerId = ownerId;
        }

        public Boolean getCanEdit() {
            return canEdit;
        }

        public void setCanEdit(Boolean canEdit) {
            this.canEdit = canEdit;
        }

        public Boolean getCanDelete() {
            return canDelete;
        }

        public void setCanDelete(Boolean canDelete) {
            this.canDelete = canDelete;
        }
    }
} 