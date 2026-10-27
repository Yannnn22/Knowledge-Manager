package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户个人资料DTO
 */
public class UserProfileDTO {
    
    private Long userId;
    
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;
    
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String department;
    
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatarUrl;
    
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;
    
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;
    
    @Size(max = 100, message = "所在地长度不能超过100个字符")
    private String location;
    
    @Size(max = 200, message = "个人网站长度不能超过200个字符")
    private String website;
    
    @Size(max = 50, message = "模型名称长度不能超过50个字符")
    private String preferredModel;
    
    @Size(max = 200, message = "API Key长度不能超过200个字符")
    private String customApiKey;
    
    private Boolean notificationEmail;
    private Boolean notificationSystem;
    private Boolean isAdmin;

    // 构造函数
    public UserProfileDTO() {}

    public UserProfileDTO(Long userId, String realName, String email, String department) {
        this.userId = userId;
        this.realName = realName;
        this.email = email;
        this.department = department;
        this.isAdmin = false; // 默认非管理员
    }

    // Getter和Setter方法
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPreferredModel() {
        return preferredModel;
    }

    public void setPreferredModel(String preferredModel) {
        this.preferredModel = preferredModel;
    }

    public String getCustomApiKey() {
        return customApiKey;
    }

    public void setCustomApiKey(String customApiKey) {
        this.customApiKey = customApiKey;
    }

    public Boolean getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(Boolean notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public Boolean getNotificationSystem() {
        return notificationSystem;
    }

    public void setNotificationSystem(Boolean notificationSystem) {
        this.notificationSystem = notificationSystem;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    @Override
    public String toString() {
        return "UserProfileDTO{" +
                "userId=" + userId +
                ", realName='" + realName + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", phone='" + phone + '\'' +
                ", bio='" + bio + '\'' +
                ", location='" + location + '\'' +
                ", website='" + website + '\'' +
                ", preferredModel='" + preferredModel + '\'' +
                ", customApiKey='" + (customApiKey != null ? "******" : "null") + '\'' +
                ", notificationEmail=" + notificationEmail +
                ", notificationSystem=" + notificationSystem +
                ", isAdmin=" + isAdmin +
                '}';
    }
}