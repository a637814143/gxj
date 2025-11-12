<template>
  <div class="weather-analytics-page">
    <div v-if="showWeatherNavigation" class="weather-type-bar">
      <el-tabs v-model="selectedWeatherType" class="weather-type-tabs">
        <el-tab-pane label="全部天气" name="ALL" />
        <el-tab-pane
          v-for="option in weatherTypeOptions"
          :key="option.value"
          :label="option.label"
          :name="option.value"
        />
      </el-tabs>
    </div>
    <el-card class="filter-card" shadow="hover">
      <div class="filter-row">
        <el-select v-model="selectedRegionId" placeholder="选择地区" class="filter-select">
          <el-option v-for="region in regionOptions" :key="region.id" :label="region.label" :value="region.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          unlink-panels
        />
        <el-input-number v-model="recordLimit" :min="30" :max="365" :step="30" class="limit-input" />
        <el-button type="primary" :loading="loading" @click="loadWeatherData">刷新</el-button>
      </div>
    </el-card>

    <el-skeleton v-if="loading" animated :rows="6" class="loading-block" />

    <template v-else>
      <el-row :gutter="16" class="summary-row">
        <el-col v-for="card in summaryCards" :key="card.label" :xs="24" :sm="12" :md="6">
          <el-card class="summary-card" shadow="hover">
            <div class="card-title">{{ card.label }}</div>
            <div class="card-value">{{ card.value }}</div>
            <div v-if="card.sub" class="card-sub">{{ card.sub }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>温度变化趋势</template>
            <BaseChart :option="temperatureOption" :height="320" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>日照时长统计</template>
            <BaseChart :option="sunshineOption" :height="320" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>天气类型占比</template>
            <BaseChart :option="weatherFrequencyOption" :height="320" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>日均温差</template>
            <BaseChart :option="temperatureGapOption" :height="320" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" class="table-card">
        <template #header>逐日气象记录</template>
        <el-table :data="pagedRecords" border :header-cell-style="tableHeaderStyle" empty-text="暂无数据">
          <el-table-column prop="recordDate" label="日期" width="140">
            <template #default="{ row }">{{ formatDate(row.recordDate) }}</template>
          </el-table-column>
          <el-table-column prop="regionName" label="地区" min-width="160" />
          <el-table-column prop="maxTemperature" label="最高温 (℃)" width="150">
            <template #default="{ row }">{{ formatNumber(row.maxTemperature) }}</template>
          </el-table-column>
          <el-table-column prop="minTemperature" label="最低温 (℃)" width="150">
            <template #default="{ row }">{{ formatNumber(row.minTemperature) }}</template>
          </el-table-column>
          <el-table-column prop="weatherText" label="天气" min-width="160" />
          <el-table-column prop="wind" label="风力风向" min-width="160" />
          <el-table-column prop="sunshineHours" label="日照 (小时)" width="150">
            <template #default="{ row }">{{ formatNumber(row.sunshineHours) }}</template>
          </el-table-column>
          <el-table-column prop="dataSource" label="数据来源" min-width="200" show-overflow-tooltip />
        </el-table>
        <div v-if="records.length" class="table-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="records.length"
            hide-on-single-page
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BaseChart from '@/components/charts/BaseChart.vue'
import apiClient from '@/services/http'
import { fetchWeatherDataset } from '@/services/weather'
import { useAuthStore } from '@/stores/auth'

const regionOptions = ref([])
const selectedRegionId = ref(null)
const dateRange = ref([])
const recordLimit = ref(120)
const loading = ref(false)
const records = ref([])
const summary = ref(null)
const suppressRangeWatch = ref(false)
const userCustomizedRange = ref(false)
const currentPage = ref(1)
const pageSize = 10
const selectedWeatherType = ref('ALL')

const authStore = useAuthStore()

const isAdminExperience = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) {
    return false
  }
  if (Array.isArray(roles)) {
    return roles.includes('ADMIN')
  }
  return roles === 'ADMIN'
})

const normalizeWeather = text => {
  if (!text || !String(text).trim()) {
    return '未标注'
  }
  return String(text).trim()
}

