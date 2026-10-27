package zjc.knowledgemanager.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.dto.ConversationDetailResponse;
import zjc.knowledgemanager.dto.ConversationListResponse;
import zjc.knowledgemanager.dto.ConversationStatsResponse;
import zjc.knowledgemanager.dto.PageResult;
import zjc.knowledgemanager.entity.Conversation;
import zjc.knowledgemanager.entity.Message;
import zjc.knowledgemanager.exception.AppException;
import zjc.knowledgemanager.mapper.ConversationMapper;
import zjc.knowledgemanager.mapper.MessageMapper;
import zjc.knowledgemanager.service.ConversationService;
import zjc.knowledgemanager.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话历史服务实现类
 * 存储策略: MySQL 持久化 + Redis 热缓存
 * 写入: 消息 -> MySQL(持久化) -> Redis缓存(热数据)
 * 读取: 先查Redis -> miss则查MySQL -> 回填Redis(由 Spring AI ChatMemory 自动完成)
 * 清除: MySQL删除 + Redis删除
 */
@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
public class ConversationServiceImpl implements ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private static final String CHAT_MEMORY_PREFIX = "spring_ai_alibaba_chat_memory:";
    private static final String ALL_CHAT_IDS_KEY = "all_chat_ids";

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public ConversationServiceImpl(ConversationMapper conversationMapper,
                                   MessageMapper messageMapper,
                                   StringRedisTemplate stringRedisTemplate,
                                   IdGenerator idGenerator) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.idGenerator = idGenerator;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Conversation createConversation(Long userId, String chatId) {
        // 先查是否已存在
        Conversation existing = conversationMapper.findByChatId(chatId);
        if (existing != null) {
            return existing;
        }
        try {
            Conversation conversation = new Conversation(idGenerator.nextId(), chatId, userId);
            conversation.setCreatedAt(LocalDateTime.now());
            int rows = conversationMapper.insert(conversation);
            if (rows <= 0) {
                throw new AppException("创建对话失败");
            }
            // 更新 Redis 全局 chatId 集合
            try {
                stringRedisTemplate.opsForSet().add(ALL_CHAT_IDS_KEY, chatId);
            } catch (Exception ignored) {}
            return conversation;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建对话失败: chatId={}, userId={}", chatId, userId, e);
            throw new AppException("创建对话失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<ConversationListResponse> listByUserId(Long userId, int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<Conversation> conversations = conversationMapper.selectPageByUserId(userId, offset, size);
            int total = conversationMapper.countByUserId(userId);

            List<ConversationListResponse> list = new ArrayList<>();
            for (Conversation conv : conversations) {
                int msgCount = messageMapper.countByChatId(conv.getChatId());
                String lastMsg = "";
                Message lastMessage = messageMapper.findLastByChatId(conv.getChatId());
                if (lastMessage != null && lastMessage.getContent() != null) {
                    lastMsg = lastMessage.getContent().length() > 100
                            ? lastMessage.getContent().substring(0, 100) + "..."
                            : lastMessage.getContent();
                }
                list.add(new ConversationListResponse(
                        conv.getChatId(),
                        conv.getTitle() != null ? conv.getTitle() : "新对话",
                        msgCount,
                        lastMsg,
                        conv.getUpdatedAt()
                ));
            }
            return PageResult.of(list, (long) total, page, size);
        } catch (Exception e) {
            log.error("查询对话列表失败: userId={}", userId, e);
            throw new AppException("查询对话列表失败: " + e.getMessage());
        }
    }

    @Override
    public ConversationDetailResponse getDetail(String chatId) {
        try {
            Conversation conversation = conversationMapper.findByChatId(chatId);
            if (conversation == null) {
                throw new AppException("对话不存在: " + chatId);
            }
            List<Message> messages = messageMapper.findByChatId(chatId);
            List<ConversationDetailResponse.MessageDTO> messageDTOs = new ArrayList<>();
            for (Message msg : messages) {
                messageDTOs.add(new ConversationDetailResponse.MessageDTO(
                        msg.getRole(), msg.getContent(), msg.getModel(), msg.getCreatedAt()
                ));
            }
            return new ConversationDetailResponse(
                    chatId,
                    conversation.getTitle() != null ? conversation.getTitle() : "新对话",
                    messages.size(),
                    messageDTOs
            );
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询对话详情失败: chatId={}", chatId, e);
            throw new AppException("查询对话详情失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteConversation(String chatId) {
        try {
            // 1. 逻辑删除 MySQL 记录
            conversationMapper.deleteByChatId(chatId);
            messageMapper.deleteByChatId(chatId);
            // 2. 删除 Redis 缓存
            stringRedisTemplate.delete(CHAT_MEMORY_PREFIX + chatId);
            stringRedisTemplate.opsForSet().remove("chat_files:" + chatId);
            stringRedisTemplate.opsForSet().remove(ALL_CHAT_IDS_KEY, chatId);
        } catch (Exception e) {
            log.error("删除对话失败: chatId={}", chatId, e);
            throw new AppException("删除对话失败: " + e.getMessage());
        }
    }

    @Override
    public void saveMessage(String chatId, Long userId, String role, String content, String model) {
        try {
            // 确保 conversation 存在
            Conversation conversation = conversationMapper.findByChatId(chatId);
            if (conversation == null) {
                conversation = createConversation(userId, chatId);
            }
            // 插入消息
            Message message = new Message(idGenerator.nextId(), chatId, role, content, model);
            message.setConversationId(conversation.getId());
            message.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(message);
            // 如果是第一条用户消息，设置对话标题
            if (Message.ROLE_USER.equals(role) && conversation.getTitle() == null) {
                String title = content != null && content.length() > 50
                        ? content.substring(0, 50) + "..."
                        : content;
                conversationMapper.updateTitle(conversation.getId(), title);
            }
        } catch (Exception e) {
            log.error("保存消息失败: chatId={}, role={}", chatId, role, e);
            // 消息保存失败不抛异常，避免影响聊天流程
        }
    }

    @Override
    public ConversationStatsResponse getStats(Long userId) {
        try {
            int totalConversations = conversationMapper.countByUserId(userId);
            // 统计该用户所有对话的消息总数
            int totalMessages = 0;
            List<Conversation> conversations = conversationMapper.selectPageByUserId(userId, 0, Integer.MAX_VALUE);
            for (Conversation conv : conversations) {
                totalMessages += messageMapper.countByChatId(conv.getChatId());
            }
            return new ConversationStatsResponse(totalConversations, totalMessages);
        } catch (Exception e) {
            log.error("获取对话统计失败: userId={}", userId, e);
            throw new AppException("获取对话统计失败: " + e.getMessage());
        }
    }
}
