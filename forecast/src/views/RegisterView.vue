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
              ref="passwordInputRef"
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
            <div class="password-strength">
              <span class="strength-label">密码强度：</span>
              <div class="strength-bars">
                <span
                  v-for="n in 3"
                  :key="n"
                  :class="[
                    'strength-bar',
                    {
                      active: n <= passwordStrength.score,
                      'level-weak': n <= passwordStrength.score && passwordStrength.level === 'weak',
                      'level-medium': n <= passwordStrength.score && passwordStrength.level === 'medium',
                      'level-strong': n <= passwordStrength.score && passwordStrength.level === 'strong'
                    }
                  ]"
                />
              </div>
              <span class="strength-text" :class="`level-${passwordStrength.level}`">
                {{ passwordStrength.label }}
              </span>
            </div>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="确认密码" :error="errors.confirmPassword">
            <el-input
              ref="confirmPasswordInputRef"
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
          <el-form-item label="邮箱" :error="errors.email" class="email-item">
            <div class="email-input-group">
              <el-input
                v-model.trim="form.email"
                placeholder="请输入邮箱"
                autocomplete="email"
                clearable
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
              <el-button
                class="send-code-button"
                type="primary"
                :loading="codeSending"
                :disabled="codeSending || sendCountdown > 0"
                @click="handleSendCode"
              >
                <template v-if="sendCountdown > 0">{{ sendCountdown }}s后重试</template>
                <template v-else>获取验证码</template>
              </el-button>
            </div>
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
        <div class="form-grid">
          <el-form-item label="图形验证码" :error="errors.captchaCode" class="captcha-item">
            <el-input v-model.trim="form.captchaCode" placeholder="请输入图形验证码" maxlength="8">
              <template #prefix>
                <el-icon><Picture /></el-icon>
              </template>
            </el-input>
            <div class="captcha-image" @click="refreshCaptcha" role="button">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else>点击刷新</span>
            </div>
          </el-form-item>
          <el-form-item label="邮箱验证码" :error="errors.emailCode">
            <el-input
              v-model.trim="form.emailCode"
              placeholder="请输入邮箱验证码"
              maxlength="8"
              clearable
            >
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled, Message, Picture } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import apiClient from '../services/http'
import { getPasswordStrength, validatePasswordPolicy } from '../utils/password'

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
  emailCode: '',
  captchaCode: '',
  roleCode: roles[0].value,
  rememberMe: true
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  emailCode: '',
  captchaCode: ''
})

const loading = ref(false)
const codeSending = ref(false)
const sendCountdown = ref(0)
const captchaId = ref('')
const captchaImage = ref('')
let countdownTimer = null

const passwordInputRef = ref(null)
const confirmPasswordInputRef = ref(null)
const clipboardTargets = new Set()

const handlePasswordClipboard = event => {
  if (event && typeof event.preventDefault === 'function') {
    event.preventDefault()
  }
  ElMessage.warning('密码不支持复制或粘贴')
}

const attachClipboardGuards = inputComponent => {
  const inputEl = inputComponent?.input
  if (!inputEl || clipboardTargets.has(inputEl)) {
    return
  }
  inputEl.addEventListener('paste', handlePasswordClipboard)
  inputEl.addEventListener('copy', handlePasswordClipboard)
  inputEl.addEventListener('cut', handlePasswordClipboard)
  clipboardTargets.add(inputEl)
}

const detachClipboardGuards = () => {
  clipboardTargets.forEach(target => {
    target.removeEventListener('paste', handlePasswordClipboard)
    target.removeEventListener('copy', handlePasswordClipboard)
    target.removeEventListener('cut', handlePasswordClipboard)
  })
  clipboardTargets.clear()
}

const passwordStrength = computed(() => getPasswordStrength(form.password))

const clearCountdown = () => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const startCountdown = seconds => {
  clearCountdown()
  sendCountdown.value = seconds
  countdownTimer = window.setInterval(() => {
    if (sendCountdown.value > 0) {
      sendCountdown.value -= 1
    }
    if (sendCountdown.value <= 0) {
      clearCountdown()
    }
  }, 1000)
}

const refreshCaptcha = async () => {
  try {
    const { data } = await apiClient.get('/api/auth/captcha', { params: { ts: Date.now() } })
    const payload = data?.data
    captchaId.value = payload?.captchaId || ''
    captchaImage.value = payload?.imageBase64 || ''
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取图形验证码失败')
  }
}

const validateForm = () => {
  errors.username = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.email = ''
  errors.emailCode = ''
  errors.captchaCode = ''

  if (!form.username) {
    errors.username = '请输入用户名'
  }
  const passwordError = validatePasswordPolicy(form.password)
  if (passwordError) {
    errors.password = passwordError
  }
  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码'
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
  }
  if (!form.email) {
    errors.email = '请输入邮箱'
  } else if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    errors.email = '请输入正确的邮箱地址'
  }
  if (!form.emailCode) {
    errors.emailCode = '请输入邮箱验证码'
  }

  return (
    !errors.username &&
    !errors.password &&
    !errors.confirmPassword &&
    !errors.email &&
    !errors.emailCode
  )
}

const validateEmailForCode = () => {
  errors.email = ''
  errors.captchaCode = ''

  if (!form.email) {
    errors.email = '请输入邮箱'
    return false
  }
  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    errors.email = '请输入正确的邮箱地址'
    return false
  }
  if (!form.captchaCode) {
    errors.captchaCode = '请输入图形验证码'
    return false
  }
  return true
}

const handleSendCode = async () => {
  if (sendCountdown.value > 0 || codeSending.value) {
    return
  }
  if (!validateEmailForCode()) {
    if (!captchaId.value) {
      await refreshCaptcha()
    }
    return
  }

  if (!captchaId.value) {
    await refreshCaptcha()
    if (!captchaId.value) {
      return
    }
  }

  codeSending.value = true
  try {
    await apiClient.post('/api/auth/email-code', {
      email: form.email,
      captchaCode: form.captchaCode,
      captchaId: captchaId.value
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown(60)
    form.captchaCode = ''
    await refreshCaptcha()
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '发送验证码失败，请稍后再试'
    ElMessage.error(message)
    await refreshCaptcha()
  } finally {
    codeSending.value = false
  }
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
      emailCode: form.emailCode,
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

onMounted(() => {
  refreshCaptcha()
  nextTick(() => {
    if (passwordInputRef.value) {
      attachClipboardGuards(passwordInputRef.value)
    }
    if (confirmPasswordInputRef.value) {
      attachClipboardGuards(confirmPasswordInputRef.value)
    }
  })
})

onBeforeUnmount(() => {
  clearCountdown()
  detachClipboardGuards()
})
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

.email-item .email-input-group {
  display: flex;
  gap: 12px;
}

.send-code-button {
  white-space: nowrap;
}

.captcha-item {
  display: flex;
  gap: 12px;
}

.captcha-item .el-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  background-color: #f5f7fa;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
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
