<template>
  <div class="login-page" :style="loginPageStyle">
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
      <el-alert
        v-if="registrationNotice"
        type="success"
        class="login-alert"
        show-icon
        closable
        @close="registrationNotice = ''"
      >
        {{ registrationNotice }}
      </el-alert>
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
      <div class="login-actions">
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
        <el-link type="primary" class="forgot-link" @click.prevent="openForgotPassword">忘记密码？</el-link>
      </div>
    </div>
  </div>
  <el-dialog
    v-model="forgotDialogVisible"
    width="480px"
    class="forgot-dialog"
    title="找回密码"
    :close-on-click-modal="false"
  >
    <el-form class="forgot-form" label-position="top" @keyup.enter="handleResetPassword">
      <el-form-item label="用户名" :error="forgotErrors.username">
        <el-input v-model.trim="forgotForm.username" placeholder="请输入用户名" autocomplete="username">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="邮箱" :error="forgotErrors.email">
        <el-input v-model.trim="forgotForm.email" placeholder="请输入绑定邮箱" autocomplete="email">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="图形验证码" :error="forgotErrors.captchaCode" class="forgot-captcha-item">
        <div class="forgot-captcha-group">
          <el-input v-model.trim="forgotForm.captchaCode" placeholder="请输入图形验证码" maxlength="8">
            <template #prefix>
              <el-icon><Picture /></el-icon>
            </template>
          </el-input>
          <div class="captcha-image" @click="refreshForgotCaptcha" role="button">
            <img v-if="forgotForm.captchaImage" :src="forgotForm.captchaImage" alt="验证码" />
            <span v-else>点击刷新</span>
          </div>
          <el-button
            class="send-code-button"
            type="primary"
            :loading="forgotCodeSending"
            :disabled="forgotCodeSending || forgotCountdown > 0"
            @click="handleSendForgotCode"
          >
            <template v-if="forgotCountdown > 0">{{ forgotCountdown }}s后重试</template>
            <template v-else>发送验证码</template>
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="邮箱验证码" :error="forgotErrors.emailCode">
        <el-input v-model.trim="forgotForm.emailCode" placeholder="请输入邮箱验证码" maxlength="8">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="新密码" :error="forgotErrors.newPassword">
        <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" show-password autocomplete="new-password">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
        <div class="password-strength">
          <span class="strength-label">密码强度：</span>
          <div class="strength-bars">
            <span
              v-for="n in 3"
              :key="n"
              :class="[
                'strength-bar',
                {
                  active: n <= forgotPasswordStrength.score,
                  'level-weak': n <= forgotPasswordStrength.score && forgotPasswordStrength.level === 'weak',
                  'level-medium': n <= forgotPasswordStrength.score && forgotPasswordStrength.level === 'medium',
                  'level-strong': n <= forgotPasswordStrength.score && forgotPasswordStrength.level === 'strong'
                }
              ]"
            />
          </div>
          <span class="strength-text" :class="`level-${forgotPasswordStrength.level}`">
            {{ forgotPasswordStrength.label }}
          </span>
        </div>
      </el-form-item>
      <el-form-item label="确认新密码" :error="forgotErrors.confirmPassword">
        <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password autocomplete="new-password">
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <div class="forgot-footer">
        <el-button @click="forgotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotResetting" @click="handleResetPassword">确认重置</el-button>
      </div>
      <p class="forgot-hint">提交后系统会立即更新密码，并记录一次密码重置日志。</p>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Picture, Message } from '@element-plus/icons-vue'
import apiClient from '../services/http'
import { useAuthStore } from '../stores/auth'
import { getPasswordStrength, validatePasswordPolicy } from '../utils/password'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginMode = ref(route.query.mode === 'user' ? 'user' : 'admin')

const loginBackgroundImage = new URL('../../beijin.jpg', import.meta.url).href

const loginPageStyle = computed(() => ({
  backgroundImage: `linear-gradient(135deg, rgba(11, 61, 46, 0.85), rgba(30, 111, 92, 0.85)), url(${loginBackgroundImage})`
}))

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
  rememberMe: false
})

