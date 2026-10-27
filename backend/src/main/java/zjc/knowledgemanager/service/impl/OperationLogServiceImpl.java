package zjc.knowledgemanager.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.dto.OperationLogListResponse;
import zjc.knowledgemanager.dto.OperationLogQueryRequest;
import zjc.knowledgemanager.dto.OperationLogStatsDTO;
import zjc.knowledgemanager.dto.PageResult;
import zjc.knowledgemanager.dto.SensitiveOperationLogResponse;
import zjc.knowledgemanager.entity.OperationLog;
import zjc.knowledgemanager.mapper.OperationLogMapper;
import zjc.knowledgemanager.service.OperationLogService;
import zjc.knowledgemanager.util.DateParserUtil;
import zjc.knowledgemanager.util.IdGenerator;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Autowired
    private IdGenerator idGenerator;
    
    @Override
    public void recordOperation(Long userId, String operation, String targetType, Long targetId, String ipAddress, String detail) {
        OperationLog operationLog = new OperationLog();
        operationLog.setId(idGenerator.nextId());
        operationLog.setUserId(userId);
        operationLog.setOperation(operation);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setOperationTime(LocalDateTime.now());
        operationLog.setIpAddress(ipAddress);
        operationLog.setDetail(detail);
        
        operationLogMapper.insert(operationLog);
    }
    
    @Override
    public OperationLogListResponse queryOperationLogs(OperationLogQueryRequest request) {
        // 设置分页
        PageHelper.startPage(request.getPage(), request.getSize());
        
        // 查询数据 - 使用新的带筛选条件的方法
        List<OperationLog> operationLogs = operationLogMapper.selectByPageWithFilters(
                request.getUserId(),
                request.getOperation() != null ? request.getOperation() : request.getOperationType(),
                request.getUsername(),
                request.getOperationType(),
                request.getStatus(),
                request.getTargetType(),
                request.getTargetId(),
                request.getKnowledgeBaseId(),
                request.getQuery(),
                request.getStartTime(),
                request.getEndTime()
        );
        
        // 获取分页信息
        PageInfo<OperationLog> pageInfo = new PageInfo<>(operationLogs);
        
        // 转换为响应DTO
        List<OperationLogListResponse.OperationLogItem> items = operationLogs.stream()
                .map(this::convertToOperationLogItem)
                .collect(Collectors.toList());
        
        return new OperationLogListResponse(pageInfo.getTotal(), items);
    }
    
    @Override
    public SensitiveOperationLogResponse querySensitiveOperations() {
        List<OperationLog> sensitiveLogs = operationLogMapper.selectSensitiveOperations();
        
        List<SensitiveOperationLogResponse.SensitiveOperationLogItem> items = sensitiveLogs.stream()
                .map(this::convertToSensitiveOperationLogItem)
                .collect(Collectors.toList());
        
        return new SensitiveOperationLogResponse(items);
    }
    
    @Override
    public PageResult<OperationLog> getOperationLogsWithFilter(
            Integer page, Integer size, Long userId, String username, 
            String operation, String targetType, Long targetId, 
            Long knowledgeBaseId, String startTime, String endTime) {
        
        // 设置分页
        PageHelper.startPage(page, size);
        
        // 构建查询条件
        List<OperationLog> operationLogs = operationLogMapper.selectByPageWithFilters(
                userId,
                operation,
                username,
                operation, // operationType is same as operation for this method
                null, // status
                targetType,
                targetId,
                knowledgeBaseId,
                null, // query
                startTime != null ? DateParserUtil.parseDateTime(startTime) : null,
                endTime != null ? DateParserUtil.parseDateTime(endTime) : null
        );
        
        // 获取分页信息
        PageInfo<OperationLog> pageInfo = new PageInfo<>(operationLogs);

        return PageResult.of(operationLogs, pageInfo.getTotal(), page, size);
    }
    
    @Override
    public OperationLogStatsDTO getOperationLogStatistics() {
        OperationLogStatsDTO stats = new OperationLogStatsDTO();

        // 获取今日的开始和结束时间
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 查询今日所有操作日志
        List<OperationLog> todayLogs = operationLogMapper.selectByTimeRange(startOfDay, endOfDay);

        // 1. 今日日志数量
        stats.setTodayLogs((long) todayLogs.size());

        // 2. 敏感操作数量
        // 根据实际数据表，定义敏感操作类型
        Set<String> sensitiveOperations = Set.of(
            "DELETE_USER",           // 删除用户
            "BULK_DELETE",          // 批量删除
            "DELETE_KNOWLEDGE_BASE", // 删除知识库
            "UPDATE_USER_STATUS",   // 更新用户状态（如禁用账号）
            "DELETE_DOCUMENT",      // 删除文档
            "ARCHIVE_DOCUMENT"      // 归档文档（可能涉及敏感数据）
        );
        
        long sensitiveCount = todayLogs.stream()
            .filter(log -> sensitiveOperations.contains(log.getOperation()))
            .count();
        stats.setSensitiveOperations(sensitiveCount);

        // 3. 活跃用户数量 (今日)
        long activeUsersCount = todayLogs.stream()
            .map(OperationLog::getUserId)
            .filter(userId -> userId != null)
            .distinct()
            .count();
        stats.setActiveUsers(activeUsersCount);

        // 4. 错误记录数量
        // 根据detail字段内容判断错误记录
        long errorRecordsCount = todayLogs.stream()
            .filter(log -> {
                if (log.getDetail() == null) return false;
                
                String detail = log.getDetail().toLowerCase();
                
                // 检查是否包含错误相关关键词
                return detail.contains("error") || 
                       detail.contains("失败") || 
                       detail.contains("exception") ||
                       detail.contains("failed") ||
                       // 检查用户状态被禁用的情况
                       (log.getOperation().equals("UPDATE_USER_STATUS") && 
                        detail.contains("\"newStatus\": 0")) ||
                       // 检查批量删除失败的情况
                       (log.getOperation().equals("BULK_DELETE") && 
                        detail.contains("失败"));
            })
            .count();
        stats.setErrorRecords(errorRecordsCount);

        return stats;
    }

    @Override
    public void exportOperationLogs(LocalDateTime startTime, LocalDateTime endTime, HttpServletResponse response) {
        List<OperationLog> operationLogs = operationLogMapper.selectForExport(startTime, endTime);
        
        // 设置响应头
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=operation_logs_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        
        try (PrintWriter writer = response.getWriter()) {
            // 写入CSV头部
            writer.println("日志ID,操作用户,操作类型,目标类型,目标ID,操作时间,IP地址,操作详情");
            
            // 写入数据
            for (OperationLog log : operationLogs) {
                String userInfo = log.getUser() != null ? 
                        log.getUser().getRealName() + "(" + log.getUser().getUsername() + ")" : 
                        "未知用户";
                
                writer.printf("\"%d\",\"%s\",\"%s\",\"%s\",\"%d\",\"%s\",\"%s\",\"%s\"%n",
                        log.getId(),
                        userInfo,
                        log.getOperation(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getOperationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        log.getIpAddress(),
                        log.getDetail() != null ? log.getDetail().replace("\"", "\"\"") : ""
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("导出操作日志失败", e);
        }
    }
    
    /**
     * 转换为操作日志项
     */
    private OperationLogListResponse.OperationLogItem convertToOperationLogItem(OperationLog operationLog) {
        OperationLogListResponse.UserInfo userInfo = null;
        if (operationLog.getUser() != null) {
            userInfo = new OperationLogListResponse.UserInfo(
                    operationLog.getUser().getId(),
                    operationLog.getUser().getUsername(),
                    operationLog.getUser().getRealName(),
                    operationLog.getUser().getDepartment()
            );
        }
        
        return new OperationLogListResponse.OperationLogItem(
                operationLog.getId(),
                operationLog.getOperation(),
                operationLog.getTargetType(),
                operationLog.getTargetId(),
                operationLog.getOperationTime(),
                userInfo
        );
    }
    
    /**
     * 转换为敏感操作日志项
     */
    private SensitiveOperationLogResponse.SensitiveOperationLogItem convertToSensitiveOperationLogItem(OperationLog operationLog) {
        return new SensitiveOperationLogResponse.SensitiveOperationLogItem(
                operationLog.getId(),
                operationLog.getOperation(),
                operationLog.getTargetId(),
                operationLog.getOperationTime(),
                operationLog.getIpAddress()
        );
    }
}
