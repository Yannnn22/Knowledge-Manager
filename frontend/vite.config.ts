import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080'
  const useProxy = (env.VITE_USE_PROXY ?? 'true') !== 'false'

  return {
    plugins: [vue()],
    server: {
      port: 3000,
      proxy: useProxy
        ? {
            '/api': {
              target: proxyTarget,
              changeOrigin: true,
              secure: false,
            },
            // 单独为 AI 接口提供直通代理（后端路径为 /ai/**，不带 /api 前缀）
            '/ai': {
              target: proxyTarget,
              changeOrigin: true,
              secure: false,
              proxyTimeout: 300000,
              timeout: 300000,
              ws: true,
            },
            '/ingest': {
              target: proxyTarget,
              changeOrigin: true,
              secure: false,
            },
            '/uploads': {
              target: proxyTarget,
              changeOrigin: true,
              secure: false,
            },
          }
        : undefined,
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  }
})
