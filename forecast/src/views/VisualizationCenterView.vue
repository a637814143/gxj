<template>
  <div class="visualization-page">
    <section class="overview-card">
      <div class="overview-copy">
        <div class="overview-badge">云惠农作业智能分析系统</div>
        <h1 class="overview-title">数据可视化洞察中心</h1>
        <p class="overview-desc">
          从时间趋势、结构构成与区域分布三个维度，为业务团队提供对农情数据的动态感知能力，助力制定科学的种植与调度策略。
        </p>
        <div class="overview-stats">
          <div v-for="item in highlightStats" :key="item.label" class="overview-stat">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
      <div class="overview-hint">
        <div class="hint-title">智能洞察建议</div>
        <ul>
          <li v-for="tip in insightTips" :key="tip">{{ tip }}</li>
        </ul>
      </div>
    </section>

    <el-alert
      v-if="fetchError"
      class="page-alert"
      type="error"
      :closable="false"
      show-icon
      :description="fetchError"
    />

    <div class="page-actions">
      <div class="actions-info">{{ datasetSummaryText }}</div>
      <el-button type="primary" :loading="isLoading" @click="loadYieldRecords">刷新可视化数据</el-button>
    </div>

    <el-row :gutter="24" class="chart-grid">
      <el-col :md="12" :sm="24">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <template #header>
            <div class="chart-header">
              <div>
                <div class="chart-title">产量趋势分析</div>
                <div class="chart-subtitle">{{ trendSubtitle }}</div>
              </div>
            </div>
          </template>
          <div class="chart-body">
            <div class="chart-wrapper">
              <div ref="trendChartRef" class="chart" />
              <el-empty
                v-if="!isLoading && !trendHasData"
                description="暂无可视化数据"
                :image-size="80"
                class="chart-empty"
              />
            </div>
            <div v-if="trendSummaryText" class="chart-summary">{{ trendSummaryText }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :md="12" :sm="24">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <template #header>
            <div class="chart-header">
              <div>
                <div class="chart-title">种植结构占比</div>
                <div class="chart-subtitle">{{ structureSubtitle }}</div>
              </div>
            </div>
          </template>
          <div class="chart-body">
            <div class="chart-wrapper">
              <div ref="structureChartRef" class="chart" />
              <el-empty
                v-if="!isLoading && !structureHasData"
                description="暂无可视化数据"
                :image-size="80"
                class="chart-empty"
              />
            </div>
            <div v-if="structureSummaryText" class="chart-summary">{{ structureSummaryText }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="chart-grid">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <template #header>
            <div class="chart-header">
              <div>
                <div class="chart-title">地理分布热力</div>
                <div class="chart-subtitle">{{ mapSubtitle }}</div>
              </div>
            </div>
          </template>
          <div class="chart-body">
            <div class="chart-wrapper">
              <div ref="mapChartRef" class="chart map-chart" />
              <el-empty
                v-if="!isLoading && !mapHasData"
                description="暂无可视化数据"
                :image-size="80"
                class="chart-empty"
              />
            </div>
            <div v-if="mapSummaryText" class="chart-summary">{{ mapSummaryText }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import chinaGeoJson from '../assets/china-geo.json'
import apiClient from '../services/http'

const trendChartRef = ref(null)
const structureChartRef = ref(null)
const mapChartRef = ref(null)

const trendChartInstance = ref(null)
const structureChartInstance = ref(null)
const mapChartInstance = ref(null)

const trendHasData = ref(false)
const structureHasData = ref(false)
const mapHasData = ref(false)

const isLoading = ref(false)
const fetchError = ref('')
const yieldRecords = ref([])

const numberFormatters = {
  0: new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }),
  1: new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }),
  2: new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
}

const colorPalette = ['#42a5f5', '#66bb6a', '#ffca28', '#ab47bc', '#ef5350', '#26a69a', '#8d6e63', '#29b6f6', '#9ccc65']

