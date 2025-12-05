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
        <el-select
          v-model="selectedRegion"
          class="region-select"
          :disabled="!regionSelectOptions.length"
          placeholder="筛选地区"
        >
          <el-option
            v-for="option in regionSelectOptions"
            :key="option.value"
            :label="option.display"
            :value="option.value"
          />
        </el-select>
        <el-button type="primary" :loading="isLoading" @click="loadYieldRecords">刷新数据</el-button>
      </div>
    </div>

    <el-card class="smart-panel" shadow="never">
      <div class="smart-panel-header">
        <div>
          <div class="smart-panel-title">智能图表推荐</div>
          <div class="smart-panel-desc">系统会根据数据特征自动匹配适合的图表类型，可随时切换为手动模式。</div>
        </div>
        <el-switch
          v-model="smartRecommendationEnabled"
          active-text="智能推荐"
          inactive-text="手动模式"
        />
      </div>
      <div class="smart-panel-body">
        <div class="smart-panel-group">
          <label>趋势分析</label>
          <div class="smart-panel-control">
            <el-select
              v-model="manualTrendType"
              size="small"
              class="smart-select"
              :disabled="smartRecommendationEnabled"
            >
              <el-option
                v-for="option in trendChartTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-tag :type="smartRecommendationEnabled ? 'success' : 'info'" class="recommend-tag">推荐：{{ chartTypeLabelMap.get(recommendedTrendType) }}</el-tag>
          </div>
        </div>
        <div class="smart-panel-group">
          <label>结构分析</label>
          <div class="smart-panel-control">
            <el-select
              v-model="manualStructureType"
              size="small"
              class="smart-select"
              :disabled="smartRecommendationEnabled"
            >
              <el-option
                v-for="option in structureChartTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-tag :type="smartRecommendationEnabled ? 'success' : 'info'" class="recommend-tag">推荐：{{ chartTypeLabelMap.get(recommendedStructureType) }}</el-tag>
          </div>
        </div>
        <div class="smart-panel-group">
          <label>分布分析</label>
          <div class="smart-panel-control">
            <el-select
              v-model="manualMapType"
              size="small"
              class="smart-select"
              :disabled="smartRecommendationEnabled"
            >
              <el-option
                v-for="option in mapChartTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-tag :type="smartRecommendationEnabled ? 'success' : 'info'" class="recommend-tag">推荐：{{ chartTypeLabelMap.get(recommendedMapType) }}</el-tag>
          </div>
        </div>
        <div class="smart-panel-group">
          <label>布局模板</label>
          <div class="smart-panel-control">
            <el-select
              :model-value="selectedLayoutTemplate"
              size="small"
              class="smart-select"
              @change="handleLayoutTemplateChange"
            >
              <el-option
                v-for="option in layoutTemplateOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
                :disabled="option.disabled"
              />
            </el-select>
          </div>
        </div>
      </div>
      <ul class="smart-panel-recommendations">
        <li v-for="message in chartRecommendationMessages" :key="message">{{ message }}</li>
      </ul>
    </el-card>

    <div class="chart-layout" :class="boardLayoutClass">
      <Draggable
        v-model="chartBlocks"
        item-key="id"
        class="chart-board"
        handle=".chart-drag-handle"
        :animation="220"
        :disabled="!hasMultipleCharts"
        @end="handleChartDragEnd"
      >
        <template #item="{ element }">
          <div v-if="isBlockVisible(element.id)" class="chart-tile" :class="`chart-tile--${element.id}`">
            <el-card class="chart-card" shadow="hover" v-loading="isLoading">
              <template #header>
                <div class="chart-header">
                  <div class="chart-heading">
                    <div class="chart-title" v-if="element.id === 'trend'">产量趋势分析</div>
                    <div class="chart-title" v-else-if="element.id === 'structure'">种植结构占比</div>
                    <div class="chart-title" v-else>地理分布热力</div>
                    <div class="chart-subtitle" v-if="element.id === 'trend'">{{ trendSubtitle }}</div>
                    <div class="chart-subtitle" v-else-if="element.id === 'structure'">{{ structureSubtitle }}</div>
                    <div class="chart-subtitle" v-else>{{ mapSubtitle }}</div>
                  </div>
                  <div class="chart-actions">
                    <span class="chart-drag-handle" aria-hidden="true">⋮⋮</span>
                    <template v-if="element.id === 'trend'">
                      <el-tag :type="smartRecommendationEnabled ? 'success' : 'info'" class="recommend-tag">推荐：{{ chartTypeLabelMap.get(recommendedTrendType) }}</el-tag>
                      <el-select
                        v-model="manualTrendType"
                        size="small"
                        class="chart-type-select"
                        :disabled="smartRecommendationEnabled"
                      >
                        <el-option
                          v-for="option in trendChartTypeOptions"
                          :key="option.value"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>
                    </template>
                    <template v-else-if="element.id === 'structure'">
                      <el-tag :type="smartRecommendationEnabled ? 'success' : 'info'" class="recommend-tag">推荐：{{ chartTypeLabelMap.get(recommendedStructureType) }}</el-tag>
                      <el-select
                        v-model="manualStructureType"
                        size="small"
                        class="chart-type-select"
                        :disabled="smartRecommendationEnabled"
                      >
                        <el-option
                          v-for="option in structureChartTypeOptions"
                          :key="option.value"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>
                    </template>
                    <template v-else>
                      <el-tag :type="smartRecommendationEnabled ? 'success' : 'info'" class="recommend-tag">推荐：{{ chartTypeLabelMap.get(recommendedMapType) }}</el-tag>
                      <el-select
                        v-model="manualMapType"
                        size="small"
                        class="chart-type-select"
                        :disabled="smartRecommendationEnabled"
                      >
                        <el-option
                          v-for="option in mapChartTypeOptions"
                          :key="option.value"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>
                    </template>
                  </div>
                </div>
              </template>
              <div class="chart-body">
                <template v-if="element.id === 'trend'">
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
                </template>
                <template v-else-if="element.id === 'structure'">
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
                </template>
                <template v-else>
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
                </template>
              </div>
            </el-card>
          </div>
        </template>
      </Draggable>
    </div>

    <el-drawer
      v-model="drilldownState.visible"
      title="联动明细"
      size="480px"
      append-to-body
    >
      <div class="drilldown-header">
        <h3 class="drilldown-title">{{ drilldownState.title }}</h3>
        <p class="drilldown-caption">{{ drilldownState.caption }}</p>
        <div v-if="drilldownState.summary" class="drilldown-summary">
          <div class="drilldown-summary-item">
            <span class="label">合计产量</span>
            <span class="value">{{ formatNumber(drilldownState.summary.totalProduction, 2) }} 万吨</span>
          </div>
          <div v-if="drilldownState.summary.totalArea > 0" class="drilldown-summary-item">
            <span class="label">合计面积</span>
            <span class="value">{{ formatNumber(drilldownState.summary.totalArea, 2) }} 千公顷</span>
          </div>
        </div>
      </div>
      <el-table
        v-if="drilldownState.records.length"
        :data="drilldownState.records"
        size="small"
        border
        class="drilldown-table"
      >
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="cropName" label="作物" min-width="120" />
        <el-table-column prop="regionName" label="地区" min-width="120" />
        <el-table-column label="产量(万吨)" min-width="120">
          <template #default="{ row }">
            {{ formatNumber(row.production ?? 0, 2) }}
          </template>
        </el-table-column>
        <el-table-column label="播种面积(千公顷)" min-width="150">
          <template #default="{ row }">
            {{ row.sownArea != null ? formatNumber(row.sownArea, 2) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无明细数据" :image-size="120" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import Draggable from 'vuedraggable'
import yunnanGeoJson from '../assets/maps/yunnan.json'
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

const chartPreferenceStorageKey = 'viz-chart-preferences'
const defaultChartPreferences = {
  smart: true,
  trendType: 'line',
  structureType: 'pie',
  mapType: 'choropleth',
  layout: 'balanced',
  layoutClass: 'layout-balanced',
  order: ['trend', 'structure', 'map']
}

const loadChartPreferences = () => {
  if (typeof window === 'undefined' || !window?.localStorage) {
    return { ...defaultChartPreferences }
  }
  try {
    const stored = window.localStorage.getItem(chartPreferenceStorageKey)
    if (!stored) {
      return { ...defaultChartPreferences }
    }
    const parsed = JSON.parse(stored)
    if (!parsed || typeof parsed !== 'object') {
      return { ...defaultChartPreferences }
    }
    return { ...defaultChartPreferences, ...parsed }
  } catch (error) {
    console.warn('[Visualization] Failed to parse chart preferences', error)
    return { ...defaultChartPreferences }
  }
}

const saveChartPreferences = preferences => {
  if (typeof window === 'undefined' || !window?.localStorage) {
    return
  }
  try {
    window.localStorage.setItem(chartPreferenceStorageKey, JSON.stringify(preferences))
  } catch (error) {
    console.warn('[Visualization] Failed to persist chart preferences', error)
  }
}

const chartLayoutTemplates = [
  { value: 'balanced', label: '均衡布局', order: ['trend', 'structure', 'map'], boardClass: 'layout-balanced' },
  { value: 'focus-trend', label: '趋势优先', order: ['trend', 'map', 'structure'], boardClass: 'layout-focus-trend' },
  { value: 'focus-structure', label: '结构洞察', order: ['structure', 'trend', 'map'], boardClass: 'layout-focus-structure' },
  { value: 'focus-geo', label: '区域洞察', order: ['map', 'trend', 'structure'], boardClass: 'layout-focus-geo' }
]

const chartLayoutTemplateMap = chartLayoutTemplates.reduce((map, template) => {
  map.set(template.value, template)
  return map
}, new Map())

const trendChartTypeOptions = [
  { label: '折线图', value: 'line' },
  { label: '面积图', value: 'area' },
  { label: '柱状图', value: 'bar' }
]

const structureChartTypeOptions = [
  { label: '饼图', value: 'pie' },
  { label: '旭日图', value: 'sunburst' },
  { label: '雷达图', value: 'radar' }
]

const mapChartTypeOptions = [
  { label: '分级着色地图', value: 'choropleth' },
  { label: '热力图', value: 'heatmap' }
]

const chartTypeLabelMap = [...trendChartTypeOptions, ...structureChartTypeOptions, ...mapChartTypeOptions].reduce(
  (map, option) => {
    map.set(option.value, option.label)
    return map
  },
  new Map()
)

const storedChartPreferences = loadChartPreferences()

const smartRecommendationEnabled = ref(Boolean(storedChartPreferences.smart))
const manualTrendType = ref(storedChartPreferences.trendType || defaultChartPreferences.trendType)
const manualStructureType = ref(storedChartPreferences.structureType || defaultChartPreferences.structureType)
const manualMapType = ref(storedChartPreferences.mapType || defaultChartPreferences.mapType)

const initialOrder = Array.isArray(storedChartPreferences.order)
  ? storedChartPreferences.order.filter(id => ['trend', 'structure', 'map'].includes(id))
  : defaultChartPreferences.order

const chartBlocks = ref(
  initialOrder.map(id => ({ id })).concat(
    ['trend', 'structure', 'map']
      .filter(id => !initialOrder.includes(id))
      .map(id => ({ id }))
  )
)

const selectedLayoutTemplate = ref(
  chartLayoutTemplateMap.has(storedChartPreferences.layout) ? storedChartPreferences.layout : defaultChartPreferences.layout
)

const activeLayoutClass = ref(
  storedChartPreferences.layoutClass || chartLayoutTemplateMap.get(selectedLayoutTemplate.value)?.boardClass ||
    defaultChartPreferences.layoutClass
)

const lastTemplateClass = ref(activeLayoutClass.value)

const layoutTemplateOptions = computed(() => [
  ...chartLayoutTemplates.map(template => ({ value: template.value, label: template.label })),
  { value: 'custom', label: '自定义布局', disabled: true }
])

const boardLayoutClass = computed(() => activeLayoutClass.value)

const isBlockVisible = id => {
  if (selectedChartMode.value === 'all') {
    return true
  }
  return selectedChartMode.value === id
}

const visibleChartBlocks = computed(() => chartBlocks.value.filter(block => isBlockVisible(block.id)))
const hasMultipleCharts = computed(() => visibleChartBlocks.value.length > 1)

const CATEGORY_ALL = '__ALL__'
const UNCATEGORIZED_LABEL = '未分类作物'
const ALL_CROPS = '__ALL_CROPS__'
const ALL_YEARS = '__ALL_YEARS__'
const ALL_REGIONS = '__ALL_REGIONS__'
const chartModeOptions = [
  { label: '展示全部图表', value: 'all' },
  { label: '仅产量趋势', value: 'trend' },
  { label: '仅结构占比', value: 'structure' },
  { label: '仅地理热力', value: 'map' }
]

const selectedCategory = ref(CATEGORY_ALL)
const selectedCrop = ref(ALL_CROPS)
const selectedYear = ref(ALL_YEARS)
const selectedRegion = ref(ALL_REGIONS)
const selectedChartMode = ref('all')

const numberFormatters = {
  0: new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }),
  1: new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }),
  2: new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
}

