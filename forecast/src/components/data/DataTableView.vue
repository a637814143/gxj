<!--
  @component DataTableView
  @description 数据表格视图 - 展示和管理产量数据
  @emits edit - 编辑数据
  @emits delete - 删除数据
  @emits selection-change - 选择变更
-->
<template>
  <el-card class="data-table-view" shadow="never">
    <template #header>
      <div class="table-header">
        <div class="table-title">数据列表</div>
        <div class="table-actions">
          <el-button
            v-if="selectedRows.length > 0"
            type="danger"
            size="small"
            @click="$emit('batch-delete', selectedRows)"
          >
            批量删除 ({{ selectedRows.length }})
          </el-button>
          <el-button size="small" @click="$emit('export')">导出数据</el-button>
        </div>
      </div>
    </template>
    
    <div class="table-filters">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索作物或地区"
        clearable
        style="width: 200px"
        @input="handleFilterChange"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <el-select
        v-model="filters.year"
        placeholder="筛选年份"
        clearable
        style="width: 150px"
        @change="handleFilterChange"
      >
        <el-option
          v-for="year in yearOptions"
          :key="year"
          :label="year"
          :value="year"
        />
      </el-select>
      
      <el-select
        v-model="filters.crop"
        placeholder="筛选作物"
        clearable
        filterable
        style="width: 150px"
        @change="handleFilterChange"
      >
        <el-option
          v-for="crop in cropOptions"
          :key="crop"
          :label="crop"
          :value="crop"
        />
      </el-select>
      
      <el-select
        v-model="filters.region"
        placeholder="筛选地区"
        clearable
        filterable
        style="width: 150px"
        @change="handleFilterChange"
      >
        <el-option
          v-for="region in regionOptions"
          :key="region"
          :label="region"
          :value="region"
        />
      </el-select>
    </div>
    
    <el-table
      :data="paginatedData"
      v-loading="loading"
      @selection-change="handleSelectionChange"
      border
      stripe
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="year" label="年份" width="100" sortable />
      <el-table-column prop="cropName" label="作物" width="120" />
      <el-table-column prop="cropCategory" label="类别" width="100" />
      <el-table-column prop="regionName" label="地区" width="120" />
      <el-table-column label="产量(万吨)" width="120" sortable>
        <template #default="{ row }">
          {{ formatNumber(row.production) }}
        </template>
      </el-table-column>
      <el-table-column label="播种面积(千公顷)" width="150">
        <template #default="{ row }">
          {{ row.sownArea ? formatNumber(row.sownArea) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="平均价格(元/吨)" width="140">
        <template #default="{ row }">
          {{ row.averagePrice ? formatNumber(row.averagePrice, 0) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="预估收益(万元)" width="140">
        <template #default="{ row }">
          {{ row.estimatedRevenue ? formatNumber(row.estimatedRevenue, 0) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="$emit('edit', row)">
            编辑
          </el-button>
          <el-button type="danger" size="small" link @click="$emit('delete', row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="table-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="filteredData.length"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete', 'batch-delete', 'export', 'selection-change'])

const filters = ref({
  keyword: '',
  year: null,
  crop: null,
  region: null
})

const pagination = ref({
  page: 1,
  pageSize: 20
})

const selectedRows = ref([])

const yearOptions = computed(() => {
  return [...new Set(props.data.map(d => d.year))].sort((a, b) => b - a)
})

const cropOptions = computed(() => {
  return [...new Set(props.data.map(d => d.cropName))].filter(Boolean).sort()
})

const regionOptions = computed(() => {
  return [...new Set(props.data.map(d => d.regionName))].filter(Boolean).sort()
})

const filteredData = computed(() => {
  let result = [...props.data]
  
  if (filters.value.keyword) {
    const keyword = filters.value.keyword.toLowerCase()
    result = result.filter(d =>
      d.cropName?.toLowerCase().includes(keyword) ||
      d.regionName?.toLowerCase().includes(keyword)
    )
  }
  
  if (filters.value.year) {
    result = result.filter(d => d.year === filters.value.year)
  }
  
  if (filters.value.crop) {
    result = result.filter(d => d.cropName === filters.value.crop)
  }
  
  if (filters.value.region) {
    result = result.filter(d => d.regionName === filters.value.region)
  }
  
  return result
})

const paginatedData = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return filteredData.value.slice(start, end)
})

const formatNumber = (value, decimals = 2) => {
  return Number(value).toFixed(decimals)
}

const handleFilterChange = () => {
  pagination.value.page = 1
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
  emit('selection-change', selection)
}

const handleSizeChange = () => {
  pagination.value.page = 1
}

const handlePageChange = () => {
  // 页码变化时滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.data-table-view {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.table-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