const provinceNameSet = new Set(
  Array.isArray(chinaGeoJson?.features)
    ? chinaGeoJson.features
        .map(feature => feature?.properties?.name)
        .filter(name => typeof name === 'string' && name.trim().length)
    : []
)

const aliasProvinceMap = {
  '内蒙古': '内蒙古自治区',
  '广西': '广西壮族自治区',
  '宁夏': '宁夏回族自治区',
  '新疆': '新疆维吾尔自治区',
  '西藏': '西藏自治区',
  '黑龙江': '黑龙江省',
  '香港': '香港特别行政区',
  '澳门': '澳门特别行政区'
}

const municipalitySet = new Set(['北京', '天津', '上海', '重庆'])

const formatNumber = (value, digits = 0) => {
  const numeric = Number(value)
  if (Number.isNaN(numeric)) {
    return '0'
  }
  const formatter = numberFormatters[digits] ?? numberFormatters[0]
  return formatter.format(numeric)
}

const hexToRgba = (hex, alpha) => {
  if (!hex) return `rgba(66, 165, 245, ${alpha})`
  let sanitized = hex.replace('#', '')
  if (sanitized.length === 3) {
    sanitized = sanitized
      .split('')
      .map(char => char + char)
      .join('')
  }
  const numeric = parseInt(sanitized, 16)
  const r = (numeric >> 16) & 255
  const g = (numeric >> 8) & 255
  const b = numeric & 255
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

const normalizeProvinceName = name => {
  if (!name) return ''
  const trimmed = String(name).trim()
  if (!trimmed) return ''
  if (provinceNameSet.has(trimmed)) {
    return trimmed
  }
  if (aliasProvinceMap[trimmed]) {
    return aliasProvinceMap[trimmed]
  }
  if (municipalitySet.has(trimmed)) {
    const candidate = `${trimmed}市`
    return provinceNameSet.has(candidate) ? candidate : trimmed
  }
  if (trimmed.endsWith('省') || trimmed.endsWith('市') || trimmed.endsWith('自治区') || trimmed.endsWith('特别行政区')) {
    return provinceNameSet.has(trimmed) ? trimmed : ''
  }
  const provinceIndex = trimmed.indexOf('省')
  if (provinceIndex > 0) {
    const candidate = trimmed.slice(0, provinceIndex + 1)
    if (provinceNameSet.has(candidate)) {
      return candidate
    }
  }
  const autonomousIndex = trimmed.indexOf('自治区')
  if (autonomousIndex > 0) {
    const candidate = trimmed.slice(0, autonomousIndex + 3)
    if (provinceNameSet.has(candidate)) {
      return candidate
    }
  }
  const cityIndex = trimmed.indexOf('市')
  if (cityIndex > 0) {
    const candidate = trimmed.slice(0, cityIndex + 1)
    if (provinceNameSet.has(candidate)) {
      return candidate
    }
  }
  const fallback = `${trimmed}省`
  return provinceNameSet.has(fallback) ? fallback : ''
}

const normalizedRecords = computed(() =>
  (Array.isArray(yieldRecords.value) ? yieldRecords.value : []).map(record => ({
    ...record,
    year: record?.year != null ? Number(record.year) : null,
    production: record?.production != null ? Number(record.production) : null,
    sownArea: record?.sownArea != null ? Number(record.sownArea) : null,
    averagePrice: record?.averagePrice != null ? Number(record.averagePrice) : null,
    estimatedRevenue: record?.estimatedRevenue != null ? Number(record.estimatedRevenue) : null
  }))
)

const datasetMetrics = computed(() => {
  const records = normalizedRecords.value
  if (!records.length) {
    return {
      totalRecords: 0,
      cropCount: 0,
      regionCount: 0,
      categoryCount: 0,
      earliestYear: null,
      latestYear: null,
      latestCollectedAt: ''
    }
  }
  const cropSet = new Set()
  const regionSet = new Set()
  const categorySet = new Set()
  let earliestYear = Number.POSITIVE_INFINITY
  let latestYear = Number.NEGATIVE_INFINITY
  let latestCollectedAtTimestamp = null
  let latestCollectedAt = ''

  records.forEach(record => {
    if (record?.cropName) {
      cropSet.add(record.cropName)
    }
    if (record?.regionName) {
      regionSet.add(record.regionName)
    }
    if (record?.cropCategory) {
      categorySet.add(record.cropCategory)
    }
    if (typeof record.year === 'number' && !Number.isNaN(record.year)) {
      if (record.year < earliestYear) {
        earliestYear = record.year
      }
      if (record.year > latestYear) {
        latestYear = record.year
      }
    }
    if (record?.collectedAt) {
      const timestamp = Date.parse(record.collectedAt)
      if (!Number.isNaN(timestamp) && (latestCollectedAtTimestamp === null || timestamp > latestCollectedAtTimestamp)) {
        latestCollectedAtTimestamp = timestamp
        latestCollectedAt = record.collectedAt
      }
    }
  })

  return {
    totalRecords: records.length,
    cropCount: cropSet.size,
    regionCount: regionSet.size,
    categoryCount: categorySet.size,
    earliestYear: Number.isFinite(earliestYear) ? earliestYear : null,
    latestYear: Number.isFinite(latestYear) ? latestYear : null,
    latestCollectedAt
  }
})

const datasetSummaryText = computed(() => {
  const metrics = datasetMetrics.value
  if (!metrics.totalRecords) {
    return '当前暂无导入的农情数据，上传后将自动生成可视化图表。'
  }
  return `已汇总 ${formatNumber(metrics.totalRecords, 0)} 条产量记录，涵盖 ${formatNumber(metrics.cropCount, 0)} 种作物与 ${formatNumber(metrics.regionCount, 0)} 个地区。`
})

const highlightStats = computed(() => {
  const metrics = datasetMetrics.value
  if (!metrics.totalRecords) {
    return [
      { label: '数据记录', value: '0 条', sub: '等待导入农情数据集' },
      { label: '覆盖作物', value: '-', sub: '导入后自动识别作物类别' },
      { label: '最新数据', value: '-', sub: '暂无采集时间信息' }
    ]
  }
  const { cropCount, categoryCount, regionCount, totalRecords, earliestYear, latestYear, latestCollectedAt } = metrics
  const yearLabel = earliestYear && latestYear ? (earliestYear === latestYear ? `${latestYear} 年` : `${earliestYear} - ${latestYear} 年`) : '年份待补充'
  return [
    { label: '覆盖作物', value: `${formatNumber(cropCount, 0)} 种`, sub: `涵盖 ${formatNumber(categoryCount, 0)} 类作物` },
    { label: '覆盖地区', value: `${formatNumber(regionCount, 0)} 个`, sub: `累计 ${formatNumber(totalRecords, 0)} 条产量记录` },
    { label: '数据时间范围', value: yearLabel, sub: latestCollectedAt ? `最近采集于 ${latestCollectedAt}` : '采集时间待补充' }
  ]
})

const trendChartData = computed(() => {
  const records = normalizedRecords.value.filter(record => typeof record.year === 'number' && record.production != null)
  if (!records.length) {
    return { years: [], series: [], metricLabel: '产量 (万吨)' }
  }
  const yearSet = new Set()
  const cropYearMap = new Map()

  records.forEach(record => {
    const year = record.year
    const cropName = record?.cropName || '未命名作物'
    yearSet.add(year)
    if (!cropYearMap.has(cropName)) {
      cropYearMap.set(cropName, new Map())
    }
    const yearMap = cropYearMap.get(cropName)
    const previous = yearMap.get(year) ?? 0
    yearMap.set(year, previous + (record.production ?? 0))
  })

  const years = Array.from(yearSet).sort((a, b) => a - b)
  const series = Array.from(cropYearMap.entries()).map(([name, valueMap]) => ({
    name,
    data: years.map(year => {
      const value = valueMap.get(year)
      return value != null ? Number(value.toFixed(2)) : 0
    })
  }))

  return { years, series, metricLabel: '产量 (万吨)' }
})

const structureData = computed(() => {
  const records = normalizedRecords.value.filter(record => record.production != null || record.sownArea != null)
  if (!records.length) {
    return { year: null, items: [], metricKey: 'production', metricLabel: '产量 (万吨)' }
  }
  const metrics = datasetMetrics.value
  const targetYear = metrics.latestYear
  const filteredByYear = typeof targetYear === 'number' ? records.filter(record => record.year === targetYear) : []
  const sourceRecords = filteredByYear.length ? filteredByYear : records
  const metricKey = sourceRecords.some(record => record.sownArea != null) ? 'sownArea' : 'production'
  const metricLabel = metricKey === 'sownArea' ? '播种面积 (千公顷)' : '产量 (万吨)'
  const cropMap = new Map()

  sourceRecords.forEach(record => {
    const cropName = record?.cropName || '未命名作物'
    const value = record[metricKey]
    if (value == null) return
    const previous = cropMap.get(cropName) ?? 0
    cropMap.set(cropName, previous + value)
  })

  const items = Array.from(cropMap.entries())
    .map(([name, value]) => ({ name, value: Number(value.toFixed(2)) }))
    .sort((a, b) => b.value - a.value)

  return {
    year: filteredByYear.length ? targetYear : null,
    items,
    metricKey,
    metricLabel
  }
})

const mapData = computed(() => {
  const records = normalizedRecords.value.filter(record => record.production != null)
  if (!records.length) {
    return { items: [], min: 0, max: 0, excluded: 0 }
  }
  const regionMap = new Map()
  let excluded = 0

  records.forEach(record => {
    const normalizedName = normalizeProvinceName(record?.regionName)
    if (!normalizedName || !provinceNameSet.has(normalizedName)) {
      excluded += 1
      return
    }
    const value = record.production ?? 0
    const previous = regionMap.get(normalizedName) ?? 0
    regionMap.set(normalizedName, previous + value)
  })

  const items = Array.from(regionMap.entries()).map(([name, value]) => ({
    name,
    value: Number(value.toFixed(2))
  }))

  const values = items.map(item => item.value)
  let min = values.length ? Math.min(...values) : 0
  let max = values.length ? Math.max(...values) : 0
  if (values.length) {
    if (min === max) {
      if (max === 0) {
        max = 1
      } else {
        min = max * 0.9
        max = max * 1.1
      }
    }
  }

  return { items, min, max, excluded }
})

const topStructureCrop = computed(() => {
  const { items } = structureData.value
  if (!items.length) return null
  return items.reduce((max, item) => (item.value > (max?.value ?? Number.NEGATIVE_INFINITY) ? item : max), null)
})

const topRegion = computed(() => {
  const { items } = mapData.value
  if (!items.length) return null
  return items.reduce((max, item) => (item.value > (max?.value ?? Number.NEGATIVE_INFINITY) ? item : max), null)
})

const trendGrowth = computed(() => {
  const data = trendChartData.value
  if (data.years.length < 2 || !data.series.length) {
    return null
  }
  const latestIndex = data.years.length - 1
  const previousIndex = latestIndex - 1
  if (previousIndex < 0) {
    return null
  }
  const stats = data.series
    .map(series => ({
      name: series.name,
      latest: series.data[latestIndex],
      previous: series.data[previousIndex]
    }))
    .filter(item => item.latest != null && item.previous != null)

  if (!stats.length) {
    return null
  }

  const focus = stats.reduce((max, item) => (item.latest > (max?.latest ?? Number.NEGATIVE_INFINITY) ? item : max), null)
  if (!focus) {
    return null
  }
  if (!focus.previous) {
    return { name: focus.name, latestYear: data.years[latestIndex], growthRate: null }
  }
  const growthRate = ((focus.latest - focus.previous) / Math.abs(focus.previous)) * 100
  return { name: focus.name, latestYear: data.years[latestIndex], growthRate }
})

const trendSubtitle = computed(() => {
  const metrics = datasetMetrics.value
  if (!metrics.totalRecords) {
    return '导入产量数据后可查看逐年趋势对比。'
  }
  if (metrics.earliestYear && metrics.latestYear) {
    if (metrics.earliestYear === metrics.latestYear) {
      return `基于 ${metrics.latestYear} 年导入的 ${formatNumber(metrics.cropCount, 0)} 种作物产量记录`
    }
    return `覆盖 ${metrics.earliestYear} - ${metrics.latestYear} 年的 ${formatNumber(metrics.cropCount, 0)} 种作物产量趋势`
  }
  return `已导入 ${formatNumber(metrics.totalRecords, 0)} 条产量记录，自动生成趋势分析`
})

const structureSubtitle = computed(() => {
  const data = structureData.value
  if (!data.items.length) {
    return '暂无可用于结构分析的数据，请补充作物种植信息。'
  }
  const yearText = data.year ? `${data.year} 年` : '最新导入数据'
  return `${yearText}的作物${data.metricLabel}构成占比`
})

const mapSubtitle = computed(() => {
  const metrics = datasetMetrics.value
  if (!metrics.totalRecords) {
    return '导入省级产量数据后可查看区域热力。'
  }
  const { items, excluded } = mapData.value
  if (!items.length) {
    return '暂无匹配到省级行政区的数据，请完善地区信息。'
  }
  return `按省份汇总产量，覆盖 ${items.length} 个省级地区${excluded ? `，忽略 ${excluded} 条缺少省级信息的记录` : ''}`
})

const trendSummaryText = computed(() => {
  const data = trendChartData.value
  if (!data.series.length) {
    return ''
  }
  return `共展示 ${data.series.length} 种作物在 ${data.years.length} 个年份的产量变化`
})

const structureSummaryText = computed(() => {
  const data = structureData.value
  if (!data.items.length) {
    return ''
  }
  const yearText = data.year ? `${data.year} 年` : '最新数据'
  return `${yearText}共有 ${data.items.length} 种作物，${data.metricLabel}占比如上`
})

const mapSummaryText = computed(() => {
  const { items, excluded } = mapData.value
  if (!items.length) {
    return datasetMetrics.value.totalRecords ? '请检查导入数据的地区字段，确保包含省级名称。' : ''
  }
  return `热力图已聚合 ${items.length} 个省级行政区${excluded ? `，${excluded} 条记录未匹配成功` : ''}`
})

const insightTips = computed(() => {
  const metrics = datasetMetrics.value
  if (!metrics.totalRecords) {
    return [
      '导入数据中心的农情数据后，系统将自动生成趋势、结构和热力图。',
      '请确保导入文件包含作物、地区和年份等关键字段。',
      '上传完成后可点击“刷新可视化数据”以获取最新图表。'
    ]
  }
  const tips = []
  const growth = trendGrowth.value
  if (growth && growth.growthRate != null) {
    const direction = growth.growthRate >= 0 ? '增长' : '下降'
    tips.push(`${growth.latestYear} 年 ${growth.name} 产量较上一年${direction} ${formatNumber(Math.abs(growth.growthRate), 1)}%，请结合库存与销售计划及时调整。`)
  } else {
    tips.push('建议持续补充多年度数据，以便识别作物产量波动趋势。')
  }
  const structureTop = topStructureCrop.value
  if (structureTop) {
    const yearText = structureData.value.year ? `${structureData.value.year} 年` : '最新年度'
    tips.push(`${yearText}中 ${structureTop.name} 占比最高，优先关注投入与风险控制。`)
  }
  const regionTop = topRegion.value
  if (regionTop) {
    tips.push(`${regionTop.name} 是目前产量最高的省级地区，可结合物流能力进行重点调度。`)
  }
  while (tips.length < 3) {
    tips.push('导入更多地区或作物数据，有助于提升可视化分析的全面性。')
  }
  return tips.slice(0, 3)
})

const initCharts = () => {
  if (trendChartRef.value && !trendChartInstance.value) {
    trendChartInstance.value = echarts.init(trendChartRef.value)
  }
  if (structureChartRef.value && !structureChartInstance.value) {
    structureChartInstance.value = echarts.init(structureChartRef.value)
  }
  if (mapChartRef.value && !mapChartInstance.value) {
    if (!echarts.getMap('china')) {
      echarts.registerMap('china', chinaGeoJson)
    }
    mapChartInstance.value = echarts.init(mapChartRef.value)
  }
}

const disposeCharts = () => {
  trendChartInstance.value?.dispose()
  structureChartInstance.value?.dispose()
  mapChartInstance.value?.dispose()
  trendChartInstance.value = null
  structureChartInstance.value = null
  mapChartInstance.value = null
}

const setChartsLoading = loading => {
  const instances = [trendChartInstance.value, structureChartInstance.value, mapChartInstance.value]
  instances.forEach(instance => {
    if (!instance) return
    if (loading) {
      instance.showLoading({ text: '数据加载中...' })
    } else {
      instance.hideLoading()
    }
  })
}

const updateTrendChart = data => {
  trendHasData.value = data.series.length > 0 && data.years.length > 0
  if (!trendChartRef.value || !trendChartInstance.value) return
  if (!trendHasData.value) {
    trendChartInstance.value.clear()
    return
  }
  const legendData = data.series.map(item => item.name)
  const series = data.series.map((item, index) => {
    const color = colorPalette[index % colorPalette.length]
    return {
      name: item.name,
      type: 'line',
      smooth: true,
      symbolSize: 8,
      emphasis: { focus: 'series' },
      data: item.data,
      itemStyle: { color },
      lineStyle: { width: 3 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: hexToRgba(color, 0.32) },
          { offset: 1, color: hexToRgba(color, 0.05) }
        ])
      }
    }
  })

  trendChartInstance.value.setOption({
    color: colorPalette,
    tooltip: { trigger: 'axis' },
    legend: { data: legendData },
    grid: { left: 40, right: 20, top: 60, bottom: 40 },
    xAxis: { type: 'category', boundaryGap: false, data: data.years },
    yAxis: { type: 'value', name: data.metricLabel },
    series
  })
}

