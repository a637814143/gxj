<template>
  <div class="dashboard-page">
    <section class="hero-card" v-loading="summaryLoading">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">数据管理中心驾驶舱</h1>
        <p class="hero-desc">
          聚焦重点区域与核心作物的生产动态，实时掌握数据导入、模型运行与风险预警情况，让管理决策更加高效可靠。
        </p>
        <div class="hero-stats">
          <div v-for="item in highlightStats" :key="item.label" class="hero-stat">
            <div class="hero-stat-label">{{ item.label }}</div>
            <div class="hero-stat-value">{{ item.value }}</div>
            <div class="hero-stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
      <div class="hero-side">
        <div class="side-card">
          <div class="side-card-title">今日运行速览</div>
          <div class="side-items">
            <div v-for="item in quickOverview" :key="item.label" class="side-item">
              <div class="side-item-label">{{ item.label }}</div>
              <div class="side-item-content">
                <div class="side-item-value">{{ item.value }}</div>
                <div v-if="item.sub" class="side-item-sub">{{ item.sub }}</div>
              </div>
              <div class="side-item-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
                {{ item.trend === null ? '—' : formatTrend(item.trend, item.trendDigits, item.trendLabel) }}
              </div>
            </div>
          </div>
          <div class="side-divider" />
          <div class="side-footer">
            <div class="side-footer-title">重点提醒</div>
            <ul>
              <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
            </ul>
          </div>
        </div>
        <div class="hero-decor" />
      </div>
    </section>

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
                <el-option v-for="item in yearOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="数据来源">
              <el-select v-model="filterForm.source" placeholder="请选择">
                <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
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
            <div class="card-title">历史产量记录</div>
            <div class="card-subtitle">展示已导入的年度作物产量数据，实时同步数据库最新内容</div>
          </div>
          <div class="card-actions">
            <el-button>导出 Excel</el-button>
            <el-button>导出 PDF</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="tableData"
        :border="false"
        :stripe="true"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
        v-loading="recordsLoading"
        empty-text="暂无导入的数据记录"
        row-key="id"
      >
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="regionName" label="地区" min-width="180" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="sownArea" label="播种面积 (公顷)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.sownArea, 1) }}</template>
        </el-table-column>
        <el-table-column prop="production" label="总产量 (吨)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.production, 1) }}</template>
        </el-table-column>
        <el-table-column prop="yieldPerHectare" label="单产 (吨/公顷)" min-width="170">
          <template #default="{ row }">{{ formatNumber(row.yieldPerHectare, 2) }}</template>
        </el-table-column>
        <el-table-column prop="averagePrice" label="平均价格 (元/公斤)" min-width="200">
          <template #default="{ row }">{{ formatNumber(row.averagePrice, 2) }}</template>
        </el-table-column>
        <el-table-column prop="dataSource" label="数据来源" min-width="220" show-overflow-tooltip />
        <el-table-column prop="collectedAt" label="采集日期" width="140">
          <template #default="{ row }">{{ formatDate(row.collectedAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <div class="table-tip">
          共 {{ filteredStats.count }} 条记录，合计产量 {{ formatNumber(filteredStats.totalProduction, 1) }} 吨，播种面积
          {{ formatNumber(filteredStats.totalSownArea, 1) }} 公顷
        </div>
        <el-pagination layout="prev, pager, next" :total="filteredStats.count" :page-size="10" small background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '../services/http'

const summary = ref(null)
const summaryLoading = ref(false)
const yieldRecords = ref([])
const recordsLoading = ref(false)
const searching = ref(false)

const filterForm = reactive({
  crop: 'ALL',
  region: 'ALL',
  year: 'ALL',
  source: 'ALL'
})

