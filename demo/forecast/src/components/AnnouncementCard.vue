<template>
  <div class="announcement-card" :class="{ inactive: statusValue !== 'ACTIVE' }">
    <div class="card-header">
      <div class="card-pill">
        <span class="pill-dot" :class="statusValue.toLowerCase()" />
        <span class="pill-text">{{ statusLabel }}</span>
      </div>
      <span v-if="updatedAtText" class="timestamp">{{ updatedAtText }}</span>
    </div>
    <h4 class="card-title">{{ announcement?.title || '平台通知' }}</h4>
    <p class="card-message">{{ displayMessage }}</p>
    <div class="card-email">
      <span class="email-label">通知邮箱</span>
      <span class="email-value">{{ displayEmail }}</span>
    </div>
    <div class="card-footer">
      <span class="footer-label">{{ statusValue === 'ACTIVE' ? '正在展示给所有用户' : '未生效' }}</span>
      <el-link v-if="announcement?.link" type="primary" :href="announcement.link" target="_blank">查看详情</el-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  announcement: {
    type: Object,
    default: () => ({})
  },
  statusLabel: {
    type: String,
    default: '未启用'
  }
})

const statusValue = computed(() => (props.announcement?.status || 'INACTIVE').toUpperCase())
const displayMessage = computed(() => props.announcement?.message || '暂无通知内容')
const displayEmail = computed(() => {
  const raw = props.announcement?.notifyEmail ?? props.announcement?.notify_email ?? ''
  const normalized = typeof raw === 'string' ? raw.trim() : ''
  return normalized || '未配置'
})

const updatedAtText = computed(() => {
  const raw = props.announcement?.updatedAt
  if (!raw) return ''
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(date)
})
</script>

<style scoped>
.announcement-card {
  min-width: 240px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.12), rgba(33, 203, 243, 0.08));
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(33, 150, 243, 0.18);
}

.announcement-card.inactive {
  background: #f6f7fb;
  border-color: #e0e6f1;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.card-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(33, 150, 243, 0.16);
  color: #1878d1;
  font-size: 12px;
  font-weight: 600;
}

.announcement-card.inactive .card-pill {
  background: #eef1f8;
  color: #607d8b;
}

.pill-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1abc9c;
  box-shadow: 0 0 0 4px rgba(26, 188, 156, 0.15);
}

.pill-dot.inactive {
  background: #b0bec5;
  box-shadow: 0 0 0 4px rgba(176, 190, 197, 0.18);
}

.pill-dot.scheduled {
  background: #ffb300;
  box-shadow: 0 0 0 4px rgba(255, 179, 0, 0.18);
}

.timestamp {
  font-size: 12px;
  color: #607d8b;
}

.card-title {
  margin: 0 0 6px;
  font-size: 16px;
  color: #0f172a;
  font-weight: 700;
}

.card-message {
  margin: 0 0 10px;
  font-size: 13px;
  color: #44566c;
  line-height: 1.5;
}

.card-email {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin: 0 0 10px;
  border-radius: 10px;
  background: rgba(33, 150, 243, 0.08);
  color: #1d4d8b;
  font-size: 12px;
  word-break: break-all;
}

.announcement-card.inactive .card-email {
  background: #eef1f8;
  color: #42526e;
}

.email-label {
  font-weight: 700;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #5c6d8d;
}

.footer-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
