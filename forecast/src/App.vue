<script setup>
import { computed, onMounted, reactive } from 'vue'
import TrendChart from './components/TrendChart.vue'

const state = reactive({
  activeSection: 'overview',
  overview: {
    loading: true,
    error: null,
    summary: null
  },
  assets: {
    initialized: false,
    loading: false,
    error: null,
    items: [],
    categories: [],
    categoryLoading: false,
    categoryError: null,
    filters: {
      search: '',
      categoryId: '',
      status: '',
      expiringSoon: false
    }
  }
})

const sectionMeta = {
  overview: {
    title: '概览数据',
    subtitle: '实时掌握软件资产运行与预测状况'
  },
  statistics: {
    title: '统计中心',
    subtitle: '更多可视化统计即将上线'
  },
  finance: {
    title: '财务中心',
    subtitle: '费用与预算模块建设中'
  },
  prediction: {
    title: '预测中心',
    subtitle: '高级预测功能正在规划中'
  },
  assets: {
    title: '资产中心',
    subtitle: '查看资产清单与分类概览'
  },
  placeholder: {
    title: '功能建设中',
    subtitle: '敬请期待更多内容'
  }
}

const currentSection = computed(() => sectionMeta[state.activeSection] ?? sectionMeta.placeholder)

const statusLabels = {
  ACTIVE: '正常运行',
  MAINTENANCE: '维护中',
  RETIRED: '已退役'
}

const formatDate = value => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}

const formatCurrency = value => {
  if (value === null || value === undefined) return '-'
  const number = Number(value)
  if (Number.isNaN(number)) {
    return value
  }
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(number)
}

const formatStatus = value => {
  if (!value) return '-'
  return statusLabels[value] ?? value
}

const formatDays = value => {
  if (value === null || value === undefined) return '-'
  if (value < 0) {
    return `逾期 ${Math.abs(value)} 天`
  }
  return `${value} 天`
}

const fetchSummary = async () => {
  state.overview.loading = true
  state.overview.error = null
  try {
    const response = await fetch('http://localhost:8080/api/dashboard/summary')
    if (!response.ok) {
      throw new Error('获取概览信息失败，请稍后重试')
    }
    state.overview.summary = await response.json()
  } catch (error) {
    state.overview.error = error.message || '数据加载异常'
  } finally {
    state.overview.loading = false
  }
}

const fetchAssetCategories = async () => {
  state.assets.categoryLoading = true
  state.assets.categoryError = null
  try {
    const response = await fetch('http://localhost:8080/api/software/categories')
    if (!response.ok) {
      throw new Error('获取分类统计失败，请稍后再试')
    }
    state.assets.categories = await response.json()
  } catch (error) {
    state.assets.categoryError = error.message || '分类数据加载异常'
  } finally {
    state.assets.categoryLoading = false
  }
}

const fetchAssets = async () => {
  state.assets.loading = true
  state.assets.error = null
  try {
    const params = new URLSearchParams()
    const { search, categoryId, status, expiringSoon } = state.assets.filters
    if (search && search.trim()) {
      params.append('search', search.trim())
    }
    if (categoryId) {
      params.append('categoryId', categoryId)
    }
    if (status) {
      params.append('status', status)
    }
    if (expiringSoon) {
      params.append('expiringSoon', 'true')
    }
    const query = params.toString()
    const response = await fetch(`http://localhost:8080/api/software${query ? `?${query}` : ''}`)
    if (!response.ok) {
      throw new Error('获取软件资产失败，请稍后再试')
    }
    state.assets.items = await response.json()
  } catch (error) {
    state.assets.error = error.message || '资产数据加载异常'
    state.assets.items = []
  } finally {
    state.assets.loading = false
  }
}

const loadAssetsSection = async () => {
  if (state.assets.initialized) {
    return
  }
  await Promise.all([fetchAssetCategories(), fetchAssets()])
  state.assets.initialized = true
}

const changeSection = async section => {
  state.activeSection = section
  if (section === 'assets') {
    await loadAssetsSection()
  }
}

