<template>
  <div class="visualization-page">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">数据可视化洞察中心</h1>
        <p class="hero-desc">
          从时间趋势、结构构成与区域分布三个维度，为业务团队提供对农情数据的动态感知能力，助力制定科学的种植与调度策略。
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
        <div class="snapshot-card">
          <div class="snapshot-title">当前筛选概览</div>
          <div class="snapshot-chip">{{ selectionTagText }}</div>
          <ul class="snapshot-list">
            <li>
              <span class="snapshot-label">记录总量</span>
              <span class="snapshot-value">{{ snapshotMetrics.total }}</span>
            </li>
            <li>
              <span class="snapshot-label">覆盖作物</span>
              <span class="snapshot-value">{{ snapshotMetrics.crops }}</span>
            </li>
            <li>
              <span class="snapshot-label">时间范围</span>
              <span class="snapshot-value">{{ snapshotMetrics.range }}</span>
            </li>
          </ul>
        </div>
        <div class="insight-card">
          <div class="insight-card-title">智能洞察建议</div>
          <ul class="insight-list">
            <li v-for="tip in insightTips" :key="tip">{{ tip }}</li>
          </ul>
        </div>
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

    <div class="filter-bar">
      <div class="filter-summary">
        <el-tag v-if="hasAnySelection" class="filter-tag" type="success" effect="light">{{ selectionTagText }}</el-tag>
        <span>{{ datasetSummaryText }}</span>
      </div>
      <div class="filter-controls">
        <el-select v-model="selectedChartMode" class="chart-mode-select" placeholder="选择图表视图">
          <el-option v-for="option in chartModeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-select
          v-model="selectedCategory"
          class="category-select"
          :disabled="!categorySelectOptions.length"
          placeholder="选择作物类别"
        >
          <el-option
            v-for="option in categorySelectOptions"
            :key="option.value"
            :label="option.display"
            :value="option.value"
          />
        </el-select>
        <el-select
          v-model="selectedCrop"
          class="crop-select"
          :disabled="!cropSelectOptions.length"
          placeholder="筛选作物"
        >
          <el-option
            v-for="option in cropSelectOptions"
            :key="option.value"
            :label="option.display"
            :value="option.value"
          />
        </el-select>
        <el-select
          v-model="selectedYear"
          class="year-select"
          :disabled="!yearSelectOptions.length"
          placeholder="筛选年份"
        >
          <el-option
            v-for="option in yearSelectOptions"
            :key="option.value"
            :label="option.display"
            :value="option.value"
          />
        </el-select>
        <el-button type="primary" :loading="isLoading" @click="loadYieldRecords">刷新数据</el-button>
      </div>
    </div>

    <el-row v-show="isTrendVisible || isStructureVisible" :gutter="24" class="chart-grid">
      <el-col
        v-show="isTrendVisible"
        :md="trendSpan"
        :lg="trendSpan"
        :sm="24"
        :xs="24"
      >
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <template #header>
            <div class="chart-header">
              <div class="chart-heading">
                <div class="chart-title">产量趋势分析</div>
                <div class="chart-subtitle">{{ trendSubtitle }}</div>
              </div>
              <div class="chart-actions">
                <el-radio-group v-model="selectedTrendType" size="small" class="chart-toggle">
                  <el-radio-button label="line">折线图</el-radio-button>
                  <el-radio-button label="bar">柱状图</el-radio-button>
                </el-radio-group>
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
      <el-col
        v-show="isStructureVisible"
        :md="structureSpan"
        :lg="structureSpan"
        :sm="24"
        :xs="24"
      >
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <template #header>
            <div class="chart-header">
              <div class="chart-heading">
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

    <el-row v-show="isMapVisible" :gutter="24" class="chart-grid">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <template #header>
            <div class="chart-header">
              <div class="chart-heading">
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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

const CATEGORY_ALL = '__ALL__'
const UNCATEGORIZED_LABEL = '未分类作物'
const ALL_CROPS = '__ALL_CROPS__'
const ALL_YEARS = '__ALL_YEARS__'
const chartModeOptions = [
  { label: '展示全部图表', value: 'all' },
  { label: '仅产量趋势', value: 'trend' },
  { label: '仅结构占比', value: 'structure' },
  { label: '仅地理热力', value: 'map' }
]

const selectedCategory = ref(CATEGORY_ALL)
const selectedCrop = ref(ALL_CROPS)
const selectedYear = ref(ALL_YEARS)
const selectedChartMode = ref('all')
const selectedTrendType = ref('line')

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

