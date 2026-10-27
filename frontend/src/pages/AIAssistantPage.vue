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

      <div class="assistant-page">
        <div class="page-header">
          <h1>智能秘书</h1>
          <ul class="breadcrumb">
            <li><a href="#">首页</a></li>
            <li>智能秘书</li>
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
            <h2 class="section-title">智能问答</h2>
          </div>

          <div class="chat-container">
            <div class="chat-sidebar">
              <div class="section-header" style="margin: 0; padding: 12px 12px;">
                <h3 class="section-title" style="margin: 0;">对话历史</h3>
                <a-button type="primary" size="small" @click="createNewConversation">新建对话</a-button>
              </div>
              <div class="chat-history">
                <div 
                  v-for="c in conversations" 
                  :key="c.id"
                  :class="['conversation-item', { active: activeConversation === c.id }]"
                >
                  <div class="conversation-content" @click="selectConversation(c.id)">
                    <div class="conversation-title">{{ c.title }}</div>
                    <div class="conversation-time">{{ formatTime(c.time) }}</div>
                  </div>
                  <a-button 
                    type="text" 
                    danger 
                    size="small" 
                    @click.stop="deleteConversation(c.id)"
                    class="delete-btn"
                  >
                    删除
                  </a-button>
                </div>
              </div>
            </div>

            <div class="chat-main">
              <div class="chat-messages" ref="messagesContainer">
                <div v-for="m in messages" :key="m.id" :class="['message', m.role]">
                  <div class="message-sender">{{ m.role === 'assistant' ? '智能秘书' : '您' }}</div>
                  <div class="message-content">
                    <template v-if="m.role === 'assistant'">
                      <div class="document-cards" v-if="parseDocuments(m.content).length > 0">
                        <a-card 
                          v-for="doc in parseDocuments(m.content)" 
                          :key="doc.id" 
                          class="document-card"
                          hoverable
                          @click="goToDocument(doc.id)"
                        >
                          <div class="doc-card-header">
                            <span class="doc-icon">📄</span>
                            <span class="doc-title">{{ doc.title }}</span>
                          </div>
                          <div class="doc-card-body">
                            <div class="doc-info-item">
                              <span class="doc-info-label">所属知识库</span>
                              <span class="doc-info-value">{{ doc.knowledgeBaseName || '未知知识库' }}</span>
                            </div>
                            <div class="doc-info-item">
                              <span class="doc-info-label">匹配度</span>
                              <span class="doc-score">{{ doc.score || '--' }}</span>
                            </div>
                          </div>
                        </a-card>
                      </div>
                      <div class="message-text">{{ m.content }}</div>
                    </template>
                    <template v-else>
                      {{ m.content }}
                    </template>
                  </div>
                </div>
              </div>

              <div class="chat-input">
                <div class="input-row">
                  <a-upload :show-upload-list="false" :before-upload="handleBeforeUpload" :max-count="1" accept=".pdf,.doc,.docx,.txt,.md,.ppt,.pptx,.xls,.xlsx">
                    <a-button>上传文件并入库</a-button>
                  </a-upload>
                  <a-input 
                    ref="inputEl"
                    v-model:value="inputMessage" 
                    placeholder="请输入您的问题..."
                    @pressEnter.prevent="sendMessage"
                    :disabled="isLoading"
                  />
                  <a-button type="primary" @click="sendMessage" :loading="isLoading" :disabled="!inputMessage.trim()">发送</a-button>
                </div>
                <div class="chat-settings">
                  <a-select v-model:value="selectedModel" style="width: 280px;">
                    <a-select-option value="">系统默认 (Qwen3.5-9B)</a-select-option>
                    <a-select-option value="Qwen/Qwen3.5-9B">Qwen3.5-9B (平衡·推荐)</a-select-option>
                    <a-select-option value="Qwen/Qwen3-8B">Qwen3-8B (轻量·快速)</a-select-option>
                    <a-select-option value="Qwen/Qwen3.5-27B">Qwen3.5-27B (高质量·慢)</a-select-option>
                  </a-select>
                  <a-select v-model:value="temperature" style="width: 120px;">
                    <a-select-option value="0.7">温度: 0.7</a-select-option>
                    <a-select-option value="0.5">温度: 0.5</a-select-option>
                    <a-select-option value="0.9">温度: 0.9</a-select-option>
                  </a-select>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { QuestionCircleOutlined, SearchOutlined, CheckCircleOutlined, SmileOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getChatOnce, getChatStream, ingestUpload, getAssistantStats, type AssistantStats, type ChatRequestParams } from '@/api/aiAssistant'
