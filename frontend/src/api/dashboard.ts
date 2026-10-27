import { getKnowledgeBases } from './knowledgeBase';

// 仪表盘统计数据
export interface DashboardStats {
  knowledgeBaseCount: number;
  documentCount: number;
  operationLogCount: number;
  pendingTasksCount: number;
  knowledgeBaseGrowth: number; // 较上月增长
  documentTypeDistribution: {
    text: number;
    nonText: number;
  };
  recentActivityDays: number;
  expiringTasksCount: number;
}

// 获取仪表盘统计数据 - 从现有API计算
export const getDashboardStats = async (): Promise<DashboardStats> => {
  try {
    // 从知识库API获取数据来计算统计
    const knowledgeBasesResponse = await getKnowledgeBases({ page: 1, size: 1000 });
    
    let knowledgeBaseCount = 0;
    let totalDocuments = 0;
    
    if (knowledgeBasesResponse && knowledgeBasesResponse.data) {
      let knowledgeBases: any[] = [];
      
      // 处理后端返回的数据格式
      if (knowledgeBasesResponse.data.knowledgeBases) {
        // 后端返回格式：{ data: { knowledgeBases: [...] } }
        knowledgeBases = knowledgeBasesResponse.data.knowledgeBases;
      } else if (Array.isArray(knowledgeBasesResponse.data)) {
        knowledgeBases = knowledgeBasesResponse.data;
      } else {
        knowledgeBases = [knowledgeBasesResponse.data];
      }
      
      knowledgeBaseCount = knowledgeBases.length;
      totalDocuments = knowledgeBases.reduce((sum: number, kb: any) => sum + (kb.documentCount || 0), 0);
    }
    
    // 计算文档类型分布（模拟数据，实际应该从文档API获取）
    const textDocuments = Math.floor(totalDocuments * 0.8);
    const nonTextDocuments = totalDocuments - textDocuments;
    
    return {
      knowledgeBaseCount,
      documentCount: totalDocuments,
      operationLogCount: Math.floor(Math.random() * 50) + 10, // 模拟数据
      pendingTasksCount: Math.floor(Math.random() * 10) + 1, // 模拟数据
      knowledgeBaseGrowth: Math.floor(Math.random() * 5) + 1, // 模拟数据
      documentTypeDistribution: {
        text: textDocuments,
        nonText: nonTextDocuments
      },
      recentActivityDays: 7,
      expiringTasksCount: Math.floor(Math.random() * 5) + 1 // 模拟数据
    };
    
  } catch (error) {
    console.error('获取仪表盘统计数据失败:', error);
    
    // 返回默认数据
    return {
      knowledgeBaseCount: 0,
      documentCount: 0,
      operationLogCount: 0,
      pendingTasksCount: 0,
      knowledgeBaseGrowth: 0,
      documentTypeDistribution: { text: 0, nonText: 0 },
      recentActivityDays: 7,
      expiringTasksCount: 0
    };
  }
};

// 获取知识库概览（用于主页显示）
export const getKnowledgeBaseOverview = async (params: { page?: number; size?: number }) => {
  return getKnowledgeBases(params);
};

// 获取最近活动
export interface RecentActivity {
  id: string;
  type: 'document_upload' | 'knowledge_base_create' | 'user_login' | 'operation_log';
  title: string;
  description: string;
  timestamp: string;
  user: string;
}

export const getRecentActivity = async (): Promise<RecentActivity[]> => {
  // 模拟最近活动数据
  return Promise.resolve([
    {
      id: '1',
      type: 'document_upload',
      title: '上传了新文档',
      description: '用户张三上传了技术规范文档',
      timestamp: new Date().toISOString(),
      user: '张三'
    },
    {
      id: '2',
      type: 'knowledge_base_create',
      title: '创建了知识库',
      description: '用户李四创建了产品设计知识库',
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      user: '李四'
    }
  ]);
};
