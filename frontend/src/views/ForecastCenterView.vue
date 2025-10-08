<template>
  <div class="page">
    <el-card shadow="hover">
      <template #header>
        <div class="page__header">
          <span>预测任务管理</span>
          <el-button type="primary">新建预测任务</el-button>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="task in tasks"
          :key="task.id"
          :timestamp="task.timestamp"
          :type="task.type"
        >
          <h4>{{ task.title }}</h4>
          <p>{{ task.content }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" class="page__panel">
          <template #header>模型表现对比</template>
          <div class="page__placeholder">雷达图占位：展示模型MAPE/RMSE</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="page__panel">
          <template #header>未来三年产量预测</template>
          <div class="page__placeholder">柱状图占位：展示预测结果</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const tasks = [
  {
    id: 1,
    timestamp: '2024-10-12 09:30',
    type: 'success',
    title: '玉米产量预测任务已完成',
    content: '模型：Prophet；MAPE=7.2%；产量增长趋势向好。'
  },
  {
    id: 2,
    timestamp: '2024-09-18 14:10',
    type: 'warning',
    title: '水稻预测任务待确认数据',
    content: '缺失2023年腾冲市数据，需补充后重新训练。'
  },
  {
    id: 3,
    timestamp: '2024-08-30 16:45',
    type: 'info',
    title: '经济作物预测模型上线',
    content: '新增XGBoost模型，提升甘蔗预测精度。'
  }
];
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page__panel {
  min-height: 260px;
  border-radius: 16px;
}

.page__placeholder {
  height: 220px;
  border-radius: 12px;
  border: 1px dashed #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
}
</style>