const formatPlainNumber = (value, digits = 0) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '0'
  }
  const fixed = numeric.toFixed(Math.max(digits, 0))
  return digits > 0 ? fixed.replace(/(\.\d*?[1-9])0+$/, '$1').replace(/\.0+$/, '').replace(/\.$/, '') : fixed
}

const formatAxisValue = value => {
  const numeric = Number(value)
  if (Number.isNaN(numeric)) {
    return '0'
  }
  const magnitude = Math.abs(numeric)
  let digits = 0
  if (magnitude < 1) {
    digits = 3
  } else if (magnitude < 10) {
    digits = 2
  } else if (magnitude < 100) {
    digits = 1
  }
  const fixed = numeric.toFixed(digits)
  const sanitized = digits ? fixed.replace(/\.0+$/, '').replace(/\.$/, '') : fixed
  return sanitized.length ? sanitized : '0'
}

const formatTrendAxisLabel = value => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return ''
  }
  const magnitude = Math.abs(numeric)
  let digits = 3
  if (magnitude >= 1000) {
    digits = 0
  } else if (magnitude >= 100) {
    digits = 1
  } else if (magnitude >= 10) {
    digits = 1
  } else if (magnitude >= 1) {
    digits = 2
  }
  const formatted = formatPlainNumber(numeric, digits)
  return formatted || '0'
}

const extractMetricUnit = label => {
  if (!label) return ''
  const match = label.match(/\(([^)]+)\)/)
  return match ? match[1] : ''
}

const formatTrendMetricValue = (value, unitLabel = '') => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '0'
  }
  const magnitude = Math.abs(numeric)
  let digits = 3
  if (magnitude >= 1000) {
    digits = 0
  } else if (magnitude >= 100) {
    digits = 1
  } else if (magnitude >= 10) {
    digits = 1
  } else if (magnitude >= 1) {
    digits = 2
  }
  const formatted = formatPlainNumber(numeric, digits)
  return unitLabel ? `${formatted} ${unitLabel}` : formatted
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

const resolveCategoryLabel = category => {
  if (category == null) {
    return UNCATEGORIZED_LABEL
  }
  const text = String(category).trim()
  return text || UNCATEGORIZED_LABEL
}

const resolveCropName = crop => {
  if (crop == null) {
    return '未命名作物'
  }
  const text = String(crop).trim()
  return text || '未命名作物'
}

const categoryFilteredRecords = computed(() => {
  const records = normalizedRecords.value
  if (selectedCategory.value === CATEGORY_ALL) {
    return records
  }
  return records.filter(record => resolveCategoryLabel(record?.cropCategory) === selectedCategory.value)
})

const filteredRecords = computed(() => {
  let records = categoryFilteredRecords.value
  if (selectedCrop.value !== ALL_CROPS) {
    records = records.filter(record => resolveCropName(record?.cropName) === selectedCrop.value)
  }
  if (selectedYear.value !== ALL_YEARS) {
    const year = Number(selectedYear.value)
    records = records.filter(record => record.year === year)
  }
  return records
})

const computeMetrics = records => {
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
      categorySet.add(resolveCategoryLabel(record.cropCategory))
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
    categoryCount: categorySet.size || (records.length ? 1 : 0),
    earliestYear: Number.isFinite(earliestYear) ? earliestYear : null,
    latestYear: Number.isFinite(latestYear) ? latestYear : null,
    latestCollectedAt
  }
}

const datasetMetrics = computed(() => computeMetrics(normalizedRecords.value))
const activeMetrics = computed(() => computeMetrics(filteredRecords.value))

const categoryOptions = computed(() => {
  const counter = new Map()
  normalizedRecords.value.forEach(record => {
    const label = resolveCategoryLabel(record?.cropCategory)
    counter.set(label, (counter.get(label) ?? 0) + 1)
  })
  return Array.from(counter.entries())
    .sort((a, b) => {
      if (b[1] === a[1]) {
        return a[0].localeCompare(b[0], 'zh-CN')
      }
      return b[1] - a[1]
    })
    .map(([label, count]) => ({ label, value: label, count }))
})

