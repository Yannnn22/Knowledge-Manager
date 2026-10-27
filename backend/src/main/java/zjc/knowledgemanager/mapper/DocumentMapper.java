package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.Document;

import java.util.List;

@Mapper
public interface DocumentMapper {
    
    int insert(Document document);
    
    Document findById(Long id);
    
    List<Document> findByKnowledgeBaseId(Long knowledgeBaseId);
    
    List<Document> findByKnowledgeBaseIdAndStatus(@Param("knowledgeBaseId") Long knowledgeBaseId, 
                                                 @Param("status") Integer status);
    
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    int update(Document document);
    
    int updateProcessTime(@Param("id") Long id, 
                         @Param("processStartTime") String processStartTime,
                         @Param("processEndTime") String processEndTime);
    
    int archiveDocument(Long id);
    
    int softDeleteDocument(Long id);
    
    List<Document> searchByKnowledgeBaseIds(@Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                            @Param("sortColumn") String sortColumn,
                                            @Param("direction") String direction);
    
    int checkUserPermission(@Param("documentId") Long documentId, @Param("userId") Long userId);
    
    List<Document> findByUploaderId(Long uploaderId);

    long countAll();

    List<Document> findPage(@Param("sortColumn") String sortColumn,
                            @Param("direction") String direction,
                            @Param("limit") int limit,
                            @Param("offset") int offset);
    
    int physicalDeleteDocument(Long id);
    
    List<Document> findPageByUserPermission(@Param("userId") Long userId,
                                           @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                           @Param("status") Integer status,
                                           @Param("sortColumn") String sortColumn,
                                           @Param("direction") String direction,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset,
                                           @Param("isAdmin") boolean isAdmin);
    
    long countByUserPermission(@Param("userId") Long userId,
                              @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                              @Param("status") Integer status,
                              @Param("isAdmin") boolean isAdmin);

    List<Document> findAllDocumentsByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId, 
                                                     @Param("limit") int limit, 
                                                     @Param("offset") int offset);

    long countAllDocumentsByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}