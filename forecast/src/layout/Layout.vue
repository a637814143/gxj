<template>
  <div class="layout-container">
    <!-- 头部 -->
    <header class="layout-header">
      <div class="header-left">
        <el-button
          type="text"
          @click="toggleSidebar"
          class="sidebar-toggle"
        >
          <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        </el-button>
        <h1 class="system-title">农作物产量预测系统</h1>
      </div>
      
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userInfo?.realName || userInfo?.username || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    
    <!-- 主体内容 -->
    <div class="layout-body">
      <!-- 侧边栏 -->
      <aside class="layout-sidebar" :class="{ collapsed }">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :unique-opened="true"
          router
          background-color="#001529"
          text-color="#fff"
          active-text-color="#1890ff"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <el-menu-item
              v-if="!route.children"
              :index="route.path"
              :disabled="!hasPermission(route.meta.roles)"
            >
              <el-icon><component :is="route.meta.icon" /></el-icon>
              <template #title>{{ route.meta.title }}</template>
            </el-menu-item>
            
            <el-sub-menu
              v-else
              :index="route.path"
              :disabled="!hasPermission(route.meta.roles)"
            >
              <template #title>
                <el-icon><component :is="route.meta.icon" /></el-icon>
                <span>{{ route.meta.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="child.path"
                :disabled="!hasPermission(child.meta.roles)"
              >
                <el-icon><component :is="child.meta.icon" /></el-icon>
                <template #title>{{ child.meta.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </aside>
      
      <!-- 主内容区 -->
      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'AppLayout',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()
    
    const collapsed = ref(false)
    
    const userInfo = computed(() => {
      const user = store.getters['user/user']
      console.log('Layout - userInfo:', user)
      return user
    })
    const userRoles = computed(() => {
      const roles = store.getters['user/roles']
      console.log('Layout - userRoles:', roles)
      return roles
    })
    
    const activeMenu = computed(() => route.path)
    
    const menuRoutes = computed(() => {
      return router.getRoutes().find(r => r.path === '/').children || []
    })
    
    const toggleSidebar = () => {
      collapsed.value = !collapsed.value
    }
    
    const hasPermission = (roles) => {
      if (!roles || roles.length === 0) return true
      return roles.some(role => userRoles.value.includes(role))
    }
    
    const handleCommand = (command) => {
      switch (command) {
        case 'profile':
          router.push('/profile')
          break
        case 'logout':
          handleLogout()
          break
      }
    }
    
    const handleLogout = () => {
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        store.dispatch('user/logout').then(() => {
          ElMessage.success('退出成功')
          router.push('/login')
        })
      }).catch(() => {
        // 用户取消
      })
    }
    
    return {
      collapsed,
      userInfo,
      activeMenu,
      menuRoutes,
      toggleSidebar,
      hasPermission,
      handleCommand
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
}

.sidebar-toggle {
  margin-right: 20px;
  font-size: 18px;
}

.system-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.user-info .el-icon {
  margin: 0 4px;
}

.layout-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.layout-sidebar {
  width: 200px;
  background: #001529;
  transition: width 0.3s;
  overflow-y: auto;
}

.layout-sidebar.collapsed {
  width: 64px;
}

.layout-main {
  flex: 1;
  padding: 20px;
  background: #f0f2f5;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .layout-sidebar {
    position: fixed;
    left: 0;
    top: 60px;
    height: calc(100vh - 60px);
    z-index: 999;
    transform: translateX(-100%);
    transition: transform 0.3s;
  }
  
  .layout-sidebar:not(.collapsed) {
    transform: translateX(0);
  }
  
  .layout-main {
    padding: 10px;
  }
  
  .system-title {
    font-size: 16px;
  }
}
</style>