const cropOptions = computed(() => {
  const crops = new Map()
  yieldRecords.value.forEach(record => {
    if (record?.crop?.code) {
      crops.set(record.crop.code, record.crop.name)
    }
  })
  const entries = Array.from(crops.entries()).sort((a, b) => a[1].localeCompare(b[1], 'zh-CN'))
  return [
    { label: '全部作物', value: 'ALL' },
    ...entries.map(([value, label]) => ({ label, value }))
  ]
})

const regionOptions = computed(() => {
  const regions = new Map()
  yieldRecords.value.forEach(record => {
    if (record?.region?.code) {
      regions.set(record.region.code, formatRegionName(record.region))
    }
  })
  const entries = Array.from(regions.entries()).sort((a, b) => a[1].localeCompare(b[1], 'zh-CN'))
  return [
    { label: '全部区域', value: 'ALL' },
    ...entries.map(([value, label]) => ({ label, value }))
  ]
})

const yearOptions = computed(() => {
  const years = new Set()
  yieldRecords.value.forEach(record => {
    if (typeof record?.year === 'number') {
      years.add(record.year)
    }
  })
  const sortedYears = Array.from(years).sort((a, b) => b - a)
  return [
    { label: '全部年份', value: 'ALL' },
    ...sortedYears.map(year => ({ label: `${year} 年`, value: year }))
  ]
})

const sourceOptions = computed(() => {
  const sources = new Set()
  yieldRecords.value.forEach(record => {
    if (record?.dataSource) {
      sources.add(record.dataSource)
    }
  })
  const sortedSources = Array.from(sources).sort((a, b) => a.localeCompare(b, 'zh-CN'))
  return [
    { label: '全部来源', value: 'ALL' },
    ...sortedSources.map(source => ({ label: source, value: source }))
  ]
})

const ensureFilterValidity = () => {
  if (!cropOptions.value.some(option => option.value === filterForm.crop)) {
    filterForm.crop = 'ALL'
  }
  if (!regionOptions.value.some(option => option.value === filterForm.region)) {
    filterForm.region = 'ALL'
  }
  if (!yearOptions.value.some(option => option.value === filterForm.year)) {
    filterForm.year = 'ALL'
  }
  if (!sourceOptions.value.some(option => option.value === filterForm.source)) {
    filterForm.source = 'ALL'
  }
}

const fetchSummary = async () => {
  summaryLoading.value = true
  try {
    const { data } = await apiClient.get('/api/dashboard/summary')
    summary.value = data
  } catch (error) {
    summary.value = null
    ElMessage.error(error?.response?.data?.message || '加载概览数据失败')
  } finally {
    summaryLoading.value = false
  }
}

const fetchYieldRecords = async () => {
  recordsLoading.value = true
  try {
    const { data } = await apiClient.get('/api/datasets/yield-records')
    const records = Array.isArray(data?.data) ? data.data : []
    yieldRecords.value = records
    ensureFilterValidity()
  } catch (error) {
    yieldRecords.value = []
    ElMessage.error(error?.response?.data?.message || '加载产量记录失败')
  } finally {
    recordsLoading.value = false
  }
}

const handleSearch = async () => {
  searching.value = true
  try {
    await Promise.all([fetchSummary(), fetchYieldRecords()])
  } finally {
    searching.value = false
  }
}

const handleReset = () => {
  filterForm.crop = 'ALL'
  filterForm.region = 'ALL'
  filterForm.year = 'ALL'
  filterForm.source = 'ALL'
  handleSearch()
}

onMounted(() => {
  handleSearch()
})

const filteredRawRecords = computed(() => {
  const records = [...yieldRecords.value].sort((a, b) => {
    const yearDiff = (b?.year ?? 0) - (a?.year ?? 0)
    if (yearDiff !== 0) return yearDiff
    const regionNameA = formatRegionName(a?.region)
    const regionNameB = formatRegionName(b?.region)
    return regionNameA.localeCompare(regionNameB, 'zh-CN')
  })
  return records.filter(record => {
    const matchesCrop = filterForm.crop === 'ALL' || record?.crop?.code === filterForm.crop
    const matchesRegion = filterForm.region === 'ALL' || record?.region?.code === filterForm.region
    const matchesYear = filterForm.year === 'ALL' || record?.year === filterForm.year
    const matchesSource = filterForm.source === 'ALL' || record?.dataSource === filterForm.source
    return matchesCrop && matchesRegion && matchesYear && matchesSource
  })
})

