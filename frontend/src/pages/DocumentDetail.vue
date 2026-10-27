<template>
  <div class="document-detail-page">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="nav-content">
        <div class="nav-left">
          <a-button type="text" @click="goBack" class="back-btn">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            {{ backButtonText }}
          </a-button>
        </div>
        <div class="nav-center">
          <h1 class="doc-title">
            <a-spin v-if="loadingDoc" size="small" style="margin-right: 8px;" />
            {{ doc?.title || doc?.originalFilename || '—' }}
          </h1>
        </div>
        <div class="nav-right">
          <a-dropdown>
            <a-button>
              <template #icon>
                <EditOutlined />
              </template>
              更多操作
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="showRenameModal">
                  <EditOutlined /> 重命名
                </a-menu-item>
                <a-menu-item @click="showMoveModal">
                  <FolderOpenOutlined /> 移动到
                </a-menu-item>
                <a-menu-item @click="showCopyModal">
                  <CopyOutlined /> 复制到
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="showEditModal">
                  <EditOutlined /> 编辑文档
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button type="primary" @click="handleDownload" :loading="downloading">
            <template #icon>
              <DownloadOutlined />
            </template>
            下载文档
          </a-button>
        </div>
      </div>
    </header>

    <!-- 文档信息卡片 -->
    <div class="doc-info-card">
      <a-card>
        <div class="doc-info-content">
          <div class="doc-info-left">
            <div class="doc-icon">
              <component :is="getFileIcon(doc?.fileType)" />
            </div>
            <div class="doc-basic-info">
              <h3>{{ doc?.title || doc?.originalFilename || '未命名文档' }}</h3>
              <p class="doc-description">{{ doc?.description || '暂无描述' }}</p>
            </div>
          </div>
          <div class="doc-info-right">
            <div class="doc-meta">
              <div class="meta-item">
                <span class="meta-label">文件类型:</span>
                <span class="meta-value">{{ doc?.fileType || '未知' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">文件大小:</span>
                <span class="meta-value" :class="{ 'size-warning': doc?.fileSize === 0 }">
                  {{ formatSize(doc?.fileSize) }}
                  <span v-if="doc?.fileSize === 0" class="size-warning-text">⚠️</span>
                </span>
              </div>
              <div class="meta-item">
                <span class="meta-label">所属知识库:</span>
                <span class="meta-value">{{ doc?.knowledgeBaseName || '未知知识库' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">上传时间:</span>
                <span class="meta-value">{{ formatTime(doc?.uploadTime || doc?.createdAt) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">文档状态:</span>
                <a-tag :color="getStatusColor(doc?.status)">
                  {{ getStatusText(doc?.status) }}
                </a-tag>
              </div>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 文档内容区域 -->
    <div class="doc-content-area">
      <a-card>
        <template #title>
          <div class="content-header">
            <span>文档内容</span>
            <a-spin v-if="loadingContent" size="small" />
          </div>
        </template>
        
        <div class="doc-content">
          <!-- 真实内容显示 -->
          <div v-if="docContent && isValidContent(docContent)" class="content-text">
            <div class="content-header-info">
              <span class="content-type-badge">{{ getContentTypeBadge() }}</span>
              <span class="content-length">{{ docContent.length }} 字符</span>
            </div>
            <pre class="content-pre">{{ docContent }}</pre>
          </div>
          
          <!-- 特殊文件类型提示 -->
          <div v-else-if="docContent && isSpecialFileType(docContent)" class="special-file-info">
            <div class="special-file-icon">
              <FileTextOutlined />
            </div>
            <div class="special-file-content">
              <h4>{{ getSpecialFileTitle() }}</h4>
              <p>{{ docContent }}</p>
              <div class="special-file-actions">
                <a-button type="primary" @click="handleDownload">
                  <template #icon><DownloadOutlined /></template>
                  下载文档查看
                </a-button>
              </div>
            </div>
          </div>
          
          <!-- 错误或无内容状态 -->
          <div v-else-if="!loadingContent" class="no-content">
            <div class="no-content-icon">
              <FileTextOutlined />
            </div>
            <p v-if="docContent && docContent.includes('没有权限')">您没有权限预览该文档</p>
            <p v-else-if="docContent && docContent.includes('加载失败')">文档预览功能暂时不可用</p>
            <p v-else>文档内容预览功能暂未实现</p>
            <p class="no-content-tip">
              <span v-if="docContent && docContent.includes('没有权限')">
                请联系管理员获取预览权限，或使用下载功能查看文档内容
              </span>
              <span v-else-if="doc?.fileSize === 0">
                <strong>⚠️ 文档大小显示为0B，可能的原因：</strong><br>
                1. 文档上传时内容丢失<br>
                2. 文档处理未完成<br>
                3. 后端存储问题<br>
                <strong>建议：</strong>重新上传文档或联系管理员检查
              </span>
              <span v-else>
                请联系管理员获取文档内容，或等待后续功能更新
              </span>
            </p>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 评论区域 -->
    <div class="comments-section">
      <a-card>
        <template #title>
          <div class="section-header">
            <MessageOutlined />
            <span>评论 ({{ comments.length }})</span>
          </div>
        </template>
        
        <!-- 添加评论 -->
        <div class="add-comment" v-if="commentsAvailable">
          <a-textarea
            v-model:value="newComment"
            placeholder="添加评论..."
            :rows="3"
            :maxlength="500"
            show-count
          />
          <div class="comment-actions">
            <a-button type="primary" @click="handleAddComment" :loading="addingComment">
              <template #icon>
                <SendOutlined />
              </template>
              发表评论
            </a-button>
          </div>
        </div>
        
        <!-- 评论功能不可用提示 -->
        <div class="comment-disabled" v-else>
          <div class="disabled-notice">
            <p style="color: #999; font-size: 14px; margin: 0;">
              💬 评论功能暂时不可用
            </p>
            <p style="color: #666; font-size: 12px; margin: 4px 0 0 0;">
              由于后端数据库配置问题，评论功能暂时无法使用
            </p>
          </div>
        </div>
        
        <!-- 评论列表 -->
        <div class="comments-list" v-if="comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <div class="comment-author">
                <strong>{{ comment.authorName || comment.author?.realName || '匿名用户' }}</strong>
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
              </div>
              <div class="comment-actions" v-if="canEditComment(comment)">
                <a-button type="text" size="small" @click="editComment(comment)">
                  编辑
                </a-button>
                <a-button type="text" size="small" danger @click="deleteCommentConfirm(comment)">
                  删除
                </a-button>
              </div>
            </div>
            <div class="comment-content">
              <p v-if="!comment.editing">{{ comment.content }}</p>
              <div v-else class="comment-edit">
                <a-textarea
                  v-model:value="comment.editContent"
                  :rows="2"
                  :maxlength="500"
                />
                <div class="edit-actions">
                  <a-button size="small" @click="cancelEditComment(comment)">取消</a-button>
                  <a-button size="small" type="primary" @click="saveEditComment(comment)">保存</a-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div v-else class="no-comments">
          <a-empty description="暂无评论" />
          <div class="comment-notice" v-if="comments.length === 0 && !loadingComments">
            <p style="color: #999; font-size: 12px; margin-top: 8px;">
              评论功能暂时不可用，可能是后端数据库配置问题
            </p>
            <p style="color: #666; font-size: 11px; margin-top: 4px;">
              请联系管理员修复数据库字段问题
            </p>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 编辑文档模态框 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑文档"
      @ok="handleEditDocument"
      @cancel="editModalVisible = false"
      :confirmLoading="editing"
      width="600px"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="文档描述">
          <a-textarea
            v-model:value="editForm.description"
            placeholder="请输入文档描述"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="文档内容">
          <a-textarea
            v-model:value="editForm.content"
            placeholder="请输入文档内容"
            :rows="8"
          />
        </a-form-item>
      </a-form>
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
              :disabled="kb.name === doc?.knowledgeBaseName"
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { 
  ArrowLeftOutlined, 
  DownloadOutlined,
  EditOutlined,
  FileTextOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FileImageOutlined,
  FileOutlined,
  MessageOutlined,
  SendOutlined,
  CopyOutlined,
  FolderOpenOutlined
} from '@ant-design/icons-vue'
import { 
  getDocumentDetail, 
  getDocumentPreview, 
  downloadDocument, 
  editDocument,
  renameDocument,
  moveDocument,
  copyDocument,
  getDocumentComments,
  addDocumentComment,
  updateComment,
  deleteComment
} from '@/api/document'
import { getKnowledgeBases } from '@/api/knowledgeBase'

const router = useRouter()
const route = useRoute()

const docId = computed(() => String(route.params.id || ''))
const from = computed(() => String(route.query.from || ''))
const kbId = computed(() => String(route.query.kbId || ''))

const doc = ref<any>(null)
const loadingDoc = ref(false)
const loadingContent = ref(false)
const downloading = ref(false)
const docContent = ref<string>('')

// 编辑相关
const editModalVisible = ref(false)
const editing = ref(false)
const editForm = ref({
  description: '',
  content: ''
})

// 重命名相关
const renameModalVisible = ref(false)
const renameLoading = ref(false)
const renameForm = ref({ name: '' })

// 移动相关
const moveModalVisible = ref(false)
const moveLoading = ref(false)
const moveForm = ref({ knowledgeBaseId: '' })

// 复制相关
const copyModalVisible = ref(false)
const copyLoading = ref(false)
const copyForm = ref({ knowledgeBaseId: '' })

// 知识库列表
const knowledgeBases = ref<any[]>([])

// 评论相关
const comments = ref<any[]>([])
const loadingComments = ref(false)
const newComment = ref('')
const addingComment = ref(false)
const commentsAvailable = ref(true) // 评论功能是否可用

// 返回按钮文本
const backButtonText = computed(() => {
  if (from.value === 'knowledge-base') {
    return '返回知识库详情'
  } else if (from.value === 'document-management') {
    return '返回文档管理'
  } else if (from.value === 'ai-assistant') {
    return '返回智能秘书'
  }
  return '返回'
})

const canEditComment = (_comment: any) => {
  return true
}

// 返回逻辑
const goBack = () => {
  if (from.value === 'knowledge-base' && kbId.value) {
    router.push({ name: 'KnowledgeBaseDetail', params: { id: kbId.value } })
  } else if (from.value === 'document-management') {
    router.push('/documents')
  } else if (from.value === 'ai-assistant') {
    router.push('/ai-assistant')
  } else {
    router.push('/documents')
  }
}

// 文件类型图标
const getFileIcon = (fileType?: string) => {
  if (!fileType) return FileOutlined
  
  const type = fileType.toLowerCase()
  if (type.includes('pdf')) return FilePdfOutlined
  if (type.includes('doc')) return FileWordOutlined
  if (type.includes('xls') || type.includes('excel')) return FileExcelOutlined
  if (type.includes('image') || type.includes('jpg') || type.includes('png')) return FileImageOutlined
  return FileTextOutlined
}

// 格式化文件大小
const formatSize = (size?: number) => {
  if (!size || isNaN(size)) return '0B'
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size/1024).toFixed(1)}KB`
  if (size < 1024 * 1024 * 1024) return `${(size/(1024*1024)).toFixed(1)}MB`
  return `${(size/(1024*1024*1024)).toFixed(1)}GB`
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '—'
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

// 状态颜色
const getStatusColor = (status?: string) => {
  switch (status) {
    case 'processing': return 'blue'
    case 'completed': return 'green'
    case 'failed': return 'red'
    case 'archived': return 'gray'
    default: return 'blue'
  }
}

// 状态文本
const getStatusText = (status?: string) => {
  switch (status) {
    case 'processing': return '处理中'
    case 'completed': return '已完成'
    case 'failed': return '处理失败'
    case 'archived': return '已归档'
    default: return status || '未知'
  }
}

// 检查是否为有效内容
const isValidContent = (content: string): boolean => {
  if (!content) return false
  
  // 排除错误信息
  const errorKeywords = ['没有权限', '加载失败', '无法获取', '暂未实现', '不支持']
  return !errorKeywords.some(keyword => content.includes(keyword))
}

// 检查是否为特殊文件类型提示
const isSpecialFileType = (content: string): boolean => {
  if (!content) return false
  
  // 检查是否为PDF或Word文档的提示信息
  const specialKeywords = ['PDF', 'Word', 'DOCX', 'DOC', '无法预览文本', '需要集成', 'Apache']
  return specialKeywords.some(keyword => content.includes(keyword))
}

// 获取内容类型标识
const getContentTypeBadge = (): string => {
  if (!doc.value?.fileType) return '文本'
  
  const typeMap: Record<string, string> = {
    'txt': '纯文本',
    'pdf': 'PDF文档',
    'docx': 'Word文档',
    'doc': 'Word文档',
    'md': 'Markdown',
    'json': 'JSON',
    'xml': 'XML',
    'csv': 'CSV'
  }
  
  return typeMap[doc.value.fileType.toLowerCase()] || '文档'
}

// 获取特殊文件类型标题
const getSpecialFileTitle = (): string => {
  if (!doc.value?.fileType) return '特殊文件类型'
  
  const typeMap: Record<string, string> = {
    'pdf': 'PDF文档',
    'docx': 'Word文档',
    'doc': 'Word文档',
    'ppt': 'PowerPoint演示文稿',
    'pptx': 'PowerPoint演示文稿',
    'xls': 'Excel表格',
    'xlsx': 'Excel表格'
  }
  
  return typeMap[doc.value.fileType.toLowerCase()] || '特殊文件类型'
}

// 加载文档详情
const loadDocumentDetail = async () => {
  loadingDoc.value = true
  loadingContent.value = true
  
  try {
    console.log('🔍 Loading document detail for ID:', docId.value)
    
    // 直接使用文档详情API
    const res = await getDocumentDetail(docId.value)
    console.log('📋 Document detail response:', res)
    
    if (res?.data) {
      const targetDoc = res.data
      
      // 格式化文件大小 - 确保是数字
      const formatFileSize = (size: any): number => {
        if (typeof size === 'string') {
          const parsed = parseInt(size, 10)
          return isNaN(parsed) ? 0 : parsed
        }
        return typeof size === 'number' ? size : 0
      }
      
      // 获取实际文件大小 - 如果后端返回0，尝试从其他字段获取
      const getActualFileSize = (doc: any): number => {
        // 首先尝试 fileSize 字段
        let size = formatFileSize(doc.fileSize)
        
        // 如果 fileSize 为 0，尝试其他可能的字段
        if (size === 0) {
          size = formatFileSize(doc.size) || formatFileSize(doc.file_size) || formatFileSize(doc.fileSizeBytes)
        }
        
        // 如果仍然为 0，尝试从文件名推断（这是一个fallback）
        if (size === 0 && doc.originalFilename) {
          // 这里可以添加一些启发式规则，但通常后端应该提供正确的文件大小
          console.warn('文件大小为0，可能需要后端修复:', doc.originalFilename)
        }
        
        return size
      }
      
      // 格式化状态 - 将数字状态码转换为字符串
      const formatStatus = (status: any): string => {
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
            return formatStatus(num)
          }
          return status
        }
        return 'processing'
      }
      
      // 获取知识库名称 - 尝试多种可能的字段
      const getKnowledgeBaseName = (doc: any): string => {
        // 直接字段
        if (doc.knowledgeBaseName) return doc.knowledgeBaseName
        if (doc.knowledge_base_name) return doc.knowledge_base_name
        
        // 嵌套对象
        if (doc.knowledgeBase?.name) return doc.knowledgeBase.name
        if (doc.knowledge_base?.name) return doc.knowledge_base.name
        
        // 通过知识库ID查找
        const kbId = doc.knowledgeBaseId || doc.knowledge_base_id
        if (kbId) {
          // 知识库ID到名称的映射
          const kbIdMap: Record<string, string> = {
            '1': '技术文档库',
            '2': '产品手册库', 
            '3': '培训资料库',
            '19337107299766272': '测试知识库2',
            '19232187194019840': '测试知识库'
          }
          
          const kbName = kbIdMap[String(kbId)]
          if (kbName) {
            return kbName
          }
          
          return `知识库${kbId}`
        }
        
        // 如果完全没有知识库信息，尝试从文档ID推断
        // 或者显示一个通用的提示
        return '知识库信息缺失'
      }
      
      doc.value = {
        id: targetDoc.id || targetDoc.documentId,
        title: targetDoc.title || targetDoc.originalFilename || targetDoc.fileName || '未命名文档',
        description: targetDoc.description || '',
        fileType: targetDoc.fileType || '未知',
        fileSize: getActualFileSize(targetDoc),
        knowledgeBaseName: getKnowledgeBaseName(targetDoc),
        uploadTime: targetDoc.uploadTime || targetDoc.createdAt || targetDoc.createTime,
        status: formatStatus(targetDoc.status),
        originalFilename: targetDoc.originalFilename || targetDoc.fileName,
        uploaderId: targetDoc.uploaderId || targetDoc.uploader_id
      }
      
      // 设置编辑表单的初始值
      editForm.value = {
        description: targetDoc.description || '',
        content: targetDoc.content || ''
      }
      
      console.log('✅ Document detail loaded:', doc.value)
    } else {
      throw new Error('未找到指定的文档')
    }
  } catch (e: any) {
    console.error('❌ Failed to load document detail:', e)
    message.error(`获取文档详情失败: ${e?.message || '未知错误'}`)
    doc.value = {
      title: '加载失败',
      description: '无法获取文档信息',
      fileType: '未知',
      fileSize: 0,
      knowledgeBaseName: '未知知识库',
      uploadTime: null,
      status: 'failed'
    }
  } finally {
    loadingDoc.value = false
    loadingContent.value = false
  }
}

// 加载文档预览
const loadDocumentPreview = async () => {
  loadingContent.value = true
  
  try {
    console.log('🔍 Loading document preview for ID:', docId.value)
    const res: any = await getDocumentPreview(docId.value)
    console.log('📋 Document preview response:', res)
    
    // 处理预览响应
    let previewData = null
    if (res?.data) {
      previewData = res.data
    } else if (res && typeof res === 'object') {
      previewData = res
    }
    
    if (previewData) {
      if (previewData.content) {
        docContent.value = previewData.content
      } else if (previewData.text) {
        docContent.value = previewData.text
      } else {
        docContent.value = '该文档暂无内容预览'
      }
      
      console.log('✅ Document preview loaded:', docContent.value)
    } else {
      docContent.value = '无法获取文档预览'
    }
  } catch (e: any) {
    console.error('❌ Failed to load document preview:', e)
    
    // 根据错误类型显示不同的提示信息
    if (e?.response?.status === 500 && e?.response?.data?.message?.includes('无权限')) {
      docContent.value = '您没有权限预览该文档'
    } else if (e?.response?.status === 404) {
      docContent.value = '文档预览功能暂未实现'
    } else {
      docContent.value = '加载文档预览失败，请稍后重试'
    }
  } finally {
    loadingContent.value = false
  }
}

// 下载文档
const handleDownload = async () => {
  if (!doc.value) {
    message.error('文档信息不完整，无法下载')
    return
  }
  
  downloading.value = true
  try {
    console.log('🔍 Downloading document:', docId.value)
    const response = await downloadDocument(docId.value)
    const blob = response.data as Blob
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = doc.value.title || doc.value.originalFilename || `document_${docId.value}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    
    message.success('文档下载成功')
  } catch (e: any) {
    console.error('❌ Failed to download document:', e)
    message.error('文档下载失败')
  } finally {
    downloading.value = false
  }
}

// 显示编辑模态框
const showEditModal = () => {
  editModalVisible.value = true
}

// 编辑文档
const handleEditDocument = async () => {
  editing.value = true
  try {
    console.log('🔍 Editing document:', docId.value)
    await editDocument(docId.value, editForm.value)
    
    message.success('文档编辑成功')
    editModalVisible.value = false
    
    // 重新加载文档详情
    await loadDocumentDetail()
  } catch (e: any) {
    console.error('❌ Failed to edit document:', e)
    message.error('文档编辑失败')
  } finally {
    editing.value = false
  }
}

// 显示重命名模态框
const showRenameModal = () => {
  renameForm.value = { name: doc.value?.title || doc.value?.originalFilename || '' }
  renameModalVisible.value = true
}

// 重命名文档
const handleRenameDocument = async () => {
  if (!renameForm.value.name.trim()) {
    message.warning('请输入文档名称')
    return
  }
  renameLoading.value = true
  try {
    await renameDocument(docId.value, renameForm.value.name)
    message.success('文档重命名成功')
    renameModalVisible.value = false
    await loadDocumentDetail()
  } catch (e: any) {
    console.error('❌ Failed to rename document:', e)
    message.error('文档重命名失败')
  } finally {
    renameLoading.value = false
  }
}

// 显示移动模态框
const showMoveModal = async () => {
  await loadKnowledgeBases()
  moveForm.value = { knowledgeBaseId: '' }
  moveModalVisible.value = true
}

// 移动文档
const handleMoveDocument = async () => {
  if (!moveForm.value.knowledgeBaseId) {
    message.warning('请选择目标知识库')
    return
  }
  moveLoading.value = true
  try {
    await moveDocument(docId.value, moveForm.value.knowledgeBaseId)
    message.success('文档移动成功')
    moveModalVisible.value = false
    await loadDocumentDetail()
  } catch (e: any) {
    console.error('❌ Failed to move document:', e)
    message.error('文档移动失败')
  } finally {
    moveLoading.value = false
  }
}

// 显示复制模态框
const showCopyModal = async () => {
  await loadKnowledgeBases()
  copyForm.value = { knowledgeBaseId: '' }
  copyModalVisible.value = true
}

// 复制文档
const handleCopyDocument = async () => {
  if (!copyForm.value.knowledgeBaseId) {
    message.warning('请选择目标知识库')
    return
  }
  copyLoading.value = true
  try {
    await copyDocument(docId.value, copyForm.value.knowledgeBaseId)
    message.success('文档复制成功')
    copyModalVisible.value = false
  } catch (e: any) {
    console.error('❌ Failed to copy document:', e)
    message.error('文档复制失败')
  } finally {
    copyLoading.value = false
  }
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  try {
    const res = await getKnowledgeBases({}) as { data?: unknown }
    let list: unknown[] = []
    const data = res?.data
    if (Array.isArray(data)) {
      list = data
    } else if (data && typeof data === 'object' && 'knowledgeBases' in data) {
      const kbList = (data as { knowledgeBases?: unknown }).knowledgeBases
      if (Array.isArray(kbList)) list = kbList
    }
    type ApiKB = { id?: string | number; kbId?: string | number; key?: string | number; name?: string }
    knowledgeBases.value = list.map((item: unknown) => {
      const obj = item as Partial<ApiKB>
      return { id: String(obj.id ?? obj.kbId ?? obj.key ?? ''), name: obj.name ?? '未命名知识库' }
    })
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

// 加载评论
const loadComments = async () => {
  loadingComments.value = true
  try {
    console.log('🔍 Loading comments for document:', docId.value)
    const res: any = await getDocumentComments(docId.value)
    console.log('📋 Comments response:', res)
    
    // 处理评论响应
    let commentsData = []
    if (res?.data) {
      commentsData = Array.isArray(res.data) ? res.data : res.data.comments || []
    } else if (Array.isArray(res)) {
      commentsData = res
    }
    
    comments.value = commentsData.map((comment: any) => ({
      ...comment,
      editing: false,
      editContent: comment.content
    }))
    
    console.log('✅ Comments loaded:', comments.value)
  } catch (e: any) {
    console.error('❌ Failed to load comments:', e)
    
    // 根据错误类型显示不同的提示信息
    if (e?.response?.data?.message?.includes('Unknown column')) {
      console.warn('数据库字段问题，评论功能暂时不可用')
      comments.value = []
      commentsAvailable.value = false
      // 不显示错误消息，因为这是后端数据库问题
    } else {
      message.error('加载评论失败')
      comments.value = []
      commentsAvailable.value = false
    }
  } finally {
    loadingComments.value = false
  }
}

// 添加评论
const handleAddComment = async () => {
  if (!newComment.value.trim()) {
    message.warning('请输入评论内容')
    return
  }
  
  addingComment.value = true
  try {
    console.log('🔍 Adding comment:', newComment.value)
    await addDocumentComment(docId.value, { content: newComment.value })
    
    message.success('评论添加成功')
    newComment.value = ''
    
    // 重新加载评论
    await loadComments()
  } catch (e: any) {
    console.error('❌ Failed to add comment:', e)
    message.error('评论添加失败')
  } finally {
    addingComment.value = false
  }
}

// 编辑评论
const editComment = (comment: any) => {
  comment.editing = true
  comment.editContent = comment.content
}

// 取消编辑评论
const cancelEditComment = (comment: any) => {
  comment.editing = false
  comment.editContent = comment.content
}

// 保存编辑评论
const saveEditComment = async (comment: any) => {
  if (!comment.editContent.trim()) {
    message.warning('请输入评论内容')
    return
  }
  
  try {
    console.log('🔍 Updating comment:', comment.id)
    await updateComment(comment.id, { content: comment.editContent })
    
    message.success('评论更新成功')
    comment.content = comment.editContent
    comment.editing = false
    
    // 重新加载评论
    await loadComments()
  } catch (e: any) {
    console.error('❌ Failed to update comment:', e)
    message.error('评论更新失败')
  }
}

// 删除评论确认
const deleteCommentConfirm = (comment: any) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条评论吗？',
    onOk: async () => {
      try {
        console.log('🔍 Deleting comment:', comment.id)
        await deleteComment(comment.id)
        
        message.success('评论删除成功')
        
        // 重新加载评论
        await loadComments()
      } catch (e: any) {
        console.error('❌ Failed to delete comment:', e)
        message.error('评论删除失败')
      }
    }
  })
}

// 监听路由变化
watch(() => route.params.id, async () => {
  if (!docId.value) {
    message.error('文档ID无效')
    goBack()
    return
  }
  await loadDocumentDetail()
}, { immediate: true })

onMounted(async () => {
  if (!docId.value) {
    message.error('文档ID无效')
    goBack()
    return
  }
  await loadDocumentDetail()
  await loadDocumentPreview()
  await loadComments()
})
</script>

<style scoped>
.document-detail-page {
  min-height: 100vh;
  background: #f5f7fb;
}

/* 顶部导航栏 */
.top-nav {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
}

.nav-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-left {
  flex: 0 0 auto;
}

.nav-center {
  flex: 1;
  text-align: center;
}

.nav-right {
  flex: 0 0 auto;
}

.back-btn {
  font-size: 14px;
  color: #666;
}

.back-btn:hover {
  color: #1890ff;
}

.doc-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 文档信息卡片 */
.doc-info-card {
  margin: 24px;
}

.doc-info-content {
  display: flex;
  gap: 24px;
}

.doc-info-left {
  display: flex;
  gap: 16px;
  flex: 1;
}

.doc-icon {
  font-size: 48px;
  color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: #f0f8ff;
  border-radius: 8px;
}

.doc-basic-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.doc-description {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.doc-info-right {
  flex: 0 0 300px;
}

.doc-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.meta-item:last-child {
  border-bottom: none;
}

.meta-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.meta-value {
  font-size: 14px;
  color: #262626;
}

.size-warning {
  color: #fa8c16 !important;
  font-weight: 600;
}

.size-warning-text {
  margin-left: 4px;
  font-size: 12px;
}

/* 文档内容区域 */
.doc-content-area {
  margin: 0 24px 24px 24px;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-content {
  min-height: 400px;
}

.content-text {
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  overflow: hidden;
}

.content-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #e9ecef;
  border-bottom: 1px solid #dee2e6;
  font-size: 12px;
}

.content-type-badge {
  background: #007bff;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.content-length {
  color: #6c757d;
  font-weight: 500;
}

.content-pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #262626;
  padding: 20px;
  margin: 0;
  max-height: 600px;
  overflow-y: auto;
  background: white;
}

.special-file-info {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  margin: 16px 0;
}

.special-file-icon {
  font-size: 48px;
  color: #856404;
  flex-shrink: 0;
}

.special-file-content {
  flex: 1;
}

.special-file-content h4 {
  margin: 0 0 12px 0;
  color: #856404;
  font-size: 18px;
}

.special-file-content p {
  margin: 0 0 16px 0;
  color: #856404;
  line-height: 1.5;
}

.special-file-actions {
  margin-top: 16px;
}

.no-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #999;
}

.no-content-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.no-content p {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.no-content-tip {
  font-size: 14px;
  opacity: 0.7;
}

/* 评论区域样式 */
.comments-section {
  margin: 0 24px 24px 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-comment {
  margin-bottom: 24px;
}

.comment-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.comment-author {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content p {
  margin: 0;
  line-height: 1.6;
  color: #333;
}

.comment-edit {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edit-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.no-comments {
  padding: 40px 0;
}

.comment-disabled {
  margin-bottom: 24px;
}

.disabled-notice {
  padding: 16px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .doc-info-content {
    flex-direction: column;
  }
  
  .doc-info-right {
    flex: 1;
  }
  
  .top-nav {
    padding: 0 16px;
  }
  
  .doc-info-card,
  .doc-content-area {
    margin: 16px;
  }
}
</style>