const colorPalette = ['#42a5f5', '#66bb6a', '#ffca28', '#ab47bc', '#ef5350', '#26a69a', '#8d6e63', '#29b6f6', '#9ccc65']

const REGION_SUFFIXES = [
  '特别行政区',
  '自治州',
  '自治县',
  '自治旗',
  '地区',
  '盟',
  '市',
  '州',
  '县',
  '区',
  '旗'
]

const REGION_SUFFIX_VARIANTS = ['市', '自治州', '州', '地区', '盟', '县', '区', '旗']

const sanitizeRegionText = raw => {
  if (!raw) return ''
  return String(raw)
    .trim()
    .replace(/[\s、，,·]/g, '')
    .replace(/^云南省?/, '')
    .replace(/^云南/, '')
    .replace(/^中国云南省?/, '')
}

const stripRegionSuffix = value => {
  if (!value) return ''
  for (const suffix of REGION_SUFFIXES) {
    if (value.endsWith(suffix) && value.length > suffix.length) {
      return value.slice(0, -suffix.length)
    }
  }
  return value
}

const buildRegionProfiles = (records = []) => {
  const list = Array.isArray(records) ? records : []
  const map = new Map()
  list.forEach(record => {
    const name = record?.regionName
    if (!name) return
    const displayName = String(name).trim()
    if (!displayName) return
    const sanitized = sanitizeRegionText(displayName)
    if (!sanitized) return
    const base = stripRegionSuffix(sanitized)
    const key = base || sanitized
    const profile = map.get(key) ?? {
      name: displayName,
      short: base || sanitized || displayName,
      aliases: new Set()
    }
    if (displayName.length > profile.name.length) {
      profile.name = displayName
    }
    profile.aliases.add(displayName)
    profile.aliases.add(sanitized)
    if (base) {
      profile.aliases.add(base)
    }
    if (profile.short) {
      REGION_SUFFIX_VARIANTS.forEach(suffix => {
        profile.aliases.add(`${profile.short}${suffix}`)
      })
    }
    map.set(key, profile)
  })
  return Array.from(map.values()).map(profile => ({
    name: profile.name,
    short: profile.short && profile.short !== profile.name ? profile.short : profile.name,
    aliases: Array.from(profile.aliases).filter(Boolean)
  }))
}