const categorySelectOptions = computed(() => {
  const totalRecords = datasetMetrics.value.totalRecords
  if (!totalRecords) {
    return []
  }
  const formattedTotal = formatNumber(totalRecords, 0)
  return [
    {
      label: '全部类别',
      value: CATEGORY_ALL,
      count: totalRecords,
      display: `全部类别 · ${formattedTotal} 条记录`
    },
    ...categoryOptions.value.map(option => ({
      ...option,
      display: `${option.label} · ${formatNumber(option.count, 0)} 条记录`
    }))
  ]
})

const cropOptions = computed(() => {
  const counter = new Map()
  categoryFilteredRecords.value.forEach(record => {
    const label = resolveCropName(record?.cropName)
    counter.set(label, (counter.get(label) ?? 0) + 1)
  })
  return Array.from(counter.entries())
    .sort((a, b) => {
      if (b[1] === a[1]) {
        return a[0].localeCompare(b[0], 'zh-CN')
      }
      return b[1] - a[1]
    })
    .map(([label, count]) => ({ label, value: label, count }))
})

const cropSelectOptions = computed(() => {
  const total = categoryFilteredRecords.value.length
  if (!total) {
    return []
  }
  const formattedTotal = formatNumber(total, 0)
  return [
    {
      label: '全部作物',
      value: ALL_CROPS,
      count: total,
      display: `全部作物 · ${formattedTotal} 条记录`
    },
    ...cropOptions.value.map(option => ({
      ...option,
      display: `${option.label} · ${formatNumber(option.count, 0)} 条记录`
    }))
  ]
})

const yearOptions = computed(() => {
  const counter = new Map()
  categoryFilteredRecords.value.forEach(record => {
    if (typeof record.year === 'number' && !Number.isNaN(record.year)) {
      counter.set(record.year, (counter.get(record.year) ?? 0) + 1)
    }
  })
  return Array.from(counter.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([year, count]) => ({ label: `${year}`, value: year, count }))
})

const yearSelectOptions = computed(() => {
  const total = categoryFilteredRecords.value.length
  if (!total) {
    return []
  }
  const formattedTotal = formatNumber(total, 0)
  return [
    {
      label: '全部年份',
      value: ALL_YEARS,
      count: total,
      display: `全部年份 · ${formattedTotal} 条记录`
    },
    ...yearOptions.value.map(option => ({
      ...option,
      display: `${option.label} 年 · ${formatNumber(option.count, 0)} 条记录`
    }))
  ]
})

const selectedCategoryLabel = computed(() => (selectedCategory.value === CATEGORY_ALL ? '全部类别' : selectedCategory.value))
const selectedCropLabel = computed(() => (selectedCrop.value === ALL_CROPS ? '全部作物' : selectedCrop.value))
const selectedYearLabel = computed(() => (selectedYear.value === ALL_YEARS ? '全部年份' : `${selectedYear.value}`))

const isAllCategorySelected = computed(() => selectedCategory.value === CATEGORY_ALL)
const hasCropFilter = computed(() => selectedCrop.value !== ALL_CROPS)
const hasYearFilter = computed(() => selectedYear.value !== ALL_YEARS)
const hasAnySelection = computed(() => !isAllCategorySelected.value || hasCropFilter.value || hasYearFilter.value)

const selectionSummaryText = computed(() => {
  const parts = []
  if (!isAllCategorySelected.value) {
    parts.push(selectedCategoryLabel.value)
  }
  if (hasCropFilter.value) {
    parts.push(selectedCropLabel.value)
  }
  if (hasYearFilter.value) {
    parts.push(`${selectedYearLabel.value} 年`)
  }
  return parts.length ? parts.join(' · ') : '全局视图'
})

const selectionTagText = computed(() => (hasAnySelection.value ? selectionSummaryText.value : '全局视图'))

const selectionFocusLabel = computed(() => {
  if (!hasAnySelection.value) {
    return ''
  }
  const parts = []
  if (!isAllCategorySelected.value) {
    parts.push(selectedCategoryLabel.value)
  }
  if (hasCropFilter.value) {
    parts.push(selectedCropLabel.value)
  }
  return parts.join(' · ')
})

const snapshotMetrics = computed(() => {
  if (!datasetMetrics.value.totalRecords) {
    return { total: '0 条', crops: '-', range: '等待导入数据' }
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return { total: '0 条', crops: '-', range: '筛选条件暂无数据' }
  }
  const metrics = hasAnySelection.value ? activeMetrics.value : datasetMetrics.value
  const { totalRecords, cropCount, earliestYear, latestYear } = metrics
  const rangeLabel =
    earliestYear && latestYear
      ? earliestYear === latestYear
        ? `${latestYear} 年`
        : `${earliestYear} - ${latestYear} 年`
      : '年份待补充'
  return {
    total: `${formatNumber(totalRecords, 0)} 条`,
    crops: `${formatNumber(cropCount, 0)} 种`,
    range: rangeLabel
  }
})

