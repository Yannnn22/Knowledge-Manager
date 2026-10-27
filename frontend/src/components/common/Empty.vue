<template>
  <div class="empty-container">
    <div class="empty-content">
      <div class="empty-icon">
        <component :is="iconComponent" />
      </div>
      <h3 class="empty-title">{{ title }}</h3>
      <p v-if="description" class="empty-description">{{ description }}</p>
      <div v-if="showAction" class="empty-actions">
        <slot name="action">
          <a-button type="primary" @click="handleAction">
            <component :is="actionIcon" v-if="actionIcon" />
            {{ actionText }}
          </a-button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  InboxOutlined,
  FileTextOutlined,
  UserOutlined,
  FolderOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'

interface Props {
  type?: 'default' | 'search' | 'data' | 'file' | 'user' | 'folder'
  title?: string
  description?: string
  showAction?: boolean
  actionText?: string
  actionIcon?: any
  onAction?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  title: '暂无数据',
  showAction: false,
  actionText: '刷新'
})

const emit = defineEmits<{
  action: []
}>()

// 根据类型选择图标
const iconComponent = computed(() => {
  const iconMap = {
    default: InboxOutlined,
    search: SearchOutlined,
    data: InboxOutlined,
    file: FileTextOutlined,
    user: UserOutlined,
    folder: FolderOutlined
  }
  return iconMap[props.type] || InboxOutlined
})

// 根据类型设置默认标题
const title = computed(() => {
  if (props.title !== '暂无数据') {
    return props.title
  }
  
  const titleMap = {
    default: '暂无数据',
    search: '未找到相关内容',
    data: '暂无数据',
    file: '暂无文件',
    user: '暂无用户',
    folder: '文件夹为空'
  }
  return titleMap[props.type] || '暂无数据'
})

// 根据类型设置默认描述
const description = computed(() => {
  if (props.description) {
    return props.description
  }
  
  const descriptionMap = {
    default: '',
    search: '尝试调整搜索条件或关键词',
    data: '当前没有可显示的数据',
    file: '还没有上传任何文件',
    user: '还没有添加任何用户',
    folder: '这个文件夹还是空的'
  }
  return descriptionMap[props.type] || ''
})

// 处理操作按钮点击
const handleAction = () => {
  if (props.onAction) {
    props.onAction()
  } else {
    emit('action')
  }
}
</script>

<style scoped>
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  min-height: 200px;
}

.empty-content {
  text-align: center;
  max-width: 400px;
}

.empty-icon {
  font-size: 64px;
  color: #bfbfbf;
  margin-bottom: 16px;
  opacity: 0.8;
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
  margin: 0 0 8px 0;
}

.empty-description {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.empty-actions {
  margin-top: 24px;
}

/* 不同尺寸变体 */
.empty-container.small {
  padding: 20px;
  min-height: 120px;
}

.empty-container.small .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-container.small .empty-title {
  font-size: 14px;
  margin-bottom: 6px;
}

.empty-container.small .empty-description {
  font-size: 12px;
  margin-bottom: 16px;
}

.empty-container.large {
  padding: 60px 20px;
  min-height: 300px;
}

.empty-container.large .empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
}

.empty-container.large .empty-title {
  font-size: 18px;
  margin-bottom: 12px;
}

.empty-container.large .empty-description {
  font-size: 16px;
  margin-bottom: 32px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .empty-container {
    padding: 24px 16px;
  }
  
  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }
  
  .empty-title {
    font-size: 14px;
    margin-bottom: 6px;
  }
  
  .empty-description {
    font-size: 12px;
    margin-bottom: 16px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .empty-title {
    color: #ffffff;
  }
  
  .empty-description {
    color: #bfbfbf;
  }
  
  .empty-icon {
    color: #595959;
  }
}

/* 动画效果 */
.empty-content {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 悬停效果 */
.empty-actions .ant-btn:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
}
</style>