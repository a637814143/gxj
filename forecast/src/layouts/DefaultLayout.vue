<template>
  <el-container class="layout">
    <el-aside width="240px" class="sidebar">
      <div class="logo">农作物产量预测平台</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/dashboard" :route="{ name: 'dashboard' }">
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/data" :route="{ name: 'data' }">
          <span>数据中心</span>
        </el-menu-item>
        <el-menu-item index="/visualization" :route="{ name: 'visualization' }">
          <span>数据可视化</span>
        </el-menu-item>
        <el-menu-item index="/forecast" :route="{ name: 'forecast' }">
          <span>预测中心</span>
        </el-menu-item>
        <el-menu-item index="/report" :route="{ name: 'report' }">
          <span>报告中心</span>
        </el-menu-item>
        <el-menu-item index="/users" :route="{ name: 'users' }">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/settings" :route="{ name: 'settings' }">
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-actions">
          <div class="user-info">
            <div class="user-name">{{ displayName }}</div>
            <div class="user-role">{{ displayRoles }}</div>
          </div>
          <el-button type="primary" link @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const titles = {
  dashboard: '概览仪表盘',
  data: '数据资源管理',
  visualization: '数据可视化洞察',
  forecast: '预测建模与任务',
  report: '报告输出与分享',
  users: '用户与部门管理',
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

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { redirect: route.fullPath } }).catch(() => {})
}
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
</style>
