<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-title">智汇库管理系统</div>
      </div>
      <div class="user-card">
        <img :src="avatarUrl" class="avatar" alt="avatar" />
        <div class="user-meta">
          <div class="user-name">{{ displayName }}</div>
          <div class="user-role">{{ roleText }}</div>
        </div>
      </div>
      <nav class="menu">
        <button :class="['menu-item', { active: isActive('/home') }]" @click="go('/home')">
          <span class="menu-icon">🏠</span>
          <span>首页</span>
        </button>
        <button :class="['menu-item', { active: isActive('/knowledge-base') }]" @click="go('/knowledge-base')">
          <span class="menu-icon">📚</span>
          <span>知识库管理</span>
        </button>
        <button :class="['menu-item', { active: isActive('/documents') }]" @click="go('/documents')">
          <span class="menu-icon">🗂️</span>
          <span>文档管理</span>
        </button>
        <button :class="['menu-item', { active: isActive('/operation-logs') }]" @click="go('/operation-logs')">
          <span class="menu-icon">📝</span>
          <span>操作日志管理</span>
        </button>
        <button :class="['menu-item', { active: isActive('/ai-assistant') }]" @click="go('/ai-assistant')">
          <span class="menu-icon">🤖</span>
          <span>智能秘书</span>
        </button>
        <button :class="['menu-item', { active: isActive('/profile') }]" @click="go('/profile')">
          <span class="menu-icon">👤</span>
          <span>个人中心</span>
        </button>
      </nav>
    </aside>

    <section class="content">
      <header class="topbar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input 
            class="search-input" 
            type="text" 
            placeholder="搜索文档..." 
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
          />
          <a-button size="small" style="margin-left:8px" @click="handleSearch">搜索</a-button>
        </div>
        <div class="actions">
          <!-- 管理员身份提示 -->
          <div v-if="authStore.isAdmin" class="admin-badge">
            <span class="admin-icon">👑</span>
            <span class="admin-text">管理员</span>
          </div>
          <span class="bell">🔔<i class="badge">3</i></span>
          <span class="profile-dot">⚫</span>
        </div>
      </header>

    <div class="document-page">
      <div class="page-header">
        <h1>文档管理</h1>
        <ul class="breadcrumb">
            <li><a href="#" @click="go('/home')">首页</a></li>
          <li>文档管理</li>
        </ul>
      </div>
  
      <div class="stats-cards">
        <a-card v-for="stat in stats" :key="stat.title" class="stat-card">
          <div class="stat-card-header">
            <div class="stat-card-title">{{ stat.title }}</div>
            <div class="stat-card-icon" :style="{ background: stat.iconBg, color: stat.iconColor }">
              <component :is="stat.icon" />
            </div>
          </div>
          <div class="stat-card-value">{{ stat.value }}</div>
          <div class="stat-card-desc">{{ stat.desc }}</div>
        </a-card>
      </div>
  
      <div class="content-section">
        <div class="section-header">
          <div class="title-with-info">
          <h2 class="section-title">文档列表</h2>
            <!-- 权限提示信息 -->
            <div class="permission-info">
              <span v-if="authStore.isAdmin" class="permission-text admin">
                🔓 管理员权限：可查看所有文档
              </span>
              <span v-else class="permission-text user">
                🔒 普通用户：仅显示有权限的文档
              </span>
            </div>
          </div>
            <div class="header-actions">
              <a-select 
                v-model:value="knowledgeBaseFilter" 
                placeholder="选择知识库" 
                style="width: 150px; margin-right: 10px;"
                @change="handleFilter"
                allowClear
              >
                <a-select-option value="">全部知识库</a-select-option>
                <a-select-option 
                  v-for="kb in knowledgeBases" 
                  :key="kb.id" 
                  :value="kb.id"
                  :title="`原始ID: ${kb.id}, 规范化ID: ${normalizeKnowledgeBaseId(kb.id)}`"
                >
                  {{ kb.name }}
                </a-select-option>
              </a-select>
              <a-select 
                v-model:value="statusFilter" 
                placeholder="文档状态" 
                style="width: 120px; margin-right: 10px;"
                @change="handleFilter"
                allowClear
              >
                <a-select-option value="">全部状态</a-select-option>
                <a-select-option value="1">处理中</a-select-option>
                <a-select-option value="2">已完成</a-select-option>
                <a-select-option value="3">处理失败</a-select-option>
              </a-select>
              <a-button @click="showFilterModal">高级筛选</a-button>
              <a-button 
                v-if="selectedRowKeys.length > 0"
                danger 
                :loading="batchDeleteLoading"
                @click="handleBatchDelete"
              >
                <template #icon>
                  <DeleteOutlined />
                </template>
                批量删除 ({{ selectedRowKeys.length }})
              </a-button>
            <a-button type="primary" @click="showUploadModal">上传文档</a-button>
          </div>
        </div>
          
        <div class="table-responsive">
            <a-table 
              :dataSource="documents" 
              :columns="documentColumns" 
              :pagination="false"
              :loading="loading"
              rowKey="key"
              :rowSelection="{
                selectedRowKeys: selectedRowKeys,
                onChange: (keys: string[], selectedRows: DocumentRow[]) => {
                  selectedRowKeys = keys
                  selectedDocuments = selectedRows
                }
              }"
            >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <div class="status-container">
                  <a-tag :color="getStatusColor(record.status)">
                    {{ getStatusText(record.status) }}
                </a-tag>
                  <div v-if="documentStatuses[record.key]" class="status-details">
                    <div class="status-progress" v-if="documentStatuses[record.key].progress < 100">
                      <a-progress 
                        :percent="documentStatuses[record.key].progress" 
                        size="small" 
                        :show-info="false"
                      />
                      <span class="progress-text">{{ documentStatuses[record.key].currentStep }}</span>
                    </div>
                  </div>
                </div>
              </template>
                <template v-else-if="column.key === 'size'">
                  {{ formatFileSize(record.size) }}
                </template>
                <template v-else-if="column.key === 'isFavorite'">
                  <a-button 
                    type="text" 
                    :title="record.isFavorite ? '取消收藏' : '收藏'"
                    @click="onToggleFavorite(record)"
                  >
                    <HeartOutlined v-if="!record.isFavorite" />
                    <HeartFilled v-else style="color: #ff4d4f;" />
                  </a-button>
              </template>
              <template v-else-if="column.key === 'actions'">
                <div class="action-buttons">
                  <a-button type="text" title="查看" @click="viewDocument(record)">
                    <EyeOutlined />
                  </a-button>
                  <a-button type="text" title="下载" @click="downloadDocument(record)">
                    <DownloadOutlined />
                  </a-button>
                  <a-dropdown>
                      <a-button type="text">
                        <MoreOutlined />
                    </a-button>
                    <template #overlay>
                      <a-menu>
                          <a-menu-item @click="showRenameModal(record)">
                            <EditOutlined /> 重命名
                          </a-menu-item>
                          <a-menu-item @click="showMoveModal(record)">
                            <FolderOpenOutlined /> 移动
                          </a-menu-item>
                          <a-menu-item @click="showCopyModal(record)">
                            <CopyOutlined /> 复制
                          </a-menu-item>
                          <a-menu-divider />
                          <a-menu-item @click="onArchiveDocument(record)" v-if="record.status !== 'archived'">
                            <InboxOutlined /> 归档
                          </a-menu-item>
                          <a-menu-item @click="showTagsModal(record)">
                            <TagOutlined /> 管理标签
                          </a-menu-item>
                          <a-menu-item @click="checkDocumentStatus(record)">
                            <InfoCircleOutlined /> 查看状态
                          </a-menu-item>
                          <a-menu-item @click="onDeleteDocument(record)" danger v-if="canDeleteDocument(record)">
                            <DeleteOutlined /> 删除
                          </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </div>
              </template>
            </template>
          </a-table>
        </div>
          
        <div class="pagination">
          <a-pagination
            v-model:current="currentPage"
            :total="totalItems"
            :pageSize="pageSize"
            show-less-items
            @change="handlePageChange"
          />
        </div>
      </div>
      </div>
    </section>
  
    <!-- 上传文档模态框 -->
      <a-modal
      v-model:open="uploadModalVisible"
        title="上传文档"
      @ok="handleUpload"
      @cancel="uploadModalVisible = false"
      :confirmLoading="uploadLoading"
        width="600px"
      >
      <a-form :model="uploadForm" layout="vertical">
        <a-form-item label="选择知识库" required>
          <a-select v-model:value="uploadForm.knowledgeBaseId" placeholder="请选择知识库">
            <a-select-option 
              v-for="kb in knowledgeBases" 
              :key="kb.id" 
              :value="kb.id"
              :title="`原始ID: ${kb.id}, 规范化ID: ${normalizeKnowledgeBaseId(kb.id)}`"
            >
              {{ kb.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="文档描述">
          <a-textarea v-model:value="uploadForm.description" placeholder="请输入文档描述" :rows="3" />
        </a-form-item>
        <a-form-item label="选择文件" required>
          <a-upload
            v-model:fileList="uploadForm.fileList"
            :beforeUpload="beforeUpload"
            :multiple="false"
            accept=".pdf,.doc,.docx,.txt,.ppt,.pptx,.xls,.xlsx"
          >
            <a-button>
              <UploadOutlined />
              选择文件
            </a-button>
          </a-upload>
        </a-form-item>
      </a-form>
      </a-modal>
  
    <!-- 高级筛选模态框 -->
      <a-modal
      v-model:open="filterModalVisible"
      title="高级筛选"
      @ok="handleAdvancedFilter"
      @cancel="filterModalVisible = false"
      >
        <a-form :model="filterForm" layout="vertical">
          <a-form-item label="文档类型">
          <a-select v-model:value="filterForm.type" placeholder="选择文档类型" allowClear>
              <a-select-option value="pdf">PDF</a-select-option>
            <a-select-option value="doc">Word</a-select-option>
            <a-select-option value="txt">文本</a-select-option>
            <a-select-option value="ppt">PowerPoint</a-select-option>
            <a-select-option value="xls">Excel</a-select-option>
            </a-select>
        </a-form-item>
        <a-form-item label="上传时间">
          <a-range-picker v-model:value="filterForm.dateRange" />
        </a-form-item>
        <a-form-item label="文件大小">
          <a-input-group compact>
            <a-input-number v-model:value="filterForm.minSize" placeholder="最小" style="width: 50%;" />
            <a-input style="width: 10%; border-left: 0; pointer-events: none; background-color: #fff;" placeholder="~" disabled />
            <a-input-number v-model:value="filterForm.maxSize" placeholder="最大" style="width: 40%; border-left: 0;" />
          </a-input-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 标签管理模态框 -->
    <a-modal
      v-model:open="tagsModalVisible"
      title="管理标签"
      @ok="handleTagsUpdate"
      @cancel="tagsModalVisible = false"
    >
      <div v-if="selectedDocument">
        <p>文档：{{ selectedDocument.name }}</p>
        <a-divider />
        <a-form layout="vertical">
          <a-form-item label="当前标签">
            <div class="current-tags">
              <a-tag 
                v-for="tag in selectedDocument.tags" 
                :key="tag.id"
                closable
                @close="removeTag(tag.id)"
              >
                {{ tag.name }}
              </a-tag>
            </div>
          </a-form-item>
          <a-form-item label="添加标签">
            <a-select
              v-model:value="newTagId"
              placeholder="选择标签"
              style="width: 100%"
            >
              <a-select-option v-for="tag in availableTags" :key="tag.id" :value="tag.id">
                {{ tag.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </div>
      </a-modal>

    <!-- 重命名文档模态框 -->
    <a-modal
      v-model:open="renameModalVisible"
      title="重命名文档"
      @ok="handleRenameDocument"
      @cancel="renameModalVisible = false"
      :confirmLoading="renameLoading"
      width="400px"
    >
      <a-form :model="renameForm" layout="vertical">
        <a-form-item label="文档名称">
          <a-input 
            v-model:value="renameForm.name" 
            placeholder="请输入新的文档名称"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 移动文档模态框 -->
    <a-modal
      v-model:open="moveModalVisible"
      title="移动文档"
      @ok="handleMoveDocument"
      @cancel="moveModalVisible = false"
      :confirmLoading="moveLoading"
      width="400px"
    >
      <a-form :model="moveForm" layout="vertical">
        <a-form-item label="目标知识库" required>
          <a-select 
            v-model:value="moveForm.knowledgeBaseId" 
            placeholder="请选择目标知识库"
            style="width: 100%"
          >
            <a-select-option 
              v-for="kb in knowledgeBases" 
              :key="kb.id" 
              :value="kb.id"
              :disabled="kb.id === selectedDocument?.knowledgeBase"
            >
              {{ kb.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 复制文档模态框 -->
    <a-modal
      v-model:open="copyModalVisible"
      title="复制文档"
      @ok="handleCopyDocument"
      @cancel="copyModalVisible = false"
      :confirmLoading="copyLoading"
      width="400px"
    >
      <a-form :model="copyForm" layout="vertical">
        <a-form-item label="目标知识库" required>
          <a-select 
            v-model:value="copyForm.knowledgeBaseId" 
            placeholder="请选择目标知识库"
            style="width: 100%"
          >
            <a-select-option 
              v-for="kb in knowledgeBases" 
              :key="kb.id" 
              :value="kb.id"
            >
              {{ kb.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    </div>
  </template>
  
  <script setup lang="ts">
import { ref, onMounted, computed, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'

  import { 
  FileTextOutlined, 
  UploadOutlined, 
  SyncOutlined, 
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  DownloadOutlined,
MoreOutlined,
HeartOutlined,
HeartFilled,
TagOutlined,
InfoCircleOutlined,
DeleteOutlined,
CopyOutlined,
FolderOpenOutlined,
EditOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { 
  uploadDocument, 
  getDocumentStatus, 
  getDocumentsWithFilter,
  deleteDocument,
  batchDeleteDocuments,
  renameDocument,
  moveDocument,
  copyDocument,
  DOCUMENT_STATUS,
  type DocumentStatus
} from '@/api/document'
import { getKnowledgeBases } from '@/api/knowledgeBase'
import { useAuthStore } from '@/stores/auth'
import { normalizeKnowledgeBaseId } from '@/utils/idMap'

// 开发环境标志
  
  interface StatCard {
    title: string;
    value: string;
    desc: string;
    icon: Component;
    iconBg: string;
    iconColor: string;
  }
  
interface DocumentRow {
    key: string;
    name: string;
    type: string;
    size: string;
    knowledgeBase: string;
    uploadTime: string;
    status: string;
  isFavorite: boolean;
  tags: { id: string; name: string }[];
}

interface Tag {
  id: string;
  name: string;
  }
  
  interface KnowledgeBase {
    id: string;
    name: string;
  }
  
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isActive = (path: string) => computed(() => route.path === path).value
const go = (path: string) => { if (route.path !== path) router.push(path) }

const avatarUrl = computed(() => authStore.user?.avatarUrl || '/default-avatar.svg')
const displayName = computed(() => authStore.user?.realName || authStore.user?.username || '用户')
const roleText = computed(() => authStore.userRole || '用户')

// 权限检查函数
const canDeleteDocument = (doc: DocumentRow) => {
  // 检查用户是否有删除权限
  const user = authStore.user
  const token = authStore.token
  
  console.log('🔍 Delete permission check:', {
    hasUser: !!user,
    hasToken: !!token,
    docStatus: doc.status,
    docName: doc.name
  })
  
  // 如果有token就允许删除（简化权限逻辑）
  if (!token) {
    console.log('❌ No token, delete not allowed')
    return false
  }
  
  // 允许删除已完成、失败或处理中的文档
  const canDelete = ['completed', 'failed', 'processing'].includes(doc.status);
  
  console.log('🔍 Can delete:', canDelete);
  return canDelete
}
  
  const stats = ref<StatCard[]>([
  { title: '文档总数', value: '0', desc: '系统中的文档总数量', icon: FileTextOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '处理中', value: '0', desc: '正在处理中的文档', icon: SyncOutlined, iconBg: '#e6f7ff', iconColor: '#1890ff' },
  { title: '已完成', value: '0', desc: '已成功处理的文档', icon: CheckCircleOutlined, iconBg: '#f6ffed', iconColor: '#52c41a' },
  { title: '处理失败', value: '0', desc: '处理失败的文档', icon: CloseCircleOutlined, iconBg: '#fff1f0', iconColor: '#f5222d' }
])

const documents = ref<DocumentRow[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalItems = ref(0)
const loading = ref(false)

// 搜索和筛选


const searchKeyword = ref('')
const knowledgeBaseFilter = ref('')
const statusFilter = ref('')

const filterModalVisible = ref(false)
const filterForm = ref({
  type: '',
  dateRange: null as [Date, Date] | null,
  minSize: null as number | null,
  maxSize: null as number | null
})

const tagsModalVisible = ref(false)
  const selectedDocument = ref<DocumentRow | null>(null)
  const newTagId = ref('')
  const documentStatuses = ref<Record<string, DocumentStatus>>({})
  const batchDeleteLoading = ref(false)

  const renameModalVisible = ref(false)
  const renameLoading = ref(false)
  const renameForm = ref({ name: '' })

  const moveModalVisible = ref(false)
  const moveLoading = ref(false)
  const moveForm = ref({ knowledgeBaseId: '' })

  const copyModalVisible = ref(false)
  const copyLoading = ref(false)
  const copyForm = ref({ knowledgeBaseId: '' })

// 批量操作相关
const selectedRowKeys = ref<string[]>([])
const selectedDocuments = ref<DocumentRow[]>([])

// loading 已在上方声明，这里移除重复声明
const uploadModalVisible = ref(false)
const uploadLoading = ref(false)
const uploadForm = ref({
  knowledgeBaseId: '',
  description: '',
  fileList: [] as unknown[]
})

const knowledgeBases = ref<KnowledgeBase[]>([])
const kbLoaded = ref(false)
const kbIdToName = computed(() => {
  const m = new Map<string, string>()
  knowledgeBases.value.forEach(kb => {
    const originalId = String(kb.id)
    const normalizedId = normalizeKnowledgeBaseId(kb.id)
    
    // 添加原始ID映射
    m.set(originalId, kb.name)
    m.set(`kb${originalId}`, kb.name)
    m.set(`KB${originalId}`, kb.name)
    
    // 添加规范化ID映射
    if (normalizedId && normalizedId !== originalId) {
      m.set(normalizedId, kb.name)
      m.set(`kb${normalizedId}`, kb.name)
      m.set(`KB${normalizedId}`, kb.name)
    }
  })
  return m
})

const availableTags = ref<Tag[]>([
  { id: 'tag1', name: '重要' },
  { id: 'tag2', name: '紧急' },
  { id: 'tag3', name: '待办' },
  { id: 'tag4', name: '已完成' }
])

const documentColumns = [
  { title: '文档名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '大小', dataIndex: 'size', key: 'size' },
  { title: '所属知识库', dataIndex: 'knowledgeBase', key: 'knowledgeBase' },
  { title: '上传时间', dataIndex: 'uploadTime', key: 'uploadTime' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'actions' }
]

const handleFilter = () => { 
  const normalizedKbId = knowledgeBaseFilter.value ? normalizeKnowledgeBaseId(knowledgeBaseFilter.value) : null;
  console.log('🔍 Filter applied:', {
    knowledgeBaseFilter: knowledgeBaseFilter.value,
    normalizedKbId: normalizedKbId,
    statusFilter: statusFilter.value,
    currentPage: currentPage.value,
    availableKnowledgeBases: knowledgeBases.value.map(kb => ({
      id: kb.id,
      normalizedId: normalizeKnowledgeBaseId(kb.id),
      name: kb.name
    })),
    selectedKnowledgeBase: knowledgeBases.value.find(kb => kb.id === knowledgeBaseFilter.value),
    kbIdToNameMap: Object.fromEntries(kbIdToName.value),
    expectedDocumentKbId: normalizedKbId,
    willSendToAPI: normalizedKbId ? [normalizedKbId] : []
  });
  currentPage.value = 1; 
  loadDocuments() 
}

const handleSearch = () => { 
  console.log('🔍 Search applied:', {
    searchKeyword: searchKeyword.value,
    knowledgeBaseFilter: knowledgeBaseFilter.value,
    currentPage: currentPage.value
  });
  currentPage.value = 1; 
  loadDocuments() 
}
const showUploadModal = () => { uploadModalVisible.value = true }
const showFilterModal = () => { filterModalVisible.value = true }
const showTagsModal = (doc: DocumentRow) => {
  selectedDocument.value = doc
  tagsModalVisible.value = true
}

const handleUpload = async () => {
  try {
    if (!uploadForm.value.knowledgeBaseId) {
      message.warning('请选择知识库')
      return
    }
    // Ant Design Vue Upload 组件的文件对象为 UploadFile，需要从 originFileObj 取原始 File
    const first = uploadForm.value.fileList?.[0] as { originFileObj?: File } | File | undefined
    const file: File | undefined = (first && 'originFileObj' in first ? first.originFileObj : first) as File | undefined
    if (!file) {
      message.warning('请选择要上传的文件')
      return
    }
    uploadLoading.value = true
    const uploadRes = await uploadDocument({
      file,
      knowledgeBaseId: String(uploadForm.value.knowledgeBaseId),
      description: uploadForm.value.description || ''
    })
    // 兼容包裹格式 { code, data: { documentId } } 或直接返回 { documentId }
    const r = uploadRes as { data?: { documentId?: string | number }; documentId?: string | number }
    const docId = r?.data?.documentId ?? r?.documentId
    message.success('上传成功')
    uploadModalVisible.value = false
    // 重置表单
    uploadForm.value = { knowledgeBaseId: '', description: '', fileList: [] }
    // 若拿到 documentId，则轮询状态，完成后刷新
    if (docId) {
      await pollDocumentUntilReady(String(docId))
    } else {
      await loadDocuments()
    }
  } catch (err) {
    console.error('上传失败:', err)
    message.error('上传失败')
  } finally {
    uploadLoading.value = false
  }
}

// 轮询文档处理状态，完成或失败即停止，并刷新列表
const pollDocumentUntilReady = async (documentId: string) => {
  const start = Date.now()
  const timeoutMs = 30000
  const intervalMs = 1500
  return new Promise<void>((resolve) => {
    const tick = async () => {
      try {
        const res = await getDocumentStatus(documentId) as { data?: DocumentStatus }
        const statusData = res?.data
        if (statusData) {
          const status = statusData.status
          const progress = statusData.progress || 0
          const currentStep = statusData.currentStep || ''
          
          // 更新文档状态
          documentStatuses.value[documentId] = statusData
          
          // 显示进度信息
          if (progress > 0 && progress < 100) {
            message.loading(`文档处理中... ${progress}% - ${currentStep}`, 1)
          }
          
          // 检查是否完成
          if (status === DOCUMENT_STATUS.COMPLETED || 
              status === DOCUMENT_STATUS.FAILED || 
              status === DOCUMENT_STATUS.ARCHIVED) {
            
            if (status === DOCUMENT_STATUS.COMPLETED) {
              message.success('文档处理完成')
            } else if (status === DOCUMENT_STATUS.FAILED) {
              message.warning('文档处理失败')
            } else if (status === DOCUMENT_STATUS.ARCHIVED) {
              message.info('文档已归档')
            }
            
          loadDocuments().finally(() => resolve())
          return
        }
        }
      } catch (error) {
        console.error('查询文档状态失败:', error)
        // 忽略单次失败继续轮询
      }
      if (Date.now() - start > timeoutMs) {
        message.warning('文档处理超时，请稍后手动刷新')
        loadDocuments().finally(() => resolve())
        return
      }
      setTimeout(tick, intervalMs)
    }
    setTimeout(tick, intervalMs)
  })
}

const handleAdvancedFilter = () => {
  message.info('应用高级筛选（演示）')
  filterModalVisible.value = false
  loadDocuments()
}

const handleTagsUpdate = () => {
  if (selectedDocument.value && newTagId.value) {
    const tag = availableTags.value.find(t => t.id === newTagId.value)
    if (tag) {
      selectedDocument.value.tags.push(tag)
      message.success(`已添加标签：${tag.name}`)
    } else {
      message.error('未找到标签')
    }
  }
  tagsModalVisible.value = false
}

const removeTag = (tagId: string) => {
  const index = selectedDocument.value?.tags.findIndex(tag => tag.id === tagId)
  if (index !== undefined && index !== -1) {
    selectedDocument.value?.tags.splice(index, 1)
    message.success('标签已移除')
  }
}

const onToggleFavorite = (doc: DocumentRow) => {
  doc.isFavorite = !doc.isFavorite
  message.success(`文档 ${doc.isFavorite ? '已收藏' : '取消收藏'}`)
}

const onArchiveDocument = (doc: DocumentRow) => {
  doc.status = 'archived'
  message.success(`文档 ${doc.name} 已归档`)
}

const checkDocumentStatus = (doc: DocumentRow) => {
  message.info(`文档 ${doc.name} 当前状态：${doc.status}`)
}

const showRenameModal = (doc: DocumentRow) => {
  selectedDocument.value = doc
  renameForm.value = { name: doc.name }
  renameModalVisible.value = true
}

const handleRenameDocument = async () => {
  if (!selectedDocument.value || !renameForm.value.name.trim()) {
    message.warning('请输入文档名称')
    return
  }
  renameLoading.value = true
  try {
    await renameDocument(selectedDocument.value.key, renameForm.value.name)
    message.success('文档重命名成功')
    renameModalVisible.value = false
    await loadDocuments()
  } catch (error) {
    console.error('重命名文档失败:', error)
    message.error('重命名文档失败')
  } finally {
    renameLoading.value = false
  }
}

const showMoveModal = (doc: DocumentRow) => {
  selectedDocument.value = doc
  moveForm.value = { knowledgeBaseId: '' }
  moveModalVisible.value = true
}

const handleMoveDocument = async () => {
  if (!selectedDocument.value || !moveForm.value.knowledgeBaseId) {
    message.warning('请选择目标知识库')
    return
  }
  moveLoading.value = true
  try {
    await moveDocument(selectedDocument.value.key, moveForm.value.knowledgeBaseId)
    message.success('文档移动成功')
    moveModalVisible.value = false
    await loadDocuments()
  } catch (error) {
    console.error('移动文档失败:', error)
    message.error('移动文档失败')
  } finally {
    moveLoading.value = false
  }
}

const showCopyModal = (doc: DocumentRow) => {
  selectedDocument.value = doc
  copyForm.value = { knowledgeBaseId: '' }
  copyModalVisible.value = true
}

const handleCopyDocument = async () => {
  if (!selectedDocument.value || !copyForm.value.knowledgeBaseId) {
    message.warning('请选择目标知识库')
    return
  }
  copyLoading.value = true
  try {
    await copyDocument(selectedDocument.value.key, copyForm.value.knowledgeBaseId)
    message.success('文档复制成功')
    copyModalVisible.value = false
    await loadDocuments()
  } catch (error) {
    console.error('复制文档失败:', error)
    message.error('复制文档失败')
  } finally {
    copyLoading.value = false
  }
}

const formatFileSize = (size: string | number | undefined) => {
  // 处理空值
  if (size === undefined || size === null || size === '') return '0 Bytes'
  
  // 转换为数字
  let bytes: number
  if (typeof size === 'string') {
    // 移除可能的空格和特殊字符
    const cleanSize = size.trim().replace(/[^\d.-]/g, '')
    bytes = parseFloat(cleanSize)
  } else {
    bytes = Number(size)
  }
  
  // 验证数字有效性
  if (isNaN(bytes) || bytes < 0) return '0 Bytes'
  if (bytes === 0) return '0 Bytes'
  
  // 格式化文件大小
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  const formattedSize = parseFloat((bytes / Math.pow(k, i)).toFixed(2))
  
  return `${formattedSize} ${sizes[i]}`
}

const formatUploadTime = (time: string | undefined) => {
  if (!time) return ''
  try {
    const date = new Date(time)
    if (isNaN(date.getTime())) return time
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return time
  }
}

const normalizeStatus = (status: unknown): string => {
  if (typeof status === 'number') {
    switch (status) { 
      case 0: return 'uploading'
      case 1: return 'processing'
      case 2: return 'completed'
      case 3: return 'failed'
      case 4: return 'archived'
      default: return 'processing'
    }
  }
  if (typeof status === 'string') {
    const num = parseInt(status)
    if (!isNaN(num)) {
      return normalizeStatus(num)
    }
    return status
  }
  return 'processing'
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'processing':
      return 'blue'
    case 'completed':
      return 'green'
    case 'failed':
      return 'red'
    case 'archived':
      return 'gray'
    default:
      return 'blue'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'processing':
      return '处理中'
    case 'completed':
      return '已完成'
    case 'failed':
      return '处理失败'
    case 'archived':
      return '已归档'
    default:
      return status
  }
}

// 移除未使用的函数

// 批量删除文档
const handleBatchDelete = async () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的文档')
    return
  }
  
  try {
    batchDeleteLoading.value = true
    const documentIds = selectedRowKeys.value.map(id => parseInt(id))
    await batchDeleteDocuments(documentIds)
    message.success(`成功删除 ${documentIds.length} 个文档`)
    selectedRowKeys.value = []
    selectedDocuments.value = []
    await loadDocuments()
  } catch (error) {
    console.error('批量删除失败:', error)
    const errorMessage = (error as Error)?.message || '未知错误'
    message.error(`批量删除失败: ${errorMessage}`)
  } finally {
    batchDeleteLoading.value = false
  }
}

const beforeUpload = (file: File) => {
  uploadForm.value.fileList = [file]
  return false
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadDocuments()
}

// 定义API响应类型
interface DocumentApiResponse {
  data?: {
    documents?: DocumentApiItem[];
    list?: DocumentApiItem[];
    total?: number;
  };
}

interface DocumentApiItem {
  id?: string | number;
  documentId?: string | number;
  knowledgeBaseId?: string | number;
  kbId?: string | number;
  knowledge_base_id?: string | number;
  originalFilename?: string;
  fileName?: string;
  fileType?: string;
  fileSize?: string | number;
  uploadTime?: string;
  createTime?: string;
  status?: number | string;
  isFavorite?: boolean;
  tags?: Array<{ id?: string | number; name?: string } | string | number>;
}

const loadDocuments = async () => {
  loading.value = true;
  console.log('📄 [loadDocuments] Starting document load...', {
    knowledgeBaseFilter: knowledgeBaseFilter.value,
    statusFilter: statusFilter.value,
    currentPage: currentPage.value,
    isAdmin: authStore.isAdmin,
  });

  try {
    // 1. 准备API参数
    const filterParams = {
          page: currentPage.value,
          size: pageSize.value,
        sortBy: 'uploadTime' as const,
        order: 'desc' as const,
      knowledgeBaseIds: knowledgeBaseFilter.value ? [knowledgeBaseFilter.value] : undefined,
      status: statusFilter.value ? Number(statusFilter.value) : undefined,
    };

    console.log('📋 [loadDocuments] Calling API with params:', filterParams);
    
    // 2. 调用API
    const res: DocumentApiResponse = await getDocumentsWithFilter(filterParams);
    console.log('✅ [loadDocuments] API call successful, response:', res);

    // 3. 处理响应数据
    const list = res?.data?.documents || res?.data?.list || [];
    const total = res?.data?.total || 0;

    console.log('📊 [loadDocuments] Processing response data:', { listLength: list.length, total });

    if (list && Array.isArray(list)) {
      // 4. 数据映射
      const normalized = list.map((item: DocumentApiItem, idx: number) => {
        const kbId = item.knowledgeBaseId ?? item.kbId ?? item.knowledge_base_id;
        const idStr = kbId != null ? String(kbId) : '';
        let kbName = kbIdToName.value.get(idStr) || `知识库${idStr}`;

        const documentId = item.id ?? item.documentId;
        const rawKey = documentId ?? `fallback_${idx}`;
      
      return {
        key: String(rawKey),
          name: item.originalFilename || item.fileName || '未命名文档',
        type: item.fileType || '文件',
        size: formatFileSize(item.fileSize),
        knowledgeBase: kbName,
          uploadTime: formatUploadTime(item.uploadTime || item.createTime),
        status: normalizeStatus(item.status),
        isFavorite: !!item.isFavorite,
          tags: (item.tags || []).map((t) => ({
            id: String(typeof t === 'object' && t !== null ? (t.id ?? '') : t),
            name: String(typeof t === 'object' && t !== null ? (t.name ?? '') : t),
          })),
        };
      });

      // 5. 更新状态
      totalItems.value = total;
      documents.value = normalized;

      console.log('✅ [loadDocuments] Finished loading documents.', { totalItems: totalItems.value, displayedCount: documents.value.length });
    } else {
      console.warn('⚠️ [loadDocuments] No document list found in response data.');
      documents.value = [];
      totalItems.value = 0;
    }
  } catch (error) {
    console.error('❌ [loadDocuments] Error loading documents:', error);
    documents.value = [];
    totalItems.value = 0;
    message.error('加载文档失败，请检查网络连接或联系管理员');
  } finally {
    loading.value = false;
  }
};


const viewDocument = (doc: DocumentRow) => { 
  router.push({
    name: 'DocumentDetail',
    params: { id: doc.key },
    query: { 
      from: 'document-management'
    }
  })
}
const downloadDocument = (doc: DocumentRow) => { message.success(`开始下载：${doc.name}`) }

// 删除文档
const onDeleteDocument = (doc: DocumentRow) => {
  // 显示确认对话框
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除文档 "${doc.name}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        console.log('🗑️ Deleting document:', {
          docKey: doc.key,
          docName: doc.name,
          docStatus: doc.status
        })
        
        // 检查是否为fallback key
        if (doc.key.startsWith('row_')) {
          message.error('无法获取有效的文档ID，删除失败。请刷新页面后重试。')
          console.error('Document key is fallback value:', doc.key)
          return
        }
        
        // 确保文档ID是有效的
        const documentId = String(doc.key).trim()
        if (!documentId || documentId === 'undefined' || documentId === 'null') {
          message.error('文档ID无效，删除失败')
          console.error('Invalid document ID:', documentId)
          return
        }
        
        console.log('🗑️ Attempting to delete document with ID:', documentId)
        await deleteDocument(documentId)
        message.success(`文档 "${doc.name}" 删除成功`)
        // 刷新文档列表
        await loadDocuments()
      } catch (error) {
        console.error('删除文档失败:', error)
        // 显示更详细的错误信息
        const errorMessage = (error as Error)?.message || '删除文档失败，请稍后重试'
        message.error(errorMessage)
      }
    }
  })
}

// 权限控制已移至后端，前端简化为显示逻辑

// 加载统计数据
const loadStatistics = async () => {
  try {
    console.log('📊 加载文档统计数据...');
    
    // 由于后端可能还没有实现统计API，我们通过文档列表API来计算统计数据
    // 获取所有文档（不分页）来计算统计
    const response = await getDocumentsWithFilter({
      page: 1,
      size: 10000, // 获取大量数据来计算统计
      sortBy: 'uploadTime',
      order: 'desc'
    });
    
    const documents = response?.data?.documents || response?.data?.list || [];
    
    // 计算统计数据
    const statistics = {
      total: documents.length,
      processing: documents.filter((doc: DocumentApiItem) => {
        const status = doc.status;
        return status === 1 || status === '1' || status === 'processing';
      }).length,
      completed: documents.filter((doc: DocumentApiItem) => {
        const status = doc.status;
        return status === 2 || status === '2' || status === 'completed';
      }).length,
      failed: documents.filter((doc: DocumentApiItem) => {
        const status = doc.status;
        return status === 3 || status === '3' || status === 'failed';
      }).length
    };
    
    console.log('📊 计算得到的统计数据:', statistics);
    
    // 更新统计卡片数据
    stats.value = [
      { 
        title: '文档总数', 
        value: `${statistics.total}件`, 
        desc: '系统中的文档总数量', 
        icon: FileTextOutlined, 
        iconBg: '#e1e9ff', 
        iconColor: '#3882F8' 
      },
      { 
        title: '处理中', 
        value: `${statistics.processing}件`, 
        desc: '正在处理中的文档', 
        icon: SyncOutlined, 
        iconBg: '#e6f7ff', 
        iconColor: '#1890ff' 
      },
      { 
        title: '已完成', 
        value: `${statistics.completed}件`, 
        desc: '已成功处理的文档', 
        icon: CheckCircleOutlined, 
        iconBg: '#f6ffed', 
        iconColor: '#52c41a' 
      },
      { 
        title: '处理失败', 
        value: `${statistics.failed}件`, 
        desc: '处理失败的文档', 
        icon: CloseCircleOutlined, 
        iconBg: '#fff1f0', 
        iconColor: '#f5222d' 
      }
    ];
    
    console.log('✅ 统计数据加载完成:', statistics);
  } catch (error) {
    console.error('❌ 加载统计数据失败:', error);
    // 保持默认值，不显示错误给用户
  }
};

const loadKnowledgeBaseOptions = async () => {
  try {
    console.log('📚 加载知识库选项，用户角色:', authStore.userRole);
    
    const res = await getKnowledgeBases({}) as { data?: unknown }
    const r = res
    let list: unknown[] = []
    const data = r?.data
    if (Array.isArray(data)) {
      list = data
    } else if (data && typeof data === 'object' && 'knowledgeBases' in data) {
      const kbList = (data as { knowledgeBases?: unknown }).knowledgeBases
      if (Array.isArray(kbList)) list = kbList
    }
    type ApiKB = { id?: string | number; kbId?: string | number; key?: string | number; name?: string }
    const toKB = (item: unknown): KnowledgeBase => {
      const obj = item as Partial<ApiKB>
      return { id: String(obj.id ?? obj.kbId ?? obj.key ?? ''), name: obj.name ?? '未命名知识库' }
    }
    
    // 直接使用所有知识库，后端会根据用户权限返回相应的知识库
    const allKnowledgeBases = list.map(toKB);
    
    console.log('📚 知识库加载完成:', {
      userRole: authStore.userRole,
      isAdmin: authStore.isAdmin,
      total: allKnowledgeBases.length,
      knowledgeBases: allKnowledgeBases.map(kb => ({
        id: kb.id,
        name: kb.name,
        normalizedId: normalizeKnowledgeBaseId(kb.id)
      }))
    });
    
    // 直接使用所有知识库（后端已处理权限过滤）
    const accessibleKnowledgeBases = allKnowledgeBases;
    
    // 智能去重：基于规范化ID和名称的组合去重
    const uniqueKnowledgeBases = accessibleKnowledgeBases.reduce((unique, kb) => {
      const normalizedId = normalizeKnowledgeBaseId(kb.id);
      const key = `${normalizedId}_${kb.name}`;
      
      // 检查是否已存在相同规范化ID和名称的知识库
      const existing = unique.find(existing => {
        const existingNormalizedId = normalizeKnowledgeBaseId(existing.id);
        const existingKey = `${existingNormalizedId}_${existing.name}`;
        return existingKey === key;
      });
      
      if (!existing) {
        // 没有重复，直接添加
        unique.push(kb);
      } else {
        // 有重复，优先保留更完整的ID（通常是长ID）
        if (kb.id.length > existing.id.length) {
          const index = unique.indexOf(existing);
          unique[index] = kb;
          console.log(`🔄 替换重复知识库: ${existing.id} -> ${kb.id} (${kb.name})`);
        } else {
          console.log(`⏭️ 跳过重复知识库: ${kb.id} (已有 ${existing.id}) (${kb.name})`);
        }
      }
      
      return unique;
    }, [] as typeof accessibleKnowledgeBases);
    
    knowledgeBases.value = uniqueKnowledgeBases;
    
    // 显示去重前后的知识库信息
    console.log('📋 知识库去重信息:', {
      去重前数量: accessibleKnowledgeBases.length,
      去重后数量: uniqueKnowledgeBases.length,
      去重前列表: accessibleKnowledgeBases.map(kb => ({ 
        id: kb.id, 
        name: kb.name, 
        normalizedId: normalizeKnowledgeBaseId(kb.id),
        key: `${normalizeKnowledgeBaseId(kb.id)}_${kb.name}`,
        idType: typeof kb.id,
        idLength: String(kb.id).length
      })),
      去重后列表: uniqueKnowledgeBases.map(kb => ({ 
        id: kb.id, 
        name: kb.name, 
        normalizedId: normalizeKnowledgeBaseId(kb.id),
        key: `${normalizeKnowledgeBaseId(kb.id)}_${kb.name}`,
        idType: typeof kb.id,
        idLength: String(kb.id).length
      })),
      映射测试: {
        '19337107299766272': normalizeKnowledgeBaseId('19337107299766272'),
        '19232187194019840': normalizeKnowledgeBaseId('19232187194019840'),
        '1': normalizeKnowledgeBaseId('1'),
        '2': normalizeKnowledgeBaseId('2'),
        '3': normalizeKnowledgeBaseId('3')
      }
    });
    
    // 🚨 关键调试：检查是否有未映射的知识库ID
    const unmappedKbs = uniqueKnowledgeBases.filter(kb => {
      const normalized = normalizeKnowledgeBaseId(kb.id);
      return normalized === kb.id && !/^\d+$/.test(normalized);
    });
    
    if (unmappedKbs.length > 0) {
      console.error('🚨 发现未映射的知识库ID，需要更新映射表:', {
        unmappedKbs: unmappedKbs.map(kb => ({
          id: kb.id,
          name: kb.name,
          建议映射: `'${kb.id}': '${uniqueKnowledgeBases.indexOf(kb) + 1}'`
        })),
        当前映射表: {
          '19337107299766272': '1',
          '19232187194019840': '2'
        },
        建议的完整映射表: uniqueKnowledgeBases.reduce((map, kb, index) => {
          if (!/^\d+$/.test(kb.id)) {
            map[kb.id] = String(index + 1);
          }
          return map;
        }, {} as Record<string, string>)
      });
    }
    
    // 🚨 重要：请将以下信息与数据库中的知识库表进行对比
    console.log('🗃️ 数据库对比信息 - 请检查数据库中的知识库表:', {
      message: '请将以下前端获取的知识库信息与数据库中的knowledge_bases表进行对比',
      前端获取的知识库: uniqueKnowledgeBases.map(kb => ({
        前端ID: kb.id,
        知识库名称: kb.name,
        规范化后ID: normalizeKnowledgeBaseId(kb.id)
      })),
      期望的映射关系: {
        '技术文档库应该映射到': '1',
        '测试知识库2应该映射到': '2'
      },
      请检查数据库: {
        表名: 'knowledge_bases',
        需要确认的字段: ['id', 'name'],
        问题: '前端ID与数据库ID是否匹配？'
      }
    });
    
    // 添加调试日志
    if (import.meta.env.DEV) {
      console.log('🔍 Knowledge Base Loading Debug:', {
        rawResponse: r,
        extractedList: list,
        allKnowledgeBases: allKnowledgeBases.length,
        accessibleKnowledgeBases: accessibleKnowledgeBases.length,
        mappedKnowledgeBases: knowledgeBases.value,
        knowledgeBaseMap: Object.fromEntries(kbIdToName.value)
      });
    }
  } catch (err) {
    if (import.meta.env.DEV) {
      console.warn('加载知识库失败', err)
    }
  } finally {
    kbLoaded.value = true
  }
}

onMounted(async () => {
  try {
    await loadStatistics() // 加载统计数据
  await loadKnowledgeBaseOptions()
  await loadDocuments()
  } catch (error) {
    console.error('页面初始化失败:', error)
  }
})
</script>

<style scoped>
/* 与首页一致的两栏布局与样式 */
.app-layout { display: grid; grid-template-columns: 260px 1fr; min-height: 100vh; background: #f5f7fb; }
.sidebar { background: #1e6bed; color: #fff; display: flex; flex-direction: column; padding: 14px 12px; }
.brand { padding: 6px 10px 16px 10px; border-bottom: 1px solid rgba(255,255,255,0.15); }
.brand-title { font-weight: 700; font-size: 18px; }
.user-card { display: flex; align-items: center; gap: 12px; padding: 14px 8px; }
.avatar { width: 36px; height: 36px; border-radius: 50%; background: #3882F8; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.user-meta .user-name { font-weight: 600; }
.user-meta .user-role { opacity: 0.9; font-size: 12px; }
.menu { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.menu-item { width: 100%; text-align: left; background: transparent; border: none; color: #e8f0ff; padding: 10px 10px; border-radius: 8px; display: flex; align-items: center; gap: 10px; cursor: pointer; }
.menu-item:hover { background: rgba(255,255,255,0.12); }
.menu-item.active { background: #0d59d6; }
.menu-icon { width: 18px; display: inline-block; text-align: center; }
.content { display: flex; flex-direction: column; }
.topbar { height: 56px; background: #fff; display: flex; align-items: center; justify-content: space-between; padding: 0 16px; border-bottom: 1px solid #eef0f5; }
.search-box { position: relative; width: 380px; }
.search-input { width: 100%; height: 34px; border-radius: 18px; border: 1px solid #e3e6f0; padding: 0 14px 0 34px; outline: none; }
.search-icon { position: absolute; left: 10px; top: 7px; opacity: 0.6; }
.actions { display: flex; align-items: center; gap: 18px; }
.admin-badge { 
  display: flex; 
  align-items: center; 
  gap: 4px; 
  background: linear-gradient(135deg, #ffd700, #ffed4e); 
  color: #8b4513; 
  padding: 4px 8px; 
  border-radius: 12px; 
  font-size: 12px; 
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(255, 215, 0, 0.3);
}
.admin-icon { font-size: 14px; }
.admin-text { font-size: 11px; }
.bell { position: relative; }
.badge { position: absolute; top: -8px; right: -8px; background: #ff4d4f; color: #fff; font-size: 10px; border-radius: 10px; padding: 0 4px; }
.profile-dot { font-size: 10px; opacity: 0.6; }

/* 页面主体样式 */
.document-page { padding: 24px; }
.page-header { margin-bottom: 25px; }
.page-header h1 { font-size: 1.8rem; color: #5a5c69; margin-bottom: 5px; }
.breadcrumb { display: flex; list-style: none; color: #5a5c69; font-size: 0.9rem; padding: 0; }
.breadcrumb li:after { content: '/'; margin: 0 10px; }
.breadcrumb li:last-child:after { content: ''; }
.breadcrumb a { color: #3882F8; text-decoration: none; }
.stats-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; margin-bottom: 30px; }
.stat-card { border-radius: 8px; box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05); }
.stat-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.stat-card-title { color: #5a5c69; font-size: 0.9rem; font-weight: 600; }
.stat-card-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.stat-card-value { font-size: 1.8rem; font-weight: bold; color: #3882F8; margin-bottom: 5px; }
.stat-card-desc { color: #5a5c69; font-size: 0.85rem; }
.content-section { background: white; border-radius: 8px; padding: 25px; box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05); margin-bottom: 30px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title-with-info { display: flex; flex-direction: column; gap: 8px; }
.section-title { font-size: 1.2rem; color: #5a5c69; margin: 0; }
.permission-info { display: flex; align-items: center; }
.permission-text { 
  font-size: 12px; 
  padding: 4px 8px; 
  border-radius: 6px; 
  font-weight: 500;
}
.permission-text.admin { 
  background: #e8f5e8; 
  color: #2d5a2d; 
  border: 1px solid #c3e6c3;
}
.permission-text.user { 
  background: #fff3cd; 
  color: #856404; 
  border: 1px solid #ffeaa7;
}
.header-actions { display: flex; align-items: center; gap: 10px; }
.action-buttons { display: flex; gap: 6px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }

.status-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-details {
  font-size: 12px;
  color: #666;
}

.status-progress {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.progress-text {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

/* 标签样式 */
.current-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 10px; }

@media (max-width: 992px) {
  .search-box { width: 220px; }
  .app-layout { grid-template-columns: 220px 1fr; }
  }
  </style>
