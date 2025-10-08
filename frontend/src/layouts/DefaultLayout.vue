<template>
  <el-container class="layout">
    <el-header class="layout__header">
      <div class="layout__brand">农作物产量预测与可视化分析系统</div>
      <div class="layout__actions">
        <el-button type="primary" plain size="small">导入数据</el-button>
        <el-button type="success" size="small">新建预测</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" class="layout__aside">
        <el-menu router default-active="/dashboard" class="layout__menu">
          <el-menu-item index="/dashboard">仪表盘</el-menu-item>
          <el-menu-item index="/data">数据管理</el-menu-item>
          <el-menu-item index="/forecast">预测分析</el-menu-item>
          <el-menu-item index="/reports">报告中心</el-menu-item>
          <el-menu-item index="/settings">系统设置</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #2563eb, #38bdf8);
  color: #fff;
  padding: 0 24px;
}

.layout__brand {
  font-weight: 600;
  font-size: 18px;
}

.layout__actions {
  display: flex;
  gap: 12px;
}

.layout__aside {
  background: #0f172a;
  color: #e2e8f0;
}

.layout__menu {
  border-right: none;
}

.layout__main {
  padding: 24px;
  background: #f4f6fb;
}
</style>
