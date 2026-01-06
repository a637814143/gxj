<!--
  @component DataStatisticsCard
  @description 数据统计卡片 - 展示数据的统计信息
  @props data - 数据数组
-->
<template>
  <div class="data-statistics-card">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #ecf5ff; color: #409eff">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">数据总量</div>
            <div class="stat-value">{{ totalRecords }}</div>
            <div class="stat-sub">条记录</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #f0f9ff; color: #67c23a">
            <el-icon><Crop /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">作物种类</div>
            <div class="stat-value">{{ uniqueCrops }}</div>
            <div class="stat-sub">种作物</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fef0f0; color: #f56c6c">
            <el-icon><Location /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">覆盖地区</div>
            <div class="stat-value">{{ uniqueRegions }}</div>
            <div class="stat-sub">个地区</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">时间跨度</div>
            <div class="stat-value">{{ yearSpan }}</div>
            <div class="stat-sub">{{ yearRange }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" class="detail-card">
          <div class="detail-title">产量统计</div>
          <div class="detail-content">
            <div class="detail-item">
              <span class="detail-label">总产量</span>
              <span class="detail-value">{{ totalProduction }} 万吨</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">平均产量</span>
              <span class="detail-value">{{ avgProduction }} 万吨</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">最高产量</span>
              <span class="detail-value">{{ maxProduction }} 万吨</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">最低产量</span>
              <span class="detail-value">{{ minProduction }} 万吨</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" class="detail-card">
          <div class="detail-title">数据质量</div>
          <div class="detail-content">
            <div class="detail-item">
              <span class="detail-label">完整数据</span>
              <span class="detail-value">
                {{ completeRecords }} 条 ({{ completePercentage }}%)
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">部分缺失</span>
              <span class="detail-value">
                {{ partialRecords }} 条 ({{ partialPercentage }}%)
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">最新数据</span>
              <span class="detail-value">{{ latestYear }} 年</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">最早数据</span>
              <span class="detail-value">{{ earliestYear }} 年</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Document, Crop, Location, Calendar } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Array, default: () => [] }
})

const totalRecords = computed(() => props.data.length)

const uniqueCrops = computed(() => {
  return new Set(props.data.map(d => d.cropName).filter(Boolean)).size
})

const uniqueRegions = computed(() => {
  return new Set(props.data.map(d => d.regionName).filter(Boolean)).size
})

const years = computed(() => {
  return props.data.map(d => d.year).filter(y => y != null)
})

const latestYear = computed(() => {
  return years.value.length ? Math.max(...years.value) : '-'
})

const earliestYear = computed(() => {
  return years.value.length ? Math.min(...years.value) : '-'
})

const yearSpan = computed(() => {
  if (!years.value.length) return 0
  return latestYear.value - earliestYear.value + 1
})

const yearRange = computed(() => {
  if (!years.value.length) return '-'
  return `${earliestYear.value}-${latestYear.value}`
})

const productions = computed(() => {
  return props.data.map(d => d.production).filter(p => p != null && p > 0)
})

const totalProduction = computed(() => {
  if (!productions.value.length) return '0.00'
  return productions.value.reduce((sum, p) => sum + p, 0).toFixed(2)
})

const avgProduction = computed(() => {
  if (!productions.value.length) return '0.00'
  return (productions.value.reduce((sum, p) => sum + p, 0) / productions.value.length).toFixed(2)
})

const maxProduction = computed(() => {
  if (!productions.value.length) return '0.00'
  return Math.max(...productions.value).toFixed(2)
})

const minProduction = computed(() => {
  if (!productions.value.length) return '0.00'
  return Math.min(...productions.value).toFixed(2)
})

const completeRecords = computed(() => {
  return props.data.filter(d =>
    d.production != null && d.production > 0 &&
    d.sownArea != null && d.sownArea > 0
  ).length
})

const partialRecords = computed(() => {
  return props.data.filter(d =>
    (d.production != null && d.production > 0) &&
    (!d.sownArea || d.sownArea <= 0)
  ).length
})

const completePercentage = computed(() => {
  if (!totalRecords.value) return '0'
  return ((completeRecords.value / totalRecords.value) * 100).toFixed(1)
})

const partialPercentage = computed(() => {
  if (!totalRecords.value) return '0'
  return ((partialRecords.value / totalRecords.value) * 100).toFixed(1)
})
</script>

<style scoped>
.data-statistics-card {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
}

.detail-card {
  padding: 20px;
}

.detail-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  font-size: 13px;
  color: #606266;
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 768px) {
  .stat-card {
    margin-bottom: 12px;
  }
}
</style>
