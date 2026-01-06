<!--
  @component SmartRecommendationPanel
  @description 智能图表推荐面板 - 根据数据特征自动推荐合适的图表类型
  @emits update:smartEnabled - 智能推荐开关变更
  @emits update:trendType - 趋势图类型变更
  @emits update:structureType - 结构图类型变更
  @emits update:mapType - 地图类型变更
  @emits update:layoutTemplate - 布局模板变更
-->
<template>
  <el-card class="smart-panel" shadow="never">
    <div class="smart-panel-header">
      <div>
        <div class="smart-panel-title">智能图表推荐</div>
        <div class="smart-panel-desc">
          系统会根据数据特征自动匹配适合的图表类型，可随时切换为手动模式。
        </div>
      </div>
      <el-switch
        :model-value="smartEnabled"
        active-text="智能推荐"
        inactive-text="手动模式"
        @update:model-value="$emit('update:smartEnabled', $event)"
      />
    </div>
    <div class="smart-panel-body">
      <div class="smart-panel-group">
        <label>趋势分析</label>
        <div class="smart-panel-control">
          <el-select
            :model-value="trendType"
            size="small"
            class="smart-select"
            :disabled="smartEnabled"
            @update:model-value="$emit('update:trendType', $event)"
          >
            <el-option
              v-for="option in trendChartTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-tag :type="smartEnabled ? 'success' : 'info'" class="recommend-tag">
            推荐：{{ getChartTypeLabel(recommendedTrendType) }}
          </el-tag>
        </div>
      </div>
      <div class="smart-panel-group">
        <label>结构分析</label>
        <div class="smart-panel-control">
          <el-select
            :model-value="structureType"
            size="small"
            class="smart-select"
            :disabled="smartEnabled"
            @update:model-value="$emit('update:structureType', $event)"
          >
            <el-option
              v-for="option in structureChartTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-tag :type="smartEnabled ? 'success' : 'info'" class="recommend-tag">
            推荐：{{ getChartTypeLabel(recommendedStructureType) }}
          </el-tag>
        </div>
      </div>
      <div class="smart-panel-group">
        <label>分布分析</label>
        <div class="smart-panel-control">
          <el-select
            :model-value="mapType"
            size="small"
            class="smart-select"
            :disabled="smartEnabled"
            @update:model-value="$emit('update:mapType', $event)"
          >
            <el-option
              v-for="option in mapChartTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-tag :type="smartEnabled ? 'success' : 'info'" class="recommend-tag">
            推荐：{{ getChartTypeLabel(recommendedMapType) }}
          </el-tag>
        </div>
      </div>
      <div class="smart-panel-group">
        <label>布局模板</label>
        <div class="smart-panel-control">
          <el-select
            :model-value="layoutTemplate"
            size="small"
            class="smart-select"
            @update:model-value="$emit('update:layoutTemplate', $event)"
          >
            <el-option
              v-for="option in layoutTemplateOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
              :disabled="option.disabled"
            />
          </el-select>
        </div>
      </div>
    </div>
    <ul v-if="recommendationMessages.length" class="smart-panel-recommendations">
      <li v-for="message in recommendationMessages" :key="message">{{ message }}</li>
    </ul>
  </el-card>
</template>

<script setup>
const props = defineProps({
  smartEnabled: { type: Boolean, required: true },
  trendType: { type: String, required: true },
  structureType: { type: String, required: true },
  mapType: { type: String, required: true },
  layoutTemplate: { type: String, required: true },
  recommendedTrendType: { type: String, required: true },
  recommendedStructureType: { type: String, required: true },
  recommendedMapType: { type: String, required: true },
  recommendationMessages: { type: Array, default: () => [] }
})

defineEmits(['update:smartEnabled', 'update:trendType', 'update:structureType', 'update:mapType', 'update:layoutTemplate'])

const trendChartTypeOptions = [
  { label: '折线图', value: 'line' },
  { label: '面积图', value: 'area' },
  { label: '柱状图', value: 'bar' }
]

const structureChartTypeOptions = [
  { label: '饼图', value: 'pie' },
  { label: '旭日图', value: 'sunburst' },
  { label: '雷达图', value: 'radar' }
]

const mapChartTypeOptions = [
  { label: '分级着色地图', value: 'choropleth' },
  { label: '热力图', value: 'heatmap' }
]

const layoutTemplateOptions = [
  { value: 'balanced', label: '均衡布局' },
  { value: 'focus-trend', label: '趋势优先' },
  { value: 'focus-structure', label: '结构洞察' },
  { value: 'focus-geo', label: '区域洞察' },
  { value: 'custom', label: '自定义布局', disabled: true }
]

const chartTypeLabelMap = new Map([
  ...trendChartTypeOptions.map(o => [o.value, o.label]),
  ...structureChartTypeOptions.map(o => [o.value, o.label]),
  ...mapChartTypeOptions.map(o => [o.value, o.label])
])

const getChartTypeLabel = (type) => chartTypeLabelMap.get(type) || type
</script>

<style scoped>
.smart-panel {
  margin-bottom: 20px;
}

.smart-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.smart-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.smart-panel-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.smart-panel-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.smart-panel-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.smart-panel-group label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.smart-panel-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.smart-select {
  flex: 1;
  min-width: 0;
}

.recommend-tag {
  flex-shrink: 0;
  font-size: 12px;
}

.smart-panel-recommendations {
  list-style: none;
  padding: 12px 16px;
  margin: 0;
  background: #f0f9ff;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.smart-panel-recommendations li {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
  padding-left: 16px;
  position: relative;
}

.smart-panel-recommendations li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #409eff;
  font-weight: bold;
}

@media (max-width: 1200px) {
  .smart-panel-body {
    grid-template-columns: 1fr;
  }
}
</style>
