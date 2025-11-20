<template>
  <div class="personal-center-page">
    <el-card class="overview-card" shadow="hover" v-loading="loadingProfile">
      <div class="overview-content">
        <div class="overview-main">
          <div class="overview-badge">个人中心</div>
          <h1 class="overview-title">管理我的资料与账号安全</h1>
          <p class="overview-desc">
            查看并更新个人基础信息，确保联系方式保持最新，同时定期修改密码以提升账号安全性。
          </p>
          <el-descriptions v-if="profile" :column="1" class="overview-descriptions" border>
            <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">
              {{ profile.fullName || '未填写' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ profile.email || '未绑定' }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-space wrap>
                <el-tag v-for="role in profile.roles" :key="role.id || role.code" type="success" effect="dark">
                  {{ formatRole(role) }}
                </el-tag>
              </el-space>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDateTime(profile.createdAt) }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-else class="overview-empty">正在加载个人资料...</div>
        </div>
        <div class="overview-side">
          <div class="side-title">安全建议</div>
          <ul class="side-tips">
            <li>保持联系方式最新，便于平台通知与协作。</li>
            <li>建议每 60 天更新一次密码并避免与其他系统相同。</li>
            <li>如发现异常登录，立即修改密码并联系管理员。</li>
          </ul>
          <div class="side-meta">
            <div class="meta-item">
              <div class="meta-label">账号创建时间</div>
              <div class="meta-value">{{ formatDateTime(profile?.createdAt) }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">当前角色数量</div>
              <div class="meta-value">{{ profile?.roles?.length ?? 0 }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <div class="form-grid">
      <el-card class="form-card" shadow="hover">
        <template #header>
          <div class="form-header">
            <div>
              <div class="form-title">基础信息</div>
              <div class="form-subtitle">更新姓名与邮箱，帮助管理员准确识别与联系</div>
            </div>
            <el-button type="primary" :loading="savingProfile" @click="submitProfileForm">保存修改</el-button>
          </div>
        </template>
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="96px"
          status-icon
          :disabled="loadingProfile"
        >
          <el-form-item label="用户名">
            <el-input :model-value="profile?.username || ''" disabled />
          </el-form-item>
          <el-form-item label="姓名" prop="fullName">
            <el-input v-model.trim="profileForm.fullName" placeholder="请输入姓名" maxlength="128" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model.trim="profileForm.email" placeholder="请输入邮箱地址" maxlength="128" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="form-card" shadow="hover">
        <template #header>
          <div class="form-header">
            <div>
              <div class="form-title">修改密码</div>
              <div class="form-subtitle">使用强密码可有效提升账号安全性</div>
            </div>
            <el-button type="primary" plain :loading="passwordSubmitting" @click="submitPasswordForm">
              更新密码
            </el-button>
          </div>
        </template>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="96px"
          status-icon
        >
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入当前密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入6-128位新密码"
            />
          </el-form-item>
          <el-form-item label="邮箱验证码" prop="emailCode">
            <el-input v-model.trim="passwordForm.emailCode" placeholder="请输入邮箱验证码" maxlength="16">
              <template #append>
                <el-button
                  :disabled="!profile?.email || emailCodeCountdown > 0"
                  :loading="emailCodeSending"
                  @click="sendEmailCode"
                >
                  {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
            <div class="form-helper">
              验证码将发送至 {{ profile?.email || '未绑定邮箱，请先在基础信息中绑定' }}
            </div>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请再次输入新密码"
            />
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchProfile, updateProfile, changePassword, sendPasswordEmailCode } from '../services/profile'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const profile = ref(null)
const loadingProfile = ref(false)
const savingProfile = ref(false)
const passwordSubmitting = ref(false)
const emailCodeSending = ref(false)
const emailCodeCountdown = ref(0)
let emailCodeTimer = null

const profileFormRef = ref()
const passwordFormRef = ref()

const profileForm = reactive({
  fullName: '',
  email: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
  emailCode: ''
})

const profileRules = {
  fullName: [
    {
      min: 0,
      max: 128,
      message: '姓名长度不能超过128个字符',
      trigger: ['blur', 'change']
    }
  ],
  email: [
    {
      type: 'email',
      message: '请输入有效的邮箱地址',
      trigger: ['blur', 'change']
    }
  ]
}

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码长度需要在6-128位之间', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码长度需要在6-128位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入新密码'))
        } else if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { min: 4, max: 16, message: '验证码长度应在4-16位之间', trigger: 'blur' }
  ]
}

const formatRole = role => {
  if (!role) {
    return '未知角色'
  }
  return role.name || role.code || '未知角色'
}

