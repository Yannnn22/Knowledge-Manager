package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 文档搜索请求DTO
 */
public class DocumentSearchRequest {
    
    @NotBlank(message = "查询字符串不能为空")
    private String query;
    
    @NotEmpty(message = "知识库ID列表不能为空")
    private List<Long> knowledgeBaseIds;

    // 可选：排序字段（受限：uploadTime | createdAt）
    private String sortBy;

    // 可选：排序方向（受限：asc | desc）
    private String order;

    public DocumentSearchRequest() {}

    public DocumentSearchRequest(String query, List<Long> knowledgeBaseIds) {
        this.query = query;
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Long> getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(List<Long> knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
} 