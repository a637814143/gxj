import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    component: () => import('../views/DashboardView.vue')
  },
  {
    path: '/data',
    component: () => import('../views/DataCenterView.vue')
  },
  {
    path: '/forecast',
    component: () => import('../views/ForecastCenterView.vue')
  },
  {
    path: '/reports',
    component: () => import('../views/ReportCenterView.vue')
  },
  {
    path: '/settings',
    component: () => import('../views/SystemSettingView.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
