<template>
  <div class="dashboard-page">
    <PageHeader
      badge="云惠农作业智能分析系统"
      title="数据管理中心驾驶舱"
      description="聚焦重点区域与核心作物的生产动态，实时掌握数据导入、模型运行与风险预警情况，让管理决策更加高效可靠。"
    >
      <template #meta>
        <div class="header-stat-grid">
          <div v-for="item in highlightStats" :key="item.label" class="header-stat">
            <div class="header-stat-label">{{ item.label }}</div>
            <div class="header-stat-value">{{ item.value }}</div>
            <div class="header-stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </template>
      <template #extra>
        <el-space wrap :size="12">
          <el-button @click="handleExportSummary">导出运行快照</el-button>
          <el-button type="primary" @click="handleCreateForecast">新建预测任务</el-button>
        </el-space>
      </template>
    </PageHeader>

    <div class="top-grid">
      <el-card class="overview-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div>
              <div class="card-title">今日运行速览</div>
              <div class="card-subtitle">实时掌握模型与数据入库情况</div>
            </div>
            <el-tag type="success" effect="light">实时更新</el-tag>
          </div>
        </template>
        <div class="overview-metrics">
          <div v-for="item in quickOverview" :key="item.label" class="overview-item">
            <div class="overview-label">{{ item.label }}</div>
            <div class="overview-value">{{ item.value }}</div>
            <div class="overview-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
              {{ formatTrend(item.trend) }}
            </div>
          </div>
        </div>
        <el-divider />
        <div class="reminder-block">
          <div class="reminder-title">重点提醒</div>
          <ul class="reminder-list">
            <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
          </ul>
        </div>
      </el-card>

      <el-card class="snapshot-card" shadow="hover" v-loading="summaryLoading">
        <template #header>
          <div class="card-header">
            <div>
              <div class="card-title">本月关键里程碑</div>
              <div class="card-subtitle">数据导入任务与模型训练进度</div>
            </div>
          </div>
        </template>
        <div class="empty-state">
          <el-empty description="加载完成后将展示近期关键进展" />
        </div>
      </el-card>
    </div>

    <el-card class="filter-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">查询条件</div>
            <div class="card-subtitle">筛选关键维度，快速定位关注的数据批次</div>
          </div>
          <div class="card-actions">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" :loading="searching" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>
      <el-form :model="filterForm" label-position="top" class="filter-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="作物范围">
              <el-select v-model="filterForm.crop" placeholder="请选择">
                <el-option v-for="item in cropOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="行政区域">
              <el-select v-model="filterForm.region" placeholder="请选择">
                <el-option v-for="item in regionOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="年度批次">
              <el-select v-model="filterForm.year" placeholder="请选择">
                <el-option label="全部年份" :value="null" />
                <el-option v-for="item in yearOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="生产周期">
              <el-select v-model="filterForm.cycle" placeholder="请选择">
                <el-option v-for="item in cycleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">历史预测数据</div>
            <div class="card-subtitle">结合最新导入的基础数据与模型输出，追踪各地区预测表现</div>
          </div>
          <div class="card-actions">
            <el-button @click="handleExportTable('excel')">导出 Excel</el-button>
            <el-button @click="handleExportTable('pdf')">导出 PDF</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="paginatedTableData"
        :border="false"
        :stripe="true"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
        v-loading="summaryLoading"
        empty-text="暂无可用数据"
      >
        <el-table-column prop="year" label="年份" width="110" />
        <el-table-column prop="regionName" label="地区" min-width="180" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column label="播种面积 (公顷)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.sownArea) }}</template>
        </el-table-column>
        <el-table-column label="总产量 (吨)" min-width="150">
          <template #default="{ row }">{{ formatNumber(row.production) }}</template>
        </el-table-column>
        <el-table-column label="单产 (吨/公顷)" min-width="150">
          <template #default="{ row }">{{ formatNumber(row.yieldPerHectare) }}</template>
        </el-table-column>
        <el-table-column label="平均价格 (元/公斤)" min-width="180">
          <template #default="{ row }">{{ formatNumber(row.averagePrice) }}</template>
        </el-table-column>
        <el-table-column label="预计收益 (万元)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.estimatedRevenue) }}</template>
        </el-table-column>
        <el-table-column label="数据日期" width="160">
          <template #default="{ row }">{{ formatDate(row.collectedAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <div class="table-tip">
          <div class="pagination-info">
            <span class="pagination-badge">每页显示 5 条</span>
            <span>分页展示（5 条/页）</span>
            <span>共 {{ filteredTableData.length }} 条，当前第 {{ tablePagination.page }}/{{ totalTablePages }} 页</span>
          </div>
          <div class="tip-extra">可通过筛选条件查看特定地区或作物</div>
        </div>
        <el-pagination
          class="table-pagination"
          :current-page="tablePagination.page"
          :page-size="tablePagination.size"
          :total="filteredTableData.length"
          layout="prev, pager, next"
          background
          @current-change="handleTablePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '../components/PageHeader.vue'
