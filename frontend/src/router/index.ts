import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'

// 路由配置
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/login',
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/pages/Dashboard.vue'),
    meta: { 
      requiresAuth: true,
      title: '首页 - 企业知识管理系统'
    }
  },
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    component: () => import('@/pages/KnowledgeBasePage.vue'),
    meta: { 
      requiresAuth: true,
      title: '知识库管理 - 企业知识管理系统'
    }
  },
  {
    path: '/knowledge-base/:id',
    name: 'KnowledgeBaseDetail',
    component: () => import('@/pages/KnowledgeBaseDetail.vue'),
    meta: {
      requiresAuth: true,
      title: '知识库详情 - 企业知识管理系统'
    }
  },
  {
    path: '/documents',
    name: 'Documents',
    component: () => import('@/pages/DocumentPage.vue'),
    meta: { 
      requiresAuth: true,
      title: '文档管理 - 企业知识管理系统'
    }
  },
  {
    path: '/document/:id',
    name: 'DocumentDetail',
    component: () => import('@/pages/DocumentDetail.vue'),
    meta: {
      requiresAuth: true,
      title: '文档详情 - 企业知识管理系统'
    }
  },
  {
    path: '/operation-logs',
    name: 'OperationLogs',
    component: () => import('@/pages/OperationLogPage.vue'),
    meta: { 
      requiresAuth: true,
      title: '操作日志管理 - 企业知识管理系统'
    }
  },
  {
    path: '/ai-assistant',
    name: 'AIAssistant',
    component: () => import('@/pages/AIAssistantPage.vue'),
    meta: { 
      requiresAuth: true,
      title: '智能秘书 - 企业知识管理系统'
    }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: { 
      requiresAuth: true,
      title: '个人中心 - 企业知识管理系统'
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { 
      requiresAuth: false,
      title: '登录 - 企业知识管理系统'
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/Register.vue'),
    meta: { 
      requiresAuth: false,
      title: '注册 - 企业知识管理系统'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFound.vue'),
    meta: { 
      requiresAuth: false,
      title: '页面不存在 - 企业知识管理系统'
    }
  }
]

// 创建路由实例
const router = createRouter({
  // 使用 Hash 路由，避免刷新或直达时后端 404 问题
  history: createWebHashHistory(),
  routes
})

// 路由守卫 - 认证检查
router.beforeEach((to, _from, next) => {
  // 直接读取本地存储中的令牌，避免响应式时序问题
  const storedToken = localStorage.getItem('token') || localStorage.getItem('userToken')
  const isLoggedInFlag = sessionStorage.getItem('isLoggedIn') === 'true'
  const isAuthed = !!storedToken && isLoggedInFlag
  
  if (to.meta.title) {
    document.title = to.meta.title as string
  }
  
  if (to.meta.requiresAuth && !isAuthed) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  
  if (to.name === 'Login' && isAuthed) {
    next({ name: 'Home' })
    return
  }
  
  next()
})

// 路由错误处理
router.onError((error) => {
  console.error('路由错误:', error)
})

export default router
