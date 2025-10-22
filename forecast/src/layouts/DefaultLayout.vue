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
          <div class="user-header-shell">
            <span class="user-header-orb orb-1" />
            <span class="user-header-orb orb-2" />
            <span class="user-header-orb orb-3" />
            <div class="user-header-content">
              <div class="user-header-left">
                <div class="user-greeting-title">你好，{{ displayName }}！</div>
                <div class="user-greeting-subtitle">欢迎回来，以下工具可帮助你快速了解最新预测与报告。</div>
                <div class="user-role-chip">{{ displayRoles }}</div>
                <div class="user-highlight-grid">
                  <div
                    v-for="highlight in userHighlights"
                    :key="highlight.name"
                    class="user-highlight-card"
                    :class="`accent-${highlight.name}`"
                  >
                    <div class="highlight-value">{{ highlight.value }}</div>
                    <div class="highlight-label">{{ highlight.label }}</div>
                    <div class="highlight-desc">{{ highlight.description }}</div>
                  </div>
                </div>
              </div>
              <div class="user-header-right">
                <div class="user-quick-actions">
                  <el-button
                    v-for="action in userQuickActions"
                    :key="action.name"
                    class="user-quick-button"
                    :class="`accent-${action.accent}`"
                    size="large"
                    round
                    @click="goToRoute(action.name)"
                  >
                    <span class="quick-button-icon">{{ action.icon }}</span>
                    <span class="quick-button-label">{{ action.label }}</span>
                    <span class="quick-button-arrow">→</span>
                  </el-button>
                </div>
                <div v-if="userDailyTip" class="user-tip-card">
                  <div class="tip-title">今日提示</div>
                  <div class="tip-content">{{ userDailyTip }}</div>
                </div>
                <el-button class="user-logout" type="danger" plain round @click="handleLogout">
                  退出登录
                </el-button>
              </div>
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

const dateFormatter = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric' })

const userQuickActions = computed(() => {
  if (isAdminExperience.value) {
    return []
  }
  const available = new Set(menuItems.value.map(item => item.name))
  const accentMap = {
    dashboard: 'sunrise',
    report: 'forest',
    visualization: 'ocean',
    forecast: 'sunset',
    profile: 'violet'
  }
  const iconMap = {
    dashboard: '📊',
    report: '📝',
    visualization: '📈',
    forecast: '🚀',
    profile: '👤'
  }
  const candidates = [
    { name: 'dashboard', label: '仪表盘速览' },
    { name: 'report', label: '报告中心' },
    { name: 'visualization', label: '趋势图表' },
    { name: 'forecast', label: '发起预测' },
    { name: 'profile', label: '个人资料' }
  ]
  return candidates
    .filter(action => available.has(action.name))
    .map(action => ({
      ...action,
      accent: accentMap[action.name] || 'sunrise',
      icon: iconMap[action.name] || '✨'
    }))
})

const userHighlights = computed(() => {
  if (isAdminExperience.value) {
    return []
  }
  const accessibleCount = Array.isArray(menuItems.value) ? menuItems.value.length : 0
  const roles = authStore.user?.roles
  const roleCount = Array.isArray(roles) ? roles.length : roles ? 1 : 0
  let todayLabel
  try {
    todayLabel = dateFormatter.format(new Date())
  } catch (error) {
    todayLabel = new Date().toLocaleDateString('zh-CN')
  }
  return [
    {
      name: 'modules',
      label: '可访问模块',
      value: accessibleCount,
      description: '当前账号可进入的功能数量'
    },
    {
      name: 'roles',
      label: '角色权限',
      value: roleCount,
      description: '已分配的角色数量'
    },
    {
      name: 'today',
      label: '今日日期',
      value: todayLabel,
      description: '保持关注最新的预测信息'
    }
  ]
})

const userDailyTip = computed(() => {
  if (isAdminExperience.value) {
    return ''
  }
  const hour = new Date().getHours()
  if (hour < 12) {
    return `${displayName.value}，上午好！先浏览仪表盘了解最新动态吧。`
  }
  if (hour < 18) {
    return `${displayName.value}，下午好！尝试生成最新报告或继续预测任务。`
  }
  return `${displayName.value}，晚上好！记得回顾报告并整理预测结论。`
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

.header-user {
  border-bottom: none;
  background: transparent;
  box-shadow: none;
  padding: 0 24px 28px;
  align-items: stretch;
  min-height: unset;
}

.user-header-shell {
  position: relative;
  width: 100%;
  background: linear-gradient(135deg, rgba(236, 253, 245, 0.95) 0%, rgba(224, 242, 254, 0.95) 50%, rgba(254, 242, 242, 0.95) 100%);
  border-radius: 28px;
  padding: 32px 36px;
  overflow: hidden;
  display: flex;
}

.user-header-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 15% 20%, rgba(56, 189, 248, 0.24), transparent 55%),
    radial-gradient(circle at 85% 30%, rgba(192, 132, 252, 0.22), transparent 50%),
    radial-gradient(circle at 65% 85%, rgba(74, 222, 128, 0.2), transparent 45%);
  z-index: 0;
  pointer-events: none;
}

