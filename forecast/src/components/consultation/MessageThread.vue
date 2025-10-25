<template>
  <section class="message-thread">
    <header class="thread-header" v-if="conversation">
      <div class="header-text">
        <h3 class="thread-title">{{ conversation.subject }}</h3>
        <p class="thread-meta">
          <span v-if="conversation.cropType">作物：{{ conversation.cropType }}</span>
          <span>会话编号：{{ conversation.id }}</span>
        </p>
      </div>
      <div class="header-tags">
        <el-tag size="small" :type="statusMeta[conversation.status]?.type || 'info'">
          {{ statusMeta[conversation.status]?.label || '未知状态' }}
        </el-tag>
        <el-tag size="small" effect="plain" v-if="conversation.priority">
          {{ priorityMeta[conversation.priority]?.label || '普通' }}
        </el-tag>
      </div>
    </header>

    <el-scrollbar ref="scrollbar" class="thread-body">
      <el-skeleton v-if="loading" animated :rows="6" />
      <template v-else>
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
              <ul v-if="message.attachments?.length" class="attachment-list">
                <li v-for="attachment in message.attachments" :key="attachment.id">
                  <el-link :href="attachment.url" target="_blank" type="primary">
                    {{ attachment.name }}
                  </el-link>
                  <span class="attachment-type">{{ attachment.type }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </template>
    </el-scrollbar>
  </section>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'

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
  }
})

const scrollbar = ref()

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

.header-tags {
  display: flex;
  gap: 8px;
}

.thread-body {
  flex: 1;
  padding: 16px 24px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 8px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-item.mine {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #72c6a2, #2c7a7b);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-fallback {
  font-size: 16px;
}

.bubble {
  max-width: 520px;
  background: #ffffff;
  border-radius: 16px;
  padding: 12px 16px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  position: relative;
}

.message-item.mine .bubble {
  background: linear-gradient(120deg, #1b7f5f, #29a76f);
  color: #ffffff;
}

.bubble-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 8px;
}

.message-item.mine .bubble-header {
  color: rgba(255, 255, 255, 0.8);
}

.sender-name {
  font-weight: 600;
}

.sender-role {
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(27, 127, 95, 0.12);
  color: #1b4332;
}

.message-item.mine .sender-role {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.timestamp {
  margin-left: auto;
}

.message-item.mine .bubble-content p {
  color: #ffffff;
}

.bubble-content p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.attachment-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attachment-type {
  margin-left: 8px;
  font-size: 12px;
  color: rgba(96, 125, 139, 0.8);
}

.message-item.mine .attachment-type {
  color: rgba(255, 255, 255, 0.7);
}
</style>
