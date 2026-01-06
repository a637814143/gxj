<!--
  @component ModelSelectionPanel
  @description 模型选择面板 - 提供预测模型的选择和说明
  @emits update:modelValue - 模型选择变更
-->
<template>
  <el-card class="model-selection-panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <div class="panel-title">选择预测模型</div>
          <div class="panel-desc">根据数据特征和预测需求选择合适的模型</div>
        </div>
      </div>
    </template>
    
    <div class="model-grid">
      <div
        v-for="model in models"
        :key="model.value"
        class="model-card"
        :class="{ active: modelValue === model.value, disabled: model.disabled }"
        @click="!model.disabled && $emit('update:modelValue', model.value)"
      >
        <div class="model-icon">{{ model.icon }}</div>
        <div class="model-name">{{ model.name }}</div>
        <div class="model-desc">{{ model.description }}</div>
        <div class="model-features">
          <el-tag
            v-for="feature in model.features"
            :key="feature"
            size="small"
            effect="plain"
            class="feature-tag"
          >
            {{ feature }}
          </el-tag>
        </div>
        <div v-if="model.disabled" class="model-disabled-badge">即将推出</div>
        <div v-if="modelValue === model.value" class="model-selected-badge">
          <el-icon><Check /></el-icon>
        </div>
      </div>
    </div>
    
    <div v-if="selectedModel" class="model-info">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          <strong>{{ selectedModel.name }}</strong> - {{ selectedModel.description }}
        </template>
        <div class="model-details">
          <div><strong>适用场景：</strong>{{ selectedModel.scenario }}</div>
          <div><strong>数据要求：</strong>{{ selectedModel.requirement }}</div>
          <div><strong>预测精度：</strong>{{ selectedModel.accuracy }}</div>
        </div>
      </el-alert>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, required: true }
})

defineEmits(['update:modelValue'])

const models = [
  {
    value: 'ARIMA',
    name: 'ARIMA 时序模型',
    icon: '📈',
    description: '经典时间序列分析模型',
    features: ['趋势分析', '季节性', '快速预测'],
    scenario: '适用于具有明显趋势和季节性的数据',
    requirement: '至少6年历史数据',
    accuracy: 'R² 通常在 0.20-0.50',
    disabled: false
  },
  {
    value: 'PROPHET',
    name: 'Prophet 事件模型',
    icon: '🔮',
    description: 'Facebook开发的时序预测模型',
    features: ['节假日效应', '异常检测', '自动调参'],
    scenario: '适用于有节假日影响和异常值的数据',
    requirement: '至少6年历史数据',
    accuracy: 'R² 通常在 0.20-0.50',
    disabled: false
  },
  {
    value: 'LSTM',
    name: 'LSTM 深度学习',
    icon: '🧠',
    description: '长短期记忆神经网络',
    features: ['深度学习', '非线性', '长期依赖'],
    scenario: '适用于复杂非线性关系的数据',
    requirement: '至少8年历史数据',
    accuracy: 'R² 可达 0.60+（优化后）',
    disabled: false
  },
  {
    value: 'ENSEMBLE',
    name: '集成模型',
    icon: '🎯',
    description: '多模型融合预测',
    features: ['高精度', '稳定性强', '综合优势'],
    scenario: '适用于对精度要求高的场景',
    requirement: '至少8年历史数据',
    accuracy: 'R² 可达 0.70+',
    disabled: true
  }
]

const selectedModel = computed(() => {
  return models.find(m => m.value === props.modelValue)
})
</script>

<style scoped>
.model-selection-panel {
  margin-bottom: 20px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.panel-desc {
  font-size: 13px;
  color: #909399;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.model-card {
  position: relative;
  padding: 20px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.model-card:hover:not(.disabled) {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.model-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.model-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

.model-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.model-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.model-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.6;
}

.model-features {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.feature-tag {
  font-size: 11px;
}

.model-disabled-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 8px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  border-radius: 4px;
}

.model-selected-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: #67c23a;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.model-info {
  margin-top: 16px;
}

.model-details {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
}

.model-details div {
  margin-bottom: 4px;
}

@media (max-width: 768px) {
  .model-grid {
    grid-template-columns: 1fr;
  }
}
</style>