const isTrendVisible = computed(() => selectedChartMode.value === 'all' || selectedChartMode.value === 'trend')
const isStructureVisible = computed(() => selectedChartMode.value === 'all' || selectedChartMode.value === 'structure')
const isMapVisible = computed(() => selectedChartMode.value === 'all' || selectedChartMode.value === 'map')
const trendSpan = computed(() => (isTrendVisible.value && isStructureVisible.value ? 12 : 24))
const structureSpan = computed(() => (isTrendVisible.value && isStructureVisible.value ? 12 : 24))

watch(categorySelectOptions, options => {
  if (!options.length) {
    selectedCategory.value = CATEGORY_ALL
    return
  }
  if (selectedCategory.value === CATEGORY_ALL) {
    return
  }
  const matched = options.some(option => option.value === selectedCategory.value)
  if (!matched) {
    selectedCategory.value = CATEGORY_ALL
  }
})

watch(cropSelectOptions, options => {
  if (!options.length) {
    selectedCrop.value = ALL_CROPS
    return
  }
  if (selectedCrop.value === ALL_CROPS) {
    return
  }
  const matched = options.some(option => option.value === selectedCrop.value)
  if (!matched) {
    selectedCrop.value = ALL_CROPS
  }
})

watch(yearSelectOptions, options => {
  if (!options.length) {
    selectedYear.value = ALL_YEARS
    return
  }
  if (selectedYear.value === ALL_YEARS) {
    return
  }
  const matched = options.some(option => option.value === selectedYear.value)
  if (!matched) {
    selectedYear.value = ALL_YEARS
  }
})

watch(selectedCategory, () => {
  selectedCrop.value = ALL_CROPS
  selectedYear.value = ALL_YEARS
})

const datasetSummaryText = computed(() => {
  if (!datasetMetrics.value.totalRecords) {
    return '当前暂无导入的农情数据，上传后将自动生成可视化图表。'
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return `${selectionSummaryText.value} 暂无可视化数据，请调整筛选条件或刷新数据。`
  }
  const metrics = hasAnySelection.value ? activeMetrics.value : datasetMetrics.value
  const cropText = `${formatNumber(metrics.cropCount, 0)} 种作物`
  const regionText = `${formatNumber(metrics.regionCount, 0)} 个地区`
  if (!hasAnySelection.value) {
    return `已汇总 ${formatNumber(metrics.totalRecords, 0)} 条产量记录，涵盖 ${cropText}与 ${regionText}。`
  }
  return `${selectionSummaryText.value} 共匹配 ${formatNumber(metrics.totalRecords, 0)} 条记录，覆盖 ${cropText}与 ${regionText}。`
})

