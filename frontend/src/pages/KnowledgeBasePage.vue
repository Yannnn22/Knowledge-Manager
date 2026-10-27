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
            placeholder="搜索知识库..." 
            v-model="searchKeyword"
            @input="handleSearch"
          />
        </div>
        <div class="actions">
          <span class="bell">🔔<i class="badge">3</i></span>
          <span class="profile-dot">⚫</span>
        </div>
      </header>

    <div class="knowledge-page">
      <div class="page-header">
        <h1>知识库管理</h1>
        <ul class="breadcrumb">
            <li><a href="#" @click="go('/home')">首页</a></li>
          <li>知识库管理</li>
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
          <h2 class="section-title">知识库列表</h2>
            <div class="header-actions">
              <a-select 
                v-model:value="departmentFilter" 
                placeholder="选择部门" 
                style="width: 150px; margin-right: 10px;"
                @change="handleFilter"
                allowClear
              >
                <a-select-option value="">全部部门</a-select-option>
                <a-select-option value="技术部">技术部</a-select-option>
                <a-select-option value="设计部">设计部</a-select-option>
                <a-select-option value="市场部">市场部</a-select-option>
                <a-select-option value="财务部">财务部</a-select-option>
              </a-select>
          <a-button type="primary" @click="showCreateModal">新建知识库</a-button>
        </div>
          </div>
          
        <!-- 添加说明提示 -->
        <div class="info-note" style="margin-bottom: 16px; padding: 8px 12px; background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 4px; color: #0369a1; font-size: 12px;">
          💡 文档数量为实时统计，确保数据准确性
        </div>
        <div class="table-responsive">
            <a-table 
              :dataSource="knowledgeBases" 
              :columns="knowledgeColumns" 
              :pagination="false"
              :loading="loading"
              rowKey="key"
            >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === '活跃' ? 'green' : 'red'">
                  {{ record.status }}
                </a-tag>
              </template>
                <template v-else-if="column.key === 'department'">
                  <a-tag color="blue">{{ record.department }}</a-tag>
              </template>
              <template v-else-if="column.key === 'actions'">
                <div class="action-buttons">
                  <a-button type="text" title="查看" @click="viewKnowledgeBase(record)">
                    <EyeOutlined />
                  </a-button>
                  <a-button 
                    v-if="isAdmin || record.canEdit !== false"
                    type="text" title="编辑" @click="editKnowledgeBase(record)">
                    <EditOutlined />
                  </a-button>
                    <a-button 
                      v-if="isAdmin || record.canDelete !== false" 
                      type="text" title="删除" @click="handleDeleteKnowledgeBase(record)" danger>
                      <DeleteOutlined />
                  </a-button>
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
  
    <!-- 创建知识库模态框 -->
      <a-modal
      v-model:open="createModalVisible"
        title="新建知识库"
      @ok="handleCreate"
      @cancel="createModalVisible = false"
      :confirmLoading="createLoading"
    >
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="知识库名称" required>
          <a-input v-model:value="createForm.name" placeholder="请输入知识库名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="createForm.description" placeholder="请输入知识库描述" :rows="3" />
        </a-form-item>
        <a-form-item label="所属部门" required>
          <a-select v-model:value="createForm.department" placeholder="请选择部门">
            <a-select-option value="技术部">技术部</a-select-option>
            <a-select-option value="设计部">设计部</a-select-option>
            <a-select-option value="市场部">市场部</a-select-option>
            <a-select-option value="财务部">财务部</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑知识库模态框 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑知识库"
      @ok="handleEdit"
      @cancel="editModalVisible = false"
      :confirmLoading="editLoading"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="知识库名称" required>
          <a-input v-model:value="editForm.name" placeholder="请输入知识库名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" placeholder="请输入知识库描述" :rows="3" />
        </a-form-item>
        <a-form-item label="所属部门" required>
          <a-select v-model:value="editForm.department" placeholder="请选择部门">
            <a-select-option value="技术部">技术部</a-select-option>
            <a-select-option value="设计部">设计部</a-select-option>
            <a-select-option value="市场部">市场部</a-select-option>
            <a-select-option value="财务部">财务部</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      </a-modal>
    </div>
  </template>
  
  <script setup lang="ts">