const captchaId = ref('')
const captchaImage = ref('')
const loading = ref(false)
const registrationNotice = ref('')

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

const handleRegistrationNotice = () => {
  if (!route.query.registered) {
    return
  }
  const message = route.query.registrationMessage || '注册成功，请使用账号登录'
  registrationNotice.value = message
  ElMessage.success(message)
  const nextQuery = { ...route.query }
  delete nextQuery.registered
  delete nextQuery.registrationMessage
  router.replace({ name: route.name || 'login', query: nextQuery }).catch(() => {})
}

watch(
  () => route.query.registered,
  () => {
    handleRegistrationNotice()
  },
  { immediate: true }
)

const forgotDialogVisible = ref(false)
const forgotForm = reactive({
  username: '',
  email: '',
  captchaId: '',
  captchaImage: '',
  captchaCode: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: ''
})
const forgotErrors = reactive({
  username: '',
  email: '',
  captchaCode: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: ''
})
const forgotCodeSending = ref(false)
const forgotCountdown = ref(0)
const forgotResetting = ref(false)
let forgotTimer = null

const forgotPasswordStrength = computed(() => getPasswordStrength(forgotForm.newPassword))

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

const resetForgotErrors = () => {
  forgotErrors.username = ''
  forgotErrors.email = ''
  forgotErrors.captchaCode = ''
  forgotErrors.emailCode = ''
  forgotErrors.newPassword = ''
  forgotErrors.confirmPassword = ''
}

const clearForgotCountdown = () => {
  if (forgotTimer) {
    window.clearInterval(forgotTimer)
    forgotTimer = null
  }
}

const startForgotCountdown = seconds => {
  clearForgotCountdown()
  forgotCountdown.value = seconds
  forgotTimer = window.setInterval(() => {
    if (forgotCountdown.value > 0) {
      forgotCountdown.value -= 1
    }
    if (forgotCountdown.value <= 0) {
      clearForgotCountdown()
    }
  }, 1000)
}

const refreshForgotCaptcha = async () => {
  try {
    const { data } = await apiClient.get('/api/auth/captcha', { params: { ts: Date.now() } })
    const payload = data?.data
    forgotForm.captchaId = payload?.captchaId || ''
    forgotForm.captchaImage = payload?.imageBase64 || ''
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取图形验证码失败')
  }
}

const openForgotPassword = async () => {
  resetForgotErrors()
  forgotForm.username = form.username || ''
  forgotForm.captchaCode = ''
  forgotForm.emailCode = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  forgotDialogVisible.value = true
  await refreshForgotCaptcha()
}

