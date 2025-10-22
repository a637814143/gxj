<template>
  <div class="page-header">
    <div class="page-header-main">
      <div class="page-title-block">
        <div v-if="badge" class="page-badge">{{ badge }}</div>
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="description" class="page-description">{{ description }}</p>
      </div>
      <div v-if="$slots.meta" class="page-meta">
        <slot name="meta" />
      </div>
    </div>
    <div v-if="$slots.extra" class="page-actions">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px;
  background: var(--surface, #fff);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.page-header-main {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: flex-start;
}

.page-title-block {
  flex: 1 1 280px;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.4px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  line-height: 32px;
  font-weight: 700;
  color: var(--text-primary, #111827);
}

.page-description {
  margin: 0;
  font-size: 15px;
  line-height: 24px;
  color: var(--text-secondary, #667085);
}

.page-meta {
  flex: 1 1 260px;
  min-width: 240px;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
}

@media (min-width: 1024px) {
  .page-header {
    flex-direction: column;
  }

  .page-actions {
    margin-left: auto;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 20px;
    border-radius: 14px;
  }

  .page-title {
    font-size: 22px;
    line-height: 30px;
  }
}
</style>
