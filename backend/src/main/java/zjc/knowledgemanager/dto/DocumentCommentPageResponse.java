package zjc.knowledgemanager.dto;

import java.util.List;

public class DocumentCommentPageResponse {
    
    private Long total;
    private Integer page;
    private Integer size;
    private List<DocumentCommentResponse> comments;

    public DocumentCommentPageResponse() {}

    public DocumentCommentPageResponse(Long total, Integer page, Integer size, List<DocumentCommentResponse> comments) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.comments = comments;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public List<DocumentCommentResponse> getComments() {
        return comments;
    }

    public void setComments(List<DocumentCommentResponse> comments) {
        this.comments = comments;
    }
}