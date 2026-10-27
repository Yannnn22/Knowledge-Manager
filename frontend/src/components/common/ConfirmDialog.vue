<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    :width="width"
    :centered="centered"
    :mask-closable="maskClosable"
    :keyboard="keyboard"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <template #footer>
      <div class="confirm-footer">
        <a-space>
          <a-button @click="handleCancel" :disabled="loading">
            {{ cancelText }}
          </a-button>
          <a-button
            :type="confirmType"
            :loading="loading"
            @click="handleConfirm"
          >
            <component :is="confirmIcon" v-if="confirmIcon && !loading" />
            {{ confirmText }}
          </a-button>
        </a-space>
      </div>
    </template>
    
    <div class="confirm-content">
      <div v-if="icon" class="confirm-icon">
        <component :is="icon" :class="iconClass" />
      </div>
      
      <div class="confirm-message">
        <div v-if="message" class="message-text">
          {{ message }}
        </div>
        
        <div v-if="description" class="message-description">
          {{ description }}
        </div>
        
        <!-- 自定义内容插槽 -->
        <div v-if="$slots.default" class="message-custom">
          <slot />
        </div>
        
        <!-- 输入确认 -->
        <div v-if="requireInput" class="confirm-input">
          <a-input
            v-model:value="inputValue"
            :placeholder="inputPlaceholder"
            :type="inputType"
            @keyup.enter="handleConfirm"
          />
          <div v-if="inputError" class="input-error">
            {{ inputError }}
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  ExclamationCircleOutlined,
  QuestionCircleOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  SaveOutlined,
  SendOutlined
} from '@ant-design/icons-vue'

interface Props {
  open?: boolean
  title?: string
  message?: string
  description?: string
  type?: 'warning' | 'danger' | 'info' | 'success' | 'question'
  confirmText?: string
  cancelText?: string
  confirmType?: 'primary' | 'danger' | 'default'
  width?: number
  centered?: boolean
  maskClosable?: boolean
  keyboard?: boolean
  requireInput?: boolean
  inputPlaceholder?: string
  inputType?: string
  inputValidator?: (value: string) => string | null
  onConfirm?: (inputValue?: string) => Promise<void> | void
  onCancel?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  title: '确认操作',
  confirmText: '确认',
  cancelText: '取消',
  confirmType: 'primary',
  width: 416,
  centered: true,
  maskClosable: false,
  keyboard: true,
  requireInput: false,
  inputPlaceholder: '请输入',
  inputType: 'text'
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: [inputValue?: string]
  cancel: []
}>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

const loading = ref(false)
const inputValue = ref('')
const inputError = ref('')

// 根据类型选择图标
const icon = computed(() => {
  const iconMap = {
    warning: ExclamationCircleOutlined,
    danger: ExclamationCircleOutlined,
    info: InfoCircleOutlined,
    success: CheckCircleOutlined,
    question: QuestionCircleOutlined
  }
  return props.type ? iconMap[props.type] : null
})

// 图标样式类
const iconClass = computed(() => {
  const classMap = {
    warning: 'icon-warning',
    danger: 'icon-danger',
    info: 'icon-info',
    success: 'icon-success',
    question: 'icon-question'
  }
  return props.type ? classMap[props.type] : ''
})

// 确认按钮图标
const confirmIcon = computed(() => {
  if (props.type === 'danger') {
    return DeleteOutlined
  }
  if (props.confirmText.includes('保存')) {
    return SaveOutlined
  }
  if (props.confirmText.includes('发送')) {
    return SendOutlined
  }
  return null
})

// 处理确认
const handleConfirm = async () => {
  // 输入验证
  if (props.requireInput) {
    if (!inputValue.value.trim()) {
      inputError.value = '请输入必要信息'
      return
    }
    
    if (props.inputValidator) {
      const error = props.inputValidator(inputValue.value)
      if (error) {
        inputError.value = error
        return
      }
    }
    
    inputError.value = ''
  }
  
  try {
    loading.value = true
    
    if (props.onConfirm) {
      await props.onConfirm(props.requireInput ? inputValue.value : undefined)
    }
    
    emit('confirm', props.requireInput ? inputValue.value : undefined)
    visible.value = false
    
    // 重置状态
    inputValue.value = ''
    inputError.value = ''
  } catch (error) {
    console.error('Confirm action failed:', error)
  } finally {
    loading.value = false
  }
}

// 处理取消
const handleCancel = () => {
  if (props.onCancel) {
    props.onCancel()
  }
  
  emit('cancel')
  visible.value = false
  
  // 重置状态
  inputValue.value = ''
  inputError.value = ''
}

// 监听弹窗打开状态，重置输入
watch(() => props.open, (newVal) => {
  if (newVal) {
    inputValue.value = ''
    inputError.value = ''
  }
})
</script>

<style scoped>
.confirm-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.confirm-icon {
  flex-shrink: 0;
  font-size: 22px;
  margin-top: 2px;
}

.icon-warning {
  color: #faad14;
}

.icon-danger {
  color: #ff4d4f;
}

.icon-info {
  color: #1890ff;
}

.icon-success {
  color: #52c41a;
}

.icon-question {
  color: #722ed1;
}

.confirm-message {
  flex: 1;
}

.message-text {
  font-size: 16px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
  line-height: 1.5;
}

.message-description {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-bottom: 16px;
}

.message-custom {
  margin: 16px 0;
}

.confirm-input {
  margin-top: 16px;
}

.input-error {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}

.confirm-footer {
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .confirm-content {
    flex-direction: column;
    gap: 8px;
  }
  
  .confirm-icon {
    align-self: center;
    margin-top: 0;
  }
  
  .message-text {
    font-size: 14px;
    text-align: center;
  }
  
  .message-description {
    font-size: 12px;
    text-align: center;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .message-text {
    color: #ffffff;
  }
  
  .message-description {
    color: #bfbfbf;
  }
}

/* 动画效果 */
.confirm-content {
  animation: slideIn 0.2s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>