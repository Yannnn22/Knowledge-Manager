<template>
  <div class="auth-debug" v-if="showDebug">
    <div class="debug-header" @click="toggleExpanded">
      <span>🔐 Auth Debug</span>
      <span class="toggle-icon">{{ expanded ? '▼' : '▶' }}</span>
    </div>
    
    <div class="debug-content" v-if="expanded">
      <div class="debug-section">
        <h4>Store State:</h4>
        <ul>
          <li>Token: {{ authStore.token ? `${authStore.token.substring(0, 20)}...` : 'null' }}</li>
          <li>User: {{ authStore.user?.username || 'null' }}</li>
          <li>Authenticated: {{ authStore.isAuthenticated }}</li>
        </ul>
      </div>
      
      <div class="debug-section">
        <h4>Local Storage:</h4>
        <ul>
          <li>token: {{ getLocalStorageItem('token') ? 'exists' : 'missing' }}</li>
          <li>userToken: {{ getLocalStorageItem('userToken') ? 'exists' : 'missing' }}</li>
          <li>authToken: {{ getLocalStorageItem('authToken') ? 'exists' : 'missing' }}</li>
          <li>isLoggedIn: {{ getSessionStorageItem('isLoggedIn') }}</li>
        </ul>
      </div>
      
      <div class="debug-section">
        <h4>Actions:</h4>
        <button @click="clearAuth" class="debug-btn">Clear Auth</button>
        <button @click="testRequest" class="debug-btn" :disabled="!authStore.token">Test Request</button>
        <button @click="refreshAuth" class="debug-btn">Refresh Auth</button>
      </div>
      
      <div class="debug-section" v-if="testResult">
        <h4>Test Result:</h4>
        <pre>{{ testResult }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const expanded = ref(false);
const testResult = ref('');

// 只在开发环境显示
const showDebug = import.meta.env.DEV;

// 安全的存储访问函数
const getLocalStorageItem = (key: string) => {
  try {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(key) : null;
  } catch {
    return null;
  }
};

const getSessionStorageItem = (key: string) => {
  try {
    return typeof sessionStorage !== 'undefined' ? sessionStorage.getItem(key) : null;
  } catch {
    return null;
  }
};

const toggleExpanded = () => {
  expanded.value = !expanded.value;
};

const clearAuth = () => {
  authStore.logout();
  testResult.value = 'Auth cleared';
};

const refreshAuth = () => {
  authStore.initAuth();
  testResult.value = 'Auth refreshed';
};

const testRequest = async () => {
  try {
    const response = await fetch('/api/v1/knowledge-bases?page=1&size=5', {
      headers: {
        'Authorization': `Bearer ${authStore.token}`,
        'Content-Type': 'application/json'
      }
    });
    
    const data = await response.json();
    testResult.value = JSON.stringify({
      status: response.status,
      statusText: response.statusText,
      data: data
    }, null, 2);
  } catch (error) {
    testResult.value = `Request failed: ${error}`;
  }
};

onMounted(() => {
  // 自动展开调试面板
  expanded.value = true;
});
</script>

<style scoped>
.auth-debug {
  position: fixed;
  top: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  border-radius: 8px;
  font-family: monospace;
  font-size: 12px;
  z-index: 9999;
  max-width: 300px;
}

.debug-header {
  padding: 10px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.toggle-icon {
  font-size: 10px;
}

.debug-content {
  padding: 10px;
}

.debug-section {
  margin-bottom: 15px;
}

.debug-section h4 {
  margin: 0 0 5px 0;
  color: #ffd700;
}

.debug-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.debug-section li {
  margin: 2px 0;
  padding: 2px 0;
}

.debug-btn {
  background: #333;
  color: white;
  border: 1px solid #666;
  padding: 4px 8px;
  margin: 2px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
}

.debug-btn:hover {
  background: #555;
}

.debug-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

pre {
  background: rgba(255, 255, 255, 0.1);
  padding: 5px;
  border-radius: 4px;
  margin: 5px 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 10px;
}
</style>
