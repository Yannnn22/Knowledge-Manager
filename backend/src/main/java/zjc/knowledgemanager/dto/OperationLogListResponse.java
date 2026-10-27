package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志列表响应DTO
 */
public class OperationLogListResponse {
    
    private Long total;
    
    private List<OperationLogItem> logs;
    
    public OperationLogListResponse() {}
    
    public OperationLogListResponse(Long total, List<OperationLogItem> logs) {
        this.total = total;
        this.logs = logs;
    }
    
    // Getters and Setters
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
    public List<OperationLogItem> getLogs() {
        return logs;
    }
    
    public void setLogs(List<OperationLogItem> logs) {
        this.logs = logs;
    }
    
    /**
     * 操作日志项
     */
    public static class OperationLogItem {
        
        private Long id;
        
        private String operation;
        
        private String targetType;
        
        private Long targetId;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime operationTime;
        
        private UserInfo user;
        
        public OperationLogItem() {}
        
        public OperationLogItem(Long id, String operation, String targetType, Long targetId, LocalDateTime operationTime, UserInfo user) {
            this.id = id;
            this.operation = operation;
            this.targetType = targetType;
            this.targetId = targetId;
            this.operationTime = operationTime;
            this.user = user;
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
        
        public UserInfo getUser() {
            return user;
        }
        
        public void setUser(UserInfo user) {
            this.user = user;
        }
        
        @Override
        public String toString() {
            return "OperationLogItem{" +
                    "id=" + id +
                    ", operation='" + operation + '\'' +
                    ", targetType='" + targetType + '\'' +
                    ", targetId=" + targetId +
                    ", operationTime=" + operationTime +
                    ", user=" + user +
                    '}';
        }
    }
    
    /**
     * 用户信息
     */
    public static class UserInfo {
        
        private Long id;
        
        private String username;
        
        private String realName;
        
        private String department;
        
        public UserInfo() {}
        
        public UserInfo(Long id, String username, String realName, String department) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.department = department;
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
        
        public String getDepartment() {
            return department;
        }
        
        public void setDepartment(String department) {
            this.department = department;
        }
        
        @Override
        public String toString() {
            return "UserInfo{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", realName='" + realName + '\'' +
                    ", department='" + department + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "OperationLogListResponse{" +
                "total=" + total +
                ", logs=" + logs +
                '}';
    }
}
