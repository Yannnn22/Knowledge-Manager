<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-title">智汇库管理系统</div>
      </div>
      <div class="user-card">
        <img v-if="avatarUrl" :src="avatarUrl" class="avatar" alt="avatar" />
        <img v-else src="/default-avatar.svg" class="avatar" alt="default avatar" />
        <div class="user-meta">
          <div class="user-name">{{ profileForm.realName || profileForm.username }}</div>
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

    <div class="profile-page">
      <div class="page-header">
        <h1>个人中心</h1>
        <ul class="breadcrumb">
          <li><a href="#">首页</a></li>
          <li>个人中心</li>
        </ul>
      </div>
  
      <div class="profile-container">
        <div class="profile-sidebar">
          <div class="profile-card">
              <div class="avatar-wrapper">
                <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" alt="avatar" />
                <img v-else src="/default-avatar.svg" class="avatar-img" alt="default avatar" />
                <div class="avatar-actions">
                  <a-upload :show-upload-list="false" :before-upload="handleAvatarBeforeUpload">
                    <a-button size="small">更换头像</a-button>
                  </a-upload>
                </div>
              </div>
              <div class="profile-info">
                <h2>{{ profileForm.realName || profileForm.username }}</h2>
                <p>{{ roleText }}</p>
                <p>{{ profileForm.department }}</p>
                <p>{{ profileForm.email }}</p>
            </div>
            <div class="profile-stats">
                <div class="stat-item"><div class="stat-value">42</div><div class="stat-label">文档</div></div>
                <div class="stat-item"><div class="stat-value">128</div><div class="stat-label">贡献</div></div>
                <div class="stat-item"><div class="stat-value">12</div><div class="stat-label">收藏</div></div>
            </div>
          </div>
          
          <div class="content-section">
            <h3 class="section-title">安全设置</h3>
              <div class="form-group"><a-button type="primary" style="width:100%" @click="showChangePassword = true">修改密码</a-button></div>
              <div class="form-group"><a-button style="width:100%" @click="message.info('设备管理（演示）')">登录设备管理</a-button></div>
          </div>
        </div>
        
        <div class="profile-content">
          <div class="content-section">
            <div class="section-header">
              <h2 class="section-title">基本信息</h2>
            </div>
              <a-form :model="profileForm" layout="vertical" @finish="handleProfileSubmit">
              <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="用户名"><a-input v-model:value="profileForm.username" disabled /></a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="真实姓名"><a-input v-model:value="profileForm.realName" /></a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="20">
                  <a-col :span="12"><a-form-item label="邮箱"><a-input v-model:value="profileForm.email" type="email" /></a-form-item></a-col>
                  <a-col :span="12"><a-form-item label="手机号"><a-input v-model:value="profileForm.phone" /></a-form-item></a-col>
              </a-row>
              <a-row :gutter="20">
                <a-col :span="12">
                  <a-form-item label="部门">
                    <a-select v-model:value="profileForm.department">
                      <a-select-option value="技术部">技术部</a-select-option>
                      <a-select-option value="产品部">产品部</a-select-option>
                      <a-select-option value="设计部">设计部</a-select-option>
                      <a-select-option value="市场部">市场部</a-select-option>
                      <a-select-option value="销售部">销售部</a-select-option>
                      <a-select-option value="人力资源">人力资源</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                  <a-col :span="12"><a-form-item label="所在地"><a-input v-model:value="profileForm.location" /></a-form-item></a-col>
              </a-row>
                <a-form-item label="个人网站"><a-input v-model:value="profileForm.website" type="url" /></a-form-item>
                <a-form-item label="个人简介"><a-textarea v-model:value="profileForm.bio" :rows="4" /></a-form-item>
                <div class="form-actions"><a-button @click="resetForm">取消</a-button><a-button type="primary" html-type="submit" :loading="submitting">保存更改</a-button></div>
            </a-form>
          </div>
          
          <div class="content-section">
              <div class="section-header"><h2 class="section-title">通知设置</h2></div>
            <div class="switch-container">
                <div><div class="switch-label">邮件通知</div><div class="switch-desc">接收重要通知和更新摘要</div></div>
                <a-switch v-model:checked="notificationSettings.email" @change="handleNotifyChange" />
              </div>
            <div class="switch-container">
                <div><div class="switch-label">系统通知</div><div class="switch-desc">接收系统消息和提醒</div></div>
                <a-switch v-model:checked="notificationSettings.system" @change="handleNotifyChange" />
              </div>
            <div class="switch-container">
                <div><div class="switch-label">任务提醒</div><div class="switch-desc">接收任务分配和截止日期提醒</div></div>
                <a-switch v-model:checked="notificationSettings.task" />
            </div>
          </div>

          <div class="content-section">
            <div class="section-header"><h2 class="section-title">AI 配置</h2></div>
            <div class="ai-config-body">
              <div class="ai-config-item">
                <div class="switch-label">DashScope API Key</div>
                <div class="switch-desc">配置您的阿里云 DashScope API Key，用于智能秘书问答。留空则使用系统默认 Key。</div>
                <div class="api-key-input-row">
                  <a-input-password
                    v-model:value="aiApiKey"
                    placeholder="请输入 DashScope API Key（sk-...）"
                    :visibility-toggle="true"
                    style="flex:1"
                  />
                  <a-button type="primary" :loading="savingApiKey" @click="handleSaveApiKey">保存</a-button>
                </div>
                <div v-if="savedApiKeyMasked" class="api-key-status">
                  <span class="status-dot configured"></span>
                  已配置：{{ savedApiKeyMasked }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  
        <a-modal v-model:visible="showChangePassword" title="修改密码" @ok="handleChangePassword" @cancel="showChangePassword=false">
          <a-form :model="passwordForm" layout="vertical">
            <a-form-item label="当前密码"><a-input-password v-model:value="passwordForm.currentPassword" /></a-form-item>
            <a-form-item label="新密码"><a-input-password v-model:value="passwordForm.newPassword" /></a-form-item>
            <a-form-item label="确认新密码"><a-input-password v-model:value="passwordForm.confirmPassword" /></a-form-item>
        </a-form>
      </a-modal>
      </div>
    </section>
    </div>
  </template>
  
  <script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getUserProfile, updateUserProfile, changePassword, uploadAvatar } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

