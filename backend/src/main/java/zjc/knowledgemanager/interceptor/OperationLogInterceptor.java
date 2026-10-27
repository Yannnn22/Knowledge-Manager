package zjc.knowledgemanager.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import zjc.knowledgemanager.service.OperationLogService;
import zjc.knowledgemanager.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志拦截器
 * 自动记录用户的操作行为
 */
@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            // 只记录成功的操作（状态码2xx）
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                recordOperation(request, response);
            }
        } catch (Exception e) {
            // 记录日志失败不应该影响正常业务流程
            System.err.println("记录操作日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录操作日志
     */
    private void recordOperation(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // 过滤不需要记录的请求
        if (shouldSkipLogging(uri, method)) {
            return;
        }
        
        // 获取用户信息
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return;
        }
        
        // 获取客户端IP
        String ipAddress = getClientIpAddress(request);
        
        // 解析操作类型和目标信息
        OperationInfo operationInfo = parseOperation(uri, method);
        
        // 构建操作详情
        Map<String, Object> detail = buildOperationDetail(request, response, operationInfo);
        
        // 记录操作日志
        operationLogService.recordOperation(
            userId,
            operationInfo.operation,
            operationInfo.targetType,
            operationInfo.targetId,
            ipAddress,
            convertToJsonString(detail)
        );
    }
    
    /**
     * 判断是否需要跳过日志记录
     */
    private boolean shouldSkipLogging(String uri, String method) {
        // 跳过查询类操作（GET请求）
        if ("GET".equals(method)) {
            return true;
        }
        
        // 跳过登录接口
        if (uri.contains("/auth/login")) {
            return true;
        }
        
        // 跳过操作日志相关接口
        if (uri.contains("/operation-logs")) {
            return true;
        }
        
        // 跳过静态资源
        if (uri.contains("/static/") || uri.contains("/css/") || uri.contains("/js/") || uri.contains("/images/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return userId;
                }
            }
        } catch (Exception e) {
            // JWT解析失败，记录错误但继续执行
            System.err.println("JWT解析失败: " + e.getMessage());
        }
        
        // 如果无法获取用户ID，尝试从请求属性中获取（JWT拦截器可能已经设置）
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return Long.valueOf(userIdAttr.toString());
        }
        
        return null;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 解析操作类型和目标信息
     */
    private OperationInfo parseOperation(String uri, String method) {
        OperationInfo info = new OperationInfo();
        
        // 用户管理操作
        if (uri.contains("/users")) {
            info.targetType = "user";
            if ("POST".equals(method)) {
                info.operation = "CREATE_USER";
            } else if ("PUT".equals(method)) {
                info.operation = "UPDATE_USER";
                info.targetId = extractIdFromUri(uri);
            } else if ("DELETE".equals(method)) {
                info.operation = "DELETE_USER";
                info.targetId = extractIdFromUri(uri);
            }
        }
        // 知识库管理操作
        else if (uri.contains("/knowledge-bases")) {
            info.targetType = "knowledge_base";
            if ("POST".equals(method)) {
                info.operation = "CREATE_KNOWLEDGE_BASE";
            } else if ("PUT".equals(method)) {
                info.operation = "UPDATE_KNOWLEDGE_BASE";
                info.targetId = extractIdFromUri(uri);
            } else if ("DELETE".equals(method)) {
                info.operation = "DELETE_KNOWLEDGE_BASE";
                info.targetId = extractIdFromUri(uri);
            }
        }
        // 文档管理操作
        else if (uri.contains("/documents")) {
            info.targetType = "document";
            if ("POST".equals(method)) {
                if (uri.contains("/upload")) {
                    info.operation = "UPLOAD_DOCUMENT";
                } else if (uri.contains("/search")) {
                    info.operation = "SEARCH_DOCUMENT";
                }
            } else if ("PUT".equals(method)) {
                if (uri.contains("/archive")) {
                    info.operation = "ARCHIVE_DOCUMENT";
                    info.targetId = extractIdFromUri(uri);
                } else {
                    info.operation = "UPDATE_DOCUMENT";
                    info.targetId = extractIdFromUri(uri);
                }
            } else if ("DELETE".equals(method)) {
                info.operation = "DELETE_DOCUMENT";
                info.targetId = extractIdFromUri(uri);
            }
        }
        // 默认操作
        else {
            info.targetType = "system";
            info.operation = method + "_" + uri.replaceAll("/", "_").toUpperCase();
            info.targetId = 0L;
        }
        
        return info;
    }
    
    /**
     * 从URI中提取ID
     */
    private Long extractIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (parts[i].matches("\\d+")) {
                    return Long.parseLong(parts[i]);
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return 0L;
    }
    
    /**
     * 构建操作详情
     */
    private Map<String, Object> buildOperationDetail(HttpServletRequest request, HttpServletResponse response, OperationInfo operationInfo) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("method", request.getMethod());
        detail.put("uri", request.getRequestURI());
        detail.put("userAgent", request.getHeader("User-Agent"));
        detail.put("timestamp", LocalDateTime.now().toString());
        detail.put("responseStatus", response.getStatus());
        
        // 添加查询参数
        if (request.getQueryString() != null) {
            detail.put("queryString", request.getQueryString());
        }
        
        return detail;
    }
    
    /**
     * 转换为JSON字符串
     */
    private String convertToJsonString(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{\"error\":\"Failed to serialize detail\"}";
        }
    }
    
    /**
     * 操作信息内部类
     */
    private static class OperationInfo {
        String operation;
        String targetType;
        Long targetId = 0L;
    }
}