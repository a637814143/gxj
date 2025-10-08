<template>
  <div class="page">
    <el-card shadow="hover">
      <template #header>
        <div class="page__header">
          <span>报告产出</span>
          <div class="page__header-actions">
            <el-button type="primary">生成分析报告</el-button>
            <el-button>批量导出</el-button>
          </div>
        </div>
      </template>
      <el-table :data="reports" border>
        <el-table-column prop="title" label="报告名称" />
        <el-table-column prop="author" label="生成人" width="120" />
        <el-table-column prop="generatedAt" label="生成时间" width="180" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="160">
          <template #default>
            <el-button type="primary" text>预览</el-button>
            <el-button text>下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const reports = [
  { title: '2024年云南省玉米产量预测报告', author: '杨润标', generatedAt: '2024-10-08 11:20', status: '已发布' },
  { title: '滇东南甘蔗产业预警简报', author: '恭浩杰', generatedAt: '2024-09-26 09:05', status: '已发布' },
  { title: '水稻种植结构调整建议', author: '李敏', generatedAt: '2024-08-30 15:40', status: '草稿' }
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

.page__header-actions {
  display: flex;
  gap: 12px;
}
</style>
