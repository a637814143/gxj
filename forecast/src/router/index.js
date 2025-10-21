import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: DefaultLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: {
          requiresAuth: true,
          title: '概览仪表盘',
          subtitle: '实时掌握平台运行态势'
        }
      },
      {
        path: 'data',
        name: 'data',
        component: () => import('../views/DataCenterView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'AGRICULTURE_DEPT'],
          title: '数据资源管理',
          subtitle: '统一管理基础数据与导入任务',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'visualization',
        name: 'visualization',
        component: () => import('../views/VisualizationCenterView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'AGRICULTURE_DEPT', 'FARMER'],
          title: '数据可视化洞察',
          subtitle: '多维图表洞察生产趋势',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'forecast',
        name: 'forecast',
        component: () => import('../views/ForecastCenterView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'AGRICULTURE_DEPT'],
          title: '预测建模与任务',
          subtitle: '配置模型与监控预测进度',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('../views/ReportCenterView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'AGRICULTURE_DEPT', 'FARMER'],
          title: '报告输出与分享',
          subtitle: '对外发布预测成果与洞察',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/UserManagementView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '用户与部门管理',
          subtitle: '集中维护农业部门与业务用户',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('../views/LogManagementView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '登录日志监控与审计',
          subtitle: '追踪平台访问与关键操作',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/ProfileView.vue'),
        meta: {
          requiresAuth: true,
          title: '个人资料中心',
          subtitle: '维护账户安全、业务信息与偏好设置',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('../views/SystemSettingView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '系统设置',
          subtitle: '配置平台参数与安全策略',
          breadcrumb: [{ label: '首页', to: '/dashboard' }]
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    try {
      await authStore.initialize()
    } catch (error) {
      console.error('Auth initialization failed', error)
    }
  }

  if (to.meta.public) {
    if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
      return next({ path: to.query.redirect || '/dashboard' })
    }
    return next()
  }

  const requiresAuth = to.matched.some(record => record.meta && record.meta.requiresAuth !== false && !record.meta.public)
  if (requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  const requiredRoles = to.matched
    .flatMap(record => (record.meta && Array.isArray(record.meta.roles) ? record.meta.roles : []))
    .filter((value, index, array) => array.indexOf(value) === index)

  if (requiresAuth && requiredRoles.length > 0 && !authStore.hasAnyRole(requiredRoles)) {
    return next({ name: 'dashboard' })
  }

  return next()
})

export default router
