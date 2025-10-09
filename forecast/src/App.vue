<script setup>
import { computed, onMounted, reactive } from 'vue'
import BarChart from './components/charts/BarChart.vue'
import LineChart from './components/charts/LineChart.vue'
import PieChart from './components/charts/PieChart.vue'

const state = reactive({
  activeSection: 'overview',
  overview: {
    loading: true,
    error: null,
    data: null
  },
  dataCenter: {
    initialized: false,
    loading: false,
    error: null,
    records: [],
    crops: [],
    regions: [],
    filters: {
      cropId: '',
      regionId: '',
      startYear: '',
      endYear: ''
    }
  }
})

const sectionMeta = {
  overview: {
    title: '云南农作物产量驾驶舱',
    subtitle: '汇聚省市县产量、面积与预测指标，为农业管理提供决策依据'
  },
  data: {
    title: '数据管理中心',
    subtitle: '多维筛选历史产量、播种面积等基础数据'
  },
  prediction: {
    title: '预测模型工坊',
    subtitle: 'ARIMA、Prophet、XGBoost、LSTM 等模型集成规划中'
  },
  decision: {
    title: '决策支持实验室',
    subtitle: '收益测算、种植推荐、情景模拟功能即将上线'
  }
}

const currentSection = computed(() => sectionMeta[state.activeSection] ?? sectionMeta.overview)

const formatNumber = value => {
  const number = Number(value)
  if (Number.isNaN(number)) {
    return '-'
  }
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(number)
}

const formatRevenue = value => {
  const number = Number(value)
  if (Number.isNaN(number)) {
    return '-'
  }
  return `${formatNumber(number)} 亿元`
}

const fetchSummary = async () => {
  state.overview.loading = true
  state.overview.error = null
  try {
    const response = await fetch('http://localhost:8080/api/dashboard/summary')
    if (!response.ok) {
      throw new Error('获取驾驶舱数据失败')
    }
    state.overview.data = await response.json()
  } catch (error) {
    state.overview.error = error.message || '数据加载异常，请稍后再试'
    state.overview.data = null
  } finally {
    state.overview.loading = false
  }
}

const fetchCrops = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/crops')
    if (!response.ok) {
      throw new Error('获取作物列表失败')
    }
    state.dataCenter.crops = await response.json()
  } catch (error) {
    state.dataCenter.error = error.message || '作物列表加载失败'
  }
}

const fetchRegions = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/regions')
    if (!response.ok) {
      throw new Error('获取地区列表失败')
    }
    state.dataCenter.regions = await response.json()
  } catch (error) {
    state.dataCenter.error = error.message || '地区列表加载失败'
  }
}

const fetchRecords = async () => {
  state.dataCenter.loading = true
  state.dataCenter.error = null
  try {
    const params = new URLSearchParams()
    const { cropId, regionId, startYear, endYear } = state.dataCenter.filters
    if (cropId) params.append('cropId', cropId)
    if (regionId) params.append('regionId', regionId)
    if (startYear) params.append('startYear', startYear)
    if (endYear) params.append('endYear', endYear)
    const query = params.toString()
    const response = await fetch(`http://localhost:8080/api/yields${query ? `?${query}` : ''}`)
    if (!response.ok) {
      throw new Error('获取历史数据失败')
    }
    state.dataCenter.records = await response.json()
  } catch (error) {
    state.dataCenter.error = error.message || '数据加载异常'
    state.dataCenter.records = []
  } finally {
    state.dataCenter.loading = false
  }
}

const initDataCenter = async () => {
  if (state.dataCenter.initialized) return
  await Promise.all([fetchCrops(), fetchRegions()])
  await fetchRecords()
  state.dataCenter.initialized = true
}

const changeSection = async section => {
  state.activeSection = section
  if (section === 'data') {
    await initDataCenter()
  }
}

const applyFilters = async () => {
  await fetchRecords()
}

const resetFilters = async () => {
  state.dataCenter.filters.cropId = ''
  state.dataCenter.filters.regionId = ''
  state.dataCenter.filters.startYear = ''
  state.dataCenter.filters.endYear = ''
  await fetchRecords()
}

const refreshSection = async () => {
  if (state.activeSection === 'overview') {
    await fetchSummary()
  } else if (state.activeSection === 'data') {
    await fetchRecords()
  }
}