const highlightStats = computed(() => {
  if (!datasetMetrics.value.totalRecords) {
    return [
      { label: '数据记录', value: '0 条', sub: '等待导入农情数据集' },
      { label: '覆盖作物', value: '-', sub: '导入后自动识别作物类别' },
      { label: '最新数据', value: '-', sub: '暂无采集时间信息' }
    ]
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return [
      { label: '当前视图', value: selectionSummaryText.value, sub: '暂无匹配数据' },
      { label: '覆盖作物', value: '-', sub: '调整筛选条件后再试' },
      { label: '数据时间范围', value: '-', sub: '暂无年份信息' }
    ]
  }
  const metrics = hasAnySelection.value ? activeMetrics.value : datasetMetrics.value
  const { cropCount, categoryCount, regionCount, totalRecords, earliestYear, latestYear, latestCollectedAt } = metrics
  const yearLabel = earliestYear && latestYear ? (earliestYear === latestYear ? `${latestYear} 年` : `${earliestYear} - ${latestYear} 年`) : '年份待补充'
  return [
    {
      label: '当前视图',
      value: selectionTagText.value,
      sub: hasAnySelection.value ? `筛选后 ${formatNumber(totalRecords, 0)} 条记录` : `累计 ${formatNumber(totalRecords, 0)} 条记录`
    },
    {
      label: '覆盖作物',
      value: `${formatNumber(cropCount, 0)} 种`,
      sub: hasAnySelection.value ? `涉及 ${formatNumber(regionCount, 0)} 个地区` : `涵盖 ${formatNumber(categoryCount, 0)} 类作物`
    },
    { label: '数据时间范围', value: yearLabel, sub: latestCollectedAt ? `最近采集于 ${latestCollectedAt}` : '采集时间待补充' }
  ]
})
const trendChartData = computed(() => {
  const records = filteredRecords.value.filter(record => typeof record.year === 'number' && record.production != null)
  if (!records.length) {
    return { years: [], series: [], metricLabel: '产量 (万吨)' }
  }
  const yearSet = new Set()
  const cropYearMap = new Map()

  records.forEach(record => {
    const year = record.year
    const cropName = resolveCropName(record?.cropName)
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
  const records = filteredRecords.value.filter(record => record.production != null || record.sownArea != null)
  if (!records.length) {
    return { year: null, items: [], metricKey: 'production', metricLabel: '产量 (万吨)' }
  }
  const metrics = hasAnySelection.value ? activeMetrics.value : datasetMetrics.value
  const preferredYear =
    selectedYear.value !== ALL_YEARS && typeof selectedYear.value === 'number'
      ? selectedYear.value
      : metrics.latestYear
  const filteredByYear = typeof preferredYear === 'number' ? records.filter(record => record.year === preferredYear) : []
  const sourceRecords = filteredByYear.length ? filteredByYear : records
  const metricKey = sourceRecords.some(record => record.sownArea != null) ? 'sownArea' : 'production'
  const metricLabel = metricKey === 'sownArea' ? '播种面积 (千公顷)' : '产量 (万吨)'
  const cropMap = new Map()

  sourceRecords.forEach(record => {
    const cropName = resolveCropName(record?.cropName)
    const value = record[metricKey]
    if (value == null) return
    const previous = cropMap.get(cropName) ?? 0
    cropMap.set(cropName, previous + value)
  })

  const items = Array.from(cropMap.entries())
    .map(([name, value]) => ({ name, value: Number(value.toFixed(2)) }))
    .sort((a, b) => b.value - a.value)

  return {
    year: filteredByYear.length ? preferredYear : null,
    items,
    metricKey,
    metricLabel
  }
})

const mapData = computed(() => {
  const records = filteredRecords.value.filter(record => record.production != null)
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
  if (!datasetMetrics.value.totalRecords) {
    return '导入产量数据后可查看逐年趋势对比。'
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return `${selectionSummaryText.value} 暂无可用的产量趋势数据。`
  }
  const metrics = hasAnySelection.value ? activeMetrics.value : datasetMetrics.value
  if (metrics.earliestYear && metrics.latestYear) {
    if (metrics.earliestYear === metrics.latestYear) {
      if (!hasAnySelection.value) {
        return `基于 ${metrics.latestYear} 年导入的 ${formatNumber(metrics.cropCount, 0)} 种作物产量记录`
      }
      return `${selectionSummaryText.value} 聚焦 ${metrics.latestYear} 年的 ${formatNumber(metrics.cropCount, 0)} 种作物产量记录`
    }
    if (!hasAnySelection.value) {
      return `覆盖 ${metrics.earliestYear} - ${metrics.latestYear} 年的 ${formatNumber(metrics.cropCount, 0)} 种作物产量趋势`
    }
    return `${selectionSummaryText.value} 覆盖 ${metrics.earliestYear} - ${metrics.latestYear} 年的产量趋势`
  }
  if (!hasAnySelection.value) {
    return `已导入 ${formatNumber(metrics.totalRecords, 0)} 条产量记录，自动生成趋势分析`
  }
  return `${selectionSummaryText.value} 共 ${formatNumber(metrics.totalRecords, 0)} 条产量记录，自动生成趋势分析`
})

const structureSubtitle = computed(() => {
  const data = structureData.value
  if (!data.items.length) {
    return hasAnySelection.value
      ? `${selectionSummaryText.value} 暂无可用于结构分析的数据。`
      : '暂无可用于结构分析的数据，请补充作物种植信息。'
  }
  const yearText = data.year ? `${data.year} 年` : hasYearFilter.value ? `${selectedYearLabel.value} 年` : '最新导入数据'
  if (!hasAnySelection.value) {
    return `${yearText}的作物${data.metricLabel}构成占比`
  }
  if (selectionFocusLabel.value) {
    return `${yearText}的 ${selectionFocusLabel.value} ${data.metricLabel}构成占比`
  }
  if (hasYearFilter.value) {
    return `${selectedYearLabel.value} 年的作物${data.metricLabel}构成占比`
  }
  return `${selectionSummaryText.value} 的 ${data.metricLabel}构成占比`
})

const mapSubtitle = computed(() => {
  if (!datasetMetrics.value.totalRecords) {
    return '导入省级产量数据后可查看区域热力。'
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return `${selectionSummaryText.value} 暂无省级分布数据，建议补充地区字段。`
  }
  const { items, excluded } = mapData.value
  if (!items.length) {
    return hasAnySelection.value
      ? `${selectionSummaryText.value} 暂无匹配到省级行政区的数据，请完善地区信息。`
      : '暂无匹配到省级行政区的数据，请完善地区信息。'
  }
  const focus = selectionFocusLabel.value || (hasAnySelection.value ? selectionSummaryText.value : '按省份汇总产量')
  const prefix = hasAnySelection.value ? `${focus} 省份产量分布` : '按省份汇总产量'
  return `${prefix}，覆盖 ${items.length} 个省级地区${excluded ? `，忽略 ${excluded} 条缺少省级信息的记录` : ''}`
})

const trendSummaryText = computed(() => {
  const data = trendChartData.value
  if (!data.series.length) {
    return ''
  }
  if (!hasAnySelection.value) {
    return `共展示 ${data.series.length} 种作物在 ${data.years.length} 个年份的产量变化`
  }
  const parts = []
  if (selectionFocusLabel.value) {
    parts.push(selectionFocusLabel.value)
  }
  if (hasYearFilter.value) {
    parts.push(`${selectedYearLabel.value} 年`)
  }
  const prefix = parts.length ? `${parts.join(' · ')} ` : ''
  return `${prefix}共展示 ${data.series.length} 种作物在 ${data.years.length} 个年份的产量变化`
})

const structureSummaryText = computed(() => {
  const data = structureData.value
  if (!data.items.length) {
    return ''
  }
  const yearText = data.year ? `${data.year} 年` : '最新数据'
  if (!hasAnySelection.value) {
    return `${yearText}共有 ${data.items.length} 种作物，${data.metricLabel}占比如上`
  }
  if (selectionFocusLabel.value) {
    return `${selectionFocusLabel.value} 在 ${yearText}共有 ${data.items.length} 种作物，${data.metricLabel}占比如上`
  }
  if (hasYearFilter.value) {
    return `${selectedYearLabel.value} 年共有 ${data.items.length} 种作物，${data.metricLabel}占比如上`
  }
  return `${selectionSummaryText.value} 共涉及 ${data.items.length} 种作物，${data.metricLabel}占比如上`
})

const mapSummaryText = computed(() => {
  const { items, excluded } = mapData.value
  if (!items.length) {
    if (!datasetMetrics.value.totalRecords) {
      return ''
    }
    return hasAnySelection.value
      ? `${selectionSummaryText.value} 暂无匹配的省级信息，请检查导入数据的地区字段。`
      : '请检查导入数据的地区字段，确保包含省级名称。'
  }
  const focus = selectionFocusLabel.value || (hasAnySelection.value ? selectionSummaryText.value : '热力图已聚合')
  const prefix = hasAnySelection.value ? `${focus} 热力图已聚合` : '热力图已聚合'
  return `${prefix} ${items.length} 个省级行政区${excluded ? `，${excluded} 条记录未匹配成功` : ''}`
})

const insightTips = computed(() => {
  if (!datasetMetrics.value.totalRecords) {
    return [
      '导入数据中心的农情数据后，系统将自动生成趋势、结构和热力图。',
      '请确保导入文件包含作物、地区和年份等关键字段。',
      '上传完成后可点击“刷新可视化数据”以获取最新图表。'
    ]
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return [
      `${selectionSummaryText.value} 暂无可视化数据，请确认数据中心已导入对应数据。`,
      '可通过筛选栏调整作物、年份或清空条件以查看整体趋势。',
      '若刚完成导入，请点击“刷新数据”同步最新记录。'
    ]
  }
  const tips = []
  const growth = trendGrowth.value
  if (growth && growth.growthRate != null) {
    const direction = growth.growthRate >= 0 ? '增长' : '下降'
    const scope = selectionFocusLabel.value || (hasAnySelection.value ? selectionSummaryText.value : '整体')
    tips.push(`${growth.latestYear} 年 ${scope.includes(growth.name) ? '' : scope + '中 '}${growth.name} 产量较上一年${direction} ${formatNumber(Math.abs(growth.growthRate), 1)}%，请结合库存与销售计划及时调整。`)
  } else {
    tips.push('建议持续补充多年度数据，以便识别作物产量波动趋势。')
  }
  const structureTop = topStructureCrop.value
  if (structureTop) {
    const yearText = structureData.value.year ? `${structureData.value.year} 年` : '最新年度'
    const focus = selectionFocusLabel.value ? `${selectionFocusLabel.value} ` : ''
    tips.push(`${yearText}中 ${focus}${structureTop.name} 占比最高，优先关注投入与风险控制。`)
  }
  const regionTop = topRegion.value
  if (regionTop) {
    const context = hasAnySelection.value ? selectionSummaryText.value : '整体数据'
    tips.push(`${regionTop.name} 是目前${context}中产量最高的省级地区，可结合物流能力进行重点调度。`)
  }
  while (tips.length < 3) {
    tips.push('导入更多地区或作物数据，有助于提升可视化分析的全面性。')
  }
  return tips.slice(0, 3)
})

const initCharts = () => {
  if (trendChartRef.value && !trendChartInstance.value) {
    trendChartInstance.value = echarts.init(trendChartRef.value)
    trendChartInstance.value.on('click', handleTrendPointClick)
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
  if (trendChartInstance.value) {
    trendChartInstance.value.off?.('click', handleTrendPointClick)
    trendChartInstance.value.dispose()
  }
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
  const chartType = selectedTrendType.value === 'bar' ? 'bar' : 'line'
  const legendData = data.series.map(item => item.name)
  const metricUnit = extractMetricUnit(data.metricLabel)
  const series = data.series.map((item, index) => {
    const color = colorPalette[index % colorPalette.length]
    const common = {
      name: item.name,
      type: chartType,
      emphasis: { focus: 'series' },
      data: item.data,
      itemStyle: { color }
    }
    if (chartType === 'line') {
      return {
        ...common,
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: hexToRgba(color, 0.32) },
            { offset: 1, color: hexToRgba(color, 0.05) }
          ])
        }
      }
    }
    return {
      ...common,
      barMaxWidth: 42,
      itemStyle: {
        color,
        borderRadius: [6, 6, 0, 0]
      }
    }
  })

  trendChartInstance.value.setOption({
    color: colorPalette,
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove|click',
      axisPointer: {
        type: chartType === 'bar' ? 'shadow' : 'line',
        snap: true,
        lineStyle: { color: '#3a5bff', width: 1 },
        shadowStyle: { opacity: 0.12 },
        label: {
          backgroundColor: '#3a5bff',
          borderRadius: 6,
          padding: [4, 8],
          formatter: ({ value }) => formatTrendAxisLabel(value)
        }
      },
      valueFormatter: value => formatTrendMetricValue(value, metricUnit)
    },
    legend: { data: legendData },
    grid: { left: 64, right: 24, top: 64, bottom: 48 },
    xAxis: { type: 'category', boundaryGap: chartType === 'bar', data: data.years },
    yAxis: {
      type: 'value',
      name: data.metricLabel,
      axisLabel: {
        show: true,
        color: '#374c7e',
        fontWeight: 500,
        formatter: formatTrendAxisLabel,
        margin: 14
      },
      nameTextStyle: {
        color: '#2b3f6d',
        fontWeight: 600,
        padding: [0, 0, 8, 0]
      },
      axisLine: {
        lineStyle: { color: '#a9b9e3' }
      },
      splitLine: {
        lineStyle: { color: '#e4ebff' }
      }
    },
    series
  })
}

