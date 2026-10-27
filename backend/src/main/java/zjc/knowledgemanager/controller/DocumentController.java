package zjc.knowledgemanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.*;
import zjc.knowledgemanager.service.DocumentService;
import zjc.knowledgemanager.util.AuthUtil;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "文档管理", description = "文档管理相关接口")
public class DocumentController {

    @Autowired
    private DocumentService documentService;
    
    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            DocumentUploadResponse response = documentService.uploadDocument(userId, knowledgeBaseId, file, description);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("上传文档失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "获取指定文档的详细信息")
    public ApiResponse<DocumentPreviewResponse> getDocumentDetail(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            DocumentPreviewResponse response = documentService.previewDocument(id, userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取文档详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/status")
    public ApiResponse<DocumentStatusResponse> getDocumentStatus(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            DocumentStatusResponse response = documentService.getDocumentStatus(userId, id);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取文档状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    public ApiResponse<DocumentSearchResponse> searchDocuments(@RequestBody DocumentSearchRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            DocumentSearchResponse response = documentService.searchDocuments(userId, request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("搜索文档失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/archive")
    public ApiResponse<String> archiveDocument(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            documentService.archiveDocument(userId, id);
            return ApiResponse.success("文档归档成功");
        } catch (Exception e) {
            return ApiResponse.error("归档文档失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "软删除指定的文档")
    public ApiResponse<String> deleteDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.deleteDocument(userId, id);
            return ApiResponse.success("文档删除成功");
        } catch (Exception e) {
            return ApiResponse.error("删除文档失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/physical")
    @Operation(summary = "物理删除文档", description = "物理删除指定的文档（仅管理员可用）")
    public ApiResponse<String> physicalDeleteDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.physicalDeleteDocument(userId, id);
            return ApiResponse.success("文档物理删除成功");
        } catch (Exception e) {
            return ApiResponse.error("物理删除文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除文档", description = "批量软删除多个文档")
    public ApiResponse<String> batchDeleteDocuments(@RequestBody List<Long> documentIds, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.batchDeleteDocuments(userId, documentIds);
            return ApiResponse.success("批量删除文档成功");
        } catch (Exception e) {
            return ApiResponse.error("批量删除文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏文档", description = "将文档添加到收藏列表")
    public ApiResponse<String> favoriteDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.favoriteDocument(userId, id);
            return ApiResponse.success("收藏成功");
        } catch (Exception e) {
            return ApiResponse.error("收藏失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏", description = "从收藏列表中移除文档")
    public ApiResponse<String> unfavoriteDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.unfavoriteDocument(userId, id);
            return ApiResponse.success("取消收藏成功");
        } catch (Exception e) {
            return ApiResponse.error("取消收藏失败: " + e.getMessage());
        }
    }

    @GetMapping("/favorites")
    @Operation(summary = "获取收藏列表", description = "获取当前用户的收藏文档列表")
    public ApiResponse<DocumentFavoriteResponse> getFavoriteDocuments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            DocumentFavoriteResponse response = documentService.getFavoriteDocuments(userId, page, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取收藏列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/tags")
    @Operation(summary = "给文档添加标签", description = "为指定文档添加标签")
    public ApiResponse<String> addDocumentTags(@PathVariable Long id, @RequestBody List<Long> tagIds, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.addDocumentTags(id, tagIds, userId);
            return ApiResponse.success("标签添加成功");
        } catch (Exception e) {
            return ApiResponse.error("添加标签失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "移除文档标签", description = "从文档中移除指定标签")
    public ApiResponse<String> removeDocumentTag(@PathVariable Long id, @PathVariable Long tagId, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            documentService.removeDocumentTag(id, tagId, userId);
            return ApiResponse.success("标签移除成功");
        } catch (Exception e) {
            return ApiResponse.error("移除标签失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/tags")
    @Operation(summary = "获取文档标签", description = "获取指定文档的所有标签")
    public ApiResponse<List<TagDTO>> getDocumentTags(@PathVariable Long id) {
        try {
            List<TagDTO> tags = documentService.getDocumentTags(id);
            return ApiResponse.success(tags);
        } catch (Exception e) {
            return ApiResponse.error("获取文档标签失败: " + e.getMessage());
        }
    }

    @GetMapping("/tags")
    @Operation(summary = "获取所有标签", description = "获取系统中所有可用的标签")
    public ApiResponse<List<TagDTO>> getAllTags() {
        try {
            List<TagDTO> tags = documentService.getAllTags();
            return ApiResponse.success(tags);
        } catch (Exception e) {
            return ApiResponse.error("获取标签列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询文档", description = "分页获取文档列表，支持白名单排序：sortBy=uploadTime|createdAt，order=asc|desc")
    public ApiResponse<DocumentPageResponse> getDocumentsPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) List<Long> knowledgeBaseIds,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            
            DocumentPageResponse response = documentService.getDocumentsPageWithFilter(
                userId, knowledgeBaseIds, status, page, size, sortBy, order);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取文档分页失败: " + e.getMessage());
        }
    }

    @PostMapping("/tags")
    @Operation(summary = "创建标签", description = "创建新的标签")
    public ApiResponse<TagDTO> createTag(@Valid @RequestBody TagDTO tagDTO, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            tagDTO.setCreatorId(userId);
            TagDTO createdTag = documentService.createTag(tagDTO);
            return ApiResponse.success(createdTag);
        } catch (Exception e) {
            return ApiResponse.error("创建标签失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/test-filter")
    @Operation(summary = "测试文档筛选", description = "测试文档筛选功能（无需认证）")
    public ApiResponse<DocumentPageResponse> testDocumentsFilter(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) List<Long> knowledgeBaseIds,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long userId) {
        try {
            System.out.println("=== 测试文档筛选接口 ===");
            System.out.println("参数 - userId: " + userId + ", page: " + page + ", size: " + size);
            System.out.println("筛选 - knowledgeBaseIds: " + knowledgeBaseIds + ", status: " + status);
            
            // 使用新的筛选方法
            DocumentPageResponse response = documentService.getDocumentsPageWithFilter(
                userId, knowledgeBaseIds, status, page, size, sortBy, order);
            System.out.println("结果 - 总数: " + response.getTotal() + ", 当前页: " + response.getDocuments().size());
            return ApiResponse.success(response);
        } catch (Exception e) {
            System.out.println("测试筛选失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取文档分页失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "文档预览", description = "预览文档内容")
    public ApiResponse<DocumentPreviewResponse> previewDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            DocumentPreviewResponse response = documentService.previewDocument(id, userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("预览文档失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "文档下载", description = "下载文档文件")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            byte[] fileContent = documentService.downloadDocument(id, userId);
            
            DocumentPreviewResponse docInfo = documentService.previewDocument(id, userId);
            String filename = docInfo.getOriginalFilename();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileContent.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/edit")
    @Operation(summary = "编辑文档", description = "编辑文档内容")
    public ApiResponse<String> editDocument(@PathVariable Long id, @Valid @RequestBody DocumentEditRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            boolean success = documentService.editDocument(id, request.getContent(), request.getDescription(), userId);
            if (success) {
                return ApiResponse.success("文档编辑成功");
            } else {
                return ApiResponse.error("文档编辑失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("编辑文档失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "重命名文档", description = "修改文档名称")
    public ApiResponse<String> renameDocument(@PathVariable Long id, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            System.out.println("=== 重命名文档 ===");
            System.out.println("文档ID: " + id + ", 用户ID: " + userId);
            String newName = request.get("name");
            System.out.println("新名称: " + newName);
            
            if (newName == null || newName.trim().isEmpty()) {
                return ApiResponse.error(400, "文档名称不能为空");
            }
            boolean success = documentService.renameDocument(id, newName.trim(), userId);
            System.out.println("重命名结果: " + success);
            if (success) {
                return ApiResponse.success("文档重命名成功");
            } else {
                return ApiResponse.error("文档重命名失败");
            }
        } catch (Exception e) {
            System.out.println("重命名异常: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("重命名文档失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "移动文档", description = "将文档移动到指定知识库")
    public ApiResponse<String> moveDocument(@PathVariable Long id, @RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            Long newKnowledgeBaseId = request.get("knowledgeBaseId");
            if (newKnowledgeBaseId == null) {
                return ApiResponse.error(400, "目标知识库ID不能为空");
            }
            boolean success = documentService.moveDocument(id, newKnowledgeBaseId, userId);
            if (success) {
                return ApiResponse.success("文档移动成功");
            } else {
                return ApiResponse.error("文档移动失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("移动文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制文档", description = "复制文档到指定知识库")
    public ApiResponse<Long> copyDocument(@PathVariable Long id, @RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            Long newKnowledgeBaseId = request.get("knowledgeBaseId");
            if (newKnowledgeBaseId == null) {
                return ApiResponse.error(400, "目标知识库ID不能为空");
            }
            Long newDocId = documentService.copyDocument(id, newKnowledgeBaseId, userId);
            return ApiResponse.success("文档复制成功", newDocId);
        } catch (Exception e) {
            return ApiResponse.error("复制文档失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/description")
    @Operation(summary = "更新文档描述", description = "单独更新文档描述")
    public ApiResponse<String> updateDocumentDescription(@PathVariable Long id, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            String description = request.get("description");
            boolean success = documentService.updateDocumentDescription(id, description, userId);
            if (success) {
                return ApiResponse.success("文档描述更新成功");
            } else {
                return ApiResponse.error("文档描述更新失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("更新文档描述失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "添加文档评论", description = "为文档添加评论")
    public ApiResponse<String> addDocumentComment(@PathVariable Long id, @Valid @RequestBody DocumentCommentRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            boolean success = documentService.addDocumentComment(id, request.getContent(), request.getParentId(), userId);
            if (success) {
                return ApiResponse.success("评论添加成功");
            } else {
                return ApiResponse.error("评论添加失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("添加评论失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "获取文档评论", description = "获取文档的评论列表")
    public ApiResponse<DocumentCommentPageResponse> getDocumentComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            DocumentCommentPageResponse response = documentService.getDocumentComments(id, page, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取评论失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定的评论")
    public ApiResponse<String> deleteDocumentComment(@PathVariable Long id, @PathVariable Long commentId, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            boolean success = documentService.deleteDocumentComment(commentId, userId);
            if (success) {
                return ApiResponse.success("评论删除成功");
            } else {
                return ApiResponse.error("评论删除失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("删除评论失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/comments/{commentId}")
    @Operation(summary = "更新评论", description = "更新评论内容")
    public ApiResponse<String> updateDocumentComment(@PathVariable Long id, @PathVariable Long commentId, @RequestBody DocumentCommentRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            boolean success = documentService.updateDocumentComment(commentId, request.getContent(), userId);
            if (success) {
                return ApiResponse.success("评论更新成功");
            } else {
                return ApiResponse.error("评论更新失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("更新评论失败: " + e.getMessage());
        }
    }
}
