package zjc.knowledgemanager.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
public class OperationLog {
    
    private Long id;
    
    @JsonProperty("user_id")
    private Long userId;
    
    private String operation;
    
    @JsonProperty("target_type")
    private String targetType;
    
    @JsonProperty("target_id")
    private Long targetId;
    
    @JsonProperty("operation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    private String detail;
    
    // 关联查询字段
    private User user;
    
    public OperationLog() {}
    
    public OperationLog(Long userId, String operation, String targetType, Long targetId, String ipAddress, String detail) {
        this.userId = userId;
        this.operation = operation;
        this.targetType = targetType;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
        this.detail = detail;
    }
    
    // Getters and Setters
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
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
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
    
    public LocalDateTime getOperationTime() {
        return operationTime;
    }
    
    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getDetail() {
        return detail;
    }
    
    public void setDetail(String detail) {
        this.detail = detail;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    @Override
    public String toString() {
        return "OperationLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", operation='" + operation + '\'' +
                ", targetType='" + targetType + '\'' +
                ", targetId=" + targetId +
                ", operationTime=" + operationTime +
                ", ipAddress='" + ipAddress + '\'' +
                ", detail='" + detail + '\'' +
                ", user=" + user +
                '}';
    }
}
