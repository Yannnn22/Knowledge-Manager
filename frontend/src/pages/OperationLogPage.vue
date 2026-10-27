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
          <input class="search-input" type="text" placeholder="搜索操作日志..." @keyup.enter="reload" />
        </div>
        <div class="actions">
          <span class="bell">🔔<i class="badge">3</i></span>
          <span class="profile-dot">⚫</span>
        </div>
      </header>

      <div class="log-page">
        <div class="page-header">
          <h1>操作日志管理</h1>
          <ul class="breadcrumb">
            <li><a href="#" @click.prevent="go('/home')">首页</a></li>
            <li>操作日志管理</li>
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
            <h2 class="section-title">操作日志</h2>
            <div>
              <a-button style="margin-right: 10px;" @click="openSensitive">敏感操作</a-button>
              <a-button style="margin-right: 10px;" @click="onExport" :loading="exporting">导出日志</a-button>
              <a-button type="default" @click="resetFilters">重置</a-button>
            </div>
          </div>

          <!-- 过滤器行 -->
          <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom: 12px;">
            <!-- 知识库筛选 -->
            <a-select v-model:value="kbFilter" placeholder="选择知识库" style="width: 200px" allowClear @change="reload">
              <a-select-option value="">全部知识库</a-select-option>
              <a-select-option v-for="kb in kbOptions" :key="kb.id" :value="String(kb.id)">{{ kb.name }}</a-select-option>
            </a-select>
            
            <!-- 操作用户名筛选 -->
            <a-input v-model:value="userFilter" placeholder="操作用户名" style="width: 150px" @keyup.enter="reload" allowClear />
            
            <!-- 操作名称筛选 -->
            <a-input v-model:value="operationFilter" placeholder="操作名称" style="width: 180px" @keyup.enter="reload" allowClear />
            
            <!-- 目标类型筛选 -->
            <a-select v-model:value="targetTypeFilter" placeholder="目标类型" style="width: 160px" allowClear @change="reload">
              <a-select-option value="">全部目标</a-select-option>
              <a-select-option value="USER">用户</a-select-option>
              <a-select-option value="DOCUMENT">文档</a-select-option>
              <a-select-option value="KNOWLEDGE_BASE">知识库</a-select-option>
            </a-select>
            
            <!-- 日期筛选（单日选择） -->
            <a-date-picker 
              v-model:value="selectedDate" 
              style="width: 180px" 
              @change="reload"
              format="YYYY-MM-DD"
              placeholder="选择日期"
              allowClear
            />
            
            <!-- 查询按钮 -->
            <a-button type="primary" @click="reload">查询</a-button>
          </div>

          <div class="table-responsive">
            <a-table :dataSource="operationLogs" :columns="logColumns" :loading="loading" :pagination="false" rowKey="key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'operation'">
                  <a-tag color="blue">{{ record.operation }}</a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="text" title="详情" @click="viewLog(record)">ℹ️</a-button>
                </template>
              </template>
            </a-table>
          </div>

          <!-- 日志详情 Modal -->
          <a-modal v-model:open="detailVisible" title="日志详情" :footer="null" width="520px">
            <div v-if="detailRecord">
              <p><strong>时间：</strong>{{ detailRecord.time }}</p>
              <p><strong>用户：</strong>{{ detailRecord.user }}</p>
              <p><strong>操作：</strong>{{ detailRecord.operation }}</p>
              <p><strong>目标类型：</strong>{{ detailRecord.targetType }}</p>
              <p><strong>目标ID：</strong>{{ detailRecord.targetId }}</p>
              <p><strong>详情：</strong>{{ detailRecord.detail || '—' }}</p>
            </div>
          </a-modal>

          <!-- 敏感操作 Drawer -->
          <a-drawer v-model:open="sensitiveVisible" title="敏感操作日志" placement="right" width="640">
            <a-table :dataSource="sensitiveLogs" :columns="logColumns" :loading="sensitiveLoading" :pagination="false" rowKey="key" />
          </a-drawer>

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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  UnorderedListOutlined, 
  ExclamationCircleOutlined, 
  UserOutlined, 
  BugOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getOperationLogs, exportOperationLogs, getSensitiveLogs, getOperationLogStatistics } from '@/api/operationLog'
import { useAuthStore } from '@/stores/auth'
import { getKnowledgeBases } from '@/api/knowledgeBase'

