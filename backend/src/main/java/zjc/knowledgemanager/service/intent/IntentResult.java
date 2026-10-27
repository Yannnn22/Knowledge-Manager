package zjc.knowledgemanager.service.intent;

import java.util.HashMap;
import java.util.Map;

public class IntentResult {

    private final IntentType intent;
    private final String originalQuery;
    private final Map<String, String> parameters;
    private final boolean requiresTool;
    private final boolean requiresRag;

    private IntentResult(IntentType intent, String originalQuery) {
        this.intent = intent;
        this.originalQuery = originalQuery;
        this.parameters = new HashMap<>();
        this.requiresTool = intent != IntentType.GENERAL_QA && 
                           intent != IntentType.RAG_RETRIEVAL &&
                           intent != IntentType.UNKNOWN;
        this.requiresRag = intent == IntentType.RAG_RETRIEVAL || 
                          intent == IntentType.DOCUMENT_SEARCH ||
                          intent == IntentType.KNOWLEDGE_BASE_SEARCH;
    }

    public static IntentResult of(IntentType intent, String originalQuery) {
        return new IntentResult(intent, originalQuery);
    }

    public IntentType getIntent() {
        return intent;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public boolean isRequiresTool() {
        return requiresTool;
    }

    public boolean isRequiresRag() {
        return requiresRag;
    }

    public boolean isHighRisk() {
        return intent.isHighRisk();
    }

    public boolean isReadOnly() {
        return intent.isReadOnly();
    }
}