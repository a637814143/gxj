<template>
  <div class="consultation-management-page">
    <el-card class="summary-card" shadow="hover">
      <div class="summary-content">
        <div class="summary-main">
          <div class="summary-badge">在线对话监管</div>
          <h1 class="summary-title">农业咨询全局监控</h1>
          <p class="summary-desc">
            查看并管理农户与农业部门之间的所有交流记录，及时掌握处理进度，保障咨询反馈的质量与效率。
          </p>
          <div class="summary-stats">
            <div class="stat-item">
              <div class="stat-value">{{ summaryMetrics.total }}</div>
              <div class="stat-label">累计会话</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ summaryMetrics.open }}</div>
              <div class="stat-label">进行中</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ summaryMetrics.closed }}</div>
              <div class="stat-label">已关闭</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ summaryMetrics.avgMessages }}</div>
              <div class="stat-label">平均消息数</div>
            </div>
          </div>
        </div>
        <div class="summary-side">
          <div class="side-title">处理建议</div>
          <ul class="side-list">
            <li>优先跟进长期未回复的咨询，确保农户得到及时反馈。</li>
            <li>关闭已解决的对话，便于团队聚焦在待处理事项。</li>
            <li>统计高频问题，沉淀为农业知识库提升服务效率。</li>
          </ul>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="table-header">
          <div>
            <div class="table-title">咨询会话列表</div>
            <div class="table-subtitle">按状态筛选与检索，快速定位需要关注的对话</div>
          </div>
          <div class="table-filters">
            <el-select v-model="filters.status" size="small" @change="handleStatusChange">
              <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-input
              v-model.trim="filters.keyword"
              size="small"
              placeholder="搜索主题 / 作物 / 发起人"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button size="small" type="primary" @click="handleSearch">查询</el-button>
            <el-button size="small" @click="handleReset">重置</el-button>
            <el-button size="small" type="success" :loading="tableLoading" @click="handleRefresh">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="tableLoading" empty-text="暂无符合条件的会话" :header-cell-style="tableHeaderStyle">
        <el-table-column label="咨询主题" min-width="240">
          <template #default="{ row }">
            <div class="subject-cell">
              <span class="subject-text">{{ row.subject }}</span>
              <el-tag v-if="priorityMeta[row.priority]" size="small" effect="plain">{{ priorityMeta[row.priority].label }}</el-tag>
            </div>
            <div class="subject-meta">
              <span v-if="row.cropType">作物：{{ row.cropType }}</span>
              <span v-if="row.description" class="subject-description">{{ truncate(row.description) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发起人" min-width="160">
          <template #default="{ row }">
            {{ row.owner?.name || '未知用户' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'">{{ statusMeta[row.status]?.label || '未知状态' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="消息数量" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="success" effect="plain">{{ row.messageCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近更新" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openDetail(row)">查看详情</el-button>
            <el-button
              type="danger"
              text
              size="small"
              :disabled="isClosed(row)"
              :loading="closingId === row.id && consultationStore.closing"
              @click="handleCloseRequest(row)"
            >
              结束对话
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="table-tip">管理员可以集中查看所有咨询，必要时手动关闭已完成的会话。</div>
        <div class="table-pagination">
          <div class="pagination-info">
            <span>共 {{ tablePagination.total }} 条记录</span>
            <span>第 {{ tablePagination.page }}/{{ totalPages }} 页</span>
          </div>
          <el-pagination
            :current-page="tablePagination.page"
            :page-size="tablePagination.pageSize"
            :total="tablePagination.total"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>

    <el-drawer
      v-model="detailVisible"
      title="会话详情"
      size="720px"
      class="detail-drawer"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="selectedConversation" class="detail-content">
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="咨询主题">{{ selectedConversation.subject }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMeta[selectedConversation.status]?.type || 'info'">
              {{ statusMeta[selectedConversation.status]?.label || '未知状态' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="作物">{{ selectedConversation.cropType || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag size="small" effect="plain">{{ priorityMeta[selectedConversation.priority]?.label || '普通' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发起人">{{ selectedConversation.owner?.name || '未知用户' }}</el-descriptions-item>
          <el-descriptions-item label="指派对象">
            {{ selectedConversation.assignedTo?.name || '未指派' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDateTime(selectedConversation.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="最新更新时间" :span="2">
            {{ formatDateTime(selectedConversation.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-description">
          <div class="detail-section-title">问题描述</div>
          <p>{{ selectedConversation.description || '暂无详细描述' }}</p>
        </div>

        <div class="detail-participants" v-if="selectedConversation.participants?.length">
          <div class="detail-section-title">参与人员</div>
          <el-space wrap>
            <el-tag v-for="participant in selectedConversation.participants" :key="participant.userId" type="info" effect="plain">
              {{ participant.name }}（{{ roleMeta[participant.role] || participant.role || '成员' }}）
            </el-tag>
          </el-space>
        </div>

        <div class="detail-thread">
          <MessageThread
            :conversation="selectedConversation"
            :messages="detailMessages"
            :loading="detailLoading"
            :current-user-id="currentUserId"
            :allow-close="canCloseSelected"
            :closing="consultationStore.closing && closingId === selectedConversation.id"
            @close="handleCloseFromThread"
          />
        </div>
      </div>
      <el-empty v-else description="请选择一个会话查看详情" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import MessageThread from '../components/consultation/MessageThread.vue'
import { useConsultationStore } from '../stores/consultation'
import { useAuthStore } from '../stores/auth'

const PAGE_SIZE = 10

const consultationStore = useConsultationStore()
const authStore = useAuthStore()

const filters = reactive({
  status: 'all',
  keyword: ''
})

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '待回复', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已完成', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]

const statusMeta = {
  pending: { label: '待回复', type: 'warning' },
  processing: { label: '处理中', type: 'primary' },
  in_progress: { label: '处理中', type: 'primary' },
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
  FARMER: '农户',
  AGRICULTURE_DEPT: '农业部门',
  ADMIN: '管理员'
}

const tableLoading = computed(() => consultationStore.loadingConversations)
const tableData = computed(() => consultationStore.conversations)
const tablePagination = computed(() => consultationStore.pagination || { page: 1, pageSize: PAGE_SIZE, total: 0 })
const totalPages = computed(() => {
  const total = tablePagination.value.total || 0
  const size = tablePagination.value.pageSize || PAGE_SIZE
  return Math.max(1, Math.ceil(total / size || 1))
})

const summaryMetrics = computed(() => {
  const items = tableData.value || []
  if (!items.length) {
    return { total: tablePagination.value.total || 0, open: 0, closed: 0, avgMessages: 0 }
  }
  const totals = items.reduce(
    (acc, item) => {
      const status = (item.status || '').toLowerCase()
      if (!['resolved', 'closed'].includes(status)) {
        acc.open += 1
      }
      if (status === 'closed') {
        acc.closed += 1
      }
      acc.messages += Number(item.messageCount || 0)
      acc.total += 1
      return acc
    },
    { total: 0, open: 0, closed: 0, messages: 0 }
  )
  const avg = totals.total ? Math.round((totals.messages / totals.total) * 10) / 10 : 0
  return {
    total: tablePagination.value.total || totals.total,
    open: totals.open,
    closed: totals.closed,
    avgMessages: avg
  }
})

const selectedConversationId = ref(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const closingId = ref(null)

const selectedConversation = computed(() => {
  if (!selectedConversationId.value) {
    return null
  }
  return (
    consultationStore.conversations.find(item => item.id === selectedConversationId.value) ||
    consultationStore.activeConversation ||
    null
  )
})

const detailMessages = computed(
  () => consultationStore.messages[selectedConversationId.value] || []
)

const currentUserId = computed(() => authStore.user?.id || authStore.user?.userId || authStore.user?.username)

const canCloseSelected = computed(() => {
  if (!selectedConversation.value) {
    return false
  }
  return (selectedConversation.value.status || '').toLowerCase() !== 'closed'
})

const tableHeaderStyle = () => ({
  background: '#f5f7fa',
  fontWeight: 600,
  color: '#1d3b2f'
})

const truncate = text => {
  if (!text) {
    return ''
  }
  return text.length > 32 ? `${text.slice(0, 32)}…` : text
}

const formatDateTime = value => {
  if (!value) {
    return '—'
  }
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      throw new Error('Invalid date')
    }
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  } catch (error) {
    console.warn('Failed to format time', error)
    return value
  }
}

const buildQueryParams = page => ({
  page,
  pageSize: PAGE_SIZE,
  status: filters.status !== 'all' ? filters.status : undefined,
  keyword: filters.keyword ? filters.keyword.trim() : undefined
})

const loadData = async page => {
  try {
    await consultationStore.loadConversations(buildQueryParams(page))
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '加载咨询列表失败'
    ElMessage.error(message)
  }
}

const handleStatusChange = () => {
  loadData(1)
}

const handleSearch = () => {
  loadData(1)
}

const handleReset = () => {
  filters.status = 'all'
  filters.keyword = ''
  loadData(1)
}

const handleRefresh = () => {
  loadData(tablePagination.value.page || 1)
}

const handlePageChange = page => {
  loadData(page)
}

const isClosed = row => (row?.status || '').toLowerCase() === 'closed'

const openDetail = async row => {
  if (!row?.id) {
    return
  }
  selectedConversationId.value = row.id
  detailVisible.value = true
  detailLoading.value = true
  consultationStore.setActiveConversation(row.id)
  try {
    await consultationStore.loadMessages(row.id)
    consultationStore.markAsRead(row.id)
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '加载对话详情失败'
    ElMessage.error(message)
  } finally {
    detailLoading.value = false
  }
}

const confirmCloseConversation = async consultationId => {
  if (!consultationId) {
    return
  }
  try {
    await ElMessageBox.confirm('确定结束该对话吗？结束后将无法继续发送消息。', '结束对话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  closingId.value = consultationId
  try {
    await consultationStore.closeConversation(consultationId)
    ElMessage.success('对话已结束')
    await loadData(tablePagination.value.page || 1)
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '结束对话失败'
    ElMessage.error(message)
  } finally {
    closingId.value = null
  }
}

const handleCloseRequest = row => {
  if (!row?.id || isClosed(row)) {
    return
  }
  confirmCloseConversation(row.id)
}

const handleCloseFromThread = () => {
  if (!selectedConversation.value) {
    return
  }
  confirmCloseConversation(selectedConversation.value.id)
}

onMounted(() => {
  loadData(1)
})
</script>

<style scoped>
.consultation-management-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px;
}

.summary-card {
  border-radius: 16px;
  background: linear-gradient(135deg, #e8f5e9 0%, #ffffff 60%);
}

.summary-content {
  display: flex;
  gap: 32px;
}

.summary-main {
  flex: 1;
}

.summary-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(11, 61, 46, 0.12);
  color: #0b3d2e;
  font-size: 12px;
  letter-spacing: 1px;
}

.summary-title {
  margin: 16px 0 8px;
  font-size: 26px;
  font-weight: 600;
  color: #1d3b2f;
}

.summary-desc {
  margin: 0 0 24px;
  color: #4f6367;
  line-height: 1.6;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 16px;
}

.stat-item {
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: inset 0 0 0 1px rgba(13, 71, 46, 0.08);
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #0b3d2e;
}

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #607d8b;
}

.summary-side {
  width: 240px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 20px;
  box-shadow: inset 0 0 0 1px rgba(13, 71, 46, 0.08);
}

.side-title {
  font-size: 16px;
  font-weight: 600;
  color: #0b3d2e;
  margin-bottom: 12px;
}

.side-list {
  margin: 0;
  padding-left: 18px;
  color: #546e7a;
  line-height: 1.6;
}

.table-card {
  border-radius: 16px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.table-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d3b2f;
}

.table-subtitle {
  font-size: 13px;
  color: #607d8b;
  margin-top: 4px;
}

.table-filters {
  display: flex;
  align-items: center;
  gap: 12px;
}

.subject-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1d3b2f;
}

.subject-meta {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #607d8b;
}

.subject-description {
  color: #546e7a;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.table-tip {
  font-size: 13px;
  color: #546e7a;
}

.table-pagination {
  display: flex;
  align-items: center;
  gap: 16px;
}

.pagination-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  color: #607d8b;
  gap: 4px;
}

.detail-drawer :deep(.el-drawer__body) {
  padding: 0 24px 24px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.detail-descriptions {
  border-radius: 12px;
  overflow: hidden;
}

.detail-description {
  background: #f8f9fb;
  padding: 16px;
  border-radius: 12px;
  color: #455a64;
}

.detail-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d3b2f;
  margin-bottom: 8px;
}

.detail-participants {
  background: #ffffff;
  padding: 16px;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(13, 71, 46, 0.06);
}

.detail-thread {
  flex: 1;
  min-height: 320px;
  display: flex;
}

.detail-thread :deep(.message-thread) {
  width: 100%;
  border: 1px solid #e0e6ef;
  border-radius: 12px;
  overflow: hidden;
}

@media (max-width: 1280px) {
  .summary-content {
    flex-direction: column;
  }

  .summary-side {
    width: 100%;
  }

  .summary-stats {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }

  .table-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-filters {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
