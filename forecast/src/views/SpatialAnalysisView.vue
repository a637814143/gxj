<template>
  <div class="spatial-page">
    <header class="page-header">
      <div>
        <p class="page-badge">空间分布分析 · 区域下钻</p>
        <h1 class="page-title">地图分层展示产量、预测区间与风险热力</h1>
        <p class="page-subtitle">
          结合论文中的空间分布分析需求，使用分层地图呈现不同区域的实测产量、预测可信区间与风险热力图，可在点击区域后继续下钻至更细粒度的作业单元。
        </p>
        <div class="breadcrumb">
          <span v-for="(item, idx) in drillPath" :key="item">
            <span class="crumb">{{ item }}</span>
            <span v-if="idx < drillPath.length - 1" class="crumb-divider">/</span>
          </span>
        </div>
      </div>
      <div class="page-actions">
        <el-select v-model="selectedScenario" class="scenario-select" size="small">
          <el-option v-for="scenario in scenarioOptions" :key="scenario.value" :label="scenario.label" :value="scenario.value" />
        </el-select>
        <el-switch v-model="showForecastPoints" active-text="预测区间" inactive-text="预测区间" />
        <el-switch v-model="showHeatmap" active-text="风险热力" inactive-text="风险热力" />
        <el-button v-if="canDrillBack" type="primary" size="small" plain @click="drillBack">返回上一级</el-button>
      </div>
    </header>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card class="map-card" shadow="never" v-loading="isLoading">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">{{ currentMap?.label || '空间分布' }}</div>
                <div class="card-desc">{{ currentMap?.description }}</div>
              </div>
              <div class="legend">
                <div class="legend-item">
                  <span class="legend-dot production"></span>
                  <span>区域产量（t/ha）</span>
                </div>
                <div class="legend-item">
                  <span class="legend-dot forecast"></span>
                  <span>预测区间中位点</span>
                </div>
                <div class="legend-item">
                  <span class="legend-dot risk"></span>
                  <span>风险热力</span>
                </div>
              </div>
            </div>
          </template>
          <el-alert
            v-if="loadError"
            :title="loadError"
            type="error"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          />
          <div v-if="!hasMapData && !isLoading" class="empty-tip">未获取到空间分布数据</div>
          <div v-else ref="mapRef" class="map-container" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="insight-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">区域细节</div>
                <div class="card-desc">实测产量、预测区间与风险等级</div>
              </div>
              <el-tag v-if="selectedRegion" type="success" effect="light">{{ selectedRegion.name }}</el-tag>
            </div>
          </template>
          <div v-if="selectedRegion" class="region-detail">
            <div class="detail-metric">
              <div class="detail-label">实测产量</div>
              <div class="detail-value">{{ selectedRegion.production.toFixed(1) }} t/ha</div>
              <div class="detail-hint">同比 {{ selectedRegion.deltaText }}</div>
            </div>
            <div class="detail-metric">
              <div class="detail-label">预测区间 (P80)</div>
              <div class="detail-value">{{ selectedRegion.forecastMin.toFixed(1) }} - {{ selectedRegion.forecastMax.toFixed(1) }} t/ha</div>
              <div class="detail-hint">区间跨度 {{ (selectedRegion.forecastMax - selectedRegion.forecastMin).toFixed(1) }} t/ha</div>
            </div>
            <div class="detail-metric">
              <div class="detail-label">综合风险</div>
              <el-progress :percentage="Math.round(selectedRegion.risk * 100)" status="exception" :stroke-width="10" />
              <div class="detail-hint">{{ riskLabels[selectedRegion.riskLabel] }}</div>
            </div>
          </div>
          <div v-else class="empty-tip">点击地图区域以查看细节</div>
        </el-card>

        <el-card class="insight-card" shadow="never" style="margin-top: 16px">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">风险热力 Top 4</div>
                <div class="card-desc">按当前层级与情景排序</div>
              </div>
            </div>
          </template>
          <ul class="risk-list">
            <li v-for="item in topRisks" :key="item.name" class="risk-row">
              <div class="risk-name">{{ item.name }}</div>
              <div class="risk-badge" :class="item.riskLabel">{{ riskLabels[item.riskLabel] }}</div>
              <div class="risk-value">{{ (item.risk * 100).toFixed(0) }}%</div>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { fetchSpatialMaps } from '../services/spatial'

const mapRef = ref(null)
let chartInstance

const mapDefinitions = ref({})
const isLoading = ref(false)
const loadError = ref('')
const drillStack = ref([])
const selectedScenario = ref('baseline')
const showHeatmap = ref(true)
const showForecastPoints = ref(true)
const selectedRegion = ref(null)

const scenarioOptions = [
  { value: 'baseline', label: '基准情景 (近三年均值)', multiplier: 1 },
  { value: 'optimistic', label: '丰收情景 (+5%)', multiplier: 1.05 },
  { value: 'stress', label: '气象压力 (-10%)', multiplier: 0.9 }
]

