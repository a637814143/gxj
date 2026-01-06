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
    </section>

    <el-card ref="reportListCard" class="report-card" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">分析报告列表</div>
            <div class="card-subtitle">追踪各业务主题的报告生成时间与核心摘要</div>
          </div>
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
            <div class="report-actions">
              <el-link type="primary" @click="viewReport(report)">查看详情</el-link>
              <el-popconfirm
                v-if="canDeleteReport"
                title="确定要删除这份报告吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDeleteReport(report.id)"
              >
                <template #reference>
                  <el-link type="danger" :loading="deletingReportId === report.id">删除</el-link>
                </template>
              </el-popconfirm>
            </div>
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

    <report-detail-drawer
      v-model="showDetailDrawer"
      :report-id="activeReportId"
      :summary="activeReportSummary"
      :can-delete="canDeleteReport"
      @delete="handleDeleteFromDrawer"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { fetchReportOverview, deleteReport } from '../services/report'
import { useAuthStore } from '../stores/auth'
import ReportDetailDrawer from '../components/report/ReportDetailDrawer.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const reports = ref([])
const metrics = ref({ totalReports: 0, publishedThisMonth: 0, pendingApproval: 0, autoGenerationEnabled: false })
const loading = ref(false)
const showDetailDrawer = ref(false)
const activeReportId = ref(null)
const activeReportSummary = ref(null)
const reportListCard = ref(null)
const deletingReportId = ref(null)
const reportPagination = reactive({
  currentPage: 1,
  pageSize: 5,
})

// 检查用户是否有删除权限（ADMIN 或 AGRICULTURE_DEPT）
const canDeleteReport = computed(() => {
  const roles = authStore.user?.roles
  if (!roles) return false
  if (Array.isArray(roles)) {
    return roles.includes('ADMIN') || roles.includes('AGRICULTURE_DEPT')
  }
  return roles === 'ADMIN' || roles === 'AGRICULTURE_DEPT'
})

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

const handleDeleteReport = async (reportId) => {
  if (!reportId) return
  
  deletingReportId.value = reportId
  try {
    await deleteReport(reportId)
    ElMessage.success('报告删除成功')
    // 刷新报告列表
    await fetchReports()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除报告失败')
  } finally {
    deletingReportId.value = null
  }
}

const handleDeleteFromDrawer = async (reportId) => {
  showDetailDrawer.value = false
  await handleDeleteReport(reportId)
}

onMounted(() => {
  fetchReports()
})

const clearRouteQueryKeys = keys => {
  const normalized = Array.isArray(keys) ? keys : [keys]
  const currentQuery = { ...route.query }
  let mutated = false
  normalized.forEach(key => {
    if (key in currentQuery) {
      delete currentQuery[key]
      mutated = true
    }
  })
  if (mutated) {
    router.replace({ query: currentQuery }).catch(() => {})
  }
}

const scrollToReportList = () => {
  nextTick(() => {
    const target = reportListCard.value?.$el || reportListCard.value
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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

watch(
  () => route.query.reportId,
  async reportId => {
    if (reportId) {
      const id = Number(reportId)
      if (!Number.isNaN(id)) {
        // 先刷新报告列表，确保新生成的报告在列表中
        await fetchReports()
        // 从列表中查找对应的报告摘要
        const report = reports.value.find(r => r.id === id)
        if (report) {
          activeReportSummary.value = report
        }
        activeReportId.value = id
        showDetailDrawer.value = true
      }
      router.replace({ query: {} }).catch(() => {})
    }
  },
  { immediate: true }
)

watch(
  () => route.query.focus,
  focus => {
    if (focus === 'list') {
      scrollToReportList()
      clearRouteQueryKeys('focus')
    }
  },
  { immediate: true }
)
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
  grid-template-columns: 1fr;
  gap: 24px;
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

.report-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

@media (max-width: 768px) {
  .hero-card {
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
