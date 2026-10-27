package zjc.knowledgemanager.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话详情响应DTO
 * 包含对话基本信息和消息列表
 */
public class ConversationDetailResponse {

    private String chatId;
    private String title;
    private int messageCount;
    private List<MessageDTO> messages;

    public ConversationDetailResponse() {}

    public ConversationDetailResponse(String chatId, String title, int messageCount,
                                      List<MessageDTO> messages) {
        this.chatId = chatId;
        this.title = title;
        this.messageCount = messageCount;
        this.messages = messages;
    }

    // Getter and Setter
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public List<MessageDTO> getMessages() { return messages; }
    public void setMessages(List<MessageDTO> messages) { this.messages = messages; }

    /**
     * 消息DTO内部类
     */
    public static class MessageDTO {
        private String role;
        private String content;
        private String model;
        private LocalDateTime createdAt;

        public MessageDTO() {}

        public MessageDTO(String role, String content, String model, LocalDateTime createdAt) {
            this.role = role;
            this.content = content;
            this.model = model;
            this.createdAt = createdAt;
        }

        // Getter and Setter
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        @Override
        public String toString() {
            return "MessageDTO{" +
                    "role='" + role + '\'' +
                    ", content='" + (content != null && content.length() > 30 ? content.substring(0, 30) + "..." : content) + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ConversationDetailResponse{" +
                "chatId='" + chatId + '\'' +
                ", messageCount=" + messageCount +
                '}';
    }
}