const tableData = computed(() =>
  filteredRawRecords.value.map(record => ({
    id: record.id,
    year: record.year,
    regionName: formatRegionName(record.region),
    cropName: record?.crop?.name ?? '-',
    sownArea: record?.sownArea,
    production: record?.production,
    yieldPerHectare: record?.yieldPerHectare,
    averagePrice: record?.averagePrice,
    dataSource: record?.dataSource ?? '-',
    collectedAt: record?.collectedAt ?? record?.updatedAt
  }))
)

const filteredStats = computed(() => {
  const records = filteredRawRecords.value
  const totalProduction = records.reduce((sum, record) => sum + (Number(record?.production) || 0), 0)
  const totalSownArea = records.reduce((sum, record) => sum + (Number(record?.sownArea) || 0), 0)
  return {
    count: records.length,
    totalProduction,
    totalSownArea
  }
})

const yearsCovered = computed(() => {
  const years = new Set()
  yieldRecords.value.forEach(record => {
    if (typeof record?.year === 'number') {
      years.add(record.year)
    }
  })
  return years.size
})

const latestYear = computed(() => {
  const years = Array.from(
    new Set(yieldRecords.value.map(record => record?.year).filter(year => typeof year === 'number'))
  )
  if (!years.length) {
    return null
  }
  return Math.max(...years)
})

const highlightStats = computed(() => {
  if (!summary.value) {
    return [
      { label: '累计总产量', value: '—', sub: '等待导入数据' },
      { label: '播种面积', value: '—', sub: '等待导入数据' },
      { label: '平均单产', value: '—', sub: '等待导入数据' },
      { label: '入库记录', value: '—', sub: '等待导入数据' }
    ]
  }

  const averageArea = yearsCovered.value ? summary.value.totalSownArea / yearsCovered.value : 0

  return [
    {
      label: '累计总产量',
      value: `${formatNumber(summary.value.totalProduction, 1)} 吨`,
      sub: `覆盖 ${yearsCovered.value} 个年份`
    },
    {
      label: '播种面积',
      value: `${formatNumber(summary.value.totalSownArea, 1)} 公顷`,
      sub: `年均 ${formatNumber(averageArea, 1)} 公顷`
    },
    {
      label: '平均单产',
      value: `${formatNumber(summary.value.averageYield, 2)} 吨/公顷`,
      sub: '基于全部历史记录计算'
    },
    {
      label: '入库记录',
      value: `${formatNumber(summary.value.recordCount, 0)} 条`,
      sub: latestYear.value ? `最近年份：${latestYear.value} 年` : '等待导入数据'
    }
  ]
})

const quickOverview = computed(() => {
  const items = []
  const trend = summary.value?.productionTrend ?? []
  const lastPoint = trend.length ? trend[trend.length - 1] : null
  const prevPoint = trend.length > 1 ? trend[trend.length - 2] : null
  const forecast = summary.value?.forecastOutlook?.[0]
  const crop = summary.value?.cropStructure?.[0]

  items.push({
    label: '最新年度总产量',
    value: lastPoint ? `${formatNumber(lastPoint.value, 1)} 吨` : '—',
    trend: lastPoint && prevPoint ? lastPoint.value - prevPoint.value : null,
    trendDigits: 1,
    trendLabel: '较上一年'
  })

  items.push({
    label: '主力作物占比',
    value: crop ? `${crop.cropName} ${formatNumber(crop.share * 100, 1)}%` : '—',
    sub: crop ? `播种面积 ${formatNumber(crop.sownArea, 1)} 公顷` : undefined,
    trend: crop ? 0 : null,
    trendDigits: 1,
    trendLabel: '结构'
  })

  items.push({
    label: '次年预测产量',
    value: forecast ? `${formatNumber(forecast.value, 1)} 吨` : '—',
    sub: forecast
      ? `区间 ${formatNumber(forecast.lowerBound, 1)} ~ ${formatNumber(forecast.upperBound, 1)} 吨`
      : undefined,
    trend: forecast && lastPoint ? forecast.value - lastPoint.value : null,
    trendDigits: 1,
    trendLabel: '较当前'
  })

  return items
})

