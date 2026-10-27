import http from '@/utils/http';
import { normalizeKnowledgeBaseId, normalizeKnowledgeBaseIds } from '@/utils/idMap';

// 文档相关类型定义
export interface Document {
  id: string;
  title: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  knowledgeBaseId: string;
  knowledgeBaseName: string;
  status: 'processing' | 'completed' | 'failed' | 'archived';
  uploadTime: string;
  isFavorite: boolean;
  tags: DocumentTag[];
  description?: string;
}

export interface DocumentTag {
  id: string;
  name: string;
  color: string;
}

export interface DocumentSearchRequest {
  query: string;
  knowledgeBaseIds?: string[];
  page?: number;
  size?: number;
  sortBy?: 'uploadTime' | 'createdAt';
  order?: 'asc' | 'desc';
}

export interface DocumentUploadRequest {
  file: File;
  knowledgeBaseId: string;
  description?: string;
}

// 上传文档 - 根据API.md
export const uploadDocument = async (data: DocumentUploadRequest) => {
  const formData = new FormData();
  formData.append('file', data.file);
  // The backend now correctly handles various ID formats, so we send the original ID.
  formData.append('knowledgeBaseId', data.knowledgeBaseId);
  if (data.description) {
    formData.append('description', data.description);
  }
  
  return http.post('/v1/documents/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// 搜索文档 - 根据API.md
export const searchDocuments = async (params: DocumentSearchRequest) => {
  // 确保 knowledgeBaseIds 参数正确传递
  const body = { 
    ...params, 
    knowledgeBaseIds: normalizeKnowledgeBaseIds(params.knowledgeBaseIds),
    _t: Date.now() // 添加时间戳避免缓存
  };
  
  console.log('🔍 Document API Debug - searchDocuments:', {
    originalParams: params,
    requestBody: body,
    url: '/v1/documents/search'
  });
  
  return http.post('/v1/documents/search', body);
};

// 文档分页列表（用于初始/空搜索场景）
export interface DocumentListParams {
  page?: number;
  size?: number;
  knowledgeBaseId?: string | number;
  status?: string;
  sortBy?: 'uploadTime' | 'createdAt';
  order?: 'asc' | 'desc';
}

export const listDocuments = async (params: DocumentListParams) => {
  // 由于后端没有实现 /api/v1/documents/page 接口，
  // 我们使用搜索接口来实现分页列表功能
  
  // 构建搜索参数
  const searchParams: any = {
    query: '', // 空查询表示获取所有文档
    page: params.page || 1,
    size: params.size || 10,
    sortBy: params.sortBy || 'uploadTime',
    order: params.order || 'desc'
  };
  
  // 只有当明确指定了知识库ID时才传递knowledgeBaseIds参数
  if (params.knowledgeBaseId) {
    const kbId = normalizeKnowledgeBaseId(params.knowledgeBaseId as string | number)
    if (kbId) {
      searchParams.knowledgeBaseIds = [kbId]
    }
  }
  // 注意：如果不指定knowledgeBaseId，则不传递knowledgeBaseIds参数，让后端返回所有文档
  
  console.log('🔍 Document API Debug - listDocuments (using search):', {
    originalParams: params,
    searchParams,
    url: '/v1/documents/search',
    note: params.knowledgeBaseId ? 'Filtering by specific KB' : 'Getting all documents (no KB filter)',
    actualRequestBody: JSON.stringify(searchParams),
    willSendKnowledgeBaseIds: searchParams.knowledgeBaseIds
  });
  
  const response = await searchDocuments(searchParams);
  
  // 如果指定了知识库ID，需要为返回的文档标记知识库ID
  if (params.knowledgeBaseId && response?.data?.results) {
    const kbId = normalizeKnowledgeBaseId(params.knowledgeBaseId as string | number);
    if (kbId) {
      const results = response.data.results as Array<Record<string, unknown>>;
      const taggedResults = results.map((doc) => ({ ...doc, knowledgeBaseId: kbId }));
      
      return {
        ...response,
        data: {
          ...response.data,
          results: taggedResults
        }
      };
    }
  }
  
  return response;
};

// 新增：获取全部知识库文档的函数
// 新增：使用改进的后端API获取文档（支持权限控制和筛选）
export const getDocumentsWithFilter = async (params: { 
  page?: number; 
  size?: number; 
  knowledgeBaseIds?: string[]; 
  status?: number;
  sortBy?: 'uploadTime' | 'createdAt'; 
  order?: 'asc' | 'desc' 
}) => {
  try {
    const queryParams = new URLSearchParams();
    
    // 添加分页参数
    queryParams.append('page', String(params.page || 1));
    queryParams.append('size', String(params.size || 10));
    
    // 添加筛选参数
    if (params.knowledgeBaseIds && params.knowledgeBaseIds.length > 0) {
      const normalizedIds = params.knowledgeBaseIds
        .map(id => normalizeKnowledgeBaseId(id))
        .filter(id => id !== null);
      
      if (normalizedIds.length > 0) {
        // 根据API文档，knowledgeBaseIds应该作为逗号分隔的字符串传递
        queryParams.append('knowledgeBaseIds', normalizedIds.join(','));
      }
    }
    
    if (params.status !== undefined) {
      queryParams.append('status', String(params.status));
    }
    
    // 添加排序参数
    if (params.sortBy) {
      queryParams.append('sortBy', params.sortBy);
    }
    if (params.order) {
      queryParams.append('order', params.order);
    }
    
    console.log('🔍 getDocumentsWithFilter 调用文档列表API:', {
      url: `/v1/documents/page?${queryParams.toString()}`,
      params: params,
      normalizedKbIds: params.knowledgeBaseIds?.map(id => normalizeKnowledgeBaseId(id))
    });
    
    const response = await http.get(`/v1/documents/page?${queryParams.toString()}`);
    
    console.log('📥 getDocumentsWithFilter 响应:', {
      status: response?.status,
      dataKeys: response?.data ? Object.keys(response.data) : [],
      documentsCount: response?.data?.documents?.length || 0,
      total: response?.data?.total,
      firstDocument: response?.data?.documents?.[0],
      rawResponse: response
    });
    
    // 确保返回的数据结构符合前端期望
    if (response?.data?.documents) {
      return {
        ...response,
        data: {
          ...response.data,
          list: response.data.documents, // 为了兼容性，同时提供list字段
          total: response.data.total
        }
      };
    }
    
    return response;
  } catch (error) {
    console.error('getDocumentsWithFilter 调用失败:', error);
    throw error;
  }
};

export const getAllDocuments = async (params: { page?: number; size?: number; sortBy?: 'uploadTime' | 'createdAt'; order?: 'asc' | 'desc' }) => {
  // 直接使用逐个知识库搜索，但只搜索有权限的知识库
  try {
    console.log('🔍 Document API Debug - getAllDocuments:', {
      note: 'Using per-KB search with permission filtering'
    });

    const kbResponse = await http.get('/v1/knowledge-bases');
    const knowledgeBases: Array<{ id: string | number; name?: string; documentCount?: number }> = kbResponse.data?.knowledgeBases || [];

    if (!Array.isArray(knowledgeBases) || knowledgeBases.length === 0) {
      console.warn('没有找到知识库，返回空结果');
      return { code: 200, message: 'success', data: { results: [], total: 0 } } as const;
    }

    const page = params.page || 1;
    const size = params.size || 10;
    const sortBy = params.sortBy || 'uploadTime';
    const order = params.order || 'desc';

    // 逐个KB搜索，但只保留有权限的结果
    const perKbPromises = knowledgeBases.map(async (kb) => {
      try {
        const normId = normalizeKnowledgeBaseId(kb.id)
        if (!normId) {
          console.warn('跳过无效的知识库ID:', kb.id);
          return [] as Array<Record<string, unknown>>;
        }

        const singleParams: DocumentSearchRequest = {
          query: '',
          knowledgeBaseIds: [normId],
          page: 1, // 按KB取第一页，后续统一在前端分页
          size: size,
          sortBy,
          order
        };
        
        console.log(`🔍 Testing KB ${normId} (${kb.name}):`, singleParams);
        const resp = await searchDocuments(singleParams);
        const results = (resp?.data?.results || resp?.data?.documents || []) as Array<Record<string, unknown>>;
        
        if (results && results.length > 0) {
          console.log(`✅ KB ${normId} has ${results.length} documents`);
          // 标记该结果属于哪个KB，便于前端名称映射
          const tagged = results.map((doc) => ({ ...doc, knowledgeBaseId: normId }));
          return tagged;
        } else {
          console.log(`ℹ️ KB ${normId} has no documents`);
          return [] as Array<Record<string, unknown>>;
        }
      } catch (e: any) {
        // 检查是否是权限错误
        if (e?.response?.data?.message?.includes('无权限') || e?.message?.includes('无权限')) {
          console.log(`🚫 No permission for KB ${kb.id} (${kb.name}), skipping`);
        } else {
          console.warn(`⚠️ Error searching KB ${kb.id}:`, e);
        }
        return [] as Array<Record<string, unknown>>;
      }
    });

    const lists = await Promise.all(perKbPromises);
    const merged = ([] as Array<Record<string, unknown>>).concat(...lists);

    // 统一做简单分页（page/size）
    const start = (page - 1) * size;
    const end = start + size;
    const paged = merged.slice(start, end);

    const payload = {
      code: 200,
      message: 'success',
      data: {
        results: paged,
        total: merged.length
      }
    } as const;

    console.log('🔍 Document API Debug - getAllDocuments result:', {
      totalKBs: knowledgeBases.length,
      mergedTotal: merged.length,
      returned: paged.length,
      accessibleKBs: knowledgeBases.filter(kb => {
        const normId = normalizeKnowledgeBaseId(kb.id);
        return normId && merged.some(doc => doc.knowledgeBaseId === normId);
      }).length
    });

    return payload;
  } catch (error) {
    console.error('获取全部文档失败:', error);
    return { code: 200, message: 'success', data: { results: [], total: 0 } } as const;
  }
};

// 获取文档状态 - 根据API.md
export const getDocumentStatus = async (documentId: string) => {
  return http.get(`/v1/documents/${documentId}/status`);
};

// 归档文档 - 根据API.md
export const archiveDocument = async (documentId: string) => {
  return http.put(`/v1/documents/${documentId}/archive`);
};

// 收藏文档 - 根据API.md
export const toggleFavorite = async (documentId: string) => {
  return http.post(`/v1/documents/${documentId}/favorite`);
};

// 取消收藏文档 - 根据API.md
export const removeFavorite = async (documentId: string) => {
  return http.delete(`/v1/documents/${documentId}/favorite`);
};

// 获取收藏列表 - 根据API.md
export const getFavorites = async (params: { page?: number; size?: number }) => {
  return http.get('/v1/documents/favorites', { params });
};

// 获取文档标签 - 根据API.md
export const getDocumentTags = async (documentId: string) => {
  return http.get(`/v1/documents/${documentId}/tags`);
};

// 添加文档标签 - 根据API.md
export const addDocumentTag = async (documentId: string, tagIds: string[]) => {
  return http.post(`/v1/documents/${documentId}/tags`, { tagIds });
};

// 移除文档标签 - 根据API.md
export const removeDocumentTag = async (documentId: string, tagId: string) => {
  return http.delete(`/v1/documents/${documentId}/tags/${tagId}`);
};

// 获取所有标签 - 根据API.md
export const getAllTags = async () => {
  return http.get('/v1/documents/tags');
};

// 创建标签 - 根据API.md
export const createTag = async (data: { name: string; color?: string; description?: string }) => {
  return http.post('/v1/documents/tags', data);
};

// 删除文档 - 根据用户需求添加
export const deleteDocument = async (documentId: string) => {
  return http.delete(`/v1/documents/${documentId}`);
};

// 获取文档详情 - 新增后端接口
export const getDocumentDetail = async (documentId: string) => {
  return http.get(`/v1/documents/${documentId}`);
};

// 文档内容预览 - 新增后端接口
export const getDocumentPreview = async (documentId: string) => {
  return http.get(`/v1/documents/${documentId}/preview`);
};

// 下载文档 - 新增后端接口
export const downloadDocument = async (documentId: string) => {
  return http.get(`/v1/documents/${documentId}/download`, { responseType: 'blob' as any });
};

// 编辑文档 - 新增后端接口
export const editDocument = async (documentId: string, data: { content?: string; description?: string }) => {
  return http.put(`/v1/documents/${documentId}/edit`, data);
};

// 获取文档评论 - 新增后端接口
export const getDocumentComments = async (documentId: string) => {
  return http.get(`/v1/documents/${documentId}/comments`);
};

// 添加文档评论 - 新增后端接口
export const addDocumentComment = async (documentId: string, data: { content: string; parentId?: string }) => {
  return http.post(`/v1/documents/${documentId}/comments`, data);
};

// 更新评论 - 新增后端接口
export const updateComment = async (commentId: string, data: { content: string }) => {
  return http.put(`/v1/comments/${commentId}`, data);
};

// 删除评论 - 新增后端接口
export const deleteComment = async (commentId: string) => {
  return http.delete(`/v1/comments/${commentId}`);
};

// 物理删除文档 - 新增后端接口（仅管理员）
export const physicalDeleteDocument = async (documentId: string) => {
  return http.delete(`/v1/documents/${documentId}/physical`);
};

// 批量删除文档 - 新增后端接口
export const batchDeleteDocuments = async (documentIds: number[]) => {
  return http.post('/v1/documents/batch-delete', documentIds);
};

// 重命名文档 - 新增后端接口
export const renameDocument = async (documentId: string, name: string) => {
  return http.put(`/v1/documents/${documentId}/rename`, { name });
};

// 移动文档 - 新增后端接口
export const moveDocument = async (documentId: string, knowledgeBaseId: string | number) => {
  return http.put(`/v1/documents/${documentId}/move`, { knowledgeBaseId });
};

// 复制文档 - 新增后端接口
export const copyDocument = async (documentId: string, knowledgeBaseId: string | number) => {
  return http.post(`/v1/documents/${documentId}/copy`, { knowledgeBaseId });
};

// 更新文档描述 - 新增后端接口
export const updateDocumentDescription = async (documentId: string, description: string) => {
  return http.put(`/v1/documents/${documentId}/description`, { description });
};

// 文档状态相关接口
export interface DocumentStatus {
  status: number;
  progress: number;
  currentStep: string;
}

// 文档状态枚举
export const DOCUMENT_STATUS = {
  UPLOADING: 0,    // 上传中
  PROCESSING: 1,   // 处理中
  COMPLETED: 2,    // 完成
  FAILED: 3,       // 失败
  ARCHIVED: 4      // 已归档
} as const;

export const DOCUMENT_STATUS_TEXT = {
  [DOCUMENT_STATUS.UPLOADING]: '上传中',
  [DOCUMENT_STATUS.PROCESSING]: '处理中',
  [DOCUMENT_STATUS.COMPLETED]: '完成',
  [DOCUMENT_STATUS.FAILED]: '失败',
  [DOCUMENT_STATUS.ARCHIVED]: '已归档'
} as const;

// 定义统计数据类型
export interface DocumentStats {
  total: number;
  processing: number;
  completed: number;
  failed: number;
}

/**
 * 获取文档统计信息
 */
export const getDocumentStatistics = async (): Promise<{ data: DocumentStats }> => {
  try {
    // 调用真实的后端API
    const response = await http.get('/v1/documents/statistics');
    return response;
  } catch (error) {
    console.error('获取文档统计信息失败:', error);
    // 如果API调用失败，返回默认值
    return {
      data: {
        total: 0,
        processing: 0,
        completed: 0,
        failed: 0,
      },
    };
  }
};