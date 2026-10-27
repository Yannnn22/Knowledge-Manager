import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { message } from 'ant-design-vue';
import { useAuthStore } from '@/stores/auth';

// 创建axios实例
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache',
    'Expires': '0',
  },
});

// 请求拦截器
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 使用 auth store 获取 token，确保响应式更新
    const authStore = useAuthStore();
    const token = authStore.token;

    // 添加调试信息
    if (import.meta.env.DEV) {
      console.log('🔐 Auth Debug:', {
        hasToken: !!token,
        tokenLength: token ? token.length : 0,
        tokenPreview: token ? `${token.substring(0, 10)}...` : 'none',
        url: config.url,
        method: config.method
      });
    }

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const payload = response.data;

    // 兼容两种风格：
    // 1) { code, message, data }
    // 2) 直接返回数据（无 code 包装）
    if (payload && typeof payload === 'object' && 'code' in payload) {
      const { code, message: msg, data } = payload as { code: number; message?: string; data?: unknown };
      
      // 对于登录等认证请求，即使 code 不是 200 也返回完整响应
      // 让调用方决定如何处理
      if (code === 200 || code === 0) {
        return payload;
      }
      
      // 对于其他请求，如果 code 不是成功状态，则拒绝
      // 但保留响应数据，避免丢失错误信息
      const error = new Error(msg || '请求失败');
      (error as any).response = response;
      (error as any).code = code;
      (error as any).data = data;
      return Promise.reject(error);
    }

    return payload;
  },
  (error) => {
    // 开发环境下，将错误降级为警告，避免控制台红色报错
    const suppress = Boolean(error?.config?.headers?.['X-Suppress-Error']);
    if (import.meta.env.DEV) {
      const status = error?.response?.status;
      if (!suppress) {
        if (status === 401 || status === 403 || status === 404 || (status && status >= 500)) {
          console.warn(`API请求失败 (${status}):`, error?.response?.data || error.message);
        } else {
          console.error('Response error:', error);
        }
      }
    } else {
      if (!suppress) console.error('Response error:', error);
    }

    if (error.response) {
      const { status, data } = error.response;

      if (!suppress) {
        switch (status) {
          case 401: {
            // 401 错误时，清除认证状态并跳转到登录页
            const authStore = useAuthStore();
            authStore.logout();
            message.error('登录已过期，请重新登录');
            // 使用 window.location 避免路由问题
            window.location.href = '/login';
            break; }
          case 403:
            message.error('没有权限访问该资源');
            break;
          case 404:
            message.error('请求的资源不存在');
            break;
          case 500:
            message.error('服务器内部错误');
            break;
          default:
            message.error(data?.message || '网络错误');
        }
      }
    } else if (error.request) {
      if (!suppress) message.error('网络连接失败，请检查网络');
    } else {
      if (!suppress) message.error('请求配置错误');
    }

    return Promise.reject(error);
  }
);

export default http;