const reminders = computed(() => {
  const items = []
  const region = summary.value?.regionComparisons?.[0]
  const forecast = summary.value?.forecastOutlook?.[0]
  const productionTrend = summary.value?.productionTrend ?? []
  const lastPoint = productionTrend.length ? productionTrend[productionTrend.length - 1] : null

  if (region) {
    items.push(
      `${region.regionName} 当前总产 ${formatNumber(region.production, 1)} 吨，单产 ${formatNumber(
        region.yieldPerHectare,
        2
      )} 吨/公顷。`
    )
  }

  if (forecast && lastPoint) {
    items.push(
      `${forecast.label} 预测约 ${formatNumber(forecast.value, 1)} 吨，相比当前 ${formatNumber(
        lastPoint.value,
        1
      )} 吨${forecast.value >= lastPoint.value ? '有增长' : '略有回落'}。`
    )
  }

  if (!yieldRecords.value.length) {
    items.push('尚未检测到入库数据，请先通过“数据中心”导入数据文件。')
  }

  if (!items.length) {
    items.push('数据加载中，请稍候。')
  }

  return items.slice(0, 3)
})

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

function formatNumber(value, digits = 0) {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  if (Number.isNaN(number)) return '-'
  return number.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  })
}

function formatTrend(value, digits = 0, label = '较上期') {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return `${label} 持平`
  }
  if (Number(value) === 0) {
    return `${label} 持平`
  }
  const absolute = Math.abs(Number(value))
  const formatted = formatNumber(absolute, digits)
  return Number(value) > 0 ? `${label} +${formatted}` : `${label} -${formatted}`
}

function formatDate(value) {
  if (!value) return '-'
  if (typeof value === 'string') {
    return value.split('T')[0]
  }
  if (value instanceof Date) {
    return value.toISOString().split('T')[0]
  }
  return String(value)
}

function formatRegionName(region) {
  if (!region) return '-'
  const name = region?.name ?? '-'
  const parentName = region?.parentName
  if (parentName) {
    return `${parentName} / ${name}`
  }
  return name
}
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
}

.hero-card {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 32px;
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
  width: 400px;
  height: 400px;
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

.hero-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 24px;
}

.side-card {
  border-radius: 20px;
  padding: 24px;
  background: linear-gradient(160deg, rgba(34, 98, 255, 0.92) 0%, rgba(100, 149, 255, 0.8) 65%, rgba(255, 255, 255, 0.95) 100%);
  color: #fff;
  box-shadow: 0 20px 45px rgba(32, 84, 204, 0.25);
  overflow: hidden;
  position: relative;
}

.side-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), transparent 65%);
  pointer-events: none;
}

.side-card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.side-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(2px);
}

.side-item-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.side-item-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.side-item-value {
  font-size: 18px;
  font-weight: 700;
}

.side-item-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
}

.side-item-trend {
  font-size: 12px;
  font-weight: 500;
}

.side-item-trend.up {
  color: #91ffba;
}

.side-item-trend.down {
  color: #ffd48a;
}

.side-divider {
  height: 1px;
  margin: 24px 0;
  background: rgba(255, 255, 255, 0.28);
}

.side-footer-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.side-footer ul {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.hero-decor {
  height: 140px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(214, 228, 255, 0.6));
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.1);
}

.filter-card,
.table-card {
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

.filter-form {
  padding-top: 8px;
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