const handleSendForgotCode = async () => {
  if (forgotCodeSending.value || forgotCountdown.value > 0) {
    return
  }
  resetForgotErrors()
  if (!forgotForm.username) {
    forgotErrors.username = '请输入用户名'
  }
  if (!forgotForm.email) {
    forgotErrors.email = '请输入邮箱'
  } else if (!/^\S+@\S+\.\S+$/.test(forgotForm.email)) {
    forgotErrors.email = '请输入正确的邮箱地址'
  }
  if (!forgotForm.captchaCode) {
    forgotErrors.captchaCode = '请输入图形验证码'
  }
  if (forgotErrors.username || forgotErrors.email || forgotErrors.captchaCode) {
    if (!forgotForm.captchaId) {
      await refreshForgotCaptcha()
    }
    return
  }
  if (!forgotForm.captchaId) {
    await refreshForgotCaptcha()
    if (!forgotForm.captchaId) {
      return
    }
  }
  forgotCodeSending.value = true
  try {
    await apiClient.post('/api/auth/password/reset-code', {
      username: forgotForm.username,
      email: forgotForm.email,
      captchaId: forgotForm.captchaId,
      captchaCode: forgotForm.captchaCode
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startForgotCountdown(60)
    forgotForm.captchaCode = ''
    await refreshForgotCaptcha()
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '发送验证码失败，请稍后再试'
    ElMessage.error(message)
    await refreshForgotCaptcha()
  } finally {
    forgotCodeSending.value = false
  }
}

const handleResetPassword = async () => {
  resetForgotErrors()
  if (!forgotForm.username) {
    forgotErrors.username = '请输入用户名'
  }
  if (!forgotForm.email) {
    forgotErrors.email = '请输入邮箱'
  } else if (!/^\S+@\S+\.\S+$/.test(forgotForm.email)) {
    forgotErrors.email = '请输入正确的邮箱地址'
  }
  if (!forgotForm.emailCode) {
    forgotErrors.emailCode = '请输入邮箱验证码'
  }
  const passwordError = validatePasswordPolicy(forgotForm.newPassword)
  if (passwordError) {
    forgotErrors.newPassword = passwordError
  }
  if (!forgotForm.confirmPassword) {
    forgotErrors.confirmPassword = '请确认新密码'
  } else if (forgotForm.confirmPassword !== forgotForm.newPassword) {
    forgotErrors.confirmPassword = '两次输入的密码不一致'
  }
  if (
    forgotErrors.username ||
    forgotErrors.email ||
    forgotErrors.emailCode ||
    forgotErrors.newPassword ||
    forgotErrors.confirmPassword
  ) {
    return
  }
  forgotResetting.value = true
  try {
    await apiClient.post('/api/auth/password/reset', {
      username: forgotForm.username,
      email: forgotForm.email,
      emailCode: forgotForm.emailCode,
      newPassword: forgotForm.newPassword
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    form.username = forgotForm.username
    form.password = forgotForm.newPassword
    forgotDialogVisible.value = false
    await refreshCaptcha()
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '重置密码失败，请稍后再试'
    ElMessage.error(message)
  } finally {
    forgotResetting.value = false
  }
}

watch(forgotDialogVisible, visible => {
  if (!visible) {
    clearForgotCountdown()
    forgotForm.captchaCode = ''
    forgotForm.captchaId = ''
    forgotForm.captchaImage = ''
    forgotForm.emailCode = ''
    forgotForm.newPassword = ''
    forgotForm.confirmPassword = ''
    resetForgotErrors()
  }
})

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

onBeforeUnmount(() => {
  clearForgotCountdown()
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
  background-color: #0b3d2e;
  background-size: 100% 100%, cover;
  background-position: center, center;
  background-repeat: no-repeat, no-repeat;
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

.login-alert {
  margin: 0 0 16px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.captcha-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-item .el-input {
  flex: 1;
}

.captcha-image {
  box-sizing: border-box;
  flex-shrink: 0;
  width: 160px;
  height: 40px;
  padding: 0;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #f7f9fb;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.captcha-image:hover {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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

.login-actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #607d8b;
  font-size: 13px;
}

.switch-auth {
  margin: 0;
  text-align: left;
  color: inherit;
}

.switch-auth .el-link {
  margin-left: 4px;
}

.forgot-link {
  white-space: nowrap;
}

.forgot-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}

.forgot-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.forgot-captcha-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.forgot-captcha-group .el-input {
  flex: 1;
}

.send-code-button {
  white-space: nowrap;
}

.forgot-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.forgot-hint {
  margin: 0;
  text-align: right;
  font-size: 12px;
  color: #909399;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: #607d8b;
}

.password-strength .strength-label {
  flex-shrink: 0;
}

.strength-bars {
  display: flex;
  gap: 4px;
}

.strength-bar {
  width: 32px;
  height: 4px;
  border-radius: 2px;
  background-color: #dcdfe6;
  transition: background-color 0.3s ease;
}

.strength-bar.active.level-weak {
  background-color: #f56c6c;
}

.strength-bar.active.level-medium {
  background-color: #e6a23c;
}

.strength-bar.active.level-strong {
  background-color: #67c23a;
}

.strength-text {
  font-weight: 600;
}

.strength-text.level-weak {
  color: #f56c6c;
}

.strength-text.level-medium {
  color: #e6a23c;
}

.strength-text.level-strong {
  color: #67c23a;
}

.strength-text.level-none {
  color: #909399;
}
</style>
