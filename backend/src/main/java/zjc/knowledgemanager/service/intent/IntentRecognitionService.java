package zjc.knowledgemanager.service.intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionService.class);

    @Autowired
    private ChatClient chatClient;

    private static final Map<IntentType, List<String>> KEYWORD_MAP = new HashMap<>();
    private static final Map<IntentType, List<Pattern>> PATTERN_MAP = new HashMap<>();

    static {
        KEYWORD_MAP.put(IntentType.DOCUMENT_CREATE, Arrays.asList(
                "创建文档", "新建文档", "写文档", "保存文档", "生成文档",
                "创建一个文档", "新建一个文档", "帮我写一个文档"
        ));
        KEYWORD_MAP.put(IntentType.DOCUMENT_EDIT, Arrays.asList(
                "编辑文档", "修改文档", "更新文档", "更改文档"
        ));
        KEYWORD_MAP.put(IntentType.DOCUMENT_DELETE, Arrays.asList(
                "删除文档", "移除文档", "删掉文档"
        ));
        KEYWORD_MAP.put(IntentType.DOCUMENT_LIST, Arrays.asList(
                "列出文档", "文档列表", "所有文档", "我的文档", "有哪些文档"
        ));
        KEYWORD_MAP.put(IntentType.DOCUMENT_SEARCH, Arrays.asList(
                "搜索文档", "查找文档", "检索文档", "找文档", "搜索一下文档",
                "帮我找个文档", "帮我搜索文档", "查找一下文档",
                "找个关于", "找一下", "有没有.*文档", "帮我找",
                "关于.*的文档", "知识库里有什么", "知识库里的文档",
                "有什么文档", "什么文档", "文档有哪些"
        ));
        KEYWORD_MAP.put(IntentType.DOCUMENT_VIEW, Arrays.asList(
                "查看文档", "获取文档", "文档内容", "打开文档", "阅读文档",
                "查看文档内容", "获取文档内容"
        ));
        KEYWORD_MAP.put(IntentType.KNOWLEDGE_BASE_SEARCH, Arrays.asList(
                "在知识库中搜索", "知识库搜索", "搜索知识库", "知识库中查找",
                "在知识库中查找", "搜索一下知识库"
        ));
        KEYWORD_MAP.put(IntentType.KNOWLEDGE_BASE_LIST, Arrays.asList(
                "知识库列表", "所有知识库", "有哪些知识库", "列出知识库"
        ));
        KEYWORD_MAP.put(IntentType.KNOWLEDGE_BASE_CREATE, Arrays.asList(
                "创建知识库", "新建知识库", "添加知识库"
        ));
        KEYWORD_MAP.put(IntentType.KNOWLEDGE_BASE_DELETE, Arrays.asList(
                "删除知识库", "移除知识库"
        ));
        KEYWORD_MAP.put(IntentType.RAG_RETRIEVAL, Arrays.asList(
                "根据文档", "根据上传的文档", "文档中说", "文档里的内容",
                "请参考文档", "基于文档", "文档中的信息", "上传的文件"
        ));
        KEYWORD_MAP.put(IntentType.NL2SQL_QUERY, Arrays.asList(
                "查询", "统计", "有多少", "数量", "多少个", "总计",
                "汇总", "数据", "信息", "记录", "列表", "排行"
        ));
    }

    static {
        PATTERN_MAP.put(IntentType.DOCUMENT_VIEW, Arrays.asList(
                Pattern.compile("查看文档\\s*(\\d+)"),
                Pattern.compile("文档\\s*(\\d+)\\s*的内容"),
                Pattern.compile("文档ID\\s*(\\d+)"),
                Pattern.compile("文档\\s*(\\d+)")
        ));
        PATTERN_MAP.put(IntentType.DOCUMENT_EDIT, Arrays.asList(
                Pattern.compile("编辑文档\\s*(\\d+)"),
                Pattern.compile("修改文档\\s*(\\d+)")
        ));
        PATTERN_MAP.put(IntentType.DOCUMENT_DELETE, Arrays.asList(
                Pattern.compile("删除文档\\s*(\\d+)"),
                Pattern.compile("移除文档\\s*(\\d+)")
        ));
    }

    public IntentResult recognize(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return IntentResult.of(IntentType.UNKNOWN, userQuery);
        }

        IntentType intent = ruleBasedRecognition(userQuery);
        
        if (intent != IntentType.UNKNOWN) {
            log.debug("规则匹配意图: {} - {}", intent.getCode(), userQuery);
            return IntentResult.of(intent, userQuery);
        }

        intent = llmBasedRecognition(userQuery);
        log.debug("LLM识别意图: {} - {}", intent.getCode(), userQuery);
        return IntentResult.of(intent, userQuery);
    }

    private IntentType ruleBasedRecognition(String userQuery) {
        String lowerQuery = userQuery.toLowerCase().trim();
        String originalQuery = userQuery.trim();

        for (Map.Entry<IntentType, List<String>> entry : KEYWORD_MAP.entrySet()) {
            IntentType intent = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (keyword.contains(".*") || keyword.contains(".")) {
                    try {
                        Pattern pattern = Pattern.compile(keyword);
                        Matcher matcher = pattern.matcher(originalQuery);
                        if (matcher.find()) {
                            return intent;
                        }
                    } catch (Exception e) {
                        if (originalQuery.contains(keyword) || lowerQuery.contains(keyword.toLowerCase())) {
                            return intent;
                        }
                    }
                } else {
                    if (originalQuery.contains(keyword) || lowerQuery.contains(keyword.toLowerCase())) {
                        return intent;
                    }
                }
            }
        }

        for (Map.Entry<IntentType, List<Pattern>> entry : PATTERN_MAP.entrySet()) {
            IntentType intent = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                Matcher matcher = pattern.matcher(originalQuery);
                if (matcher.find()) {
                    return intent;
                }
            }
        }

        return IntentType.UNKNOWN;
    }

    private IntentType llmBasedRecognition(String userQuery) {
        try {
            String prompt = """
                    你是一个意图识别专家，请分析用户的输入并判断其意图类型。
                    
                    可用的意图类型：
                    1. general_qa - 普通问答：用户询问知识、信息、建议等，不需要调用工具
                    2. rag_retrieval - RAG检索：用户询问已上传文档中的内容，需要进行向量检索
                    3. document_search - 文档搜索：用户想要搜索或查找文档
                    4. document_view - 文档查看：用户想要查看某个文档的内容
                    5. document_create - 文档创建：用户想要创建新文档
                    6. document_edit - 文档编辑：用户想要编辑现有文档
                    7. document_delete - 文档删除：用户想要删除文档
                    8. document_list - 文档列表：用户想要列出所有文档
                    9. kb_search - 知识库搜索：用户想要在知识库中搜索
                    10. kb_list - 知识库列表：用户想要列出所有知识库
                    11. nl2sql_query - NL2SQL查询：用户想要通过自然语言查询数据库，获取统计信息或列表数据
                    
                    请只返回意图类型的代码，不要返回其他任何内容。
                    用户输入：%s
                    """.formatted(userQuery);

            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content()
                    .trim();

            for (IntentType intent : IntentType.values()) {
                if (result.equalsIgnoreCase(intent.getCode())) {
                    return intent;
                }
            }
        } catch (Exception e) {
            log.warn("LLM意图识别失败，回退到普通问答: {}", e.getMessage());
        }

        return IntentType.GENERAL_QA;
    }

    public boolean requiresToolCall(IntentType intent) {
        return intent != IntentType.GENERAL_QA && 
               intent != IntentType.RAG_RETRIEVAL &&
               intent != IntentType.UNKNOWN;
    }

    public boolean requiresRag(IntentType intent) {
        return intent == IntentType.RAG_RETRIEVAL || 
               intent == IntentType.DOCUMENT_SEARCH ||
               intent == IntentType.KNOWLEDGE_BASE_SEARCH;
    }
}