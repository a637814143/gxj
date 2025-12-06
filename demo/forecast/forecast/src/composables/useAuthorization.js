import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const normalizeRoles = roles => {
  if (!roles) {
    return []
  }
  if (Array.isArray(roles)) {
    return roles
  }
  return [roles]
}

export function useAuthorization() {
  const router = useRouter()
  const authStore = useAuthStore()

  const userRoles = computed(() => authStore.user?.roles || [])

  const hasRole = roles => authStore.hasAnyRole(normalizeRoles(roles))

  const canAccessRoute = routeName => {
    if (!routeName) {
      return false
    }

    const targetRoute = router.getRoutes().find(record => record.name === routeName)

    if (!targetRoute) {
      return false
    }

    const requiredRoles = normalizeRoles(targetRoute.meta?.roles)
    if (!requiredRoles.length) {
      return true
    }

    return hasRole(requiredRoles)
  }

  return {
    userRoles,
    hasRole,
    canAccessRoute
  }
}
