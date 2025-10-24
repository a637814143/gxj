<template>
  <div :class="['data-center-page', { 'user-friendly': isUserTheme }]">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">数据资产与导入中心</h1>
        <p class="hero-desc">
          集中管理各类基础与业务数据资产，实时掌握数据导入进度与质量预警，保障模型训练与分析任务稳定运行。
        </p>
        <div class="hero-stats">
          <div v-for="item in highlightStats" :key="item.label" class="hero-stat">
            <div class="hero-stat-label">{{ item.label }}</div>
            <div class="hero-stat-value">{{ item.value }}</div>
            <div class="hero-stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
      <div class="hero-extra">
        <div class="extra-grid">
          <div
            v-for="item in quickOverview"
            :key="item.label"
            class="extra-card"
            :class="{ positive: item.trend > 0, negative: item.trend < 0 }"
          >
            <div class="extra-label">{{ item.label }}</div>
            <div class="extra-value">{{ item.value }}</div>
            <div v-if="item.sub" class="extra-sub">{{ item.sub }}</div>
            <div class="extra-trend">{{ formatTrend(item.trend) }}</div>
          </div>
        </div>
        <div class="extra-card reminder-card">
          <div class="extra-title">操作建议</div>
          <ul class="extra-list">
            <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
          </ul>
        </div>
      </div>
    </section>

    <el-card class="data-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">数据集管理</div>
            <div class="card-subtitle">查看已导入的 Excel 数据文件并维护基础信息</div>
          </div>
          <div class="card-actions">
            <el-button
              type="danger"
              plain
              :disabled="!datasetSelection.length || datasetDeleting"
              :loading="datasetDeleting"
              @click="confirmDeleteDatasets"
            >
              删除数据集
            </el-button>
            <el-button @click="fetchDatasets" :loading="datasetLoading">刷新</el-button>
            <el-button type="primary" @click="openUpload">上传数据</el-button>
          </div>
        </div>
      </template>
      <el-table
        ref="datasetTableRef"
        :data="datasets"
        v-loading="datasetLoading"
        empty-text="暂无导入记录"
        :header-cell-style="tableHeaderStyle"
        row-key="id"
        @selection-change="handleDatasetSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="name" label="数据集名称" min-width="220" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            {{ datasetTypeMap[row.type] ?? row.type ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="200">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
      </el-table>
      <div class="table-footer">
        <div class="table-tip">共 {{ datasets.length }} 个数据集，建议定期核验文件完整性</div>
        <el-button type="primary" link @click="openUpload">立即导入</el-button>
      </div>
    </el-card>

    <el-card v-if="lastImportStats" class="data-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">最新导入预览</div>
            <div class="card-subtitle">
              展示已清洗入库的前 {{ importPreview.length || 0 }} 条记录，并提供自动校验摘要
            </div>
          </div>
        </div>
      </template>
      <div class="import-summary">
        <div v-for="item in importSummaryBadges" :key="item.label" class="summary-item">
          <div class="summary-label">{{ item.label }}</div>
          <div class="summary-value">{{ item.value }}</div>
        </div>
      </div>
      <el-table
        v-if="importPreview.length"
        :data="importPreview"
        :header-cell-style="tableHeaderStyle"
        empty-text="本次导入暂无可预览记录"
      >
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="regionName" label="地区" min-width="160" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="production" label="总产量 (吨)" width="150">
          <template #default="{ row }">
            {{ formatNumber(row.production) }}
          </template>
        </el-table-column>
        <el-table-column prop="yieldPerHectare" label="单产 (吨/公顷)" width="170">
          <template #default="{ row }">
            {{ formatNumber(row.yieldPerHectare) }}
          </template>
        </el-table-column>
        <el-table-column prop="averagePrice" label="平均价格 (元/公斤)" width="190">
          <template #default="{ row }">
            {{ formatNumber(row.averagePrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="collectedAt" label="采集日期" width="180">
          <template #default="{ row }">
            {{ formatDate(row.collectedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="dataSource" label="数据来源" min-width="180" show-overflow-tooltip />
      </el-table>
      <div v-else class="import-preview-empty">本次导入数据已完成清洗并入库，未发现可展示的预览记录。</div>
    </el-card>

    <el-card class="data-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">导入任务记录</div>
            <div class="card-subtitle">追踪历史导入批次与入库结果</div>
          </div>
          <div class="card-actions task-filter-bar">
            <el-button
              type="danger"
              plain
              size="small"
              :disabled="!taskSelection.length || taskDeleting"
              :loading="taskDeleting"
              @click="confirmDeleteTasks"
            >
              删除记录
            </el-button>
            <el-select v-model="taskFilter.status" size="small" class="task-filter-select" @change="applyTaskFilter">
              <el-option label="全部状态" value="ALL" />
              <el-option label="排队中" value="QUEUED" />
              <el-option label="执行中" value="RUNNING" />
              <el-option label="已完成" value="SUCCEEDED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-input
              v-model="taskFilter.keyword"
              size="small"
              placeholder="按名称/文件搜索"
              clearable
              class="task-filter-input"
              @clear="applyTaskFilter"
              @keyup.enter.native="applyTaskFilter"
            >
              <template #append>
                <el-button size="small" @click="applyTaskFilter">搜索</el-button>
              </template>
            </el-input>
            <el-button size="small" @click="resetTaskFilter">重置</el-button>
            <el-button size="small" @click="fetchImportTasks" :loading="taskLoading">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table
        ref="taskTableRef"
        :data="importTasks"
        v-loading="taskLoading"
        empty-text="暂无导入任务"
        :header-cell-style="tableHeaderStyle"
        highlight-current-row
        :current-row-key="selectedTaskId"
        row-key="taskId"
        @row-click="handleTaskRowClick"
        @selection-change="handleTaskSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="datasetName" label="任务/数据集" min-width="220">
          <template #default="{ row }">
            <div class="task-name">{{ row.datasetName ?? '-' }}</div>
            <div class="task-filename">{{ row.originalFilename ?? '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ formatStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            {{ formatProgress(row.progress) }}
          </template>
        </el-table-column>
        <el-table-column label="处理情况" min-width="180">
          <template #default="{ row }">
            新增 {{ row.insertedRows ?? 0 }} · 更新 {{ row.updatedRows ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="失败/跳过" width="140">
          <template #default="{ row }">
            {{ row.failedRows ?? 0 }} / {{ row.skippedRows ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="warningCount" label="预警" width="90" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="完成时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.finishedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="message" label="任务摘要" min-width="240" show-overflow-tooltip />
      </el-table>
      <div class="task-pagination-area">
        <div class="pagination-info">
          <span class="pagination-badge">每页显示 5 条</span>
          <span>分页展示（5 条/页）</span>
          <span>共 {{ taskPagination.total }} 条，当前第 {{ taskPagination.page }}/{{ taskTotalPages }} 页</span>
        </div>
        <el-pagination
          class="task-pagination"
          :current-page="taskPagination.page"
          :page-size="taskPagination.size"
          :total="taskPagination.total"
          layout="prev, pager, next"
          background
          @current-change="handleTaskPageChange"
        />
      </div>
      <div v-if="importWarnings.length" class="warnings">
        <el-alert
          v-for="(warning, index) in importWarnings"
          :key="`${index}-${warning}`"
          type="warning"
          :title="warning"
          show-icon
          :closable="false"
        />
      </div>
      <div v-if="selectedTaskDetail?.errors?.length" class="error-summary">
        <el-alert
          v-for="error in selectedTaskDetail.errors"
          :key="`${error.rowNumber}-${error.message}`"
          type="error"
          show-icon
          :closable="false"
        >
          <template #title>
            第 {{ error.rowNumber > 0 ? error.rowNumber : '-' }} 行：{{ error.message }}
          </template>
          <template v-if="error.rawValue" #description>
            原始值：{{ error.rawValue }}
          </template>
        </el-alert>
      </div>
    </el-card>

    <el-dialog
      v-model="uploadDialogVisible"
      class="upload-dialog"
      title="导入数据文件"
      width="520px"
      @closed="resetUploadForm"
    >
      <el-form label-width="88px" class="upload-form">
        <el-form-item label="数据类型">
          <el-select v-model="uploadForm.type">
            <el-option v-for="item in datasetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集名称">
          <el-input v-model="uploadForm.name" placeholder="默认使用文件名" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            placeholder="可选，最长 256 字"
            :rows="3"
            maxlength="256"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            class="upload-block"
            drag
            :auto-upload="false"
            :file-list="uploadFileList"
            accept=".xls,.xlsx,.csv,text/csv"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 Excel（.xls/.xlsx）和 UTF-8 编码的 CSV（.csv）文件，系统会自动识别常见的分隔符。
              </div>
              <ul class="upload-tip-list">
                <li>CSV 将自动检测逗号、制表符、分号或竖线分隔。</li>
                <li>请保留模板中的字段名称（如“作物”“地区”“年份”等），便于系统匹配。</li>
              </ul>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false" :disabled="uploading">取 消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading" :disabled="uploadFileList.length === 0">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import apiClient from '../services/http'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isUserTheme = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) return true
  if (Array.isArray(roles)) {
    return !roles.includes('ADMIN')
  }
  return roles !== 'ADMIN'
})

const TASK_PAGE_SIZE = 5

const datasets = ref([])
const datasetLoading = ref(false)

const datasetTableRef = ref(null)
const datasetSelection = ref([])
const datasetDeleting = ref(false)

const clearDatasetSelection = () => {
  datasetSelection.value = []
  nextTick(() => {
    datasetTableRef.value?.clearSelection?.()
  })
}

const handleDatasetSelectionChange = selection => {
  datasetSelection.value = Array.isArray(selection) ? selection : []
}

const deleteSelectedDatasets = async () => {
  if (!datasetSelection.value.length) {
    return
  }
  const ids = datasetSelection.value
    .map(item => item?.id)
    .filter(id => typeof id === 'number' || typeof id === 'string')
    .map(id => Number(id))
    .filter(id => !Number.isNaN(id))

  if (!ids.length) {
    ElMessage.warning('未找到所选数据集的标识，无法删除')
    return
  }

  datasetDeleting.value = true
  try {
    await apiClient.delete('/api/datasets/files', { data: ids })
    ElMessage.success('已删除选中的数据集')
    clearDatasetSelection()
    await Promise.all([fetchDatasets(), fetchImportTasks(true)])
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除数据集失败')
  } finally {
    datasetDeleting.value = false
  }
}

const confirmDeleteDatasets = () => {
  if (!datasetSelection.value.length || datasetDeleting.value) {
    return
  }
  ElMessageBox.confirm(
    '删除后将同步清理对应的入库数据，相关导入任务记录将标记为已归档，可在列表中删除。是否继续？',
    '删除数据集',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(() => deleteSelectedDatasets())
    .catch(() => {})
}

const importTasks = ref([])
const taskLoading = ref(false)
const taskTableRef = ref(null)
const taskSelection = ref([])
const taskDeleting = ref(false)
const taskFilter = reactive({ status: 'ALL', keyword: '' })
const taskPagination = reactive({ page: 1, size: TASK_PAGE_SIZE, total: 0 })
const taskTotalPages = computed(() => {
  const total = Math.max(taskPagination.total, 0)
  const size = taskPagination.size || TASK_PAGE_SIZE
  return Math.max(1, Math.ceil(total / size || 1))
})

const selectedTaskId = ref('')
const selectedTaskDetail = ref(null)

const importWarnings = ref([])
const importPreview = ref([])
const lastImportStats = ref(null)

const datasetTypeMap = {
  YIELD: '产量',
  PRICE: '价格',
  WEATHER: '气象',
  SOIL: '土壤'
}

const datasetTypeOptions = [
  { value: 'YIELD', label: '产量' },
  { value: 'PRICE', label: '价格' },
  { value: 'WEATHER', label: '气象' },
  { value: 'SOIL', label: '土壤' }
]

const statusDisplayMap = {
  QUEUED: '排队中',
  RUNNING: '执行中',
  SUCCEEDED: '已完成',
  FAILED: '失败'
}

const uploadDialogVisible = ref(false)
const uploadFileList = ref([])
const uploading = ref(false)
const uploadForm = reactive({
  type: 'YIELD',
  name: '',
  description: ''
})

let taskPollTimer = null

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatNumber(value) {
  if (value === null || value === undefined) {
    return '-'
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return value
  }
  return number.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function formatStatus(status) {
  return statusDisplayMap[status] ?? status ?? '-'
}

function statusTagType(status) {
  switch (status) {
    case 'SUCCEEDED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
      return 'warning'
    default:
      return 'info'
  }
}

function formatProgress(progress) {
  const value = Number(progress ?? 0)
  return `${Math.round(value * 100)}%`
}

function formatTrend(value) {
  if (value > 0) return '状态提升'
  if (value < 0) return '请关注'
  return '正常'
}

const highlightStats = computed(() => {
  const totalDatasets = datasets.value.length
  const typeCount = totalDatasets
    ? new Set(datasets.value.map(item => datasetTypeMap[item.type] ?? item.type ?? '其他')).size
    : 0
  const latestDataset = datasets.value
    .slice()
    .sort((a, b) => new Date(b?.updatedAt ?? 0).getTime() - new Date(a?.updatedAt ?? 0).getTime())[0]
  const summary = selectedTaskDetail.value?.summary
  const totalTasks = taskPagination.total
  const latestTask = importTasks.value[0]
  return [
    {
      label: '已导入数据集',
      value: `${totalDatasets} 个`,
      sub: typeCount ? `覆盖 ${typeCount} 类业务主题` : '等待导入基础数据'
    },
    {
      label: '当前任务状态',
      value: summary ? formatStatus(summary.status) : '暂无任务',
      sub: summary
        ? `进度 ${formatProgress(summary.progress ?? 0)}`
        : '点击右侧按钮上传'
    },
    {
      label: '历史导入批次',
      value: `${totalTasks} 次`,
      sub: latestTask ? `最新任务：${formatDate(latestTask.createdAt)}` : '尚未同步导入记录'
    }
  ]
})

const quickOverview = computed(() => {
  const stats = lastImportStats.value
  const inserted = stats?.insertedRows ?? 0
  const updated = stats?.updatedRows ?? 0
  const processed = stats?.processedRows ?? 0
  return [
    {
      label: '任务状态',
      value: formatStatus(stats?.status),
      trend: stats?.status === 'FAILED' ? -1 : stats?.status === 'SUCCEEDED' ? 1 : 0
    },
    {
      label: '当前进度',
      value: formatProgress(stats?.progress ?? 0),
      trend: (stats?.progress ?? 0) > 0 ? 1 : 0
    },
    {
      label: '本批数据',
      value: processed ? `${processed} 条` : '0 条',
      sub: `新增 ${inserted} · 更新 ${updated}`,
      trend: processed ? 1 : 0
    }
  ]
})

const importSummaryBadges = computed(() => {
  if (!lastImportStats.value) {
    return []
  }
  const summary = lastImportStats.value
  return [
    { label: '识别总行数', value: `${summary.totalRows ?? 0} 行` },
    { label: '已处理', value: `${summary.processedRows ?? 0} 条` },
    { label: '新增入库', value: `${summary.insertedRows ?? 0} 条` },
    { label: '更新覆盖', value: `${summary.updatedRows ?? 0} 条` },
    { label: '跳过记录', value: `${summary.skippedRows ?? 0} 条` },
    { label: '失败记录', value: `${summary.failedRows ?? 0} 条` }
  ]
})

const reminders = [
  '建议每周核对一次数据来源与口径',
  '导入前请确保文件格式符合模板要求',
  '完成导入后及时通知模型团队更新训练集'
]

const fetchDatasets = async () => {
  datasetLoading.value = true
  try {
    const { data } = await apiClient.get('/api/datasets/files')
    datasets.value = data?.data ?? []
    clearDatasetSelection()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载数据集列表失败')
  } finally {
    datasetLoading.value = false
  }
}

const clearTaskSelection = () => {
  taskSelection.value = []
  nextTick(() => {
    taskTableRef.value?.clearSelection?.()
  })
}

const handleTaskSelectionChange = selection => {
  taskSelection.value = Array.isArray(selection) ? selection : []
}

const fetchImportTasks = async (silent = false) => {
  taskLoading.value = true
  try {
    const params = {
      page: taskPagination.page - 1,
      size: taskPagination.size
    }
    if (taskFilter.status && taskFilter.status !== 'ALL') {
      params.status = taskFilter.status
    }
    if (taskFilter.keyword.trim()) {
      params.keyword = taskFilter.keyword.trim()
    }
    const { data } = await apiClient.get('/api/data-import/tasks', { params })
    const payload = data?.data ?? {}
    const totalElements = Number(payload.totalElements ?? 0)
    const totalPageCount = Math.max(1, Math.ceil(Math.max(totalElements, 0) / TASK_PAGE_SIZE))
    if (taskPagination.page > totalPageCount && totalElements > 0) {
      taskPagination.page = totalPageCount
      await fetchImportTasks(true)
      return
    }
    importTasks.value = Array.isArray(payload.content) ? payload.content : []
    clearTaskSelection()
    taskPagination.total = totalElements
    taskPagination.size = TASK_PAGE_SIZE
    const serverPage = Number(payload.number ?? taskPagination.page - 1)
    taskPagination.page = Math.min(Math.max(serverPage + 1, 1), totalPageCount)
    const currentExists = importTasks.value.some(item => item.taskId === selectedTaskId.value)
    if (!importTasks.value.length) {
      selectedTaskId.value = ''
      await fetchTaskDetail('', true)
    } else if (!currentExists) {
      selectedTaskId.value = importTasks.value[0].taskId
      await fetchTaskDetail(selectedTaskId.value, true)
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error(error?.response?.data?.message || '加载导入任务失败')
    }
  } finally {
    taskLoading.value = false
  }
}

const fetchTaskDetail = async (taskId, silent = false) => {
  if (!taskId) {
    selectedTaskDetail.value = null
    lastImportStats.value = null
    importWarnings.value = []
    importPreview.value = []
    return
  }
  try {
    const { data } = await apiClient.get(`/api/data-import/tasks/${taskId}`)
    const detail = data?.data
    selectedTaskDetail.value = detail
    const summary = detail?.summary
    if (summary) {
      lastImportStats.value = {
        status: summary.status,
        progress: summary.progress,
        totalRows: Number(summary.totalRows ?? 0),
        processedRows: Number(summary.processedRows ?? 0),
        insertedRows: Number(summary.insertedRows ?? 0),
        updatedRows: Number(summary.updatedRows ?? 0),
        skippedRows: Number(summary.skippedRows ?? 0),
        failedRows: Number(summary.failedRows ?? 0),
        warningCount: Number(summary.warningCount ?? 0)
      }
    } else {
      lastImportStats.value = null
    }
    importWarnings.value = Array.isArray(detail?.warnings) ? detail.warnings : []
    importPreview.value = Array.isArray(detail?.preview) ? detail.preview : []
  } catch (error) {
    if (!silent) {
      ElMessage.error(error?.response?.data?.message || '获取任务详情失败')
    }
  }
}

const startTaskPolling = () => {
  stopTaskPolling()
  taskPollTimer = window.setInterval(() => {
    if (selectedTaskId.value) {
      fetchTaskDetail(selectedTaskId.value, true)
    }
  }, 5000)
}

const stopTaskPolling = () => {
  if (taskPollTimer) {
    clearInterval(taskPollTimer)
    taskPollTimer = null
  }
}

watch(
  () => selectedTaskDetail.value?.summary?.status,
  (status, prevStatus) => {
    if (status === 'RUNNING' || status === 'QUEUED') {
      startTaskPolling()
    } else {
      stopTaskPolling()
      if (status && status !== prevStatus && (status === 'SUCCEEDED' || status === 'FAILED')) {
        fetchImportTasks(true)
        if (status === 'SUCCEEDED') {
          fetchDatasets()
        }
      }
    }
  }
)

const applyTaskFilter = async () => {
  taskPagination.page = 1
  await fetchImportTasks()
  if (selectedTaskId.value) {
    await fetchTaskDetail(selectedTaskId.value, true)
  }
}

const resetTaskFilter = async () => {
  taskFilter.status = 'ALL'
  taskFilter.keyword = ''
  taskPagination.page = 1
  await fetchImportTasks()
  if (selectedTaskId.value) {
    await fetchTaskDetail(selectedTaskId.value, true)
  }
}

const handleTaskPageChange = async page => {
  taskPagination.page = page
  await fetchImportTasks(true)
  if (selectedTaskId.value) {
    await fetchTaskDetail(selectedTaskId.value, true)
  }
}

const handleTaskRowClick = async row => {
  if (!row?.taskId) return
  selectedTaskId.value = row.taskId
  await fetchTaskDetail(row.taskId)
}

const deleteSelectedTasks = async () => {
  if (!taskSelection.value.length) {
    return
  }
  const ids = taskSelection.value
    .map(item => item?.taskId)
    .filter(id => typeof id === 'string' && id.trim())

  if (!ids.length) {
    ElMessage.warning('未找到所选任务的标识，无法删除')
    return
  }

  taskDeleting.value = true
  try {
    await apiClient.delete('/api/data-import/tasks', { data: ids })
    ElMessage.success('已删除选中的导入任务记录')
    clearTaskSelection()
    if (ids.includes(selectedTaskId.value)) {
      selectedTaskId.value = ''
      selectedTaskDetail.value = null
    }
    await fetchImportTasks(true)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除导入任务记录失败')
  } finally {
    taskDeleting.value = false
  }
}

const confirmDeleteTasks = () => {
  if (!taskSelection.value.length || taskDeleting.value) {
    return
  }
  ElMessageBox.confirm('删除后将无法恢复所选导入任务记录，确定继续？', '删除记录', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => deleteSelectedTasks())
    .catch(() => {})
}

const beforeUpload = file => {
  const lowerName = file.name?.toLowerCase?.() ?? ''
  const mimeType = file.type ?? ''
  const isExcel = lowerName.endsWith('.xls') || lowerName.endsWith('.xlsx') || mimeType.includes('sheet')
  const isCsv = lowerName.endsWith('.csv') || mimeType.includes('csv')
  if (!(isExcel || isCsv)) {
    ElMessage.error('仅支持 Excel（.xls/.xlsx）或 UTF-8 编码的 CSV（.csv）文件')
  }
  return isExcel || isCsv
}

const handleFileChange = (file, fileList) => {
  const target = file.raw ?? file
  if (!beforeUpload(target)) {
    uploadFileList.value = []
    return
  }
  uploadFileList.value = fileList.slice(-1)
}

const handleFileRemove = file => {
  uploadFileList.value = uploadFileList.value.filter(item => item.uid !== file.uid)
}

const submitUpload = async () => {
  if (!uploadFileList.value.length) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  const [fileItem] = uploadFileList.value
  const formData = new FormData()
  formData.append('file', fileItem.raw)
  formData.append('type', uploadForm.type)
  if (uploadForm.name.trim()) {
    formData.append('name', uploadForm.name.trim())
  }
  if (uploadForm.description.trim()) {
    formData.append('description', uploadForm.description.trim())
  }

  uploading.value = true
  try {
    const { data } = await apiClient.post('/api/data-import/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const result = data?.data
    if (result?.taskId) {
      selectedTaskId.value = result.taskId
      ElMessage.success('导入任务已提交，系统正在后台处理')
    } else {
      ElMessage.success('导入任务已提交')
    }
    uploadDialogVisible.value = false
    importWarnings.value = []
    importPreview.value = []
    await fetchImportTasks(true)
    if (result?.taskId) {
      await fetchTaskDetail(result.taskId)
    }
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '导入失败'
    ElMessage.error(message)
  } finally {
    uploading.value = false
  }
}

const openUpload = () => {
  uploadDialogVisible.value = true
  importWarnings.value = []
}

const resetUploadForm = () => {
  uploadForm.type = 'YIELD'
  uploadForm.name = ''
  uploadForm.description = ''
  uploadFileList.value = []
}

onMounted(async () => {
  await Promise.all([fetchDatasets(), fetchImportTasks(true)])
  if (selectedTaskId.value) {
    await fetchTaskDetail(selectedTaskId.value, true)
  }
})

onBeforeUnmount(() => {
  stopTaskPolling()
})
</script>

<style scoped>
.data-center-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
}

.hero-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 32px;
  border-radius: 24px;
  background: linear-gradient(120deg, #e8f1ff 0%, #f4f8ff 35%, #ffffff 100%);
  box-shadow: 0 28px 60px rgba(51, 112, 255, 0.15);
  overflow: hidden;
}

.hero-card::before {
  content: '';
  position: absolute;
  top: -160px;
  right: -160px;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle at center, rgba(60, 132, 255, 0.35), transparent 65%);
  transform: rotate(12deg);
}

.hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  font-size: 13px;
  border-radius: 999px;
  color: #2262ff;
  background: rgba(34, 98, 255, 0.12);
  font-weight: 600;
  letter-spacing: 1px;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  color: #0d1b3d;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #455471;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.hero-stat {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.08);
  backdrop-filter: blur(4px);
}

.hero-stat-label {
  font-size: 12px;
  color: #5c6d8d;
  margin-bottom: 8px;
}

.hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #163b8c;
  margin-bottom: 6px;
}

.hero-stat-sub {
  font-size: 12px;
  color: #6e7fa1;
}

.hero-extra {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .hero-extra {
    grid-template-columns: 1fr;
  }
}

.extra-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.extra-card {
  border-radius: 18px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.08);
  backdrop-filter: blur(4px);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.extra-card.positive {
  border-left: 4px solid rgba(45, 212, 191, 0.9);
}

.extra-card.negative {
  border-left: 4px solid rgba(248, 113, 113, 0.85);
}

.extra-label {
  font-size: 13px;
  color: #52617f;
}

.extra-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a2a4a;
}

.extra-sub {
  font-size: 12px;
  color: #64748b;
}

.extra-trend {
  font-size: 12px;
  color: #64748b;
}

.extra-card.positive .extra-trend {
  color: #0f766e;
}

.extra-card.negative .extra-trend {
  color: #be123c;
}

.reminder-card {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(14, 165, 233, 0.12));
  border: 1px solid rgba(14, 165, 233, 0.15);
}

.extra-title {
  font-size: 15px;
  font-weight: 600;
  color: #163b8c;
}

.extra-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #42526b;
}

.task-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.task-filter-select {
  width: 120px;
}

.task-filter-input {
  width: 240px;
}

.task-name {
  font-weight: 600;
  color: #3c4b66;
}

.task-filename {
  font-size: 12px;
  color: #8a94ad;
  margin-top: 2px;
}

.task-pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.task-pagination {
  display: flex;
  justify-content: flex-end;
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #607d8b;
  font-size: 12px;
}

.pagination-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #e3f2fd;
  color: #1565c0;
  font-weight: 600;
  font-size: 12px;
}

.error-summary {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}


.data-card {
  border-radius: 20px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a2a4a;
}

.card-subtitle {
  font-size: 13px;
  color: #6c7d9c;
  margin-top: 4px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #5f6f8c;
  font-size: 12px;
}

.table-tip {
  display: flex;
  align-items: center;
  gap: 6px;
}

.import-summary {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(34, 98, 255, 0.08);
  border: 1px solid rgba(34, 98, 255, 0.12);
}

.summary-label {
  font-size: 12px;
  color: #4b5d82;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #1841a1;
}

.import-preview-empty {
  padding: 20px;
  border-radius: 14px;
  background: rgba(245, 247, 252, 0.9);
  color: #5a6c8f;
  text-align: center;
  font-size: 13px;
}

.warnings {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.upload-form {
  padding-right: 8px;
}

.upload-block {
  width: 100%;
}

.upload-icon {
  font-size: 32px;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.upload-tip-list {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #5a6c8f;
  font-size: 12px;
  line-height: 1.6;
}

.upload-tip-list li + li {
  margin-top: 4px;
}

.upload-dialog {
  --el-dialog-body-padding: 20px 24px 24px;
}

.upload-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 4px;
}

.upload-dialog :deep(.el-form-item) {
  margin-bottom: 16px;
}

/* User theme enhancements */
.data-center-page.user-friendly {
  position: relative;
  padding: 4px 0 40px;
  background: radial-gradient(circle at 15% 20%, rgba(129, 212, 250, 0.22), transparent 55%),
    radial-gradient(circle at 80% 80%, rgba(244, 143, 177, 0.18), transparent 60%),
    linear-gradient(150deg, #f5fbff 0%, #fef7ff 50%, #ffffff 100%);
}

.data-center-page.user-friendly::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.45), transparent 65%);
  pointer-events: none;
}

.data-center-page.user-friendly .hero-card {
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: linear-gradient(115deg, rgba(229, 246, 255, 0.95) 0%, rgba(245, 232, 255, 0.9) 50%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 28px 68px rgba(136, 132, 255, 0.18);
}

.data-center-page.user-friendly .hero-card::before {
  background: radial-gradient(circle at center, rgba(129, 212, 250, 0.45), transparent 65%);
}

.data-center-page.user-friendly .hero-stat {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(233, 246, 255, 0.85));
  box-shadow: 0 16px 36px rgba(102, 126, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.data-center-page.user-friendly .extra-card {
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.92), rgba(231, 246, 255, 0.88));
  box-shadow: 0 18px 36px rgba(148, 163, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.data-center-page.user-friendly .reminder-card {
  background: linear-gradient(135deg, rgba(165, 180, 252, 0.2), rgba(110, 231, 183, 0.18));
  border: 1px solid rgba(255, 255, 255, 0.55);
}

.data-center-page.user-friendly .extra-title {
  color: #4338ca;
}

.data-center-page.user-friendly .extra-list {
  color: #4c1d95;
}

.data-center-page.user-friendly .data-card {
  border: none;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.92) 0%, rgba(237, 248, 255, 0.9) 60%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 28px 54px rgba(148, 163, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.data-center-page.user-friendly :deep(.data-card .el-card__header) {
  background: linear-gradient(120deg, rgba(230, 244, 255, 0.82), rgba(255, 255, 255, 0.88));
  border-bottom: 1px solid rgba(209, 213, 255, 0.3);
}

.data-center-page.user-friendly :deep(.data-card .el-card__body) {
  background: transparent;
}

.data-center-page.user-friendly .summary-item {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(235, 245, 255, 0.88));
  border: 1px solid rgba(209, 213, 255, 0.4);
  box-shadow: 0 14px 30px rgba(143, 155, 255, 0.18);
}

.data-center-page.user-friendly .pagination-badge {
  background: rgba(129, 212, 250, 0.2);
  color: #0f766e;
}

@media (max-width: 992px) {
  .hero-card {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .card-actions {
    justify-content: flex-start;
  }
}
</style>
