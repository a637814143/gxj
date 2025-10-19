<template>
  <el-card class="ml-card" shadow="hover">
    <template #header>
      <div class="ml-card-header">
        <div>
          <div class="card-title">机器学习预测服务</div>
          <div class="card-subtitle">连接 Flask 模型，实时预测作物产量</div>
        </div>
        <div class="ml-status-group">
          <el-tag v-if="healthStatus" type="success" effect="plain">{{ healthStatus }}</el-tag>
          <el-button size="small" :loading="healthLoading" @click="fetchHealth">刷新状态</el-button>
        </div>
      </div>
    </template>

    <div class="ml-grid">
      <section class="ml-section">
        <div class="section-header">
          <h3>预测参数</h3>
          <p>填写基础信息后提交，服务会返回预测产量</p>
        </div>
        <el-alert
          v-if="healthError"
          type="error"
          :closable="false"
          class="section-alert"
          :title="healthError"
        />
        <el-alert
          v-else-if="disablePredict"
          type="info"
          :closable="false"
          class="section-alert"
          title="请先在上方完成产量预测配置的地区、作物与模型选择，系统会同步到此处。"
        />
        <el-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-width="108px"
          label-position="left"
          class="ml-form"
        >
          <el-form-item label="作物" prop="crop">
            <el-input v-model="form.crop" placeholder="例如：粮食" clearable />
          </el-form-item>
          <el-form-item label="地区" prop="region">
            <el-input v-model="form.region" placeholder="例如：云南省" clearable />
          </el-form-item>
          <el-form-item label="年份" prop="year">
            <el-input-number v-model="form.year" :min="2000" :max="2100" :step="1" />
          </el-form-item>
          <el-form-item label="播种面积" prop="sown_area_kha">
            <el-input-number
              v-model="form.sown_area_kha"
              :min="0"
              :precision="1"
              :step="0.1"
            />
            <span class="input-hint">千公顷</span>
          </el-form-item>
          <el-form-item label="平均价格" prop="avg_price_yuan_per_ton">
            <el-input-number v-model="form.avg_price_yuan_per_ton" :min="0" :step="1" />
            <span class="input-hint">元 / 吨</span>
          </el-form-item>
          <div class="form-actions">
            <el-button @click="resetForm">重置</el-button>
            <el-button
              type="primary"
              :disabled="disablePredict"
              :loading="predictionLoading"
              @click="submitPrediction"
            >
              发起预测
            </el-button>
          </div>
        </el-form>

        <el-alert
          v-if="predictionError"
          type="error"
          :closable="false"
          class="section-alert"
          :title="predictionError"
        />

        <div v-if="prediction" class="prediction-result">
          <div class="result-stats">
            <div class="result-block">
              <div class="result-label">预测产量 (万吨)</div>
              <div class="result-value">{{ formatNumber(prediction.predicted_yield_10kt, 2) }}</div>
            </div>
            <div class="result-block">
              <div class="result-label">预测产量 (吨)</div>
              <div class="result-value">{{ formatNumber(prediction.predicted_yield_tonnes, 0) }}</div>
            </div>
          </div>
          <el-descriptions border :column="2" title="请求参数回顾">
            <el-descriptions-item label="作物">{{ prediction.inputs.crop }}</el-descriptions-item>
            <el-descriptions-item label="地区">{{ prediction.inputs.region }}</el-descriptions-item>
            <el-descriptions-item label="年份">{{ prediction.inputs.year }}</el-descriptions-item>
            <el-descriptions-item label="播种面积(千公顷)">
              {{ formatNumber(prediction.inputs.sown_area_kha, 1) }}
            </el-descriptions-item>
            <el-descriptions-item label="平均价格(元/吨)">
              {{ formatNumber(prediction.inputs.avg_price_yuan_per_ton, 0) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </section>

      <section class="ml-section">
        <div class="section-header">
          <h3>训练数据预览</h3>
          <p>快速查看模型训练时使用的样本</p>
        </div>
        <div class="preview-actions">
          <div class="preview-control">
            <span>展示行数</span>
            <el-input-number
              v-model="previewLimit"
              :min="1"
              :max="50"
              :step="1"
              size="small"
            />
          </div>
          <el-button size="small" :loading="previewLoading" @click="loadPreview">刷新样本</el-button>
        </div>
        <el-skeleton v-if="previewLoading" animated :rows="6" />
        <template v-else>
          <el-table v-if="hasPreview" :data="preview.rows" border size="small" class="preview-table">
            <el-table-column
              v-for="column in preview.columns"
              :key="column"
              :prop="column"
              :label="columnLabels[column] || column"
              :min-width="getColumnWidth(column)"
              show-overflow-tooltip
            />
          </el-table>
          <el-empty v-else description="暂无训练数据，请确认机器学习服务已启动" />
          <el-alert
            v-if="previewError"
            class="section-alert"
            type="error"
            :closable="false"
            :title="previewError"
          />
        </template>
      </section>
    </div>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getHealthStatus, previewTrainingData, predictCropYield } from '@/services/machineLearning'

const props = defineProps({
  prefill: {
    type: Object,
    default: () => ({}),
  },
  disablePredict: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['prediction-success'])

const formRef = ref(null)
const prediction = ref(null)
const predictionError = ref('')
const predictionLoading = ref(false)
const health = ref(null)
const healthLoading = ref(false)
const healthError = ref('')
const preview = reactive({ columns: [], rows: [] })
const previewLimit = ref(5)
const previewLoading = ref(false)
const previewError = ref('')

const sanitizePrefill = value => {
  if (!value || typeof value !== 'object') return {}

  const toNumber = candidate => {
    const parsed = Number(candidate)
    return Number.isFinite(parsed) ? parsed : undefined
  }

  return {
    crop: typeof value.crop === 'string' && value.crop.trim() ? value.crop.trim() : undefined,
    region: typeof value.region === 'string' && value.region.trim() ? value.region.trim() : undefined,
    year: toNumber(value.year),
    sown_area_kha: toNumber(value.sown_area_kha),
    avg_price_yuan_per_ton: toNumber(value.avg_price_yuan_per_ton),
  }
}

const filterDefined = source =>
  Object.fromEntries(Object.entries(source).filter(([, item]) => item !== undefined))

const defaultForm = (overrides = {}) => ({
  crop: '粮食',
  region: '云南省',
  year: new Date().getFullYear(),
  sown_area_kha: 240,
  avg_price_yuan_per_ton: 2300,
  ...overrides,
})

const lastPrefill = ref(sanitizePrefill(props.prefill))
const form = reactive(defaultForm(filterDefined(lastPrefill.value)))

const formRules = {
  crop: [{ required: true, message: '请输入作物名称', trigger: 'blur' }],
  region: [{ required: true, message: '请输入地区名称', trigger: 'blur' }],
  year: [{ required: true, message: '请输入年份', trigger: 'change' }],
  sown_area_kha: [
    { required: true, message: '请输入播种面积', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (value == null || Number.isNaN(value)) {
          callback(new Error('播种面积需要是数值'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  avg_price_yuan_per_ton: [
    { required: true, message: '请输入平均价格', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (value == null || Number.isNaN(value)) {
          callback(new Error('平均价格需要是数值'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

const shouldResetPrediction = (next, prev) =>
  Boolean(
    (prev.crop && !next.crop) ||
      (prev.region && !next.region) ||
      (Number.isFinite(prev.year) && !Number.isFinite(next.year)) ||
      (next.crop && next.crop !== prev.crop) ||
      (next.region && next.region !== prev.region) ||
      (Number.isFinite(next.year) && next.year !== prev.year)
  )

const applyPrefill = (sanitized, previous = {}) => {
  if (sanitized.crop && (!form.crop || !previous.crop || form.crop === previous.crop)) {
    form.crop = sanitized.crop
  }
  if (sanitized.region && (!form.region || !previous.region || form.region === previous.region)) {
    form.region = sanitized.region
  }
  if (
    Number.isFinite(sanitized.year) &&
    (!Number.isFinite(form.year) || !Number.isFinite(previous.year) || form.year === previous.year)
  ) {
    form.year = sanitized.year
  }
  if (
    Number.isFinite(sanitized.sown_area_kha) &&
    (!Number.isFinite(form.sown_area_kha) || !Number.isFinite(previous.sown_area_kha) || form.sown_area_kha === previous.sown_area_kha)
  ) {
    form.sown_area_kha = sanitized.sown_area_kha
  }
  if (
    Number.isFinite(sanitized.avg_price_yuan_per_ton) &&
    (!Number.isFinite(form.avg_price_yuan_per_ton) ||
      !Number.isFinite(previous.avg_price_yuan_per_ton) ||
      form.avg_price_yuan_per_ton === previous.avg_price_yuan_per_ton)
  ) {
    form.avg_price_yuan_per_ton = sanitized.avg_price_yuan_per_ton
  }
}

watch(
  () => props.prefill,
  value => {
    const sanitized = sanitizePrefill(value)
    const previous = lastPrefill.value || {}

    if (shouldResetPrediction(sanitized, previous)) {
      prediction.value = null
      predictionError.value = ''
    }

    applyPrefill(sanitized, previous)
    lastPrefill.value = sanitized
  },
  { deep: true, immediate: true }
)

const columnLabels = {
  crop: '作物',
  crop_category: '作物类别',
  region: '地区',
  administrative_level: '行政级别',
  parent_region: '上级地区',
  year: '年份',
  sown_area_kha: '播种面积(千公顷)',
  yield_10kt: '产量(万吨)',
  yield_per_ha: '单产(吨/公顷)',
  avg_price_yuan_per_ton: '平均价格(元/吨)',
  data_source: '数据来源',
  collected_at: '采集日期',
}

const healthStatus = computed(() => {
  if (!health.value) return ''
  if (health.value.status !== 'ok') return '服务未就绪'
  return `${health.value.dataset} · ${health.value.records} 条样本`
})

const hasPreview = computed(() => Array.isArray(preview.rows) && preview.rows.length > 0)

const formatNumber = (value, fractionDigits = 2) => {
  if (value == null || Number.isNaN(Number(value))) return '-'
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(Number(value))
}

const getColumnWidth = column => {
  const wideColumns = ['avg_price_yuan_per_ton', 'sown_area_kha', 'yield_10kt', 'yield_per_ha']
  if (wideColumns.includes(column)) return 160
  if (column === 'crop' || column === 'region') return 140
  return 120
}

const extractErrorMessage = error => {
  if (error?.response?.data?.error) return error.response.data.error
  if (typeof error?.message === 'string') return error.message
  return '服务请求失败'
}

const resetForm = () => {
  Object.assign(form, defaultForm(filterDefined(lastPrefill.value || {})))
  prediction.value = null
  predictionError.value = ''
}

const fetchHealth = async () => {
  healthLoading.value = true
  healthError.value = ''
  try {
    health.value = await getHealthStatus()
  } catch (error) {
    health.value = null
    healthError.value = extractErrorMessage(error)
  } finally {
    healthLoading.value = false
  }
}

const loadPreview = async () => {
  previewLoading.value = true
  previewError.value = ''
  try {
    const limit = Math.max(1, Number(previewLimit.value) || 5)
    const data = await previewTrainingData(limit)
    preview.columns = Array.isArray(data?.columns) ? data.columns : []
    preview.rows = Array.isArray(data?.rows) ? data.rows : []
  } catch (error) {
    preview.columns = []
    preview.rows = []
    previewError.value = extractErrorMessage(error)
  } finally {
    previewLoading.value = false
  }
}

const submitPrediction = async () => {
  if (props.disablePredict) {
    ElMessage.warning('请先完成上方的产量预测配置后再使用机器学习预测')
    return
  }
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  predictionLoading.value = true
  predictionError.value = ''
  try {
    const response = await predictCropYield({ ...form })
    if (!response?.success) {
      throw new Error(response?.error || '预测失败')
    }
    prediction.value = response.data
    emit('prediction-success', response.data)
    ElMessage.success('预测生成成功')
  } catch (error) {
    prediction.value = null
    predictionError.value = extractErrorMessage(error)
    ElMessage.error(predictionError.value)
  } finally {
    predictionLoading.value = false
  }
}

onMounted(async () => {
  await fetchHealth()
  await loadPreview()
})
</script>

<style scoped>
.ml-card {
  border-radius: 20px;
  overflow: hidden;
}

.ml-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #0d1b3d;
}

.card-subtitle {
  font-size: 13px;
  color: #5c6d8d;
  margin-top: 4px;
}

.ml-status-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ml-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.ml-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2d4d;
}

.section-header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6e7fa1;
}

.section-alert {
  margin-top: -8px;
}

.ml-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 12px;
}

.input-hint {
  margin-left: 12px;
  color: #6e7fa1;
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.prediction-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.08), rgba(64, 158, 255, 0.08));
  border: 1px solid rgba(64, 158, 255, 0.15);
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.result-block {
  padding: 12px 16px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(22, 59, 140, 0.08);
}

.result-label {
  font-size: 12px;
  color: #5c6d8d;
  margin-bottom: 6px;
}

.result-value {
  font-size: 22px;
  font-weight: 700;
  color: #163b8c;
}

.preview-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.preview-control {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5c6d8d;
}

.preview-table {
  max-height: 320px;
  overflow: auto;
}

@media (max-width: 768px) {
  .ml-card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .ml-status-group {
    align-self: flex-end;
  }
}
</style>
