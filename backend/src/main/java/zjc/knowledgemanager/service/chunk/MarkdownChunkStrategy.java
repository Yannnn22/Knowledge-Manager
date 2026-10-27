package zjc.knowledgemanager.service.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownChunkStrategy implements ChunkStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownChunkStrategy.class);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final int MAX_CHUNK_TOKENS = 512;
    private static final int ESTIMATED_CHARS_PER_TOKEN = 4;

    @Override
    public String getName() {
        return "markdown";
    }

    @Override
    public List<Document> chunk(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) {
                continue;
            }
            
            List<String> sections = parseMarkdownSections(content);
            
            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i);
                String title = extractTitle(section);
                
                List<String> sectionChunks = splitBySize(section, MAX_CHUNK_TOKENS * ESTIMATED_CHARS_PER_TOKEN);
                
                for (int j = 0; j < sectionChunks.size(); j++) {
                    Document chunk = new Document(sectionChunks.get(j));
                    chunk.getMetadata().putAll(doc.getMetadata());
                    chunk.getMetadata().put("chunkType", "markdown");
                    chunk.getMetadata().put("sectionTitle", title);
                    chunk.getMetadata().put("sectionIndex", i);
                    chunk.getMetadata().put("chunkIndex", j);
                    chunk.getMetadata().put("totalChunks", sectionChunks.size());
                    chunks.add(chunk);
                }
            }
        }
        
        logger.info("Markdown chunking completed: {} chunks from {} documents", chunks.size(), documents.size());
        return chunks;
    }

    private List<String> parseMarkdownSections(String content) {
        List<String> sections = new ArrayList<>();
        StringBuilder currentSection = new StringBuilder();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches()) {
                if (currentSection.length() > 0) {
                    sections.add(currentSection.toString().trim());
                    currentSection = new StringBuilder();
                }
            }
            currentSection.append(line).append("\n");
        }
        
        if (currentSection.length() > 0) {
            sections.add(currentSection.toString().trim());
        }
        
        return sections;
    }

    private String extractTitle(String section) {
        Matcher matcher = HEADING_PATTERN.matcher(section);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "Untitled";
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
                int lastNewline = content.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline;
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
}