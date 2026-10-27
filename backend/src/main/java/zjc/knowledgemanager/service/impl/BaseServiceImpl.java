package zjc.knowledgemanager.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import zjc.knowledgemanager.exception.AppException;
import zjc.knowledgemanager.service.BaseService;
import zjc.knowledgemanager.util.ModelsUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 基础服务实现类
 * 提供模型验证和温度参数验证功能的具体实现
 */
@Service
public class BaseServiceImpl implements BaseService {

    @Value("${app.ai.default-model:Qwen/Qwen3.5-27B}")
    private String defaultModel;

    @Value("${app.ai.default-temperature:0.8}")
    private double defaultTemperature;

    /**
     * 验证模型是否支持
     * @param model 用户指定的模型名称
     * @return 验证后的模型名称（如果为空则返回默认模型）
     * @throws AppException 如果模型不支持
     */
    @Override
    public String validateAndGetModel(String model) {
        // 获取所有可用的模型配置
        Set<Map<String, String>> models;
        try {
            models = ModelsUtils.getDashScopeModels();
        } catch (IOException e) {
            throw new AppException("Get models failed, " + e.getMessage());
        }

        // 从模型配置中提取所有模型名称
        List<String> modelNames = models.stream()
                .flatMap(map -> map.keySet().stream().map(map::get))
                .distinct()
                .toList();

        // 如果用户指定了模型，验证是否支持
        if (model != null && !model.trim().isEmpty()) {
            if (!modelNames.contains(model)) {
                throw new AppException("Input model not support: " + model);
            }
            return model;
        }
        
        // 如果没有指定模型，返回默认模型
        return defaultModel;
    }

    /**
     * 验证并返回温度值
     * 允许范围：0.0 ~ 2.0；未传时默认 0.8
     */
    @Override
    public double validateAndGetTemperature(Double temperature) {
        if (temperature == null) {
            return defaultTemperature;
        }
        double t = temperature.doubleValue();
        if (t < 0.0 || t > 2.0) {
            throw new AppException("Input temperature out of range [0.0, 2.0]: " + t);
        }
        return t;
    }
}
