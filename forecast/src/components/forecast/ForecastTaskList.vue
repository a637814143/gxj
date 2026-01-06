<!--
  @component ForecastTaskList
  @description 预测任务列表 - 展示预测任务的状态和结果
  @emits view-result - 查看结果
  @emits delete-task - 删除任务
-->
<template>
  <el-card class="forecast-task-list" shadow="never">
    <template #header>
      <div class="list-header">
        <div class="list-title">预测任务列表</div>
        <el-button size="small" @click="$emit('refresh')">刷新</el-button>
      </div>
    </template>
    
    <el-table :data="tasks" v-loading="loading" border>
      <el-table-column prop="id" label="任务ID" width="80" />
      <el-table-column prop="modelType" label="模型" width="120">
        <template #default="{ row }">
          <el-tag :type="getModelTagType(row.modelType)" size="small">
            {{ row.modelType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cropName" label="作物" width="120" />
      <el-table-column prop="regionName" label="地区" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="progress" label="进度" width="120">
        <template #default="{ row }">
          <el-progress
            v-if="row.status === 'RUNNING'"
            :percentage="row.progress || 0"
            :status="row.progress === 100 ? 'success' : undefined"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="r2Score" label="R²值" width="100">
        <template #default="{ row }">
          <span v-if="row.r2Score != null" :class="getR2Class(row.r2Score)">
            {{ row.r2Score.toFixed(3) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'COMPLETED'"
            type="primary"
            size="small"
            link
            @click="$emit('view-result', row)"
          >
            查看结果
          </el-button>
          <el-button
            v-if="row.status === 'RUNNING'"
            type="warning"
            size="small"
            link
            @click="$emit('cancel-task', row)"
          >
            取消
          </el-button>
          <el-button
            v-if="row.status === 'FAILED'"
            type="info"
            size="small"
            link
            @click="$emit('retry-task', row)"
          >
            重试
          </el-button>
          <el-button
            type="danger"
            size="small"
            link
            @click="$emit('delete-task', row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="!loading && tasks.length === 0" class="empty-state">
      <el-empty description="暂无预测任务" />
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['view-result', 'delete-task', 'cancel-task', 'retry-task', 'refresh'])

const getModelTagType = (modelType) => {
  const typeMap = {
    'ARIMA': 'primary',
    'PROPHET': 'success',
    'LSTM': 'warning'
  }
  return typeMap[modelType] || 'info'
}

const getStatusTagType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const labelMap = {
    'PENDING': '等待中',
    'RUNNING': '运行中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return labelMap[status] || status
}

const getR2Class = (r2) => {
  if (r2 >= 0.6) return 'r2-excellent'
  if (r2 >= 0.4) return 'r2-good'
  if (r2 >= 0.2) return 'r2-fair'
  return 'r2-poor'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.forecast-task-list {
  margin-bottom: 20px;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.empty-state {
  padding: 40px 0;
}

.r2-excellent {
  color: #67c23a;
  font-weight: 600;
}

.r2-good {
  color: #409eff;
  font-weight: 600;
}

.r2-fair {
  color: #e6a23c;
  font-weight: 600;
}

.r2-poor {
  color: #f56c6c;
  font-weight: 600;
}
</style>
