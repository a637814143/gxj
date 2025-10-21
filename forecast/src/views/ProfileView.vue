<template>
  <div class="profile-page">
    <PageHeader
      badge="账户安全"
      title="修改登录密码"
      description="定期更新密码可以有效降低账号被盗风险，请谨慎保管您的凭据。"
    >
      <template #meta>
        <div class="header-meta">
          <div class="meta-item">
            <div class="meta-label">安全建议</div>
            <div class="meta-value">建议 90 天更新一次密码，并避免与其他系统相同</div>
          </div>
        </div>
      </template>
    </PageHeader>

    <el-card class="password-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">修改密码</div>
            <div class="card-subtitle">输入当前密码与新密码，保存成功后会立即生效。</div>
          </div>
        </div>
      </template>

      <el-alert
        class="tips-alert"
        title="密码至少 8 位，建议同时包含字母、数字与符号的组合。"
        type="info"
        show-icon
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" status-icon class="password-form">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="form.currentPassword"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="至少 8 位，建议包含字母和数字"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-space :size="12">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
            <el-button :disabled="submitting" @click="handleReset">重置</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../components/PageHeader.vue'
import profileService from '../services/profile'

const formRef = ref()
const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码长度不能少于 8 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const submitting = ref(false)

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    submitting.value = true
    try {
      await profileService.updatePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword
      })
      ElMessage.success('密码修改成功')
      handleReset()
    } catch (error) {
      const message = error.response?.data?.message || '密码修改失败，请稍后重试'
      ElMessage.error(message)
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 24px;
}

.header-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  background: rgba(37, 99, 235, 0.08);
  border-radius: 12px;
  padding: 12px 16px;
}

.meta-label {
  font-size: 12px;
  color: #2563eb;
  margin-bottom: 4px;
}

.meta-value {
  font-size: 14px;
  color: #111827;
}

.password-card {
  border-radius: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.card-subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: #6b7280;
}

.tips-alert {
  margin-bottom: 24px;
}

.password-form {
  max-width: 520px;
}

.password-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.password-form :deep(.el-form-item__label) {
  font-weight: 500;
}
</style>