const weatherTypeOptions = computed(() => {
  const unique = new Map()
  records.value.forEach(record => {
    const normalized = normalizeWeather(record.weatherText)
    if (!unique.has(normalized)) {
      unique.set(normalized, {
        value: normalized,
        label: normalized
      })
    }
  })
  return Array.from(unique.values())
})

const showWeatherNavigation = computed(
  () => !isAdminExperience.value && weatherTypeOptions.value.length > 0
)

const filteredRecords = computed(() => {
  if (selectedWeatherType.value === 'ALL') {
    return records.value
  }
  return records.value.filter(
    record => normalizeWeather(record.weatherText) === selectedWeatherType.value
  )
})

const pagedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredRecords.value.slice(start, start + pageSize)
})

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

const hasData = computed(
  () => Array.isArray(filteredRecords.value) && filteredRecords.value.length > 0
)

const summaryCards = computed(() => {
  if (!summary.value) {
    return []
  }
  const info = summary.value
  return [
    {
      label: '数据覆盖范围',
      value: formatCoverage(info.firstRecordDate, info.lastRecordDate)
    },
    {
      label: '平均最高温',
      value: formatMetric(info.averageMaxTemperature, '℃')
    },
    {
      label: '平均最低温',
      value: formatMetric(info.averageMinTemperature, '℃')
    },
    {
      label: '累计日照',
      value: formatMetric(info.totalSunshineHours, '小时'),
      sub: formatMetric(info.averageSunshineHours, '平均日照')
    },
    {
      label: '主导天气',
      value: info.dominantWeather ? `${info.dominantWeather}` : '—',
      sub: info.dominantWeatherRatio
        ? `${(info.dominantWeatherRatio * 100).toFixed(1)}% 覆盖`
        : ''
    }
  ]
})

const temperatureCategories = computed(() =>
  filteredRecords.value.map(record => formatDate(record.recordDate))
)
const maxTemperatureSeries = computed(() =>
  filteredRecords.value.map(record => record.maxTemperature ?? null)
)
const minTemperatureSeries = computed(() =>
  filteredRecords.value.map(record => record.minTemperature ?? null)
)
const sunshineSeries = computed(() =>
  filteredRecords.value.map(record => record.sunshineHours ?? 0)
)

const temperatureOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['最高温', '最低温'] },
  xAxis: { type: 'category', data: temperatureCategories.value },
  yAxis: { type: 'value', name: '℃' },
  series: [
    { name: '最高温', type: 'line', smooth: true, data: maxTemperatureSeries.value },
    { name: '最低温', type: 'line', smooth: true, data: minTemperatureSeries.value }
  ]
}))

const sunshineOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: temperatureCategories.value },
  yAxis: { type: 'value', name: '小时' },
  series: [
    {
      name: '日照时长',
      type: 'bar',
      data: sunshineSeries.value,
      itemStyle: { color: '#f6ad55' }
    }
  ]
}))

const weatherFrequencyOption = computed(() => {
  if (!hasData.value) {
    return {
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', data: [] }]
    }
  }
  const counts = filteredRecords.value.reduce((acc, record) => {
    const key = record.weatherText || '未标注'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
  const data = Object.entries(counts).map(([name, value]) => ({ name, value }))
  return {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '天气类型',
        type: 'pie',
        radius: '65%',
        data
      }
    ]
  }
})

const temperatureGapOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: temperatureCategories.value },
  yAxis: { type: 'value', name: '℃' },
  series: [
    {
      name: '日均温差',
      type: 'line',
      smooth: true,
      data: filteredRecords.value.map(record => {
        if (record.maxTemperature == null || record.minTemperature == null) {
          return null
        }
        return Number((record.maxTemperature - record.minTemperature).toFixed(2))
      })
    }
  ]
}))

function formatMetric(value, unit) {
  if (value === null || value === undefined) {
    return '—'
  }
  if (unit === '平均日照') {
    return `${formatNumber(value)} 小时/日`
  }
  return `${formatNumber(value)} ${unit}`
}