import { getUserProfile, updateUserProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

interface Conversation { id: string; title: string; time: Date }
interface ChatMessage { id: string; role: 'user' | 'assistant'; content: string; timestamp: string }
interface ParsedDocument { id: string; title: string; description?: string; score?: string; knowledgeBaseName?: string }

const router = useRouter()
const route = useRoute()
const isActive = (path: string) => computed(() => route.path === path).value
const go = (path: string) => { if (route.path !== path) router.push(path) }

const authStore = useAuthStore()
const avatarUrl = computed(() => authStore.user?.avatarUrl || '/default-avatar.svg')
const displayName = computed(() => authStore.user?.realName || (authStore.user as any)?.username || '用户')
const roleText = computed(() => ((authStore.user as any)?.isAdmin ? '管理员' : '用户'))

const statsData = ref<AssistantStats | null>(null)

const stats = computed(() => {
  const data = statsData.value;
  if (!data) {
    return [
      { title: '今日问答', value: '--', desc: '加载中...', icon: QuestionCircleOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
      { title: '知识检索', value: '--', desc: '加载中...', icon: SearchOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
      { title: '任务完成', value: '--', desc: '加载中...', icon: CheckCircleOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
      { title: '用户满意度', value: '--', desc: '加载中...', icon: SmileOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' }
    ];
  }
  return [
    { title: '今日问答', value: `${data.todayChatCount || 0}次`, desc: '较昨日增加15%', icon: QuestionCircleOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
    { title: '知识检索', value: `${data.todayMessageCount || 0}次`, desc: `准确率${data.accuracyRate || 92}%`, icon: SearchOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
    { title: '任务完成', value: `${data.taskCompleted || 0}项`, desc: `${data.taskInProgress || 0}项进行中`, icon: CheckCircleOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' },
    { title: '用户满意度', value: `${data.satisfactionRate || 94}%`, desc: '较上月提升3%', icon: SmileOutlined, iconBg: '#e1e9ff', iconColor: '#3882F8' }
  ];
})

const conversations = ref<Conversation[]>([])
const messages = ref<ChatMessage[]>([])
const activeConversation = ref<string | null>(null)
const activeFileId = ref<string | null>(null)
const conversationMessages = ref<Record<string, ChatMessage[]>>({})
const conversationFileIdMap = ref<Record<string, string | null>>({})
const inputMessage = ref('')
const inputEl = ref()
const isLoading = ref(false)
const selectedModel = ref('')
const temperature = ref('0.7')
const messagesContainer = ref<HTMLElement | null>(null)

const formatTime = (time: Date) => {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today); yesterday.setDate(yesterday.getDate() - 1)
  const d = new Date(time); const dDay = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  if (dDay.getTime() === today.getTime()) return `今天 ${d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  if (dDay.getTime() === yesterday.getTime()) return `昨天 ${d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  return d.toLocaleDateString('zh-CN')
}

const scrollToBottom = () => { nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight }) }

// 本地持久化（避免偶发刷新/重渲导致对话丢失）
const STORAGE_KEY = 'ai_assistant_state_v1'
const persistState = () => {
  try {
    const payload = {
      conversations: conversations.value,
      activeConversation: activeConversation.value,
      activeFileId: activeFileId.value,
      conversationMessages: conversationMessages.value,
      conversationFileIdMap: conversationFileIdMap.value,
      selectedModel: selectedModel.value,
      temperature: temperature.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  } catch {}
}
const restoreState = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return false
    const data = JSON.parse(raw)
    conversations.value = Array.isArray(data.conversations) ? data.conversations : []
    activeConversation.value = data.activeConversation || null
    activeFileId.value = data.activeFileId || null
    conversationMessages.value = data.conversationMessages || {}
    conversationFileIdMap.value = data.conversationFileIdMap || {}
    if (data.selectedModel !== undefined) selectedModel.value = data.selectedModel
    if (data.temperature !== undefined) temperature.value = String(data.temperature)
    if (activeConversation.value) {
      messages.value = conversationMessages.value[activeConversation.value] || []
      activeFileId.value = conversationFileIdMap.value[activeConversation.value] || null
    }
    return true
  } catch { return false }
}

const createNewConversation = () => {
  const id = Date.now().toString()
  conversations.value.unshift({ id, title: '新对话', time: new Date() })
  activeConversation.value = id
  conversationMessages.value[id] = []
  conversationFileIdMap.value[id] = null
  messages.value = []
  persistState()
}

const deleteConversation = (id: string) => {
  const index = conversations.value.findIndex(c => c.id === id)
  if (index === -1) return
  
  if (activeConversation.value === id) {
    conversations.value.splice(index, 1)
    delete conversationMessages.value[id]
    delete conversationFileIdMap.value[id]
    
    if (conversations.value.length > 0) {
      selectConversation(conversations.value[0].id)
    } else {
      createNewConversation()
    }
  } else {
    conversations.value.splice(index, 1)
    delete conversationMessages.value[id]
    delete conversationFileIdMap.value[id]
  }
  
  persistState()
}

const selectConversation = (id: string) => {
  const isSame = activeConversation.value === id
  activeConversation.value = id
  const currentMsgs = conversationMessages.value[id] || []
  if (!isSame && currentMsgs.length === 0) {
    conversationMessages.value[id] = [
      { id: '1', role: 'assistant', content: '您好！这里是智能秘书，请问需要什么帮助？', timestamp: new Date().toISOString() }
    ]
  }
  messages.value = conversationMessages.value[id] || []
  activeFileId.value = conversationFileIdMap.value[id] || null
  scrollToBottom()
  persistState()
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !activeConversation.value) return
  const current = inputMessage.value.trim()
  inputMessage.value = ''
  
  setTimeout(() => {
    inputMessage.value = ''
    if (inputEl.value) {
      const input = inputEl.value.$el?.querySelector('input') || inputEl.value.$el?.querySelector('textarea')
      if (input) {
        input.value = ''
        input.dispatchEvent(new Event('input', { bubbles: true }))
      }
    }
  }, 50)
  
  const userMsg: ChatMessage = { id: Date.now().toString(), role: 'user', content: current, timestamp: new Date().toISOString() }
  const convId = activeConversation.value
  const list = conversationMessages.value[convId] || []
  conversationMessages.value[convId] = [...list, userMsg]
  messages.value = conversationMessages.value[convId]
  persistState()
  isLoading.value = true
  scrollToBottom()

  const replyId = (Date.now() + 1).toString()
  const reply: ChatMessage = { id: replyId, role: 'assistant', content: '', timestamp: new Date().toISOString() }
  const convMsgs = conversationMessages.value[convId] || []
  conversationMessages.value[convId] = [...convMsgs, reply]
  messages.value = conversationMessages.value[convId]
  scrollToBottom()

  const model = selectedModel.value || undefined
  const temperatureNum = parseFloat(temperature.value)

  const requestParams = {
    prompt: current,
    topK: 5,
    threshold: 0.3,
    fileId: activeFileId.value || undefined,
    model,
    temperature: isNaN(temperatureNum) ? 0.3 : temperatureNum,
    chatId: convId,
  }

  let streamAborted = false

  const handleError = () => {
    if (streamAborted) return
    streamAborted = true
    message.warning('流式响应失败，切换到普通模式')
    sendMessageFallback(requestParams, replyId, convId)
  }

  const handleDone = () => {
    if (streamAborted) return
    isLoading.value = false
    persistState()
    scrollToBottom()
    if (inputEl.value?.focus) inputEl.value.focus()
  }

  getChatStream(requestParams,
    (chunk: string) => {
      if (streamAborted) return
      const msgs = conversationMessages.value[convId]
      const target = msgs.find(m => m.id === replyId)
      if (target) {
        target.content += chunk
        messages.value = [...msgs]
        scrollToBottom()
      }
    },
    handleError,
    handleDone
  )
}

const sendMessageFallback = async (params: ChatRequestParams, replyId: string, convId: string) => {
  try {
    const result = await getChatOnce(params)
    const msgs = conversationMessages.value[convId]
    const target = msgs.find(m => m.id === replyId)
    if (target) {
      target.content = result.data?.answer || '暂无回答'
      messages.value = [...msgs]
    }
  } catch (error: any) {
    message.error('发送失败: ' + (error.message || '未知错误'))
    const msgs = conversationMessages.value[convId]
    const target = msgs.find(m => m.id === replyId)
    if (target) {
      target.content = target.content || '' + '\n\n[错误: ' + error.message + ']'
      messages.value = [...msgs]
    }
  } finally {
    isLoading.value = false
    persistState()
    scrollToBottom()
    if (inputEl.value?.focus) inputEl.value.focus()
  }
}

const handleBeforeUpload = async (file: File) => {
  if (!activeConversation.value) {
    message.warning('请先创建或选择一个对话')
    return false
  }
  try {
    message.loading({ content: '正在上传并入库...', key: 'ingest', duration: 0 })
    const fileId = await ingestUpload(file)
    activeFileId.value = fileId
    if (activeConversation.value) {
      conversationFileIdMap.value[activeConversation.value] = fileId
    }
    persistState()
    message.success({ content: `文件上传并入库成功，已绑定到当前对话`, key: 'ingest' })
    const infoMsg: ChatMessage = { 
      id: Date.now().toString(), 
      role: 'assistant', 
      content: `已成功上传文件 "${file.name}"，您现在可以基于该文件进行提问。`, 
      timestamp: new Date().toISOString() 
    }
    const convId = activeConversation.value
    const list = conversationMessages.value[convId] || []
    conversationMessages.value[convId] = [...list, infoMsg]
    messages.value = conversationMessages.value[convId]
    scrollToBottom()
  } catch (e: any) {
    const errorMsg = e?.message || '入库失败'
    message.error({ content: errorMsg, key: 'ingest' })
    if (errorMsg.includes('Milvus') || errorMsg.includes('服务不可用')) {
      message.warning('向量数据库服务不可用，请确保 Milvus 已启动')
    }
  }
  return false // 阻止 antd 自动上传
}

const parseDocuments = (content: string): ParsedDocument[] => {
  const documents: ParsedDocument[] = []
  const docIdSet = new Set<string>()
  
  const patterns = [
    /([^\s（\(]+?\.(?:txt|pdf|docx?|md|ppt[x]?|xls[x]?))(?:\s*\([^)]+\))?\s*[（\(]?文档?ID[：:]?\s*(\d+)[）\)]?/gi,
    /文档ID[:：]?\s*(\d+)[^\n]*?([^\s|，。、；：""''（）【】《》,.;:\'\"\(\)\[\]<>]+?\.(?:txt|pdf|docx?|md|ppt[x]?|xls[x]?))(?:\s*\([^)]+\))?/gi
  ]
  
  for (const pattern of patterns) {
    let match
    pattern.lastIndex = 0
    
    while ((match = pattern.exec(content)) !== null) {
      const docId = match[match.length === 3 ? 2 : 1]
      if (docIdSet.has(docId)) continue
      docIdSet.add(docId)
      
      const titleGroup = match.length === 3 ? 1 : 2
      const title = match[titleGroup] ? match[titleGroup].trim() : ''
      
      const doc: ParsedDocument = { id: docId, title: title, knowledgeBaseName: '', score: '' }
      
      const startPos = Math.max(0, match.index - 50)
      const endPos = Math.min(content.length, match.index + match[0].length + 200)
      const context = content.substring(startPos, endPos)
      
      const kbMatch = context.match(/所属知识库[：:]?\s*([^\s，。、；：""''（）【】《》,.;:\'\"\(\)\[\]<>]+)/)
      if (kbMatch) {
        doc.knowledgeBaseName = kbMatch[1].trim()
      }
      
      const scoreMatch = context.match(/匹配度[：:]?\s*([\d.]+)/)
      if (scoreMatch) {
        doc.score = scoreMatch[1]
      }
      
      if (doc.title) {
        documents.push(doc)
      }
    }
  }
  
  return documents
}


const goToDocument = (docId: string) => {
  router.push({ 
    name: 'DocumentDetail', 
    params: { id: docId },
    query: { from: 'ai-assistant', chatId: activeConversation.value }
  })
}

onMounted(async () => {
  // 加载用户偏好模型
  try {
    const profile = await getUserProfile()
    if ((profile as any).preferredModel) {
      selectedModel.value = (profile as any).preferredModel
    }
  } catch { /* 使用默认模型 */ }
  
  // 获取统计数据
  try {
    statsData.value = await getAssistantStats()
  } catch {
    console.log('获取统计数据失败')
  }
  
  const restored = restoreState()
  if (!restored) {
    // 初始化一个会话
    createNewConversation()
    // 初次进入时仅在空消息情况下添加欢迎语
    selectConversation(conversations.value[0].id)
  }
})

// 模型切换时自动保存偏好
watch(selectedModel, async (newModel) => {
  try {
    await updateUserProfile({ preferredModel: newModel })
  } catch { /* 静默保存失败 */ }
})
</script>

<style scoped>
/* 统一两栏布局 */
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

/* 页面主体 */
.assistant-page { padding: 24px; }
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

.chat-container { display: flex; height: 600px; border: 1px solid #e3e6f0; border-radius: 8px; overflow: hidden; }
.chat-sidebar { width: 300px; border-right: 1px solid #e3e6f0; display: flex; flex-direction: column; }
.chat-history { flex: 1; overflow-y: auto; padding: 12px; }
.conversation-item { padding: 10px; border-radius: 6px; margin-bottom: 10px; cursor: pointer; transition: background-color .3s; display: flex; justify-content: space-between; align-items: center; }
.conversation-item:hover { background-color: #f0f4ff; }
.conversation-item.active { background-color: #f0f4ff; }
.conversation-content { flex: 1; min-width: 0; }
.conversation-title { font-weight: 500; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conversation-time { font-size: 12px; color: #5a5c69; }
.delete-btn { opacity: 0; transition: opacity 0.2s; padding: 0; margin-left: 8px; }
.conversation-item:hover .delete-btn { opacity: 1; }

.chat-main { flex: 1; display: flex; flex-direction: column; }
.chat-messages { flex: 1; padding: 16px; overflow-y: auto; background: #fafbfc; }
.message { margin-bottom: 14px; max-width: 80%; }
.message.user { margin-left: auto; }
.message-sender { font-weight: 500; margin-bottom: 6px; }
.message.user .message-sender { text-align: right; }
.message-content { padding: 12px 16px; border-radius: 18px; background: white; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
.message.user .message-content { background: #3882F8; color: #fff; }

.chat-input { padding: 12px; border-top: 1px solid #e3e6f0; background: #fff; }
.input-row { display: flex; gap: 10px; }
.chat-settings { display: flex; gap: 10px; margin-top: 10px; }

.document-cards { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.document-card { cursor: pointer; border: 1px solid #e8eaed; border-radius: 10px; transition: all 0.25s ease; background: #fafbfc; }
.document-card:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.12); background: #fff; transform: translateY(-1px); }
.doc-card-header { display: flex; align-items: center; gap: 10px; padding: 14px 16px 10px; border-bottom: 1px solid #f0f0f0; }
.doc-icon { font-size: 20px; }
.doc-title { font-weight: 600; color: #303133; font-size: 14px; flex: 1; }
.doc-card-body { padding: 10px 16px 14px; display: flex; flex-direction: column; gap: 8px; }
.doc-info-item { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.doc-info-label { color: #909399; font-weight: 400; }
.doc-info-value { color: #409eff; font-weight: 500; }
.doc-score { color: #67c23a; font-weight: 600; font-size: 13px; }
.message-text { font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; }

@media (max-width: 992px) {
  .search-box { width: 220px; }
  .app-layout { grid-template-columns: 220px 1fr; }
  .chat-container { flex-direction: column; height: auto; }
  .chat-sidebar { width: 100%; height: 220px; border-right: none; border-bottom: 1px solid #e3e6f0; }
}
</style>
