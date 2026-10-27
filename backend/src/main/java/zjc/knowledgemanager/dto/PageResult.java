package zjc.knowledgemanager.dto;

import java.util.List;

/**
 * 分页结果封装类
 */
public class PageResult<T> {
    
    private List<T> records;       // 数据记录
    private Long total;            // 总记录数
    private Integer page;          // 当前页码
    private Integer size;          // 每页大小
    
    public PageResult() {}
    
    public PageResult(List<T> records, Long total, Integer page, Integer size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }
    
    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        return new PageResult<>(records, total, page, size);
    }
    
    // Getters and Setters
    public List<T> getRecords() {
        return records;
    }
    
    public void setRecords(List<T> records) {
        this.records = records;
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
    
    @Override
    public String toString() {
        return "PageResult{" +
                "records=" + records +
                ", total=" + total +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}
