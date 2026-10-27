package zjc.knowledgemanager.entity;

import java.time.LocalDateTime;

/**
 * 消息实体类
 * 对应数据库表 message
 */
public class Message {

    // 消息角色常量
    public static final String ROLE_USER      = "user";
    public static final String ROLE_ASSISTANT  = "assistant";
    public static final String ROLE_SYSTEM     = "system";

    private Long id;
    private Long conversationId;
    private String chatId;
    private String role;
    private String content;
    private String model;
    private LocalDateTime createdAt;
    private Integer isDeleted;

    public Message() {}

    public Message(Long id, String chatId, String role, String content, String model) {
        this.id = id;
        this.chatId = chatId;
        this.role = role;
        this.content = content;
        this.model = model;
        this.isDeleted = 0;
    }

    // Getter and Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", chatId='" + chatId + '\'' +
                ", role='" + role + '\'' +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                '}';
    }
}
