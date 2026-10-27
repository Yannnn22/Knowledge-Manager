<template>
  <div :class="['loading-container', { 'loading-overlay': overlay }]">
    <div class="loading-content">
      <a-spin :size="size" :spinning="true">
        <template #indicator>
          <LoadingOutlined style="font-size: 24px" spin />
        </template>
      </a-spin>
      <div v-if="text" class="loading-text">{{ text }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { LoadingOutlined } from '@ant-design/icons-vue'

interface Props {
  size?: 'small' | 'default' | 'large'
  text?: string
  overlay?: boolean
}

withDefaults(defineProps<Props>(), {
  size: 'default',
  text: '加载中...',
  overlay: false
})
</script>

<style scoped>
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.loading-container.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  z-index: 9999;
  backdrop-filter: blur(2px);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loading-text {
  color: #666;
  font-size: 14px;
  margin-top: 8px;
}

/* 自定义旋转动画 */
:deep(.ant-spin-dot) {
  font-size: inherit;
}

:deep(.ant-spin-dot-item) {
  background-color: #1890ff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-container {
    padding: 16px;
  }
  
  .loading-text {
    font-size: 12px;
  }
}
</style>