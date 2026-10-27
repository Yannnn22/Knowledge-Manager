package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.Message;

import java.util.List;

/**
 * 消息Mapper接口
 */
@Mapper
public interface MessageMapper {

    /**
     * 根据chatId查询消息列表(按时间正序)
     */
    List<Message> findByChatId(String chatId);

    /**
     * 插入单条消息
     */
    int insert(Message message);

    /**
     * 批量插入消息
     */
    int insertBatch(@Param("list") List<Message> messages);

    /**
     * 按chatId统计消息数量
     */
    int countByChatId(String chatId);

    /**
     * 逻辑删除指定chatId的所有消息
     */
    int deleteByChatId(String chatId);

    /**
     * 删除指定conversationId的所有消息
     */
    int deleteByConversationId(Long conversationId);

    /**
     * 按chatId查询最后一条消息
     */
    Message findLastByChatId(String chatId);

    /**
     * 按chatId分页查询消息（用于摘要压缩）
     * @param chatId 对话ID
     * @param offset 偏移量
     * @param limit  查询条数
     */
    List<Message> selectByChatIdRange(@Param("chatId") String chatId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);
}
