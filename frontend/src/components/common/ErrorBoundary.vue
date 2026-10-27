<template>
  <div class="error-boundary">
    <div v-if="hasError" class="error-container">
      <div class="error-content">
        <div class="error-icon">
          <ExclamationCircleOutlined />
        </div>
        <h3 class="error-title">{{ title || '出现错误' }}</h3>
        <p class="error-message">{{ message || '抱歉，页面出现了一些问题' }}</p>
        <div v-if="showDetails && errorDetails" class="error-details">
          <a-collapse>
            <a-collapse-panel key="1" header="错误详情">
              <pre class="error-stack">{{ errorDetails }}</pre>
            </a-collapse-panel>
          </a-collapse>
        </div>
        <div class="error-actions">
          <a-space>
            <a-button type="primary" @click="handleRetry">
              <ReloadOutlined />
              重试
            </a-button>
            <a-button @click="handleGoBack">
              <ArrowLeftOutlined />
              返回
            </a-button>
            <a-button v-if="showReport" @click="handleReport">
              <BugOutlined />
              报告问题
            </a-button>
          </a-space>
        </div>
      </div>
    </div>
    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { message } from 'ant-design-vue'
import {
  ExclamationCircleOutlined,
  ReloadOutlined,
  ArrowLeftOutlined,
  BugOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

interface Props {
  title?: string
  message?: string
  showDetails?: boolean
  showReport?: boolean
  onRetry?: () => void
  onGoBack?: () => void
  onReport?: (error: Error) => void
}

const props = withDefaults(defineProps<Props>(), {
  showDetails: false,
  showReport: true
})

const router = useRouter()
const hasError = ref(false)
const errorDetails = ref('')
const currentError = ref<Error | null>(null)

// 捕获子组件错误
onErrorCaptured((error: Error) => {
  console.error('ErrorBoundary caught error:', error)
  hasError.value = true
  errorDetails.value = error.stack || error.message
  currentError.value = error
  return false // 阻止错误继续传播
})

// 重试处理
const handleRetry = () => {
  if (props.onRetry) {
    props.onRetry()
  } else {
    hasError.value = false
    errorDetails.value = ''
    currentError.value = null
    // 刷新页面
    window.location.reload()
  }
}

// 返回处理
const handleGoBack = () => {
  if (props.onGoBack) {
    props.onGoBack()
  } else {
    router.go(-1)
  }
}

// 报告问题处理
const handleReport = () => {
  if (props.onReport && currentError.value) {
    props.onReport(currentError.value)
  } else {
    // 默认报告处理
    message.info('错误报告已发送，感谢您的反馈')
    console.log('Error report:', {
      error: currentError.value,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString(),
      url: window.location.href
    })
  }
}

// 暴露方法供外部调用
defineExpose({
  triggerError: (error: Error) => {
    hasError.value = true
    errorDetails.value = error.stack || error.message
    currentError.value = error
  },
  reset: () => {
    hasError.value = false
    errorDetails.value = ''
    currentError.value = null
  }
})
</script>

<style scoped>
.error-boundary {
  width: 100%;
  height: 100%;
}

.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px 20px;
  background-color: #fafafa;
}

.error-content {
  text-align: center;
  max-width: 500px;
  width: 100%;
}

.error-icon {
  font-size: 64px;
  color: #ff4d4f;
  margin-bottom: 24px;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
}

.error-message {
  font-size: 16px;
  color: #8c8c8c;
  margin: 0 0 32px 0;
  line-height: 1.6;
}

.error-details {
  margin: 24px 0;
  text-align: left;
}

.error-stack {
  background-color: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  color: #595959;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.error-actions {
  margin-top: 32px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .error-container {
    padding: 20px 16px;
    min-height: 300px;
  }
  
  .error-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }
  
  .error-title {
    font-size: 20px;
    margin-bottom: 12px;
  }
  
  .error-message {
    font-size: 14px;
    margin-bottom: 24px;
  }
  
  .error-actions {
    margin-top: 24px;
  }
  
  .error-actions .ant-space {
    flex-direction: column;
    width: 100%;
  }
  
  .error-actions .ant-btn {
    width: 100%;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .error-container {
    background-color: #141414;
  }
  
  .error-title {
    color: #ffffff;
  }
  
  .error-message {
    color: #bfbfbf;
  }
  
  .error-stack {
    background-color: #262626;
    border-color: #434343;
    color: #bfbfbf;
  }
}
</style>