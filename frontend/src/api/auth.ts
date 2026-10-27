// src/api/auth.ts
import http from '@/utils/http';

// 用户登录 - 根据API.md
export const login = (credentials: { username: string; password: string }) => {
  return http.post('/v1/auth/login', credentials);
};

// 注意：API.md中只有登录接口，没有注册、刷新、登出等接口
// 这些功能可能在其他模块中实现