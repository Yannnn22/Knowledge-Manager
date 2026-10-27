package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.Conversation;

import java.util.List;

/**
 * 对话Mapper接口
 */
@Mapper
public interface ConversationMapper {

    /**
     * 根据ID查询对话
     */
    Conversation findById(Long id);

    /**
     * 根据chatId查询对话
     */
    Conversation findByChatId(String chatId);

    /**
     * 插入对话
     */
    int insert(Conversation conversation);

    /**
     * 更新对话标题
     */
    int updateTitle(@Param("id") Long id, @Param("title") String title);

    /**
     * 按用户ID分页查询对话列表
     */
    List<Conversation> selectPageByUserId(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 按用户ID统计对话数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 逻辑删除对话
     */
    int deleteById(Long id);

    /**
     * 逻辑删除对话(按chatId)
     */
    int deleteByChatId(String chatId);

    /**
     * 按用户ID统计今日对话数量
     */
    int countTodayByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID统计本月对话数量
     */
    int countMonthByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID统计今日消息数量
     */
    int countTodayMessagesByUserId(@Param("userId") Long userId);

    /**
     * 统计所有用户的总对话数
     */
    int countAll();

    /**
     * 统计今日新增对话数
     */
    int countTodayAll();
}