const buildRegionAliasMap = profiles => {
  const map = new Map()
  profiles.forEach(profile => {
    profile.aliases.forEach(alias => {
      const sanitized = sanitizeRegionText(alias)
      if (sanitized && !map.has(sanitized)) {
        map.set(sanitized, profile.name)
      }
    })
  })
  return map
}

const regionProfiles = computed(() => buildRegionProfiles(yieldRecords.value))
const regionAliasMap = computed(() => buildRegionAliasMap(regionProfiles.value))
const regionAliasEntries = computed(() => Array.from(regionAliasMap.value.entries()))
const regionCanonicalMap = computed(() => {
  const map = new Map()
  regionProfiles.value.forEach(profile => {
    const sanitized = sanitizeRegionText(profile.name)
    if (sanitized) {
      map.set(sanitized, profile.name)
    }
  })
  return map
})
const regionNameSet = computed(() => new Set(regionProfiles.value.map(profile => profile.name)))
const regionShortNameMap = computed(() => new Map(regionProfiles.value.map(profile => [profile.name, profile.short])))
const regionOrderMap = computed(() => new Map(regionProfiles.value.map((profile, index) => [profile.name, index])))

const extractCoordinatesFromGeometry = geometry => {
  if (!geometry) return []
  const { type, coordinates } = geometry
  if (!coordinates || !coordinates.length) {
    return []
  }
  if (type === 'Polygon') {
    return coordinates.flat().map(([lng, lat]) => [Number(lng), Number(lat)])
  }
  if (type === 'MultiPolygon') {
    return coordinates.flat(2).map(([lng, lat]) => [Number(lng), Number(lat)])
  }
  return []
}

const computeCoordinateCentroid = points => {
  if (!points.length) return null
  let x = 0
  let y = 0
  let count = 0
  points.forEach(point => {
    if (!Array.isArray(point) || point.length < 2) return
    const [lng, lat] = point
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
    x += lng
    y += lat
    count += 1
  })
  if (!count) return null
  return [Number((x / count).toFixed(4)), Number((y / count).toFixed(4))]
}

