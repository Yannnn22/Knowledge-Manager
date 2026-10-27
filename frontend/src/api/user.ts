import http from '@/utils/http';
import type { RegisterRequest, User, ApiResponse } from '@/types';

// 注册（测试）
export const registerUser = async (data: RegisterRequest): Promise<ApiResponse<User>> => {
  return http.post('/v1/test/create-user', data);
};

// 注册（正式，需要JWT）
export const registerUserWithAuth = async (data: RegisterRequest, token: string): Promise<ApiResponse<User>> => {
  return http.post('/v1/users', data, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
  });
};

// 用户分页查询 - 根据API.md
export const getUsers = async (params: { page?: number; size?: number; department?: string; status?: number }) => {
  return http.get('/v1/users/page', { params });
};

// 更新用户 - 根据API.md
export const updateUser = async (id: number, data: { email?: string; realName?: string; department?: string; status: number }) => {
  return http.put(`/v1/users/${id}`, data);
};

// 删除用户 - 根据API.md
export const deleteUser = async (id: number) => {
  return http.delete(`/v1/users/${id}`);
};

export interface UserProfileDTO {
  id: string
  username?: string
  realName?: string
  email?: string
  department?: string
  avatarUrl?: string
  phone?: string
  bio?: string
  location?: string
  website?: string
  notificationEmail?: boolean
  notificationSystem?: boolean
  isAdmin?: boolean
  preferredModel?: string
  customApiKey?: string
}

export const getUserProfile = async (): Promise<UserProfileDTO> => {
  const res: any = await http.get('/v1/users/profile')
  return (res?.data as UserProfileDTO) || (res as UserProfileDTO)
}

// 更新当前用户个人资料 - 根据API.md
export const updateUserProfile = async (data: any) => {
  return http.put('/v1/users/profile', data);
};

// 修改密码 - 根据API.md
export const changePassword = async (data: { currentPassword: string; newPassword: string; confirmPassword: string }) => {
  return http.put('/v1/users/password', data);
};

// 注意：API.md中没有用户统计和头像上传接口，这些可能需要后端实现

// 上传头像（multipart/form-data），返回可访问的 URL
export const uploadAvatar = async (file: File): Promise<string> => {
  const form = new FormData();
  form.append('file', file);
  const res: any = await http.post('/v1/users/profile/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  // 兼容返回 { code, data } 或直接返回字符串
  const url = (res?.data && (res.data.url || res.data)) || res?.url || res;
  return String(url);
};
