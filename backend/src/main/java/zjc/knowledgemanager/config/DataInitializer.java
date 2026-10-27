package zjc.knowledgemanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import zjc.knowledgemanager.entity.User;
import zjc.knowledgemanager.mapper.UserMapper;

/**
 * 数据库初始化器 — 在应用启动时自动检查并创建默认管理员账号。
 * 仅在管理员用户不存在时执行，不会覆盖已有数据。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
    private static final String DEFAULT_ADMIN_REAL_NAME = "管理员";
    private static final String DEFAULT_ADMIN_DEPARTMENT = "技术部";

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        try {
            User existing = userMapper.findByUsername(DEFAULT_ADMIN_USERNAME);
            if (existing != null) {
                log.info("管理员账号已存在: username={}, status={}", existing.getUsername(), existing.getStatus());
                return;
            }

            User admin = new User();
            admin.setId(1L);
            admin.setUsername(DEFAULT_ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
            admin.setRealName(DEFAULT_ADMIN_REAL_NAME);
            admin.setDepartment(DEFAULT_ADMIN_DEPARTMENT);
            admin.setStatus(User.STATUS_ADMIN);
            admin.setIsDeleted(0);

            userMapper.insert(admin);
            log.info("默认管理员账号已创建: username={}, password=******, status=管理员(2)", DEFAULT_ADMIN_USERNAME);
        } catch (Exception e) {
            log.warn("初始化管理员账号失败（可能数据库未就绪）: {}", e.getMessage());
        }
    }
}
