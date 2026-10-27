package zjc.knowledgemanager.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.dto.*;
import zjc.knowledgemanager.entity.User;
import zjc.knowledgemanager.entity.UserProfile;
import zjc.knowledgemanager.mapper.UserMapper;
import zjc.knowledgemanager.mapper.UserProfileMapper;
import zjc.knowledgemanager.service.UserService;
import zjc.knowledgemanager.util.IdGenerator;
import zjc.knowledgemanager.util.JwtUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private IdGenerator idGenerator;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {
        // 根据用户名查询用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查用户状态：仅启用/在职可登录
        if (!user.isEnabled()) {
            throw new RuntimeException("用户被禁用或已离职");
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return new LoginResponse(token, user.getId());
    }

    @Override
    public Long createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userMapper.countByEmail(request.getEmail()) > 0) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建用户对象
        User user = new User();
        user.setId(idGenerator.nextId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRealName(request.getRealName());
        user.setDepartment(request.getDepartment());
        user.setStatus(1); // 默认启用
        user.setIsDeleted(0); // 默认未删除

        // 插入数据库
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("创建用户失败");
        }

        return user.getId();
    }

    @Override
    public UserPageResponse getUserPage(Integer page, Integer size, String department, Integer status) {
        // 设置分页参数
        PageHelper.startPage(page, size);

        // 查询用户列表
        List<User> userList = userMapper.selectPage(department, status);

        // 获取分页信息
        PageInfo<User> pageInfo = new PageInfo<>(userList);

        // 转换为DTO
        List<UserPageResponse.UserInfo> userInfoList = userList.stream()
                .map(user -> new UserPageResponse.UserInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getDepartment(),
                        user.getStatus(),
                        user.getCreateTime()
                ))
                .collect(Collectors.toList());

        return new UserPageResponse(pageInfo.getTotal(), userInfoList);
    }

    @Override
    public boolean updateUser(Long id, UpdateUserRequest request) {
        // 检查用户是否存在
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 如果更新邮箱，检查邮箱是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
            if (userMapper.countByEmail(request.getEmail()) > 0) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
        }

        // 创建更新对象
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setEmail(request.getEmail());
        updateUser.setRealName(request.getRealName());
        updateUser.setDepartment(request.getDepartment());
        updateUser.setStatus(request.getStatus());

        // 更新数据库
        int result = userMapper.update(updateUser);
        return result > 0;
    }

    @Override
    public boolean deleteUser(Long id) {
        // 检查用户是否存在
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 逻辑删除用户
        int result = userMapper.deleteById(id);
        return result > 0;
    }
    
    @Override
    public UserProfileDTO getUserProfile(Long userId) {
        // 获取用户基本信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取用户个人资料
        UserProfile profile = userProfileMapper.findByUserId(userId);
        
        // 构建响应DTO
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(userId);
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        
        // 设置管理员标识
        dto.setIsAdmin(user.isAdmin());
        
        if (profile != null) {
            dto.setAvatarUrl(profile.getAvatarUrl());
            dto.setPhone(profile.getPhone());
            dto.setBio(profile.getBio());
            dto.setLocation(profile.getLocation());
            dto.setWebsite(profile.getWebsite());
            dto.setPreferredModel(profile.getPreferredModel());
            dto.setCustomApiKey(profile.getCustomApiKey());
            dto.setNotificationEmail(profile.getNotificationEmail() == 1);
            dto.setNotificationSystem(profile.getNotificationSystem() == 1);
        }
        
        return dto;
    }
    
    @Override
    public boolean updateUserProfile(UserProfileDTO request) {
        Long userId = request.getUserId();
        
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户基本信息
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        userMapper.update(user);
        
        // 更新或创建用户个人资料
        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            // 创建新的个人资料记录
            profile = new UserProfile(userId);
            profile.setId(idGenerator.nextId());
        }
        
        // 更新个人资料字段
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }
        if (request.getPreferredModel() != null) {
            profile.setPreferredModel(request.getPreferredModel());
        }
        if (request.getCustomApiKey() != null) {
            profile.setCustomApiKey(request.getCustomApiKey());
        }
        if (request.getNotificationEmail() != null) {
            profile.setNotificationEmail(request.getNotificationEmail() ? 1 : 0);
        }
        if (request.getNotificationSystem() != null) {
            profile.setNotificationSystem(request.getNotificationSystem() ? 1 : 0);
        }
        
        // 保存或更新个人资料
        if (userProfileMapper.findByUserId(userId) == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.update(profile);
        }
        
        return true;
    }
    
    @Override
    public boolean updatePassword(Long userId, UpdatePasswordRequest request) {
        // 验证新密码和确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("新密码和确认密码不一致");
        }
        
        // 获取用户信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证当前密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        int result = userMapper.update(user);
        
        return result > 0;
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.findById(userId);
    }
}