const riskLabels = {
  low: '低风险 · 适宜稳产',
  medium: '中风险 · 持续监测',
  high: '高风险 · 需重点预警'
}

const currentScenario = computed(() => scenarioOptions.find(item => item.value === selectedScenario.value) || scenarioOptions[0])

const currentMap = computed(() => mapDefinitions.value[drillStack.value[drillStack.value.length - 1]] || null)

const drillPath = computed(() => drillStack.value.map(key => mapDefinitions.value[key]?.label || key))

const canDrillBack = computed(() => drillStack.value.length > 1)

const adjustedRegions = computed(() => {
  if (!currentMap.value) {
    return []
  }
  const multiplier = currentScenario.value.multiplier
  return (currentMap.value?.regions || []).map(region => {
    const delta = multiplier - 1
    const adjustedRisk = Math.min(1, Math.max(0, region.risk + (delta < 0 ? 0.05 : delta * -0.2)))
    const riskLabel = adjustedRisk > 0.66 ? 'high' : adjustedRisk > 0.4 ? 'medium' : 'low'
    return {
      ...region,
      production: +(region.production * multiplier).toFixed(2),
      forecastMin: +(region.forecastMin * multiplier).toFixed(2),
      forecastMax: +(region.forecastMax * multiplier).toFixed(2),
      risk: +adjustedRisk.toFixed(2),
      riskLabel,
      deltaText: delta === 0 ? '持平' : `${delta > 0 ? '+' : ''}${(delta * 100).toFixed(0)}%`
    }
  })
})

const heatmapPoints = computed(() => {
  if (!currentMap.value) {
    return []
  }
  const multiplier = currentScenario.value.multiplier
  const base = currentMap.value?.heatmapPoints || []
  return base.map(point => {
    const riskBoost = multiplier < 1 ? 0.06 : multiplier > 1 ? -0.03 : 0
    const value = Math.min(1, Math.max(0, point.value[2] + riskBoost))
    return { ...point, value: [point.value[0], point.value[1], +value.toFixed(2)] }
  })
})

const topRisks = computed(() => adjustedRegions.value.slice().sort((a, b) => b.risk - a.risk).slice(0, 4))

const hasMapData = computed(() => !!currentMap.value && (!!currentMap.value.geoJson || (currentMap.value.regions || []).length))

const initializeSpatialMaps = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const { maps, rootKey } = await fetchSpatialMaps()
    const normalized = maps.reduce((acc, item) => {
      acc[item.key] = item
      return acc
    }, {})
    mapDefinitions.value = normalized

    const initialKey = rootKey || Object.keys(normalized)[0]
    drillStack.value = initialKey ? [initialKey] : []
    selectedRegion.value = adjustedRegions.value[0] || null
    renderChart()
  } catch (error) {
    loadError.value = error?.message || '获取空间分布数据失败'
    mapDefinitions.value = {}
    drillStack.value = []
    selectedRegion.value = null
    if (chartInstance) {
      chartInstance.clear()
    }
  } finally {
    isLoading.value = false
  }
}

const drillBack = () => {
  if (!canDrillBack.value) return
  drillStack.value = drillStack.value.slice(0, -1)
}

const handleRegionClick = params => {
  const target = adjustedRegions.value.find(item => item.name === params.name)
  if (target) {
    selectedRegion.value = target
  }
  if (!target?.childKey) return
  if (mapDefinitions.value[target.childKey]) {
    drillStack.value = [...drillStack.value, target.childKey]
  }
}

