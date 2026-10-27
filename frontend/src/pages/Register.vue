<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1>企业知识管理系统</h1>
        <p>创建新用户账户</p>
      </div>

      <a-form
        :model="registerForm"
        :rules="rules"
        @finish="handleRegister"
        @finishFailed="handleRegisterFailed"
        layout="vertical"
        class="register-form"
      >
        <!-- 必填字段 -->
        <div class="form-section">
          <h3 class="section-title">基本信息</h3>
          
          <a-form-item label="用户名" name="username">
            <a-input
              v-model:value="registerForm.username"
              placeholder="请输入用户名（唯一）"
              size="large"
              :prefix="h(UserOutlined)"
            />
          </a-form-item>

          <a-form-item label="密码" name="password">
            <a-input-password
              v-model:value="registerForm.password"
              placeholder="请输入密码"
              size="large"
              :prefix="h(LockOutlined)"
            />
          </a-form-item>

          <a-form-item label="确认密码" name="confirmPassword">
            <a-input-password
              v-model:value="registerForm.confirmPassword"
              placeholder="请再次输入密码"
              size="large"
              :prefix="h(LockOutlined)"
            />
          </a-form-item>

          <a-form-item label="邮箱" name="email">
            <a-input
              v-model:value="registerForm.email"
              placeholder="请输入邮箱地址"
              size="large"
              :prefix="h(MailOutlined)"
            />
          </a-form-item>

          <a-form-item label="真实姓名" name="realName">
            <a-input
              v-model:value="registerForm.realName"
              placeholder="请输入真实姓名"
              size="large"
              :prefix="h(IdcardOutlined)"
            />
          </a-form-item>

          <a-form-item label="部门" name="department">
            <a-select
              v-model:value="registerForm.department"
              placeholder="请选择部门"
              size="large"
            >
              <a-select-option value="技术部">技术部</a-select-option>
              <a-select-option value="产品部">产品部</a-select-option>
              <a-select-option value="设计部">设计部</a-select-option>
              <a-select-option value="运营部">运营部</a-select-option>
              <a-select-option value="市场部">市场部</a-select-option>
              <a-select-option value="人事部">人事部</a-select-option>
              <a-select-option value="财务部">财务部</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <!-- 可选字段 -->
        <div class="form-section">
          <h3 class="section-title">详细信息（可选）</h3>
          
          <a-form-item label="手机号" name="phone">
            <a-input
              v-model:value="registerForm.phone"
              placeholder="请输入手机号"
              size="large"
              :prefix="h(PhoneOutlined)"
            />
          </a-form-item>

          <a-form-item label="个人简介" name="bio">
            <a-textarea
              v-model:value="registerForm.bio"
              placeholder="请输入个人简介"
              :rows="3"
              size="large"
            />
          </a-form-item>

          <a-form-item label="所在地" name="location">
            <a-input
              v-model:value="registerForm.location"
              placeholder="请输入所在地"
              size="large"
              :prefix="h(EnvironmentOutlined)"
            />
          </a-form-item>

          <a-form-item label="个人网站" name="website">
            <a-input
              v-model:value="registerForm.website"
              placeholder="请输入个人网站"
              size="large"
              :prefix="h(LinkOutlined)"
            />
          </a-form-item>

          <a-form-item label="头像URL" name="avatarUrl">
            <a-input
              v-model:value="registerForm.avatarUrl"
              placeholder="请输入头像URL地址"
              size="large"
              :prefix="h(PictureOutlined)"
            />
          </a-form-item>

          <a-form-item label="通知设置">
            <a-space direction="vertical" style="width: 100%">
              <a-checkbox v-model:checked="registerForm.emailNotification">
                接收邮件通知
              </a-checkbox>
              <a-checkbox v-model:checked="registerForm.systemNotification">
                接收系统通知
              </a-checkbox>
            </a-space>
          </a-form-item>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" :loading="loading" block>
            注册
          </a-button>
        </a-form-item>

        <div class="form-footer">
          <span>已有账户？</span>
          <a @click="goToLogin">立即登录</a>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, h } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { 
  UserOutlined, 
  LockOutlined, 
  MailOutlined, 
  IdcardOutlined, 
  PhoneOutlined,
  EnvironmentOutlined,
  LinkOutlined,
  PictureOutlined
} from '@ant-design/icons-vue';
import type { RegisterRequest } from '@/types';
import { registerUser } from '@/api/user';

const router = useRouter();
const loading = ref(false);

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  realName: '',
  department: '',
  phone: '',
  bio: '',
  location: '',
  website: '',
  avatarUrl: '',
  emailNotification: true,
  systemNotification: true,
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: async (_rule: unknown, value: string) => {
        if (value !== registerForm.password) {
          throw new Error('两次输入的密码不一致');
        }
      },
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' },
  ],
  department: [
    { required: true, message: '请选择部门', trigger: 'change' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' },
  ],
  website: [
    { type: 'url', message: '请输入正确的网址格式', trigger: 'blur' },
  ],
};

const handleRegister = async (values: RegisterRequest) => {
  try {
    loading.value = true;
    
    // 构建注册请求数据
    const registerData: RegisterRequest = {
      username: values.username,
      password: values.password,
      email: values.email,
      realName: values.realName,
      department: values.department,
      phone: values.phone || undefined,
      bio: values.bio || undefined,
      location: values.location || undefined,
      website: values.website || undefined,
      avatarUrl: values.avatarUrl || undefined,
      emailNotification: values.emailNotification,
      systemNotification: values.systemNotification,
    };

    console.log('注册数据:', registerData);
    
    // 调用真实注册API
    const response = await registerUser(registerData);
    console.log('注册响应:', response);
    
    // 检查响应格式
    if (response && typeof response === 'object' && 'code' in response) {
      if (response.code === 200) {
        message.success('注册成功！请登录您的账户');
        router.push('/login');
      } else {
        throw new Error(response.message || '注册失败');
      }
    } else {
      message.success('注册成功！请登录您的账户');
      router.push('/login');
    }
    
  } catch (error: unknown) {
    console.error('注册失败:', error);
    
    // 更详细的错误处理
    let errorMessage = '注册失败，请稍后重试';
    if (error instanceof Error) {
      errorMessage = error.message;
    } else if (error && typeof error === 'object') {
      // 处理API错误响应
      const apiError = error as any;
      if (apiError.response?.data?.message) {
        errorMessage = apiError.response.data.message;
      } else if (apiError.message) {
        errorMessage = apiError.message;
      } else {
        errorMessage = JSON.stringify(error);
      }
    }
    
    message.error(errorMessage);
  } finally {
    loading.value = false;
  }
};

const handleRegisterFailed = (errorInfo: { values: RegisterRequest; errorFields: Array<{ name: string[]; errors: string[] }> }) => {
  console.log('Failed:', errorInfo);
};

const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 500px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px;
  max-height: 90vh;
  overflow-y: auto;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h1 {
  color: #1890ff;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.register-header p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.register-form {
  margin-top: 24px;
}

.form-section {
  margin-bottom: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.section-title {
  color: #1890ff;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e6f7ff;
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
  .register-card {
    padding: 24px;
  }

  .register-header h1 {
    font-size: 20px;
  }

  .form-section {
    padding: 16px;
  }
}
</style>
