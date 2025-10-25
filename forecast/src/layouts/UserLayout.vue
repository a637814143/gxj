<template>
  <div class="user-layout">
    <aside class="quick-panel">
      <div class="quick-brand">
        <span class="brand-icon">🌾</span>
        <div class="brand-text">
          <div class="brand-title">智慧农服</div>
          <div class="brand-subtitle">快捷入口</div>
        </div>
      </div>
      <div class="quick-actions">
        <button
          v-for="action in quickActions"
          :key="action.key"
          type="button"
          class="quick-button"
          :class="[{ active: isActive(action) }, `accent-${action.accent || 'default'}`]"
          @click="handleAction(action)"
        >
          <span class="quick-icon">{{ action.icon }}</span>
          <span class="quick-label">{{ action.label }}</span>
        </button>
      </div>
    </aside>

    <div class="content-shell">
      <header class="user-header">
        <div class="header-main">
          <div class="header-text">
            <h1 class="header-title">{{ currentTitle }}</h1>
            <p v-if="userSubtitle" class="header-subtitle">{{ userSubtitle }}</p>
          </div>
          <div class="header-actions">
            <WeatherWidget class="header-weather" />
            <div class="user-info">
              <div class="user-name">{{ displayName }}</div>
              <div class="user-role">{{ displayRoles }}</div>
            </div>
            <el-button type="primary" @click="handleLogout">退出登录</el-button>
          </div>
        </div>
        <div v-if="navLinks.length" class="nav-links">
          <el-button
            v-for="link in navLinks"
            :key="link.name"
            class="nav-chip"
            :type="route.name === link.name ? 'primary' : 'default'"
            link
            @click="navigateTo(link.name)"
          >
            {{ link.label }}
          </el-button>
        </div>
      </header>

      <main class="user-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAuthorization } from '../composables/useAuthorization'
import WeatherWidget from '../components/weather/WeatherWidget.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { canAccessRoute, hasRole } = useAuthorization()

const rawMenuItems = [
  { label: '仪表盘', name: 'dashboard', path: '/dashboard' },
  { label: '数据中心', name: 'data', path: '/data' },
  { label: '数据可视化', name: 'visualization', path: '/visualization' },
  { label: '预测中心', name: 'forecast', path: '/forecast' },
  { label: '在线咨询', name: 'consultation', path: '/consultation' },
  { label: '报告中心', name: 'report', path: '/report' },
  { label: '个人中心', name: 'profile', path: '/profile' }
]

const menuItems = computed(() => rawMenuItems.filter(item => canAccessRoute(item.name)))

const quickActionConfigs = computed(() => [
  { key: 'dashboard', label: '仪表盘', icon: '📊', type: 'route', name: 'dashboard', accent: 'sunrise' },
  { key: 'data', label: '数据中心', icon: '🗄️', type: 'route', name: 'data', accent: 'coral' },
  { key: 'visualization', label: '数据可视化', icon: '📈', type: 'route', name: 'visualization', accent: 'violet' },
  { key: 'forecast', label: '预测中心', icon: '🚀', type: 'route', name: 'forecast', accent: 'sunset' },
  { key: 'consultation', label: '在线咨询', icon: '💬', type: 'route', name: 'consultation', accent: 'lagoon' },
  { key: 'report', label: '报告中心', icon: '📄', type: 'route', name: 'report', accent: 'ocean' },
  { key: 'report-generate', label: '生成报告', icon: '📝', type: 'generate', accent: 'forest' },
  { key: 'profile', label: '个人中心', icon: '👤', type: 'route', name: 'profile', accent: 'peach' }
])

const quickActions = computed(() =>
  quickActionConfigs.value.filter(action => {
    if (action.type === 'route') {
      return canAccessRoute(action.name)
    }
    if (action.type === 'generate') {
      return canAccessRoute('report') && hasRole(['ADMIN', 'AGRICULTURE_DEPT'])
    }
    return true
  })
)

const quickActionNames = computed(() => new Set(quickActions.value.filter(item => item.name).map(item => item.name)))

const navLinks = computed(() =>
  menuItems.value.filter(item => !quickActionNames.value.has(item.name))
)

const titles = {
  dashboard: '概览仪表盘',
  data: '数据资源管理',
  visualization: '数据可视化洞察',
  forecast: '预测建模与任务',
  consultation: '在线咨询与专家建议',
  report: '报告输出与分享',
  profile: '个人资料与安全设置'
}