const applyAssetFilters = async () => {
  if (!state.assets.initialized) {
    state.assets.initialized = true
  }
  await fetchAssets()
}

const resetAssetFilters = async () => {
  state.assets.filters.search = ''
  state.assets.filters.categoryId = ''
  state.assets.filters.status = ''
  state.assets.filters.expiringSoon = false
  await fetchAssets()
}

const refreshSection = async () => {
  if (state.activeSection === 'overview') {
    await fetchSummary()
  } else if (state.activeSection === 'assets') {
    await Promise.all([fetchAssetCategories(), fetchAssets()])
  }
}

onMounted(fetchSummary)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <span class="brand__logo">软</span>
        <div>
          <p class="brand__title">软件资产预测平台</p>
          <p class="brand__subtitle">Software Asset Forecast</p>
        </div>
      </div>
      <nav class="sidebar__menu">
        <p class="menu__title">导航中心</p>
        <ul>
          <li :class="{ active: state.activeSection === 'overview' }" @click="changeSection('overview')">
            概览看板
          </li>
          <li :class="{ active: state.activeSection === 'statistics' }" @click="changeSection('statistics')">
            统计中心
          </li>
          <li :class="{ active: state.activeSection === 'finance' }" @click="changeSection('finance')">
            财务中心
          </li>
          <li :class="{ active: state.activeSection === 'prediction' }" @click="changeSection('prediction')">
            预测中心
          </li>
          <li :class="{ active: state.activeSection === 'assets' }" @click="changeSection('assets')">
            资产中心
          </li>
        </ul>
      </nav>
      <div class="sidebar__footer">
        <p class="footer__title">预测进度</p>
        <p class="footer__value">76% 本月计划完成</p>
      </div>
    </aside>
    <main class="content">
      <header class="topbar">
        <div>
          <h1>{{ currentSection.title }}</h1>
          <p class="topbar__subtitle">{{ currentSection.subtitle }}</p>
        </div>
        <button class="refresh-button" type="button" @click="refreshSection">
          刷新数据
        </button>
      </header>
      <section class="main-area">
        <template v-if="state.activeSection === 'overview'">
          <div v-if="state.overview.loading" class="loading">数据加载中...</div>
          <div v-else-if="state.overview.error" class="error">{{ state.overview.error }}</div>
          <template v-else>
            <section class="stats-grid">
              <article class="stat-card">
                <p class="stat-card__label">软件数量</p>
                <p class="stat-card__value">{{ state.overview.summary?.softwareCount ?? '-' }}</p>
                <p class="stat-card__hint">包含所有纳管软件资产</p>
              </article>
              <article class="stat-card">
                <p class="stat-card__label">已预测软件</p>
                <p class="stat-card__value">{{ state.overview.summary?.predictedSoftwareCount ?? '-' }}</p>
                <p class="stat-card__hint">至少拥有一条预测记录的软件</p>
              </article>
              <article class="stat-card">
                <p class="stat-card__label">预测次数</p>
                <p class="stat-card__value">{{ state.overview.summary?.predictionCount ?? '-' }}</p>
                <p class="stat-card__hint">近周期内系统生成的预测</p>
              </article>
              <article class="stat-card">
                <p class="stat-card__label">软件类别</p>
                <p class="stat-card__value">{{ state.overview.summary?.categoryCount ?? '-' }}</p>
                <p class="stat-card__hint">当前纳管的分类维度</p>
              </article>
            </section>

            <div class="panel-grid">
              <section class="panel">
                <div class="panel__header">
                  <h2>近期预测趋势</h2>
                  <span class="panel__hint">展示最近六个月预测生成情况</span>
                </div>
                <TrendChart :points="state.overview.summary?.recentPredictionTrend ?? []" />
              </section>

              <section class="panel">
                <div class="panel__header">
                  <h2>即将到期的维护</h2>
                  <span class="panel__hint">未来 120 天需要关注的资产</span>
                </div>
                <div class="table-wrapper" v-if="state.overview.summary?.upcomingRenewals?.length">
                  <table>
                    <thead>
                      <tr>
                        <th>软件名称</th>
                        <th>类别</th>
                        <th>维护到期</th>
                        <th>剩余天数</th>
                        <th>授权类型</th>
                        <th>预测覆盖</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="item in state.overview.summary.upcomingRenewals" :key="item.softwareName">
                        <td>
                          <span class="name">{{ item.softwareName }}</span>
                          <span class="muted">{{ item.vendor }}</span>
                        </td>
                        <td>{{ item.categoryName || '-' }}</td>
                        <td>{{ formatDate(item.maintenanceExpiryDate) }}</td>
                        <td>
                          <span :class="['days', item.daysRemaining <= 30 ? 'danger' : '']">
                            {{ Math.max(item.daysRemaining ?? 0, 0) }} 天
                          </span>
                        </td>
                        <td>{{ item.licenseType || '-' }}</td>
                        <td>
                          <span :class="['badge', item.hasRecentPrediction ? 'badge--success' : 'badge--warning']">
                            {{ item.hasRecentPrediction ? '已纳入' : '待预测' }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <p v-else class="empty-hint">暂无需要关注的维护计划</p>
              </section>

              <section class="panel">
                <div class="panel__header">
                  <h2>最新预测记录</h2>
                  <span class="panel__hint">按预测时间倒序展示最近 5 条</span>
                </div>
                <div class="table-wrapper" v-if="state.overview.summary?.latestPredictions?.length">
                  <table>
                    <thead>
                      <tr>
                        <th>软件名称</th>
                        <th>预测日期</th>
                        <th>年度成本</th>
                        <th>活跃用户</th>
                        <th>置信度</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="item in state.overview.summary.latestPredictions"
                        :key="item.softwareName + item.predictionDate"
                      >
                        <td>
                          <span class="name">{{ item.softwareName }}</span>
                          <span class="muted">{{ item.categoryName }}</span>
                        </td>
                        <td>{{ formatDate(item.predictionDate) }}</td>
                        <td>{{ formatCurrency(item.predictedAnnualCost) }}</td>
                        <td>{{ item.predictedActiveUsers ?? '-' }}</td>
                        <td>{{ item.confidence != null ? `${Math.round(item.confidence * 100)}%` : '-' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <p v-else class="empty-hint">暂无预测记录</p>
              </section>
            </div>
          </template>
        </template>

        <template v-else-if="state.activeSection === 'assets'">
          <section class="panel">
            <div class="panel__header">
              <h2>资产分类概览</h2>
              <span class="panel__hint">按照软件类别统计数量与年度成本</span>
            </div>
            <div v-if="state.assets.categoryLoading" class="loading loading--sub">分类数据加载中...</div>
            <div v-else-if="state.assets.categoryError" class="error">{{ state.assets.categoryError }}</div>
            <div v-else-if="state.assets.categories.length" class="category-grid">
              <article
                v-for="category in state.assets.categories"
                :key="category.id ?? 'uncategorized'"
                class="category-card"
              >
                <h3>{{ category.name }}</h3>
                <p class="category-card__description">{{ category.description || '暂无描述' }}</p>
                <div class="category-card__stats">
                  <span>总计 {{ category.softwareCount }} 个</span>
                  <span>活跃 {{ category.activeSoftwareCount }} 个</span>
                  <span>已预测 {{ category.predictedSoftwareCount }} 个</span>
                </div>
                <p class="category-card__cost">年度成本 {{ formatCurrency(category.totalAnnualCost) }}</p>
              </article>
            </div>
            <p v-else class="empty-hint">暂无分类数据</p>
          </section>

          <section class="panel">
            <div class="panel__header">
              <h2>软件资产列表</h2>
              <span class="panel__hint">支持按类别、状态和关键字进行筛选</span>
            </div>
            <form class="filter-bar" @submit.prevent="applyAssetFilters">
              <input
                v-model="state.assets.filters.search"
                type="search"
                placeholder="搜索软件名称、供应商或授权类型"
              />
              <select v-model="state.assets.filters.categoryId">
                <option value="">全部类别</option>
                <option
                  v-for="category in state.assets.categories"
                  :key="`filter-${category.id}`"
                  v-if="category.id !== null"
                  :value="String(category.id)"
                >
                  {{ category.name }}
                </option>
              </select>
              <select v-model="state.assets.filters.status">
                <option value="">全部状态</option>
                <option value="ACTIVE">正常运行</option>
                <option value="MAINTENANCE">维护中</option>
                <option value="RETIRED">已退役</option>
              </select>
              <label class="filter-checkbox">
                <input type="checkbox" v-model="state.assets.filters.expiringSoon" @change="applyAssetFilters" />
                <span>关注 60 天内到期</span>
              </label>
              <div class="filter-actions">
                <button class="refresh-button" type="submit">应用筛选</button>
                <button class="ghost-button" type="button" @click="resetAssetFilters">重置</button>
              </div>
            </form>

            <div v-if="state.assets.loading" class="loading loading--sub">资产数据加载中...</div>
            <div v-else-if="state.assets.error" class="error">{{ state.assets.error }}</div>
            <div v-else-if="state.assets.items.length" class="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>软件名称</th>
                    <th>类别</th>
                    <th>状态</th>
                    <th>维护到期</th>
                    <th>剩余天数</th>
                    <th>供应商</th>
                    <th>授权类型</th>
                    <th>年度成本</th>
                    <th>预测覆盖</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in state.assets.items" :key="item.id">
                    <td>
                      <span class="name">{{ item.name }}</span>
                      <span class="muted">版本 {{ item.version || '未知' }}</span>
                    </td>
                    <td>{{ item.categoryName || '未分类' }}</td>
                    <td>
                      <span :class="['badge', 'badge--status', `badge--status-${(item.status || '').toLowerCase()}`]">
                        {{ formatStatus(item.status) }}
                      </span>
                    </td>
                    <td>{{ formatDate(item.maintenanceExpiryDate) }}</td>
                    <td>
                      <span :class="['days', item.expired ? 'danger' : item.expiringSoon ? 'warning' : '']">
                        {{ formatDays(item.daysUntilExpiry) }}
                      </span>
                    </td>
                    <td>{{ item.vendor || '-' }}</td>
                    <td>{{ item.licenseType || '-' }}</td>
                    <td>{{ formatCurrency(item.annualCost) }}</td>
                    <td>
                      <span :class="['badge', item.hasPrediction ? 'badge--success' : 'badge--warning']">
                        {{ item.hasPrediction ? '已纳入' : '待预测' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="empty-hint">暂无资产记录，尝试调整筛选条件</p>
          </section>
        </template>

        <template v-else>
          <section class="panel section-placeholder">
            <h2>模块建设中</h2>
            <p>我们正在规划更多关于 {{ currentSection.title }} 的能力，敬请期待。</p>
          </section>
        </template>
      </section>
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: var(--background);
  color: var(--text-primary);
}

.sidebar {
  background: #0a1f44;
  color: #f5f7fa;
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand__logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 22px;
  color: #ffffff;
}

.brand__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.brand__subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  opacity: 0.7;
}

.sidebar__menu {
  flex: 1;
}

.menu__title {
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
  opacity: 0.6;
  margin-bottom: 12px;
}

.sidebar__menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
  font-size: 14px;
}

.sidebar__menu li {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
  opacity: 0.8;
}

.sidebar__menu li:hover,
.sidebar__menu li.active {
  background: rgba(255, 255, 255, 0.12);
  opacity: 1;
}

.sidebar__footer {
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
}

.footer__title {
  font-size: 12px;
  text-transform: uppercase;
  opacity: 0.7;
  margin: 0 0 8px;
}

.footer__value {
  margin: 0;
  font-weight: 600;
}

.content {
  padding: 32px 40px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.topbar h1 {
  margin: 0;
  font-size: 26px;
}

.topbar__subtitle {
  margin: 4px 0 0;
  color: var(--muted-color);
  font-size: 14px;
}

.refresh-button {
  border: none;
  background: var(--accent-color);
  color: white;
  padding: 10px 16px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.refresh-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(51, 147, 255, 0.3);
}

.main-area {
  display: grid;
  gap: 24px;
}

.loading,
.error,
.empty-hint {
  padding: 20px;
  border-radius: 12px;
  background: white;
  text-align: center;
  color: var(--muted-color);
}

.error {
  color: #d9534f;
}

.stats-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(16, 37, 64, 0.08);
  display: grid;
  gap: 8px;
}

.stat-card__label {
  margin: 0;
  font-size: 14px;
  color: var(--muted-color);
}

.stat-card__value {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-card__hint {
  margin: 0;
  font-size: 12px;
  color: var(--muted-color);
}

.panel-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(16, 37, 64, 0.08);
  display: grid;
  gap: 20px;
}

.panel__header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.panel__header h2 {
  margin: 0;
  font-size: 18px;
}

.panel__hint {
  font-size: 13px;
  color: var(--muted-color);
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.table-wrapper table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.table-wrapper th,
.table-wrapper td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #eef2f8;
}

.table-wrapper th {
  color: var(--muted-color);
  font-weight: 500;
}

.table-wrapper td .name {
  display: block;
  font-weight: 600;
  color: var(--text-primary);
}

.table-wrapper td .muted {
  display: block;
  color: var(--muted-color);
  font-size: 12px;
  margin-top: 2px;
}

.days {
  font-weight: 600;
  color: var(--accent-color);
}

.days.danger {
  color: #d9534f;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.badge--success {
  background: rgba(61, 180, 144, 0.15);
  color: #1f8a70;
}

.badge--warning {
  background: rgba(255, 177, 66, 0.15);
  color: #be7c18;
}

.loading--sub {
  background: #f7f9fc;
  border: 1px dashed #d7deeb;
}

.category-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.category-card {
  background: #f7f9fc;
  border: 1px solid #e2e8f4;
  border-radius: 12px;
  padding: 16px;
  display: grid;
  gap: 10px;
}

.category-card h3 {
  margin: 0;
  font-size: 16px;
}

.category-card__description {
  margin: 0;
  font-size: 13px;
  color: var(--muted-color);
}

.category-card__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--muted-color);
}

