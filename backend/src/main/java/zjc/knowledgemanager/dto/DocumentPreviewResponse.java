package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档预览响应DTO
 */
public class DocumentPreviewResponse {
    
    private Long id;
    
    private String originalFilename;
    
    private String fileType;
    
    private Long fileSize;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
    
    private String content; // 文档内容（文本形式）
    
    private String previewUrl; // 预览URL（如果是图片等）
    
    private Integer status; // 文档状态
    
    private UserInfo uploader;
    
    private List<TagDTO> tags;
    
    // 新增字段
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private String description;
    
    public DocumentPreviewResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getPreviewUrl() {
        return previewUrl;
    }
    
    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public UserInfo getUploader() {
        return uploader;
    }
    
    public void setUploader(UserInfo uploader) {
        this.uploader = uploader;
    }
    
    public List<TagDTO> getTags() {
        return tags;
    }
    
    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 用户信息
     */
    public static class UserInfo {
        
        private Long id;
        
        private String username;
        
        private String realName;
        
        private String avatar;
        
        public UserInfo() {}
        
        public UserInfo(Long id, String username, String realName, String avatar) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.avatar = avatar;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getRealName() {
            return realName;
        }
        
        public void setRealName(String realName) {
            this.realName = realName;
        }
        
        public String getAvatar() {
            return avatar;
        }
        
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        @Override
        public String toString() {
            return "UserInfo{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", realName='" + realName + '\'' +
                    ", avatar='" + avatar + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "DocumentPreviewResponse{" +
                "id=" + id +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileSize=" + fileSize +
                ", uploadTime=" + uploadTime +
                ", content='" + content + '\'' +
                ", previewUrl='" + previewUrl + '\'' +
                ", status=" + status +
                ", uploader=" + uploader +
                ", tags=" + tags +
                ", knowledgeBaseId=" + knowledgeBaseId +
                ", knowledgeBaseName='" + knowledgeBaseName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

