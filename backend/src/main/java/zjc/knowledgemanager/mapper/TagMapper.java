package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.Tag;

import java.util.List;

/**
 * 标签Mapper接口
 */
@Mapper
public interface TagMapper {
    
    /**
     * 根据ID查询标签
     */
    Tag findById(Long id);
    
    /**
     * 根据名称查询标签
     */
    Tag findByName(String name);
    
    /**
     * 查询所有标签
     */
    List<Tag> findAll();
    
    /**
     * 根据文档ID查询标签
     */
    List<Tag> findByDocumentId(Long documentId);
    
    /**
     * 插入标签
     */
    int insert(Tag tag);
    
    /**
     * 更新标签
     */
    int update(Tag tag);
    
    /**
     * 增加标签使用次数
     */
    int incrementUsageCount(Long tagId);
    
    /**
     * 减少标签使用次数
     */
    int decrementUsageCount(Long tagId);
    
    /**
     * 根据ID删除标签（逻辑删除）
     */
    int deleteById(Long id);
}