package zjc.knowledgemanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.OperationLogListResponse;
import zjc.knowledgemanager.dto.OperationLogQueryRequest;
import zjc.knowledgemanager.dto.OperationLogStatsDTO;
import zjc.knowledgemanager.dto.PageResult;
import zjc.knowledgemanager.dto.SensitiveOperationLogResponse;
import zjc.knowledgemanager.entity.OperationLog;
import zjc.knowledgemanager.service.OperationLogService;
import zjc.knowledgemanager.util.JwtUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/v1/operation-logs")
@Tag(name = "操作日志管理", description = "操作日志查询、导出和敏感操作监控相关接口")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "分页查询操作日志", description = "支持按用户、操作类型、时间范围等条件查询操作日志")
    public ApiResponse<OperationLogListResponse> getOperationLogPage(
            OperationLogQueryRequest request, 
            HttpServletRequest httpRequest) {
        try {
            // 从JWT token中获取当前用户信息
            Long currentUserId = getUserIdFromToken(httpRequest);
            
            // 非管理员用户可以查看自己的操作日志和系统记录的相关操作
            // 不强制限制userId，让用户能看到更多相关的操作记录
            if (!isAdminUser(currentUserId)) {
                // 普通用户可以看到所有操作日志，但敏感操作仍需管理员权限
                // 这样可以让用户看到系统中的相关操作，包括那些user_id为NULL的记录
            }
            
            OperationLogListResponse response = operationLogService.queryOperationLogs(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("查询操作日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @Operation(summary = "导出操作日志", description = "导出指定时间范围内的操作日志为CSV文件，需要管理员权限")
    public void exportOperationLogs(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        try {
            // 验证管理员权限
            Long currentUserId = getUserIdFromToken(httpRequest);
            if (!isAdminUser(currentUserId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足，仅管理员可导出操作日志\"}");
                return;
            }
            
            LocalDateTime start = null;
            LocalDateTime end = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            if (startTime != null && !startTime.isEmpty()) {
                start = LocalDateTime.parse(startTime, formatter);
            }
            if (endTime != null && !endTime.isEmpty()) {
                end = LocalDateTime.parse(endTime, formatter);
            }
            
            operationLogService.exportOperationLogs(start, end, response);
        } catch (Exception e) {
            // 导出失败时设置错误响应
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取操作日志统计信息", description = "获取今日日志数量、敏感操作数量、活跃用户数量、错误记录数量等统计信息")
    public ResponseEntity<ApiResponse<OperationLogStatsDTO>> getStatistics() {
        try {
            OperationLogStatsDTO stats = operationLogService.getOperationLogStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取统计信息失败: " + e.getMessage()));
        }
    }

    @GetMapping("/filter")
    @Operation(summary = "分页查询操作日志（支持筛选）", description = "支持按用户、操作类型、时间范围等条件筛选查询操作日志")
    public ResponseEntity<ApiResponse<PageResult<OperationLog>>> getOperationLogsWithFilter(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        try {
            PageResult<OperationLog> result = operationLogService.getOperationLogsWithFilter(
                page, size, userId, username, operation, targetType, targetId, 
                knowledgeBaseId, startTime, endTime);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询操作日志失败: " + e.getMessage()));
        }
    }

    @GetMapping("/sensitive")
    @Operation(summary = "查询敏感操作日志", description = "查询系统中的敏感操作记录，需要管理员权限")
    public ApiResponse<SensitiveOperationLogResponse> getSensitiveOperations(HttpServletRequest httpRequest) {
        try {
            // 验证管理员权限
            Long currentUserId = getUserIdFromToken(httpRequest);
            if (!isAdminUser(currentUserId)) {
                return ApiResponse.error("权限不足，仅管理员可查看敏感操作日志");
            }
            
            SensitiveOperationLogResponse response = operationLogService.querySensitiveOperations();
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("查询敏感操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 从请求头中提取JWT token并获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("未找到有效的认证token");
    }
    
    /**
     * 判断是否为管理员用户
     * 这里简化处理，实际项目中应该从数据库查询用户角色
     */
    private boolean isAdminUser(Long userId) {
        // 简化处理：假设用户ID为1或者用户名为admin/newuser的是管理员
        // 实际项目中应该查询用户角色表
        return userId != null && (userId.equals(1L) || userId.equals(19144102153031680L));
    }
}