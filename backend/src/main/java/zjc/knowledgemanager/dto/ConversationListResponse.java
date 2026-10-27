package zjc.knowledgemanager.dto;

import java.time.LocalDateTime;

/**
 * 对话列表项响应DTO
 * 用于对话列表页展示
 */
public class ConversationListResponse {

    private String chatId;
    private String title;
    private int messageCount;
    private String lastMessage;
    private LocalDateTime updatedAt;

    public ConversationListResponse() {}

    public ConversationListResponse(String chatId, String title, int messageCount,
                                    String lastMessage, LocalDateTime updatedAt) {
        this.chatId = chatId;
        this.title = title;
        this.messageCount = messageCount;
        this.lastMessage = lastMessage;
        this.updatedAt = updatedAt;
    }

    // Getter and Setter
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ConversationListResponse{" +
                "chatId='" + chatId + '\'' +
                ", title='" + title + '\'' +
                ", messageCount=" + messageCount +
                '}';
    }
}
