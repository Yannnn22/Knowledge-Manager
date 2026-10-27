package zjc.knowledgemanager.dto;

/**
 * 对话统计响应DTO
 */
public class ConversationStatsResponse {

    private int totalConversations;
    private int totalMessages;

    public ConversationStatsResponse() {}

    public ConversationStatsResponse(int totalConversations, int totalMessages) {
        this.totalConversations = totalConversations;
        this.totalMessages = totalMessages;
    }

    // Getter and Setter
    public int getTotalConversations() { return totalConversations; }
    public void setTotalConversations(int totalConversations) { this.totalConversations = totalConversations; }

    public int getTotalMessages() { return totalMessages; }
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }

    @Override
    public String toString() {
        return "ConversationStatsResponse{" +
                "totalConversations=" + totalConversations +
                ", totalMessages=" + totalMessages +
                '}';
    }
}
