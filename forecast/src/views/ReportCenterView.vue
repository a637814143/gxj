<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分析报告</span>
          <el-button type="primary" @click="createReport">生成报告</el-button>
        </div>
      </template>
      <el-empty v-if="!reports.length" description="暂未生成报告" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="report in reports"
          :key="report.id"
          :timestamp="report.time"
          type="success"
        >
          <el-card shadow="never">
            <h3>{{ report.title }}</h3>
            <p class="desc">{{ report.description }}</p>
            <el-link type="primary">查看详情</el-link>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'

const reports = [
  {
    id: 1,
    title: '2024年春季小麦产量预测报告',
    description: '综合天气监测、历史产量和市场需求，预测2024年春季主要产区小麦产量同比增长3.2%。',
    time: '2024-03-28'
  }
]

const createReport = () => {
  ElMessage.info('后续将接入报告生成流程')
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.desc {
  color: #909399;
  margin: 12px 0;
}
</style>
