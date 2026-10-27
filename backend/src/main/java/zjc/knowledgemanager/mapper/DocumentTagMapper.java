package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.DocumentTag;

import java.util.List;

/**
 * 文档标签关联Mapper接口
 */
@Mapper
public interface DocumentTagMapper {
    
    /**
     * 根据文档ID和标签ID查询关联记录
     */
    DocumentTag findByDocumentIdAndTagId(@Param("documentId") Long documentId, @Param("tagId") Long tagId);
    
    /**
     * 根据文档ID查询所有标签关联
     */
    List<DocumentTag> findByDocumentId(Long documentId);
    
    /**
     * 根据标签ID查询所有文档关联
     */
    List<DocumentTag> findByTagId(Long tagId);
    
    /**
     * 插入文档标签关联
     */
    int insert(DocumentTag documentTag);
    
    /**
     * 批量插入文档标签关联
     */
    int batchInsert(@Param("documentTags") List<DocumentTag> documentTags);
    
    /**
     * 删除文档标签关联
     */
    int deleteByDocumentIdAndTagId(@Param("documentId") Long documentId, @Param("tagId") Long tagId);
    
    /**
     * 根据文档ID删除所有标签关联
     */
    int deleteByDocumentId(Long documentId);
    
    /**
     * 根据标签ID删除所有文档关联
     */
    int deleteByTagId(Long tagId);
}