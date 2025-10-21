<template>
  <el-container class="layout">
    <aside class="sidebar" :class="{ 'is-collapsed': isSidebarCollapsed, 'is-hidden-mobile': isMobile }">
      <div class="sidebar-inner">
        <div class="sidebar-header">
          <div class="brand">
            <span class="brand-mark">云</span>
            <span class="brand-text" v-if="!isSidebarCollapsed">农作物产量预测</span>
          </div>
          <el-button
            v-if="!isMobile"
            class="collapse-btn"
            circle
            text
            @click="toggleSidebarCollapse"
          >
            <el-icon>
              <component :is="isSidebarCollapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
        </div>
        <el-scrollbar class="sidebar-scroll">
          <el-menu
            v-if="menuItems.length"
            :default-active="active"
            :collapse="!isMobile && isSidebarCollapsed"
            class="nav-menu"
            router
            @select="handleMenuSelect"
          >
            <el-menu-item
              v-for="item in menuItems"
              :key="item.name"
              :index="item.path"
              :route="{ name: item.name }"
            >
              <el-icon v-if="item.icon">
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-menu>
          <div v-else class="empty-menu">暂无可访问功能</div>
        </el-scrollbar>
      </div>
    </aside>

    <el-container class="content-area">
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button
            v-if="isMobile"
            class="menu-trigger"
            circle
            text
            @click="openMobileSidebar"
          >
            <el-icon><MenuIcon /></el-icon>
          </el-button>
          <div class="topbar-brand">
            <span class="brand-mark">云</span>
            <div class="brand-copy">
              <span class="brand-name">农作物产量预测平台</span>
              <span class="brand-sub">农业部门数字化管理中枢</span>
            </div>
          </div>
        </div>
        <div class="topbar-center">
          <el-input
            v-model.trim="searchKeyword"
            class="global-search"
            clearable
            placeholder="搜索数据、用户或功能"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button class="search-btn" type="primary" plain @click="handleSearch">搜索</el-button>
        </div>
        <div class="topbar-actions">
          <el-button
            v-if="!isMobile"
            class="collapse-toggle"
            circle
            text
            @click="toggleSidebarCollapse"
          >
            <el-icon>
              <component :is="isSidebarCollapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-dropdown-trigger">
              <el-avatar :size="36" class="user-avatar">{{ displayInitials }}</el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ displayName }}</span>
                <span class="user-role">{{ displayRoles }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="preferences">系统偏好</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <div class="content-wrapper">
          <el-breadcrumb v-if="breadcrumbs.length" separator="/" class="breadcrumb">
            <el-breadcrumb-item
              v-for="(crumb, index) in breadcrumbs"
              :key="`${crumb.label}-${index}`"
            >
              <router-link v-if="crumb.to && index !== breadcrumbs.length - 1" :to="crumb.to">
                {{ crumb.label }}
              </router-link>
              <span v-else>{{ crumb.label }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
          <div class="page-meta-header">
            <div class="page-meta-title">
              <h1>{{ pageTitle }}</h1>
              <p v-if="pageSubtitle">{{ pageSubtitle }}</p>
            </div>
          </div>
          <router-view />
        </div>
      </el-main>
    </el-container>

    <el-drawer
      v-model="mobileSidebarVisible"
      direction="ltr"
      size="260px"
      :with-header="false"
      custom-class="mobile-sidebar"
    >
      <div class="mobile-sidebar-inner">
        <div class="mobile-sidebar-brand">
          <span class="brand-mark">云</span>
          <span class="brand-text">功能导航</span>
        </div>
        <el-menu
          v-if="menuItems.length"
          :default-active="active"
          class="nav-menu"
          router
          @select="handleMenuSelect"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="`mobile-${item.name}`"
            :index="item.path"
            :route="{ name: item.name }"
          >
            <el-icon v-if="item.icon">
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
        <div v-else class="empty-menu">暂无可访问功能</div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  DataAnalysis,
  Document,
  Expand,
  Fold,
  Grid,
  Menu as MenuIcon,
  Monitor,
  Search,
  Setting,
  Timer,
  TrendCharts,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useAuthorization } from '../composables/useAuthorization'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { canAccessRoute } = useAuthorization()

const searchKeyword = ref('')
const isSidebarCollapsed = ref(false)
const isMobile = ref(false)
const mobileSidebarVisible = ref(false)

const rawMenuItems = [
  { label: '仪表盘', name: 'dashboard', path: '/dashboard', icon: Grid },
  { label: '数据中心', name: 'data', path: '/data', icon: DataAnalysis },
  { label: '数据可视化', name: 'visualization', path: '/visualization', icon: TrendCharts },
  { label: '预测中心', name: 'forecast', path: '/forecast', icon: Timer },
  { label: '报告中心', name: 'report', path: '/report', icon: Document },
  { label: '用户管理', name: 'users', path: '/users', icon: UserFilled },
  { label: '日志管理', name: 'logs', path: '/logs', icon: Monitor },
  { label: '系统设置', name: 'settings', path: '/settings', icon: Setting }
]

const menuItems = computed(() => rawMenuItems.filter(item => canAccessRoute(item.name)))

const active = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '农作物产量预测平台')
const pageSubtitle = computed(() => route.meta?.subtitle || '')
const breadcrumbs = computed(() => {
  const base = Array.isArray(route.meta?.breadcrumb)
    ? route.meta.breadcrumb.map(item => ({ label: item.label, to: item.to || null }))
    : []
  if (!base.length || base[base.length - 1]?.label !== pageTitle.value) {
    base.push({ label: pageTitle.value, to: null })
  }
  return base
})

