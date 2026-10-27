package zjc.knowledgemanager.service.chunk;

import org.springframework.ai.document.Document;

import java.util.List;

public interface ChunkStrategy {
    String getName();
    List<Document> chunk(List<Document> documents);
}