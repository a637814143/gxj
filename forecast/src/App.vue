<script setup>
import { onMounted, reactive } from 'vue'
import TrendChart from './components/TrendChart.vue'

const state = reactive({
  loading: true,
  error: null,
  summary: null
})

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

const fetchSummary = async () => {
  state.loading = true
  state.error = null
  try {
    const response = await fetch('http://localhost:8080/api/dashboard/summary')
    if (!response.ok) {
      throw new Error('获取概览信息失败，请稍后重试')
    }
    state.summary = await response.json()
  } catch (error) {
    state.error = error.message || '数据加载异常'
  } finally {
    state.loading = false
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
          <li class="active">概览看板</li>
          <li>统计中心</li>
          <li>财务中心</li>
          <li>预测中心</li>
          <li>资产中心</li>
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
          <h1>概览数据</h1>
          <p class="topbar__subtitle">实时掌握软件资产运行与预测状况</p>
        </div>
        <button class="refresh-button" type="button" @click="fetchSummary">
          刷新数据
        </button>
      </header>
      <section class="main-area">
        <div v-if="state.loading" class="loading">数据加载中...</div>
        <div v-else-if="state.error" class="error">{{ state.error }}</div>
        <template v-else>
          <section class="stats-grid">
            <article class="stat-card">
              <p class="stat-card__label">软件数量</p>
              <p class="stat-card__value">{{ state.summary?.softwareCount ?? '-' }}</p>
              <p class="stat-card__hint">包含所有纳管软件资产</p>
            </article>
            <article class="stat-card">
              <p class="stat-card__label">已预测软件</p>
              <p class="stat-card__value">{{ state.summary?.predictedSoftwareCount ?? '-' }}</p>
              <p class="stat-card__hint">至少拥有一条预测记录的软件</p>
            </article>
            <article class="stat-card">
              <p class="stat-card__label">预测次数</p>
              <p class="stat-card__value">{{ state.summary?.predictionCount ?? '-' }}</p>
              <p class="stat-card__hint">近周期内系统生成的预测</p>
            </article>
            <article class="stat-card">
              <p class="stat-card__label">软件类别</p>
              <p class="stat-card__value">{{ state.summary?.categoryCount ?? '-' }}</p>
              <p class="stat-card__hint">当前纳管的分类维度</p>
            </article>
          </section>

          <div class="panel-grid">
            <section class="panel">
              <div class="panel__header">
                <h2>近期预测趋势</h2>
                <span class="panel__hint">展示最近六个月预测生成情况</span>
              </div>
              <TrendChart :points="state.summary?.recentPredictionTrend ?? []" />
            </section>

            <section class="panel">
              <div class="panel__header">
                <h2>即将到期的维护</h2>
                <span class="panel__hint">未来 120 天需要关注的资产</span>
              </div>
              <div class="table-wrapper" v-if="state.summary?.upcomingRenewals?.length">
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
                    <tr v-for="item in state.summary.upcomingRenewals" :key="item.softwareName">
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
              <div class="table-wrapper" v-if="state.summary?.latestPredictions?.length">
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
                    <tr v-for="item in state.summary.latestPredictions" :key="item.softwareName + item.predictionDate">
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
