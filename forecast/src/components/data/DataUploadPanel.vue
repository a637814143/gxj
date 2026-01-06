<!--
  @component DataUploadPanel
  @description 数据上传面板 - 支持文件上传和批量导入
  @emits upload-success - 上传成功
-->
<template>
  <el-card class="data-upload-panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div class="panel-title">数据上传</div>
        <el-button size="small" @click="downloadTemplate">下载模板</el-button>
      </div>
    </template>
    
    <el-upload
      ref="uploadRef"
      class="upload-area"
      drag
      :action="uploadUrl"
      :headers="uploadHeaders"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      :file-list="fileList"
      accept=".xlsx,.xls,.csv"
      multiple
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="upload-text">
        <div class="upload-title">将文件拖到此处，或<em>点击上传</em></div>
        <div class="upload-hint">支持 Excel (.xlsx, .xls) 和 CSV 文件，单个文件不超过 10MB</div>
      </div>
    </el-upload>
    
    <el-divider />
    
    <div class="upload-tips">
      <div class="tips-title">📋 上传说明</div>
      <ul class="tips-list">
        <li>请使用提供的模板文件，确保数据格式正确</li>
        <li>必填字段：年份、作物名称、地区名称、产量</li>
        <li>可选字段：播种面积、平均价格、预估收益</li>
        <li>年份格式：YYYY（如 2023）</li>
        <li>数值字段请使用数字，不要包含单位</li>
        <li>支持批量上传多个文件</li>
      </ul>
    </div>
    
    <div v-if="uploadResult" class="upload-result">
      <el-alert
        :type="uploadResult.type"
        :title="uploadResult.title"
        :closable="false"
        show-icon
      >
        <div class="result-details">
          <div v-if="uploadResult.success > 0">成功导入: {{ uploadResult.success }} 条</div>
          <div v-if="uploadResult.failed > 0">失败: {{ uploadResult.failed }} 条</div>
          <div v-if="uploadResult.errors?.length">
            <el-collapse>
              <el-collapse-item title="查看错误详情" name="errors">
                <ul class="error-list">
                  <li v-for="(error, index) in uploadResult.errors" :key="index">
                    {{ error }}
                  </li>
                </ul>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </el-alert>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import apiClient from '../../services/http'

const emit = defineEmits(['upload-success'])

const uploadRef = ref(null)
const fileList = ref([])
const uploadResult = ref(null)

const uploadUrl = computed(() => {
  return `${import.meta.env.VITE_API_BASE_URL || ''}/api/yield-records/upload`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const beforeUpload = (file) => {
  const isValidType = ['application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'text/csv'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只支持 Excel 和 CSV 文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  
  uploadResult.value = null
  return true
}

const handleSuccess = (response, file, fileList) => {
  if (response.success) {
    uploadResult.value = {
      type: 'success',
      title: '上传成功',
      success: response.data?.successCount || 0,
      failed: response.data?.failedCount || 0,
      errors: response.data?.errors || []
    }
    ElMessage.success('数据上传成功!')
    emit('upload-success', response.data)
  } else {
    uploadResult.value = {
      type: 'error',
      title: '上传失败',
      success: 0,
      failed: 0,
      errors: [response.message || '未知错误']
    }
    ElMessage.error(response.message || '上传失败')
  }
}

const handleError = (error, file, fileList) => {
  uploadResult.value = {
    type: 'error',
    title: '上传失败',
    success: 0,
    failed: 0,
    errors: [error.message || '网络错误']
  }
  ElMessage.error('上传失败，请检查网络连接')
}

const downloadTemplate = async () => {
  try {
    const response = await apiClient.get('/api/yield-records/template', {
      responseType: 'blob'
    })
    
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '产量数据导入模板.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('模板下载成功')
  } catch (error) {
    ElMessage.error('模板下载失败')
  }
}
</script>

<style scoped>
.data-upload-panel {
  margin-bottom: 20px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.upload-area {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px 20px;
}

.upload-icon {
  font-size: 67px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.upload-text {
  text-align: center;
}

.upload-title {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.upload-title em {
  color: #409eff;
  font-style: normal;
}

.upload-hint {
  font-size: 13px;
  color: #909399;
}

.upload-tips {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.tips-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.upload-result {
  margin-top: 16px;
}

.result-details {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
}

.error-list {
  margin: 0;
  padding-left: 20px;
  font-size: 12px;
  color: #f56c6c;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
}
</style>