const productionTrend = computed(() => state.overview.data?.productionTrend ?? [])
const cropStructure = computed(() => state.overview.data?.cropStructure ?? [])
const regionComparisons = computed(() => state.overview.data?.regionComparisons ?? [])
const forecastOutlook = computed(() => state.overview.data?.forecastOutlook ?? [])
const recentRecords = computed(() => state.overview.data?.recentRecords ?? [])

onMounted(fetchSummary)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <span class="brand__logo">农</span>
        <div>
          <p class="brand__title">云南农作物分析系统</p>
          <p class="brand__subtitle">Yunnan Crop Intelligence</p>
        </div>
      </div>
      <nav class="sidebar__menu">
        <p class="menu__title">功能导航</p>
        <ul>
          <li :class="{ active: state.activeSection === 'overview' }" @click="changeSection('overview')">数据驾驶舱</li>
          <li :class="{ active: state.activeSection === 'data' }" @click="changeSection('data')">数据管理</li>
          <li :class="{ active: state.activeSection === 'prediction' }" @click="changeSection('prediction')">预测工坊</li>
          <li :class="{ active: state.activeSection === 'decision' }" @click="changeSection('decision')">决策支持</li>
        </ul>
      </nav>
      <div class="sidebar__footer">
        <p class="footer__title">系统说明</p>
        <p class="footer__value">支持作物产量分析、模型预测、收益测算等核心功能建设</p>
      </div>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <h1>{{ currentSection.title }}</h1>
          <p class="topbar__subtitle">{{ currentSection.subtitle }}</p>
        </div>
        <button class="refresh-button" type="button" @click="refreshSection">刷新</button>
      </header>

      <section class="main-area">
        <template v-if="state.activeSection === 'overview'">
          <div v-if="state.overview.loading" class="loading">数据加载中...</div>
          <div v-else-if="state.overview.error" class="error">{{ state.overview.error }}</div>
          <template v-else>
            <section class="stats-grid">
              <article class="stat-card">
                <p class="stat-card__label">统计产量</p>
                <p class="stat-card__value">{{ formatNumber(state.overview.data?.totalProduction) }} 万吨</p>
                <p class="stat-card__hint">覆盖省内主要粮经作物</p>
              </article>
              <article class="stat-card">
                <p class="stat-card__label">播种面积</p>
                <p class="stat-card__value">{{ formatNumber(state.overview.data?.totalSownArea) }} 千公顷</p>
                <p class="stat-card__hint">包含省、市州统计口径</p>
              </article>
              <article class="stat-card">
                <p class="stat-card__label">平均单产</p>
                <p class="stat-card__value">{{ formatNumber(state.overview.data?.averageYield) }} 吨/公顷</p>
                <p class="stat-card__hint">依据最新年度数据测算</p>
              </article>
              <article class="stat-card">
                <p class="stat-card__label">历史记录</p>
                <p class="stat-card__value">{{ formatNumber(state.overview.data?.recordCount) }}</p>
                <p class="stat-card__hint">累计采集县级以上数据</p>
              </article>
            </section>

            <div class="panel-grid">
              <section class="panel">
                <div class="panel__header">
                  <h2>年度产量趋势</h2>
                  <span class="panel__hint">近年全省主要作物合计产量</span>
                </div>
                <LineChart :points="productionTrend" />
              </section>

              <section class="panel">
                <div class="panel__header">
                  <h2>作物结构占比</h2>
                  <span class="panel__hint">展示重点作物的产量与面积占比</span>
                </div>
                <PieChart :slices="cropStructure" />
              </section>

              <section class="panel">
                <div class="panel__header">
                  <h2>地区对比分析</h2>
                  <span class="panel__hint">对比主要州市产量与播种面积</span>
                </div>
                <BarChart :items="regionComparisons" />
              </section>
            </div>

            <div class="panel-grid panel-grid--two">
              <section class="panel">
                <div class="panel__header">
                  <h2>未来三年产量预测</h2>
                  <span class="panel__hint">基于 ARIMA 模型估算并给出置信区间</span>
                </div>
                <table v-if="forecastOutlook.length" class="table">
                  <thead>
                    <tr>
                      <th>年份</th>
                      <th>预测产量</th>
                      <th>置信区间</th>
                      <th>模型</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in forecastOutlook" :key="item.label">
                      <td>{{ item.label }}</td>
                      <td>{{ formatNumber(item.value) }} 万吨</td>
                      <td>{{ formatNumber(item.lowerBound) }} - {{ formatNumber(item.upperBound) }} 万吨</td>
                      <td>{{ item.model }}</td>
                    </tr>
                  </tbody>
                </table>
                <p v-else class="empty-hint">暂无预测结果</p>
              </section>

              <section class="panel">
                <div class="panel__header">
                  <h2>最新入库记录</h2>
                  <span class="panel__hint">展示最近采集的作物产量数据</span>
                </div>
                <table v-if="recentRecords.length" class="table">
                  <thead>
                    <tr>
                      <th>作物</th>
                      <th>地区</th>
                      <th>年份</th>
                      <th>播种面积</th>
                      <th>产量</th>
                      <th>平均单价</th>
                      <th>预估产值(亿元)</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="record in recentRecords" :key="record.cropName + record.regionName + record.year">
                      <td>{{ record.cropName }}</td>
                      <td>{{ record.regionName }}</td>
                      <td>{{ record.year }}</td>
                      <td>{{ formatNumber(record.sownArea) }} 千公顷</td>
                      <td>{{ formatNumber(record.production) }} 万吨</td>
                      <td>{{ record.averagePrice ? `${record.averagePrice.toFixed(2)} 元/公斤` : '-' }}</td>
                      <td>{{ formatRevenue(record.estimatedRevenue) }}</td>
                    </tr>
                  </tbody>
                </table>
                <p v-else class="empty-hint">暂无最新记录</p>
              </section>
            </div>
          </template>
        </template>

        <template v-else-if="state.activeSection === 'data'">
          <section class="panel">
            <div class="panel__header">
              <h2>查询条件</h2>
              <span class="panel__hint">支持按作物、地区、年份多维组合查询</span>
            </div>
            <form class="filter-form" @submit.prevent="applyFilters">
              <label>
                作物
                <select v-model="state.dataCenter.filters.cropId">
                  <option value="">全部作物</option>
                  <option v-for="crop in state.dataCenter.crops" :key="crop.id" :value="crop.id">{{ crop.name }}</option>
                </select>
              </label>
              <label>
                地区
                <select v-model="state.dataCenter.filters.regionId">
                  <option value="">全部地区</option>
                  <option v-for="region in state.dataCenter.regions" :key="region.id" :value="region.id">{{ region.name }}</option>
                </select>
              </label>
              <label>
                起始年份
                <input v-model="state.dataCenter.filters.startYear" placeholder="如 2020" type="number" />
              </label>
              <label>
                截止年份
                <input v-model="state.dataCenter.filters.endYear" placeholder="如 2023" type="number" />
              </label>
              <div class="filter-actions">
                <button type="submit" class="primary">应用筛选</button>
                <button type="button" @click="resetFilters">重置</button>
              </div>
            </form>
          </section>

          <section class="panel">
            <div class="panel__header">
              <h2>历史数据明细</h2>
              <span class="panel__hint">可用于导出分析、模型训练及报告编制</span>
            </div>
            <div v-if="state.dataCenter.loading" class="loading loading--sub">数据加载中...</div>
            <div v-else-if="state.dataCenter.error" class="error">{{ state.dataCenter.error }}</div>
            <div v-else-if="state.dataCenter.records.length" class="table-wrapper">
              <table class="table">
                <thead>
                  <tr>
                    <th>作物</th>
                    <th>类别</th>
                    <th>地区</th>
                    <th>年份</th>
                    <th>播种面积(千公顷)</th>
                    <th>产量(万吨)</th>
                    <th>平均单产</th>
                    <th>平均价格</th>
                    <th>预估产值(亿元)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in state.dataCenter.records" :key="item.id">
                    <td>{{ item.cropName }}</td>
                    <td>{{ item.cropCategory }}</td>
                    <td>{{ item.regionName }}</td>
                    <td>{{ item.year }}</td>
                    <td>{{ formatNumber(item.sownArea) }}</td>
                    <td>{{ formatNumber(item.production) }}</td>
                    <td>{{ formatNumber(item.yieldPerHectare) }} 吨/公顷</td>
                    <td>{{ item.averagePrice ? `${item.averagePrice.toFixed(2)} 元/公斤` : '-' }}</td>
                    <td>{{ formatRevenue(item.estimatedRevenue) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="empty-hint">暂无满足条件的数据，请调整筛选后重试</p>
          </section>
        </template>

        <template v-else>
          <section class="panel placeholder">
            <h2>功能建设中</h2>
            <p>该模块正在根据需求说明书规划设计，后续将提供模型接入、情景模拟、收益测算等功能。</p>
          </section>
        </template>
      </section>
    </main>
  </div>
</template>
