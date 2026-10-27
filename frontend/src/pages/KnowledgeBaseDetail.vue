<template>
  <div class="kb-detail-page">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="nav-content">
        <div class="nav-left">
          <a-button type="text" @click="go('/knowledge-base')" class="back-btn">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            返回知识库管理
          </a-button>
        </div>
        <div class="nav-center">
          <h1 class="kb-title">
            <a-spin v-if="loadingKB" size="small" style="margin-right: 8px;" />
            {{ kb?.name || '—' }}
          </h1>
        </div>
      </div>
    </header>

    <!-- 知识库描述 -->
    <div class="kb-description" v-if="kb?.description">
      <a-card>
        <h3>知识库简介</h3>
        <p>{{ kb.description }}</p>
      </a-card>
    </div>

    <!-- 文档列表 -->
    <div class="documents-section">
      <a-card>
        <template #title>
          <div class="section-header">
            <span>文档列表</span>
            <a-spin v-if="loadingDocs" size="small" />
          </div>
        </template>
        
        <div class="documents-grid" v-if="displayedDocuments.length > 0">
          <div 
            v-for="doc in displayedDocuments" 
            :key="doc.id"
            class="document-card"
            @click="openDoc(doc)"
          >
            <div class="doc-icon">
              <component :is="getFileIcon(doc.fileType)" />
            </div>
            <div class="doc-info">
              <h4 class="doc-title">{{ doc.title || doc.originalFilename || '未命名文档' }}</h4>
              <p class="doc-description">{{ doc.description || '暂无描述' }}</p>
              <div class="doc-meta">
                <span class="doc-size" :class="{ 'size-warning': doc.fileSize === 0 }">
                  {{ formatSize(doc.fileSize) }}
                  <span v-if="doc.fileSize === 0" class="size-warning-text">⚠️</span>
                </span>
                <span class="doc-time">{{ formatTime(doc.uploadTime || doc.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-else-if="!loadingDocs" class="no-documents">
          <a-empty description="暂无文档" />
        </div>
        
        <!-- 分页 -->
        <div class="pagination" v-if="docTotal > 0">
          <a-pagination
            v-model:current="docPage"
            :total="docTotal"
            :pageSize="docSize"
            show-less-items
            @change="loadDocuments"
          />
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  ArrowLeftOutlined,
  FileTextOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FileImageOutlined,
  FileOutlined
} from '@ant-design/icons-vue'
import { getKnowledgeBaseDetail, getKnowledgeBaseDocuments } from '@/api/knowledgeBase'

const router = useRouter()
const route = useRoute()

// ID映射函数 - 临时解决方案
const mapKnowledgeBaseId = (id: string): string => {
  // 如果ID是长字符串，尝试映射到数字ID
  if (id && id.length > 10) {
    console.warn('⚠️ 检测到长知识库ID，尝试映射:', id)
    
    // 临时映射逻辑 - 根据实际情况调整
    // 这里我们尝试将长ID映射到数据库中的实际ID
    const idMappings: Record<string, string> = {
      '19337107299766272': '1', // 根据实际情况调整
      // 可以添加更多映射
    }
    
    const mappedId = idMappings[id]
    if (mappedId) {
      console.log('✅ ID映射成功:', id, '->', mappedId)
      return mappedId
    } else {
      console.warn('❌ 未找到ID映射，使用原ID:', id)
      return id
    }
  }
  return id
}

const kbId = computed(() => {
  const routeId = String(route.params.id || '')
  return mapKnowledgeBaseId(routeId)
})
const kb = ref<any>(null)
const loadingKB = ref(false)

const allDocuments = ref<any[]>([])
const loadingDocs = ref(false)
const docPage = ref(1)
const docSize = ref(10)
const docTotal = ref(0)

const displayedDocuments = computed(() => {
  return allDocuments.value.filter(doc => doc.is_deleted !== 1 && doc.isDeleted !== 1);
});

const go = (path: string) => {
  router.push(path)
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

// 加载知识库详情
const loadKB = async () => {
  loadingKB.value = true
  try {
    const res: any = await getKnowledgeBaseDetail(kbId.value)
    let kbData = null
    if (res?.data) {
      kbData = res.data
    } else if (res && typeof res === 'object') {
      kbData = res
    }

    if (!kbData) {
      throw new Error('知识库数据为空')
    }

    kb.value = {
      ...kbData,
      name: kbData.name || '未命名知识库',
      description: kbData.description || '',
      department: kbData.department || '—'
    }
  } catch (e: any) {
    message.error(`获取知识库详情失败: ${e?.message || '未知错误'}`)
    kb.value = {
      name: '加载失败',
      description: '无法获取知识库信息',
      department: '—'
    }
  } finally {
    loadingKB.value = false
  }
}

// 加载文档列表
const loadDocuments = async () => {
  loadingDocs.value = true
  try {
    const res: any = await getKnowledgeBaseDocuments(kbId.value, docPage.value, docSize.value);
    console.log('📋 知识库文档响应数据:', res);

    if (res?.data?.documents) {
      allDocuments.value = res.data.documents; // Store all documents, including deleted ones
      docTotal.value = res.data.total || allDocuments.value.length;
    } else {
      allDocuments.value = [];
      docTotal.value = 0;
    }
  } catch (e: any) {
    console.error('❌ Failed to load documents:', e)
    message.error('加载文档列表失败')
    allDocuments.value = [];
    docTotal.value = 0;
  } finally {
    loadingDocs.value = false
  }
}

// 打开文档
const openDoc = (doc: any) => {
  router.push({
    name: 'DocumentDetail',
    params: { id: doc.id },
    query: { 
      from: 'knowledge-base',
      kbId: kbId.value
    }
  })
}

// 监听路由变化
watch(() => route.params.id, async () => {
  if (!kbId.value) {
    message.error('知识库ID无效')
    go('/knowledge-base')
    return
  }
  await loadKB()
  await loadDocuments()
}, { immediate: true })


</script>

<style scoped>
.kb-detail-page {
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

.back-btn {
  font-size: 14px;
  color: #666;
}

.back-btn:hover {
  color: #1890ff;
}

.kb-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 知识库描述 */
.kb-description {
  margin: 24px;
}

.kb-description h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.kb-description p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

/* 文档列表 */
.documents-section {
  margin: 0 24px 24px 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.document-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  gap: 12px;
}

.document-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  transform: translateY(-2px);
}

.doc-icon {
  font-size: 32px;
  color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #f0f8ff;
  border-radius: 6px;
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-title {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-description {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #999;
}

.doc-size,
.doc-time {
  white-space: nowrap;
}

.size-warning {
  color: #fa8c16 !important;
  font-weight: 600;
}

.size-warning-text {
  margin-left: 4px;
  font-size: 10px;
}

.no-documents {
  padding: 40px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .documents-grid {
    grid-template-columns: 1fr;
  }
  
  .top-nav {
    padding: 0 16px;
  }
  
  .kb-description,
  .documents-section {
    margin: 16px;
  }
}
</style>
