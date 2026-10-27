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
          <input class="search-input" type="text" placeholder="搜索..." />
        </div>
        <div class="actions">
          <span class="bell">🔔<i class="badge">3</i></span>
          <span class="profile-dot">⚫</span>
        </div>
      </header>

      <div class="home-page">
        <div class="page-header">
          <h1>仪表盘</h1>
          <ul class="breadcrumb">
            <li><a href="#">首页</a></li>
            <li>仪表盘</li>
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
            <h2 class="section-title">知识库概览</h2>
            <a-button type="primary" @click="viewAllKnowledgeBases">查看全部</a-button>
          </div>
          <div class="table-responsive">
            <a-table :dataSource="knowledgeBases" :columns="knowledgeColumns" :pagination="false" :loading="loading">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.status === '活跃' ? 'green' : 'red'">
                    {{ record.status }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <div class="action-buttons">
                    <a-button type="text" title="查看" @click="viewKnowledgeBase(record)">
                      <EyeOutlined />
                    </a-button>
                    <a-button type="text" title="编辑" @click="editKnowledgeBase(record)">
                      <EditOutlined />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  BookOutlined, 
  FileTextOutlined, 
  UnorderedListOutlined, 
  CheckCircleOutlined,
  EyeOutlined,
  EditOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getKnowledgeBases } from '@/api/knowledgeBase'
import { getDashboardStats } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'

interface StatCard {
  title: string;
  value: string;
  desc: string;
  icon: Component;
  iconBg: string;
  iconColor: string;
}

interface KnowledgeBase {
  key: string;
  name: string;
  documentCount: number;
  lastUpdate: string;
  status: string;
}

const router = useRouter()
const route = useRoute()
const isActive = (path: string) => computed(() => route.path === path).value
const go = (path: string) => {
  if (route.path !== path) router.push(path)
}

// 用户信息（头像/名称/身份）
const authStore = useAuthStore()
const avatarUrl = computed(() => (authStore as any).user?.avatarUrl || '/default-avatar.svg')
const displayName = computed(() => authStore.user?.realName || (authStore.user as any)?.username || '用户')
const roleText = computed(() => ((authStore.user as any)?.isAdmin ? '管理员' : '用户'))

// 统计数据
const dashboardStats = ref({
  knowledgeBaseCount: 0,
  documentCount: 0,
  operationLogCount: 0,
  pendingTasksCount: 0,
  knowledgeBaseGrowth: 0,
  documentTypeDistribution: { text: 0, nonText: 0 },
  recentActivityDays: 0,
  expiringTasksCount: 0
})

// 计算统计卡片数据
const stats = computed<StatCard[]>(() => [
  { 
    title: '知识库数量', 
    value: `${dashboardStats.value.knowledgeBaseCount}个`, 
    desc: `较上月${dashboardStats.value.knowledgeBaseGrowth > 0 ? '增加' : '减少'}${Math.abs(dashboardStats.value.knowledgeBaseGrowth)}个`, 
    icon: BookOutlined, 
    iconBg: '#e1e9ff', 
    iconColor: '#3882F8' 
  },
  { 
    title: '文档总数', 
    value: `${dashboardStats.value.documentCount}件`, 
    desc: `文本 ${Math.round((dashboardStats.value.documentTypeDistribution.text / dashboardStats.value.documentCount) * 100)}% | 非文本 ${Math.round((dashboardStats.value.documentTypeDistribution.nonText / dashboardStats.value.documentCount) * 100)}%`, 
    icon: FileTextOutlined, 
    iconBg: '#e1e9ff', 
    iconColor: '#3882F8' 
  },
  { 
    title: '操作日志', 
    value: `${dashboardStats.value.operationLogCount}条`, 
    desc: `最近${dashboardStats.value.recentActivityDays}天活动`, 
    icon: UnorderedListOutlined, 
    iconBg: '#e1e9ff', 
    iconColor: '#3882F8' 
  },
  { 
    title: '待处理事项', 
    value: `${dashboardStats.value.pendingTasksCount}项`, 
    desc: `${dashboardStats.value.expiringTasksCount}项即将到期`, 
    icon: CheckCircleOutlined, 
    iconBg: '#e1e9ff', 
    iconColor: '#3882F8' 
  }
])

