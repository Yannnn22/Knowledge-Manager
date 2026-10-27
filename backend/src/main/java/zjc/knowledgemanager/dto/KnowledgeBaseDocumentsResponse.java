package zjc.knowledgemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zjc.knowledgemanager.entity.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDocumentsResponse {
    private int page;
    private int size;
    private long total;
    private List<Document> documents;
}