const updateStructureChart = data => {
  structureHasData.value = data.items.length > 0
  if (!structureChartRef.value || !structureChartInstance.value) return
  if (!structureHasData.value) {
    structureChartInstance.value.clear()
    return
  }
  structureChartInstance.value.setOption({
    tooltip: {
      trigger: 'item',
      formatter: params => `${params.name}<br />${data.metricLabel}：${formatNumber(params.value, 1)}<br />占比：${formatNumber(params.percent, 1)}%`
    },
    legend: { orient: 'vertical', right: 0, top: 'middle' },
    series: [
      {
        type: 'pie',
        radius: ['38%', '70%'],
        center: ['40%', '50%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: {
          formatter: params => `${params.name}\n${formatNumber(params.percent, 1)}%`
        },
        labelLine: { length: 15, length2: 20 },
        data: data.items
      }
    ]
  })
}

const updateMapChart = data => {
  mapHasData.value = data.items.length > 0
  if (!mapChartRef.value || !mapChartInstance.value) return
  if (!mapHasData.value) {
    mapChartInstance.value.clear()
    return
  }
  mapChartInstance.value.setOption({
    tooltip: {
      trigger: 'item',
      formatter: params => `${params.name}<br />产量：${formatNumber(params.value, 1)} 万吨`
    },
    visualMap: {
      min: data.min,
      max: data.max,
      left: 20,
      bottom: 20,
      text: ['高', '低'],
      calculable: true,
      inRange: { color: ['#e3f2fd', '#64b5f6', '#1e88e5'] }
    },
    geo: {
      map: 'china',
      roam: true,
      emphasis: { itemStyle: { areaColor: '#ffcc80' } },
      itemStyle: { borderColor: '#90caf9', borderWidth: 0.8 }
    },
    series: [
      {
        name: '省级产量',
        type: 'map',
        geoIndex: 0,
        data: data.items
      }
    ]
  })
}

