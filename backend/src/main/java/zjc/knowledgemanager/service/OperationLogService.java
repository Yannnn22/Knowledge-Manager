package zjc.knowledgemanager.service;

import zjc.knowledgemanager.dto.OperationLogListResponse;
import zjc.knowledgemanager.dto.OperationLogQueryRequest;
import zjc.knowledgemanager.dto.OperationLogStatsDTO;
import zjc.knowledgemanager.dto.PageResult;
import zjc.knowledgemanager.dto.SensitiveOperationLogResponse;
import zjc.knowledgemanager.entity.OperationLog;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {
    
    /**
     * 记录操作日志
     */
    void recordOperation(Long userId, String operation, String targetType, Long targetId, String ipAddress, String detail);
    
    /**
     * 分页查询操作日志
     */
    OperationLogListResponse queryOperationLogs(OperationLogQueryRequest request);
    
    /**
     * 分页查询操作日志（支持筛选）
     */
    PageResult<OperationLog> getOperationLogsWithFilter(
            Integer page, Integer size, Long userId, String username,
            String operation, String targetType, Long targetId,
            Long knowledgeBaseId, String startTime, String endTime);
    
    /**
     * 获取操作日志统计信息
     */
    OperationLogStatsDTO getOperationLogStatistics();
    
    /**
     * 查询敏感操作日志
     */
    SensitiveOperationLogResponse querySensitiveOperations();
    
    /**
     * 导出操作日志为CSV文件
     */
    void exportOperationLogs(LocalDateTime startTime, LocalDateTime endTime, HttpServletResponse response);
}