const knowledgeBases = ref<KnowledgeBase[]>([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalItems = ref(0)
const loading = ref(false)

// 查看知识库详情
const viewKnowledgeBase = (record: KnowledgeBase) => {
  router.push(`/knowledge-base?id=${record.key}`)
}

// 编辑知识库
const editKnowledgeBase = (record: KnowledgeBase) => {
  router.push(`/knowledge-base/edit?id=${record.key}`)
}

// 查看全部知识库
const viewAllKnowledgeBases = () => {
  router.push('/knowledge-base')
}

const knowledgeColumns = [
  { title: '知识库名称', dataIndex: 'name', key: 'name' },
  { title: '文档数量', dataIndex: 'documentCount', key: 'documentCount' },
  { title: '最后更新', dataIndex: 'lastUpdate', key: 'lastUpdate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'actions' }
]

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 并行加载统计数据
    const [statsData, knowledgeData] = await Promise.all([
      getDashboardStats(),
      getKnowledgeBases({ page: currentPage.value, size: pageSize.value })
    ])
    
    // 更新统计数据
    dashboardStats.value = statsData
    
    // 更新知识库列表 - 处理真实API响应
    if (knowledgeData && typeof knowledgeData === 'object') {
      let list: unknown[] = []
      let total = 0
      
      // 处理后端返回的数据格式
      if (knowledgeData.data && knowledgeData.data.knowledgeBases) {
        // 后端返回格式：{ data: { knowledgeBases: [...] } }
        list = knowledgeData.data.knowledgeBases
        total = list.length
      } else if (Array.isArray(knowledgeData)) {
        list = knowledgeData
        total = knowledgeData.length
      } else if (knowledgeData.data && Array.isArray(knowledgeData.data)) {
        list = knowledgeData.data
        // 兼容返回中携带 total
        const t = (knowledgeData as unknown as { total?: number }).total
        total = t ?? (knowledgeData.data as unknown[]).length
      } else {
        // 处理类似 Spring 分页格式
        const pageResp = knowledgeData as unknown as { content?: unknown[]; totalElements?: number }
        if (pageResp.content && Array.isArray(pageResp.content)) {
          list = pageResp.content
          total = pageResp.totalElements ?? pageResp.content.length
        }
      }
      
      // 转换数据格式
      type ApiKb = {
        id?: string | number; key?: string;
        name?: string; title?: string;
        documentCount?: number; docCount?: number;
        updatedAt?: string; updateTime?: string; lastUpdate?: string; createTime?: string;
        status?: string | number;
      }
      knowledgeBases.value = (list as ApiKb[]).map((item) => ({
        key: item.id != null ? String(item.id) : (item.key ?? Math.random().toString()),
        name: item.name || item.title || '未命名知识库',
        documentCount: item.documentCount ?? item.docCount ?? 0,
        lastUpdate: item.updatedAt || item.updateTime || item.lastUpdate || item.createTime || '未知',
        status: item.status === 'active' || item.status === 1 ? '活跃' : '非活跃'
      }))
      
      totalItems.value = total
      
      console.log('知识库数据加载成功:', {
        list: knowledgeBases.value,
        total: totalItems.value,
        rawResponse: knowledgeData
      })
    }
    
  } catch (err: unknown) {
    console.error('加载数据失败:', err)
    
    // 清除缓存数据，强制重新加载
    localStorage.removeItem('dashboard_cache')
    console.log('已清除Dashboard缓存，强制重新加载数据')
    
    // 使用默认数据作为fallback
    if (knowledgeBases.value.length === 0) {
      dashboardStats.value = {
        knowledgeBaseCount: 0,
        documentCount: 0,
        operationLogCount: 0,
        pendingTasksCount: 0,
        knowledgeBaseGrowth: 0,
        documentTypeDistribution: { text: 0, nonText: 0 },
        recentActivityDays: 0,
        expiringTasksCount: 0
      }
      
      knowledgeBases.value = []
      totalItems.value = 0
    }
    
    // 显示错误信息
    const e = err as { response?: { status?: number }; message?: string }
    if (e?.response?.status === 401) {
      message.warning('请先登录')
    } else if (e?.response?.status === 500) {
      message.error('服务器内部错误，请稍后重试')
    } else if (e?.response?.status === 404) {
      message.error('接口不存在，请检查后端配置')
    } else {
      message.error(`获取数据失败: ${e?.message || '未知错误'}`)
    }
  } finally {
    loading.value = false
  }
}

// 分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.app-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  min-height: 100vh;
  background: #f5f7fb;
}

.sidebar {
  background: #1e6bed;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 14px 12px;
}

.brand {
  padding: 6px 10px 16px 10px;
  border-bottom: 1px solid rgba(255,255,255,0.15);
}
.brand-title {
  font-weight: 700;
  font-size: 18px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 8px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #3882F8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.user-meta .user-name { font-weight: 600; }
.user-meta .user-role { opacity: 0.9; font-size: 12px; }

.menu { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.menu-item {
  width: 100%;
  text-align: left;
  background: transparent;
  border: none;
  color: #e8f0ff;
  padding: 10px 10px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.menu-item:hover { background: rgba(255,255,255,0.12); }
.menu-item.active { background: #0d59d6; }
.menu-icon { width: 18px; display: inline-block; text-align: center; }

.content { display: flex; flex-direction: column; }
.topbar {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #eef0f5;
}
.search-box { position: relative; width: 380px; }
.search-input {
  width: 100%;
  height: 34px;
  border-radius: 18px;
  border: 1px solid #e3e6f0;
  padding: 0 14px 0 34px;
  outline: none;
}
.search-icon { position: absolute; left: 10px; top: 7px; opacity: 0.6; }
.actions { display: flex; align-items: center; gap: 18px; }
.bell { position: relative; }
.badge { position: absolute; top: -8px; right: -8px; background: #ff4d4f; color: #fff; font-size: 10px; border-radius: 10px; padding: 0 4px; }
.profile-dot { font-size: 10px; opacity: 0.6; }

/* 原首页样式，保持不变 */
.home-page { padding: 24px; }
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
.action-buttons { display: flex; }
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }

@media (max-width: 992px) {
  .search-box { width: 220px; }
  .app-layout { grid-template-columns: 220px 1fr; }
}
</style>
