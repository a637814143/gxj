<template>
  <el-container :class="['layout', layoutTheme]">
    <el-aside width="240px" class="sidebar">
      <div class="logo">农作物产量预测平台</div>
      <el-menu v-if="menuItems.length" :default-active="active" router>
        <el-menu-item
          v-for="item in menuItems"
          :key="item.name"
          :index="item.path"
          :route="{ name: item.name }"
        >
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div v-else class="empty-menu">暂无可访问功能</div>
    </el-aside>
    <el-container>
      <el-header class="header" :class="{ 'header-user': !isAdminExperience }">
        <div class="header-main">
          <div class="header-text">
            <div class="header-title">{{ currentTitle }}</div>
            <div v-if="!isAdminExperience && userSubtitle" class="header-subtitle">{{ userSubtitle }}</div>
          </div>
          <div class="header-actions">
            <div class="user-info">
              <div class="user-name">{{ displayName }}</div>
              <div class="user-role">{{ displayRoles }}</div>
            </div>
            <el-button type="primary" link @click="handleLogout">退出登录</el-button>
          </div>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
    <side-quick-actions v-if="showSideActions" />
  </el-container>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAuthorization } from '../composables/useAuthorization'
import SideQuickActions from '../components/SideQuickActions.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { canAccessRoute } = useAuthorization()

const rawMenuItems = [
  { label: '仪表盘', name: 'dashboard', path: '/dashboard' },
  { label: '数据中心', name: 'data', path: '/data' },
  { label: '数据可视化', name: 'visualization', path: '/visualization' },
  { label: '预测中心', name: 'forecast', path: '/forecast' },
  { label: '报告中心', name: 'report', path: '/report' },
  { label: '用户管理', name: 'users', path: '/users' },
  { label: '个人中心', name: 'profile', path: '/profile' },
  { label: '日志管理', name: 'logs', path: '/logs' },
  { label: '系统设置', name: 'settings', path: '/settings' }
]

const menuItems = computed(() => rawMenuItems.filter(item => canAccessRoute(item.name)))

const titles = {
  dashboard: '概览仪表盘',
  data: '数据资源管理',
  visualization: '数据可视化洞察',
  forecast: '预测建模与任务',
  report: '报告输出与分享',
  users: '用户与部门管理',
  profile: '个人资料与安全设置',
  logs: '登录日志监控与审计',
  settings: '系统设置'
}

const active = computed(() => route.path)
const currentTitle = computed(() => titles[route.name] || '农作物产量预测平台')
const displayName = computed(() => authStore.user?.fullName || authStore.user?.username || '未登录')
const displayRoles = computed(() => {
  if (!authStore.user?.roles?.length) {
    return '未分配角色'
  }
  return authStore.user.roles.join(' / ')
})

const isAdminExperience = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) {
    return false
  }
  if (Array.isArray(roles)) {
    return roles.includes('ADMIN')
  }
  return roles === 'ADMIN'
})

const layoutTheme = computed(() => (isAdminExperience.value ? 'admin-theme' : 'user-theme'))

const userSubtitle = computed(() => {
  if (isAdminExperience.value) {
    return ''
  }
  return `${displayName.value}，欢迎回来！祝你今日使用顺利。`
})

const showSideActions = computed(() => !isAdminExperience.value)

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { redirect: route.fullPath } }).catch(() => {})
}

watch(
  () => [route.name, menuItems.value],
  ([currentName, items]) => {
    if (!Array.isArray(items) || !items.length) {
      return
    }
    const hasCurrent = items.some(item => item.name === currentName)
    if (!hasCurrent) {
      router.replace({ name: items[0].name }).catch(() => {})
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.layout {
  height: 100vh;
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
}

.sidebar {
  background-color: #0b3d2e;
  color: #fff;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.logo {
  font-size: 18px;
  font-weight: 600;
  padding: 24px 16px;
  text-align: center;
  letter-spacing: 2px;
}

.el-menu {
  border-right: none;
  flex: 1;
  background-color: transparent;
}

.el-menu-item {
  color: #cfd8dc;
}

.el-menu-item.is-active {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.empty-menu {
  padding: 24px 16px;
  font-size: 14px;
  color: #cfd8dc;
}

.header {
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid #ebeef5;
  position: relative;
  z-index: 1;
  height: auto !important;
  min-height: 64px;
  background-color: #ffffff;
}

.header-title {
  font-size: 20px;
  font-weight: 500;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  color: #90a4ae;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #263238;
}

.user-role {
  color: #607d8b;
}

.main {
  background: #f5f7fa;
  position: relative;
  z-index: 1;
}

.layout.user-theme {
  background: linear-gradient(160deg, #f0f9ff 0%, #fdfdfd 100%);
}

.layout.user-theme::before,
.layout.user-theme::after {
  content: '';
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  opacity: 0.3;
  filter: blur(0px);
  pointer-events: none;
  z-index: 0;
}

.layout.user-theme::before {
  top: -200px;
  left: -160px;
  background: radial-gradient(circle at center, rgba(56, 189, 248, 0.4), transparent 70%);
}

.layout.user-theme::after {
  bottom: -220px;
  right: -140px;
  background: radial-gradient(circle at center, rgba(236, 72, 153, 0.35), transparent 65%);
}

.layout.user-theme .sidebar {
  background: linear-gradient(180deg, #ffffff 0%, #f0f9ff 45%, #fff0f6 100%);
  color: #1f2933;
  box-shadow: 6px 0 18px rgba(15, 118, 110, 0.08);
  border-right: 1px solid rgba(14, 165, 233, 0.2);
}

.layout.user-theme .logo {
  font-size: 20px;
  font-weight: 700;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0f2f1;
  background: linear-gradient(90deg, #0f766e 0%, #0891b2 50%, #9333ea 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.layout.user-theme :deep(.el-menu) {
  padding: 12px 0;
  background: transparent;
}

.layout.user-theme :deep(.el-menu-item) {
  margin: 4px 12px;
  border-radius: 12px;
  height: 48px;
  line-height: 48px;
  color: #3d5a80;
  font-weight: 500;
  transition: all 0.2s ease;
}

.layout.user-theme :deep(.el-menu-item:hover) {
  background: rgba(56, 189, 248, 0.12);
  color: #0369a1;
  transform: translateX(4px);
}

.layout.user-theme :deep(.el-menu-item.is-active) {
  background: rgba(16, 185, 129, 0.18);
  color: #047857;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(16, 185, 129, 0.32);
}

.header-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 16px;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-subtitle {
  font-size: 13px;
  color: #64748b;
}

.header-user {
  background: linear-gradient(135deg, #f0f9ff 0%, #fef6fb 100%);
  border-bottom: 1px solid rgba(14, 165, 233, 0.2);
}

.header-user .header-title {
  color: #0f766e;
}

.header-user .header-subtitle {
  color: #0e7490;
}

.header-user .user-info .user-name {
  color: #0f172a;
}

.header-user .user-info .user-role {
  color: #0f766e;
}

.layout.user-theme .main {
  background: linear-gradient(180deg, #f4f9ff 0%, #ffffff 100%);
  padding: 24px;
  overflow-x: hidden;
  overflow-y: auto;
}

.layout.user-theme .main::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 15%, rgba(56, 189, 248, 0.12), transparent 55%),
    radial-gradient(circle at 80% 10%, rgba(251, 191, 36, 0.1), transparent 50%),
    radial-gradient(circle at 30% 85%, rgba(16, 185, 129, 0.1), transparent 55%);
  pointer-events: none;
  z-index: -1;
}

@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .header-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
