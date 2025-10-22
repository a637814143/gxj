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
        <template v-if="isAdminExperience">
          <div class="header-title">{{ currentTitle }}</div>
          <div class="header-actions">
            <div class="user-info">
              <div class="user-name">{{ displayName }}</div>
              <div class="user-role">{{ displayRoles }}</div>
            </div>
            <el-button type="primary" link @click="handleLogout">退出登录</el-button>
          </div>
        </template>
        <template v-else>
          <div class="user-header-content">
            <div class="user-header-left">
              <div class="user-greeting-title">你好，{{ displayName }}！</div>
              <div class="user-greeting-subtitle">欢迎回来，以下工具可帮助你快速了解最新预测与报告。</div>
              <div class="user-role-chip">{{ displayRoles }}</div>
            </div>
            <div class="user-header-right">
              <div class="user-quick-actions">
                <el-button
                  v-for="action in userQuickActions"
                  :key="action.name"
                  :type="action.type"
                  size="large"
                  round
                  plain
                  @click="goToRoute(action.name)"
                >
                  {{ action.label }}
                </el-button>
              </div>
              <el-button class="user-logout" type="danger" plain round @click="handleLogout">
                退出登录
              </el-button>
            </div>
          </div>
        </template>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAuthorization } from '../composables/useAuthorization'

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

const userQuickActions = computed(() => {
  if (isAdminExperience.value) {
    return []
  }
  const available = new Set(menuItems.value.map(item => item.name))
  const candidates = [
    { name: 'dashboard', label: '仪表盘速览', type: 'primary' },
    { name: 'report', label: '报告中心', type: 'success' },
    { name: 'visualization', label: '趋势图表', type: 'info' },
    { name: 'forecast', label: '发起预测', type: 'warning' },
    { name: 'profile', label: '个人资料', type: 'primary' }
  ]
  return candidates.filter(action => available.has(action.name))
})

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { redirect: route.fullPath } }).catch(() => {})
}

const goToRoute = name => {
  if (!name) {
    return
  }
  router.push({ name }).catch(() => {})
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
}

.sidebar {
  background-color: #0b3d2e;
  color: #fff;
  display: flex;
  flex-direction: column;
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
}

.layout.user-theme {
  background: linear-gradient(160deg, #f0f9ff 0%, #fdfdfd 100%);
}

.layout.user-theme .sidebar {
  background: #ffffff;
  color: #1f2933;
  box-shadow: 6px 0 18px rgba(15, 118, 110, 0.08);
}

.layout.user-theme .logo {
  color: #0f766e;
  font-size: 20px;
  font-weight: 700;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0f2f1;
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
}

.layout.user-theme :deep(.el-menu-item:hover) {
  background: rgba(56, 189, 248, 0.12);
  color: #0369a1;
}

.layout.user-theme :deep(.el-menu-item.is-active) {
  background: rgba(16, 185, 129, 0.18);
  color: #047857;
  font-weight: 600;
}

.header-user {
  border-bottom: none;
  background: linear-gradient(120deg, #ecfdf5 0%, #eff6ff 100%);
  box-shadow: 0 8px 24px rgba(79, 209, 197, 0.18);
  border-radius: 0 0 32px 32px;
  padding: 24px 32px;
}

.user-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  width: 100%;
}

.user-header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-greeting-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f766e;
}

.user-greeting-subtitle {
  color: #0e7490;
  font-size: 14px;
}

.user-role-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 148, 136, 0.16);
  color: #0f766e;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.5px;
  width: fit-content;
}

.user-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.user-quick-actions :deep(.el-button) {
  min-width: 132px;
  font-weight: 600;
}

.user-logout {
  font-weight: 600;
}

.layout.user-theme .main {
  background: linear-gradient(180deg, #f4f9ff 0%, #ffffff 100%);
  padding: 24px;
}
</style>