.user-header-orb {
  position: absolute;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  filter: blur(0px);
  opacity: 0.35;
  mix-blend-mode: screen;
  pointer-events: none;
}

.user-header-orb.orb-1 {
  top: -60px;
  left: -40px;
  background: linear-gradient(135deg, rgba(45, 212, 191, 0.65), rgba(56, 189, 248, 0.5));
}

.user-header-orb.orb-2 {
  bottom: -80px;
  right: 80px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.5), rgba(147, 197, 253, 0.45));
}

.user-header-orb.orb-3 {
  top: 20%;
  right: -100px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.4), rgba(248, 113, 113, 0.45));
}

.user-header-content {
  position: relative;
  z-index: 1;
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
  max-width: 520px;
}

.user-greeting-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f766e;
}

.user-greeting-subtitle {
  color: #0e7490;
  font-size: 14px;
  line-height: 1.7;
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

.user-highlight-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.user-highlight-card {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 26px rgba(14, 116, 144, 0.14);
  color: #0f172a;
  backdrop-filter: blur(4px);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.user-highlight-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 38px rgba(14, 116, 144, 0.2);
}

.user-highlight-card .highlight-value {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 4px;
}

.user-highlight-card .highlight-label {
  font-size: 13px;
  letter-spacing: 0.5px;
}

.user-highlight-card .highlight-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #0369a1;
}

.user-highlight-card.accent-modules {
  border: 1px solid rgba(56, 189, 248, 0.45);
}

.user-highlight-card.accent-roles {
  border: 1px solid rgba(236, 72, 153, 0.35);
}

.user-highlight-card.accent-today {
  border: 1px solid rgba(163, 230, 53, 0.45);
}

.user-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16px;
}

.user-quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.user-quick-actions :deep(.el-button.user-quick-button) {
  min-width: 148px;
  font-weight: 600;
  border: none;
  color: #ffffff;
  padding: 18px 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.user-quick-actions :deep(.el-button.user-quick-button::after) {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.18);
  mix-blend-mode: overlay;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.user-quick-actions :deep(.el-button.user-quick-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 16px 35px rgba(14, 116, 144, 0.25);
}

.user-quick-actions :deep(.el-button.user-quick-button:hover::after) {
  opacity: 1;
}

.user-quick-actions :deep(.el-button.user-quick-button .quick-button-icon) {
  font-size: 20px;
}

.user-quick-actions :deep(.el-button.user-quick-button .quick-button-label) {
  font-size: 14px;
  letter-spacing: 0.5px;
}

.user-quick-actions :deep(.el-button.user-quick-button .quick-button-arrow) {
  font-size: 18px;
  opacity: 0.85;
}

.user-quick-actions :deep(.el-button.user-quick-button.accent-sunrise) {
  background: linear-gradient(135deg, #facc15 0%, #fb7185 100%);
  box-shadow: 0 12px 28px rgba(251, 113, 133, 0.28);
}

.user-quick-actions :deep(.el-button.user-quick-button.accent-forest) {
  background: linear-gradient(135deg, #34d399 0%, #22d3ee 100%);
  box-shadow: 0 12px 28px rgba(45, 212, 191, 0.28);
}

.user-quick-actions :deep(.el-button.user-quick-button.accent-ocean) {
  background: linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%);
  box-shadow: 0 12px 28px rgba(14, 165, 233, 0.28);
}

.user-quick-actions :deep(.el-button.user-quick-button.accent-sunset) {
  background: linear-gradient(135deg, #f97316 0%, #f43f5e 100%);
  box-shadow: 0 12px 28px rgba(244, 63, 94, 0.28);
}

.user-quick-actions :deep(.el-button.user-quick-button.accent-violet) {
  background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%);
  box-shadow: 0 12px 28px rgba(139, 92, 246, 0.28);
}

.user-tip-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 16px 20px;
  min-width: 220px;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.18);
  border-left: 4px solid #f59e0b;
  backdrop-filter: blur(4px);
  text-align: left;
}

.tip-title {
  font-size: 13px;
  font-weight: 600;
  color: #f97316;
  margin-bottom: 6px;
}

.tip-content {
  font-size: 13px;
  color: #1e293b;
  line-height: 1.6;
}

.user-logout {
  font-weight: 600;
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

@media (max-width: 1200px) {
  .user-header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-header-right {
    align-items: stretch;
    width: 100%;
  }

  .user-quick-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .header-user {
    padding: 0 16px 20px;
  }

  .user-header-shell {
    padding: 24px;
  }

  .user-quick-actions :deep(.el-button.user-quick-button) {
    width: 100%;
  }
}
</style>
