<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>企业知识管理系统</h1>
        <p>请登录您的账户</p>
      </div>

      <a-form
        :model="loginForm"
        :rules="rules"
        @finish="handleLogin"
        @finishFailed="handleLoginFailed"
        layout="vertical"
        class="login-form"
      >
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix="h(UserOutlined)"
          />
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="loginForm.password"
            placeholder="请输入密码"
            size="large"
            :prefix="h(LockOutlined)"
          />
        </a-form-item>

        <a-form-item>
          <a-checkbox v-model:checked="loginForm.rememberMe"> 记住我 </a-checkbox>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" :loading="loading" block>
            登录
          </a-button>
        </a-form-item>

        <div class="form-footer">
          <span>还没有账户？</span>
          <a @click="goToRegister">立即注册</a>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, h } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import { useAuthStore } from '@/stores/auth';
import type { LoginRequest } from '@/types';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);

const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
  rememberMe: false,
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
};

const handleLogin = async (values: LoginRequest) => {
  try {
    loading.value = true;
    await authStore.login(values);
    message.success('登录成功');
    
    const redirect = router.currentRoute.value.query.redirect as string | undefined;
    if (redirect && redirect !== '/login') {
      router.push(redirect);
    } else {
      router.push({ name: 'Home' });
    }
  } catch (error: Error | unknown) {
    const errorMessage = error instanceof Error ? error.message : '登录失败，请检查用户名和密码';
    message.error(errorMessage);
  } finally {
    loading.value = false;
  }
};

const handleLoginFailed = (errorInfo: { values: LoginRequest; errorFields: Array<{ name: string[]; errors: string[] }> }) => {
  console.log('Failed:', errorInfo);
};

const goToRegister = () => {
  router.push('/register');
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  color: #1890ff;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.login-header p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.login-form {
  margin-top: 24px;
}

.form-footer {
  text-align: center;
  margin-top: 16px;
  color: #666;
}

.form-footer a {
  color: #1890ff;
  margin-left: 4px;
  cursor: pointer;
}

.form-footer a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px;
  }

  .login-header h1 {
    font-size: 20px;
  }
}
</style>