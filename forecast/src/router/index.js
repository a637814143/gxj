import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

const routes = [
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'AppDashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'data-management',
        name: 'DataManagement',
        component: () => import('@/views/DataManagement.vue'),
        meta: { title: '数据管理', icon: 'DataBoard', roles: ['ADMIN'] }
      },
      {
        path: 'data-analysis',
        name: 'DataAnalysis',
        component: () => import('@/views/DataAnalysis.vue'),
        meta: { title: '数据分析', icon: 'TrendCharts', roles: ['ADMIN', 'RESEARCHER'] }
      },
      {
        path: 'prediction',
        name: 'CropPrediction',
        component: () => import('@/views/Prediction.vue'),
        meta: { title: '产量预测', icon: 'TrendCharts', roles: ['ADMIN', 'RESEARCHER', 'FARMER'] }
      },
      {
        path: 'visualization',
        name: 'DataVisualization',
        component: () => import('@/views/Visualization.vue'),
        meta: { title: '数据可视化', icon: 'PieChart', roles: ['ADMIN', 'RESEARCHER', 'FARMER'] }
      },
      {
        path: 'reports',
        name: 'DataReports',
        component: () => import('@/views/Reports.vue'),
        meta: { title: '报告管理', icon: 'Document', roles: ['ADMIN', 'RESEARCHER'] }
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] }
      },
      {
        path: 'system-settings',
        name: 'SystemSettings',
        component: () => import('@/views/SystemSettings.vue'),
        meta: { title: '系统设置', icon: 'Setting', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人资料', icon: 'User' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 从多个地方尝试获取token
  const token = store.getters.token || store.state.user.token
  const userRoles = store.getters.roles || store.state.user.roles
  
  console.log('路由守卫 - store.getters.token:', store.getters.token)
  console.log('路由守卫 - store.state.user.token:', store.state.user.token)
  console.log('路由守卫 - 最终token:', token)
  console.log('路由守卫 - userRoles:', userRoles)
  console.log('路由守卫 - to.path:', to.path)
  console.log('路由守卫 - to.meta:', to.meta)
  console.log('路由守卫 - store state:', store.state.user)
  
  if (to.meta.requiresAuth !== false) {
    if (!token) {
      console.log('路由守卫 - 没有token，跳转到登录页')
      next('/login')
      return
    }
    
    // 检查角色权限
    if (to.meta.roles && to.meta.roles.length > 0) {
      const hasRole = to.meta.roles.some(role => userRoles.includes(role))
      console.log('路由守卫 - 角色检查:', { requiredRoles: to.meta.roles, userRoles, hasRole })
      if (!hasRole) {
        console.log('路由守卫 - 权限不足，跳转到仪表盘')
        next('/dashboard')
        return
      }
    }
  }
  
  if (to.path === '/login' && token) {
    console.log('路由守卫 - 已登录，跳转到仪表盘')
    next('/dashboard')
    return
  }
  
  console.log('路由守卫 - 允许访问')
  next()
})

export default router
