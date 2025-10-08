<template>
  <div class="page">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in overviewCards" :key="card.title">
        <el-card shadow="hover" class="page__card">
          <div class="page__card-title">{{ card.title }}</div>
          <div class="page__card-value">{{ card.value }}</div>
          <div class="page__card-desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover" class="page__panel">
          <template #header>近10年主要作物产量趋势</template>
          <div class="page__placeholder">折线图占位：整合ECharts实现趋势分析</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="page__panel">
          <template #header>区域结构占比</template>
          <div class="page__placeholder">饼图占位：显示地州产量占比</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const overviewCards = [
  { title: '累计数据集', value: '18,245', desc: '历史导入的作物数据记录' },
  { title: '覆盖作物', value: '12', desc: '重点监测的作物种类' },
  { title: '预测任务', value: '36', desc: '已执行的预测批次' },
  { title: '平均误差(MAPE)', value: '6.8%', desc: '上一季度模型综合表现' }
];
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page__card {
  border-radius: 16px;
  background: linear-gradient(180deg, #eff6ff 0%, #fff 100%);
}

.page__card-title {
  font-size: 14px;
  color: #6b7280;
}

.page__card-value {
  font-size: 26px;
  font-weight: 600;
  margin: 12px 0 6px;
}

.page__card-desc {
  color: #9ca3af;
}

.page__panel {
  border-radius: 16px;
  min-height: 280px;
}

.page__placeholder {
  height: 240px;
  background: repeating-linear-gradient(
    45deg,
    rgba(37, 99, 235, 0.08),
    rgba(37, 99, 235, 0.08) 10px,
    rgba(59, 130, 246, 0.08) 10px,
    rgba(59, 130, 246, 0.08) 20px
  );
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-weight: 500;
}
</style>
