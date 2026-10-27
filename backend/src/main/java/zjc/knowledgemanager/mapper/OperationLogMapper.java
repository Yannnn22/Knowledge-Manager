package zjc.knowledgemanager.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import zjc.knowledgemanager.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface OperationLogMapper {
    
    /**
     * 插入操作日志
     */
    int insert(OperationLog operationLog);
    
    /**
     * 分页查询操作日志
     */
    List<OperationLog> selectByPage(@Param("userId") Long userId,
                                   @Param("operation") String operation,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 分页查询操作日志（支持更多筛选条件）
     */
    List<OperationLog> selectByPageWithFilters(@Param("userId") Long userId,
                                              @Param("operation") String operation,
                                              @Param("username") String username,
                                              @Param("operationType") String operationType,
                                              @Param("status") String status,
                                              @Param("targetType") String targetType,
                                              @Param("targetId") Long targetId,
                                              @Param("knowledgeBaseId") Long knowledgeBaseId,
                                              @Param("query") String query,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询操作日志总数
     */
    Long countByCondition(@Param("userId") Long userId,
                         @Param("operation") String operation,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询操作日志总数（支持更多筛选条件）
     */
    Long countByConditionWithFilters(@Param("userId") Long userId,
                                    @Param("operation") String operation,
                                    @Param("username") String username,
                                    @Param("operationType") String operationType,
                                    @Param("status") String status,
                                    @Param("knowledgeBaseId") Long knowledgeBaseId,
                                    @Param("query") String query,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询敏感操作日志
     */
    List<OperationLog> selectSensitiveOperations();
    
    /**
     * 查询指定时间范围内的操作日志（用于导出）
     */
    List<OperationLog> selectForExport(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定时间范围内的操作日志（用于统计）
     */
    List<OperationLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}
