package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.DocumentComment;

import java.util.List;

/**
 * 文档评论Mapper接口
 */
@Mapper
public interface DocumentCommentMapper {
    
    /**
     * 插入评论
     */
    int insert(DocumentComment comment);
    
    /**
     * 根据ID查询评论
     */
    DocumentComment selectById(@Param("id") Long id);
    
    /**
     * 根据文档ID查询评论列表（分页）
     */
    List<DocumentComment> selectByDocumentId(@Param("documentId") Long documentId, 
                                           @Param("offset") Integer offset, 
                                           @Param("limit") Integer limit);
    
    /**
     * 根据文档ID查询评论总数
     */
    Long countByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * 根据父评论ID查询回复列表
     */
    List<DocumentComment> selectByParentId(@Param("parentId") Long parentId);
    
    /**
     * 逻辑删除评论
     */
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
    
    /**
     * 根据文档ID删除所有评论
     */
    int deleteByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * 更新评论内容
     */
    int updateContent(@Param("id") Long id, @Param("content") String content, @Param("userId") Long userId);
}
