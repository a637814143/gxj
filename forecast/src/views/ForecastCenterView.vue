<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>预测模型管理</span>
              <el-button type="primary" @click="createModel">新建模型</el-button>
            </div>
          </template>
          <el-table :data="models" size="small">
            <el-table-column prop="name" label="模型名称" />
            <el-table-column prop="type" label="算法类型" width="140" />
            <el-table-column prop="description" label="说明" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>预测任务</span>
              <el-button type="primary" @click="createTask">发起任务</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="task in tasks"
              :key="task.id"
              :timestamp="task.time"
              :type="task.type"
            >
              <p class="task-title">{{ task.title }}</p>
              <p class="task-desc">{{ task.description }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'

const models = [
  { name: '基于气象的ARIMA模型', type: 'ARIMA', description: '适用于产量时间序列预测' },
  { name: '遥感指标融合LSTM', type: 'LSTM', description: '结合遥感特征进行长周期预测' }
]

const tasks = [
  { id: 1, title: '2024年小麦产量预测', time: '2024-04-05', type: 'primary', description: '基于最新气象数据生成结果' },
  { id: 2, title: '玉米价格走势预测', time: '2024-04-02', type: 'success', description: '融合市场行情与库存数据' }
]

const createModel = () => {
  ElMessage.info('后续将接入模型配置表单')
}

const createTask = () => {
  ElMessage.info('后续将接入任务配置向导')
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

.task-title {
  font-weight: 600;
}

.task-desc {
  margin: 4px 0 0;
  color: #909399;
}
</style>
