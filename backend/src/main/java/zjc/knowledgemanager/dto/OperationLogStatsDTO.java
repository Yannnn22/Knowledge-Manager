package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 操作日志统计响应DTO
 */
public class OperationLogStatsDTO {
    
    @JsonProperty("today_logs")
    private Long todayLogs;        // 今日日志数量
    
    @JsonProperty("sensitive_operations")
    private Long sensitiveOperations; // 敏感操作数量
    
    @JsonProperty("active_users")
    private Long activeUsers;      // 活跃用户数量
    
    @JsonProperty("error_records")
    private Long errorRecords;     // 错误记录数量
    
    public OperationLogStatsDTO() {}
    
    public OperationLogStatsDTO(Long todayLogs, Long sensitiveOperations, Long activeUsers, Long errorRecords) {
        this.todayLogs = todayLogs;
        this.sensitiveOperations = sensitiveOperations;
        this.activeUsers = activeUsers;
        this.errorRecords = errorRecords;
    }
    
    // Getters and Setters
    public Long getTodayLogs() {
        return todayLogs;
    }
    
    public void setTodayLogs(Long todayLogs) {
        this.todayLogs = todayLogs;
    }
    
    public Long getSensitiveOperations() {
        return sensitiveOperations;
    }
    
    public void setSensitiveOperations(Long sensitiveOperations) {
        this.sensitiveOperations = sensitiveOperations;
    }
    
    public Long getActiveUsers() {
        return activeUsers;
    }
    
    public void setActiveUsers(Long activeUsers) {
        this.activeUsers = activeUsers;
    }
    
    public Long getErrorRecords() {
        return errorRecords;
    }
    
    public void setErrorRecords(Long errorRecords) {
        this.errorRecords = errorRecords;
    }
    
    @Override
    public String toString() {
        return "OperationLogStatsDTO{" +
                "todayLogs=" + todayLogs +
                ", sensitiveOperations=" + sensitiveOperations +
                ", activeUsers=" + activeUsers +
                ", errorRecords=" + errorRecords +
                '}';
    }
}
