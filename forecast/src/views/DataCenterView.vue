<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据集管理</span>
          <el-button type="primary" @click="openUpload">上传数据</el-button>
        </div>
      </template>
      <el-table :data="datasets">
        <el-table-column prop="name" label="数据集名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column prop="description" label="描述" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'

const datasets = [
  { name: '2020-2023年小麦产量', type: '产量', updatedAt: '2024-04-01', description: '全国重点省份小麦产量数据' },
  { name: '2019-2023年玉米价格', type: '价格', updatedAt: '2024-03-21', description: '市场平均收购价' },
  { name: '2023年气象指标', type: '气象', updatedAt: '2024-02-15', description: '日均温度、降水、日照时长' }
]

const openUpload = () => {
  ElMessage.info('后续将接入文件上传能力')
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
</style>
