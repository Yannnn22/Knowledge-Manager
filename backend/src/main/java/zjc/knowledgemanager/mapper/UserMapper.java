package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.User;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 根据ID查询用户
     */
    User findById(Long id);
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 逻辑删除用户
     */
    int deleteById(Long id);
    
    /**
     * 分页查询用户列表
     */
    List<User> selectPage(@Param("department") String department, 
                          @Param("status") Integer status);
    
    /**
     * 检查用户名是否存在
     */
    int countByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    int countByEmail(String email);
} 