import apiClient from '../services/http'

const summary = ref(null)
const summaryLoading = ref(false)

const router = useRouter()

const TABLE_PAGE_SIZE = 5
const tablePagination = reactive({ page: 1, size: TABLE_PAGE_SIZE })

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => Array.from({ length: 6 }, (_, index) => currentYear - index))

const cropOptions = ref([{ label: '全部作物', value: 'ALL' }])
const regionOptions = ref([{ label: '全部区域', value: 'ALL' }])
const cycleOptions = [
  { label: '全年', value: 'ALL' },
  { label: '春季', value: 'SPRING' },
  { label: '夏季', value: 'SUMMER' },
  { label: '秋季', value: 'AUTUMN' },
  { label: '冬季', value: 'WINTER' }
]

const filterForm = reactive({
  crop: 'ALL',
  region: 'ALL',
  year: null,
  cycle: 'ALL'
})

const highlightStats = computed(() => {
  const data = summary.value
  if (!data) {
    return [
      { label: '总产量', value: '加载中…', sub: '正在请求数据库数据' },
      { label: '播种面积', value: '加载中…', sub: '请稍候' },
      { label: '平均单产', value: '加载中…', sub: '请稍候' },
      { label: '入库记录', value: '加载中…', sub: '等待导入数据' }
    ]
  }
  return [
    {
      label: '总产量',
      value: `${formatNumber(data.totalProduction)} 吨`,
      sub: `覆盖 ${formatInteger(data.recordCount)} 条记录`
    },
    {
      label: '播种面积',
      value: `${formatNumber(data.totalSownArea)} 公顷`,
      sub: '包含所有地区累计数据'
    },
    {
      label: '平均单产',
      value: `${formatNumber(data.averageYield)} 吨/公顷`,
      sub: '基于最新导入数据计算'
    },
    {
      label: '入库记录',
      value: `${formatInteger(data.recordCount)} 条`,
      sub: '导入任务实时汇总'
    }
  ]
})

const quickOverview = computed(() => {
  const data = summary.value
  const cropCount = data?.cropStructure?.length ?? 0
  const regionCount = data?.regionComparisons?.length ?? 0
  const forecastCount = data?.forecastOutlook?.length ?? 0
  return [
    { label: '监测作物', value: `${cropCount} 种`, trend: cropCount ? 1 : 0 },
    { label: '覆盖地区', value: `${regionCount} 个`, trend: regionCount ? 1 : 0 },
    { label: '预测展望', value: `${forecastCount} 期`, trend: forecastCount ? 1 : 0 }
  ]
})

const reminders = computed(() => {
  const data = summary.value
  if (!data) {
    return ['正在加载最新的产量与价格统计…', '请稍候']
  }
  const list = []
  const topCrop = data.cropStructure?.[0]
  if (topCrop) {
    list.push(`产量最高的作物：${topCrop.cropName}，占比 ${formatPercent(topCrop.share)}`)
  }
  const topRegion = data.regionComparisons?.[0]
  if (topRegion) {
    list.push(`${topRegion.regionName} ${formatNumber(topRegion.production)} 吨，平均单产 ${formatNumber(topRegion.yieldPerHectare)} 吨/公顷`)
  }
  const latestRecord = data.recentRecords?.[0]
  if (latestRecord) {
    list.push(`最新入库：${latestRecord.year} 年 ${latestRecord.regionName} ${latestRecord.cropName} 数据`)
  }
  if (!list.length) {
    list.push('暂无需要关注的提醒，欢迎导入最新的作物数据。')
  }
  return list
})

const handleExportSummary = () => {
  ElMessage.info('运行快照导出功能即将上线')
}

const handleCreateForecast = () => {
  router.push({ name: 'forecast' }).catch(() => {})
}

const handleExportTable = format => {
  const label = format === 'pdf' ? 'PDF' : 'Excel'
  ElMessage.info(`${label} 导出功能即将上线`)
}

const tableData = computed(() => summary.value?.recentRecords ?? [])

const filteredTableData = computed(() => {
  let records = tableData.value
  if (filterForm.crop && filterForm.crop !== 'ALL') {
    records = records.filter(record => record.cropName === filterForm.crop)
  }
  if (filterForm.region && filterForm.region !== 'ALL') {
    records = records.filter(record => record.regionName === filterForm.region)
  }
  if (filterForm.year !== null) {
    records = records.filter(record => record.year === Number(filterForm.year))
  }
  return records
})

