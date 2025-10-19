<template>
  <div class="report-page">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">分析报告中心</h1>
        <p class="hero-desc">
          汇总预测运行、数据导入与风险预警信息，生成可视化分析报告，支持导出分享与跨部门协同。
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
          <div class="side-card-title">生成规划</div>
          <div class="side-items">
            <div v-for="item in quickOverview" :key="item.label" class="side-item">
              <div class="side-item-label">{{ item.label }}</div>
              <div class="side-item-value">{{ item.value }}</div>
              <div class="side-item-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
                {{ formatTrend(item.trend) }}
              </div>
            </div>
          </div>
          <div class="side-divider" />
          <div class="side-footer">
            <div class="side-footer-title">制作建议</div>
            <ul>
              <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
            </ul>
          </div>
        </div>
        <div class="hero-decor" />
      </div>
    </section>

    <el-card class="report-card" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">分析报告列表</div>
            <div class="card-subtitle">追踪各业务主题的报告生成时间与核心摘要</div>
          </div>
          <el-button v-if="canGenerateReport" type="primary" @click="createReport">生成报告</el-button>
        </div>
      </template>
      <el-empty v-if="!loading && !reports.length" description="暂未生成报告" />
      <el-timeline v-else-if="reports.length" class="report-timeline">
        <el-timeline-item
          v-for="report in reports"
          :key="report.id"
          :timestamp="formatDate(report.publishedAt)"
          type="success"
        >
          <el-card shadow="never" class="report-item">
            <h3>{{ report.title }}</h3>
            <p class="desc">{{ report.description }}</p>
            <div class="report-meta">
              <span>作者：{{ report.author }}</span>
              <span>覆盖周期：{{ report.coveragePeriod || '未填写' }}</span>
            </div>
            <el-link type="primary">查看详情</el-link>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <template v-else />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '../services/http'
import { useAuthorization } from '../composables/useAuthorization'

const reports = ref([])
const metrics = ref({ totalReports: 0, publishedThisMonth: 0, pendingApproval: 0, autoGenerationEnabled: false })
const loading = ref(false)

const { hasRole } = useAuthorization()
const canGenerateReport = computed(() => hasRole(['ADMIN', 'AGRICULTURE_DEPT']))

const highlightStats = computed(() => {
  const total = metrics.value.totalReports || reports.value.length
  const latest = reports.value[0]
  return [
    {
      label: '累计报告',
      value: `${total} 份`,
      sub: total ? '覆盖重点作物与地区专题' : '等待生成首份报告'
    },
    {
      label: '最新报告时间',
      value: latest ? formatDate(latest.publishedAt) : '暂无',
      sub: latest ? latest.title : '生成后可快速下载'
    },
    {
      label: '自动生成',
      value: metrics.value.autoGenerationEnabled ? '已启用' : '未启用',
      sub: metrics.value.autoGenerationEnabled ? '系统将按计划自动生成定期报告' : '可在系统设置启用自动生成'
    }
  ]
})

const quickOverview = computed(() => [
  { label: '本月发布', value: `${metrics.value.publishedThisMonth ?? 0} 份`, trend: metrics.value.publishedThisMonth > 0 ? 1 : 0 },
  { label: '待审核', value: `${metrics.value.pendingApproval ?? 0} 份`, trend: metrics.value.pendingApproval > 0 ? 1 : 0 },
  { label: '自动生成', value: metrics.value.autoGenerationEnabled ? '启用' : '未启用', trend: 0 }
])

const reminders = computed(() => {
  const items = []
  if (metrics.value.pendingApproval > 0) {
    items.push(`存在 ${metrics.value.pendingApproval} 份待审核报告，请尽快处理以便发布`) 
  } else {
    items.push('暂无待审核报告，可根据计划安排新的分析任务')
  }

  items.push(
    metrics.value.autoGenerationEnabled
      ? '自动生成已启用，建议定期检查模板确保输出格式一致'
      : '自动生成未启用，可在系统设置中开启以节省手工操作'
  )

  const latest = reports.value[0]
  if (latest) {
    items.push(`最新报告覆盖周期 ${latest.coveragePeriod || '近期数据'}，请及时共享给相关团队`)
  } else {
    items.push('尚未生成报告，请先运行预测任务并生成首份报告')
  }

  return items
})

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

function formatDate(value) {
  if (!value) return '暂无'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN')
}

const fetchReports = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get('/api/report')
    const payload = data?.data ?? data ?? {}
    if (Array.isArray(payload.reports)) {
      reports.value = payload.reports
    } else {
      reports.value = []
    }
    metrics.value = {
      totalReports: payload.metrics?.totalReports ?? reports.value.length,
      publishedThisMonth: payload.metrics?.publishedThisMonth ?? 0,
      pendingApproval: payload.metrics?.pendingApproval ?? 0,
      autoGenerationEnabled: Boolean(payload.metrics?.autoGenerationEnabled)
    }
  } catch (error) {
    reports.value = []
    metrics.value = { totalReports: 0, publishedThisMonth: 0, pendingApproval: 0, autoGenerationEnabled: false }
    ElMessage.error(error?.response?.data?.message || '加载报告数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchReports()
})

const createReport = () => {
  if (!canGenerateReport.value) {
    ElMessage.warning('当前账号没有生成报告的权限')
    return
  }
  ElMessage.info('后续将接入报告生成流程')
}
</script>

<style scoped>
.report-page {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(2px);
}

.side-item-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.side-item-value {
  font-size: 18px;
  font-weight: 700;
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

.report-card {
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

.report-timeline {
  margin-top: 12px;
}

.report-item h3 {
  margin: 0;
  font-size: 18px;
  color: #1a2a4a;
}

.report-item .desc {
  color: #6c7d9c;
  margin: 12px 0;
}

.report-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

@media (max-width: 992px) {
  .hero-card {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header > :last-child {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .card-header > :last-child {
    justify-content: flex-start;
  }
}
</style>