function formatCoverage(start, end) {
  if (!start && !end) {
    return '—'
  }
  if (start && end) {
    return `${formatDate(start)} 至 ${formatDate(end)}`
  }
  return formatDate(start || end)
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN', { hour12: false })
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

const fetchRegions = async () => {
  try {
    const { data } = await apiClient.get('/api/base/regions')
    const payload = Array.isArray(data?.data) ? data.data : []
    regionOptions.value = payload.map(item => ({ id: item.id, label: item.name }))
    if (!selectedRegionId.value && regionOptions.value.length) {
      userCustomizedRange.value = false
      selectedRegionId.value = regionOptions.value[0].id
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载地区列表失败')
  }
}

const sanitizeRecords = rawRecords => {
  if (!Array.isArray(rawRecords)) {
    return []
  }
  return rawRecords
    .filter(item => {
      const source = (item?.dataSource ?? '').toString().trim()
      if (!source) {
        return false
      }
      return source !== '示例数据'
    })
    .map(item => ({
      ...item,
      recordDate: item.recordDate
    }))
}

const loadWeatherData = async () => {
  if (!selectedRegionId.value) {
    return
  }
  loading.value = true
  try {
    const params = { regionId: selectedRegionId.value, limit: recordLimit.value }
    if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const { data } = await fetchWeatherDataset(params)
    const payload = data?.data
    const sanitized = sanitizeRecords(payload?.records)
    records.value = sanitized
    summary.value = sanitized.length ? payload?.summary || null : null
    if (!userCustomizedRange.value && summary.value?.firstRecordDate && summary.value?.lastRecordDate) {
      const coverageRange = [summary.value.firstRecordDate, summary.value.lastRecordDate]
      const currentRange = Array.isArray(dateRange.value) ? dateRange.value : []
      if (currentRange[0] !== coverageRange[0] || currentRange[1] !== coverageRange[1]) {
        suppressRangeWatch.value = true
        dateRange.value = coverageRange
      }
    }
    selectedWeatherType.value = 'ALL'
    currentPage.value = 1
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取气象数据失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = page => {
  currentPage.value = page
}

watch(selectedRegionId, newValue => {
  if (newValue) {
    userCustomizedRange.value = false
    loadWeatherData()
  } else {
    records.value = []
    summary.value = null
  }
})

watch(dateRange, () => {
  if (suppressRangeWatch.value) {
    suppressRangeWatch.value = false
    return
  }
  userCustomizedRange.value = true
  if (selectedRegionId.value) {
    loadWeatherData()
  }
})

watch(selectedWeatherType, () => {
  currentPage.value = 1
})

watch(weatherTypeOptions, options => {
  if (
    selectedWeatherType.value !== 'ALL' &&
    !options.some(option => option.value === selectedWeatherType.value)
  ) {
    selectedWeatherType.value = 'ALL'
  }
})

onMounted(async () => {
  await fetchRegions()
  if (selectedRegionId.value) {
    loadWeatherData()
  }
})
</script>

<style scoped>
.weather-analytics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.weather-type-bar {
  background: #ffffff;
  border-radius: 12px;
  padding: 4px 16px 0;
}

.weather-type-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.weather-type-tabs :deep(.el-tabs__nav-wrap) {
  padding: 4px 0;
}

.weather-type-tabs :deep(.el-tabs__nav) {
  gap: 12px;
}

.filter-card {
  border-radius: 12px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.filter-select {
  min-width: 200px;
}

.limit-input {
  width: 140px;
}

.loading-block {
  margin-top: 24px;
}

.summary-row {
  margin-top: 4px;
}

.summary-card {
  border-radius: 12px;
  text-align: left;
  min-height: 120px;
}

.card-title {
  font-size: 14px;
  color: #607d8b;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 8px;
}

.card-sub {
  margin-top: 6px;
  color: #90a4ae;
}

.chart-row {
  margin-top: 8px;
}

.chart-card {
  border-radius: 12px;
  min-height: 360px;
}

.table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.table-card {
  border-radius: 12px;
}
</style>
