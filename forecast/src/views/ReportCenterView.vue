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
      <div class="hero-actions">
        <div class="actions-header">
          <div class="actions-title">快捷导航</div>
          <p class="actions-desc">按照你的工作节奏快速跳转到常用模块，提升预测与报告协同效率。</p>
        </div>
        <div class="actions-grid">
          <button
            v-for="action in reportQuickActions"
            :key="action.key"
            type="button"
            class="action-card"
            :class="`accent-${action.accent}`"
            @click="handleAction(action)"
          >
            <span class="action-icon">{{ action.icon }}</span>
            <span class="action-content">
              <span class="action-label">{{ action.label }}</span>
              <span class="action-desc">{{ action.description }}</span>
            </span>
            <span class="action-arrow">→</span>
          </button>
        </div>
        <div class="actions-tip">
          <span class="tip-icon">💡</span>
          <span>善用快捷入口快速发起预测、查看仪表盘或管理个人资料。</span>
        </div>
      </div>
    </section>

    <el-card ref="reportListCard" class="report-card" shadow="hover" v-loading="loading">
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
          v-for="report in paginatedReports"
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
            <p v-if="report.insights" class="report-insight">洞察：{{ report.insights }}</p>
            <el-link type="primary" @click="viewReport(report)">查看详情</el-link>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <div
        v-if="reports.length > reportPagination.pageSize"
        class="timeline-pagination"
      >
        <el-pagination
          background
          layout="prev, pager, next"
          :total="reports.length"
          :page-size="reportPagination.pageSize"
          :current-page="reportPagination.currentPage"
          @current-change="handleReportPageChange"
        />
      </div>
      <template v-else />
    </el-card>

    <report-generate-dialog v-model="showGenerateDialog" @success="handleGenerateSuccess" />
    <report-detail-drawer
      v-model="showDetailDrawer"
      :report-id="activeReportId"
      :summary="activeReportSummary"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { fetchReportOverview } from '../services/report'
import { useAuthorization } from '../composables/useAuthorization'
import ReportGenerateDialog from '../components/report/ReportGenerateDialog.vue'
import ReportDetailDrawer from '../components/report/ReportDetailDrawer.vue'

const router = useRouter()

const reports = ref([])
const metrics = ref({ totalReports: 0, publishedThisMonth: 0, pendingApproval: 0, autoGenerationEnabled: false })
const loading = ref(false)
const showGenerateDialog = ref(false)
const showDetailDrawer = ref(false)
const activeReportId = ref(null)
const activeReportSummary = ref(null)
const reportListCard = ref(null)
const reportPagination = reactive({
  currentPage: 1,
  pageSize: 5,
})

const { hasRole, canAccessRoute } = useAuthorization()
const canGenerateReport = computed(() => hasRole(['ADMIN', 'AGRICULTURE_DEPT']))

const paginatedReports = computed(() => {
  if (!reports.value.length) {
    return []
  }
  const start = (reportPagination.currentPage - 1) * reportPagination.pageSize
  return reports.value.slice(start, start + reportPagination.pageSize)
})

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

const reportQuickActions = computed(() => {
  const baseActions = [
    {
      key: 'dashboard',
      name: 'dashboard',
      label: '仪表盘总览',
      description: '回到首页查看最新运行态势',
      accent: 'sunrise',
      icon: '📊',
      type: 'route'
    },
    {
      key: 'forecast',
      name: 'forecast',
      label: '预测中心',
      description: '发起或查看预测任务',
      accent: 'sunset',
      icon: '🚀',
      type: 'route'
    },
    {
      key: 'report-generate',
      label: '生成报告',
      description: '一键生成新的分析报告',
      accent: 'forest',
      icon: '📝',
      type: 'generate'
    },
    {
      key: 'report-history',
      label: '报告列表',
      description: '跳转到下方历史记录',
      accent: 'ocean',
      icon: '📚',
      type: 'scroll'
    },
    {
      key: 'profile',
      name: 'profile',
      label: '个人中心',
      description: '维护资料与安全信息',
      accent: 'violet',
      icon: '👤',
      type: 'route'
    }
  ]

  return baseActions.filter(action => {
    if (action.type === 'route') {
      return canAccessRoute(action.name)
    }
    if (action.type === 'generate') {
      return canGenerateReport.value
    }
    return true
  })
})