const handleResize = () => {
  trendChartInstance.value?.resize()
  structureChartInstance.value?.resize()
  mapChartInstance.value?.resize()
}

const loadYieldRecords = async () => {
  isLoading.value = true
  fetchError.value = ''
  setChartsLoading(true)
  try {
    const { data } = await apiClient.get('/api/yields')
    const payload = Array.isArray(data) ? data : Array.isArray(data?.data) ? data.data : []
    yieldRecords.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    const message = error?.response?.data?.message || '加载可视化数据失败'
    fetchError.value = message
    yieldRecords.value = []
    ElMessage.error(message)
  } finally {
    isLoading.value = false
    setChartsLoading(false)
  }
}

watch(
  trendChartData,
  data => {
    updateTrendChart(data)
  },
  { deep: true, immediate: true }
)

watch(
  structureData,
  data => {
    updateStructureChart(data)
  },
  { deep: true, immediate: true }
)

watch(
  mapData,
  data => {
    updateMapChart(data)
  },
  { deep: true, immediate: true }
)

onMounted(() => {
  initCharts()
  window.addEventListener('resize', handleResize)
  loadYieldRecords()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})

</script>

<style scoped>
.visualization-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.overview-card {
  display: flex;
  gap: 32px;
  background: linear-gradient(135deg, #f0fdf4 0%, #e0f2f1 100%);
  padding: 32px;
  border-radius: 24px;
  box-shadow: 0 12px 32px rgba(16, 86, 82, 0.12);
  border: 1px solid rgba(16, 86, 82, 0.15);
}

.overview-copy {
  flex: 2;
}

.overview-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #0b5345;
  background: rgba(13, 71, 60, 0.12);
  border-radius: 999px;
  letter-spacing: 2px;
}

