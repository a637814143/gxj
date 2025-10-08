<template>
  <div class="page">
    <el-card shadow="hover">
      <template #header>
        <div class="page__header">
          <span>数据资源管理</span>
          <div class="page__header-actions">
            <el-button type="primary">上传数据</el-button>
            <el-button>下载模板</el-button>
          </div>
        </div>
      </template>
      <el-table :data="dataset" border stripe>
        <el-table-column prop="name" label="数据集" width="220" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="records" label="记录数" width="120" />
        <el-table-column prop="updatedAt" label="最近更新" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag type="success" v-if="row.status === '已验证'">已验证</el-tag>
            <el-tag type="warning" v-else>待清洗</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
const dataset = [
  { name: '云南省粮食产量统计(2010-2024)', type: 'Excel', records: 2100, updatedAt: '2024-09-30', status: '已验证' },
  { name: '地州级气象指标', type: 'CSV', records: 3600, updatedAt: '2024-09-12', status: '待清洗' },
  { name: '市场价格与收益数据', type: 'Excel', records: 480, updatedAt: '2024-08-18', status: '已验证' }
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
