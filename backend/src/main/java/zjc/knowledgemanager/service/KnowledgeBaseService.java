package zjc.knowledgemanager.service;

import zjc.knowledgemanager.dto.BatchDeleteResult;
import zjc.knowledgemanager.dto.CreateKnowledgeBaseRequest;
import zjc.knowledgemanager.dto.KnowledgeBaseDetailResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseListResponse;
import zjc.knowledgemanager.dto.KnowledgeBaseStatsResponse;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {
    
    /**
     * 创建知识库
     */
    Long createKnowledgeBase(Long userId, CreateKnowledgeBaseRequest request);
    
    /**
     * 获取知识库列表
     */
    KnowledgeBaseListResponse getKnowledgeBaseList(Long userId, String department, boolean includeAll);
    
    /**
     * 获取知识库详情
     */
    KnowledgeBaseDetailResponse getKnowledgeBaseDetail(Long userId, Long knowledgeBaseId);
    
    /**
     * 获取知识库统计信息
     */
    KnowledgeBaseStatsResponse getKnowledgeBaseStats(Long userId, Long knowledgeBaseId);

    /**
     * 更新知识库
     */
    KnowledgeBaseDetailResponse updateKnowledgeBase(Long userId, Long id, zjc.knowledgemanager.dto.UpdateKnowledgeBaseRequest request);

    /**
     * 删除知识库（软删）
     */
    void deleteKnowledgeBase(Long userId, Long id);

    /**
     * 批量删除知识库
     */
    BatchDeleteResult batchDeleteKnowledgeBases(Long userId, List<Long> ids);
} 