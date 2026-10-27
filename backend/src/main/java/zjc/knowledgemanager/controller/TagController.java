package zjc.knowledgemanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import zjc.knowledgemanager.common.ApiResponse;
import zjc.knowledgemanager.dto.TagDTO;
import zjc.knowledgemanager.service.TagService;
import zjc.knowledgemanager.util.AuthUtil;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "标签管理", description = "标签管理相关接口")
public class TagController {

    @Autowired
    private TagService tagService;

    @PostMapping
    @Operation(summary = "创建标签", description = "创建新的标签")
    public ApiResponse<TagDTO> createTag(@Valid @RequestBody TagDTO tagDTO, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            tagDTO.setCreatorId(userId);
            TagDTO createdTag = tagService.createTag(tagDTO);
            return ApiResponse.success(createdTag);
        } catch (Exception e) {
            return ApiResponse.error("创建标签失败: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "获取所有标签", description = "获取系统中所有可用的标签")
    public ApiResponse<List<TagDTO>> getAllTags() {
        try {
            List<TagDTO> tags = tagService.getAllTags();
            return ApiResponse.success(tags);
        } catch (Exception e) {
            return ApiResponse.error("获取标签列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取标签详情", description = "获取指定标签的详细信息")
    public ApiResponse<TagDTO> getTagDetail(@PathVariable Long id) {
        try {
            TagDTO tag = tagService.getTagById(id);
            if (tag == null) {
                return ApiResponse.error(404, "标签不存在");
            }
            return ApiResponse.success(tag);
        } catch (Exception e) {
            return ApiResponse.error("获取标签详情失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签", description = "更新标签信息")
    public ApiResponse<TagDTO> updateTag(@PathVariable Long id, @RequestBody TagDTO tagDTO, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            TagDTO updatedTag = tagService.updateTag(id, tagDTO, userId);
            return ApiResponse.success(updatedTag);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("更新标签失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签", description = "删除指定的标签")
    public ApiResponse<String> deleteTag(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = AuthUtil.getUserId(request);
            boolean success = tagService.deleteTag(id, userId);
            if (success) {
                return ApiResponse.success("标签删除成功");
            } else {
                return ApiResponse.error(404, "标签不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("删除标签失败: " + e.getMessage());
        }
    }
}