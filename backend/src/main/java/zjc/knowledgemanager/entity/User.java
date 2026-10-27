package zjc.knowledgemanager.entity;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
public class User {
    
    // 状态常量
    public static final int STATUS_DISABLED = 0;   // 禁用
    public static final int STATUS_NORMAL   = 1;   // 普通用户
    public static final int STATUS_ADMIN    = 2;   // 管理员
    public static final int STATUS_LEFT     = 3;   // 离职
    
    private Long id;
    private String username;
    private String password;
    private String email;
    private String realName;
    private String department;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    // 构造函数
    public User() {}

    public User(String username, String password, String email, String realName, String department) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.realName = realName;
        this.department = department;
        this.status = STATUS_NORMAL; // 默认启用为普通用户
        this.isDeleted = 0; // 默认未删除
    }

    // Getter和Setter方法
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    // 管理员判断
    public boolean isAdmin() {
        // 修复：通过比较原始int值来避免Integer.equals(int)的潜在问题
        return status != null && status.intValue() == STATUS_ADMIN;
    }

    // 启用状态判断
    public boolean isEnabled() {
        return status != null && status != STATUS_DISABLED && status != STATUS_LEFT;
    }

    // 普通用户判断
    public boolean isNormalUser() {
        return status != null && status.equals(STATUS_NORMAL);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", realName='" + realName + '\'' +
                ", department='" + department + '\'' +
                ", avatar='" + avatar + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", isDeleted=" + isDeleted +
                '}';
    }
} 