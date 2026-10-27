<template>
  <div class="document-upload">
    <a-upload-dragger
      v-model:fileList="fileList"
      :before-upload="beforeUpload"
      :multiple="true"
      accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
    >
      <p class="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
      <p class="ant-upload-hint">
        支持 PDF、Word、Excel、PowerPoint、TXT 等格式
      </p>
    </a-upload-dragger>

    <div class="upload-form" style="margin-top: 20px;">
      <a-form :model="uploadForm" layout="vertical">
        <a-form-item label="文档名称">
          <a-input v-model:value="uploadForm.name" placeholder="请输入文档名称" />
        </a-form-item>
        
        <a-form-item label="所属知识库">
          <a-select v-model:value="uploadForm.knowledgeBase" placeholder="请选择知识库">
            <a-select-option v-for="kb in knowledgeBases" :key="kb.id" :value="kb.id">
              {{ kb.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="文档描述">
          <a-textarea 
            v-model:value="uploadForm.description" 
            :rows="3"
            placeholder="请输入文档描述..."
          />
        </a-form-item>
        
        <a-form-item label="访问权限">
          <a-select v-model:value="uploadForm.accessLevel" placeholder="请选择访问权限">
            <a-select-option value="public">公开</a-select-option>
            <a-select-option value="team">团队内部</a-select-option>
            <a-select-option value="private">私有</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </div>

    <div class="upload-actions" style="margin-top: 20px; text-align: right;">
      <a-button @click="$emit('cancel')" style="margin-right: 10px;">取消</a-button>
      <a-button 
        type="primary" 
        @click="handleUpload"
        :loading="uploading"
        :disabled="fileList.length === 0"
      >
        上传
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { InboxOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue';

interface KnowledgeBase {
  id: string;
  name: string;
}

interface UploadForm {
  name: string;
  knowledgeBase: string;
  description: string;
  accessLevel: string;
}

const emit = defineEmits<{
  success: [];
  cancel: [];
}>();

const fileList = ref<UploadFile[]>([]);
const knowledgeBases = ref<KnowledgeBase[]>([]);
const uploading = ref(false);

const uploadForm = ref<UploadForm>({
  name: '',
  knowledgeBase: '',
  description: '',
  accessLevel: 'team'
});

const beforeUpload = (file: File) => {
  const isValidType = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'text/plain'
  ].includes(file.type);

  if (!isValidType) {
    message.error('不支持的文件格式！');
    return false;
  }

  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB！');
    return false;
  }

  return false; // 阻止自动上传
};

const loadKnowledgeBases = async () => {
  // 模拟加载知识库列表
  knowledgeBases.value = [
    { id: '1', name: '产品设计规范' },
    { id: '2', name: '技术开发文档' },
    { id: '3', name: '市场营销资料' }
  ];
};

const handleUpload = async () => {
  if (fileList.value.length === 0) {
    message.error('请选择要上传的文件');
    return;
  }

  if (!uploadForm.value.name.trim()) {
    message.error('请输入文档名称');
    return;
  }

  if (!uploadForm.value.knowledgeBase) {
    message.error('请选择所属知识库');
    return;
  }

  uploading.value = true;

  try {
    // 模拟上传过程
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    message.success('文档上传成功');
    emit('success');
  } catch (error) {
    message.error('文档上传失败');
  } finally {
    uploading.value = false;
  }
};

onMounted(() => {
  loadKnowledgeBases();
});
</script>

<style scoped>
.document-upload {
  padding: 20px;
}

.upload-form {
  background: #fafafa;
  padding: 20px;
  border-radius: 6px;
}

.upload-actions {
  border-top: 1px solid #e8e8e8;
  padding-top: 20px;
}
</style>
