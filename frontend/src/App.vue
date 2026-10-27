<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// 应用启动时检查本地存储的token
onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    authStore.setToken(token)
    // 这里可以添加获取用户信息的逻辑
  }
})
</script>

<style>
#app {
  min-height: 100vh;
  background-color: #f8f9fc;
}
</style>