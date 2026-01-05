<template>
  <teleport to="body">
    <div v-if="visibleActions.length" class="side-quick-actions">
      <button
        v-for="action in visibleActions"
        :key="action.key"
        type="button"
        class="quick-action"
        :class="{ active: isActive(action), [`accent-${action.accent}`]: Boolean(action.accent) }"
        @click="handleAction(action)"
      >
        <span class="quick-icon">{{ action.icon }}</span>
        <span class="quick-label">{{ action.label }}</span>
      </button>
    </div>
  </teleport>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthorization } from '../composables/useAuthorization'

const route = useRoute()
const router = useRouter()
const { canAccessRoute, hasRole } = useAuthorization()

const actionConfigs = computed(() => [
  { key: 'dashboard', label: '仪表盘', icon: '📊', type: 'route', name: 'dashboard', accent: 'sunrise' },
  { key: 'forecast', label: '预测中心', icon: '🚀', type: 'route', name: 'forecast', accent: 'sunset' },
  { key: 'report', label: '报告中心', icon: '📄', type: 'route', name: 'report', accent: 'ocean' },
  { key: 'profile', label: '个人中心', icon: '👤', type: 'route', name: 'profile', accent: 'peach' }
])

const visibleActions = computed(() =>
  actionConfigs.value.filter(action => {
    if (action.type === 'route') {
      return canAccessRoute(action.name)
    }
    return true
  })
)

const isActive = action => action.type === 'route' && action.name === route.name

const handleAction = action => {
  if (!action) return
  if (action.type === 'route' && action.name) {
    router.push({ name: action.name }).catch(() => {})
  }
}
</script>

<style scoped>
.side-quick-actions {
  position: fixed;
  top: 50%;
  right: 32px;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 2100;
  pointer-events: none;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  padding: 12px 16px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.16);
  color: #0f172a;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(15, 118, 110, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  pointer-events: auto;
  backdrop-filter: blur(4px);
}

.quick-action:hover {
  transform: translateX(-4px);
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.22);
}

.quick-action.active {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.35), 0 14px 28px rgba(59, 130, 246, 0.28);
  transform: translateX(-4px);
}

.quick-icon {
  font-size: 18px;
}

.quick-label {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.quick-action.accent-sunrise {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.95), rgba(251, 113, 133, 0.85));
  color: #2f1b02;
}

.quick-action.accent-sunset {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.95), rgba(225, 29, 72, 0.88));
  color: #2a0900;
}

.quick-action.accent-ocean {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.92), rgba(79, 70, 229, 0.88));
  color: #f0f9ff;
}

.quick-action.accent-forest {
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.92), rgba(45, 212, 191, 0.88));
  color: #023427;
}

.quick-action.accent-violet {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.92), rgba(244, 63, 94, 0.85));
  color: #fdf4ff;
}

.quick-action.accent-peach {
  background: linear-gradient(135deg, rgba(253, 186, 116, 0.92), rgba(244, 114, 182, 0.85));
  color: #3b0a1a;
}

@media (max-width: 1024px) {
  .side-quick-actions {
    top: auto;
    bottom: 32px;
    right: 24px;
    transform: none;
  }
}

@media (max-width: 768px) {
  .side-quick-actions {
    left: 50%;
    right: auto;
    bottom: 24px;
    transform: translateX(-50%);
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
  }

  .quick-action {
    border-radius: 16px;
    padding: 10px 12px;
    font-size: 12px;
  }
}
</style>
