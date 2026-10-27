package zjc.knowledgemanager.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.dto.ConversationDetailResponse;
import zjc.knowledgemanager.dto.ConversationListResponse;
import zjc.knowledgemanager.dto.ConversationStatsResponse;
import zjc.knowledgemanager.dto.PageResult;
import zjc.knowledgemanager.entity.Conversation;
import zjc.knowledgemanager.service.ConversationService;

import java.util.Map;
import java.util.UUID;

/**
 * 对话历史管理控制器（新版）
 * 存储策略：MySQL 持久化 + Redis 热缓存
 * 所有接口需要从 JWT 中获取 userId，无需前端传递
 */
@RestController
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "对话历史管理", description = "对话历史查询、删除和统计相关接口")
@RequestMapping("/api/v1/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 创建新对话
     * 后端生成 chatId，写入 MySQL，返回 chatId 给前端
     */
    @PostMapping
    @Operation(
        summary = "创建新对话",
        description = "后端生成 chatId，创建对话记录并返回 chatId"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "创建成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(description = "包含 chatId 的响应"))
        )
    })
    public Map<String, String> createConversation(
        @Parameter(description = "用户ID（从JWT中获取，无需前端传递）")
        @RequestAttribute("userId") Long userId
    ) {
        String chatId = UUID.randomUUID().toString();
        conversationService.createConversation(userId, chatId);
        return Map.of("chatId", chatId);
    }

    /**
     * 分页查询当前用户的对话列表
     * 按最后更新时间倒排（最近对话在前）
     */
    @GetMapping("/list")
    @Operation(
        summary = "查询对话列表",
        description = "分页查询当前用户的对话列表，按最后更新时间倒排"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResult.class)))
    })
    public PageResult<ConversationListResponse> listConversations(
        @Parameter(description = "用户ID（从JWT中获取）")
        @RequestAttribute("userId") Long userId,
        @Parameter(description = "页码（从1开始）", example = "1")
        @RequestParam(value = "page", defaultValue = "1") int page,
        @Parameter(description = "每页大小", example = "10")
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return conversationService.listByUserId(userId, page, size);
    }

    /**
     * 获取指定对话的详细信息（含消息列表）
     */
    @GetMapping("/detail/{chatId}")
    @Operation(
        summary = "查询对话详情",
        description = "根据 chatId 获取对话详情，包含完整的消息列表"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ConversationDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    public ConversationDetailResponse getConversationDetail(
        @Parameter(description = "对话ID", required = true, example = "a1b2c3d4-...")
        @PathVariable String chatId
    ) {
        return conversationService.getDetail(chatId);
    }

    /**
     * 删除指定对话
     * 同时清除 MySQL 记录、Redis 缓存、绑定的文件集合
     */
    @DeleteMapping("/{chatId}")
    @Operation(
        summary = "删除对话",
        description = "删除指定对话（MySQL逻辑删除 + Redis清除缓存）"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "对话不存在")
    })
    public Map<String, String> deleteConversation(
        @Parameter(description = "要删除的对话ID", required = true)
        @PathVariable String chatId
    ) {
        conversationService.deleteConversation(chatId);
        return Map.of("message", "删除成功");
    }

    /**
     * 获取当前用户的对话统计信息
     */
    @GetMapping("/stats")
    @Operation(
        summary = "获取对话统计",
        description = "获取当前用户的对话总数和消息总数"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ConversationStatsResponse.class)))
    })
    public ConversationStatsResponse getConversationStats(
        @Parameter(description = "用户ID（从JWT中获取）")
        @RequestAttribute("userId") Long userId
    ) {
        return conversationService.getStats(userId);
    }
}