interface UserProfile { username: string; realName: string; email: string; phone: string; department: string; location: string; website: string; bio: string; avatarUrl?: string; notificationEmail?: boolean; notificationSystem?: boolean; status?: number; isAdmin?: boolean; preferredModel?: string; customApiKey?: string }
interface NotificationSettings { email: boolean; system: boolean; task: boolean }
interface PasswordForm { currentPassword: string; newPassword: string; confirmPassword: string }

const router = useRouter(); const route = useRoute()
const isActive = (path: string) => computed(() => route.path === path).value
const go = (path: string) => { if (route.path !== path) router.push(path) }

const userProfile = ref<UserProfile>({ username: '', realName: '', email: '', phone: '', department: '技术部', location: '', website: '', bio: '', avatarUrl: '', isAdmin: false })
const profileForm = reactive<UserProfile>({ ...userProfile.value })
const notificationSettings = reactive<NotificationSettings>({ email: true, system: true, task: false })
const passwordForm = reactive<PasswordForm>({ currentPassword: '', newPassword: '', confirmPassword: '' })
const authStore = useAuthStore()
const avatarUrl = ref<string>('')
const aiApiKey = ref('')
const savedApiKeyMasked = ref('')
const savingApiKey = ref(false)
const roleText = computed(() => {
  if (userProfile.value.isAdmin || (authStore as any)?.user?.isAdmin) return '管理员'
  const s = userProfile.value.status
  if (s === 2) return '管理员'
  if (s === 1) return '普通用户'
  if (s === 0) return '已禁用'
  if (s === 3) return '离职'
  return '用户'
})

const handleAvatarBeforeUpload = async (file: File) => {
  const isImg = file.type.startsWith('image/')
  if (!isImg) { message.error('请上传图片文件'); return false }
  try {
    const url = await uploadAvatar(file)
    await updateUserProfile({ ...profileForm, avatarUrl: url, notificationEmail: notificationSettings.email, notificationSystem: notificationSettings.system, customApiKey: undefined, preferredModel: undefined })
    avatarUrl.value = url
    userProfile.value.avatarUrl = url
    // 同步更新全局 authStore，确保其他页面立即生效
    try { (authStore as any).user = { ...(authStore as any).user, avatarUrl: url } } catch {}
    message.success('头像已更新')
  } catch (e) {
    console.error(e)
    message.error('头像更新失败')
  }
  return false
}

const handleNotifyChange = async () => {
  try {
    await updateUserProfile({ ...profileForm, notificationEmail: notificationSettings.email, notificationSystem: notificationSettings.system, customApiKey: undefined, preferredModel: undefined })
  } catch {
    message.error('通知设置更新失败')
  }
}

const handleSaveApiKey = async () => {
  savingApiKey.value = true
  try {
    await updateUserProfile({ ...profileForm, customApiKey: aiApiKey.value })
    userProfile.value.customApiKey = aiApiKey.value
    profileForm.customApiKey = aiApiKey.value  // 同步到 profileForm，防止后续"保存更改"覆盖
    savedApiKeyMasked.value = maskApiKey(aiApiKey.value)
    message.success('API Key 已保存')
    aiApiKey.value = ''
  } catch {
    message.error('API Key 保存失败')
  } finally {
    savingApiKey.value = false
  }
}

const maskApiKey = (key: string | undefined | null) => {
  if (!key || key.length < 8) return ''
  return key.substring(0, 4) + '****' + key.substring(key.length - 4)
}

