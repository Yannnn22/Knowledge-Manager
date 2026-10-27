package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建对话请求DTO
 */
public class CreateConversationRequest {

    @NotBlank(message = "对话ID不能为空")
    private String chatId;

    public CreateConversationRequest() {}

    public CreateConversationRequest(String chatId) {
        this.chatId = chatId;
    }

    // Getter and Setter
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    @Override
    public String toString() {
        return "CreateConversationRequest{" +
                "chatId='" + chatId + '\'' +
                '}';
    }
}
