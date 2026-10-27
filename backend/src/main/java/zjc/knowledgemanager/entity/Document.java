package zjc.knowledgemanager.entity;

import zjc.knowledgemanager.enums.DocumentStatus;

import java.time.LocalDateTime;

/**
 * 文档实体类 - 映射到 knowledge_document 表
 */
public class Document {
    
    private Long id;
    private Long knowledgeBaseId;
    private String name;                    // 对应 knowledge_document.name
    private String filePath;                // 对应 knowledge_document.file_path
    private String status;                  // 对应 knowledge_document.status (enum类型)
    private LocalDateTime createTime;       // 对应 knowledge_document.create_time
    private LocalDateTime updateTime;       // 对应 knowledge_document.update_time
    private Integer isDeleted;
    
    // 为了保持API兼容性，添加虚拟字段
    private String originalFilename;        // 虚拟字段，从name映射
    private String storagePath;             // 虚拟字段，从filePath映射
    private Long fileSize;                  // 虚拟字段，从文件系统获取
    private String fileType;                // 虚拟字段，从name提取
    private Integer statusCode;             // 虚拟字段，从status转换
    private Long uploaderId;                // 虚拟字段，暂时设为1
    private LocalDateTime uploadTime;       // 虚拟字段，从createTime映射
    private LocalDateTime processStartTime; // 虚拟字段
    private LocalDateTime processEndTime;   // 虚拟字段

    // 构造函数
    public Document() {}

    public Document(Long knowledgeBaseId, String name, String filePath) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.name = name;
        this.filePath = filePath;
        this.status = "processing"; // 默认处理中
        this.isDeleted = 0; // 默认未删除
    }
    
    // 兼容性构造函数
    public Document(Long knowledgeBaseId, String originalFilename, String storagePath, 
                   Long fileSize, String fileType, Long uploaderId) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.name = originalFilename;
        this.filePath = storagePath;
        this.status = "processing";
        this.isDeleted = 0;
    }

    // 基础字段的Getter和Setter方法
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public DocumentStatus getStatusEnum() {
        return DocumentStatus.fromValue(status);
    }

    public void setStatusEnum(DocumentStatus status) {
        this.status = status.getValue();
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    // 兼容性方法 - 为了保持API响应格式不变
    public String getOriginalFilename() {
        return this.name;
    }

    public void setOriginalFilename(String originalFilename) {
        this.name = originalFilename;
    }

    public String getStoragePath() {
        return this.filePath;
    }

    public void setStoragePath(String storagePath) {
        this.filePath = storagePath;
    }

    public String getFileType() {
        if (this.name != null && this.name.contains(".")) {
            return this.name.substring(this.name.lastIndexOf(".") + 1).toLowerCase();
        }
        return "unknown";
    }

    public void setFileType(String fileType) {
        // 不需要实际设置，因为从文件名推导
    }

    public Long getFileSize() {
        // 从文件系统获取实际文件大小
        if (fileSize != null) {
            return fileSize;
        }
        return 0L;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getStatusCode() {
        return getStatusEnum().getCode();
    }

    public Long getUploaderId() {
        // 暂时返回1，实际应该从其他表关联获取
        return 1L;
    }

    public void setUploaderId(Long uploaderId) {
        // 暂时不处理
    }

    public LocalDateTime getUploadTime() {
        return this.createTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.createTime = uploadTime;
    }

    public LocalDateTime getProcessStartTime() {
        return this.createTime;
    }

    public void setProcessStartTime(LocalDateTime processStartTime) {
        // 暂时不处理
    }

    public LocalDateTime getProcessEndTime() {
        return this.updateTime;
    }

    public void setProcessEndTime(LocalDateTime processEndTime) {
        // 暂时不处理
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", knowledgeBaseId=" + knowledgeBaseId +
                ", name='" + name + '\'' +
                ", filePath='" + filePath + '\'' +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", isDeleted=" + isDeleted +
                '}';
    }
} 