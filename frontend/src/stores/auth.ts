// src/stores/auth.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import * as authApi from '@/api/auth';
import { getUserProfile } from '@/api/user';
import type { User } from '@/types';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token') || localStorage.getItem('userToken') || localStorage.getItem('authToken'));
  const user = ref<User | null>(null);
  
  const isAuthenticated = computed(() => !!token.value);
  
  // 添加管理员状态检查
  const isAdmin = computed(() => {
    if (!user.value) return false;
    // 根据后端逻辑，status = 2 表示管理员
    return (user.value as any)?.status === 2 || (user.value as any)?.isAdmin === true;
  });
  
  // 添加用户角色显示名称
  const userRole = computed(() => {
    if (!user.value) return '未知';
    return isAdmin.value ? '管理员' : '普通用户';
  });
  
  const setToken = (newToken: string) => {
    token.value = newToken;
    localStorage.setItem('token', newToken);
    localStorage.setItem('userToken', newToken);
    localStorage.setItem('authToken', newToken);
    sessionStorage.setItem('isLoggedIn', 'true');
    
    if (import.meta.env.DEV) {
      console.log('🔑 Token Set:', {
        tokenLength: newToken.length,
        tokenPreview: `${newToken.substring(0, 10)}...`,
        localStorage: {
          token: localStorage.getItem('token') ? 'exists' : 'missing',
          userToken: localStorage.getItem('userToken') ? 'exists' : 'missing'
        },
        sessionStorage: {
          isLoggedIn: sessionStorage.getItem('isLoggedIn')
        }
      });
    }
  };
  
  const setUser = (userData: User) => {
    user.value = userData;
    // 将关键字段持久化，便于刷新后显示用户名/头像
    try {
      localStorage.setItem('username', (userData as any)?.username || '');
      if ((userData as any)?.realName) localStorage.setItem('realName', (userData as any).realName);
      if ((userData as any)?.department) localStorage.setItem('department', (userData as any).department);
      if ((userData as any)?.avatarUrl) localStorage.setItem('avatarUrl', (userData as any).avatarUrl as string);
    } catch {}
  };
  
  const mergeUserProfile = async () => {
    try {
      const profile = await getUserProfile();
      user.value = {
        ...(user.value || { id: profile.id }),
        id: profile.id,
        realName: profile.realName || user.value?.realName || '',
        email: profile.email || user.value?.email || '',
        department: profile.department || user.value?.department || '',
        avatarUrl: profile.avatarUrl || user.value?.avatarUrl,
        // @ts-ignore 可扩展字段
        isAdmin: (profile as any).isAdmin === true,
        status: (profile as any).status || (user.value as any)?.status || 1, // 添加status字段
        updatedAt: new Date().toISOString(),
        createdAt: user.value?.createdAt || new Date().toISOString(),
        username: (user.value as any)?.username || ''
      } as unknown as User;
      if (import.meta.env.DEV) console.log('👤 Profile merged:', user.value);
    } catch (e) {
      if (import.meta.env.DEV) console.warn('⚠️ Fetch profile failed:', e);
    }
  };
  
  const logout = () => {
    token.value = null;
    user.value = null;
    localStorage.removeItem('token');
    localStorage.removeItem('userToken');
    localStorage.removeItem('authToken');
    sessionStorage.removeItem('isLoggedIn');
  };

  const login = async (payload: { username: string; password: string }) => {
    try {
      const res = await authApi.login({ username: payload.username, password: payload.password });

      if (import.meta.env.DEV) {
        console.log('🔐 Login Response:', {
          responseType: typeof res,
          hasData: res && typeof res === 'object' && 'data' in res,
          responseKeys: res && typeof res === 'object' ? Object.keys(res) : [],
          fullResponse: res
        });
      }

      let extractedToken: string | null = null;
      let extractedUserId: string | number | null = null;

      if (res && typeof res === 'object') {
        if ('data' in res && (res as unknown as Record<string, unknown>).data) {
          const data = (res as unknown as { data: Record<string, unknown> }).data;
          extractedToken = (data.token as string) || (data.accessToken as string) || null;
          extractedUserId = (data.userId as string | number) ?? null;
        } else {
          const plain = res as unknown as Record<string, unknown>;
          extractedToken = (plain.token as string) || (plain.accessToken as string) || null;
          extractedUserId = (plain.userId as string | number) ?? null;
        }
      }

      if (import.meta.env.DEV) {
        console.log('🔑 Extracted Auth Data:', {
          extractedToken: extractedToken ? `${extractedToken.substring(0, 20)}...` : 'null',
          extractedUserId,
          tokenLength: extractedToken?.length || 0
        });
      }

      if (extractedToken) {
        setToken(extractedToken);
        setUser({
          id: (extractedUserId ?? Date.now()).toString(),
          username: payload.username,
          email: '',
          realName: '',
          department: '',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        } as unknown as User);
        sessionStorage.setItem('isLoggedIn', 'true');
        await mergeUserProfile();
        if (import.meta.env.DEV) console.log('✅ Login successful, token stored');
      } else {
        if (import.meta.env.DEV) console.error('❌ No token found in response');
        throw new Error('登录响应中未找到token');
      }
    } catch (error) {
      if (import.meta.env.DEV) console.error('❌ Login error:', error);
      throw error;
    }
  };

  const initAuth = async () => {
    const existing = localStorage.getItem('token') || localStorage.getItem('userToken') || localStorage.getItem('authToken');
    if (import.meta.env.DEV) {
      console.log('🚀 Init Auth:', {
        localStorage: {
          token: localStorage.getItem('token') ? 'exists' : 'missing',
          userToken: localStorage.getItem('userToken') ? 'exists' : 'missing',
          authToken: localStorage.getItem('authToken') ? 'exists' : 'missing'
        },
        sessionStorage: {
          isLoggedIn: sessionStorage.getItem('isLoggedIn')
        },
        existingToken: existing ? `${existing.substring(0, 10)}...` : 'none'
      });
    }
    if (existing) {
      setToken(existing);
      // 先用本地快照恢复基本用户信息（尤其是用户名），避免刷新后显示“用户”
      const snapshot = {
        id: localStorage.getItem('userId') || Date.now().toString(),
        username: localStorage.getItem('username') || '',
        realName: localStorage.getItem('realName') || '',
        email: '',
        department: localStorage.getItem('department') || '',
        avatarUrl: localStorage.getItem('avatarUrl') || undefined,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      } as unknown as User
      if (snapshot.username || snapshot.realName) {
        setUser(snapshot)
      }
      await mergeUserProfile();
    }
  };
  
  return {
    token,
    user,
    isAuthenticated,
    isAdmin,
    userRole,
    setToken,
    setUser,
    logout,
    login,
    initAuth
  };
});