.overview-title {
  margin: 16px 0 12px;
  font-size: 28px;
  font-weight: 600;
  color: #0d473c;
}

.overview-desc {
  font-size: 15px;
  line-height: 1.8;
  color: #265c55;
}

.overview-stats {
  display: flex;
  gap: 16px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.overview-stat {
  min-width: 160px;
  padding: 16px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(13, 71, 60, 0.08);
}

.stat-label {
  font-size: 13px;
  color: #4f776f;
}

.stat-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 600;
  color: #0f5132;
}

.stat-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #6c8f86;
}

.overview-hint {
  flex: 1;
  padding: 24px;
  border-radius: 20px;
  background: rgba(11, 61, 46, 0.82);
  color: #ecfdf5;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hint-title {
  font-size: 16px;
  font-weight: 600;
}

.overview-hint ul {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
  color: rgba(236, 253, 245, 0.85);
  font-size: 14px;
}

.page-alert {
  border-radius: 16px;
}

.page-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f2fbf7;
  border: 1px solid rgba(13, 71, 60, 0.12);
  border-radius: 16px;
  padding: 14px 20px;
  color: #0d473c;
  font-size: 14px;
}

.actions-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.chart-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chart-wrapper {
  position: relative;
  min-height: 280px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-summary {
  font-size: 13px;
  color: #50746a;
}

.chart-grid {
  width: 100%;
}

.chart-card {
  border-radius: 20px;
  overflow: hidden;
}

.chart-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #0d473c;
}

.chart-subtitle {
  font-size: 13px;
  color: #5d8078;
}

.chart {
  width: 100%;
  height: 360px;
}

.map-chart {
  height: 420px;
}

@media (max-width: 992px) {
  .overview-card {
    flex-direction: column;
  }

  .overview-hint {
    width: 100%;
  }

  .page-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .chart {
    height: 320px;
  }

  .map-chart {
    height: 360px;
  }
}
</style>