const formatDateTime = value => {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

const normalizeField = value => {
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed.length ? trimmed : null
}

const applyProfileToForm = data => {
  profileForm.fullName = data?.fullName ?? ''
  profileForm.email = data?.email ?? ''
}

const loadProfile = async () => {
  loadingProfile.value = true
  try {
    const data = await fetchProfile()
    if (data) {
      profile.value = data
      applyProfileToForm(data)
    }
  } catch (error) {
    const message = error?.response?.data?.message || '加载个人资料失败'
    ElMessage.error(message)
  } finally {
    loadingProfile.value = false
  }
}

const submitProfileForm = () => {
  profileFormRef.value?.validate(async valid => {
    if (!valid) {
      return
    }
    savingProfile.value = true
    try {
      const payload = {
        fullName: normalizeField(profileForm.fullName),
        email: normalizeField(profileForm.email)
      }
      const data = await updateProfile(payload)
      if (data) {
        profile.value = data
        applyProfileToForm(data)
        await authStore.fetchCurrentUser()
        ElMessage.success('个人资料已更新')
      }
    } catch (error) {
      const message = error?.response?.data?.message || '更新个人资料失败'
      ElMessage.error(message)
    } finally {
      savingProfile.value = false
    }
  })
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordForm.emailCode = ''
  passwordFormRef.value?.clearValidate?.()
}

const clearEmailCountdown = () => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
    emailCodeTimer = null
  }
}

const startEmailCountdown = seconds => {
  emailCodeCountdown.value = seconds
  clearEmailCountdown()
  emailCodeTimer = setInterval(() => {
    if (emailCodeCountdown.value <= 1) {
      clearEmailCountdown()
      emailCodeCountdown.value = 0
    } else {
      emailCodeCountdown.value -= 1
    }
  }, 1000)
}

const sendEmailCode = async () => {
  if (!profile.value?.email) {
    ElMessage.warning('请先在基础信息中绑定邮箱')
    return
  }
  if (emailCodeCountdown.value > 0 || emailCodeSending.value) {
    return
  }
  emailCodeSending.value = true
  try {
    await sendPasswordEmailCode()
    ElMessage.success('验证码已发送，请查收邮箱')
    startEmailCountdown(60)
  } catch (error) {
    const message = error?.response?.data?.message || '发送验证码失败，请稍后再试'
    ElMessage.error(message)
  } finally {
    emailCodeSending.value = false
  }
}

const submitPasswordForm = () => {
  passwordFormRef.value?.validate(async valid => {
    if (!valid) {
      return
    }
    passwordSubmitting.value = true
    try {
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
        emailCode: passwordForm.emailCode
      })
      ElMessage.success('密码更新成功')
      resetPasswordForm()
    } catch (error) {
      const message = error?.response?.data?.message || '密码更新失败'
      ElMessage.error(message)
    } finally {
      passwordSubmitting.value = false
    }
  })
}

onMounted(() => {
  loadProfile()
})

onBeforeUnmount(() => {
  clearEmailCountdown()
})
</script>

<style scoped>
.personal-center-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.overview-card {
  background: linear-gradient(135deg, #f0f7f4 0%, #ffffff 100%);
}

.overview-content {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.overview-main {
  flex: 1 1 480px;
}

.overview-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(0, 150, 136, 0.12);
  color: #00796b;
  font-size: 13px;
  font-weight: 600;
}

.overview-title {
  margin: 16px 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1b3d2f;
}

.overview-desc {
  margin: 0 0 24px;
  color: #546e7a;
  line-height: 1.6;
}

.overview-descriptions {
  max-width: 520px;
  background: rgba(255, 255, 255, 0.85);
}

.overview-empty {
  color: #90a4ae;
  font-size: 14px;
}

.overview-side {
  flex: 1 1 280px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  padding: 24px;
  box-shadow: inset 0 0 0 1px rgba(0, 150, 136, 0.12);
}

.side-title {
  font-size: 16px;
  font-weight: 600;
  color: #004d40;
  margin-bottom: 12px;
}

.side-tips {
  margin: 0;
  padding-left: 20px;
  color: #607d8b;
  line-height: 1.6;
}

.side-tips li + li {
  margin-top: 8px;
}

.side-meta {
  margin-top: 24px;
  display: grid;
  gap: 12px;
}

.meta-item {
  background: rgba(0, 150, 136, 0.08);
  border-radius: 10px;
  padding: 12px 16px;
}

.meta-label {
  font-size: 12px;
  color: #00695c;
}

.meta-value {
  font-size: 16px;
  font-weight: 600;
  color: #004d40;
  margin-top: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 24px;
}

.form-card {
  min-height: 260px;
}

.form-helper {
  margin-top: 6px;
  color: #607d8b;
  font-size: 12px;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #1b3d2f;
}

.form-subtitle {
  font-size: 13px;
  color: #607d8b;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .form-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-header .el-button {
    width: 100%;
  }
}
</style>
