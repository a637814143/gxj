<template>
  <section class="message-thread">
    <header v-if="conversation" class="thread-header">
      <div class="header-text">
        <h3 class="thread-title">{{ conversation.subject }}</h3>
        <p class="thread-meta">
          <span v-if="conversation.cropType">作物：{{ conversation.cropType }}</span>
          <span>会话编号：{{ conversation.id }}</span>
        </p>
      </div>
      <div class="header-actions">
        <div class="header-tags">
          <el-tag size="small" :type="statusMeta[conversation.status]?.type || 'info'">
            {{ statusMeta[conversation.status]?.label || '未知状态' }}
          </el-tag>
          <el-tag size="small" effect="plain" v-if="conversation.priority">
            {{ priorityMeta[conversation.priority]?.label || '普通' }}
          </el-tag>
        </div>
        <el-button
          v-if="canCloseConversation"
          type="danger"
          size="small"
          plain
          :loading="closing"
          @click="$emit('close', conversation.id)"
        >
          结束对话
        </el-button>
      </div>
    </header>

    <el-scrollbar ref="scrollbar" class="thread-body">
      <el-skeleton v-if="loading" animated :rows="6" />
      <template v-else>
        <div v-if="isClosed" class="thread-notice">
          该对话已结束，如需继续沟通请发起新的咨询。
        </div>
        <el-empty
          v-if="!messages.length"
          description="暂未有消息，开始对话吧"
          :image-size="140"
        />
        <div v-else class="message-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="{ mine: isMine(message) }"
          >
            <div class="avatar">
              <img
                v-if="message.senderAvatar"
                :src="message.senderAvatar"
                alt="头像"
                class="avatar-image"
              />
              <div v-else class="avatar-fallback">{{ getInitial(message.senderName) }}</div>
            </div>
            <div class="bubble">
              <div class="bubble-header">
                <span class="sender-name">{{ message.senderName }}</span>
                <span class="sender-role" v-if="message.senderRole">{{ roleMeta[message.senderRole] || message.senderRole }}</span>
                <span class="timestamp">{{ formatTime(message.createdAt) }}</span>
              </div>
              <div class="bubble-content" v-if="message.content">
                <p>{{ message.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-scrollbar>
  </section>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentUserId: {
    type: [String, Number],
    default: null
  },
  conversation: {
    type: Object,
    default: null
  },
  allowClose: {
    type: Boolean,
    default: false
  },
  closing: {
    type: Boolean,
    default: false
  }
})

defineEmits(['close'])

const scrollbar = ref()

const isClosed = computed(() => (props.conversation?.status || '').toLowerCase() === 'closed')

const canCloseConversation = computed(() => props.allowClose && props.conversation && !isClosed.value)

const statusMeta = {
  pending: { label: '待回复', type: 'warning' },
  in_progress: { label: '处理中', type: 'primary' },
  processing: { label: '处理中', type: 'primary' },
  resolved: { label: '已完成', type: 'success' },
  closed: { label: '已关闭', type: 'info' }
}

const priorityMeta = {
  low: { label: '低优先级' },
  normal: { label: '普通优先级' },
  high: { label: '高优先级' },
  urgent: { label: '紧急' }
}

const roleMeta = {
  FARMER: '种植户',
  AGRICULTURE_DEPT: '农业部门',
  ADMIN: '管理员'
}

const isMine = message => {
  if (!message) {
    return false
  }
  if (props.currentUserId && message.senderId) {
    return String(message.senderId) === String(props.currentUserId)
  }
  const currentRole = props.conversation?.participants?.find(participant => participant?.self)?.role
  if (!currentRole || !message.senderRole) {
    return false
  }
  return currentRole === message.senderRole
}

const getInitial = name => {
  if (!name) {
    return '访客'
  }
  return name.trim().charAt(0).toUpperCase()
}

const formatTime = value => {
  if (!value) {
    return '刚刚'
  }
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      throw new Error('Invalid date')
    }
    return new Intl.DateTimeFormat('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  } catch (error) {
    console.warn('Failed to format message time', error)
    return value
  }
}

watch(
  () => props.messages?.length,
  async () => {
    await nextTick()
    if (scrollbar.value?.wrapRef) {
      scrollbar.value.wrapRef.scrollTop = scrollbar.value.wrapRef.scrollHeight
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.message-thread {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.thread-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.thread-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1b4332;
}

.thread-meta {
  margin: 8px 0 0;
  font-size: 12px;
  display: flex;
  gap: 12px;
  color: #607d8b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-tags {
  display: flex;
  gap: 8px;
}

.thread-body {
  flex: 1;
  min-height: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.thread-notice {
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(21, 82, 56, 0.08);
  color: #1b4332;
  font-size: 13px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-item.mine {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e8f5e9;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 16px;
  font-weight: 600;
  color: #2f855a;
}

.bubble {
  flex: 1;
  max-width: 70%;
  background: #ffffff;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 10px 30px rgba(27, 67, 50, 0.06);
}

.message-item.mine .bubble {
  background: #1b4332;
  color: #ffffff;
}

.bubble-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.sender-name {
  font-weight: 600;
}

.sender-role {
  font-size: 12px;
  color: rgba(96, 125, 139, 0.8);
}

.message-item.mine .sender-role {
  color: rgba(255, 255, 255, 0.8);
}

.timestamp {
  margin-left: auto;
  font-size: 12px;
  color: rgba(96, 125, 139, 0.8);
}

.message-item.mine .timestamp {
  color: rgba(255, 255, 255, 0.7);
}

.bubble-content p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
