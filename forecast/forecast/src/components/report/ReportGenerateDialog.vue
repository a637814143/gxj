<template>
  <el-dialog
    class="report-generate-dialog"
    :model-value="modelValue"
    width="640px"
    top="6vh"
    :append-to-body="true"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <template #header>
      <div class="dialog-header">
        <h3>生成专题分析报告</h3>
        <p>选择目标区域与作物，系统将自动分析历史数据并生成报告。</p>
      </div>
    </template>

    <div v-loading="loadingOptions" class="dialog-body">
      <el-alert v-if="optionsError" type="error" :closable="false" show-icon class="mb-3">
        {{ optionsError }}
      </el-alert>

      <el-form
        v-if="!loadingOptions"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="112px"
        label-position="left"
        size="large"
      >
        <el-form-item label="目标作物" prop="cropId">
          <el-select v-model="form.cropId" filterable placeholder="请选择作物">
            <el-option v-for="crop in crops" :key="crop.id" :label="crop.name" :value="crop.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="分析区域" prop="regionId">
          <el-select v-model="form.regionId" filterable placeholder="请选择区域">
            <el-option v-for="region in regions" :key="region.id" :label="region.name" :value="region.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="年份范围" required>
          <div class="year-range">
            <el-form-item prop="startYear" label-width="0">
              <el-input-number v-model="form.startYear" :min="1900" :max="2100" :step="1" />
            </el-form-item>
            <span class="year-separator">至</span>
            <el-form-item prop="endYear" label-width="0">
              <el-input-number v-model="form.endYear" :min="1900" :max="2100" :step="1" />
            </el-form-item>
          </div>
        </el-form-item>

        <el-form-item label="报告标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="不填写则按所选作物与区域自动生成"
            maxlength="128"
            show-word-limit
            @input="titleTouched = true"
          />
        </el-form-item>

        <el-form-item label="内容摘要" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="可填写报告摘要或生成目的"
            maxlength="512"
            show-word-limit
            :rows="2"
          />
        </el-form-item>

        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="默认显示为系统自动" maxlength="128" />
        </el-form-item>

        <el-form-item label="包含价格分析">
          <el-switch v-model="form.includePriceAnalysis" />
        </el-form-item>

        <el-form-item label="对比预测结果">
          <el-switch v-model="form.includeForecastComparison" @change="handleForecastToggle" />
        </el-form-item>

        <el-form-item v-if="form.includeForecastComparison" label="预测结果 ID" prop="forecastResultId">
          <el-input-number
            v-model="form.forecastResultId"
            :min="1"
            :controls="false"
            placeholder="请输入已生成的预测结果 ID"
          />
          <div class="field-hint">可在预测中心查看执行记录并复制结果 ID。</div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">生成报告</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { fetchCrops, fetchRegions, generateReport } from '../../services/report'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const submitting = ref(false)
const loadingOptions = ref(false)
const optionsError = ref('')
const crops = ref([])
const regions = ref([])
const titleTouched = ref(false)

const nowYear = new Date().getFullYear()
const form = reactive({
  cropId: null,
  regionId: null,
  startYear: nowYear - 4,
  endYear: nowYear,
  title: '',
  description: '',
  author: '',
  includePriceAnalysis: true,
  includeForecastComparison: false,
  forecastResultId: null
})

const rules = {
  cropId: [{ required: true, message: '请选择作物', trigger: 'change' }],
  regionId: [{ required: true, message: '请选择区域', trigger: 'change' }],
  startYear: [
    {
      validator: (_, value, callback) => {
        if (!value) {
          callback(new Error('请选择开始年份'))
        } else if (form.endYear && value > form.endYear) {
          callback(new Error('开始年份不能晚于结束年份'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  endYear: [
    {
      validator: (_, value, callback) => {
        if (!value) {
          callback(new Error('请选择结束年份'))
        } else if (form.startYear && value < form.startYear) {
          callback(new Error('结束年份不能早于开始年份'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  forecastResultId: [
    {
      validator: (_, value, callback) => {
        if (!form.includeForecastComparison) {
          callback()
          return
        }
        if (!value) {
          callback(new Error('请输入预测结果 ID'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const suggestionTitle = computed(() => {
  const crop = crops.value.find(item => item.id === form.cropId)
  const region = regions.value.find(item => item.id === form.regionId)
  if (!crop || !region) return ''
  if (form.startYear && form.endYear) {
    return `${region.name}${crop.name}${form.startYear}~${form.endYear}年分析报告`
  }
  return `${region.name}${crop.name}分析报告`
})

watch(
  () => props.modelValue,
  value => {
    if (value) {
      loadOptions()
      if (!titleTouched.value && !form.title && suggestionTitle.value) {
        form.title = suggestionTitle.value
      }
    }
  }
)

watch(
  [() => form.cropId, () => form.regionId, () => form.startYear, () => form.endYear],
  () => {
    if (!titleTouched.value && suggestionTitle.value) {
      form.title = suggestionTitle.value
    }
  }
)

const loadOptions = async () => {
  if (loadingOptions.value || crops.value.length || regions.value.length) {
    return
  }
  loadingOptions.value = true
  optionsError.value = ''
  try {
    const [cropResp, regionResp] = await Promise.all([fetchCrops(), fetchRegions()])
    crops.value = Array.isArray(cropResp?.data?.data) ? cropResp.data.data : cropResp?.data ?? []
    regions.value = Array.isArray(regionResp?.data?.data) ? regionResp.data.data : regionResp?.data ?? []
  } catch (error) {
    optionsError.value = error?.response?.data?.message || '加载作物与区域列表失败'
  } finally {
    loadingOptions.value = false
  }
}

const buildTitle = () => form.title?.trim() || suggestionTitle.value || '自动生成报告'

const buildDescription = () => {
  if (form.description?.trim()) return form.description.trim()
  const crop = crops.value.find(item => item.id === form.cropId)
  const region = regions.value.find(item => item.id === form.regionId)
  if (!crop || !region) return ''
  return `${region.name}${crop.name} ${form.startYear}~${form.endYear} 年自动生成的专题分析`
}

const handleForecastToggle = value => {
  if (!value) {
    form.forecastResultId = null
  }
}

const resetForm = () => {
  form.cropId = null
  form.regionId = null
  form.startYear = nowYear - 4
  form.endYear = nowYear
  form.title = ''
  form.description = ''
  form.author = ''
  form.includePriceAnalysis = true
  form.includeForecastComparison = false
  form.forecastResultId = null
  titleTouched.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      cropId: form.cropId,
      regionId: form.regionId,
      startYear: form.startYear,
      endYear: form.endYear,
      includePriceAnalysis: form.includePriceAnalysis,
      includeForecastComparison: form.includeForecastComparison,
      forecastResultId: form.includeForecastComparison ? form.forecastResultId : null,
      title: buildTitle(),
      description: buildDescription(),
      author: form.author?.trim() || null
    }

    const { data } = await generateReport(payload)
    const detail = data?.data ?? data
    ElMessage.success('报告生成成功')
    emit('success', detail)
    emit('update:modelValue', false)
    resetForm()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '生成报告失败')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
  resetForm()
}
</script>

<style scoped>
.report-generate-dialog :deep(.el-dialog) {
  margin: 6vh auto 0;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}

.report-generate-dialog :deep(.el-dialog__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
}

.report-generate-dialog :deep(.el-dialog__footer) {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.dialog-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialog-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.dialog-header p {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.dialog-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 24px 20px;
}

.year-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.year-separator {
  color: var(--el-text-color-regular);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.mb-3 {
  margin-bottom: 16px;
}
</style>
