package zjc.knowledgemanager.service;

import zjc.knowledgemanager.dto.ConversationDetailResponse;
import zjc.knowledgemanager.dto.ConversationListResponse;
import zjc.knowledgemanager.dto.ConversationStatsResponse;
import zjc.knowledgemanager.dto.PageResult;
import zjc.knowledgemanager.entity.Conversation;

/**
 * 对话历史服务接口
 * 处理对话历史的查询、删除等业务逻辑
 * 存储策略: MySQL 持久化 + Redis 热缓存
 */
public interface ConversationService {

    /**
     * 创建新对话(或获取已存在的对话)
     * @param userId 用户ID
     * @param chatId 对话UUID
     * @return 对话实体
     * @throws zjc.knowledgemanager.exception.AppException 如果创建失败
     */
    Conversation createConversation(Long userId, String chatId);

    /**
     * 按用户ID分页查询对话列表
     * @param userId 用户ID
     * @param page 页码(从1开始)
     * @param size 每页大小
     * @return 分页结果
     * @throws zjc.knowledgemanager.exception.AppException 如果查询失败
     */
    PageResult<ConversationListResponse> listByUserId(Long userId, int page, int size);

    /**
     * 获取指定对话的详细信息(含消息列表)
     * @param chatId 对话UUID
     * @return 对话详情(含消息)
     * @throws zjc.knowledgemanager.exception.AppException 如果对话不存在或获取失败
     */
    ConversationDetailResponse getDetail(String chatId);

    /**
     * 删除指定对话(MySQL + Redis + 关联文件绑定)
     * @param chatId 对话UUID
     * @throws zjc.knowledgemanager.exception.AppException 如果对话不存在或删除失败
     */
    void deleteConversation(String chatId);

    /**
     * 保存单条消息(写入MySQL + 更新Redis缓存)
     * @param chatId 对话UUID
     * @param userId 用户ID(用于自动创建conversation)
     * @param role 消息角色(user/assistant)
     * @param content 消息内容
     * @param model AI模型名称
     * @throws zjc.knowledgemanager.exception.AppException 如果保存失败
     */
    void saveMessage(String chatId, Long userId, String role, String content, String model);

    /**
     * 获取对话统计信息
     * @param userId 用户ID
     * @return 统计信息
     * @throws zjc.knowledgemanager.exception.AppException 如果获取失败
     */
    ConversationStatsResponse getStats(Long userId);
}