.category-card__cost {
  margin: 0;
  font-weight: 600;
  color: var(--accent-color);
}

.filter-bar {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  align-items: center;
}

.filter-bar input,
.filter-bar select {
  border: 1px solid #d7deeb;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  background: #f9fbff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.filter-bar input:focus,
.filter-bar select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px rgba(51, 147, 255, 0.15);
}

.filter-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--muted-color);
}

.filter-checkbox input {
  width: 16px;
  height: 16px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.ghost-button {
  border: 1px solid var(--accent-color);
  background: transparent;
  color: var(--accent-color);
  padding: 10px 16px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.ghost-button:hover {
  background: rgba(51, 147, 255, 0.12);
  color: #1b5cb8;
}

.badge--status {
  background: rgba(107, 122, 153, 0.12);
  color: #53627a;
}

.badge--status-active {
  background: rgba(61, 180, 144, 0.15);
  color: #1f8a70;
}

.badge--status-maintenance {
  background: rgba(255, 177, 66, 0.15);
  color: #be7c18;
}

.badge--status-retired {
  background: rgba(214, 76, 76, 0.12);
  color: #b33a3a;
}

.days.warning {
  color: #be7c18;
}

.section-placeholder {
  text-align: center;
  gap: 12px;
}

.section-placeholder h2 {
  margin: 0;
  font-size: 20px;
}

.section-placeholder p {
  margin: 0;
  color: var(--muted-color);
}

:global(:root) {
  --background: #f3f6fb;
  --text-primary: #1c2b36;
  --muted-color: #6b7a99;
  --accent-color: #3393ff;
}

@media (max-width: 960px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding: 20px;
  }

  .sidebar__menu,
  .sidebar__footer {
    display: none;
  }
}
</style>
