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
        meta: { requiresAuth: true }
      },
      {
        path: 'data',
        name: 'data',
        component: () => import('../views/DataCenterView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'AGRICULTURE_DEPT'] }
      },
      {
        path: 'visualization',
        name: 'visualization',
        component: () => import('../views/VisualizationCenterView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'AGRICULTURE_DEPT'] }
      },
      {
        path: 'forecast',
        name: 'forecast',
        component: () => import('../views/ForecastCenterView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'AGRICULTURE_DEPT'] }
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('../views/ReportCenterView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'AGRICULTURE_DEPT', 'FARMER'] }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/UserManagementView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('../views/SystemSettingView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
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