interface StatCard {
  title: string;
  value: string;
  desc: string;
  icon: any;
  iconBg: string;
  iconColor: string;
}

interface OperationLogRow {
  key: string;
  time: string;
  user: string;
  operation: string;
  targetType: string;
  targetId: string;
  detail?: string;
}

const router = useRouter()
const route = useRoute()
const isActive = (path: string) => computed(() => route.path === path).value
const go = (path: string) => { if (route.path !== path) router.push(path) }

const authStore = useAuthStore()
const avatarUrl = computed(() => (authStore as any).user?.avatarUrl || '/default-avatar.svg')
const displayName = computed(() => authStore.user?.realName || (authStore.user as any)?.username || '用户')
const roleText = computed(() => ((authStore.user as any)?.isAdmin ? '管理员' : '用户'))

const stats = ref<StatCard[]>([
  { title: '今日日志', value: '—', desc: '', icon: UnorderedListOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '敏感操作', value: '—', desc: '', icon: ExclamationCircleOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '用户活跃度', value: '—', desc: '', icon: UserOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
  { title: '错误记录', value: '—', desc: '', icon: BugOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' }
])

const operationLogs = ref<OperationLogRow[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalItems = ref(0)
const loading = ref(false)
const exporting = ref(false)

// filters - 优化后的筛选字段
const kbFilter = ref<string | undefined>(undefined)
const userFilter = ref('')
const selectedDate = ref<string | null>(null)
const operationFilter = ref<string>('')
const targetTypeFilter = ref<string | undefined>(undefined)

const kbOptions = ref<Array<{ id: string | number; name: string }>>([])

const logColumns = [
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '用户', dataIndex: 'user', key: 'user' },
  { title: '操作', dataIndex: 'operation', key: 'operation' },
  { title: '目标类型', dataIndex: 'targetType', key: 'targetType' },
  { title: '目标ID', dataIndex: 'targetId', key: 'targetId' },
  { title: '详情', key: 'actions' }
]

const resetFilters = () => {
  kbFilter.value = undefined
  userFilter.value = ''
  selectedDate.value = null
  operationFilter.value = ''
  targetTypeFilter.value = undefined
  currentPage.value = 1
  reload()
}

const serializeParams = () => {
  const params: any = {
    page: currentPage.value,
    size: pageSize.value
  }
  
  // 传递优化后的筛选字段
  if (userFilter.value) params.username = userFilter.value
  if (operationFilter.value) params.operation = operationFilter.value
  if (kbFilter.value && kbFilter.value !== '') params.knowledgeBaseId = kbFilter.value
  if (targetTypeFilter.value) params.targetType = targetTypeFilter.value
  if (selectedDate.value) {
    // 单日筛选：将选择的日期转换为当天的开始和结束时间
    const date = selectedDate.value
    params.startTime = `${date} 00:00:00`
    params.endTime = `${date} 23:59:59`
  }
  
  console.log('🔍 发送筛选参数:', params)
  return params
}

const reload = async () => {
  await loadOperationLogs()
}

const viewLog = (record: OperationLogRow) => { 
  detailRecord.value = record
  detailVisible.value = true
}

const onExport = async () => {
  try {
    exporting.value = true
    const p = serializeParams()
    const blob = await exportOperationLogs({ startTime: p.startTime, endTime: p.endTime })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `operation-logs-${dayjs().format('YYYYMMDD-HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    message.error('导出失败')
    console.error('Export error:', error)
  } finally {
    exporting.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadOperationLogs()
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    console.log('📊 加载操作日志统计数据...');
    const response = await getOperationLogStatistics();
    const data = response.data;
    
    console.log('📊 后端返回的原始数据:', data);
    
    // 更新统计卡片数据 - 使用后端实际返回的字段名，并处理字符串类型
    stats.value = [
      { 
        title: '今日日志', 
        value: `${Number(data.today_logs) || 0}条`, 
        desc: '今日产生的操作日志数量', 
        icon: UnorderedListOutlined, 
        iconBg: '#e1e9ff', 
        iconColor: '#3882F8' 
      },
      { 
        title: '敏感操作', 
        value: `${Number(data.sensitive_operations) || 0}条`, 
        desc: '需要特别关注的敏感操作', 
        icon: ExclamationCircleOutlined, 
        iconBg: '#fff1f0', 
        iconColor: '#f5222d' 
      },
      { 
        title: '用户活跃度', 
        value: `${Number(data.active_users) || 0}人`, 
        desc: '今日活跃用户数量', 
        icon: UserOutlined, 
        iconBg: '#f6ffed', 
        iconColor: '#52c41a' 
      },
      { 
        title: '错误记录', 
        value: `${Number(data.error_records) || 0}条`, 
        desc: '今日产生的错误日志', 
        icon: BugOutlined, 
        iconBg: '#fff3cd', 
        iconColor: '#faad14' 
      }
    ];
    
    console.log('✅ 操作日志统计数据加载完成:', data);
  } catch (error) {
    console.error('❌ 加载操作日志统计数据失败:', error);
    // 保持默认值，不显示错误给用户
  }
};

const loadKbOptions = async () => {
  try {
    const res: any = await getKnowledgeBases({ page: 1, size: 1000 })
    const data = res?.data?.knowledgeBases || res?.data || []
    kbOptions.value = (Array.isArray(data) ? data : []).map((x: any) => ({ id: x.id ?? x.kbId ?? x.key, name: x.name ?? '未命名知识库' }))
  } catch {
    kbOptions.value = []
  }
}

const loadOperationLogs = async () => {
  loading.value = true
  try {
    const params = serializeParams()
    console.log('🔍 Loading operation logs with params:', params)
    
    const res: any = await getOperationLogs(params)
    console.log('📋 Operation logs API response:', res)
    
    // 根据新的后端API响应结构解析数据
    let list = []
    let total = 0
    
    if (res?.data?.records && Array.isArray(res.data.records)) {
      // 新格式: {data: {records: [...], total: 88}}
      list = res.data.records
      total = parseInt(res.data.total) || 0
    } else if (res?.data?.list && Array.isArray(res.data.list)) {
      // 另一种格式: {data: {list: [...], total: 88}}
      list = res.data.list
      total = parseInt(res.data.total) || 0
    } else if (Array.isArray(res?.data)) {
      // 直接返回数组: {data: [...]}
      list = res.data
      total = list.length
    } else {
      // 兜底处理
      list = []
      total = 0
    }

    console.log('📋 Extracted list:', list, 'Total:', total, 'Is Array:', Array.isArray(list))

    // 确保list是数组
    const safeList = Array.isArray(list) ? list : []
    
    operationLogs.value = safeList.map((x: any, idx: number) => ({
      key: String(x.id ?? idx),
      time: x.operation_time || '',
      user: x.user?.username || '—',
      operation: x.operation || '—',
      targetType: x.target_type || '—',
      targetId: String(x.target_id ?? ''),
      detail: x.detail || ''
    }))
    totalItems.value = total
    
    if (safeList.length === 0) {
      message.info('暂无操作日志数据')
    }
  } catch (error) {
    operationLogs.value = []
    totalItems.value = 0
    console.error('Load operation logs error:', error)
    message.error('加载操作日志失败，请检查网络连接或联系管理员')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStatistics() // 加载统计数据
  await loadKbOptions()
  await loadOperationLogs()
})

// 详情弹窗
const detailVisible = ref(false)
const detailRecord = ref<OperationLogRow | null>(null)

// 敏感操作
const sensitiveVisible = ref(false)
const sensitiveLoading = ref(false)
const sensitiveLogs = ref<OperationLogRow[]>([])

const openSensitive = async () => {
  sensitiveVisible.value = true
  sensitiveLoading.value = true
  try {
    const res: any = await getSensitiveLogs()
    const list = res?.data?.logs || res?.logs || res?.data || []
    const safeList = Array.isArray(list) ? list : []
    sensitiveLogs.value = safeList.map((x: any, idx: number) => ({
      key: String(x.id ?? idx),
      time: x.operationTime || '',
      user: x.user?.username || '—', // Use username for consistency with backend filtering
      operation: x.operation || '—',
      targetType: x.targetType || '—',
      targetId: String(x.targetId ?? ''),
      detail: x.detail || ''
    }))
  } catch (e) {
    message.error('加载敏感操作失败')
  } finally {
    sensitiveLoading.value = false
  }
}
</script>

<style scoped>
/* 统一两栏布局样式 */
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
.log-page { padding: 24px; }
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
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }

@media (max-width: 992px) {
  .search-box { width: 220px; }
  .app-layout { grid-template-columns: 220px 1fr; }
}
</style>
