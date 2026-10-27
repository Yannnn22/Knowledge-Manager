package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.DocumentFavorite;
import zjc.knowledgemanager.dto.DocumentFavoriteResponse;

import java.util.List;

/**
 * 文档收藏Mapper接口
 */
@Mapper
public interface DocumentFavoriteMapper {
    
    /**
     * 根据用户ID和文档ID查询收藏记录
     */
    DocumentFavorite findByUserIdAndDocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);
    
    /**
     * 根据用户ID查询收藏的文档列表（分页）
     */
    List<DocumentFavoriteResponse.FavoriteDocumentItem> findFavoriteDocumentsByUserId(Long userId);
    
    /**
     * 统计用户收藏的文档数量
     */
    Long countByUserId(Long userId);
    
    /**
     * 插入收藏记录
     */
    int insert(DocumentFavorite documentFavorite);
    
    /**
     * 删除收藏记录
     */
    int deleteByUserIdAndDocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);
    
    /**
     * 根据用户ID删除所有收藏记录
     */
    int deleteByUserId(Long userId);
    
    /**
     * 根据文档ID删除所有收藏记录
     */
    int deleteByDocumentId(Long documentId);
}