function formatDate(value) {
  if (!value) return '暂无'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN')
}

const handleAction = action => {
  if (!action) {
    return
  }
  if (action.type === 'generate') {
    createReport()
    return
  }
  if (action.type === 'scroll') {
    reportListCard.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  if (action.type === 'route' && action.name) {
    router.push({ name: action.name }).catch(() => {})
  }
}

const fetchReports = async () => {
  loading.value = true
  try {
    const { data } = await fetchReportOverview()
    const payload = data?.data ?? data ?? {}
    reports.value = Array.isArray(payload.reports) ? payload.reports : []
    if (reportPagination.currentPage < 1) {
      reportPagination.currentPage = 1
    }
    const totalPages = reports.value.length
      ? Math.ceil(reports.value.length / reportPagination.pageSize)
      : 1
    if (reportPagination.currentPage > totalPages) {
      reportPagination.currentPage = totalPages
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
  showGenerateDialog.value = true
}

const handleGenerateSuccess = detail => {
  reportPagination.currentPage = 1
  fetchReports().finally(() => {
    const summary = detail?.summary ?? null
    if (summary?.id) {
      activeReportSummary.value = summary
      activeReportId.value = summary.id
      showDetailDrawer.value = true
    }
  })
}

const handleReportPageChange = page => {
  reportPagination.currentPage = page
}

const viewReport = report => {
  if (!report?.id) {
    ElMessage.warning('无法打开报告详情')
    return
  }
  activeReportSummary.value = report
  activeReportId.value = report.id
  showDetailDrawer.value = true
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
  grid-template-columns: minmax(320px, 1.4fr) minmax(260px, 1fr);
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

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.actions-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.actions-title {
  font-size: 18px;
  font-weight: 700;
  color: #1346af;
}

.actions-desc {
  margin: 0;
  font-size: 13px;
  color: #3c4f79;
  line-height: 1.6;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  border: none;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  color: #fff;
  font: inherit;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-align: left;
}

.action-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), transparent 60%);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 35px rgba(34, 98, 255, 0.2);
}

.action-card:hover::after {
  opacity: 1;
}

.action-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.action-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-label {
  font-size: 15px;
  font-weight: 600;
}

.action-desc {
  font-size: 12px;
  opacity: 0.9;
}

.action-arrow {
  margin-left: auto;
  font-size: 18px;
  opacity: 0.85;
}

.actions-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(34, 98, 255, 0.08);
  color: #345296;
  font-size: 12px;
}

.tip-icon {
  font-size: 16px;
}

.action-card.accent-sunrise {
  background: linear-gradient(135deg, #facc15 0%, #fb7185 100%);
  box-shadow: 0 12px 28px rgba(251, 113, 133, 0.26);
}

.action-card.accent-sunset {
  background: linear-gradient(135deg, #f97316 0%, #f43f5e 100%);
  box-shadow: 0 12px 28px rgba(244, 63, 94, 0.26);
}

.action-card.accent-forest {
  background: linear-gradient(135deg, #34d399 0%, #22d3ee 100%);
  box-shadow: 0 12px 28px rgba(45, 212, 191, 0.26);
}

.action-card.accent-ocean {
  background: linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%);
  box-shadow: 0 12px 28px rgba(14, 165, 233, 0.26);
}

.action-card.accent-violet {
  background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%);
  box-shadow: 0 12px 28px rgba(139, 92, 246, 0.26);
}

.report-card {
  border-radius: 20px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.card-subtitle {
  font-size: 13px;
  color: #6f7f9f;
}

.report-timeline {
  padding: 12px 0 24px 0;
}

.timeline-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 0 0 12px;
}

.report-item {
  border-radius: 14px;
  border: 1px solid #eef2ff;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(244, 249, 255, 0.92) 100%);
}

.report-item h3 {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
  color: #1a2858;
}

.report-item .desc {
  margin: 0 0 12px;
  font-size: 14px;
  color: #4a5a78;
}

.report-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #6b7a99;
}

.report-insight {
  margin: 0 0 12px;
  font-size: 13px;
  color: #3b4a6b;
}

@media (max-width: 768px) {
  .hero-card {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-desc {
    font-size: 13px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .actions-grid {
    grid-template-columns: 1fr;
  }
}
</style>