const renderChart = () => {
  if (!mapRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(mapRef.value)
  }

  const mapKey = currentMap.value?.mapKey
  if (!mapKey || !currentMap.value?.geoJson) {
    chartInstance.clear()
    return
  }

  if (!echarts.getMap(mapKey)) {
    echarts.registerMap(mapKey, currentMap.value.geoJson)
  }

  const regionData = adjustedRegions.value
  if (!regionData.length) {
    chartInstance.clear()
    return
  }
  const productionValues = regionData.map(item => item.production)
  const minProduction = Math.min(...productionValues)
  const maxProduction = Math.max(...productionValues)

  const mapSeries = {
    name: '区域产量',
    type: 'map',
    map: mapKey,
    roam: true,
    label: { show: false },
    itemStyle: {
      areaColor: '#e1f5f2',
      borderColor: '#7fc3b1'
    },
    emphasis: {
      itemStyle: { areaColor: '#c5e4dd' },
      label: { show: true, color: '#0b3d2e' }
    },
    data: regionData.map(item => ({
      name: item.name,
      value: item.production,
      forecast: [item.forecastMin, item.forecastMax],
      risk: item.risk
    }))
  }

  const scatterSeries = showForecastPoints.value
    ? {
        name: '预测区间',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        symbolSize: val => Math.max(10, Math.abs(val[3] - val[2]) * 15),
        itemStyle: { color: '#ff9f43', shadowBlur: 10, shadowColor: 'rgba(255,159,67,0.35)' },
        data: regionData.map(item => ({
          name: item.name,
          value: [...(item.center || [0, 0]), item.forecastMin, item.forecastMax],
          risk: item.risk
        })),
        tooltip: {
          valueFormatter: () => ''
        }
      }
    : null

  const heatmapSeries = showHeatmap.value
    ? {
        name: '风险热力',
        type: 'heatmap',
        coordinateSystem: 'geo',
        pointSize: 20,
        blurSize: 25,
        data: heatmapPoints.value
      }
    : null

  const riskVisualMap = showHeatmap.value
    ? {
        show: true,
        type: 'continuous',
        min: 0,
        max: 1,
        right: 20,
        bottom: 30,
        text: ['高风险', '低风险'],
        calculable: true,
        inRange: {
          color: ['#c7f0ff', '#51c4d3', '#ff6b6b']
        },
        seriesIndex: heatmapSeries ? (scatterSeries ? 2 : 1) : 1
      }
    : null

  chartInstance.setOption(
    {
      tooltip: {
        trigger: 'item',
        formatter: params => {
          const region = regionData.find(item => item.name === params.name)
          if (!region) return params.name
          return [
            `<div class="tooltip-title">${region.name}</div>`,
            `<div>实测产量：${region.production.toFixed(2)} t/ha</div>`,
            `<div>预测区间：${region.forecastMin.toFixed(2)} - ${region.forecastMax.toFixed(2)} t/ha</div>`,
            `<div>风险等级：${riskLabels[region.riskLabel]} (${(region.risk * 100).toFixed(0)}%)</div>`
          ].join('')
        }
      },
      visualMap: [
        {
          type: 'continuous',
          min: minProduction,
          max: maxProduction,
          left: 20,
          bottom: 30,
          text: ['高产量', '低产量'],
          inRange: {
            color: ['#eaf7f4', '#0b3d2e']
          },
          calculable: true,
          seriesIndex: 0
        },
        riskVisualMap
      ].filter(Boolean),
      geo: {
        map: mapKey,
        roam: true,
        zoom: 1.05,
        emphasis: { label: { show: false } },
        itemStyle: {
          areaColor: '#f2fbf8',
          borderColor: '#a6cfc1'
        }
      },
      series: [mapSeries, scatterSeries, heatmapSeries].filter(Boolean)
    },
    true
  )

  chartInstance.off('click')
  chartInstance.on('click', handleRegionClick)
}

watch([currentMap, selectedScenario, showForecastPoints, showHeatmap], () => {
  if (!currentMap.value) {
    selectedRegion.value = null
    if (chartInstance) {
      chartInstance.clear()
    }
    return
  }
  selectedRegion.value = adjustedRegions.value[0] || null
  renderChart()
})

watch(drillStack, () => {
  if (!currentMap.value) {
    selectedRegion.value = null
    if (chartInstance) {
      chartInstance.clear()
    }
    return
  }
  selectedRegion.value = adjustedRegions.value[0] || null
  renderChart()
})

onMounted(() => {
  initializeSpatialMaps()
  window.addEventListener('resize', resizeChart)
})

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>

<style scoped>
.spatial-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  background: linear-gradient(90deg, #0b3d2e, #0f5c46);
  color: #fff;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-badge {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.page-title {
  font-size: 22px;
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 10px;
  opacity: 0.85;
  line-height: 1.5;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
}

.crumb {
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
}

.crumb-divider {
  opacity: 0.5;
}

.map-card {
  height: 540px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
}

.card-desc {
  font-size: 13px;
  color: #607d8b;
}

.legend {
  display: flex;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #607d8b;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.legend-dot.production {
  background: #0b3d2e;
}

.legend-dot.forecast {
  background: #ff9f43;
}

.legend-dot.risk {
  background: linear-gradient(90deg, #c7f0ff, #51c4d3, #ff6b6b);
}

.map-container {
  width: 100%;
  height: 460px;
}

.insight-card {
  height: 100%;
}

.region-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-metric {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #fafafa;
}

.detail-label {
  font-size: 13px;
  color: #607d8b;
}

.detail-value {
  font-size: 18px;
  font-weight: 700;
  margin: 4px 0;
}

.detail-hint {
  font-size: 12px;
  color: #90a4ae;
}

.empty-tip {
  color: #9e9e9e;
  text-align: center;
  padding: 24px 0;
}

.risk-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #f9fbfa;
}

.risk-name {
  font-weight: 600;
}

.risk-badge {
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 12px;
  color: #fff;
}

.risk-badge.low {
  background: #4caf50;
}

.risk-badge.medium {
  background: #ff9800;
}

.risk-badge.high {
  background: #f44336;
}

.risk-value {
  font-weight: 700;
  color: #263238;
}

.tooltip-title {
  margin-bottom: 6px;
  font-weight: 700;
}
</style>