const paginatedTableData = computed(() => {
  const size = tablePagination.size || TABLE_PAGE_SIZE
  const start = (tablePagination.page - 1) * size
  return filteredTableData.value.slice(start, start + size)
})

const totalTablePages = computed(() => {
  const size = tablePagination.size || TABLE_PAGE_SIZE
  const total = filteredTableData.value.length
  return Math.max(1, Math.ceil((total || 0) / size || 1))
})

const searching = computed(() => summaryLoading.value)

const updateFilterOptions = () => {
  const data = summary.value
  if (!data) {
    cropOptions.value = [{ label: '全部作物', value: 'ALL' }]
    regionOptions.value = [{ label: '全部区域', value: 'ALL' }]
    return
  }
  const crops = Array.from(new Set((data.cropStructure ?? []).map(item => item.cropName).filter(Boolean)))
  cropOptions.value = [{ label: '全部作物', value: 'ALL' }, ...crops.map(name => ({ label: name, value: name }))]
  if (!cropOptions.value.some(option => option.value === filterForm.crop)) {
    filterForm.crop = 'ALL'
  }

  const regions = Array.from(new Set((data.regionComparisons ?? []).map(item => item.regionName).filter(Boolean)))
  regionOptions.value = [{ label: '全部区域', value: 'ALL' }, ...regions.map(name => ({ label: name, value: name }))]
  if (!regionOptions.value.some(option => option.value === filterForm.region)) {
    filterForm.region = 'ALL'
  }

  if (filterForm.year !== null && !yearOptions.value.includes(filterForm.year)) {
    filterForm.year = null
  }
}

const fetchDashboardSummary = async () => {
  summaryLoading.value = true
  try {
    const { data } = await apiClient.get('/api/dashboard/summary')
    summary.value = data ?? null
    updateFilterOptions()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载仪表盘数据失败')
  } finally {
    summaryLoading.value = false
  }
}

const handleSearch = () => {
  tablePagination.page = 1
  fetchDashboardSummary()
}

const handleReset = () => {
  filterForm.crop = 'ALL'
  filterForm.region = 'ALL'
  filterForm.year = null
  filterForm.cycle = 'ALL'
  tablePagination.page = 1
  fetchDashboardSummary()
}

const handleTablePageChange = page => {
  tablePagination.page = page
}

onMounted(() => {
  fetchDashboardSummary()
})

watch(
  () => [filterForm.crop, filterForm.region, filterForm.year],
  () => {
    tablePagination.page = 1
  }
)

watch(
  filteredTableData,
  records => {
    const totalPages = Math.max(1, Math.ceil((records.length || 0) / (tablePagination.size || TABLE_PAGE_SIZE) || 1))
    if (tablePagination.page > totalPages) {
      tablePagination.page = totalPages
    }
    if (tablePagination.page < 1) {
      tablePagination.page = 1
    }
  },
  { immediate: true }
)

function formatNumber(value, fractionDigits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits
  })
}

function formatInteger(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '0'
  }
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${(Number(value) * 100).toFixed(1)}%`
}

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN')
}

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

const tableCellStyle = () => ({
  color: '#1f2d3d',
  fontSize: '13px'
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 40px;
}

.header-stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.header-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--surface-muted, #f2f4f7);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.08);
}

.header-stat-label {
  font-size: 12px;
  color: var(--text-secondary, #667085);
}

.header-stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.header-stat-sub {
  font-size: 12px;
  color: var(--text-muted, #98a2b3);
}

.top-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.overview-card,
.snapshot-card,
.filter-card,
.table-card {
  border: none;
  border-radius: 16px;
  box-shadow: var(--shadow-soft, 0 12px 32px rgba(15, 23, 42, 0.08));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.card-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary, #667085);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  padding: 4px 0 12px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.08);
}

.overview-label {
  font-size: 13px;
  color: var(--text-secondary, #667085);
}

.overview-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.overview-trend {
  font-size: 12px;
  color: #64748b;
}

.overview-trend.up {
  color: #16a34a;
}

.overview-trend.down {
  color: #dc2626;
}

.reminder-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reminder-title {
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.reminder-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text-secondary, #667085);
  font-size: 13px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}

.filter-form {
  padding-top: 8px;
}

.table-card {
  overflow: hidden;
}

.table-footer {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.table-tip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--text-secondary, #667085);
  font-size: 12px;
}

.tip-extra {
  color: var(--text-muted, #98a2b3);
}

.table-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.pagination-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.pagination-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  font-weight: 600;
}

@media (max-width: 992px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions {
    justify-content: flex-end;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .header-stat-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }

  .overview-metrics {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }

  .card-actions {
    justify-content: flex-start;
  }
}
</style>
