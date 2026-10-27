package zjc.knowledgemanager.service.intent;

public enum IntentType {
    GENERAL_QA("general_qa", "普通问答", "用户询问知识、信息、建议等，不需要调用工具"),
    RAG_RETRIEVAL("rag_retrieval", "RAG检索", "用户询问已上传文档中的内容，需要进行向量检索"),
    DOCUMENT_SEARCH("document_search", "文档搜索", "用户想要搜索或查找文档"),
    DOCUMENT_VIEW("document_view", "文档查看", "用户想要查看某个文档的内容"),
    DOCUMENT_CREATE("document_create", "文档创建", "用户想要创建新文档"),
    DOCUMENT_EDIT("document_edit", "文档编辑", "用户想要编辑现有文档"),
    DOCUMENT_DELETE("document_delete", "文档删除", "用户想要删除文档"),
    DOCUMENT_LIST("document_list", "文档列表", "用户想要列出所有文档"),
    KNOWLEDGE_BASE_SEARCH("kb_search", "知识库搜索", "用户想要在知识库中搜索"),
    KNOWLEDGE_BASE_LIST("kb_list", "知识库列表", "用户想要列出所有知识库"),
    KNOWLEDGE_BASE_CREATE("kb_create", "知识库创建", "用户想要创建知识库"),
    KNOWLEDGE_BASE_DELETE("kb_delete", "知识库删除", "用户想要删除知识库"),
    NL2SQL_QUERY("nl2sql_query", "NL2SQL查询", "用户想要通过自然语言查询数据库"),
    UNKNOWN("unknown", "未知意图", "无法识别的意图");

    private final String code;
    private final String name;
    private final String description;

    IntentType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHighRisk() {
        return this == DOCUMENT_DELETE || this == DOCUMENT_EDIT || 
               this == KNOWLEDGE_BASE_DELETE || this == KNOWLEDGE_BASE_CREATE;
    }

    public boolean isReadOnly() {
        return this == GENERAL_QA || this == RAG_RETRIEVAL || 
               this == DOCUMENT_SEARCH || this == DOCUMENT_VIEW || 
               this == DOCUMENT_LIST || this == KNOWLEDGE_BASE_SEARCH || 
               this == KNOWLEDGE_BASE_LIST || this == NL2SQL_QUERY;
    }
}