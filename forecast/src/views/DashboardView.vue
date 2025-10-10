<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card class="metric-card">
          <div class="metric-title">{{ card.title }}</div>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-desc">{{ card.description }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="chart-card">
      <template #header>
        <span>近期产量预测趋势</span>
      </template>
      <div class="placeholder">图表占位，后续接入可视化组件</div>
    </el-card>
  </div>
</template>

<script setup>
const cards = [
  { title: '覆盖区域', value: '32', description: '已接入的重点农业产区数量' },
  { title: '监测作物', value: '18', description: '追踪预测的主要农作物种类' },
  { title: '数据文件', value: '256', description: '已上传的历史数据文件数量' },
  { title: '模型任务', value: '12', description: '近30天内执行的预测任务数' }
]
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-card {
  border-radius: 12px;
}

.metric-title {
  font-size: 14px;
  color: #909399;
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  margin: 12px 0 4px;
}

.metric-desc {
  font-size: 12px;
  color: #a8abb2;
}

.chart-card {
  min-height: 320px;
}

.placeholder {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}
</style>
