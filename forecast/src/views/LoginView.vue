<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-toggle">
        <el-radio-group v-model="loginMode" size="large" class="login-toggle-group">
          <el-radio-button label="admin">管理员登录</el-radio-button>
          <el-radio-button label="user">用户登录</el-radio-button>
        </el-radio-group>
      </div>
      <div class="login-header">
        <h1>农作物产量预测平台</h1>
        <p>{{ loginSubtitle }}</p>
      </div>
      <el-form :model="form" class="login-form" @keyup.enter="handleSubmit">
        <el-form-item>
          <el-input v-model.trim="form.username" placeholder="用户名" autocomplete="username">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            placeholder="密码"
            type="password"
            autocomplete="current-password"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item class="captcha-item">
          <el-input v-model.trim="form.captchaCode" placeholder="验证码">
            <template #prefix>
              <el-icon><Picture /></el-icon>
            </template>
          </el-input>
          <div class="captcha-image" @click="refreshCaptcha" role="button">
            <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
            <span v-else>点击刷新</span>
          </div>
        </el-form-item>
        <div class="form-footer">
          <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
          <el-button type="primary" :loading="loading" @click="handleSubmit" class="login-button">登录</el-button>
        </div>
      </el-form>
      <p class="login-mode-hint">{{ loginModeHint }}</p>
      <div class="switch-auth">
        <template v-if="loginMode === 'admin'">
          普通用户入口？
          <el-link type="primary" @click.prevent="switchToMode('user')">切换至用户登录</el-link>
        </template>
        <template v-else>
          还没有账号？
          <el-link type="primary" @click.prevent="goToRegister">立即注册</el-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Picture } from '@element-plus/icons-vue'
import apiClient from '../services/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginMode = ref(route.query.mode === 'user' ? 'user' : 'admin')

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
  rememberMe: false
})

const captchaId = ref('')
const captchaImage = ref('')
const loading = ref(false)

const loginSubtitle = computed(() =>
  loginMode.value === 'admin'
    ? '系统管理员与农业部门账号请从此入口登录'
    : '企业/农户等普通用户请从此入口登录'
)

const loginModeHint = computed(() =>
  loginMode.value === 'admin'
    ? '管理员登录后可使用数据管理、预测建模、系统维护等高级功能'
    : '普通用户登录后可查看预测结果、报告中心和个人资料等功能'
)

const refreshCaptcha = async () => {
  try {
    const { data } = await apiClient.get('/api/auth/captcha', { params: { ts: Date.now() } })
    const payload = data?.data
    captchaId.value = payload?.captchaId || ''
    captchaImage.value = payload?.imageBase64 || ''
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取验证码失败')
  }
}

const handleSubmit = async () => {
  if (!form.username || !form.password || !form.captchaCode) {
    ElMessage.warning('请完整填写用户名、密码和验证码')
    return
  }
  if (!captchaId.value) {
    await refreshCaptcha()
  }
  loading.value = true
  try {
    await authStore.login({
      username: form.username,
      password: form.password,
      captchaCode: form.captchaCode,
      captchaId: captchaId.value,
      rememberMe: form.rememberMe,
      loginMode: loginMode.value
    })
    const redirect = route.query.redirect || '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '登录失败，请重试'
    ElMessage.error(message)
    await refreshCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})

const goToRegister = () => {
  const redirect = route.query.redirect
  router.push({ name: 'register', ...(redirect ? { query: { redirect } } : {}) })
}

const switchToMode = mode => {
  loginMode.value = mode
  const nextQuery = { ...route.query }
  if (mode === 'admin') {
    delete nextQuery.mode
  } else {
    nextQuery.mode = mode
  }
  router.replace({ name: 'login', query: nextQuery }).catch(() => {})
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0b3d2e 0%, #1e6f5c 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 48px rgba(11, 61, 46, 0.25);
  padding: 32px;
}

.login-toggle {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.login-toggle-group {
  width: 100%;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h1 {
  margin: 0;
  font-size: 24px;
  color: #0b3d2e;
}

.login-header p {
  margin: 8px 0 0;
  color: #607d8b;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.captcha-item {
  display: flex;
  gap: 12px;
}

.captcha-item .el-input {
  flex: 1;
}

.captcha-image {
  width: 140px;
  height: 44px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-button {
  min-width: 120px;
}

.login-mode-hint {
  margin: 16px 0 0;
  color: #607d8b;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.switch-auth {
  margin-top: 16px;
  text-align: center;
  color: #607d8b;
}

.switch-auth .el-link {
  margin-left: 4px;
}
</style>
