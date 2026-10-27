package zjc.knowledgemanager.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import zjc.knowledgemanager.interceptor.JwtInterceptor;
import zjc.knowledgemanager.interceptor.OperationLogInterceptor;

/**
 * Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;
    
    @Autowired
    private OperationLogInterceptor operationLogInterceptor;
    
    @org.springframework.beans.factory.annotation.Value("${file.upload.base-dir:./uploads}")
    private String uploadBaseDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT认证拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/users/**", "/api/v1/knowledge-bases/**", "/api/v1/documents/**", "/api/v1/operation-logs/**", "/ai/stats")
                .excludePathPatterns("/api/v1/auth/login");
        
        // 操作日志拦截器
        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/login", "/api/v1/test/**");
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // 允许前端直接调用 AI/RAG 相关接口
        registry.addMapping("/ai/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // 允许前端直接调用向量化/检索/删除接口
        registry.addMapping("/ingest/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // 兜底：放开所有路径的预检（避免某些OPTIONS未命中具体映射导致403）
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = uploadBaseDir;
        if (baseDir == null || baseDir.isEmpty()) {
            baseDir = System.getProperty("file.upload.base-dir");
        }
        if (baseDir == null || baseDir.isEmpty()) {
            baseDir = System.getenv("FILE_UPLOAD_BASE_DIR");
        }
        if (baseDir == null || baseDir.isEmpty()) {
            baseDir = "./uploads";
        }
        if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + baseDir);
    }

    /**
     * 将 Long/long 统一序列化为字符串，避免前端精度丢失。
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(module);
        // 支持 Java 8 时间类型，使用 ISO 格式输出
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 全局忽略未知字段（前端额外传 page/size 等不致失败）
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}