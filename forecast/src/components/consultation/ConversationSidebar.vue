<template>
  <aside class="conversation-sidebar">
    <header class="sidebar-header">
      <div class="header-text">
        <h2 class="header-title">咨询会话</h2>
        <p class="header-subtitle">与农业部门保持实时沟通</p>
      </div>
      <el-button type="primary" size="small" @click="$emit('create')">
        <el-icon class="icon"><Plus /></el-icon>
        新建咨询
      </el-button>
    </header>

    <div class="sidebar-filters">
      <el-radio-group v-model="filterState" size="small" class="filter-toggle">
        <el-radio-button label="active">进行中</el-radio-button>
        <el-radio-button label="history">历史记录</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        placeholder="搜索作物或主题"
        clearable
        size="small"
        class="filter-search"
        @clear="handleClearSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-scrollbar class="conversation-list">
      <el-skeleton v-if="loading" animated :rows="6" class="skeleton" />
      <template v-else>
        <div v-if="!filteredConversations.length" class="empty">
          <el-empty
            :image-size="120"
            description="暂无相关会话，立即发起咨询"
          />
        </div>
        <button
          v-for="item in filteredConversations"
          :key="item.id"
          type="button"
          class="conversation-item"
          :class="{ active: item.id === activeId }"
          @click="$emit('select', item.id)"
        >
          <div class="item-main">
            <div class="item-title-row">
              <span class="item-subject">{{ item.subject }}</span>
              <el-tag v-if="statusMeta[item.status]" size="small" :type="statusMeta[item.status].type">
                {{ statusMeta[item.status].label }}
              </el-tag>
            </div>
            <div class="item-meta">
              <span class="item-crop" v-if="item.cropType">{{ item.cropType }}</span>
              <span class="item-time">{{ formatTime(item.updatedAt || item.createdAt) }}</span>
            </div>
            <div class="item-preview">
              <el-badge :value="item.unreadCount" :hidden="!item.unreadCount" type="danger">
                <span class="preview-text">
                  {{ item.lastMessage?.content || '暂无最新消息' }}
                </span>
              </el-badge>
            </div>
          </div>
        </button>
      </template>
    </el-scrollbar>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'

const props = defineProps({
  conversations: {
    type: Array,
    default: () => []
  },
  activeId: {
    type: [String, Number],
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select', 'create'])

const filterState = ref('active')
const keyword = ref('')

const statusMeta = {
  pending: { label: '待回复', type: 'warning' },
  in_progress: { label: '处理中', type: 'primary' },
  processing: { label: '处理中', type: 'primary' },
  resolved: { label: '已完成', type: 'success' },
  closed: { label: '已关闭', type: 'info' }
}

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())

const filteredConversations = computed(() => {
  const list = Array.isArray(props.conversations) ? props.conversations : []
  return list
    .filter(item => {
      if (filterState.value === 'history') {
        return ['resolved', 'closed'].includes(item.status)
      }
      return !['resolved', 'closed'].includes(item.status)
    })
    .filter(item => {
      if (!normalizedKeyword.value) {
        return true
      }
      const text = `${item.subject ?? ''} ${item.cropType ?? ''}`.toLowerCase()
      return text.includes(normalizedKeyword.value)
    })
})

const handleClearSearch = () => {
  keyword.value = ''
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
    console.warn('Failed to format conversation time', error)
    return value
  }
}
</script>

<style scoped>
.conversation-sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #f5f9ff 0%, #ffffff 100%);
}

.sidebar-header {
  padding: 20px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.header-text {
  flex: 1;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d3b2f;
}

.header-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #607d8b;
}

.sidebar-filters {
  padding: 0 20px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-toggle {
  display: flex;
  justify-content: center;
}

.filter-toggle :deep(.el-radio-button__inner) {
  padding: 6px 16px;
}

.filter-search :deep(.el-input__wrapper) {
  background-color: #f7fafc;
}

.conversation-list {
  flex: 1;
  padding: 0 12px 12px;
}

.conversation-item {
  width: 100%;
  display: flex;
  border: none;
  background: transparent;
  border-radius: 12px;
  padding: 12px;
  margin: 0 0 8px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.conversation-item:hover {
  background-color: rgba(38, 105, 71, 0.08);
  transform: translateY(-1px);
}

.conversation-item.active {
  background: linear-gradient(120deg, rgba(28, 133, 95, 0.15), rgba(28, 133, 95, 0.05));
  box-shadow: 0 6px 12px rgba(18, 87, 62, 0.08);
}

.item-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.item-subject {
  font-weight: 600;
  font-size: 14px;
  color: #1b4332;
  flex: 1;
}

.item-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #78909c;
}

.item-preview {
  font-size: 12px;
  color: #546e7a;
  display: flex;
}

.preview-text {
  display: inline-block;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty {
  padding-top: 80px;
}
</style>