const showChangePassword = ref(false)
const submitting = ref(false)

const handleChangePassword = async () => {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('请正确填写密码');
    return;
  }
  try {
    await changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword, confirmPassword: passwordForm.confirmPassword })
    message.success('密码修改成功')
    showChangePassword.value = false
    passwordForm.currentPassword = passwordForm.newPassword = passwordForm.confirmPassword = ''
  } catch {
    message.error('密码修改失败')
  }
}
  
  const handleProfileSubmit = async () => {
  submitting.value = true
  try {
    // 排除 AI 相关字段，避免覆盖"AI 配置"区块单独保存的内容
    const { customApiKey, preferredModel, ...basicFields } = profileForm
    const payload = { ...basicFields }
    await updateUserProfile(payload)
    Object.assign(userProfile.value, payload)
    // 姓名等基础信息修改后，同步到全局
    try { (authStore as any).user = { ...(authStore as any).user, realName: payload.realName, username: payload.username, department: payload.department } } catch {}
    message.success('个人信息更新成功')
  } catch {
    message.error('个人信息更新失败')
    } finally {
    submitting.value = false
  }
}
const resetForm = () => { Object.assign(profileForm, userProfile.value) }

onMounted(async () => {
  try {
    const data = await getUserProfile()
    userProfile.value = {
      username: data.username,
      realName: data.realName,
      email: data.email,
      department: data.department,
      avatarUrl: data.avatarUrl,
      phone: data.phone,
      bio: data.bio,
      location: data.location,
      website: data.website,
      notificationEmail: data.notificationEmail,
      notificationSystem: data.notificationSystem,
      status: (data as any).status,
      isAdmin: (data as any).isAdmin === true,
      preferredModel: (data as any).preferredModel,
      customApiKey: (data as any).customApiKey,
    } as unknown as UserProfile
    Object.assign(profileForm, userProfile.value)
    avatarUrl.value = userProfile.value.avatarUrl || ''
    notificationSettings.email = Boolean(data.notificationEmail)
    notificationSettings.system = Boolean(data.notificationSystem)
    savedApiKeyMasked.value = maskApiKey((data as any).customApiKey)
  } catch {
    message.error('获取用户信息失败')
  }
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
.profile-page { padding: 24px; }
.page-header { margin-bottom: 25px; }
.page-header h1 { font-size: 1.8rem; color: #5a5c69; margin-bottom: 5px; }
.breadcrumb { display: flex; list-style: none; color: #5a5c69; font-size: 0.9rem; padding: 0; }
.breadcrumb li:after { content: '/'; margin: 0 10px; }
.breadcrumb li:last-child:after { content: ''; }
.breadcrumb a { color: #3882F8; text-decoration: none; }

.profile-container { display: flex; gap: 24px; }
.profile-sidebar { width: 300px; flex-shrink: 0; }
.profile-content { flex: 1; }
.profile-card { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 3px 10px rgba(0,0,0,.05); text-align: center; margin-bottom: 20px; }
.avatar-wrapper { display:flex; flex-direction: column; align-items: center; gap: 8px; }
.avatar-img { width: 120px; height: 120px; border-radius: 50%; object-fit: cover; background: #f0f0f0; }
.profile-info h2 { margin-bottom: 8px; color: #5a5c69; }
.profile-info p { color: #5a5c69; margin-bottom: 6px; font-size: 0.9rem; }
.profile-stats { display: flex; justify-content: space-around; margin-top: 18px; padding-top: 18px; border-top: 1px solid #e3e6f0; }
.stat-item { text-align: center; }
.stat-value { font-size: 1.2rem; font-weight: 700; color: #3882F8; }
.stat-label { font-size: 12px; color: #5a5c69; }

.content-section { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 3px 10px rgba(0,0,0,.05); margin-bottom: 20px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: 1.2rem; color: #5a5c69; }
.form-group { margin-bottom: 12px; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
.switch-container { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e3e6f0; }
.switch-container:last-child { border-bottom: none; }
.switch-label { font-weight: 600; color: #5a5c69; }
.switch-desc { font-size: 0.85rem; color: #5a5c69; margin-top: 4px; }

.ai-config-body { padding: 4px 0; }
.ai-config-item { margin-bottom: 4px; }
.api-key-input-row { display: flex; gap: 10px; margin-top: 10px; align-items: center; }
.api-key-status { margin-top: 8px; font-size: 0.85rem; color: #5a5c69; display: flex; align-items: center; gap: 6px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.status-dot.configured { background: #52c41a; }
  
  @media (max-width: 992px) {
  .search-box { width: 220px; }
  .app-layout { grid-template-columns: 220px 1fr; }
  .profile-container { flex-direction: column; }
  .profile-sidebar { width: 100%; }
  }
  </style>
