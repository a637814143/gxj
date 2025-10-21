<template>
  <div class="log-management-page">
    <el-card class="hero-card" shadow="hover" v-loading="summaryLoading">
      <div class="hero-content">
        <div class="hero-text">
          <div class="hero-badge">安全审计中心</div>
          <h1 class="hero-title">用户登录行为监控</h1>
          <p class="hero-desc">
            统一记录并分析系统用户的登录行为，快速识别异常来源，满足安全合规与审计追踪的需要。
          </p>
          <div class="hero-metrics">
            <div class="metric-item">
              <div class="metric-value">{{ summary.totalCount }}</div>
              <div class="metric-label">累计登录事件</div>
            </div>
            <div class="metric-item">
              <div class="metric-value">{{ summary.successCount }}</div>
              <div class="metric-label">成功登录</div>
            </div>
            <div class="metric-item">
              <div class="metric-value">{{ summary.failureCount }}</div>
              <div class="metric-label">失败登录</div>
            </div>
            <div class="metric-item">
              <div class="metric-value">{{ successRate }}%</div>
              <div class="metric-label">成功率</div>
            </div>
          </div>
        </div>
        <div class="hero-side">
          <div class="side-card">
            <div class="side-title">最新动态</div>
            <ul class="side-list">
              <li>最近成功：{{ formatLatest(summary.lastSuccessUsername, summary.lastSuccessTime) }}</li>
              <li>最近失败：{{ formatLatest(summary.lastFailureUsername, summary.lastFailureTime) }}</li>
            </ul>
            <div class="side-divider" />
            <div class="side-title">风险提示</div>
            <ul class="side-list">
              <li v-for="tip in heroTips" :key="tip">{{ tip }}</li>
            </ul>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="log-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">登录日志列表</div>
            <div class="card-subtitle">支持条件筛选、快速定位异常登录并追踪请求来源</div>
          </div>
          <div class="card-actions">
            <el-button @click="handleRefresh" :loading="loading">刷新</el-button>
            <el-button type="primary" @click="openCreateDialog">手动记录</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="用户名">
          <el-input
            v-model.trim="filters.username"
            placeholder="输入用户名关键词"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="登录结果">
          <el-select v-model="filters.success" placeholder="全部">
            <el-option label="全部" value="all" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failure" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            unlink-panels
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="logs"
        v-loading="loading"
        :header-cell-style="tableHeaderStyle"
        empty-text="暂无登录记录"
      >
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="登录结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="dark">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP 地址" min-width="160">
          <template #default="{ row }">
            {{ row.ipAddress || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="userAgent" label="User-Agent" min-width="220">
          <template #default="{ row }">
            <span class="ua-text" :title="row.userAgent">{{ row.userAgent || '未记录' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="描述" min-width="200">
          <template #default="{ row }">
            {{ row.message || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="记录时间" width="200">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openDetail(row)">查看</el-button>
            <el-button type="warning" text size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="table-info">共 {{ pagination.total }} 条记录，当前第 {{ pagination.page }}/{{ totalPages }} 页</div>
        <el-pagination
          layout="prev, pager, next"
          background
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px" status-icon>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="dialogForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="登录结果" prop="success">
          <el-radio-group v-model="dialogForm.success">
            <el-radio :label="true">成功</el-radio>
            <el-radio :label="false">失败</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="IP 地址" prop="ipAddress">
          <el-input v-model.trim="dialogForm.ipAddress" placeholder="例如 192.168.0.1" />
        </el-form-item>
        <el-form-item label="User-Agent" prop="userAgent">
          <el-input v-model.trim="dialogForm.userAgent" placeholder="浏览器或客户端信息" />
        </el-form-item>
        <el-form-item label="描述信息" prop="message">
          <el-input
            v-model.trim="dialogForm.message"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="补充登录说明，如失败原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="dialogSubmitting" @click="submitDialog">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      title="登录事件详情"
      size="420px"
      :close-on-click-modal="true"
    >
      <div class="detail-content" v-loading="detailLoading">
        <el-descriptions :column="1" border size="large">
          <el-descriptions-item label="用户名">{{ selectedLog?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录结果">
            <el-tag :type="selectedLog?.success ? 'success' : 'danger'" effect="dark">
              {{ selectedLog?.success ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="IP 地址">{{ selectedLog?.ipAddress || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="User-Agent">
            <span class="ua-text">{{ selectedLog?.userAgent || '未记录' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="描述信息">{{ selectedLog?.message || '—' }}</el-descriptions-item>
          <el-descriptions-item label="记录时间">{{ formatDateTime(selectedLog?.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(selectedLog?.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchLoginLogSummary,
  fetchLoginLogs,
  fetchLoginLog,
  createLoginLog,
  updateLoginLog,
  deleteLoginLog
} from '../services/loginLogs'

const PAGE_SIZE = 10

const summaryLoading = ref(false)
const loading = ref(false)

const summary = reactive({
  totalCount: 0,
  successCount: 0,
  failureCount: 0,
  lastSuccessTime: null,
  lastSuccessUsername: null,
  lastFailureTime: null,
  lastFailureUsername: null
})

const filters = reactive({
  username: '',
  success: 'all',
  dateRange: []
})

const logs = ref([])

const pagination = reactive({
  page: 1,
  size: PAGE_SIZE,
  total: 0
})

const totalPages = computed(() => {
  const total = Math.max(pagination.total, 0)
  const size = Math.max(pagination.size, 1)
  return Math.max(1, Math.ceil(total / size))
})

const successRate = computed(() => {
  if (!summary.totalCount) {
    return 0
  }
  return Math.round((summary.successCount / summary.totalCount) * 100)
})

const heroTips = computed(() => {
  const tips = []
  const total = Number(summary.totalCount || 0)
  const success = Number(summary.successCount || 0)
  const failure = Number(summary.failureCount || 0)
  tips.push(`累计采集 ${total} 条登录事件，成功 ${success} 次，失败 ${failure} 次。`)
  if (failure > 0) {
    tips.push('发现失败登录，请关注密码错误或异常来源，必要时进行账号锁定。')
  } else {
    tips.push('暂无失败登录记录，建议继续保持当前的身份认证策略。')
  }
  tips.push('定期导出日志进行备份与审计，满足安全合规要求。')
  return tips
})

const dialogVisible = ref(false)
const dialogMode = ref('create')
const dialogFormRef = ref()
const dialogSubmitting = ref(false)

const dialogForm = reactive({
  id: null,
  username: '',
  success: true,
  ipAddress: '',
  userAgent: '',
  message: ''
})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增登录日志' : '编辑登录日志'))

const dialogRules = {
  username: [
    {
      validator: (_, value, callback) => {
        const text = (value ?? '').toString().trim()
        if (!text) {
          callback(new Error('请输入用户名'))
          return
        }
        if (text.length > 64) {
          callback(new Error('用户名长度不能超过64个字符'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  success: [{ required: true, message: '请选择登录结果', trigger: 'change' }],
  ipAddress: [createOptionalLengthRule(64, 'IP 地址')],
  userAgent: [createOptionalLengthRule(256, 'User-Agent')],
  message: [createOptionalLengthRule(256, '描述信息')]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const selectedLog = ref(null)

function createOptionalLengthRule(max, label) {
  return {
    validator: (_, value, callback) => {
      if (value == null || value === '') {
        callback()
        return
      }
      const text = value.toString().trim()
      if (!text) {
        callback()
        return
      }
      if (text.length > max) {
        callback(new Error(`${label}长度不能超过${max}个字符`))
        return
      }
      callback()
    },
    trigger: ['blur', 'change']
  }
}

function sanitizeOptional(value) {
  if (value == null) {
    return null
  }
  const text = value.toString().trim()
  return text.length ? text : null
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function formatLatest(username, time) {
  if (!time) {
    return '暂无记录'
  }
  const formatted = formatDateTime(time)
  if (!username) {
    return formatted
  }
  return `${username}（${formatted}）`
}

function tableHeaderStyle() {
  return {
    backgroundColor: '#f5f7fa',
    color: '#455a64',
    fontWeight: 500
  }
}

async function refreshSummary() {
  summaryLoading.value = true
  try {
    const data = await fetchLoginLogSummary()
    summary.totalCount = Number(data?.totalCount ?? 0)
    summary.successCount = Number(data?.successCount ?? 0)
    summary.failureCount = Number(data?.failureCount ?? 0)
    summary.lastSuccessTime = data?.lastSuccessTime ?? null
    summary.lastSuccessUsername = data?.lastSuccessUsername ?? null
    summary.lastFailureTime = data?.lastFailureTime ?? null
    summary.lastFailureUsername = data?.lastFailureUsername ?? null
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取登录日志概览失败')
  } finally {
    summaryLoading.value = false
  }
}

async function loadLogs() {
  loading.value = true
  try {
    const params = {
      page: Math.max(pagination.page - 1, 0),
      size: pagination.size
    }
    const keyword = filters.username.trim()
    if (keyword) {
      params.username = keyword
    }
    if (filters.success === 'success') {
      params.success = true
    } else if (filters.success === 'failure') {
      params.success = false
    }
    if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
      const [start, end] = filters.dateRange
      if (start) {
        params.startDate = start
      }
      if (end) {
        params.endDate = end
      }
    }
    const response = await fetchLoginLogs(params)
    logs.value = Array.isArray(response?.records) ? response.records : []
    pagination.total = Number(response?.total ?? 0)
    const currentPage = Number(response?.page ?? params.page)
    pagination.page = currentPage + 1
    pagination.size = Number(response?.size ?? pagination.size)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取登录日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

function resetFilters() {
  filters.username = ''
  filters.success = 'all'
  filters.dateRange = []
  handleSearch()
}

function handlePageChange(page) {
  pagination.page = page
  loadLogs()
}

function handleRefresh() {
  refreshSummary()
  loadLogs()
}

function resetDialogForm() {
  dialogForm.id = null
  dialogForm.username = ''
  dialogForm.success = true
  dialogForm.ipAddress = ''
  dialogForm.userAgent = ''
  dialogForm.message = ''
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetDialogForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  if (!row) {
    return
  }
  dialogMode.value = 'edit'
  dialogForm.id = row.id ?? null
  dialogForm.username = row.username ?? ''
  dialogForm.success = Boolean(row.success)
  dialogForm.ipAddress = row.ipAddress ?? ''
  dialogForm.userAgent = row.userAgent ?? ''
  dialogForm.message = row.message ?? ''
  dialogVisible.value = true
}

async function submitDialog() {
  if (!dialogFormRef.value) {
    return
  }
  try {
    await dialogFormRef.value.validate()
  } catch (validationError) {
    return
  }

  dialogSubmitting.value = true
  const payload = {
    username: dialogForm.username.trim(),
    success: dialogForm.success,
    ipAddress: sanitizeOptional(dialogForm.ipAddress),
    userAgent: sanitizeOptional(dialogForm.userAgent),
    message: sanitizeOptional(dialogForm.message)
  }

  try {
    if (dialogMode.value === 'create') {
      await createLoginLog(payload)
      ElMessage.success('已创建登录日志')
    } else if (dialogForm.id != null) {
      await updateLoginLog(dialogForm.id, payload)
      ElMessage.success('已更新登录日志')
    }
    dialogVisible.value = false
    await Promise.all([refreshSummary(), loadLogs()])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存日志失败')
  } finally {
    dialogSubmitting.value = false
  }
}

async function confirmDelete(row) {
  if (!row?.id) {
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除该登录记录吗？删除后不可恢复。', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteLoginLog(row.id)
    ElMessage.success('已删除登录记录')
    await Promise.all([refreshSummary(), loadLogs()])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除登录记录失败')
  }
}

async function openDetail(row) {
  if (!row?.id) {
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const data = await fetchLoginLog(row.id)
    selectedLog.value = data || row
  } catch (error) {
    selectedLog.value = row
    ElMessage.error(error?.response?.data?.message || '获取登录日志详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  refreshSummary()
  loadLogs()
})
</script>

<style scoped>
.log-management-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 24px;
}

.hero-card {
  background: linear-gradient(120deg, #0b3d2e, #1a6f4a);
  color: #fff;
}

.hero-content {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.hero-text {
  flex: 1;
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  font-size: 13px;
  margin-bottom: 12px;
}

.hero-title {
  font-size: 28px;
  margin-bottom: 8px;
}

.hero-desc {
  font-size: 14px;
  line-height: 1.6;
  max-width: 520px;
  margin-bottom: 24px;
  color: rgba(255, 255, 255, 0.85);
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.metric-item {
  min-width: 120px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  text-align: center;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
}

.metric-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 8px;
}

.hero-side {
  width: 320px;
  display: flex;
  align-items: stretch;
}

.side-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-title {
  font-size: 15px;
  font-weight: 600;
}

.side-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.side-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.2);
  margin: 8px 0;
}

.log-card {
  background: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #263238;
}

.card-subtitle {
  font-size: 13px;
  color: #607d8b;
  margin-top: 4px;
}

.card-actions {
  display: flex;
  gap: 12px;
}

.filter-form {
  margin-bottom: 16px;
}

.ua-text {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.table-info {
  font-size: 13px;
  color: #607d8b;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.detail-content {
  padding: 12px 0;
}

@media (max-width: 1200px) {
  .hero-content {
    flex-direction: column;
  }

  .hero-side {
    width: 100%;
  }
}
</style>
