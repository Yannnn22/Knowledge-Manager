package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 统一错误响应格式
 * 用于标准化API错误返回结构
 */
public class ErrorResponse {
    
    // 错误码
    private int code;
    
    // 错误消息
    private String message;
    
    // 详细错误信息（可选）
    private String details;
    
    // 请求路径
    private String path;
    
    // 错误发生时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 构造函数
     */
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     * @param path 请求路径
     */
    public ErrorResponse(int code, String message, String path) {
        this();
        this.code = code;
        this.message = message;
        this.path = path;
    }

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     * @param details 详细错误信息
     * @param path 请求路径
     */
    public ErrorResponse(int code, String message, String details, String path) {
        this(code, message, path);
        this.details = details;
    }

    // Getter和Setter方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", details='" + details + '\'' +
                ", path='" + path + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
