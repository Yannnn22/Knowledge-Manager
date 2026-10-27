package zjc.knowledgemanager.service.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SmallLargeChunkStrategy implements ChunkStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SmallLargeChunkStrategy.class);
    private static final int SMALL_CHUNK_TOKENS = 256;
    private static final int LARGE_CHUNK_TOKENS = 512;
    private static final int OVERLAP_TOKENS = 64;
    private static final int ESTIMATED_CHARS_PER_TOKEN = 4;

    @Override
    public String getName() {
        return "small-large";
    }

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) {
                continue;
            }
            
            List<String> smallChunks = splitContent(content, SMALL_CHUNK_TOKENS * ESTIMATED_CHARS_PER_TOKEN, 
                    OVERLAP_TOKENS * ESTIMATED_CHARS_PER_TOKEN);
            List<String> largeChunks = splitContent(content, LARGE_CHUNK_TOKENS * ESTIMATED_CHARS_PER_TOKEN, 
                    OVERLAP_TOKENS * ESTIMATED_CHARS_PER_TOKEN);
            
            for (int i = 0; i < smallChunks.size(); i++) {
                Document chunk = new Document(smallChunks.get(i));
                chunk.getMetadata().putAll(doc.getMetadata());
                chunk.getMetadata().put("chunkType", "small");
                chunk.getMetadata().put("chunkSize", "small");
                chunk.getMetadata().put("chunkIndex", i);
                chunk.getMetadata().put("totalChunks", smallChunks.size());
                chunks.add(chunk);
            }
            
            for (int i = 0; i < largeChunks.size(); i++) {
                Document chunk = new Document(largeChunks.get(i));
                chunk.getMetadata().putAll(doc.getMetadata());
                chunk.getMetadata().put("chunkType", "large");
                chunk.getMetadata().put("chunkSize", "large");
                chunk.getMetadata().put("chunkIndex", i);
                chunk.getMetadata().put("totalChunks", largeChunks.size());
                chunks.add(chunk);
            }
        }
        
        logger.info("Small-large chunking completed: {} chunks from {} documents", chunks.size(), documents.size());
        return chunks;
    }

    private List<String> splitContent(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        if (content.length() <= chunkSize) {
            chunks.add(content);
            return chunks;
        }
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            
            if (end < content.length()) {
                int lastNewline = content.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline;
                }
            }
            
            chunks.add(content.substring(start, end).trim());
            start = end - overlap;
            if (start < 0) start = 0;
        }
        
        return chunks;
    }
}