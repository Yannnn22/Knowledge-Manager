// 用户相关类型
export interface User {
  id: string;
  username: string;
  email: string;
  realName: string;
  department: string;
  avatarUrl?: string;
  avatar?: string;
  phone?: string;
  bio?: string;
  location?: string;
  website?: string;
  position?: string;
  role?: 'admin' | 'user' | 'guest';
  status?: 'active' | 'inactive';
  lastLogin?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  realName: string;
  department: string;
  phone?: string;
  bio?: string;
  location?: string;
  website?: string;
  avatarUrl?: string;
  emailNotification?: boolean;
  systemNotification?: boolean;
}

export interface LoginResponse {
  token: string;
  user: User;
}

// 知识库相关类型
export interface KnowledgeBase {
  id: string;
  name: string;
  description: string;
  department: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  documentCount: number;
  status: 'active' | 'inactive';
}

export interface KnowledgeBaseStats {
  totalDocuments: number;
  totalSize: number;
  categoryDistribution: Record<string, number>;
  recentActivity: Array<{
    date: string;
    count: number;
  }>;
}

// 文档相关类型
export interface Document {
  id: string;
  title: string;
  content: string;
  fileId: string;
  knowledgeBaseId: string;
  tags: string[];
  status: 'processing' | 'completed' | 'failed' | 'archived';
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  size: number;
  type: string;
  isFavorite: boolean;
}

export interface DocumentSearchRequest {
  query: string;
  knowledgeBaseId?: string;
  tags?: string[];
  type?: string;
  page?: number;
  size?: number;
}

export interface DocumentTag {
  id: string;
  name: string;
  color: string;
  count: number;
}

// 操作日志相关类型
export interface OperationLog {
  id: string;
  userId: string;
  username: string;
  operation: string;
  resource: string;
  resourceId: string;
  details: string;
  ip: string;
  userAgent: string;
  createdAt: string;
  isSensitive: boolean;
}

// AI/RAG相关类型
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface Conversation {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: string;
  updatedAt: string;
}

export interface ChatRequest {
  message: string;
  chatId?: string;
  model?: string;
  temperature?: number;
  topK?: number;
  threshold?: number;
  fileId?: string;
}

export interface ConversationStats {
  totalConversations: number;
  totalMessages: number;
  averageMessagesPerConversation: number;
  mostActiveDay: string;
}

// 通用类型
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
  order?: 'asc' | 'desc';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 表单验证规则类型
export interface FormRule {
  required?: boolean;
  message?: string;
  trigger?: string | string[];
  min?: number;
  max?: number;
  pattern?: RegExp;
  validator?: () => void;
}

// 表格列配置类型
export interface TableColumn {
  key: string;
  title: string;
  dataIndex?: string;
  width?: number;
  align?: 'left' | 'center' | 'right';
  sorter?: boolean;
  render?: () => unknown;
}

// 路由元信息类型
export interface RouteMeta {
  title?: string;
  requiresAuth?: boolean;
  roles?: string[];
  icon?: string;
  hidden?: boolean;
  [key: string]: unknown;
}