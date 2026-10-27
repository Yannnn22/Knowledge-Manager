package zjc.knowledgemanager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.dto.BatchDeleteResult;
import zjc.knowledgemanager.dto.CreateKnowledgeBaseRequest;
import zjc.knowledgemanager.dto.KnowledgeBaseDetailResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseListResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseStatsResponse;
import zjc.knowledgemanager.dto.UpdateKnowledgeBaseRequest;
import zjc.knowledgemanager.entity.KnowledgeBase;
import zjc.knowledgemanager.entity.User;
import zjc.knowledgemanager.mapper.KnowledgeBaseMapper;
import zjc.knowledgemanager.mapper.UserMapper;
import zjc.knowledgemanager.service.KnowledgeBaseService;
import zjc.knowledgemanager.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 知识库服务实现类
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public Long createKnowledgeBase(Long userId, CreateKnowledgeBaseRequest request) {
        // 检查知识库名称是否已存在
        if (knowledgeBaseMapper.countByName(request.getName()) > 0) {
            throw new RuntimeException("知识库名称已存在");
        }

        // 创建知识库对象
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(idGenerator.nextId());
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setOwnerId(userId);
        knowledgeBase.setDepartment(request.getDepartment());
        knowledgeBase.setIsDeleted(0);

        // 插入数据库
        int result = knowledgeBaseMapper.insert(knowledgeBase);
        if (result <= 0) {
            throw new RuntimeException("创建知识库失败");
        }

        return knowledgeBase.getId();
    }

    @Override
    public KnowledgeBaseListResponse getKnowledgeBaseList(Long userId, String department, boolean includeAll) {
        List<KnowledgeBase> knowledgeBaseList;

        if (includeAll) {
            knowledgeBaseList = knowledgeBaseMapper.findAll();
        } else if (department != null && !department.trim().isEmpty()) {
            knowledgeBaseList = knowledgeBaseMapper.findByOwnerIdAndDepartment(userId, department);
        } else {
            knowledgeBaseList = knowledgeBaseMapper.findByOwnerId(userId);
        }

        // 转换为DTO
        List<KnowledgeBaseListResponse.KnowledgeBaseInfo> knowledgeBaseInfoList = knowledgeBaseList.stream()
                .map(kb -> {
                    // 获取所有者信息
                    User owner = userMapper.findById(kb.getOwnerId());
                    String ownerName = owner != null ? owner.getRealName() : "未知用户";

                    // 获取文档数量
                    int documentCount = knowledgeBaseMapper.getDocumentCount(kb.getId());

                    KnowledgeBaseListResponse.KnowledgeBaseInfo info = new KnowledgeBaseListResponse.KnowledgeBaseInfo(
                            kb.getId(),
                            kb.getName(),
                            ownerName,
                            kb.getOwnerId(),
                            kb.getDepartment(),
                            kb.getCreateTime(),
                            kb.getUpdateTime(),
                            documentCount,
                            false,
                            false
                    );
                    User cur = userMapper.findById(userId);
                    boolean isAdmin = cur != null && cur.isAdmin();
                    String ownerIdStr = String.valueOf(kb.getOwnerId());
                    String curIdStr = cur == null ? "" : String.valueOf(cur.getId());
                    boolean can = isAdmin || ownerIdStr.equals(curIdStr);
                    info.setCanEdit(can);
                    info.setCanDelete(can);
                    return info;
                })
                .collect(java.util.stream.Collectors.toList());

        return new KnowledgeBaseListResponse(knowledgeBaseInfoList);
    }

    @Override
    public KnowledgeBaseDetailResponse getKnowledgeBaseDetail(Long userId, Long knowledgeBaseId) {
        // 查询知识库
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new RuntimeException("知识库不存在");
        }

        // 权限检查：管理员或知识库所有者可以访问
        User currentUser = userMapper.findById(userId);
        if (currentUser == null) {
            throw new SecurityException("用户不存在: " + userId);
        }

        boolean isAdmin = currentUser.isAdmin();
        boolean isOwner = knowledgeBase.getOwnerId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("无权限访问该知识库");
        }

        // 获取所有者信息
        User owner = userMapper.findById(knowledgeBase.getOwnerId());
        String ownerName = owner != null ? owner.getRealName() : "未知用户";

        return new KnowledgeBaseDetailResponse(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                ownerName,
                knowledgeBase.getCreateTime()
        );
    }

    @Override
    public KnowledgeBaseStatsResponse getKnowledgeBaseStats(Long userId, Long knowledgeBaseId) {
        // 查询知识库
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new RuntimeException("知识库不存在");
        }

        // 权限检查：管理员或知识库所有者可以访问
        User currentUser = userMapper.findById(userId);
        if (currentUser == null) {
            throw new SecurityException("用户不存在: " + userId);
        }

        boolean isAdmin = currentUser.isAdmin();
        boolean isOwner = knowledgeBase.getOwnerId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("无权限访问该知识库");
        }

        // 获取统计信息
        KnowledgeBaseMapper.KnowledgeBaseStats stats = knowledgeBaseMapper.getStats(knowledgeBaseId);
        
        if (stats == null) {
            // 如果没有文档，返回默认值
            return new KnowledgeBaseStatsResponse(0, 0, 0, 0);
        }

        return new KnowledgeBaseStatsResponse(
                stats.getTotalDocuments(),
                stats.getProcessing(),
                stats.getCompleted(),
                stats.getFailed()
        );
    }

    private void checkKbPermissionOr403(KnowledgeBase kb, User currentUser) {
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        String ownerId = String.valueOf(kb.getOwnerId());
        String currentId = currentUser == null ? "" : String.valueOf(currentUser.getId());
        if (!(isAdmin || ownerId.equals(currentId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限: 只有管理员或所有者可以操作知识库");
        }
    }

    @Override
    public KnowledgeBaseDetailResponse updateKnowledgeBase(Long userId, Long id, UpdateKnowledgeBaseRequest request) {
        // 获取知识库信息
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(id);
        if (knowledgeBase == null || knowledgeBase.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        // 当前用户
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        // 权限检查
        checkKbPermissionOr403(knowledgeBase, user);

        // 名称冲突检查（排除自身）
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            int count = knowledgeBaseMapper.countByNameExcludingId(request.getName().trim(), id);
            if (count > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "名称冲突: 知识库名称已存在");
            }
        }

        KnowledgeBase updateKb = new KnowledgeBase();
        updateKb.setId(id);
        updateKb.setName(request.getName());
        updateKb.setDescription(request.getDescription());
        updateKb.setDepartment(request.getDepartment());

        int result = knowledgeBaseMapper.updateSelective(updateKb);
        if (result <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新知识库失败");
        }

        return getKnowledgeBaseDetail(userId, id);
    }

    @Override
    public void deleteKnowledgeBase(Long userId, Long id) {
        // 获取知识库信息
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.findById(id);
        if (knowledgeBase == null || knowledgeBase.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        // 当前用户
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        // 权限检查
        checkKbPermissionOr403(knowledgeBase, user);

        int totalCount = knowledgeBaseMapper.getDocumentCount(id);
        if (totalCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "无法删除: 知识库中仍有 " + totalCount + " 个文档，请先删除或移动这些文档");
        }

        int result = knowledgeBaseMapper.softDelete(id);
        if (result <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除知识库失败");
        }
    }

    @Override
    public BatchDeleteResult batchDeleteKnowledgeBases(Long userId, List<Long> ids) {
        BatchDeleteResult result = new BatchDeleteResult();
        List<Long> ok = new java.util.ArrayList<>();
        List<String> failed = new java.util.ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            result.setSuccessIds(ok);
            result.setFailed(failed);
            return result;
        }
        for (Long id : ids) {
            try {
                deleteKnowledgeBase(userId, id);
                ok.add(id);
            } catch (Exception e) {
                failed.add(id + ": " + e.getMessage());
            }
        }
        result.setSuccessIds(ok);
        result.setFailed(failed);
        return result;
    }
} 