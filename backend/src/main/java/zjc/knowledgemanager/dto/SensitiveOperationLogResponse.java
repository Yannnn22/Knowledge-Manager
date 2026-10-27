package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 敏感操作日志响应DTO
 */
public class SensitiveOperationLogResponse {
    
    private List<SensitiveOperationLogItem> logs;
    
    public SensitiveOperationLogResponse() {}
    
    public SensitiveOperationLogResponse(List<SensitiveOperationLogItem> logs) {
        this.logs = logs;
    }
    
    // Getters and Setters
    public List<SensitiveOperationLogItem> getLogs() {
        return logs;
    }
    
    public void setLogs(List<SensitiveOperationLogItem> logs) {
        this.logs = logs;
    }
    
    /**
     * 敏感操作日志项
     */
    public static class SensitiveOperationLogItem {
        
        private Long id;
        
        private String operation;
        
        private Long targetId;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime operationTime;
        
        private String ipAddress;
        
        public SensitiveOperationLogItem() {}
        
        public SensitiveOperationLogItem(Long id, String operation, Long targetId, LocalDateTime operationTime, String ipAddress) {
            this.id = id;
            this.operation = operation;
            this.targetId = targetId;
            this.operationTime = operationTime;
            this.ipAddress = ipAddress;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getOperation() {
            return operation;
        }
        
        public void setOperation(String operation) {
            this.operation = operation;
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
        
        @Override
        public String toString() {
            return "SensitiveOperationLogItem{" +
                    "id=" + id +
                    ", operation='" + operation + '\'' +
                    ", targetId=" + targetId +
                    ", operationTime=" + operationTime +
                    ", ipAddress='" + ipAddress + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "SensitiveOperationLogResponse{" +
                "logs=" + logs +
                '}';
    }
}