const handleTrendPointClick = params => {
  if (!params || params.value == null || params.componentType !== 'series') {
    return
  }
  const { seriesName, name, value } = params
  const unit = extractMetricUnit(trendChartData.value.metricLabel)
  const valueText = formatTrendMetricValue(value, unit)
  const message = `${seriesName} - ${name}：${valueText}`
  if (typeof ElMessage.closeAll === 'function') {
    ElMessage.closeAll()
  }
  ElMessage({ type: 'info', message, duration: 3000, showClose: true })
  if (trendChartInstance.value) {
    trendChartInstance.value.dispatchAction({
      type: 'showTip',
      seriesIndex: params.seriesIndex,
      dataIndex: params.dataIndex
    })
    trendChartInstance.value.dispatchAction({
      type: 'highlight',
      seriesIndex: params.seriesIndex,
      dataIndex: params.dataIndex
    })
    if (typeof window !== 'undefined' && typeof window.setTimeout === 'function') {
      window.setTimeout(() => {
        trendChartInstance.value?.dispatchAction({
          type: 'downplay',
          seriesIndex: params.seriesIndex,
          dataIndex: params.dataIndex
        })
      }, 3200)
    }
  }
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

watch(selectedChartMode, () => {
  nextTick(() => {
    handleResize()
  })
})

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

watch(selectedTrendType, () => {
  const data = trendChartData.value
  updateTrendChart(data)
})

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
  padding-bottom: 32px;
}

