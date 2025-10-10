import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue')
  },
  {
    path: '/data',
    name: 'data',
    component: () => import('../views/DataCenterView.vue')
  },
  {
    path: '/forecast',
    name: 'forecast',
    component: () => import('../views/ForecastCenterView.vue')
  },
  {
    path: '/report',
    name: 'report',
    component: () => import('../views/ReportCenterView.vue')
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('../views/SystemSettingView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
