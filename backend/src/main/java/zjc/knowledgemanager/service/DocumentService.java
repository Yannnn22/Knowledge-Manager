package zjc.knowledgemanager.service;

import org.springframework.web.multipart.MultipartFile;
import zjc.knowledgemanager.dto.*;

import java.util.List;

public interface DocumentService {
    
    DocumentUploadResponse uploadDocument(Long userId, Long knowledgeBaseId, MultipartFile file, String description);
    
    DocumentStatusResponse getDocumentStatus(Long userId, Long documentId);
    
    DocumentSearchResponse searchDocuments(Long userId, DocumentSearchRequest request);
    
    boolean archiveDocument(Long userId, Long documentId);
    
    boolean deleteDocument(Long userId, Long documentId);
    
    boolean favoriteDocument(Long userId, Long documentId);
    
    boolean unfavoriteDocument(Long userId, Long documentId);
    
    DocumentFavoriteResponse getFavoriteDocuments(Long userId, Integer page, Integer size);
    
    boolean addDocumentTags(Long documentId, List<Long> tagIds, Long userId);
    
    boolean removeDocumentTag(Long documentId, Long tagId, Long userId);
    
    List<TagDTO> getDocumentTags(Long documentId);
    
    List<TagDTO> getAllTags();
    
    TagDTO createTag(TagDTO tagDTO);

    DocumentPageResponse getDocumentsPage(Integer page, Integer size, String sortBy, String order);
    
    DocumentPageResponse getDocumentsPageWithFilter(Long userId, List<Long> knowledgeBaseIds, Integer status, 
                                                   Integer page, Integer size, String sortBy, String order);
    
    boolean physicalDeleteDocument(Long userId, Long documentId);
    
    boolean batchDeleteDocuments(Long userId, List<Long> documentIds);
    
    DocumentPreviewResponse previewDocument(Long documentId, Long userId);
    
    byte[] downloadDocument(Long documentId, Long userId);
    
    boolean editDocument(Long documentId, String content, String description, Long userId);
    
    boolean renameDocument(Long documentId, String newName, Long userId);
    
    boolean moveDocument(Long documentId, Long newKnowledgeBaseId, Long userId);
    
    Long copyDocument(Long documentId, Long newKnowledgeBaseId, Long userId);
    
    boolean updateDocumentDescription(Long documentId, String description, Long userId);
    
    boolean addDocumentComment(Long documentId, String content, Long parentId, Long userId);
    
    DocumentCommentPageResponse getDocumentComments(Long documentId, Integer page, Integer size);
    
    boolean deleteDocumentComment(Long commentId, Long userId);
    
    boolean updateDocumentComment(Long commentId, String content, Long userId);

    KnowledgeBaseDocumentsResponse findDocumentsByKnowledgeBaseId(Long knowledgeBaseId, int page, int size);
}