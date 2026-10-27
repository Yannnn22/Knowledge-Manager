// src/api/knowledgeBase.ts
import http from '@/utils/http';

// 获取知识库列表 - 根据API.md
export const getKnowledgeBases = async (params: { page?: number; size?: number; department?: string; name?: string; includeAll?: boolean }) => {
  try {
    // 默认携带 includeAll=true，除非调用方显式设置为 false
    const finalParams = { includeAll: true, ...(params || {}) } as Record<string, unknown>

    // 调用真实API
    const response = await http.get('/v1/knowledge-bases', { params: finalParams });
    
    // 处理响应数据
    if (response && typeof response === 'object') {
      // 如果响应包含data字段，说明是标准API响应格式
      if (response.data) {
        return response;
      }
      // 如果响应直接是数组，包装成标准格式
      if (Array.isArray(response)) {
        return {
          data: response,
          total: response.length,
          code: 200,
          message: 'success'
        };
      }
      // 如果是Spring Boot分页格式
      const pageResp = response as unknown as { content?: unknown[]; totalElements?: number };
      if (pageResp.content && Array.isArray(pageResp.content)) {
        return {
          data: pageResp.content,
          total: pageResp.totalElements ?? pageResp.content.length,
          code: 200,
          message: 'success'
        };
      }
    }
    
    return response;
  } catch (error) {
    console.error('获取知识库列表失败:', error);
    
    // 开发环境返回模拟数据
    if (import.meta.env.DEV) {
      const mockData: Array<Record<string, unknown>> = [
        {
          id: '1',
          name: '产品设计规范',
          description: '产品设计相关文档和规范',
          department: '设计部',
          documentCount: 42,
          createdAt: '2023-10-15T10:00:00Z',
          updatedAt: '2023-10-15T10:00:00Z',
          status: 'active'
        },
        {
          id: '2',
          name: '技术开发文档',
          description: '技术开发相关文档',
          department: '技术部',
          documentCount: 68,
          createdAt: '2023-10-18T14:30:00Z',
          updatedAt: '2023-10-18T14:30:00Z',
          status: 'active'
        },
        {
          id: '3',
          name: '市场运营资料',
          description: '市场运营相关文档',
          department: '市场部',
          documentCount: 25,
          createdAt: '2023-10-20T09:15:00Z',
          updatedAt: '2023-10-20T09:15:00Z',
          status: 'active'
        }
      ];
      
      const page = (params?.page as number) ?? 1;
      const size = (params?.size as number) ?? 10;
      const start = (page - 1) * size;
      const end = start + size;
      
      return {
        data: mockData.slice(start, end),
        total: mockData.length,
        code: 200,
        message: 'success'
      };
    }
    
    throw error;
  }
};

// 创建知识库 - 根据API.md
export const createKnowledgeBase = async (data: { name: string; description?: string; department: string }) => {
  return http.post('/v1/knowledge-bases', data);
};

// 更新知识库 - 根据API.md
export const updateKnowledgeBase = async (id: string, data: { name?: string; description?: string; department?: string }) => {
  return http.put(`/v1/knowledge-bases/${id}`, data);
};

// 删除知识库 - 根据API.md
export const deleteKnowledgeBase = async (id: string) => {
  return http.delete(`/v1/knowledge-bases/${id}`);
};

// 获取知识库详情 - 根据API.md
export const getKnowledgeBaseDetail = async (id: string) => {
  return http.get(`/v1/knowledge-bases/${id}`);
};

// 获取知识库统计 - 根据API.md
export const getKnowledgeBaseStats = async (id: string) => {
  return http.get(`/v1/knowledge-bases/${id}/stats`);
};

// 获取知识库下的文档列表（包含已删除）
export const getKnowledgeBaseDocuments = async (id: string, page: number, size: number) => {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    _t: String(Date.now()) // 添加时间戳避免缓存
  });
  return http.get(`/v1/knowledge-bases/${id}/documents?${params.toString()}`);
};