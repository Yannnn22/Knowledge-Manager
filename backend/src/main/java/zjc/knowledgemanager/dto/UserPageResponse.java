package zjc.knowledgemanager.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户分页响应DTO
 */
public class UserPageResponse {
    
    private Long total;
    private List<UserInfo> users;

    public UserPageResponse() {}

    public UserPageResponse(Long total, List<UserInfo> users) {
        this.total = total;
        this.users = users;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<UserInfo> getUsers() {
        return users;
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
    }

    /**
     * 用户信息DTO
     */
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String department;
        private Integer status;
        private LocalDateTime createTime;

        public UserInfo() {}

        public UserInfo(Long id, String username, String email, String department, Integer status, LocalDateTime createTime) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.department = department;
            this.status = status;
            this.createTime = createTime;
        }

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }
} 