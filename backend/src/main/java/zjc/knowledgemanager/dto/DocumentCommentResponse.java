package zjc.knowledgemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档评论响应DTO
 */
public class DocumentCommentResponse {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime commentTime;
    
    private String content;
    
    private Long parentId;
    
    private UserInfo user;
    
    private List<DocumentCommentResponse> replies; // 回复列表
    
    public DocumentCommentResponse() {}
    
    public DocumentCommentResponse(Long id, LocalDateTime commentTime, String content, Long parentId, UserInfo user) {
        this.id = id;
        this.commentTime = commentTime;
        this.content = content;
        this.parentId = parentId;
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getCommentTime() {
        return commentTime;
    }
    
    public void setCommentTime(LocalDateTime commentTime) {
        this.commentTime = commentTime;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public UserInfo getUser() {
        return user;
    }
    
    public void setUser(UserInfo user) {
        this.user = user;
    }
    
    public List<DocumentCommentResponse> getReplies() {
        return replies;
    }
    
    public void setReplies(List<DocumentCommentResponse> replies) {
        this.replies = replies;
    }
    
    /**
     * 用户信息
     */
    public static class UserInfo {
        
        private Long id;
        
        private String username;
        
        private String realName;
        
        private String avatar;
        
        public UserInfo() {}
        
        public UserInfo(Long id, String username, String realName, String avatar) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.avatar = avatar;
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
        
        public String getAvatar() {
            return avatar;
        }
        
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        @Override
        public String toString() {
            return "UserInfo{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", realName='" + realName + '\'' +
                    ", avatar='" + avatar + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "DocumentCommentResponse{" +
                "id=" + id +
                ", commentTime=" + commentTime +
                ", content='" + content + '\'' +
                ", parentId=" + parentId +
                ", user=" + user +
                ", replies=" + replies +
                '}';
    }
}