const prefectureCoordinateMap = (() => {
  const map = new Map()
  if (!yunnanGeoJson?.features) {
    return map
  }
  yunnanGeoJson.features.forEach(feature => {
    const name = feature?.properties?.name
    if (!name) return
    const centroid = computeCoordinateCentroid(extractCoordinatesFromGeometry(feature.geometry))
    if (centroid) {
      map.set(name, centroid)
    }
  })
  return map
})()

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

const normalizePrefectureName = name => {
  const sanitized = sanitizeRegionText(name)
  if (!sanitized) return ''
  const canonicalMap = regionCanonicalMap.value
  if (canonicalMap.has(sanitized)) {
    return canonicalMap.get(sanitized)
  }
  const direct = regionAliasMap.value.get(sanitized)
  if (direct) {
    return direct
  }
  const candidates = (() => {
    const base = stripRegionSuffix(sanitized)
    const list = [sanitized]
    if (base && base !== sanitized) {
      list.unshift(base)
    }
    if (base) {
      REGION_SUFFIX_VARIANTS.forEach(suffix => {
        list.push(`${base}${suffix}`)
      })
    }
    return Array.from(new Set(list))
  })()
  for (const candidate of candidates) {
    if (canonicalMap.has(candidate)) {
      return canonicalMap.get(candidate)
    }
    const mapped = regionAliasMap.value.get(candidate)
    if (mapped) {
      return mapped
    }
    if (regionNameSet.value.has(candidate)) {
      return candidate
    }
  }
  for (const [alias, canonical] of regionAliasEntries.value) {
    if (alias && sanitized.includes(alias)) {
      return canonical
    }
  }
  return ''
}

  const normalizedRecords = computed(() =>
    (Array.isArray(yieldRecords.value) ? yieldRecords.value : []).map(record => ({
      ...record,
      year: record?.year != null ? Number(record.year) : null,
      production: record?.production != null ? Number(record.production) : null,
      sownArea: record?.sownArea != null ? Number(record.sownArea) : null
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
  if (selectedRegion.value !== ALL_REGIONS) {
    records = records.filter(record => normalizePrefectureName(record?.regionName) === selectedRegion.value)
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

const regionOptions = computed(() => {
  const counter = new Map()
  categoryFilteredRecords.value.forEach(record => {
    const normalized = normalizePrefectureName(record?.regionName)
    if (!normalized) return
    counter.set(normalized, (counter.get(normalized) ?? 0) + 1)
  })
  return Array.from(counter.entries())
    .sort((a, b) => {
      const orderA = regionOrderMap.value.get(a[0]) ?? Number.POSITIVE_INFINITY
      const orderB = regionOrderMap.value.get(b[0]) ?? Number.POSITIVE_INFINITY
      if (orderA === orderB) {
        return a[0].localeCompare(b[0], 'zh-CN')
      }
      return orderA - orderB
    })
    .map(([label, count]) => {
      const short = regionShortNameMap.value.get(label)
      const displayLabel = short && short !== label ? `${label}（${short}）` : label
      return { label, value: label, count, short, displayLabel }
    })
})

const regionSelectOptions = computed(() => {
  const total = categoryFilteredRecords.value.length
  if (!total) {
    return []
  }
  const formattedTotal = formatNumber(total, 0)
  return [
    {
      label: '全部地区',
      value: ALL_REGIONS,
      count: total,
      display: `全部地区 · ${formattedTotal} 条记录`
    },
    ...regionOptions.value.map(option => ({
      ...option,
      display: `${option.displayLabel} · ${formatNumber(option.count, 0)} 条记录`
    }))
  ]
})

const selectedCategoryLabel = computed(() => (selectedCategory.value === CATEGORY_ALL ? '全部类别' : selectedCategory.value))
const selectedCropLabel = computed(() => (selectedCrop.value === ALL_CROPS ? '全部作物' : selectedCrop.value))
const selectedYearLabel = computed(() => (selectedYear.value === ALL_YEARS ? '全部年份' : `${selectedYear.value}`))
const selectedRegionLabel = computed(() => {
  if (selectedRegion.value === ALL_REGIONS) {
    return '全部地区'
  }
  const value = selectedRegion.value
  const short = regionShortNameMap.value.get(value)
  return short && short !== value ? `${value}（${short}）` : value
})

const isAllCategorySelected = computed(() => selectedCategory.value === CATEGORY_ALL)
const hasCropFilter = computed(() => selectedCrop.value !== ALL_CROPS)
const hasYearFilter = computed(() => selectedYear.value !== ALL_YEARS)
const hasRegionFilter = computed(() => selectedRegion.value !== ALL_REGIONS)
const hasAnySelection = computed(
  () => !isAllCategorySelected.value || hasCropFilter.value || hasYearFilter.value || hasRegionFilter.value
)

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
  if (hasRegionFilter.value) {
    parts.push(selectedRegionLabel.value)
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
  if (hasYearFilter.value) {
    parts.push(`${selectedYearLabel.value} 年`)
  }
  if (hasRegionFilter.value) {
    parts.push(selectedRegionLabel.value)
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

const chartInteractionLocks = reactive({ category: false })

const drilldownState = reactive({
  visible: false,
  title: '',
  caption: '',
  metricLabel: '',
  records: [],
  filters: {},
  summary: null
})

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

watch(regionSelectOptions, options => {
  if (!options.length) {
    selectedRegion.value = ALL_REGIONS
    return
  }
  if (selectedRegion.value === ALL_REGIONS) {
    return
  }
  const matched = options.some(option => option.value === selectedRegion.value)
  if (!matched) {
    selectedRegion.value = ALL_REGIONS
  }
})

watch(selectedCategory, () => {
  if (chartInteractionLocks.category) {
    chartInteractionLocks.category = false
    return
  }
  selectedCrop.value = ALL_CROPS
  selectedYear.value = ALL_YEARS
})

const buildDrilldownRecords = filters => {
  const { category, crop, year, region } = filters || {}
  return normalizedRecords.value.filter(record => {
    if (category && resolveCategoryLabel(record?.cropCategory) !== category) {
      return false
    }
    if (crop && resolveCropName(record?.cropName) !== crop) {
      return false
    }
    if (year && record?.year !== year) {
      return false
    }
    if (region && normalizePrefectureName(record?.regionName) !== region) {
      return false
    }
    return true
  })
}

const formatDrilldownSummary = (filters, records) => {
  const descriptions = []
  if (filters?.category) {
    descriptions.push(filters.category)
  }
  if (filters?.crop) {
    descriptions.push(filters.crop)
  }
  if (filters?.year) {
    descriptions.push(`${filters.year} 年`)
  }
  if (filters?.region) {
    descriptions.push(filters.region)
  }
  const totalProduction = records.reduce((sum, record) => sum + (record.production ?? 0), 0)
  const totalArea = records.reduce((sum, record) => sum + (record.sownArea ?? 0), 0)
  const metrics = []
  if (totalProduction > 0) {
    metrics.push(`产量 ${formatNumber(totalProduction, 1)} 万吨`)
  }
  if (totalArea > 0) {
    metrics.push(`播种面积 ${formatNumber(totalArea, 1)} 千公顷`)
  }
  const focus = descriptions.length ? descriptions.join(' · ') : '当前筛选'
  const base = `${focus} · ${formatNumber(records.length, 0)} 条记录`
  return metrics.length ? `${base} · ${metrics.join('，')}` : base
}

const openDrilldownPanel = (title, filters = {}, metricLabel = '') => {
  const resolvedFilters = { ...filters }
  const rows = buildDrilldownRecords(resolvedFilters)
  drilldownState.title = title
  drilldownState.metricLabel = metricLabel
  drilldownState.filters = resolvedFilters
  drilldownState.records = rows
  drilldownState.summary = {
    totalProduction: rows.reduce((sum, record) => sum + (record.production ?? 0), 0),
    totalArea: rows.reduce((sum, record) => sum + (record.sownArea ?? 0), 0)
  }
  drilldownState.caption = formatDrilldownSummary(resolvedFilters, rows)
  drilldownState.visible = true
}

const closeDrilldownPanel = () => {
  drilldownState.visible = false
}

const persistChartPreferences = () => {
  saveChartPreferences({
    smart: smartRecommendationEnabled.value,
    trendType: manualTrendType.value,
    structureType: manualStructureType.value,
    mapType: manualMapType.value,
    layout: selectedLayoutTemplate.value,
    layoutClass: activeLayoutClass.value,
    order: chartBlocks.value.map(block => block.id)
  })
}

const applyLayoutTemplate = value => {
  const template = chartLayoutTemplateMap.get(value)
  if (!template) {
    return
  }
  const blockMap = new Map(chartBlocks.value.map(block => [block.id, block]))
  const ordered = template.order.map(id => blockMap.get(id)).filter(Boolean)
  const remaining = chartBlocks.value.filter(block => !template.order.includes(block.id))
  chartBlocks.value = [...ordered, ...remaining]
  activeLayoutClass.value = template.boardClass
  lastTemplateClass.value = template.boardClass
}

const handleLayoutTemplateChange = value => {
  if (value === 'custom') {
    selectedLayoutTemplate.value = value
    activeLayoutClass.value = lastTemplateClass.value
    persistChartPreferences()
    return
  }
  selectedLayoutTemplate.value = value
  applyLayoutTemplate(value)
  persistChartPreferences()
}

const handleChartDragEnd = () => {
  selectedLayoutTemplate.value = 'custom'
  persistChartPreferences()
}

watch(smartRecommendationEnabled, persistChartPreferences)
watch(manualTrendType, persistChartPreferences)
watch(manualStructureType, persistChartPreferences)
watch(manualMapType, persistChartPreferences)
watch(activeLayoutClass, persistChartPreferences)

if (selectedLayoutTemplate.value !== 'custom') {
  applyLayoutTemplate(selectedLayoutTemplate.value)
}

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
    return { year: null, items: [], metricKey: 'production', metricLabel: '产量 (万吨)', hierarchy: [], cropCategories: {} }
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
  const categoryHierarchy = new Map()
  const cropCategoryMap = new Map()

  sourceRecords.forEach(record => {
    const cropName = resolveCropName(record?.cropName)
    const categoryLabel = resolveCategoryLabel(record?.cropCategory)
    const value = record[metricKey]
    if (value == null) return
    cropCategoryMap.set(cropName, categoryLabel)
    const previous = cropMap.get(cropName) ?? 0
    cropMap.set(cropName, previous + value)
    if (!categoryHierarchy.has(categoryLabel)) {
      categoryHierarchy.set(categoryLabel, { name: categoryLabel, value: 0, children: new Map() })
    }
    const categoryNode = categoryHierarchy.get(categoryLabel)
    categoryNode.value += value
    const childMap = categoryNode.children
    childMap.set(cropName, (childMap.get(cropName) ?? 0) + value)
  })

  const items = Array.from(cropMap.entries())
    .map(([name, value]) => ({ name, value: Number(value.toFixed(2)) }))
    .sort((a, b) => b.value - a.value)

  const hierarchy = Array.from(categoryHierarchy.values())
    .map(category => ({
      name: category.name,
      value: Number(category.value.toFixed(2)),
      children: Array.from(category.children.entries()).map(([name, value]) => ({
        name,
        value: Number(value.toFixed(2))
      }))
    }))
    .sort((a, b) => b.value - a.value)

  return {
    year: filteredByYear.length ? preferredYear : null,
    items,
    metricKey,
    metricLabel,
    hierarchy,
    cropCategories: Object.fromEntries(cropCategoryMap)
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
    const normalizedName = normalizePrefectureName(record?.regionName)
    if (!normalizedName || !regionNameSet.value.has(normalizedName)) {
      excluded += 1
      return
    }
    const value = record.production ?? 0
    const previous = regionMap.get(normalizedName) ?? 0
    regionMap.set(normalizedName, previous + value)
  })

  const items = Array.from(regionMap.entries())
    .sort((a, b) => {
      const orderA = regionOrderMap.value.get(a[0]) ?? Number.POSITIVE_INFINITY
      const orderB = regionOrderMap.value.get(b[0]) ?? Number.POSITIVE_INFINITY
      if (orderA === orderB) {
        return a[0].localeCompare(b[0], 'zh-CN')
      }
      return orderA - orderB
    })
    .map(([name, value]) => ({
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
    return '导入云南各地州的产量数据后可查看区域热力。'
  }
  if (hasAnySelection.value && !activeMetrics.value.totalRecords) {
    return `${selectionSummaryText.value} 暂无地州分布数据，建议补充地区字段。`
  }
  const { items, excluded } = mapData.value
  if (!items.length) {
    return hasAnySelection.value
      ? `${selectionSummaryText.value} 暂无匹配到地州行政区的数据，请完善地区信息。`
      : '暂无匹配到地州行政区的数据，请完善地区信息。'
  }
  const focus = selectionFocusLabel.value || (hasAnySelection.value ? selectionSummaryText.value : '按地州汇总产量')
  const prefix = hasAnySelection.value ? `${focus} 地州产量分布` : '按地州汇总产量'
  return `${prefix}，覆盖 ${items.length} 个地州（市）${excluded ? `，忽略 ${excluded} 条缺少地州信息的记录` : ''}`
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
      ? `${selectionSummaryText.value} 暂无匹配的地州信息，请检查导入数据的地区字段。`
      : '请检查导入数据的地区字段，确保包含地州名称（如大理州）。'
  }
  const focus = selectionFocusLabel.value || (hasAnySelection.value ? selectionSummaryText.value : '热力图已聚合')
  const prefix = hasAnySelection.value ? `${focus} 热力图已聚合` : '热力图已聚合'
  return `${prefix} ${items.length} 个地州（市）${excluded ? `，${excluded} 条记录未匹配成功` : ''}`
})

const recommendedTrendType = computed(() => {
  const data = trendChartData.value
  if (!data.series.length || !data.years.length) {
    return 'line'
  }
  if (data.years.length <= 3) {
    return 'bar'
  }
  if (data.series.length === 1 && data.years.length >= 6) {
    return 'area'
  }
  if (data.series.length >= 4 && data.years.length >= 4) {
    return 'area'
  }
  if (data.series.length >= 5) {
    return 'bar'
  }
  return 'line'
})

const recommendedStructureType = computed(() => {
  const { items } = structureData.value
  if (!items.length) {
    return 'pie'
  }
  if (items.length > 8) {
    return 'sunburst'
  }
  if (items.length <= 3) {
    return 'radar'
  }
  return 'pie'
})

const recommendedMapType = computed(() => {
  const { items } = mapData.value
  if (!items.length) {
    return 'choropleth'
  }
  if (items.length > 12) {
    return 'heatmap'
  }
  return 'choropleth'
})

const resolvedTrendType = computed(() => (smartRecommendationEnabled.value ? recommendedTrendType.value : manualTrendType.value || 'line'))
const resolvedStructureType = computed(() =>
  smartRecommendationEnabled.value ? recommendedStructureType.value : manualStructureType.value || 'pie'
)
const resolvedMapType = computed(() => (smartRecommendationEnabled.value ? recommendedMapType.value : manualMapType.value || 'choropleth'))

const chartRecommendationMessages = computed(() => {
  const messages = []
  const trendLabel = chartTypeLabelMap.get(recommendedTrendType.value)
  const structureLabel = chartTypeLabelMap.get(recommendedStructureType.value)
  const mapLabel = chartTypeLabelMap.get(recommendedMapType.value)

  if (trendLabel) {
    const data = trendChartData.value
    const reason = data.series.length
      ? `当前共有 ${formatNumber(data.series.length, 0)} 种作物、${formatNumber(data.years.length, 0)} 个年份的趋势数据`
      : '等待导入趋势数据'
    messages.push(`趋势分析推荐使用「${trendLabel}」，${reason}，便于观察整体走势与波动幅度。`)
  }

  if (structureLabel) {
    const { items } = structureData.value
    const reason = items.length
      ? `共识别 ${formatNumber(items.length, 0)} 种作物参与结构分析`
      : '等待可用于结构分析的数据'
    messages.push(`结构占比推荐使用「${structureLabel}」，${reason}，帮助识别优势作物。`)
  }

  if (mapLabel) {
    const { items } = mapData.value
    const reason = items.length
      ? `覆盖 ${formatNumber(items.length, 0)} 个地州（市）`
      : '暂未匹配到地州数据'
    messages.push(`区域分布推荐使用「${mapLabel}」，${reason}，突出区域冷热与重点区域。`)
  }

  return messages
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
  if (smartRecommendationEnabled.value) {
    tips.push(...chartRecommendationMessages.value.slice(0, 1))
  }
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
    tips.push(`${regionTop.name} 是目前${context}中产量最高的地州，可结合物流能力进行重点调度。`)
  }
  if (!smartRecommendationEnabled.value && tips.length < 3) {
    tips.unshift(...chartRecommendationMessages.value.slice(0, Math.max(0, 2 - tips.length)))
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
    structureChartInstance.value.on('click', handleStructureSliceClick)
  }
  if (mapChartRef.value && !mapChartInstance.value) {
    if (!echarts.getMap('yunnan')) {
      echarts.registerMap('yunnan', yunnanGeoJson)
    }
    mapChartInstance.value = echarts.init(mapChartRef.value)
    mapChartInstance.value.on('click', handleMapRegionClick)
  }
}

const disposeCharts = () => {
  if (trendChartInstance.value) {
    trendChartInstance.value.off?.('click', handleTrendPointClick)
    trendChartInstance.value.dispose()
  }
  if (structureChartInstance.value) {
    structureChartInstance.value.off?.('click', handleStructureSliceClick)
    structureChartInstance.value.dispose()
  }
  if (mapChartInstance.value) {
    mapChartInstance.value.off?.('click', handleMapRegionClick)
    mapChartInstance.value.dispose()
  }
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

const updateTrendChart = (data, variant = resolvedTrendType.value) => {
  trendHasData.value = data.series.length > 0 && data.years.length > 0
  if (!trendChartRef.value || !trendChartInstance.value) return
  if (!trendHasData.value) {
    trendChartInstance.value.clear()
    return
  }
  const chartVariant = variant || 'line'
  const isBar = chartVariant === 'bar'
  const isArea = chartVariant === 'area'
  const chartType = isBar ? 'bar' : 'line'
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
        areaStyle: isArea
          ? {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: hexToRgba(color, 0.36) },
                { offset: 1, color: hexToRgba(color, 0.04) }
              ])
            }
          : undefined
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
        type: isBar ? 'shadow' : 'line',
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
  const numericYear = Number(name)
  if (Number.isFinite(numericYear)) {
    selectedYear.value = numericYear
  }
  selectedCrop.value = resolveCropName(seriesName)
  openDrilldownPanel(`${seriesName} · ${name}`, {
    crop: resolveCropName(seriesName),
    year: Number.isFinite(numericYear) ? numericYear : undefined,
    category: structureData.value.cropCategories?.[resolveCropName(seriesName)],
    region: hasRegionFilter.value ? selectedRegion.value : undefined
  }, trendChartData.value.metricLabel)
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

const handleStructureSliceClick = params => {
  if (!params?.name) {
    return
  }
  const name = params.name
  const variant = resolvedStructureType.value
  const filters = {}
  if (variant === 'sunburst') {
    const path = Array.isArray(params.treePathInfo)
      ? params.treePathInfo.map(node => node?.name).filter(Boolean)
      : []
    if (params.data?.children?.length) {
      chartInteractionLocks.category = true
      selectedCategory.value = name
      filters.category = name
    } else if (path.length >= 2) {
      const categoryName = path[path.length - 2]
      chartInteractionLocks.category = true
      selectedCategory.value = categoryName
      filters.category = categoryName
      selectedCrop.value = name
      filters.crop = name
    } else {
      selectedCrop.value = name
      filters.crop = name
    }
  } else {
    const cropCategories = structureData.value.cropCategories || {}
    const categoryName = cropCategories[name]
    if (categoryName) {
      chartInteractionLocks.category = true
      selectedCategory.value = categoryName
      filters.category = categoryName
    }
    selectedCrop.value = name
    filters.crop = name
  }
  if (hasYearFilter.value) {
    filters.year = Number(selectedYear.value)
  }
  if (hasRegionFilter.value) {
    filters.region = selectedRegion.value
  }
  openDrilldownPanel(`${name} 作物结构`, filters, structureData.value.metricLabel)
}

const handleMapRegionClick = params => {
  const regionName = normalizePrefectureName(params?.name) || params?.name
  if (!regionName) {
    return
  }
  selectedRegion.value = regionName
  const filters = {
    region: regionName
  }
  if (!isAllCategorySelected.value) {
    filters.category = selectedCategory.value
  }
  if (hasCropFilter.value) {
    filters.crop = selectedCrop.value
  }
  if (hasYearFilter.value) {
    filters.year = Number(selectedYear.value)
  }
  openDrilldownPanel(`${regionName} 区域分布`, filters, mapData.value.items.length ? '产量 (万吨)' : '')
}

const updateStructureChart = (data, variant = resolvedStructureType.value) => {
  structureHasData.value = data.items.length > 0
  if (!structureChartRef.value || !structureChartInstance.value) return
  if (!structureHasData.value) {
    structureChartInstance.value.clear()
    return
  }
  const chartVariant = variant || 'pie'
  const palette = colorPalette

  if (chartVariant === 'sunburst') {
    const sunburstData = (Array.isArray(data.hierarchy) ? data.hierarchy : []).map((node, index) => ({
      ...node,
      itemStyle: { color: palette[index % palette.length] }
    }))
    structureChartInstance.value.setOption({
      tooltip: {
        trigger: 'item',
        formatter: params => {
          const percent = params.percent != null ? `${formatNumber(params.percent, 1)}%` : ''
          const metric = params.value != null ? `${data.metricLabel}：${formatNumber(params.value, 1)}` : ''
          return [params.treePathInfo?.map(node => node.name).filter(Boolean).join(' / '), metric, percent].filter(Boolean).join('<br />')
        }
      },
      series: [
        {
          type: 'sunburst',
          radius: [0, '80%'],
          data: sunburstData,
          sort: (a, b) => b.value - a.value,
          emphasis: { focus: 'ancestor' },
          levels: [
            {},
            { r0: '10%', r: '45%', itemStyle: { borderWidth: 2 } },
            { r0: '45%', r: '80%', itemStyle: { borderWidth: 2 } }
          ]
        }
      ]
    })
    return
  }

  if (chartVariant === 'radar') {
    const topItems = data.items.slice(0, 6)
    const maxValue = topItems.reduce((max, item) => (item.value > max ? item.value : max), 0) * 1.2 || 1
    structureChartInstance.value.setOption({
      tooltip: {
        trigger: 'item',
        formatter: params => {
          const values = params.value
            .map((value, idx) => `${topItems[idx]?.name ?? ''}：${formatNumber(value, 1)}`)
            .join('<br />')
          return `${params.name}<br />${values}`
        }
      },
      radar: {
        indicator: topItems.map(item => ({ name: item.name, max: maxValue })),
        radius: '70%',
        axisName: { color: '#1f2f5c', fontWeight: 600 }
      },
      series: [
        {
          type: 'radar',
          name: data.metricLabel,
          data: [
            {
              value: topItems.map(item => item.value),
              name: data.metricLabel,
              areaStyle: { color: hexToRgba(palette[0], 0.25) },
              lineStyle: { color: palette[0], width: 2 },
              itemStyle: { color: palette[0] }
            }
          ]
        }
      ]
    })
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

const updateMapChart = (data, variant = resolvedMapType.value) => {
  mapHasData.value = data.items.length > 0
  if (!mapChartRef.value || !mapChartInstance.value) return
  if (!mapHasData.value) {
    mapChartInstance.value.clear()
    return
  }
  const chartVariant = variant || 'choropleth'
  const geoOption = {
    map: 'yunnan',
    roam: true,
    zoom: 1.05,
    layoutCenter: ['50%', '55%'],
    layoutSize: '90%',
    label: { show: true, color: '#0b3c5d', fontWeight: 600, fontSize: 12 },
    itemStyle: { borderColor: '#ffffff', borderWidth: 1, areaColor: '#e3f2fd' },
    emphasis: {
      label: { show: true, color: '#0b3c5d', fontWeight: 700 },
      itemStyle: { areaColor: '#ffe082', shadowBlur: 18, shadowColor: 'rgba(255, 193, 7, 0.35)' }
    }
  }

  if (chartVariant === 'heatmap') {
    const heatmapData = data.items
      .map(item => {
        const coord = prefectureCoordinateMap.get(item.name)
        if (!coord) return null
        return { name: item.name, value: [...coord, item.value] }
      })
      .filter(Boolean)

    mapChartInstance.value.setOption({
      tooltip: {
        trigger: 'item',
        formatter: params => {
          const value = Array.isArray(params.value) ? params.value[2] : params.value
          return `${params.name}<br />产量：${formatNumber(value, 1)} 万吨`
        }
      },
      visualMap: {
        min: data.min,
        max: data.max,
        left: 20,
        bottom: 20,
        text: ['高', '低'],
        calculable: true,
        inRange: { color: ['#e8f8f5', '#76d7c4', '#1abc9c'] }
      },
      geo: geoOption,
      series: [
        {
          name: '地州产量',
          type: 'map',
          geoIndex: 0,
          data: data.items
        },
        {
          name: '热力强度',
          type: 'heatmap',
          coordinateSystem: 'geo',
          data: heatmapData,
          pointSize: 18,
          blurSize: 32,
          itemStyle: { opacity: 0.85 }
      }
    ]
  })
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
      inRange: { color: ['#e8f8f5', '#76d7c4', '#1abc9c'] }
    },
    geo: geoOption,
    series: [
      {
        name: '地州产量',
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
    updateTrendChart(data, resolvedTrendType.value)
  },
  { deep: true, immediate: true }
)

watch(
  resolvedTrendType,
  () => {
    updateTrendChart(trendChartData.value, resolvedTrendType.value)
  }
)

watch(
  structureData,
  data => {
    updateStructureChart(data, resolvedStructureType.value)
  },
  { deep: true, immediate: true }
)

watch(
  resolvedStructureType,
  () => {
    updateStructureChart(structureData.value, resolvedStructureType.value)
  }
)

watch(
  mapData,
  data => {
    updateMapChart(data, resolvedMapType.value)
  },
  { deep: true, immediate: true }
)

watch(
  resolvedMapType,
  () => {
    updateMapChart(mapData.value, resolvedMapType.value)
  }
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
.year-select,
.region-select {
  min-width: 200px;
}

.smart-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px 28px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 20px 42px rgba(64, 118, 255, 0.08);
}

.smart-panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.smart-panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a2f6b;
}

.smart-panel-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #5c6f92;
  max-width: 640px;
}

.smart-panel-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;
}

.smart-panel-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.smart-panel-group label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: #5f6f94;
}

.smart-panel-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.smart-select {
  min-width: 160px;
}

.smart-panel-recommendations {
  margin: 0;
  padding-left: 20px;
  color: #5c6f92;
  font-size: 13px;
  line-height: 1.8;
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
  gap: 12px;
}

.chart-type-select {
  min-width: 140px;
}

.recommend-tag {
  font-size: 12px;
}

.chart-drag-handle {
  font-size: 20px;
  line-height: 1;
  color: #9aa7c3;
  cursor: grab;
  user-select: none;
}

.chart-drag-handle:active {
  cursor: grabbing;
}

.chart-layout {
  width: 100%;
}


.chart-board {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.chart-layout.layout-balanced .chart-board {
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.chart-layout.layout-focus-trend .chart-board,
.chart-layout.layout-focus-structure .chart-board,
.chart-layout.layout-focus-geo .chart-board {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.chart-layout.layout-focus-trend .chart-tile--trend,
.chart-layout.layout-focus-structure .chart-tile--structure,
.chart-layout.layout-focus-geo .chart-tile--map {
  grid-column: span 2;
}

.chart-tile {
  min-width: 0;
}

.drilldown-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.drilldown-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a2f6b;
}

.drilldown-caption {
  margin: 0;
  font-size: 13px;
  color: #5c6f92;
}

.drilldown-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #4b5f82;
}

.drilldown-summary-item {
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.drilldown-summary-item .value {
  font-weight: 600;
  color: #1f3f95;
}

.drilldown-table {
  max-height: 420px;
  overflow: auto;
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
  .filter-controls .year-select,
  .filter-controls .region-select {
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

  .chart-board {
    grid-template-columns: 1fr;
  }

  .chart-layout.layout-focus-trend .chart-tile--trend,
  .chart-layout.layout-focus-structure .chart-tile--structure,
  .chart-layout.layout-focus-geo .chart-tile--map {
    grid-column: span 1;
  }
}
</style>
