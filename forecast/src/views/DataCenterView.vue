<template>
  <div class="data-center-page">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">数据资产与导入中心</h1>
        <p class="hero-desc">
          集中管理各类基础与业务数据资产，实时掌握数据导入进度与质量预警，保障模型训练与分析任务稳定运行。
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
          <div class="side-card-title">导入协同面板</div>
          <div class="side-items">
            <div v-for="item in quickOverview" :key="item.label" class="side-item">
              <div class="side-item-label">{{ item.label }}</div>
              <div class="side-item-content">
                <div class="side-item-value">{{ item.value }}</div>
                <div v-if="item.sub" class="side-item-sub">{{ item.sub }}</div>
              </div>
              <div class="side-item-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
                {{ formatTrend(item.trend) }}
              </div>
            </div>
          </div>
          <div class="side-divider" />
          <div class="side-footer">
            <div class="side-footer-title">操作建议</div>
            <ul>
              <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
            </ul>
          </div>
        </div>
        <div class="hero-decor" />
      </div>
    </section>

    <el-card class="data-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">数据集管理</div>
            <div class="card-subtitle">查看已导入的 Excel 数据文件并维护基础信息</div>
          </div>
          <div class="card-actions">
            <el-button @click="fetchDatasets" :loading="datasetLoading">刷新</el-button>
            <el-button type="primary" @click="openUpload">上传数据</el-button>
          </div>
        </div>
      </template>
      <el-table :data="datasets" v-loading="datasetLoading" empty-text="暂无导入记录" :header-cell-style="tableHeaderStyle">
        <el-table-column prop="name" label="数据集名称" min-width="220" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            {{ datasetTypeMap[row.type] ?? row.type ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="200">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
      </el-table>
      <div class="table-footer">
        <div class="table-tip">共 {{ datasets.length }} 个数据集，建议定期核验文件完整性</div>
        <el-button type="primary" link @click="openUpload">立即导入</el-button>
      </div>
    </el-card>

    <el-card v-if="lastImportStats" class="data-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">最新导入预览</div>
            <div class="card-subtitle">
              展示已清洗入库的前 {{ importPreview.length || 0 }} 条记录，并提供自动校验摘要
            </div>
          </div>
        </div>
      </template>
      <div class="import-summary">
        <div v-for="item in importSummaryBadges" :key="item.label" class="summary-item">
          <div class="summary-label">{{ item.label }}</div>
          <div class="summary-value">{{ item.value }}</div>
        </div>
      </div>
      <el-table
        v-if="importPreview.length"
        :data="importPreview"
        :header-cell-style="tableHeaderStyle"
        empty-text="本次导入暂无可预览记录"
      >
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="regionName" label="地区" min-width="160" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="production" label="总产量 (吨)" width="150">
          <template #default="{ row }">
            {{ formatNumber(row.production) }}
          </template>
        </el-table-column>
        <el-table-column prop="yieldPerHectare" label="单产 (吨/公顷)" width="170">
          <template #default="{ row }">
            {{ formatNumber(row.yieldPerHectare) }}
          </template>
        </el-table-column>
        <el-table-column prop="averagePrice" label="平均价格 (元/公斤)" width="190">
          <template #default="{ row }">
            {{ formatNumber(row.averagePrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="collectedAt" label="采集日期" width="180">
          <template #default="{ row }">
            {{ formatDate(row.collectedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="dataSource" label="数据来源" min-width="180" show-overflow-tooltip />
      </el-table>
      <div v-else class="import-preview-empty">本次导入数据已完成清洗并入库，未发现可展示的预览记录。</div>
    </el-card>

    <el-card class="data-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">导入任务记录</div>
            <div class="card-subtitle">追踪历史导入批次、覆盖地区与产量指标</div>
          </div>
          <el-button @click="fetchYieldRecords" :loading="yieldLoading">刷新</el-button>
        </div>
      </template>
      <el-table :data="yieldRecords" v-loading="yieldLoading" empty-text="暂未导入数据" :header-cell-style="tableHeaderStyle">
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="regionName" label="地区" min-width="160" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="production" label="总产量 (吨)" width="150">
          <template #default="{ row }">
            {{ formatNumber(row.production) }}
          </template>
        </el-table-column>
        <el-table-column prop="yieldPerHectare" label="单产 (吨/公顷)" width="170">
          <template #default="{ row }">
            {{ formatNumber(row.yieldPerHectare) }}
          </template>
        </el-table-column>
        <el-table-column prop="averagePrice" label="平均价格 (元/公斤)" width="190">
          <template #default="{ row }">
            {{ formatNumber(row.averagePrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="dataSource" label="数据来源" min-width="180" show-overflow-tooltip />
      </el-table>
      <div v-if="importWarnings.length" class="warnings">
        <el-alert
          v-for="(warning, index) in importWarnings"
          :key="`${index}-${warning}`"
          type="warning"
          :title="warning"
          show-icon
          :closable="false"
        />
      </div>
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="导入数据文件" width="520px" @closed="resetUploadForm">
      <el-form label-width="88px" class="upload-form">
        <el-form-item label="数据类型">
          <el-select v-model="uploadForm.type">
            <el-option v-for="item in datasetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集名称">
          <el-input v-model="uploadForm.name" placeholder="默认使用文件名" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            placeholder="可选，最长 256 字"
            :rows="3"
            maxlength="256"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            class="upload-block"
            drag
            :auto-upload="false"
            :file-list="uploadFileList"
            accept=".xls,.xlsx"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">仅支持 Excel 文件（.xls/.xlsx）</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false" :disabled="uploading">取 消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading" :disabled="uploadFileList.length === 0">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import apiClient from '../services/http'

const datasets = ref([])
const datasetLoading = ref(false)
const yieldRecords = ref([])
const yieldLoading = ref(false)
const importWarnings = ref([])
const importPreview = ref([])
const lastImportStats = ref(null)

const datasetTypeMap = {
  YIELD: '产量',
  PRICE: '价格',
  WEATHER: '气象',
  SOIL: '土壤'
}

const datasetTypeOptions = [
  { value: 'YIELD', label: '产量' },
  { value: 'PRICE', label: '价格' },
  { value: 'WEATHER', label: '气象' },
  { value: 'SOIL', label: '土壤' }
]

const uploadDialogVisible = ref(false)
const uploadFileList = ref([])
const uploading = ref(false)
const uploadForm = reactive({
  type: 'YIELD',
  name: '',
  description: ''
})

const tableHeaderStyle = () => ({
  background: '#f4f7fb',
  color: '#3c4b66',
  fontWeight: 600,
  fontSize: '13px'
})

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatNumber(value) {
  if (value === null || value === undefined) {
    return '-'
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return value
  }
  return number.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

const highlightStats = computed(() => {
  const total = datasets.value.length
  const typeCount = total
    ? new Set(datasets.value.map(item => datasetTypeMap[item.type] ?? item.type ?? '其他')).size
    : 0
  const latest = datasets.value
    .slice()
    .sort((a, b) => new Date(b?.updatedAt ?? 0).getTime() - new Date(a?.updatedAt ?? 0).getTime())[0]

  const toRecordTime = record => {
    const raw = record?.updatedAt ?? (record?.year ? `${record.year}` : null)
    if (!raw) return 0
    const parsed = new Date(raw)
    return Number.isNaN(parsed.getTime()) ? Number(record?.year ?? 0) : parsed.getTime()
  }

  const recentImport = yieldRecords.value
    .slice()
    .sort((a, b) => toRecordTime(b) - toRecordTime(a))[0]

  return [
    {
      label: '已导入数据集',
      value: `${total} 个`,
      sub: typeCount ? `覆盖 ${typeCount} 类业务主题` : '等待导入基础数据'
    },
    {
      label: '最近更新',
      value: latest ? formatDate(latest.updatedAt) : '暂无记录',
      sub: latest ? `文件：${latest.name}` : '点击右侧按钮上传'
    },
    {
      label: '历史导入批次',
      value: `${yieldRecords.value.length} 次`,
      sub: recentImport
        ? `最新批次：${recentImport.year ?? '未知'} · ${recentImport.regionName ?? '-'}`
        : '尚未同步导入记录'
    }
  ]
})

const quickOverview = computed(() => {
  const stats = lastImportStats.value
  const inserted = stats?.inserted ?? 0
  const updated = stats?.updated ?? 0
  const processed = inserted + updated
  return [
    {
      label: '待处理预警',
      value: `${importWarnings.value.length} 项`,
      trend: importWarnings.value.length > 0 ? 1 : 0
    },
    {
      label: '今日上传',
      value: uploadFileList.value.length ? `${uploadFileList.value.length} 份` : '0 份',
      trend: uploadFileList.value.length ? 1 : 0
    },
    {
      label: '本次入库',
      value: processed ? `${processed} 条` : '0 条',
      trend: processed ? 1 : 0,
      sub: processed ? `新增 ${inserted} · 更新 ${updated}` : '待导入数据'
    }
  ]
})

const importSummaryBadges = computed(() => {
  if (!lastImportStats.value) {
    return []
  }
  const { total = 0, inserted = 0, updated = 0, skipped = 0 } = lastImportStats.value
  return [
    { label: '识别总行数', value: `${total} 行` },
    { label: '新增入库', value: `${inserted} 条` },
    { label: '更新覆盖', value: `${updated} 条` },
    { label: '自动跳过', value: `${skipped} 条` }
  ]
})

const reminders = [
  '建议每周核对一次数据来源与口径',
  '导入前请确保文件格式符合模板要求',
  '完成导入后及时通知模型团队更新训练集'
]

const fetchDatasets = async () => {
  datasetLoading.value = true
  try {
    const { data } = await apiClient.get('/api/datasets/files')
    datasets.value = data?.data ?? []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载数据集列表失败')
  } finally {
    datasetLoading.value = false
  }
}

const fetchYieldRecords = async () => {
  yieldLoading.value = true
  try {
    const { data } = await apiClient.get('/api/yields')
    yieldRecords.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载导入数据失败')
  } finally {
    yieldLoading.value = false
  }
}

const beforeUpload = file => {
  const lowerName = file.name?.toLowerCase?.() ?? ''
  const isExcel = lowerName.endsWith('.xls') || lowerName.endsWith('.xlsx') || (file.type ?? '').includes('sheet')
  if (!isExcel) {
    ElMessage.error('仅支持 Excel 文件（.xls/.xlsx）')
  }
  return isExcel
}

const handleFileChange = (file, fileList) => {
  const target = file.raw ?? file
  if (!beforeUpload(target)) {
    uploadFileList.value = []
    return
  }
  uploadFileList.value = fileList.slice(-1)
}

const handleFileRemove = file => {
  uploadFileList.value = uploadFileList.value.filter(item => item.uid !== file.uid)
}

const submitUpload = async () => {
  if (!uploadFileList.value.length) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  const [fileItem] = uploadFileList.value
  const formData = new FormData()
  formData.append('file', fileItem.raw)
  formData.append('type', uploadForm.type)
  if (uploadForm.name.trim()) {
    formData.append('name', uploadForm.name.trim())
  }
  if (uploadForm.description.trim()) {
    formData.append('description', uploadForm.description.trim())
  }

  uploading.value = true
  try {
    const { data } = await apiClient.post('/api/data-import/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const result = data?.data
    importWarnings.value = result?.warnings ?? []
    lastImportStats.value = {
      total: Number(result?.totalRows ?? 0),
      inserted: Number(result?.insertedRows ?? 0),
      updated: Number(result?.updatedRows ?? 0),
      skipped: Number(result?.skippedRows ?? 0)
    }
    importPreview.value = Array.isArray(result?.preview) ? result.preview : []
    ElMessage.success(`导入完成，新增 ${result?.insertedRows ?? 0} 条，更新 ${result?.updatedRows ?? 0} 条`)
    uploadDialogVisible.value = false
    await Promise.all([fetchDatasets(), fetchYieldRecords()])
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '导入失败'
    ElMessage.error(message)
  } finally {
    uploading.value = false
  }
}

const openUpload = () => {
  uploadDialogVisible.value = true
  importWarnings.value = []
}

const resetUploadForm = () => {
  uploadForm.type = 'YIELD'
  uploadForm.name = ''
  uploadForm.description = ''
  uploadFileList.value = []
}

onMounted(async () => {
  await Promise.all([fetchDatasets(), fetchYieldRecords()])
})
</script>

<style scoped>
.data-center-page {
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
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(2px);
}

.side-item-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.side-item-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.side-item-value {
  font-size: 18px;
  font-weight: 700;
}

.side-item-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
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

.data-card {
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

.import-summary {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(34, 98, 255, 0.08);
  border: 1px solid rgba(34, 98, 255, 0.12);
}

.summary-label {
  font-size: 12px;
  color: #4b5d82;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #1841a1;
}

.import-preview-empty {
  padding: 20px;
  border-radius: 14px;
  background: rgba(245, 247, 252, 0.9);
  color: #5a6c8f;
  text-align: center;
  font-size: 13px;
}

.warnings {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.upload-form {
  padding-right: 8px;
}

.upload-block {
  width: 100%;
}

.upload-icon {
  font-size: 32px;
  color: var(--el-color-primary);
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
