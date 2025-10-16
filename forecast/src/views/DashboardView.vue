<template>
  <div class="dashboard-page">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">数据管理中心驾驶舱</h1>
        <p class="hero-desc">
          聚焦重点区域与核心作物的生产动态，实时掌握数据导入、模型运行与风险预警情况，让管理决策更加高效可靠。
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
          <div class="side-card-title">今日运行速览</div>
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
            <div class="side-footer-title">重点提醒</div>
            <ul>
              <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
            </ul>
          </div>
        </div>
        <div class="hero-decor" />
      </div>
    </section>

    <el-card class="filter-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">查询条件</div>
            <div class="card-subtitle">筛选关键维度，快速定位关注的数据批次</div>
          </div>
          <div class="card-actions">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" :loading="searching" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>
      <el-form :model="filterForm" label-position="top" class="filter-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="作物范围">
              <el-select v-model="filterForm.crop" placeholder="请选择">
                <el-option v-for="item in cropOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="行政区域">
              <el-select v-model="filterForm.region" placeholder="请选择">
                <el-option v-for="item in regionOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="年度批次">
              <el-select v-model="filterForm.year" placeholder="请选择">
                <el-option v-for="item in yearOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="生产周期">
              <el-select v-model="filterForm.cycle" placeholder="请选择">
                <el-option v-for="item in cycleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">历史预测数据</div>
            <div class="card-subtitle">结合最新导入的基础数据与模型输出，追踪各地区预测表现</div>
          </div>
          <div class="card-actions">
            <el-button>导出 Excel</el-button>
            <el-button>导出 PDF</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" :border="false" :stripe="true" :header-cell-style="tableHeaderStyle" :cell-style="tableCellStyle">
        <el-table-column prop="batch" label="批次" width="110" />
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="region" label="地区" min-width="160" />
        <el-table-column prop="crop" label="作物" min-width="140" />
        <el-table-column prop="acreage" label="种植面积 (公顷)" min-width="160">
          <template #default="{ row }">{{ formatNumber(row.acreage) }}</template>
        </el-table-column>
        <el-table-column prop="yield" label="预测产量 (吨)" min-width="150">
          <template #default="{ row }">{{ formatNumber(row.yield) }}</template>
        </el-table-column>
        <el-table-column prop="accuracy" label="模型准确率" width="140">
          <template #default="{ row }">{{ row.accuracy }}%</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status]">{{ statusLabelMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="负责人" width="120" />
      </el-table>
      <div class="table-footer">
        <div class="table-tip">共 {{ tableData.length }} 条记录，更多数据请使用筛选条件查询</div>
        <el-pagination layout="prev, pager, next" :total="120" :page-size="10" small background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'

const highlightStats = [
  { label: '接入地区', value: '48 个地市', sub: '本月新增 4 个重点区域' },
  { label: '监测作物', value: '26 种', sub: '覆盖粮食、经济与特色作物' },
  { label: '数据文件', value: '312 份', sub: '含历史与实时监测数据' },
  { label: '模型任务', value: '18 次', sub: '近 30 天内完成的预测任务' }
]

const quickOverview = [
  { label: '今日导入数据', value: '12 份', trend: 3 },
  { label: '模型运行', value: '5 次', trend: 1 },
  { label: '预警提示', value: '1 项', trend: -1 }
]

const reminders = [
  '西南片区玉米产量预测需人工复核',
  '请关注华北冬小麦数据采集进度',
  '新模型版本验证通过，可安排上线'
]

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  return Array.from({ length: 6 }, (_, index) => currentYear - index)
})

const cropOptions = [
  { label: '全部作物', value: 'ALL' },
  { label: '水稻', value: 'RICE' },
  { label: '小麦', value: 'WHEAT' },
  { label: '玉米', value: 'CORN' },
  { label: '大豆', value: 'SOYBEAN' }
]

const regionOptions = [
  { label: '全部区域', value: 'ALL' },
  { label: '东北产粮区', value: 'NE' },
  { label: '华北平原', value: 'HB' },
  { label: '长江中下游', value: 'YRD' },
  { label: '西南丘陵区', value: 'SW' }
]

const cycleOptions = [
  { label: '全年', value: 'ALL' },
  { label: '春季', value: 'SPRING' },
  { label: '夏季', value: 'SUMMER' },
  { label: '秋季', value: 'AUTUMN' },
  { label: '冬季', value: 'WINTER' }
]

const filterForm = reactive({
  crop: cropOptions[0].value,
  region: regionOptions[0].value,
  year: yearOptions.value[0],
  cycle: cycleOptions[0].value
})

const tableData = [
  {
    batch: '2024-Q1-01',
    date: '2024-03-28',
    region: '东北产粮区 / 黑龙江',
    crop: '玉米',
    acreage: 12840,
    yield: 74200,
    accuracy: 92.4,
    status: 'FINISHED',
    operator: '王志强'
  },
  {
    batch: '2024-Q1-02',
    date: '2024-03-25',
    region: '华北平原 / 河北',
    crop: '冬小麦',
    acreage: 10320,
    yield: 56120,
    accuracy: 90.1,
    status: 'FINISHED',
    operator: '李晓梅'
  },
  {
    batch: '2024-Q1-03',
    date: '2024-03-22',
    region: '长江中下游 / 湖南',
    crop: '早稻',
    acreage: 8720,
    yield: 43860,
    accuracy: 88.6,
    status: 'RUNNING',
    operator: '陈建军'
  },
  {
    batch: '2024-Q1-04',
    date: '2024-03-18',
    region: '西南丘陵区 / 四川',
    crop: '油菜',
    acreage: 6920,
    yield: 25840,
    accuracy: 84.3,
    status: 'WARNING',
    operator: '杨慧敏'
  },
  {
    batch: '2024-Q1-05',
    date: '2024-03-14',
    region: '东北产粮区 / 吉林',
    crop: '大豆',
    acreage: 7860,
    yield: 31670,
    accuracy: 89.7,
    status: 'FINISHED',
    operator: '赵立新'
  }
]

const statusLabelMap = {
  FINISHED: '已完成',
  RUNNING: '运行中',
  WARNING: '需关注'
}

const statusTypeMap = {
  FINISHED: 'success',
  RUNNING: 'info',
  WARNING: 'warning'
}

const searching = computed(() => false)

const handleSearch = () => {
  // 预留查询逻辑
}

const handleReset = () => {
  filterForm.crop = cropOptions[0].value
  filterForm.region = regionOptions[0].value
  filterForm.year = yearOptions.value[0]
  filterForm.cycle = cycleOptions[0].value
}

const formatNumber = value => {
  if (value === null || value === undefined) return '-'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 0 })
}

const formatTrend = value => {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

const tableCellStyle = () => ({
  color: '#1f2d3d',
  fontSize: '13px'
})
</script>

<style scoped>
.dashboard-page {
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

.filter-card,
.table-card {
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

.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-form {
  padding-top: 8px;
}

.table-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #5f6f8c;
  font-size: 12px;
}

.table-tip {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 992px) {
  .hero-card {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .card-actions {
    justify-content: flex-start;
  }
}
</style>
