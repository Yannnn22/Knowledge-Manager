import http from '@/utils/http';

// 与后端 DTO 对齐的类型
export interface OperationLogItem {
  id: number;
  operation: string;
  targetType: string;
  targetId: number;
  operationTime: string; // yyyy-MM-dd HH:mm:ss
  user?: {
    id: number;
    username: string;
    realName?: string;
    department?: string;
  };
  ipAddress?: string;
  detail?: string;
}

export interface OperationLogListResponse {
  total: number;
  logs: OperationLogItem[];
}

export interface OperationLogQueryParams {
  page?: number; // > 0
  size?: number; // > 0
  userId?: number;
  username?: string;
  operation?: string;
  operationType?: string; // 前端辅助
  status?: string; // 前端辅助
  targetType?: string;
  targetId?: number;
  knowledgeBaseId?: number;
  query?: string;
  startTime?: string; // yyyy-MM-dd HH:mm:ss
  endTime?: string;   // yyyy-MM-dd HH:mm:ss
}

// 查询操作日志（分页）
export const getOperationLogs = async (params: OperationLogQueryParams): Promise<{ data: OperationLogListResponse } | OperationLogListResponse> => {
  // 使用新的筛选接口
  return http.get('/v1/operation-logs/filter', { params });
};

// 导出CSV（管理员）
export const exportOperationLogs = async (params: { startTime?: string; endTime?: string }): Promise<Blob> => {
  const res = await http.get('/v1/operation-logs/export', { params, responseType: 'blob' });
  return res as unknown as Blob;
};

// 敏感操作（管理员）
export const getSensitiveLogs = async (params?: Partial<OperationLogQueryParams>) => {
  return http.get('/v1/operation-logs/sensitive', { params });
};

// 操作日志统计数据类型 - 匹配后端实际返回的字段名
export interface OperationLogStats {
  today_logs: number;
  sensitive_operations: number;
  active_users: number;
  error_records: number;
}

/**
 * 获取操作日志统计信息
 */
export const getOperationLogStatistics = async (): Promise<{ data: OperationLogStats }> => {
  try {
    // 调用真实的后端API
    const response = await http.get('/v1/operation-logs/statistics');
    return response;
  } catch (error) {
    console.error('获取操作日志统计信息失败:', error);
    // 如果API调用失败，返回默认值
    return {
      data: {
        today_logs: 0,
        sensitive_operations: 0,
        active_users: 0,
        error_records: 0,
      },
    };
  }
};

// 注意：API.md中没有获取用户列表接口，这个可能需要后端实现
