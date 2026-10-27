package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * 操作日志查询请求DTO
 */
public class OperationLogQueryRequest {
    
    private Long userId;
    
    private String operation;
    
    // 新增字段支持前端筛选
    private String username;
    
    private String operationType;
    
    private String status;
    
    private Long knowledgeBaseId;
    
    private String query;

    private String targetType;

    private Long targetId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;
    
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer size = 10;
    
    public OperationLogQueryRequest() {}
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }
    
    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
    
    @Override
    public String toString() {
        return "OperationLogQueryRequest{" +
                "userId=" + userId +
                ", operation='" + operation + '\'' +
                ", username='" + username + '\'' +
                ", operationType='" + operationType + '\'' +
                ", status='" + status + '\'' +
                ", knowledgeBaseId=" + knowledgeBaseId +
                ", query='" + query + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}
