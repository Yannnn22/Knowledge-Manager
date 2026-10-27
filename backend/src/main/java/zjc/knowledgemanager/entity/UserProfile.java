package zjc.knowledgemanager.entity;

import java.time.LocalDateTime;

/**
 * 用户个人资料实体类
 */
public class UserProfile {
    
    private Long id;
    private Long userId;
    private String avatarUrl;
    private String phone;
    private String bio;
    private String location;
    private String website;
    private String preferredModel;
    private String customApiKey;
    private Integer notificationEmail;
    private Integer notificationSystem;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 构造函数
    public UserProfile() {}

    public UserProfile(Long userId) {
        this.userId = userId;
        this.notificationEmail = 1; // 默认开启邮件通知
        this.notificationSystem = 1; // 默认开启系统通知
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Integer getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(Integer notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public Integer getNotificationSystem() {
        return notificationSystem;
    }

    public void setNotificationSystem(Integer notificationSystem) {
        this.notificationSystem = notificationSystem;
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

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", userId=" + userId +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", phone='" + phone + '\'' +
                ", bio='" + bio + '\'' +
                ", location='" + location + '\'' +
                ", website='" + website + '\'' +
                ", preferredModel='" + preferredModel + '\'' +
                ", customApiKey='" + (customApiKey != null ? "******" : "null") + '\'' +
                ", notificationEmail=" + notificationEmail +
                ", notificationSystem=" + notificationSystem +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}