const currentTitle = computed(() => titles[route.name] || '智慧农服工作台')
const displayName = computed(() => authStore.user?.fullName || authStore.user?.username || '未登录')
const displayRoles = computed(() => {
  const roles = authStore.user?.roles
  if (!roles || !roles.length) {
    return '未分配角色'
  }
  return roles.join(' / ')
})

const userSubtitle = computed(() => {
  if (!displayName.value || displayName.value === '未登录') {
    return ''
  }
  const hours = new Date().getHours()
  if (hours < 12) {
    return `${displayName.value}，早上好！祝你今天工作顺利。`
  }
  if (hours < 18) {
    return `${displayName.value}，下午好！记得适时查看最新预测数据。`
  }
  return `${displayName.value}，晚上好！欢迎继续查阅最新报告。`
})

const isActive = action => action.type === 'route' && action.name === route.name

const navigateTo = name => {
  if (!name) return
  router.push({ name }).catch(() => {})
}

const navigateToReport = query => {
  router.push({ name: 'report', query }).catch(() => {})
}

const handleAction = action => {
  if (!action) return
  if (action.type === 'route' && action.name) {
    navigateTo(action.name)
    return
  }
  if (action.type === 'generate') {
    navigateToReport({ action: 'generate' })
    return
  }
}

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
.user-layout {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(150deg, #f0f9ff 0%, #fdf5ff 45%, #ffffff 100%);
  overflow-x: hidden;
}

.quick-panel {
  width: 240px;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.16) 0%, rgba(244, 114, 182, 0.14) 100%);
  border-right: 1px solid rgba(14, 165, 233, 0.2);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.35);
}

.quick-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(14, 116, 144, 0.25);
}

.brand-icon {
  font-size: 28px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f766e;
}

.brand-subtitle {
  font-size: 12px;
  color: #0ea5e9;
  letter-spacing: 1px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-button {
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  border-radius: 16px;
  padding: 14px 18px;
  background: rgba(59, 130, 246, 0.12);
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.12);
}

.quick-button:hover {
  transform: translateX(6px);
  box-shadow: 0 18px 32px rgba(59, 130, 246, 0.22);
}

.quick-button.active {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.35), 0 16px 32px rgba(59, 130, 246, 0.25);
  transform: translateX(6px);
}

.quick-icon {
  font-size: 18px;
}

.quick-label {
  letter-spacing: 0.5px;
}

.quick-button.accent-sunrise {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.92), rgba(251, 113, 133, 0.88));
  color: #3b1700;
}

.quick-button.accent-sunset {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.95), rgba(225, 29, 72, 0.88));
  color: #2a0900;
}

.quick-button.accent-ocean {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.92), rgba(79, 70, 229, 0.88));
  color: #f8fafc;
}

.quick-button.accent-coral {
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.92), rgba(244, 114, 182, 0.88));
  color: #3f0a1f;
}

.quick-button.accent-forest {
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.92), rgba(45, 212, 191, 0.88));
  color: #064e3b;
}

.quick-button.accent-violet {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.92), rgba(244, 63, 94, 0.85));
  color: #f5f3ff;
}

.quick-button.accent-peach {
  background: linear-gradient(135deg, rgba(253, 186, 116, 0.92), rgba(244, 114, 182, 0.85));
  color: #3f0a1f;
}

.content-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(0px);
  min-width: 0;
}

.user-header {
  padding: 28px 32px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(135deg, rgba(224, 242, 254, 0.8), rgba(240, 249, 255, 0.95));
}

.header-main {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.header-subtitle {
  margin: 0;
  font-size: 14px;
  color: #0369a1;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-weather {
  flex: 0 0 auto;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  font-size: 12px;
  color: #64748b;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.user-role {
  color: #0f766e;
}

.nav-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.nav-chip {
  font-weight: 500;
  border-radius: 999px !important;
}

.user-main {
  flex: 1;
  padding: 24px 32px;
  background: transparent;
  overflow-y: auto;
  overflow-x: hidden;
}

@media (max-width: 1024px) {
  .user-layout {
    flex-direction: column;
  }

  .quick-panel {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 20px 24px;
  }

  .quick-brand {
    border-bottom: none;
    border-right: 1px dashed rgba(14, 116, 144, 0.25);
    padding-bottom: 0;
    padding-right: 16px;
    margin-right: 8px;
  }

  .quick-actions {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 10px;
  }

  .quick-button {
    padding: 12px 14px;
    border-radius: 14px;
  }
}

@media (max-width: 768px) {
  .header-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    margin-left: 0;
    width: 100%;
    justify-content: space-between;
  }

  .user-main {
    padding: 16px 20px;
  }
}
</style>
