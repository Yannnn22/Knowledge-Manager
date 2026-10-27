package zjc.knowledgemanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ModelFallBackService {

    private static final Logger log = LoggerFactory.getLogger(ModelFallBackService.class);

    private static final String FALLBACK_KEY_PREFIX = "model_fallback:";
    private static final int FAILURE_THRESHOLD = 3;
    private static final int FALLBACK_DURATION_MINUTES = 10;

    private final StringRedisTemplate redis;
    
    @Value("${app.ai.default-model:Qwen/Qwen3.5-27B}")
    private String primaryModel;
    
    @Value("${app.ai.fallback-model:Qwen/Qwen3.5-9B}")
    private String fallbackModel;

    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();

    public ModelFallBackService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String getModel(String model) {
        if (!model.equals(primaryModel)) {
            return model;
        }

        String redisKey = FALLBACK_KEY_PREFIX + primaryModel;
        String isFallback = redis.opsForValue().get(redisKey);
        
        if ("true".equals(isFallback)) {
            log.warn("主模型 {} 已降级到 {}", primaryModel, fallbackModel);
            return fallbackModel;
        }

        return primaryModel;
    }

    public void recordFailure(String model) {
        if (!model.equals(primaryModel)) {
            return;
        }

        int count = failureCounts.merge(model, 1, Integer::sum);
        log.warn("主模型 {} 失败次数: {}", primaryModel, count);

        if (count >= FAILURE_THRESHOLD) {
            triggerFallback();
        }
    }

    public void recordSuccess(String model) {
        if (!model.equals(primaryModel)) {
            return;
        }

        failureCounts.put(model, 0);
        
        String redisKey = FALLBACK_KEY_PREFIX + primaryModel;
        redis.delete(redisKey);
        
        log.info("主模型 {} 恢复正常", primaryModel);
    }

    private void triggerFallback() {
        String redisKey = FALLBACK_KEY_PREFIX + primaryModel;
        redis.opsForValue().set(redisKey, "true", FALLBACK_DURATION_MINUTES, TimeUnit.MINUTES);
        
        failureCounts.put(primaryModel, 0);
        
        log.error("触发模型降级: {} → {}, 持续 {} 分钟", 
                primaryModel, fallbackModel, FALLBACK_DURATION_MINUTES);
    }

    public void clearFallback() {
        String redisKey = FALLBACK_KEY_PREFIX + primaryModel;
        redis.delete(redisKey);
        failureCounts.put(primaryModel, 0);
        log.info("手动清除降级状态");
    }
}
