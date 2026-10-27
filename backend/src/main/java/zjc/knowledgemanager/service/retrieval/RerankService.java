package zjc.knowledgemanager.service.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RerankService {

    private static final Logger logger = LoggerFactory.getLogger(RerankService.class);
    
    private static final String RERANK_PROMPT = """
            你是一个专业的文档重排助手。请根据以下查询和文档片段，对文档片段进行相关性排序。
            
            查询：{query}
            
            文档片段列表：
            {documents}
            
            请按照与查询的相关性从高到低排序，输出文档的编号（从0开始），用逗号分隔。
            只输出排序后的编号列表，不要输出其他内容。
            
            示例输出：2,0,1,3,4
            """;

    private final ChatClient chatClient;

    @Autowired
    public RerankService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<org.springframework.ai.document.Document> rerank(String query, 
            List<org.springframework.ai.document.Document> documents, int topK) {
        
        if (documents == null || documents.isEmpty() || documents.size() <= topK) {
            return documents;
        }
        
        logger.info("Starting rerank for {} documents, query: '{}'", documents.size(), query);
        
        StringBuilder docsBuilder = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            String content = documents.get(i).getText();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            docsBuilder.append(i).append(". ").append(content).append("\n\n");
        }
        
        String promptText = RERANK_PROMPT
                .replace("{query}", query)
                .replace("{documents}", docsBuilder.toString());
        
        try {
            String result = chatClient.prompt()
                    .system("你是一个专业的文档重排助手，只输出排序结果。")
                    .user(promptText)
                    .call()
                    .content()
                    .trim();
            
            List<Integer> sortedIndices = parseRerankResult(result, documents.size());
            
            return sortedIndices.stream()
                    .map(documents::get)
                    .limit(topK)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.warn("Rerank failed, returning original order: {}", e.getMessage());
            return documents.stream().limit(topK).collect(Collectors.toList());
        }
    }

    private List<Integer> parseRerankResult(String result, int maxIndex) {
        List<Integer> indices = new ArrayList<>();
        
        try {
            String[] parts = result.split("[,\\s]+");
            Set<Integer> seen = new HashSet<>();
            
            for (String part : parts) {
                try {
                    int index = Integer.parseInt(part.trim());
                    if (index >= 0 && index < maxIndex && seen.add(index)) {
                        indices.add(index);
                    }
                } catch (NumberFormatException e) {
                    // skip invalid numbers
                }
            }
            
            for (int i = 0; i < maxIndex; i++) {
                if (!seen.contains(i)) {
                    indices.add(i);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse rerank result: {}", e.getMessage());
            for (int i = 0; i < maxIndex; i++) {
                indices.add(i);
            }
        }
        
        return indices;
    }
}