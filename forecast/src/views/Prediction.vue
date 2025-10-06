<template>
  <div class="prediction">
    <div class="page-header">
      <h2>产量预测</h2>
      <p>使用机器学习模型预测作物产量，支持多种预测算法</p>
    </div>
    
    <!-- 预测模型选择 -->
    <div class="model-selection">
      <div class="chart-container">
        <div class="chart-title">选择预测模型</div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <div class="model-card" :class="{ active: selectedModel === 'arima' }" @click="selectModel('arima')">
              <div class="model-icon">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="model-name">ARIMA</div>
              <div class="model-desc">时间序列分析</div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <div class="model-card" :class="{ active: selectedModel === 'prophet' }" @click="selectModel('prophet')">
              <div class="model-icon">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <div class="model-name">Prophet</div>
              <div class="model-desc">Facebook预测工具</div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <div class="model-card" :class="{ active: selectedModel === 'xgboost' }" @click="selectModel('xgboost')">
              <div class="model-icon">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="model-name">XGBoost</div>
              <div class="model-desc">梯度提升算法</div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <div class="model-card" :class="{ active: selectedModel === 'lstm' }" @click="selectModel('lstm')">
              <div class="model-icon">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="model-name">LSTM</div>
              <div class="model-desc">长短期记忆网络</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
    
    <!-- 预测参数设置 -->
    <div class="prediction-params" v-if="selectedModel">
      <div class="chart-container">
        <div class="chart-title">预测参数设置</div>
        <el-form :model="predictionForm" label-width="120px">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="选择地区">
                <el-select v-model="predictionForm.region" placeholder="请选择地区" style="width: 100%">
                  <el-option label="华北地区" value="north" />
                  <el-option label="华东地区" value="east" />
                  <el-option label="华南地区" value="south" />
                  <el-option label="华中地区" value="central" />
                  <el-option label="西北地区" value="northwest" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="12">
              <el-form-item label="选择作物">
                <el-select v-model="predictionForm.crop" placeholder="请选择作物" style="width: 100%">
                  <el-option label="小麦" value="wheat" />
                  <el-option label="水稻" value="rice" />
                  <el-option label="玉米" value="corn" />
                  <el-option label="大豆" value="soybean" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="12">
              <el-form-item label="预测年份">
                <el-date-picker
                  v-model="predictionForm.year"
                  type="year"
                  placeholder="选择预测年份"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="12">
              <el-form-item label="历史数据范围">
                <el-date-picker
                  v-model="predictionForm.historyRange"
                  type="yearrange"
                  range-separator="至"
                  start-placeholder="开始年份"
                  end-placeholder="结束年份"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item>
            <el-button type="primary" size="large" @click="runPrediction" :loading="predicting">
              <el-icon><VideoPlay /></el-icon>
              开始预测
            </el-button>
            <el-button @click="resetForm">重置参数</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 预测结果 -->
    <div class="prediction-results" v-if="predictionResult">
      <div class="chart-container">
        <div class="chart-title">预测结果</div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="8">
            <div class="result-card">
              <div class="result-label">预测产量</div>
              <div class="result-value">{{ predictionResult.predictedYield }} 吨</div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="8">
            <div class="result-card">
              <div class="result-label">预测准确度</div>
              <div class="result-value">{{ (predictionResult.accuracy * 100).toFixed(1) }}%</div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="8">
            <div class="result-card">
              <div class="result-label">置信区间</div>
              <div class="result-value">{{ predictionResult.confidenceInterval }}</div>
            </div>
          </el-col>
        </el-row>
        
        <div class="result-chart">
          <div class="placeholder-chart">
            <el-icon><TrendCharts /></el-icon>
            <p>预测结果图表将在这里显示</p>
            <p class="placeholder-text">功能开发中...</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 情景模拟 -->
    <div class="scenario-simulation" v-if="selectedModel">
      <div class="chart-container">
        <div class="chart-title">情景模拟</div>
        <p class="section-desc">调整不同参数，查看预测结果的变化</p>
        
        <el-form :model="scenarioForm" label-width="120px">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8">
              <el-form-item label="价格变化">
                <el-slider
                  v-model="scenarioForm.priceChange"
                  :min="-50"
                  :max="50"
                  :step="5"
                  show-input
                  :format-tooltip="formatPriceTooltip"
                />
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="8">
              <el-form-item label="种植面积变化">
                <el-slider
                  v-model="scenarioForm.areaChange"
                  :min="-30"
                  :max="30"
                  :step="5"
                  show-input
                  :format-tooltip="formatAreaTooltip"
                />
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="8">
              <el-form-item label="天气条件">
                <el-select v-model="scenarioForm.weather" placeholder="选择天气条件" style="width: 100%">
                  <el-option label="正常" value="normal" />
                  <el-option label="干旱" value="drought" />
                  <el-option label="洪涝" value="flood" />
                  <el-option label="风调雨顺" value="favorable" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item>
            <el-button type="success" @click="runScenarioSimulation" :loading="simulating">
              <el-icon><Refresh /></el-icon>
              运行模拟
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="scenario-results" v-if="scenarioResult">
          <el-alert
            :title="`模拟结果：预测产量 ${scenarioResult.predictedYield} 吨，变化 ${scenarioResult.change > 0 ? '+' : ''}${scenarioResult.change}%`"
            type="info"
            show-icon
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'CropPrediction',
  setup() {
    const selectedModel = ref('')
    const predicting = ref(false)
    const simulating = ref(false)
    
    const predictionForm = reactive({
      region: '',
      crop: '',
      year: '',
      historyRange: []
    })
    
    const scenarioForm = reactive({
      priceChange: 0,
      areaChange: 0,
      weather: 'normal'
    })
    
    const predictionResult = ref(null)
    const scenarioResult = ref(null)
    
    const selectModel = (model) => {
      selectedModel.value = model
      ElMessage.success(`已选择 ${model.toUpperCase()} 模型`)
    }
    
    const runPrediction = () => {
      if (!selectedModel.value) {
        ElMessage.warning('请先选择预测模型')
        return
      }
      
      if (!predictionForm.region || !predictionForm.crop || !predictionForm.year) {
        ElMessage.warning('请填写完整的预测参数')
        return
      }
      
      predicting.value = true
      
      // 模拟预测过程
      setTimeout(() => {
        predictionResult.value = {
          predictedYield: 1250.5,
          accuracy: 0.85,
          confidenceInterval: '1100-1400 吨'
        }
        predicting.value = false
        ElMessage.success('预测完成')
      }, 2000)
    }
    
    const runScenarioSimulation = () => {
      if (!predictionResult.value) {
        ElMessage.warning('请先完成基础预测')
        return
      }
      
      simulating.value = true
      
      // 模拟情景分析
      setTimeout(() => {
        const baseYield = predictionResult.value.predictedYield
        const change = (scenarioForm.priceChange + scenarioForm.areaChange) / 10
        const newYield = baseYield * (1 + change / 100)
        
        scenarioResult.value = {
          predictedYield: newYield.toFixed(1),
          change: change.toFixed(1)
        }
        simulating.value = false
        ElMessage.success('情景模拟完成')
      }, 1000)
    }
    
    const resetForm = () => {
      Object.keys(predictionForm).forEach(key => {
        if (key === 'historyRange') {
          predictionForm[key] = []
        } else {
          predictionForm[key] = ''
        }
      })
      predictionResult.value = null
      scenarioResult.value = null
    }
    
    const formatPriceTooltip = (value) => {
      return `${value > 0 ? '+' : ''}${value}%`
    }
    
    const formatAreaTooltip = (value) => {
      return `${value > 0 ? '+' : ''}${value}%`
    }
    
    return {
      selectedModel,
      predicting,
      simulating,
      predictionForm,
      scenarioForm,
      predictionResult,
      scenarioResult,
      selectModel,
      runPrediction,
      runScenarioSimulation,
      resetForm,
      formatPriceTooltip,
      formatAreaTooltip
    }
  }
}
</script>

<style scoped>
.prediction {
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

.model-selection {
  margin-bottom: 30px;
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

.model-card {
  background: #f8f9fa;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.model-card:hover {
  border-color: #1890ff;
  transform: translateY(-2px);
}

.model-card.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.model-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.model-icon .el-icon {
  font-size: 20px;
  color: #fff;
}

.model-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.model-desc {
  font-size: 12px;
  color: #666;
}

.prediction-params {
  margin-bottom: 30px;
}

.prediction-results {
  margin-bottom: 30px;
}

.result-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  margin-bottom: 20px;
}

.result-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.result-value {
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
}

.result-chart {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
  margin-top: 20px;
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

.scenario-simulation {
  margin-bottom: 30px;
}

.section-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.scenario-results {
  margin-top: 20px;
}
</style>
