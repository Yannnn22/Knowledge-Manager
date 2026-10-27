package zjc.knowledgemanager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import zjc.knowledgemanager.dto.*;
import zjc.knowledgemanager.entity.*;
import zjc.knowledgemanager.enums.DocumentStatus;
import zjc.knowledgemanager.mapper.*;
import zjc.knowledgemanager.service.DocumentService;
import zjc.knowledgemanager.service.UserService;
import zjc.knowledgemanager.util.IdGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;
    
    @Autowired
    private UserService userService;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;
    
    @Autowired
    private DocumentFavoriteMapper documentFavoriteMapper;
    
    @Autowired
    private TagMapper tagMapper;
    
    @Autowired
    private DocumentTagMapper documentTagMapper;

    @Autowired
    private IdGenerator idGenerator;
    
    @org.springframework.beans.factory.annotation.Value("${file.upload.base-dir:./uploads}")
    private String uploadBaseDir;
    
    @Autowired
    private DocumentCommentMapper documentCommentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public DocumentUploadResponse uploadDocument(Long userId, Long knowledgeBaseId, MultipartFile file, String description) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new RuntimeException("知识库不存在");
        }

        boolean isAdmin = isAdminUser(userId);
        if (!isAdmin && !knowledgeBase.getOwnerId().equals(userId)) {
            throw new RuntimeException("无权限上传文档到该知识库");
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        long fileSize = file.getSize();

        String storagePath = generateStoragePath(knowledgeBaseId, originalFilename);
        
        Document document = new Document();
        document.setId(idGenerator.nextId());
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setOriginalFilename(originalFilename);
        document.setStoragePath(storagePath);
        document.setFileSize(fileSize);
        document.setFileType(fileType);
        document.setUploaderId(userId);
        document.setStatusEnum(DocumentStatus.PROCESSING);
        document.setIsDeleted(0);

        int result = documentMapper.insert(document);
        if (result <= 0) {
            throw new RuntimeException("保存文档记录失败");
        }

        try {
            saveFileToStorage(file, storagePath);
        } catch (IOException e) {
            documentMapper.softDeleteDocument(document.getId());
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }

        documentMapper.updateStatus(document.getId(), 1);

        String statusUrl = "/api/v1/documents/" + document.getId() + "/status";

        return new DocumentUploadResponse(document.getId(), statusUrl);
    }

    @Override
    public DocumentStatusResponse getDocumentStatus(Long userId, Long documentId) {
        if (documentMapper.checkUserPermission(documentId, userId) <= 0) {
            throw new RuntimeException("无权限访问该文档");
        }

        Document document = documentMapper.findById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        int progress = getProgressByStatus(document.getStatusCode());
        String currentStep = getCurrentStepByStatus(document.getStatusCode());

        return new DocumentStatusResponse(document.getStatusCode(), progress, currentStep);
    }

    @Override
    public DocumentSearchResponse searchDocuments(Long userId, DocumentSearchRequest request) {
        boolean isAdmin = isAdminUser(userId);
        
        if (!isAdmin) {
            for (Long knowledgeBaseId : request.getKnowledgeBaseIds()) {
                KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(knowledgeBaseId);
                if (knowledgeBase == null || !knowledgeBase.getOwnerId().equals(userId)) {
                    throw new RuntimeException("无权限搜索该知识库的文档");
                }
            }
        }

        Map<String, String> sortMap = Map.of(
                "uploadtime", "create_time",
                "createdat", "create_time"
        );
        String sortKey = request.getSortBy() == null ? "uploadtime" : request.getSortBy().toLowerCase(Locale.ROOT);
        String sortColumn = sortMap.getOrDefault(sortKey, "create_time");
        String direction = (request.getOrder() != null && request.getOrder().equalsIgnoreCase("asc")) ? "ASC" : "DESC";

        List<Document> documents = documentMapper.searchByKnowledgeBaseIds(request.getKnowledgeBaseIds(), sortColumn, direction);

        String query = request.getQuery().toLowerCase();
        
        List<DocumentSearchResponse.SearchResult> results = documents.stream()
                .filter(doc -> doc.getOriginalFilename().toLowerCase().contains(query))
                .map(doc -> {
                    String fileName = doc.getOriginalFilename().toLowerCase();
                    double score = calculateMatchScore(fileName, query);
                    
                    KnowledgeBase kb = knowledgeBaseMapper.findById(doc.getKnowledgeBaseId());
                    String kbName = kb != null ? kb.getName() : "未知知识库";
                    Long kbId = kb != null ? kb.getId() : null;
                    
                    return new DocumentSearchResponse.SearchResult(
                            doc.getId(),
                            doc.getOriginalFilename(),
                            "文档内容片段...",
                            score,
                            kbId,
                            kbName
                    );
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        return new DocumentSearchResponse(results);
    }

    @Override
    public DocumentPageResponse getDocumentsPage(Integer page, Integer size, String sortBy, String order) {
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Map<String, String> sortMap = Map.of(
                "uploadtime", "create_time",
                "createdat", "create_time"
        );
        String sortKey = sortBy == null ? "uploadtime" : sortBy.toLowerCase(Locale.ROOT);
        String sortColumn = sortMap.getOrDefault(sortKey, "create_time");
        String direction = (order != null && order.equalsIgnoreCase("asc")) ? "ASC" : "DESC";

        long total = documentMapper.countAll();
        int offset = (pageNo - 1) * pageSize;
        List<Document> records = documentMapper.findPage(sortColumn, direction, pageSize, offset);

        List<DocumentPageResponse.Item> items = records.stream().map(d -> {
            DocumentPageResponse.Item item = new DocumentPageResponse.Item();
            item.setId(d.getId());
            item.setKnowledgeBaseId(d.getKnowledgeBaseId());
            item.setOriginalFilename(d.getOriginalFilename());
            item.setFileType(d.getFileType());
            item.setFileSize(calculateActualFileSize(d));
            item.setStatus(d.getStatusCode());
            item.setUploaderId(d.getUploaderId());
            item.setUploadTime(d.getUploadTime());
            return item;
        }).collect(Collectors.toList());

        return new DocumentPageResponse(total, items);
    }
    
    @Override
    public DocumentPageResponse getDocumentsPageWithFilter(Long userId, List<Long> knowledgeBaseIds, Integer status, 
                                                          Integer page, Integer size, String sortBy, String order) {
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Map<String, String> sortMap = Map.of(
                "uploadtime", "create_time",
                "createdat", "create_time", 
                "filename", "name",
                "filesize", "create_time",
                "status", "status"
        );
        String sortKey = sortBy == null ? "uploadtime" : sortBy.toLowerCase(Locale.ROOT);
        String sortColumn = sortMap.getOrDefault(sortKey, "upload_time");
        String direction = (order != null && order.equalsIgnoreCase("asc")) ? "ASC" : "DESC";

        boolean isAdmin = isAdminUser(userId);

        long total = documentMapper.countByUserPermission(userId, knowledgeBaseIds, status, isAdmin);
        
        int offset = (pageNo - 1) * pageSize;
        List<Document> records = documentMapper.findPageByUserPermission(userId, knowledgeBaseIds, status, 
                                                                         sortColumn, direction, pageSize, offset, isAdmin);

        List<DocumentPageResponse.Item> items = records.stream().map(d -> {
            DocumentPageResponse.Item item = new DocumentPageResponse.Item();
            item.setId(d.getId());
            item.setKnowledgeBaseId(d.getKnowledgeBaseId());
            item.setOriginalFilename(d.getOriginalFilename());
            item.setFileType(d.getFileType());
            item.setFileSize(calculateActualFileSize(d));
            item.setStatus(d.getStatusCode());
            item.setUploaderId(d.getUploaderId());
            item.setUploadTime(d.getUploadTime());
            return item;
        }).collect(Collectors.toList());

        return new DocumentPageResponse(total, items);
    }

    @Override
    public boolean archiveDocument(Long userId, Long documentId) {
        if (documentMapper.checkUserPermission(documentId, userId) <= 0) {
            throw new RuntimeException("无权限操作该文档");
        }

        int result = documentMapper.archiveDocument(documentId);
        return result > 0;
    }
    
    @Override
    public boolean deleteDocument(Long userId, Long documentId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        boolean hasPermission = documentMapper.checkUserPermission(documentId, userId) > 0;
        
        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("无权限删除该文档");
        }
        
        documentFavoriteMapper.deleteByDocumentId(documentId);
        documentTagMapper.deleteByDocumentId(documentId);
        return documentMapper.softDeleteDocument(documentId) > 0;
    }
    
    @Override
    public boolean physicalDeleteDocument(Long userId, Long documentId) {
        Document document = documentMapper.findById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        if (!isAdmin) {
            throw new RuntimeException("只有管理员可以物理删除文档");
        }
        
        documentFavoriteMapper.deleteByDocumentId(documentId);
        documentTagMapper.deleteByDocumentId(documentId);
        documentCommentMapper.deleteByDocumentId(documentId);
        
        try {
            String baseDir = System.getProperty("file.upload.base-dir");
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = System.getenv("FILE_UPLOAD_BASE_DIR");
            }
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = "./uploads";
            }
            Path filePath = Paths.get(baseDir).resolve(document.getStoragePath().startsWith("/") ? document.getStoragePath().substring(1) : document.getStoragePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            System.err.println("删除文件失败: " + e.getMessage());
        }
        
        return documentMapper.physicalDeleteDocument(documentId) > 0;
    }
    
    @Override
    public boolean batchDeleteDocuments(Long userId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new RuntimeException("文档ID列表不能为空");
        }
        
        boolean allSuccess = true;
        for (Long documentId : documentIds) {
            try {
                deleteDocument(userId, documentId);
            } catch (Exception e) {
                allSuccess = false;
                System.err.println("删除文档 " + documentId + " 失败: " + e.getMessage());
            }
        }
        
        return allSuccess;
    }
    
    private boolean isAdminUser(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            User user = userService.getUserById(userId);
            return user != null && user.isAdmin();
        } catch (Exception e) {
            System.err.println("获取用户信息失败: " + e.getMessage());
            return false;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String generateStoragePath(Long knowledgeBaseId, String filename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("/documents/%d/%s_%s", knowledgeBaseId, timestamp, filename);
    }

    private void saveFileToStorage(MultipartFile file, String storagePath) throws IOException {
        String baseDir = uploadBaseDir;
        if (baseDir == null || baseDir.isEmpty()) {
            baseDir = System.getenv("FILE_UPLOAD_BASE_DIR");
        }
        if (baseDir == null || baseDir.isEmpty()) {
            baseDir = "./uploads";
        }
        Path fullPath = Paths.get(baseDir).resolve(storagePath.startsWith("/") ? storagePath.substring(1) : storagePath);
        Files.createDirectories(fullPath.getParent());
        file.transferTo(fullPath.toFile());
    }

    private int getProgressByStatus(Integer status) {
        switch (status) {
            case 0: return 10;
            case 1: return 50;
            case 2: return 100;
            case 3: return 0;
            case 4: return 100;
            default: return 0;
        }
    }

    private String getCurrentStepByStatus(Integer status) {
        switch (status) {
            case 0: return "文件上传中";
            case 1: return "文档处理中";
            case 2: return "处理完成";
            case 3: return "处理失败";
            case 4: return "已归档";
            default: return "未知状态";
        }
    }
    
    @Override
    public boolean favoriteDocument(Long userId, Long documentId) {
        Document document = documentMapper.findById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        DocumentFavorite existing = documentFavoriteMapper.findByUserIdAndDocumentId(userId, documentId);
        if (existing != null) {
            throw new RuntimeException("文档已经收藏过了");
        }
        
        DocumentFavorite favorite = new DocumentFavorite(userId, documentId);
        favorite.setId(idGenerator.nextId());
        
        int result = documentFavoriteMapper.insert(favorite);
        return result > 0;
    }
    
    @Override
    public boolean unfavoriteDocument(Long userId, Long documentId) {
        DocumentFavorite existing = documentFavoriteMapper.findByUserIdAndDocumentId(userId, documentId);
        if (existing == null) {
            throw new RuntimeException("文档未收藏");
        }
        
        int result = documentFavoriteMapper.deleteByUserIdAndDocumentId(userId, documentId);
        return result > 0;
    }
    
    @Override
    public DocumentFavoriteResponse getFavoriteDocuments(Long userId, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<DocumentFavoriteResponse.FavoriteDocumentItem> documents = 
            documentFavoriteMapper.findFavoriteDocumentsByUserId(userId);
        PageInfo<DocumentFavoriteResponse.FavoriteDocumentItem> pageInfo = new PageInfo<>(documents);
        for (DocumentFavoriteResponse.FavoriteDocumentItem item : documents) {
            List<TagDTO> tags = getDocumentTags(item.getDocumentId());
            item.setTags(tags);
        }
        return new DocumentFavoriteResponse(pageInfo.getTotal(), documents);
    }
    
    @Override
    public boolean addDocumentTags(Long documentId, List<Long> tagIds, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        
        for (Long tagId : tagIds) {
            Tag tag = tagMapper.findById(tagId);
            if (tag == null) {
                continue;
            }
            
            DocumentTag existing = documentTagMapper.findByDocumentIdAndTagId(documentId, tagId);
            if (existing != null) {
                continue;
            }
            
            DocumentTag documentTag = new DocumentTag(documentId, tagId, userId);
            documentTag.setId(idGenerator.nextId());
            documentTagMapper.insert(documentTag);
            
            tagMapper.incrementUsageCount(tagId);
        }
        
        return true;
    }
    
    @Override
    public boolean removeDocumentTag(Long documentId, Long tagId, Long userId) {
        DocumentTag existing = documentTagMapper.findByDocumentIdAndTagId(documentId, tagId);
        if (existing == null) {
            throw new RuntimeException("文档标签关联不存在");
        }
        
        int result = documentTagMapper.deleteByDocumentIdAndTagId(documentId, tagId);
        if (result > 0) {
            tagMapper.decrementUsageCount(tagId);
        }
        
        return result > 0;
    }
    
    @Override
    public List<TagDTO> getDocumentTags(Long documentId) {
        List<Tag> tags = tagMapper.findByDocumentId(documentId);
        return tags.stream().map(this::convertToTagDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<TagDTO> getAllTags() {
        List<Tag> tags = tagMapper.findAll();
        return tags.stream().map(this::convertToTagDTO).collect(Collectors.toList());
    }
    
    @Override
    public TagDTO createTag(TagDTO tagDTO) {
        Tag existing = tagMapper.findByName(tagDTO.getName());
        if (existing != null) {
            throw new RuntimeException("标签名称已存在");
        }
        
        Tag tag = new Tag();
        tag.setId(idGenerator.nextId());
        tag.setName(tagDTO.getName());
        tag.setColor(tagDTO.getColor() != null ? tagDTO.getColor() : "#1890ff");
        tag.setDescription(tagDTO.getDescription());
        tag.setCreatorId(tagDTO.getCreatorId());
        tag.setUsageCount(0);
        tag.setIsDeleted(0);
        
        int result = tagMapper.insert(tag);
        if (result > 0) {
            return convertToTagDTO(tag);
        }
        
        throw new RuntimeException("创建标签失败");
    }
    
    private TagDTO convertToTagDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        dto.setDescription(tag.getDescription());
        dto.setCreatorId(tag.getCreatorId());
        dto.setUsageCount(tag.getUsageCount());
        return dto;
    }
    
    @Override
    public DocumentPreviewResponse previewDocument(Long documentId, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        if (documentMapper.checkUserPermission(documentId, userId) <= 0) {
            throw new RuntimeException("无权限预览该文档");
        }
        
        DocumentPreviewResponse response = new DocumentPreviewResponse();
        response.setId(document.getId());
        response.setOriginalFilename(document.getOriginalFilename());
        response.setFileType(document.getFileType());
        
        Long actualFileSize = calculateActualFileSize(document);
        response.setFileSize(actualFileSize);
        
        response.setUploadTime(document.getUploadTime());
        response.setStatus(document.getStatusCode());
        
        User uploader = userMapper.findById(document.getUploaderId());
        if (uploader != null) {
            DocumentPreviewResponse.UserInfo userInfo = new DocumentPreviewResponse.UserInfo(
                uploader.getId(), uploader.getUsername(), uploader.getRealName(), uploader.getAvatar()
            );
            response.setUploader(userInfo);
        }
        
        List<TagDTO> tags = getDocumentTags(documentId);
        response.setTags(tags);
        
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(document.getKnowledgeBaseId());
        if (knowledgeBase != null) {
            response.setKnowledgeBaseId(knowledgeBase.getId());
            response.setKnowledgeBaseName(knowledgeBase.getName());
            response.setDescription(knowledgeBase.getDescription());
        } else {
            response.setKnowledgeBaseId(document.getKnowledgeBaseId());
            response.setKnowledgeBaseName("未知知识库");
            response.setDescription("知识库信息缺失");
        }
        
        String content = extractDocumentContent(document);
        response.setContent(content);
        
        if (isImageFile(document.getFileType())) {
            response.setPreviewUrl("/api/v1/documents/" + documentId + "/download");
        }
        
        return response;
    }
    
    private Long calculateActualFileSize(Document document) {
        try {
            String baseDir = System.getProperty("file.upload.base-dir");
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = System.getenv("FILE_UPLOAD_BASE_DIR");
            }
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = "./uploads";
            }
            
            Path filePath = Paths.get(baseDir).resolve(
                document.getStoragePath().startsWith("/") ? 
                document.getStoragePath().substring(1) : 
                document.getStoragePath()
            );
            
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            } else {
                return document.getFileSize() != null ? document.getFileSize() : 0L;
            }
        } catch (Exception e) {
            return document.getFileSize() != null ? document.getFileSize() : 0L;
        }
    }
    
    private double calculateMatchScore(String fileName, String query) {
        double score = 0.0;
        
        if (fileName.contains(query)) {
            int matchIndex = fileName.indexOf(query);
            
            if (matchIndex == 0) {
                score += 0.3;
            }
            
            String[] queryWords = query.split("[\\s\\-_]+");
            int matchedWords = 0;
            for (String word : queryWords) {
                if (fileName.contains(word)) {
                    matchedWords++;
                }
            }
            score += (double) matchedWords / queryWords.length * 0.4;
            
            int fileNameLength = fileName.length();
            int queryLength = query.length();
            double overlapRatio = (double) Math.min(fileNameLength, queryLength) / fileNameLength;
            score += overlapRatio * 0.3;
            
            score = Math.min(score, 1.0);
        }
        
        return score;
    }
    
    @Override
    public byte[] downloadDocument(Long documentId, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        if (documentMapper.checkUserPermission(documentId, userId) <= 0) {
            throw new RuntimeException("无权限下载该文档");
        }
        
        try {
            String baseDir = System.getProperty("file.upload.base-dir");
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = System.getenv("FILE_UPLOAD_BASE_DIR");
            }
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = "./uploads";
            }
            Path filePath = Paths.get(baseDir).resolve(document.getStoragePath().startsWith("/") ? document.getStoragePath().substring(1) : document.getStoragePath());
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                String content = "这是文档 " + document.getOriginalFilename() + " 的内容。\n\n" +
                               "文档ID: " + document.getId() + "\n" +
                               "文件类型: " + document.getFileType() + "\n" +
                               "文件大小: " + document.getFileSize() + " bytes\n" +
                               "上传时间: " + document.getUploadTime() + "\n\n" +
                               "实际项目中，这里应该是从存储服务中读取的真实文件内容。";
                return content.getBytes("UTF-8");
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean editDocument(Long documentId, String content, String description, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        boolean isUploader = document.getUploaderId().equals(userId);
        
        if (!isAdmin && !isUploader) {
            throw new RuntimeException("无权限编辑该文档");
        }
        
        return true;
    }
    
    @Override
    public boolean renameDocument(Long documentId, String newName, Long userId) {
        System.out.println("=== renameDocument ===");
        System.out.println("documentId: " + documentId + ", newName: " + newName + ", userId: " + userId);
        
        Document document = documentMapper.findById(documentId);
        System.out.println("findById result: " + (document != null ? "not null" : "null"));
        
        if (document != null) {
            System.out.println("document.id: " + document.getId());
            System.out.println("document.name: " + document.getName());
            System.out.println("document.isDeleted: " + document.getIsDeleted());
            System.out.println("document.status: " + document.getStatusEnum());
        }
        
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        System.out.println("isAdmin: " + isAdmin);
        
        boolean isUploader = document.getUploaderId().equals(userId);
        System.out.println("isUploader: " + isUploader + ", uploaderId: " + document.getUploaderId());
        
        if (!isAdmin && !isUploader) {
            throw new RuntimeException("无权限重命名该文档");
        }
        
        document.setName(newName);
        document.setUpdateTime(LocalDateTime.now());
        
        System.out.println("准备更新 - document.id: " + document.getId() + ", document.name: " + document.getName());
        
        int result = documentMapper.update(document);
        System.out.println("update result: " + result);
        
        return result > 0;
    }
    
    @Override
    public boolean moveDocument(Long documentId, Long newKnowledgeBaseId, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        KnowledgeBase newKnowledgeBase = knowledgeBaseMapper.findById(newKnowledgeBaseId);
        if (newKnowledgeBase == null || newKnowledgeBase.getIsDeleted() == 1) {
            throw new RuntimeException("目标知识库不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        boolean isUploader = document.getUploaderId().equals(userId);
        
        if (!isAdmin && !isUploader) {
            throw new RuntimeException("无权限移动该文档");
        }
        
        document.setKnowledgeBaseId(newKnowledgeBaseId);
        document.setUpdateTime(LocalDateTime.now());
        int result = documentMapper.update(document);
        return result > 0;
    }
    
    @Override
    public Long copyDocument(Long documentId, Long newKnowledgeBaseId, Long userId) {
        Document original = documentMapper.findById(documentId);
        if (original == null || original.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        KnowledgeBase newKnowledgeBase = knowledgeBaseMapper.findById(newKnowledgeBaseId);
        if (newKnowledgeBase == null || newKnowledgeBase.getIsDeleted() == 1) {
            throw new RuntimeException("目标知识库不存在");
        }
        
        Document copy = new Document();
        copy.setId(idGenerator.nextId());
        copy.setName(original.getName() + " (副本)");
        copy.setKnowledgeBaseId(newKnowledgeBaseId);
        copy.setFilePath(original.getFilePath());
        copy.setStatusEnum(original.getStatusEnum());
        copy.setUploaderId(userId);
        copy.setIsDeleted(0);
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());
        
        documentMapper.insert(copy);
        return copy.getId();
    }
    
    @Override
    public boolean updateDocumentDescription(Long documentId, String description, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        boolean isUploader = document.getUploaderId().equals(userId);
        
        if (!isAdmin && !isUploader) {
            throw new RuntimeException("无权限更新该文档描述");
        }
        
        document.setUpdateTime(LocalDateTime.now());
        int result = documentMapper.update(document);
        return result > 0;
    }
    
    @Override
    public boolean addDocumentComment(Long documentId, String content, Long parentId, Long userId) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        if (documentMapper.checkUserPermission(documentId, userId) <= 0) {
            throw new RuntimeException("无权限评论该文档");
        }
        
        if (parentId != null) {
            DocumentComment parentComment = documentCommentMapper.selectById(parentId);
            if (parentComment == null || !parentComment.getDocumentId().equals(documentId)) {
                throw new RuntimeException("父评论不存在或不属于该文档");
            }
        }
        
        DocumentComment comment = new DocumentComment(documentId, userId, content, parentId);
        comment.setId(idGenerator.nextId());
        comment.setCommentTime(LocalDateTime.now());
        
        int result = documentCommentMapper.insert(comment);
        return result > 0;
    }
    
    @Override
    public DocumentCommentPageResponse getDocumentComments(Long documentId, Integer page, Integer size) {
        Document document = documentMapper.findById(documentId);
        if (document == null || document.getIsDeleted() == 1) {
            throw new RuntimeException("文档不存在");
        }
        
        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);
        int offset = (pageNo - 1) * pageSize;
        
        Long total = documentCommentMapper.countByDocumentId(documentId);
        
        List<DocumentComment> mainComments = documentCommentMapper.selectByDocumentId(documentId, offset, pageSize);
        
        List<DocumentCommentResponse> responses = new ArrayList<>();
        for (DocumentComment comment : mainComments) {
            DocumentCommentResponse response = convertToCommentResponse(comment);
            
            List<DocumentComment> replies = documentCommentMapper.selectByParentId(comment.getId());
            List<DocumentCommentResponse> replyResponses = replies.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
            response.setReplies(replyResponses);
            
            responses.add(response);
        }
        
        return new DocumentCommentPageResponse(total, pageNo, pageSize, responses);
    }
    
    @Override
    public boolean deleteDocumentComment(Long commentId, Long userId) {
        DocumentComment comment = documentCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        boolean isAdmin = isAdminUser(userId);
        boolean isAuthor = comment.getUserId().equals(userId);
        
        if (!isAdmin && !isAuthor) {
            throw new RuntimeException("无权限删除该评论");
        }
        
        int result = documentCommentMapper.deleteById(commentId, userId);
        return result > 0;
    }
    
    @Override
    public boolean updateDocumentComment(Long commentId, String content, Long userId) {
        DocumentComment comment = documentCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权限编辑该评论");
        }
        
        int result = documentCommentMapper.updateContent(commentId, content, userId);
        return result > 0;
    }
    
    private String extractDocumentContent(Document document) {
        try {
            String baseDir = System.getProperty("file.upload.base-dir");
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = System.getenv("FILE_UPLOAD_BASE_DIR");
            }
            if (baseDir == null || baseDir.isEmpty()) {
                baseDir = "./uploads";
            }
            
            Path filePath = Paths.get(baseDir).resolve(
                document.getStoragePath().startsWith("/") ? 
                document.getStoragePath().substring(1) : 
                document.getStoragePath()
            );
            
            if (!Files.exists(filePath)) {
                return "文件不存在，无法提取内容。";
            }
            
            if (isTextFile(document.getFileType())) {
                return extractTextContent(filePath, document.getFileType());
            } else if (isImageFile(document.getFileType())) {
                return "这是一个图片文件，无法直接显示文本内容。";
            } else {
                return "该文件类型暂不支持内容预览。";
            }
        } catch (Exception e) {
            return "提取文档内容时发生错误: " + e.getMessage();
        }
    }
    
    private String extractTextContent(Path filePath, String fileType) {
        try {
            if ("txt".equals(fileType)) {
                return Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8);
            } else if ("pdf".equals(fileType)) {
                return "PDF文件内容提取功能需要集成专门的PDF解析库（如Apache PDFBox）。\n" +
                       "文件路径: " + filePath.toString() + "\n" +
                       "文件大小: " + Files.size(filePath) + " 字节";
            } else if ("docx".equals(fileType) || "doc".equals(fileType)) {
                return "Word文档内容提取功能需要集成专门的文档解析库（如Apache POI）。\n" +
                       "文件路径: " + filePath.toString() + "\n" +
                       "文件大小: " + Files.size(filePath) + " 字节";
            } else {
                try {
                    return Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return "无法读取文件内容，文件可能不是文本格式或编码不支持。\n" +
                           "文件路径: " + filePath.toString() + "\n" +
                           "文件大小: " + Files.size(filePath) + " 字节";
                }
            }
        } catch (Exception e) {
            return "读取文件内容时发生错误: " + e.getMessage();
        }
    }
    
    private boolean isTextFile(String fileType) {
        return fileType != null && (
            fileType.equals("txt") || fileType.equals("pdf") || 
            fileType.equals("doc") || fileType.equals("docx") ||
            fileType.equals("md") || fileType.equals("html")
        );
    }
    
    private boolean isImageFile(String fileType) {
        return fileType != null && (
            fileType.equals("jpg") || fileType.equals("jpeg") || 
            fileType.equals("png") || fileType.equals("gif") ||
            fileType.equals("bmp") || fileType.equals("webp")
        );
    }
    
    private DocumentCommentResponse convertToCommentResponse(DocumentComment comment) {
        DocumentCommentResponse response = new DocumentCommentResponse(
            comment.getId(), comment.getCommentTime(), comment.getContent(), 
            comment.getParentId(), null
        );
        
        if (comment.getUser() != null) {
            DocumentCommentResponse.UserInfo userInfo = new DocumentCommentResponse.UserInfo(
                comment.getUser().getId(), comment.getUser().getUsername(),
                comment.getUser().getRealName(), comment.getUser().getAvatar()
            );
            response.setUser(userInfo);
        }
        
        return response;
    }

    @Override
    public KnowledgeBaseDocumentsResponse findDocumentsByKnowledgeBaseId(Long knowledgeBaseId, int page, int size) {
        int offset = (page - 1) * size;
        List<Document> documents = documentMapper.findAllDocumentsByKnowledgeBaseId(knowledgeBaseId, size, offset);
        long total = documentMapper.countAllDocumentsByKnowledgeBaseId(knowledgeBaseId);
        return new KnowledgeBaseDocumentsResponse(page, size, total, documents);
    }
}