.hero-card {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 32px;
  padding: 32px;
  border-radius: 24px;
  background: linear-gradient(120deg, #e8f2ff 0%, #f5f9ff 45%, #ffffff 100%);
  box-shadow: 0 28px 60px rgba(64, 118, 255, 0.16);
  overflow: hidden;
}

.hero-card::before {
  content: '';
  position: absolute;
  top: -160px;
  right: -160px;
  width: 420px;
  height: 420px;
  background: radial-gradient(circle at center, rgba(64, 118, 255, 0.28), transparent 68%);
  transform: rotate(10deg);
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
  font-size: 12px;
  font-weight: 600;
  color: #3a6cff;
  background: rgba(58, 108, 255, 0.12);
  border-radius: 999px;
  letter-spacing: 1.4px;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  color: #0d1b3d;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #405174;
  max-width: 560px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.hero-stat {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(58, 108, 255, 0.12);
  backdrop-filter: blur(4px);
}

.hero-stat-label {
  font-size: 12px;
  color: #5f6f94;
  margin-bottom: 6px;
}

.hero-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f3f95;
  margin-bottom: 6px;
}

.hero-stat-sub {
  font-size: 12px;
  color: #6c7fa5;
}

.hero-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.snapshot-card {
  border-radius: 20px;
  padding: 24px;
  background: linear-gradient(160deg, rgba(58, 108, 255, 0.12) 0%, rgba(255, 255, 255, 0.95) 100%);
  box-shadow: 0 18px 40px rgba(58, 108, 255, 0.12);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.snapshot-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a2f6b;
}

.snapshot-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(58, 108, 255, 0.15);
  color: #1d3a85;
  font-size: 12px;
  font-weight: 600;
}

