package zjc.knowledgemanager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.CreateUserRequest;
import zjc.knowledgemanager.dto.DocumentPageResponse;
import zjc.knowledgemanager.service.UserService;
import zjc.knowledgemanager.service.DocumentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DocumentService documentService;

    @PostMapping("/create-user")
    public ApiResponse<Long> createTestUser(@RequestBody CreateUserRequest request) {
        try {
            Long userId = userService.createUser(request);
            return ApiResponse.success("用户创建成功", userId);
        } catch (Exception e) {
            return ApiResponse.error("创建用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        return ApiResponse.success("Hello, World!");
    }
    
    @GetMapping("/documents")
    public ApiResponse<DocumentPageResponse> testDocuments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) List<Long> knowledgeBaseIds,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long userId) {
        try {
            System.out.println("=== 测试文档筛选 ===");
            System.out.println("参数: userId=" + userId + ", page=" + page + ", size=" + size);
            System.out.println("筛选: knowledgeBaseIds=" + knowledgeBaseIds + ", status=" + status);
            
            DocumentPageResponse response = documentService.getDocumentsPageWithFilter(
                userId, knowledgeBaseIds, status, page, size, sortBy, order);
            
            System.out.println("结果: 总数=" + response.getTotal() + ", 当前页=" + response.getDocuments().size());
            return ApiResponse.success(response);
        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }
}