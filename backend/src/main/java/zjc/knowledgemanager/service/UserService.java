package zjc.knowledgemanager.service;

import zjc.knowledgemanager.dto.*;
import zjc.knowledgemanager.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 创建用户
     */
    Long createUser(CreateUserRequest request);
    
    /**
     * 分页查询用户
     */
    UserPageResponse getUserPage(Integer page, Integer size, String department, Integer status);
    
    /**
     * 更新用户信息
     */
    boolean updateUser(Long id, UpdateUserRequest request);
    
    /**
     * 删除用户
     */
    boolean deleteUser(Long id);
    
    /**
     * 获取用户个人资料
     */
    UserProfileDTO getUserProfile(Long userId);
    
    /**
     * 更新用户个人资料
     */
    boolean updateUserProfile(UserProfileDTO request);
    
    /**
     * 修改用户密码
     */
    boolean updatePassword(Long userId, UpdatePasswordRequest request);
    
    /**
     * 根据用户ID获取用户信息
     */
    User getUserById(Long userId);
}