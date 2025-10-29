<template>
  <div class="weather-analytics-page">
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
        <el-table :data="records" border :header-cell-style="tableHeaderStyle" empty-text="暂无数据">
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

const regionOptions = ref([])
const selectedRegionId = ref(null)
const dateRange = ref([])
const recordLimit = ref(120)
const loading = ref(false)
const records = ref([])
const summary = ref(null)

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

const hasData = computed(() => Array.isArray(records.value) && records.value.length > 0)

const summaryCards = computed(() => {
  if (!summary.value) {
    return []
  }
  const info = summary.value
  return [
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

const temperatureCategories = computed(() => records.value.map(record => formatDate(record.recordDate)))
const maxTemperatureSeries = computed(() => records.value.map(record => record.maxTemperature ?? null))
const minTemperatureSeries = computed(() => records.value.map(record => record.minTemperature ?? null))
const sunshineSeries = computed(() => records.value.map(record => record.sunshineHours ?? 0))

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
  const counts = records.value.reduce((acc, record) => {
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
      data: records.value.map(record => {
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
      selectedRegionId.value = regionOptions.value[0].id
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载地区列表失败')
  }
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
    summary.value = payload?.summary || null
    records.value = Array.isArray(payload?.records)
      ? payload.records.map(item => ({
          ...item,
          recordDate: item.recordDate
        }))
      : []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取气象数据失败')
  } finally {
    loading.value = false
  }
}

watch(selectedRegionId, () => {
  if (selectedRegionId.value) {
    loadWeatherData()
  }
})

watch(dateRange, () => {
  if (selectedRegionId.value) {
    loadWeatherData()
  }
})

onMounted(async () => {
  const today = new Date()
  const start = new Date(today)
  start.setMonth(start.getMonth() - 3)
  dateRange.value = [start.toISOString().slice(0, 10), today.toISOString().slice(0, 10)]
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

.table-card {
  border-radius: 12px;
}
</style>
