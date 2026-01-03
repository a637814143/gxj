<template>
  <el-container class="layout">
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
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-actions">
          <div class="user-info">
            <div class="user-name">{{ displayName }}</div>
            <div class="user-role">{{ displayRoles }}</div>
          </div>
          <el-button type="primary" link :loading="authStore.isLoggingOut" @click="handleLogout">
            退出登录
          </el-button>
        </div>
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
  { label: '预测可视化', name: 'forecastVisualization', path: '/forecast-visualization' },
  { label: '天气监测', name: 'weather', path: '/weather' },
  { label: '气象分析', name: 'weatherAnalytics', path: '/weather-analytics' },
  { label: '报告中心', name: 'report', path: '/report' },
  { label: '咨询管理', name: 'consultations', path: '/consultations' },
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
  forecastVisualization: '预测数据可视化',
  report: '报告输出与分享',
  weather: '实时天气监测',
  weatherAnalytics: '气象数据分析',
  consultations: '在线咨询管理',
  users: '用户与部门管理',
  profile: '个人资料与安全设置',
  logs: '登录日志监控与审计',
  settings: '系统设置'
}

const active = computed(() => route.path)
const currentTitle = computed(() => titles[route.name] || '农作物产量预测平台')
const displayName = computed(() => authStore.user?.fullName || authStore.user?.username || '未登录')
const displayRoles = computed(() => {
  const roles = authStore.user?.roles
  if (!roles || !roles.length) {
    return '未分配角色'
  }
  return roles.join(' / ')
})

const handleLogout = async () => {
  if (authStore.isLoggingOut) {
    return
  }
  authStore.beginLogout()
  try {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
  } catch (error) {
    console.warn('导航至登录页失败', error)
  } finally {
    authStore.logout()
  }
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

.header-widgets {
  display: flex;
  align-items: center;
  gap: 12px;
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
  overflow-y: auto;
}
</style>
