<template>
  <component :is="activeLayout" />
</template>

<script setup>
import { computed } from 'vue'
import AdminLayout from './AdminLayout.vue'
import UserLayout from './UserLayout.vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const isAdminExperience = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) {
    return false
  }
  if (Array.isArray(roles)) {
    return roles.includes('ADMIN')
  }
  return roles === 'ADMIN'
})

const activeLayout = computed(() => (isAdminExperience.value ? AdminLayout : UserLayout))
</script>
