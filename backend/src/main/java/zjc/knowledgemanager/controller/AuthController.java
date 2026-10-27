package zjc.knowledgemanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.LoginRequest;
import zjc.knowledgemanager.dto.LoginResponse;
import zjc.knowledgemanager.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (Exception e) {
            log.error("登录失败: username={}, error={}", request.getUsername(), e.getMessage(), e);
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = "登录服务异常，请稍后重试";
            }
            return ApiResponse.error(message);
        }
    }
}
