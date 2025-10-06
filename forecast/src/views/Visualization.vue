<template>
  <div class="visualization">
    <div class="page-header">
      <h2>数据可视化</h2>
      <p>通过图表展示作物产量数据，支持多种可视化方式</p>
    </div>
    
    <!-- 图表类型选择 -->
    <div class="chart-selection">
      <div class="chart-container">
        <div class="chart-title">选择图表类型</div>
        <el-radio-group v-model="selectedChartType" @change="handleChartTypeChange">
          <el-radio-button label="line">趋势图</el-radio-button>
          <el-radio-button label="bar">柱状图</el-radio-button>
          <el-radio-button label="pie">饼图</el-radio-button>
          <el-radio-button label="scatter">散点图</el-radio-button>
          <el-radio-button label="map">地图</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    
    <!-- 图表配置 -->
    <div class="chart-config">
      <div class="chart-container">
        <div class="chart-title">图表配置</div>
        <el-form :model="chartConfig" label-width="100px" inline>
          <el-form-item label="选择地区">
            <el-select v-model="chartConfig.region" placeholder="请选择地区" style="width: 200px">
              <el-option label="全部地区" value="all" />
              <el-option label="华北地区" value="north" />
              <el-option label="华东地区" value="east" />
              <el-option label="华南地区" value="south" />
              <el-option label="华中地区" value="central" />
              <el-option label="西北地区" value="northwest" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="选择作物">
            <el-select v-model="chartConfig.crop" placeholder="请选择作物" style="width: 200px">
              <el-option label="全部作物" value="all" />
              <el-option label="小麦" value="wheat" />
              <el-option label="水稻" value="rice" />
              <el-option label="玉米" value="corn" />
              <el-option label="大豆" value="soybean" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="chartConfig.dateRange"
              type="yearrange"
              range-separator="至"
              start-placeholder="开始年份"
              end-placeholder="结束年份"
              style="width: 200px"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="generateChart">生成图表</el-button>
            <el-button @click="resetConfig">重置配置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 图表展示 -->
    <div class="chart-display">
      <div class="chart-container">
        <div class="chart-title">{{ getChartTitle() }}</div>
        <div class="chart-content">
          <div class="placeholder-chart">
            <el-icon><component :is="getChartIcon()" /></el-icon>
            <p>{{ getChartDescription() }}</p>
            <p class="placeholder-text">点击"生成图表"查看可视化结果</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 图表操作 -->
    <div class="chart-actions">
      <div class="chart-container">
        <div class="chart-title">图表操作</div>
        <div class="action-buttons">
          <el-button type="primary" @click="exportChart">
            <el-icon><Download /></el-icon>
            导出图片
          </el-button>
          <el-button type="success" @click="exportData">
            <el-icon><Document /></el-icon>
            导出数据
          </el-button>
          <el-button type="warning" @click="shareChart">
            <el-icon><Share /></el-icon>
            分享图表
          </el-button>
          <el-button type="info" @click="printChart">
            <el-icon><Printer /></el-icon>
            打印图表
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 图表库 -->
    <div class="chart-gallery">
      <div class="chart-container">
        <div class="chart-title">图表库</div>
        <p class="section-desc">保存的图表模板，可快速应用</p>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" v-for="template in chartTemplates" :key="template.id">
            <div class="template-card">
              <div class="template-preview">
                <el-icon><component :is="template.icon" /></el-icon>
              </div>
              <div class="template-info">
                <h4>{{ template.name }}</h4>
                <p>{{ template.description }}</p>
                <div class="template-actions">
                  <el-button size="small" @click="applyTemplate(template)">应用</el-button>
                  <el-button size="small" type="danger" @click="deleteTemplate(template.id)">删除</el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'DataVisualization',
  setup() {
    const selectedChartType = ref('line')
    
    const chartConfig = reactive({
      region: 'all',
      crop: 'all',
      dateRange: []
    })
    
    const chartTemplates = ref([
      {
        id: 1,
        name: '产量趋势分析',
        description: '展示历年作物产量变化趋势',
        icon: 'TrendCharts',
        type: 'line'
      },
      {
        id: 2,
        name: '地区产量对比',
        description: '对比不同地区的作物产量',
        icon: 'DataBoard',
        type: 'bar'
      },
      {
        id: 3,
        name: '作物结构分析',
        description: '分析作物种植结构占比',
        icon: 'PieChart',
        type: 'pie'
      }
    ])
    
    const handleChartTypeChange = (type) => {
      ElMessage.success(`已选择${getChartTypeName(type)}`)
    }
    
    const getChartTypeName = (type) => {
      const typeMap = {
        line: '趋势图',
        bar: '柱状图',
        pie: '饼图',
        scatter: '散点图',
        map: '地图'
      }
      return typeMap[type] || '未知类型'
    }
    
    const getChartTitle = () => {
      return `${getChartTypeName(selectedChartType.value)} - 作物产量数据可视化`
    }
    
    const getChartIcon = () => {
      const iconMap = {
        line: 'TrendCharts',
        bar: 'DataBoard',
        pie: 'PieChart',
        scatter: 'Scatter',
        map: 'Location'
      }
      return iconMap[selectedChartType.value] || 'TrendCharts'
    }
    
    const getChartDescription = () => {
      const descMap = {
        line: '展示数据随时间的变化趋势',
        bar: '对比不同类别的数据大小',
        pie: '显示数据的占比关系',
        scatter: '展示两个变量之间的相关性',
        map: '在地图上展示地理分布数据'
      }
      return descMap[selectedChartType.value] || '数据可视化图表'
    }
    
    const generateChart = () => {
      if (!chartConfig.dateRange || chartConfig.dateRange.length === 0) {
        ElMessage.warning('请选择时间范围')
        return
      }
      
      ElMessage.success('图表生成功能开发中...')
    }
    
    const resetConfig = () => {
      chartConfig.region = 'all'
      chartConfig.crop = 'all'
      chartConfig.dateRange = []
      ElMessage.success('配置已重置')
    }
    
    const exportChart = () => {
      ElMessage.success('图表导出功能开发中...')
    }
    
    const exportData = () => {
      ElMessage.success('数据导出功能开发中...')
    }
    
    const shareChart = () => {
      ElMessage.success('图表分享功能开发中...')
    }
    
    const printChart = () => {
      ElMessage.success('图表打印功能开发中...')
    }
    
    const applyTemplate = (template) => {
      selectedChartType.value = template.type
      ElMessage.success(`已应用模板：${template.name}`)
    }
    
    const deleteTemplate = (id) => {
      ElMessageBox.confirm('确定要删除这个模板吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = chartTemplates.value.findIndex(t => t.id === id)
        if (index > -1) {
          chartTemplates.value.splice(index, 1)
          ElMessage.success('模板删除成功')
        }
      })
    }
    
    return {
      selectedChartType,
      chartConfig,
      chartTemplates,
      handleChartTypeChange,
      getChartTitle,
      getChartIcon,
      getChartDescription,
      generateChart,
      resetConfig,
      exportChart,
      exportData,
      shareChart,
      printChart,
      applyTemplate,
      deleteTemplate
    }
  }
}
</script>

<style scoped>
.visualization {
  padding: 0;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
  font-size: 14px;
}

.chart-selection {
  margin-bottom: 20px;
}

.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.chart-config {
  margin-bottom: 20px;
}

.chart-display {
  margin-bottom: 20px;
}

.chart-content {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
}

.placeholder-chart {
  text-align: center;
  color: #999;
}

.placeholder-chart .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.placeholder-text {
  font-size: 12px;
  margin-top: 8px;
}

.chart-actions {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.chart-gallery {
  margin-bottom: 20px;
}

.section-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.template-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.template-preview {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.template-preview .el-icon {
  font-size: 24px;
  color: #fff;
}

.template-info h4 {
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}

.template-info p {
  color: #666;
  font-size: 12px;
  margin-bottom: 12px;
}

.template-actions {
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
  }
  
  .chart-content {
    height: 300px;
  }
}
</style>
