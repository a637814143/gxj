<!--
  @component ParameterConfigForm
  @description 参数配置表单 - 配置预测模型的参数
  @emits update:parameters - 参数变更
-->
<template>
  <el-card class="parameter-config-form" shadow="never">
    <template #header>
      <div class="form-header">
        <div class="form-title">模型参数配置</div>
        <el-button size="small" @click="resetToDefault">恢复默认</el-button>
      </div>
    </template>
    
    <el-form :model="formData" label-width="140px" label-position="left">
      <!-- 通用参数 -->
      <el-divider content-position="left">通用参数</el-divider>
      
      <el-form-item label="预测步长">
        <el-input-number
          v-model="formData.forecastSteps"
          :min="1"
          :max="10"
          :step="1"
          @change="handleChange"
        />
        <span class="form-hint">预测未来多少年</span>
      </el-form-item>
      
      <el-form-item label="置信区间">
        <el-slider
          v-model="formData.confidenceLevel"
          :min="80"
          :max="99"
          :step="1"
          show-stops
          @change="handleChange"
        />
        <span class="form-hint">{{ formData.confidenceLevel }}%</span>
      </el-form-item>
      
      <!-- ARIMA 特定参数 -->
      <template v-if="modelType === 'ARIMA'">
        <el-divider content-position="left">ARIMA 参数</el-divider>
        
        <el-form-item label="自回归阶数 (p)">
          <el-input-number
            v-model="formData.arimaP"
            :min="0"
            :max="5"
            @change="handleChange"
          />
          <span class="form-hint">建议 0-2</span>
        </el-form-item>
        
        <el-form-item label="差分阶数 (d)">
          <el-input-number
            v-model="formData.arimaD"
            :min="0"
            :max="2"
            @change="handleChange"
          />
          <span class="form-hint">建议 1</span>
        </el-form-item>
        
        <el-form-item label="移动平均阶数 (q)">
          <el-input-number
            v-model="formData.arimaQ"
            :min="0"
            :max="5"
            @change="handleChange"
          />
          <span class="form-hint">建议 0-2</span>
        </el-form-item>
      </template>
      
      <!-- LSTM 特定参数 -->
      <template v-if="modelType === 'LSTM'">
        <el-divider content-position="left">LSTM 参数</el-divider>
        
        <el-form-item label="训练轮数">
          <el-input-number
            v-model="formData.lstmEpochs"
            :min="10"
            :max="100"
            :step="10"
            @change="handleChange"
          />
          <span class="form-hint">建议 20-50</span>
        </el-form-item>
        
        <el-form-item label="隐藏层大小">
          <el-input-number
            v-model="formData.lstmHiddenSize"
            :min="16"
            :max="128"
            :step="16"
            @change="handleChange"
          />
          <span class="form-hint">建议 32-64</span>
        </el-form-item>
        
        <el-form-item label="学习率">
          <el-select v-model="formData.lstmLearningRate" @change="handleChange">
            <el-option label="0.0001 (慢)" :value="0.0001" />
            <el-option label="0.001 (推荐)" :value="0.001" />
            <el-option label="0.01 (快)" :value="0.01" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="Dropout率">
          <el-slider
            v-model="formData.lstmDropout"
            :min="0"
            :max="0.5"
            :step="0.1"
            :format-tooltip="val => `${(val * 100).toFixed(0)}%`"
            @change="handleChange"
          />
        </el-form-item>
      </template>
      
      <!-- Prophet 特定参数 -->
      <template v-if="modelType === 'PROPHET'">
        <el-divider content-position="left">Prophet 参数</el-divider>
        
        <el-form-item label="季节性模式">
          <el-select v-model="formData.prophetSeasonality" @change="handleChange">
            <el-option label="加法模式" value="additive" />
            <el-option label="乘法模式" value="multiplicative" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="年度季节性">
          <el-switch
            v-model="formData.prophetYearlySeason"
            @change="handleChange"
          />
        </el-form-item>
        
        <el-form-item label="周期性检测">
          <el-switch
            v-model="formData.prophetWeeklySeason"
            @change="handleChange"
          />
        </el-form-item>
      </template>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelType: { type: String, required: true },
  parameters: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:parameters'])

const defaultParameters = {
  forecastSteps: 3,
  confidenceLevel: 95,
  arimaP: 1,
  arimaD: 1,
  arimaQ: 1,
  lstmEpochs: 20,
  lstmHiddenSize: 32,
  lstmLearningRate: 0.001,
  lstmDropout: 0.1,
  prophetSeasonality: 'additive',
  prophetYearlySeason: true,
  prophetWeeklySeason: false
}

const formData = ref({ ...defaultParameters, ...props.parameters })

const handleChange = () => {
  emit('update:parameters', { ...formData.value })
}

const resetToDefault = () => {
  formData.value = { ...defaultParameters }
  handleChange()
}

watch(() => props.parameters, (newVal) => {
  formData.value = { ...defaultParameters, ...newVal }
}, { deep: true })
</script>

<style scoped>
.parameter-config-form {
  margin-bottom: 20px;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

:deep(.el-divider__text) {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}
</style>
