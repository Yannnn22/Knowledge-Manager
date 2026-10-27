package zjc.knowledgemanager.dto;

/**
 * 登录响应DTO
 */
public class LoginResponse {
    
    private String token;
    private Long userId;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId) {
        this.token = token;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 