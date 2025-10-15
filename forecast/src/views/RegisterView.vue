<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <h1>创建新账户</h1>
        <p>填写以下信息以加入农作物产量预测平台</p>
      </div>
      <el-form class="register-form" :model="form" label-position="top" @keyup.enter="handleSubmit">
        <div class="form-grid">
          <el-form-item label="用户名" :error="errors.username">
            <el-input
              v-model.trim="form.username"
              placeholder="请输入用户名"
              autocomplete="username"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" :error="errors.password">
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              type="password"
              autocomplete="new-password"
              show-password
              clearable
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="确认密码" :error="errors.confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              placeholder="请再次输入密码"
              type="password"
              autocomplete="new-password"
              show-password
              clearable
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="姓名">
            <el-input
              v-model.trim="form.fullName"
              placeholder="请输入姓名（选填）"
              autocomplete="name"
              clearable
            >
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="邮箱" :error="errors.email">
            <el-input
              v-model.trim="form.email"
              placeholder="请输入邮箱（选填）"
              autocomplete="email"
              clearable
            >
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="用户类型">
            <el-select v-model="form.roleCode" placeholder="请选择用户类型">
              <el-option
                v-for="role in roles"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-footer">
          <el-checkbox v-model="form.rememberMe">保持登录状态</el-checkbox>
          <el-button type="primary" :loading="loading" @click="handleSubmit" class="register-button">
            创建账户
          </el-button>
        </div>
        <div class="switch-auth">
          已有账户？
          <el-link type="primary" @click.prevent="goToLogin">返回登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const roles = [
  { label: '企业/农户用户', value: 'FARMER' },
  { label: '农业部门用户', value: 'AGRICULTURE_DEPT' }
]

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  fullName: '',
  email: '',
  roleCode: roles[0].value,
  rememberMe: true
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const loading = ref(false)

const validateForm = () => {
  errors.username = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.email = ''

  if (!form.username) {
    errors.username = '请输入用户名'
  }
  if (!form.password) {
    errors.password = '请输入密码'
  } else if (form.password.length < 6) {
    errors.password = '密码长度至少为6位'
  }
  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码'
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
  }
  if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) {
    errors.email = '请输入正确的邮箱地址'
  }

  return !errors.username && !errors.password && !errors.confirmPassword && !errors.email
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }
  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
      fullName: form.fullName || undefined,
      email: form.email || undefined,
      roleCode: form.roleCode,
      rememberMe: form.rememberMe
    })
    const redirect = route.query.redirect || '/dashboard'
    await router.replace(redirect)
    ElMessage.success('注册成功，已自动登录')
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '注册失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  const redirect = route.query.redirect
  router.push({ name: 'login', ...(redirect ? { query: { redirect } } : {}) })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 640px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 24px 48px rgba(30, 60, 114, 0.2);
  padding: 36px;
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.register-header h1 {
  margin: 0;
  font-size: 26px;
  color: #1e3c72;
}

.register-header p {
  margin: 8px 0 0;
  color: #607d8b;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.register-button {
  min-width: 140px;
}

.switch-auth {
  text-align: center;
  color: #607d8b;
}

.switch-auth .el-link {
  margin-left: 6px;
}
</style>
