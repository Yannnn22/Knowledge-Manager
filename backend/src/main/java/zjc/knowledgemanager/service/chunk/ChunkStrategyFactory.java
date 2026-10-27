package zjc.knowledgemanager.service.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChunkStrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(ChunkStrategyFactory.class);
    
    private final Map<String, ChunkStrategy> strategies;
    private final MarkdownChunkStrategy markdownStrategy;
    private final SmallLargeChunkStrategy smallLargeStrategy;
    private final ParagraphChunkStrategy paragraphStrategy;

    @Autowired
    public ChunkStrategyFactory(List<ChunkStrategy> strategyList,
                                MarkdownChunkStrategy markdownStrategy,
                                SmallLargeChunkStrategy smallLargeStrategy,
                                ParagraphChunkStrategy paragraphStrategy) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(ChunkStrategy::getName, s -> s));
        this.markdownStrategy = markdownStrategy;
        this.smallLargeStrategy = smallLargeStrategy;
        this.paragraphStrategy = paragraphStrategy;
    }

    public List<Document> smartChunk(List<Document> documents) {
        List<Document> allChunks = documents.stream()
                .flatMap(doc -> chunkDocument(doc).stream())
                .collect(Collectors.toList());
        
        logger.info("Smart chunking completed: {} total chunks from {} documents", 
                allChunks.size(), documents.size());
        return allChunks;
    }

    private List<Document> chunkDocument(Document document) {
        String content = document.getText();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        
        boolean isMarkdown = isMarkdownContent(content);
        
        if (isMarkdown) {
            logger.debug("Using Markdown chunking strategy");
            List<Document> markdownChunks = markdownStrategy.chunk(List.of(document));
            
            if (markdownChunks.size() > 1) {
                return markdownChunks;
            }
        }
        
        logger.debug("Using Small-Large chunking strategy");
        List<Document> smallLargeChunks = smallLargeStrategy.chunk(List.of(document));
        
        if (smallLargeChunks.isEmpty()) {
            logger.debug("Falling back to Paragraph chunking strategy");
            return paragraphStrategy.chunk(List.of(document));
        }
        
        return smallLargeChunks;
    }

    private boolean isMarkdownContent(String content) {
        int headingCount = countOccurrences(content, "^#{1,6}\\s");
        int codeBlockCount = countOccurrences(content, "^```");
        int linkCount = countOccurrences(content, "\\[.*\\]\\(.*\\)");
        
        return headingCount >= 2 || codeBlockCount >= 2 || linkCount >= 5;
    }

    private int countOccurrences(String content, String regex) {
        return (int) content.lines().filter(line -> line.matches(regex)).count();
    }

    public ChunkStrategy getStrategy(String name) {
        return strategies.get(name);
    }
}