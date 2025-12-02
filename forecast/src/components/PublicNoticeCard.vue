<template>
  <div class="notice-card" :class="statusClass">
    <div class="notice-header">
      <div class="notice-badge">平台通知</div>
      <div class="notice-status">
        <span class="status-dot" :class="statusClass" />
        <span class="status-text">{{ statusLabel }}</span>
      </div>
    </div>

    <div class="notice-body" v-if="!loading && notice && notice.visible">
      <h3 class="notice-title">{{ notice.title || '系统通知' }}</h3>
      <p class="notice-summary">{{ notice.summary || '暂无摘要，管理员可在系统设置中维护公告内容。' }}</p>
      <div class="notice-meta">
        <div class="meta-item">
          <span class="meta-label">受众</span>
          <span class="meta-value">{{ notice.audience || '全体用户' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">级别</span>
          <span class="meta-value">{{ notice.level || '普通' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">生效时间</span>
          <span class="meta-value">{{ formatRange(notice.startAt, notice.endAt) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">同步时间</span>
          <span class="meta-value">{{ formatDate(notice.updatedAt) }}</span>
        </div>
      </div>
    </div>
    <div class="notice-body" v-else-if="loading">
      <div class="skeleton title" />
      <div class="skeleton line" />
      <div class="skeleton line" />
      <div class="skeleton meta" />
    </div>
    <div class="notice-body" v-else>
      <h3 class="notice-title">暂无公告</h3>
      <p class="notice-summary">管理员可在系统设置中开启并发布对外通知。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  notice: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const statusClass = computed(() => {
  const state = resolveStatus()
  if (state === 'upcoming') return 'status-upcoming'
  if (state === 'expired') return 'status-expired'
  if (state === 'hidden') return 'status-hidden'
  return 'status-active'
})

const statusLabel = computed(() => {
  const state = resolveStatus()
  switch (state) {
    case 'upcoming':
      return '即将生效'
    case 'expired':
      return '已过期'
    case 'hidden':
      return '未开放'
    default:
      return '生效中'
  }
})

const resolveStatus = () => {
  if (!props.notice || !props.notice.visible) return 'hidden'
  const now = new Date().getTime()
  const start = parseDate(props.notice.startAt)?.getTime()
  const end = parseDate(props.notice.endAt)?.getTime()
  if (start && now < start) return 'upcoming'
  if (end && now > end) return 'expired'
  return 'active'
}

const formatDate = value => {
  if (!value) return '—'
  const date = parseDate(value)
  if (!date) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatRange = (start, end) => {
  const startText = formatDate(start)
  const endText = formatDate(end)
  if (!start && !end) return '长期有效'
  if (start && !end) return `${startText} 起`
  if (!start && end) return `即日起 - ${endText}`
  return `${startText} - ${endText}`
}

const parseDate = value => {
  if (!value) return null
  const parsed = value instanceof Date ? value : new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}
</script>

<style scoped>
.notice-card {
  position: relative;
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(140deg, #f6f9ff 0%, #eef3ff 50%, #ffffff 100%);
  box-shadow: 0 16px 40px rgba(61, 92, 255, 0.12);
  border: 1px solid rgba(74, 106, 255, 0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.notice-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 50px rgba(61, 92, 255, 0.18);
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notice-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(74, 106, 255, 0.12);
  color: #3a57f7;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.notice-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #4f5b76;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  background: #00c853;
  box-shadow: 0 0 0 6px rgba(0, 200, 83, 0.12);
}

.status-active .status-dot {
  background: #00c853;
  box-shadow: 0 0 0 6px rgba(0, 200, 83, 0.12);
}

.status-upcoming .status-dot {
  background: #ffb300;
  box-shadow: 0 0 0 6px rgba(255, 179, 0, 0.12);
}

.status-expired .status-dot,
.status-hidden .status-dot {
  background: #c5c9d6;
  box-shadow: 0 0 0 6px rgba(197, 201, 214, 0.12);
}

.notice-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-title {
  margin: 0;
  font-size: 18px;
  color: #0f1f3d;
}

.notice-summary {
  margin: 0;
  color: #44506a;
  line-height: 1.6;
  font-size: 14px;
}

.notice-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(74, 106, 255, 0.06);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #6a7796;
}

.meta-value {
  font-size: 14px;
  color: #1f2b47;
  font-weight: 600;
}

.skeleton {
  border-radius: 8px;
  background: linear-gradient(90deg, #eef1f7 25%, #f6f8fc 50%, #eef1f7 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

.skeleton.title {
  width: 70%;
  height: 18px;
}

.skeleton.line {
  width: 100%;
  height: 12px;
}

.skeleton.meta {
  width: 80%;
  height: 12px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