.snapshot-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.snapshot-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #3a4d76;
}

.snapshot-label {
  color: #5f6f94;
}

.snapshot-value {
  font-weight: 600;
  color: #1d3a85;
}

.insight-card {
  border-radius: 20px;
  padding: 24px;
  background: linear-gradient(160deg, rgba(34, 197, 94, 0.08) 0%, rgba(255, 255, 255, 0.95) 85%);
  box-shadow: 0 18px 40px rgba(45, 153, 101, 0.12);
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #1a2f6b;
}

.insight-card-title {
  font-size: 16px;
  font-weight: 600;
}

.insight-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  line-height: 1.8;
  color: #3a4d76;
}

.page-alert {
  border-radius: 16px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  background: #f6f9ff;
  border: 1px solid rgba(58, 108, 255, 0.16);
  border-radius: 16px;
  padding: 16px 20px;
  color: #1d3a85;
  font-size: 14px;
}

.filter-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.filter-tag {
  border-radius: 999px;
  font-size: 12px;
  height: auto;
  line-height: 1.4;
  padding: 2px 10px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.chart-mode-select,
.category-select,
.crop-select,
.year-select {
  min-width: 200px;
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
  color: #4a638a;
}

.chart-grid {
  width: 100%;
}

.chart-card {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(64, 118, 255, 0.08);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chart-heading {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a2f6b;
}

.chart-subtitle {
  font-size: 13px;
  color: #5c6f92;
}

.chart-actions {
  display: flex;
  align-items: center;
}

.chart-toggle :deep(.el-radio-button__inner) {
  padding: 6px 14px;
}

.chart {
  width: 100%;
  height: 360px;
}

.map-chart {
  height: 420px;
}

@media (max-width: 992px) {
  .hero-card {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-controls {
    width: 100%;
  }

  .filter-controls :deep(.el-select),
  .filter-controls .chart-mode-select,
  .filter-controls .category-select,
  .filter-controls .crop-select,
  .filter-controls .year-select {
    flex: 1 1 100%;
    min-width: 0;
  }

  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .chart-actions {
    width: 100%;
  }

  .chart {
    height: 320px;
  }

  .map-chart {
    height: 360px;
  }
}
</style>
