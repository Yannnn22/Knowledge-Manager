package zjc.knowledgemanager.middleware;

/**
 * 跨 Advisor 透传运行时上下文（ThreadLocal）。
 *
 * <p>PromptPipeline 在每次调用前 set，调用后 clear。
 * 各 Advisor 通过静态方法读取，无需显式依赖注入。
 */
public final class PromptContextHolder {

    private PromptContextHolder() {}

    private static final ThreadLocal<String> CONVERSATION_ID = new ThreadLocal<>();

    public static void setConversationId(String id) {
        CONVERSATION_ID.set(id);
    }

    public static String getConversationId() {
        return CONVERSATION_ID.get();
    }

    public static void clear() {
        CONVERSATION_ID.remove();
    }
}
