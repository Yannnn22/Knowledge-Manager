package zjc.knowledgemanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.*;
import zjc.knowledgemanager.service.UserService;
import zjc.knowledgemanager.util.JwtUtil;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping
    public ApiResponse<Long> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            Long userId = userService.createUser(request);
            return ApiResponse.success(userId);
        } catch (Exception e) {
            return ApiResponse.error("创建用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    public ApiResponse<UserPageResponse> getUserPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer status) {
        try {
            UserPageResponse response = userService.getUserPage(page, size, department, status);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("查询用户列表失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<String> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            userService.updateUser(id, request);
            return ApiResponse.success("更新成功");
        } catch (Exception e) {
            return ApiResponse.error("更新用户失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            return ApiResponse.error("删除用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户个人资料", description = "获取当前登录用户的个人资料信息")
    public ApiResponse<UserProfileDTO> getUserProfile(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            Long userId = jwtUtil.getUserIdFromToken(token);
            UserProfileDTO profile = userService.getUserProfile(userId);
            return ApiResponse.success(profile);
        } catch (Exception e) {
            return ApiResponse.error("获取个人资料失败: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的个人资料")
    public ApiResponse<String> updateUserProfile(@Valid @RequestBody UserProfileDTO request, HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            Long userId = jwtUtil.getUserIdFromToken(token);
            request.setUserId(userId);
            userService.updateUserProfile(request);
            return ApiResponse.success("个人资料更新成功");
        } catch (Exception e) {
            return ApiResponse.error("更新个人资料失败: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public ApiResponse<String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            Long userId = jwtUtil.getUserIdFromToken(token);
            userService.updatePassword(userId, request);
            return ApiResponse.success("密码修改成功");
        } catch (Exception e) {
            return ApiResponse.error("修改密码失败: " + e.getMessage());
        }
    }

    /**
     * 从请求头中提取JWT token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("未找到有效的认证token");
    }
}
