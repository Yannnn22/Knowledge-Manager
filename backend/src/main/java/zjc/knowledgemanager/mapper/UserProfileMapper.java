package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import zjc.knowledgemanager.entity.UserProfile;

/**
 * 用户个人资料Mapper接口
 */
@Mapper
public interface UserProfileMapper {
    
    /**
     * 根据用户ID查询个人资料
     */
    UserProfile findByUserId(Long userId);
    
    /**
     * 插入个人资料
     */
    int insert(UserProfile userProfile);
    
    /**
     * 更新个人资料
     */
    int update(UserProfile userProfile);
    
    /**
     * 根据用户ID删除个人资料
     */
    int deleteByUserId(Long userId);
}