import { ref, onMounted, computed, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
  import { 
    BookOutlined, 
    FileTextOutlined, 
    PlusCircleOutlined, 
    DatabaseOutlined,
    EyeOutlined,
    EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { getKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledgeBase'
import http from '@/utils/http' // 使用项目的http实例而不是axios
  
  interface StatCard {
    title: string;
    value: string;
    desc: string;
  icon: Component;
    iconBg: string;
    iconColor: string;
  }
  
interface KnowledgeBaseRow {
    id: string;
    key: string;
    name: string;
  description?: string;
    documentCount: number;
    createdAt: string;
    updatedAt: string;
    access: string;
    status: string;
  department: string;
  ownerId?: string;
  canEdit?: boolean;
  canDelete?: boolean;
  }

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const isAdmin = computed(() => {
  const u: any = (authStore as any)?.user
  return !!(u && (u.isAdmin === true))
})
const isActive = (path: string) => computed(() => route.path === path).value
const go = (path: string) => { if (route.path !== path) router.push(path) }

const avatarUrl = computed(() => (authStore as any).user?.avatarUrl || '/default-avatar.svg')
const displayName = computed(() => authStore.user?.realName || (authStore.user as any)?.username || '用户')
const roleText = computed(() => ((authStore.user as any)?.isAdmin ? '管理员' : '用户'))
  
  const stats = ref<StatCard[]>([
  { title: '知识库总数', value: '—', desc: '—', icon: BookOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '总文档数', value: '—', desc: '—', icon: FileTextOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '本周新增', value: '—', desc: '—', icon: PlusCircleOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '存储使用', value: '—', desc: '—', icon: DatabaseOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' }
])

const knowledgeBases = ref<KnowledgeBaseRow[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalItems = ref(0)
const loading = ref(false)

const searchKeyword = ref('')
const departmentFilter = ref('')

const createModalVisible = ref(false)
const createLoading = ref(false)
const createForm = ref({
  name: '',
  description: '',
  department: '技术部'
})

const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = ref({
  id: '',
  name: '',
  description: '',
  department: '技术部'
})
  
  const knowledgeColumns = [
  { title: '知识库名称', dataIndex: 'name', key: 'name' },
  { title: '文档数量', dataIndex: 'documentCount', key: 'documentCount' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '最后更新', dataIndex: 'updatedAt', key: 'updatedAt' },
  { title: '访问权限', dataIndex: 'access', key: 'access' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '部门', dataIndex: 'department', key: 'department' },
  { title: '操作', key: 'actions' }
]


  
  const showCreateModal = () => {
  createForm.value = {
    name: '',
    description: '',
    department: '技术部'
  }
  createModalVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.name) {
    message.error('知识库名称不能为空')
    return
  }
  createLoading.value = true
  try {
         await createKnowledgeBase({
       name: createForm.value.name,
       description: createForm.value.description,
       department: createForm.value.department
     })
    message.success('创建成功')
    createModalVisible.value = false
    loadKnowledgeBases()
  } catch {
    message.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

const viewKnowledgeBase = (record: KnowledgeBaseRow) => {
  console.log('🔍 知识库查看调试信息:', {
    recordId: record.id,
    recordKey: record.key,
    recordName: record.name,
    recordType: typeof record.key
  })
  router.push({ name: 'KnowledgeBaseDetail', params: { id: record.key } })
}

const editKnowledgeBase = (record: KnowledgeBaseRow) => {
      editForm.value = {
      id: record.id,
      name: record.name,
      description: record.description || '',
      department: record.department
    }
  editModalVisible.value = true
}

const handleEdit = async () => {
  if (!editForm.value.name) {
    message.error('知识库名称不能为空')
    return
  }
  editLoading.value = true
  try {
    await updateKnowledgeBase(editForm.value.id, {
      name: editForm.value.name,
      description: editForm.value.description,
      department: editForm.value.department
    })
    message.success('编辑成功')
    editModalVisible.value = false
    loadKnowledgeBases()
  } catch (e) {
    message.error('编辑失败: 无权限或数据冲突')
  } finally {
    editLoading.value = false
  }
}

const handleDeleteKnowledgeBase = async (record: KnowledgeBaseRow) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除知识库 "${record.name}" 吗？此操作不可逆。`,
    async onOk() {
      try {
        await deleteKnowledgeBase(record.id || record.key)
        message.success('删除成功')
        loadKnowledgeBases()
      } catch (e: any) {
        // 尝试刷新后再提示后端消息
        await loadKnowledgeBases()
        const status = e?.response?.status
        if (status === 403) {
          message.error('无权限删除该知识库')
        } else {
          const msg = e?.response?.data?.message || '删除失败'
          message.error(msg)
        }
      }
    }
  })
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadKnowledgeBases()
}

const handleFilter = () => {
  currentPage.value = 1
  loadKnowledgeBases()
}

const handleSearch = () => {
  currentPage.value = 1
  loadKnowledgeBases()
}


const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const params: { page?: number; size?: number; name?: string; department?: string; includeAll?: boolean } = {
      page: currentPage.value,
      size: pageSize.value,
      includeAll: true,
      ...(searchKeyword.value ? { name: searchKeyword.value } : {}),
      // 后端 includeAll=true 时，前端改为本地按部门筛选
    }
    const res: unknown = await getKnowledgeBases(params)

    // 统一解析
    let list: any[] = []
    let total = 0
    if (res && typeof res === 'object') {
      const r = res as { data?: any; total?: number }
      const data = r.data
      if (data && typeof data === 'object' && Array.isArray(data.knowledgeBases)) {
        list = data.knowledgeBases
        total = data.total ?? data.knowledgeBases.length
      } else if (data && typeof data === 'object' && (Array.isArray(data.records) || Array.isArray(data.list))) {
        list = (Array.isArray(data.records) ? data.records : data.list) as any[]
        total = data.total ?? list.length
      } else if (data && typeof data === 'object' && Array.isArray(data.content)) {
        list = data.content
        total = data.totalElements ?? data.content.length
      } else if (Array.isArray(data)) {
        list = data
        total = (res as any).total ?? data.length
      }
    }

    // 本地部门过滤（includeAll=true）
    if (departmentFilter.value) {
      list = list.filter((x: any) => String(x.department || '').includes(departmentFilter.value))
    }

    type ApiKB = { id?: string | number; key?: string | number; name?: string; description?: string; documentCount?: number; createdAt?: string; createTime?: string; updatedAt?: string; updateTime?: string; accessLevel?: string; status?: string | number; department?: string; ownerId?: string | number; canEdit?: boolean; canDelete?: boolean }
    
    const getRealDocumentCount = async (kbId: string | number): Promise<number> => {
      try {
        const searchParams = {
          query: '',
          knowledgeBaseIds: [String(kbId)],
          page: 1,
          size: 100,
          sortBy: 'uploadTime',
          order: 'desc'
        };
        const searchResponse = await http.post('/v1/documents/search', searchParams, { headers: { 'X-Suppress-Error': '1' } as any });
        let documentCount = 0;
        if (searchResponse && typeof searchResponse === 'object') {
          if ('total' in searchResponse.data) {
            documentCount = searchResponse.data.total || 0;
          } else if ('results' in searchResponse.data && Array.isArray(searchResponse.data.results)) {
            documentCount = searchResponse.data.results.length;
          } else if ('documents' in searchResponse.data && Array.isArray(searchResponse.data.documents)) {
            documentCount = searchResponse.data.documents.length;
          }
        }
        return documentCount;
      } catch {
        return 0;
      }
    };
    
    const toRow = async (item: unknown): Promise<KnowledgeBaseRow> => {
      const obj = item as Partial<ApiKB & { ownerId?: string | number; canEdit?: boolean; canDelete?: boolean }>
      const kbId = obj.id?.toString() || (obj.key != null ? String(obj.key) : '')
      let department = obj.department
      let createdAt = obj.createdAt || obj.createTime
      let updatedAt = obj.updatedAt || obj.updateTime
      const idStr = kbId || Math.random().toString()
      let realDocumentCount = obj.documentCount || 0
      if (kbId) realDocumentCount = await getRealDocumentCount(kbId)
      return {
        id: idStr,
        key: idStr,
        name: obj.name || '未命名知识库',
        description: obj.description || '',
        documentCount: realDocumentCount,
        createdAt: createdAt || '未知',
        updatedAt: updatedAt || '未知',
        access: obj.accessLevel || 'team',
        status: obj.status === 'active' || obj.status === 1 ? '活跃' : '非活跃',
        department: department || '技术部',
        ownerId: obj.ownerId != null ? String(obj.ownerId) : undefined,
        canEdit: obj.canEdit,
        canDelete: obj.canDelete
      }
    }

    const rows = await Promise.all(list.map(toRow))

    totalItems.value = (departmentFilter.value ? rows.length : (total || rows.length))
    const start = (currentPage.value - 1) * pageSize.value
    const end = currentPage.value * pageSize.value
    knowledgeBases.value = rows.slice(start, end)
    stats.value[0].value = `${totalItems.value}个`
    stats.value[1].value = `${knowledgeBases.value.reduce((a,b)=>a+(b.documentCount||0),0)}件`
    stats.value[2].value = '—'
    stats.value[3].value = '—'
  } catch (error) {
    console.error('获取知识库列表失败:', error)
    message.error('获取知识库列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadKnowledgeBases() })
</script>

<style scoped>
/* 通用两栏布局，保持与首页一致 */
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
.bell { position: relative; }
.badge { position: absolute; top: -8px; right: -8px; background: #ff4d4f; color: #fff; font-size: 10px; border-radius: 10px; padding: 0 4px; }
.profile-dot { font-size: 10px; opacity: 0.6; }

/* 页面样式 */
.knowledge-page { padding: 24px; }
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
.section-title { font-size: 1.2rem; color: #5a5c69; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.action-buttons { display: flex; }
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }

@media (max-width: 992px) {
  .search-box { width: 220px; }
  .app-layout { grid-template-columns: 220px 1fr; }
  }
  </style>
