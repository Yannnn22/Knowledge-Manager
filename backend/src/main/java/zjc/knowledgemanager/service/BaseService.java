package zjc.knowledgemanager.service;

/**
 * 基础服务接口
 * 提供模型验证和温度参数验证功能
 */
public interface BaseService {

    /**
     * 验证模型是否支持
     * @param model 用户指定的模型名称
     * @return 验证后的模型名称（如果为空则返回默认模型）
     * @throws zjc.knowledgemanager.exception.AppException 如果模型不支持
     */
    String validateAndGetModel(String model);

    /**
     * 验证并返回温度值
     * 允许范围：0.0 ~ 2.0；未传时默认 0.8
     * @param temperature 温度参数
     * @return 验证后的温度值
     * @throws zjc.knowledgemanager.exception.AppException 如果温度值超出范围
     */
    double validateAndGetTemperature(Double temperature);
}

