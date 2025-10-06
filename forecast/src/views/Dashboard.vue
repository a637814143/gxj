<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="statistics-row">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="statistics-card">
            <div class="card-icon">
              <el-icon><DataBoard /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-number">{{ statistics.totalData || 0 }}</div>
              <div class="card-label">数据总量</div>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <div class="statistics-card">
            <div class="card-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-number">{{ statistics.totalPredictions || 0 }}</div>
              <div class="card-label">预测结果</div>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <div class="statistics-card">
            <div class="card-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-number">{{ statistics.totalRegions || 0 }}</div>
              <div class="card-label">覆盖地区</div>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <div class="statistics-card">
            <div class="card-icon">
              <el-icon><Crop /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-number">{{ statistics.totalCrops || 0 }}</div>
              <div class="card-label">作物种类</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 图表区域 -->
    <div class="charts-row">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="12">
          <div class="chart-container">
            <div class="chart-title">产量趋势分析</div>
            <div class="chart-content">
              <v-chart
                :option="yieldTrendOption"
                style="height: 300px;"
                autoresize
              />
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :lg="12">
          <div class="chart-container">
            <div class="chart-title">地区产量分布</div>
            <div class="chart-content">
              <v-chart
                :option="regionDistributionOption"
                style="height: 300px;"
                autoresize
              />
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 预测结果概览 -->
    <div class="prediction-overview">
      <div class="chart-container">
        <div class="chart-title">最新预测结果</div>
        <el-table
          :data="predictionResults"
          style="width: 100%"
          :loading="loading"
        >
          <el-table-column prop="regionName" label="地区" width="120" />
          <el-table-column prop="cropType" label="作物类型" width="120" />
          <el-table-column prop="predictionYear" label="预测年份" width="100" />
          <el-table-column prop="predictedYield" label="预测产量(吨)" width="120" />
          <el-table-column prop="accuracyScore" label="准确度" width="100">
            <template #default="scope">
              <el-tag :type="getAccuracyType(scope.row.accuracyScore)">
                {{ (scope.row.accuracyScore * 100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="预测时间" width="180" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

export default {
  name: 'AppDashboard',
  components: {
    VChart
  },
  setup() {
    const loading = ref(false)
    const statistics = reactive({
      totalData: 0,
      totalPredictions: 0,
      totalRegions: 0,
      totalCrops: 0
    })
    
    const predictionResults = ref([])
    
    // 产量趋势图表配置
    const yieldTrendOption = reactive({
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['水稻', '小麦', '玉米', '大豆']
      },
      xAxis: {
        type: 'category',
        data: ['2019', '2020', '2021', '2022', '2023', '2024']
      },
      yAxis: {
        type: 'value',
        name: '产量(万吨)'
      },
      series: [
        {
          name: '水稻',
          type: 'line',
          data: [120, 132, 101, 134, 90, 230]
        },
        {
          name: '小麦',
          type: 'line',
          data: [220, 182, 191, 234, 290, 330]
        },
        {
          name: '玉米',
          type: 'line',
          data: [150, 232, 201, 154, 190, 330]
        },
        {
          name: '大豆',
          type: 'line',
          data: [320, 332, 301, 334, 390, 330]
        }
      ]
    })
    
    // 地区分布图表配置
    const regionDistributionOption = reactive({
      tooltip: {
        trigger: 'item'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '产量分布',
          type: 'pie',
          radius: '50%',
          data: [
            { value: 1048, name: '华北地区' },
            { value: 735, name: '华东地区' },
            { value: 580, name: '华南地区' },
            { value: 484, name: '华中地区' },
            { value: 300, name: '西北地区' }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    })
    
    const getAccuracyType = (score) => {
      if (score >= 0.8) return 'success'
      if (score >= 0.6) return 'warning'
      return 'danger'
    }
    
    const loadDashboardData = async () => {
      loading.value = true
      try {
        // 模拟数据加载
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        // 更新统计数据
        statistics.totalData = 12580
        statistics.totalPredictions = 342
        statistics.totalRegions = 28
        statistics.totalCrops = 15
        
        // 模拟预测结果数据
        predictionResults.value = [
          {
            regionName: '华北地区',
            cropType: '小麦',
            predictionYear: 2024,
            predictedYield: 1250.5,
            accuracyScore: 0.85,
            createdAt: '2024-01-15 10:30:00'
          },
          {
            regionName: '华东地区',
            cropType: '水稻',
            predictionYear: 2024,
            predictedYield: 980.2,
            accuracyScore: 0.78,
            createdAt: '2024-01-15 09:15:00'
          },
          {
            regionName: '华南地区',
            cropType: '玉米',
            predictionYear: 2024,
            predictedYield: 756.8,
            accuracyScore: 0.92,
            createdAt: '2024-01-15 08:45:00'
          }
        ]
      } catch (error) {
        console.error('加载仪表盘数据失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    onMounted(() => {
      loadDashboardData()
    })
    
    return {
      loading,
      statistics,
      predictionResults,
      yieldTrendOption,
      regionDistributionOption,
      getAccuracyType
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.statistics-row {
  margin-bottom: 20px;
}

.statistics-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
}

.card-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.card-content {
  flex: 1;
}

.card-number {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: #666;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.chart-content {
  height: 300px;
}

.prediction-overview {
  margin-bottom: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .statistics-card {
    flex-direction: column;
    text-align: center;
  }
  
  .card-icon {
    margin-right: 0;
    margin-bottom: 15px;
  }
  
  .chart-content {
    height: 250px;
  }
}
</style>
