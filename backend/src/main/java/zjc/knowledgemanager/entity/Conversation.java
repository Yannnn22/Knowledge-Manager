package zjc.knowledgemanager.entity;

import java.time.LocalDateTime;

/**
 * 对话实体类
 * 对应数据库表 conversation
 */
public class Conversation {

    private Long id;
    private String chatId;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;

    public Conversation() {}

    public Conversation(Long id, String chatId, Long userId) {
        this.id = id;
        this.chatId = chatId;
        this.userId = userId;
        this.isDeleted = 0;
    }

    // Getter and Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", chatId='" + chatId + '\'' +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                '}';
    }
}