const displayName = computed(
  () => authStore.user?.fullName || authStore.user?.username || '未登录用户'
)
const displayRoles = computed(() => {
  if (!Array.isArray(authStore.user?.roles) || !authStore.user.roles.length) {
    return '未分配角色'
  }
  return authStore.user.roles.join(' / ')
})

const displayInitials = computed(() => {
  const raw = displayName.value?.trim()
  if (!raw) {
    return '访客'
  }
  const sanitized = raw.replace(/\s+/g, '')
  if (!sanitized) {
    return '访客'
  }
  if (/^[a-zA-Z]+$/.test(sanitized)) {
    return sanitized.slice(0, 2).toUpperCase()
  }
  return sanitized.slice(0, 2)
})

const toggleSidebarCollapse = () => {
  if (isMobile.value) {
    return
  }
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const openMobileSidebar = () => {
  mobileSidebarVisible.value = true
}

const handleMenuSelect = () => {
  if (isMobile.value) {
    mobileSidebarVisible.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { redirect: route.fullPath } }).catch(() => {})
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.info('请输入要搜索的关键字')
    return
  }
  ElMessage.success(`已为您查询「${keyword}」的相关内容，功能持续完善中`)
}

const handleUserCommand = command => {
  if (command === 'logout') {
    handleLogout()
    return
  }
  if (command === 'profile') {
    router.push({ name: 'profile' }).catch(() => {})
    return
  }
  if (command === 'preferences') {
    router.push({ name: 'settings' }).catch(() => {})
  }
}

const updateIsMobile = () => {
  isMobile.value = window.innerWidth <= 992
  if (!isMobile.value) {
    mobileSidebarVisible.value = false
  }
}

onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIsMobile)
})

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
  min-height: 100vh;
  background: var(--background, #f6f7fb);
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1f2a56 0%, #182041 100%);
  color: #eef2ff;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
}

.sidebar.is-collapsed {
  width: 88px;
}

.sidebar.is-hidden-mobile {
  display: none;
}

.sidebar-inner {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 20px 12px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-weight: 700;
}

.brand-text {
  font-size: 16px;
}

.collapse-btn {
  color: rgba(226, 232, 255, 0.8);
}

.sidebar-scroll {
  flex: 1;
  padding: 12px 0 24px;
}

.nav-menu {
  border-right: none;
  background: transparent;
  padding: 0 12px;
}

:deep(.nav-menu .el-menu-item) {
  margin: 4px 0;
  height: 44px;
  line-height: 44px;
  border-radius: 10px;
  color: rgba(226, 232, 255, 0.8);
  font-weight: 500;
}

:deep(.nav-menu .el-menu-item .el-icon) {
  margin-right: 12px;
}

:deep(.nav-menu .el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.2);
}

.empty-menu {
  padding: 24px;
  font-size: 14px;
  color: rgba(226, 232, 255, 0.8);
}

.content-area {
  display: flex;
  flex-direction: column;
  background: var(--background, #f6f7fb);
}

.topbar {
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 24px;
  background: var(--surface, #fff);
  box-shadow: 0 1px 0 var(--border-soft, #e5e7eb);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-trigger {
  color: var(--text-secondary, #667085);
}

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.brand-sub {
  font-size: 12px;
  color: var(--text-secondary, #667085);
}

.topbar-center {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 520px;
}

.global-search :deep(.el-input__wrapper) {
  border-radius: 999px;
  padding: 0 14px;
  background: var(--surface-muted, #f2f4f7);
  border: 1px solid transparent;
}

.global-search :deep(.el-input__wrapper:hover),
.global-search :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(37, 99, 235, 0.4);
  background: #fff;
}

.search-btn {
  border-radius: 999px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-toggle {
  color: var(--text-secondary, #667085);
}

.user-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--surface-muted, #f2f4f7);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-dropdown-trigger:hover {
  background: rgba(37, 99, 235, 0.12);
}

.user-avatar {
  font-size: 14px;
  background: rgba(37, 99, 235, 0.16);
  color: #2563eb;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 120px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.user-role {
  font-size: 12px;
  color: var(--text-secondary, #667085);
}

.main {
  background: var(--background, #f6f7fb);
  padding: 24px;
}

.content-wrapper {
  max-width: 1320px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.breadcrumb {
  font-size: 13px;
  color: var(--text-secondary, #667085);
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary, #111827);
  font-weight: 600;
}

.page-meta-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 0 4px;
}

.page-meta-title h1 {
  margin: 0;
  font-size: 24px;
  line-height: 32px;
  font-weight: 700;
  color: var(--text-primary, #111827);
}

.page-meta-title p {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--text-secondary, #667085);
}

.mobile-sidebar {
  background: linear-gradient(180deg, #1f2a56 0%, #182041 100%);
  color: #fff;
}

.mobile-sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px 8px 24px;
}

.mobile-sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px 16px;
  font-weight: 600;
}

@media (max-width: 992px) {
  .sidebar {
    display: none;
  }

  .main {
    padding: 20px 16px 32px;
  }

  .content-wrapper {
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .topbar {
    flex-wrap: wrap;
    height: auto;
    padding: 12px 16px;
    gap: 16px;
  }

  .topbar-center {
    order: 3;
    width: 100%;
    max-width: unset;
  }

  .topbar-actions {
    margin-left: auto;
  }

  .page-meta-title h1 {
    font-size: 22px;
  }
}
</style>
