package zjc.knowledgemanager.service.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ParagraphChunkStrategy implements ChunkStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ParagraphChunkStrategy.class);
    private static final int MAX_CHUNK_TOKENS = 512;
    private static final int ESTIMATED_CHARS_PER_TOKEN = 4;

    @Override
    public String getName() {
        return "paragraph";
    }

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) {
                continue;
            }
            
            String[] paragraphs = content.split("\n\\s*\n");
            
            for (int i = 0; i < paragraphs.length; i++) {
                String paragraph = paragraphs[i].trim();
                if (paragraph.isEmpty()) {
                    continue;
                }
                
                List<String> paragraphChunks = splitBySize(paragraph, MAX_CHUNK_TOKENS * ESTIMATED_CHARS_PER_TOKEN);
                
                for (int j = 0; j < paragraphChunks.size(); j++) {
                    Document chunk = new Document(paragraphChunks.get(j));
                    chunk.getMetadata().putAll(doc.getMetadata());
                    chunk.getMetadata().put("chunkType", "paragraph");
                    chunk.getMetadata().put("paragraphIndex", i);
                    chunk.getMetadata().put("chunkIndex", j);
                    chunk.getMetadata().put("totalChunks", paragraphChunks.size());
                    chunks.add(chunk);
                }
            }
        }
        
        logger.info("Paragraph chunking completed: {} chunks from {} documents", chunks.size(), documents.size());
        return chunks;
    }

    private List<String> splitBySize(String content, int maxSize) {
        List<String> chunks = new ArrayList<>();
        
        if (content.length() <= maxSize) {
            chunks.add(content);
            return chunks;
        }
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxSize, content.length());
            
            if (end < content.length()) {
                int lastSentenceEnd = findLastSentenceEnd(content, start, end);
                if (lastSentenceEnd > start) {
                    end = lastSentenceEnd;
                } else {
                    int lastSpace = content.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace;
                    }
                }
            }
            
            chunks.add(content.substring(start, end).trim());
            start = end;
        }
        
        return chunks;
    }

    private int findLastSentenceEnd(String content, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = content.charAt(i);
            if (c == '。' || c == '!' || c == '?' || c == '.' || c == ';' || c == '\n') {
                return i + 1;
            }
        }
        return -1;
    }
}