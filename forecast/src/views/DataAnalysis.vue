<template>
  <div class="data-analysis">
    <div class="page-header">
      <h2>数据分析</h2>
      <p>分析作物产量历史数据，发现趋势和规律</p>
    </div>
    
    <!-- 分析工具 -->
    <div class="analysis-tools">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8">
          <div class="tool-card">
            <div class="tool-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="tool-content">
              <h3>趋势分析</h3>
              <p>分析作物产量的时间趋势变化</p>
              <el-button type="primary" @click="showTrendAnalysis = true">开始分析</el-button>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="8">
          <div class="tool-card">
            <div class="tool-icon">
              <el-icon><PieChart /></el-icon>
            </div>
            <div class="tool-content">
              <h3>地区对比</h3>
              <p>对比不同地区的作物产量情况</p>
              <el-button type="success" @click="showRegionComparison = true">开始对比</el-button>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="8">
          <div class="tool-card">
            <div class="tool-icon">
              <el-icon><DataBoard /></el-icon>
            </div>
            <div class="tool-content">
              <h3>结构分析</h3>
              <p>分析作物种植结构和占比</p>
              <el-button type="warning" @click="showStructureAnalysis = true">开始分析</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 分析结果展示 -->
    <div class="analysis-results" v-if="showTrendAnalysis || showRegionComparison || showStructureAnalysis">
      <div class="chart-container">
        <div class="chart-title">分析结果</div>
        <div class="chart-content">
          <div class="placeholder-chart">
            <el-icon><TrendCharts /></el-icon>
            <p>分析结果图表将在这里显示</p>
            <p class="placeholder-text">功能开发中...</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 趋势分析对话框 -->
    <el-dialog v-model="showTrendAnalysis" title="趋势分析" width="600px">
      <el-form :model="trendForm" label-width="100px">
        <el-form-item label="选择地区">
          <el-select v-model="trendForm.region" placeholder="请选择地区" style="width: 100%">
            <el-option label="华北地区" value="north" />
            <el-option label="华东地区" value="east" />
            <el-option label="华南地区" value="south" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择作物">
          <el-select v-model="trendForm.crop" placeholder="请选择作物" style="width: 100%">
            <el-option label="小麦" value="wheat" />
            <el-option label="水稻" value="rice" />
            <el-option label="玉米" value="corn" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="trendForm.dateRange"
            type="yearrange"
            range-separator="至"
            start-placeholder="开始年份"
            end-placeholder="结束年份"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTrendAnalysis = false">取消</el-button>
        <el-button type="primary" @click="runTrendAnalysis">开始分析</el-button>
      </template>
    </el-dialog>
    
    <!-- 地区对比对话框 -->
    <el-dialog v-model="showRegionComparison" title="地区对比" width="600px">
      <el-form :model="regionForm" label-width="100px">
        <el-form-item label="选择地区">
          <el-select v-model="regionForm.regions" multiple placeholder="请选择地区" style="width: 100%">
            <el-option label="华北地区" value="north" />
            <el-option label="华东地区" value="east" />
            <el-option label="华南地区" value="south" />
            <el-option label="华中地区" value="central" />
            <el-option label="西北地区" value="northwest" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择作物">
          <el-select v-model="regionForm.crop" placeholder="请选择作物" style="width: 100%">
            <el-option label="小麦" value="wheat" />
            <el-option label="水稻" value="rice" />
            <el-option label="玉米" value="corn" />
          </el-select>
        </el-form-item>
        <el-form-item label="对比年份">
          <el-date-picker
            v-model="regionForm.year"
            type="year"
            placeholder="选择年份"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegionComparison = false">取消</el-button>
        <el-button type="primary" @click="runRegionComparison">开始对比</el-button>
      </template>
    </el-dialog>
    
    <!-- 结构分析对话框 -->
    <el-dialog v-model="showStructureAnalysis" title="结构分析" width="600px">
      <el-form :model="structureForm" label-width="100px">
        <el-form-item label="选择地区">
          <el-select v-model="structureForm.region" placeholder="请选择地区" style="width: 100%">
            <el-option label="华北地区" value="north" />
            <el-option label="华东地区" value="east" />
            <el-option label="华南地区" value="south" />
          </el-select>
        </el-form-item>
        <el-form-item label="分析年份">
          <el-date-picker
            v-model="structureForm.year"
            type="year"
            placeholder="选择年份"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分析维度">
          <el-radio-group v-model="structureForm.dimension">
            <el-radio label="area">种植面积</el-radio>
            <el-radio label="yield">产量</el-radio>
            <el-radio label="value">产值</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStructureAnalysis = false">取消</el-button>
        <el-button type="primary" @click="runStructureAnalysis">开始分析</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'DataAnalysis',
  setup() {
    const showTrendAnalysis = ref(false)
    const showRegionComparison = ref(false)
    const showStructureAnalysis = ref(false)
    
    const trendForm = reactive({
      region: '',
      crop: '',
      dateRange: []
    })
    
    const regionForm = reactive({
      regions: [],
      crop: '',
      year: ''
    })
    
    const structureForm = reactive({
      region: '',
      year: '',
      dimension: 'area'
    })
    
    const runTrendAnalysis = () => {
      ElMessage.success('趋势分析功能开发中...')
      showTrendAnalysis.value = false
    }
    
    const runRegionComparison = () => {
      ElMessage.success('地区对比功能开发中...')
      showRegionComparison.value = false
    }
    
    const runStructureAnalysis = () => {
      ElMessage.success('结构分析功能开发中...')
      showStructureAnalysis.value = false
    }
    
    return {
      showTrendAnalysis,
      showRegionComparison,
      showStructureAnalysis,
      trendForm,
      regionForm,
      structureForm,
      runTrendAnalysis,
      runRegionComparison,
      runStructureAnalysis
    }
  }
}
</script>

<style scoped>
.data-analysis {
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

.analysis-tools {
  margin-bottom: 30px;
}

.tool-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  text-align: center;
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.tool-card:hover {
  transform: translateY(-4px);
}

.tool-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.tool-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.tool-content h3 {
  color: #333;
  margin-bottom: 8px;
}

.tool-content p {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
}

.analysis-results {
  margin-bottom: 30px;
}

.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.chart-content {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
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
</style>
