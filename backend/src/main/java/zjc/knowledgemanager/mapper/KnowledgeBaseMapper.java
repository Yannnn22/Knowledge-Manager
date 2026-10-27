package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库Mapper接口
 */
@Mapper
public interface KnowledgeBaseMapper {
    
    /**
     * 插入知识库
     */
    int insert(KnowledgeBase knowledgeBase);
    
    /**
     * 根据ID查询知识库
     */
    KnowledgeBase findById(Long id);
    
    /**
     * 根据用户ID查询知识库列表
     */
    List<KnowledgeBase> findByOwnerId(Long ownerId);
    
    /**
     * 根据部门查询知识库列表
     */
    List<KnowledgeBase> findByDepartment(String department);
    
    /**
     * 根据用户ID和部门查询知识库列表
     */
    List<KnowledgeBase> findByOwnerIdAndDepartment(@Param("ownerId") Long ownerId, 
                                                   @Param("department") String department);

    /**
     * 查询全部未删除的知识库
     */
    List<KnowledgeBase> findAll();
    
    /**
     * 获取知识库文档数量
     */
    int getDocumentCount(Long knowledgeBaseId);
    
    /**
     * 获取知识库统计信息
     */
    KnowledgeBaseStats getStats(Long knowledgeBaseId);
    
    /**
     * 检查知识库名称是否存在
     */
    int countByName(String name);

    /**
     * 排除自身的重名检查
     */
    int countByNameExcludingId(@Param("name") String name, @Param("id") Long id);

    /**
     * 选择性更新
     */
    int updateSelective(KnowledgeBase knowledgeBase);

    /**
     * 软删除
     */
    int softDelete(Long id);

    /**
     * 统计未完成处理文档
     */
    int countProcessingDocuments(@Param("knowledgeBaseId") Long knowledgeBaseId);
    
    /**
     * 知识库统计信息
     */
    class KnowledgeBaseStats {
        private Integer totalDocuments;
        private Integer processing;
        private Integer completed;
        private Integer failed;
        
        public KnowledgeBaseStats() {}
        
        public KnowledgeBaseStats(Integer totalDocuments, Integer processing, Integer completed, Integer failed) {
            this.totalDocuments = totalDocuments;
            this.processing = processing;
            this.completed = completed;
            this.failed = failed;
        }
        
        public Integer getTotalDocuments() {
            return totalDocuments;
        }
        
        public void setTotalDocuments(Integer totalDocuments) {
            this.totalDocuments = totalDocuments;
        }
        
        public Integer getProcessing() {
            return processing;
        }
        
        public void setProcessing(Integer processing) {
            this.processing = processing;
        }
        
        public Integer getCompleted() {
            return completed;
        }
        
        public void setCompleted(Integer completed) {
            this.completed = completed;
        }
        
        public Integer getFailed() {
            return failed;
        }
        
        public void setFailed(Integer failed) {
            this.failed = failed;
        }
    }
} 