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
        <el-menu-item index="/forecast" :route="{ name: 'forecast' }">
          <span>预测中心</span>
        </el-menu-item>
        <el-menu-item index="/report" :route="{ name: 'report' }">
          <span>报告中心</span>
        </el-menu-item>
        <el-menu-item index="/settings" :route="{ name: 'settings' }">
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const titles = {
  dashboard: '概览仪表盘',
  data: '数据资源管理',
  forecast: '预测建模与任务',
  report: '报告输出与分享',
  settings: '系统设置'
}

const active = computed(() => route.path)
const currentTitle = computed(() => titles[route.name] || '农作物产量预测平台')
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

.main {
  background: #f5f7fa;
}
</style>
