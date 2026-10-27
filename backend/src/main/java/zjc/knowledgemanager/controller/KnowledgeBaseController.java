package zjc.knowledgemanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.BatchDeleteRequest;
import zjc.knowledgemanager.dto.BatchDeleteResult;
import zjc.knowledgemanager.dto.CreateKnowledgeBaseRequest;
import zjc.knowledgemanager.dto.KnowledgeBaseDetailResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseDocumentsResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseListResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseStatsResponse;
import zjc.knowledgemanager.dto.UpdateKnowledgeBaseRequest;
import zjc.knowledgemanager.service.DocumentService;
import zjc.knowledgemanager.service.KnowledgeBaseService;
import zjc.knowledgemanager.util.AuthUtil;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ApiResponse<Long> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            Long knowledgeBaseId = knowledgeBaseService.createKnowledgeBase(userId, request);
            return ApiResponse.success(knowledgeBaseId);
        } catch (Exception e) {
            return ApiResponse.error("创建知识库失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<KnowledgeBaseListResponse> getKnowledgeBaseList(@RequestParam(required = false) String department,
                                                                       @RequestParam(required = false, defaultValue = "false") Boolean includeAll,
                                                                       HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            KnowledgeBaseListResponse response = knowledgeBaseService.getKnowledgeBaseList(userId, department, includeAll != null && includeAll);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取知识库列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseDetailResponse> getKnowledgeBaseDetail(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            KnowledgeBaseDetailResponse response = knowledgeBaseService.getKnowledgeBaseDetail(userId, id);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取知识库详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<KnowledgeBaseStatsResponse> getKnowledgeBaseStats(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            KnowledgeBaseStatsResponse response = knowledgeBaseService.getKnowledgeBaseStats(userId, id);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取知识库统计失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDetailResponse> updateKnowledgeBase(@PathVariable Long id,
                                                                        @RequestBody UpdateKnowledgeBaseRequest request,
                                                                        HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            KnowledgeBaseDetailResponse updated = knowledgeBaseService.updateKnowledgeBase(userId, id, request);
            return ApiResponse.success("更新成功", updated);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(400, "参数错误: " + ex.getMessage());
        } catch (SecurityException ex) {
            return ApiResponse.error(403, "无权限: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResponse.error(409, "冲突: " + ex.getMessage());
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "更新失败";
            if (msg.contains("不存在")) {
                return ApiResponse.error(404, msg);
            }
            return ApiResponse.error(500, "更新失败: " + msg);
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            knowledgeBaseService.deleteKnowledgeBase(userId, id);
            return ApiResponse.success("删除成功", null);
        } catch (SecurityException ex) {
            return ApiResponse.error(403, "无权限: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResponse.error(409, "冲突: " + ex.getMessage());
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "删除失败";
            if (msg.contains("不存在")) {
                return ApiResponse.error(404, msg);
            }
            return ApiResponse.error(500, "删除失败: " + msg);
        }
    }

    @PostMapping("/batch-delete")
    public ApiResponse<BatchDeleteResult> batchDelete(@RequestBody BatchDeleteRequest request,
                                                      HttpServletRequest httpRequest) {
        try {
            Long userId = AuthUtil.getUserId(httpRequest);
            BatchDeleteResult result = knowledgeBaseService.batchDeleteKnowledgeBases(userId, request.getIds());
            return ApiResponse.success("删除完成", result);
        } catch (RuntimeException ex) {
            return ApiResponse.error("批量删除失败: " + ex.getMessage());
        }
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<KnowledgeBaseDocumentsResponse> getKnowledgeBaseDocuments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            KnowledgeBaseDocumentsResponse documentPage = documentService.findDocumentsByKnowledgeBaseId(id, page, size);
            return ApiResponse.success(documentPage);
        } catch (Exception e) {
            return ApiResponse.error("获取知识库文档失败: " + e.getMessage());
        }
    }
}
