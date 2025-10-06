<template>
  <div class="system-settings">
    <div class="page-header">
      <h2>系统设置</h2>
      <p>配置系统参数和模型设置</p>
    </div>
    
    <!-- 系统配置 -->
    <div class="system-config">
      <div class="chart-container">
        <div class="chart-title">系统配置</div>
        <el-form :model="systemConfig" label-width="150px">
          <el-form-item label="系统名称">
            <el-input v-model="systemConfig.systemName" style="width: 300px" />
          </el-form-item>
          
          <el-form-item label="数据备份频率">
            <el-select v-model="systemConfig.backupFrequency" style="width: 200px">
              <el-option label="每日" value="daily" />
              <el-option label="每周" value="weekly" />
              <el-option label="每月" value="monthly" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="日志保留天数">
            <el-input-number v-model="systemConfig.logRetentionDays" :min="1" :max="365" />
          </el-form-item>
          
          <el-form-item label="最大文件上传大小">
            <el-input-number v-model="systemConfig.maxFileSize" :min="1" :max="100" />
            <span style="margin-left: 8px;">MB</span>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="saveSystemConfig">保存配置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 模型配置 -->
    <div class="model-config">
      <div class="chart-container">
        <div class="chart-title">预测模型配置</div>
        
        <el-tabs v-model="activeModelTab">
          <el-tab-pane label="ARIMA模型" name="arima">
            <el-form :model="arimaConfig" label-width="120px">
              <el-form-item label="自动参数选择">
                <el-switch v-model="arimaConfig.autoSelect" />
              </el-form-item>
              <el-form-item label="最大阶数">
                <el-input-number v-model="arimaConfig.maxOrder" :min="1" :max="10" />
              </el-form-item>
              <el-form-item label="差分次数">
                <el-input-number v-model="arimaConfig.diffOrder" :min="0" :max="3" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="Prophet模型" name="prophet">
            <el-form :model="prophetConfig" label-width="120px">
              <el-form-item label="季节性模式">
                <el-checkbox-group v-model="prophetConfig.seasonality">
                  <el-checkbox label="yearly">年度</el-checkbox>
                  <el-checkbox label="weekly">周度</el-checkbox>
                  <el-checkbox label="daily">日度</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="趋势变化点">
                <el-input-number v-model="prophetConfig.changepointPriorScale" :min="0.001" :max="0.5" :step="0.001" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="XGBoost模型" name="xgboost">
            <el-form :model="xgboostConfig" label-width="120px">
              <el-form-item label="学习率">
                <el-input-number v-model="xgboostConfig.learningRate" :min="0.01" :max="1" :step="0.01" />
              </el-form-item>
              <el-form-item label="树的最大深度">
                <el-input-number v-model="xgboostConfig.maxDepth" :min="3" :max="10" />
              </el-form-item>
              <el-form-item label="子样本比例">
                <el-input-number v-model="xgboostConfig.subsample" :min="0.1" :max="1" :step="0.1" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="LSTM模型" name="lstm">
            <el-form :model="lstmConfig" label-width="120px">
              <el-form-item label="隐藏层单元数">
                <el-input-number v-model="lstmConfig.hiddenUnits" :min="32" :max="512" />
              </el-form-item>
              <el-form-item label="序列长度">
                <el-input-number v-model="lstmConfig.sequenceLength" :min="5" :max="50" />
              </el-form-item>
              <el-form-item label="学习率">
                <el-input-number v-model="lstmConfig.learningRate" :min="0.001" :max="0.1" :step="0.001" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
        
        <div class="model-actions">
          <el-button type="primary" @click="saveModelConfig">保存模型配置</el-button>
          <el-button @click="resetModelConfig">重置配置</el-button>
        </div>
      </div>
    </div>
    
    <!-- 数据源配置 -->
    <div class="data-source-config">
      <div class="chart-container">
        <div class="chart-title">数据源配置</div>
        
        <el-table :data="dataSources" stripe>
          <el-table-column prop="name" label="数据源名称" width="200" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '活跃' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastUpdate" label="最后更新" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" @click="editDataSource(scope.row)">编辑</el-button>
              <el-button size="small" type="success" @click="testDataSource(scope.row)">测试</el-button>
              <el-button size="small" type="danger" @click="deleteDataSource(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="data-source-actions">
          <el-button type="primary" @click="addDataSource">
            <el-icon><Plus /></el-icon>
            添加数据源
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 系统监控 -->
    <div class="system-monitor">
      <div class="chart-container">
        <div class="chart-title">系统监控</div>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <div class="monitor-card">
              <div class="monitor-icon">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="monitor-content">
                <div class="monitor-value">45%</div>
                <div class="monitor-label">CPU使用率</div>
              </div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <div class="monitor-card">
              <div class="monitor-icon">
                <el-icon><DataBoard /></el-icon>
              </div>
              <div class="monitor-content">
                <div class="monitor-value">2.1GB</div>
                <div class="monitor-label">内存使用</div>
              </div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <div class="monitor-card">
              <div class="monitor-icon">
                <el-icon><Folder /></el-icon>
              </div>
              <div class="monitor-content">
                <div class="monitor-value">156GB</div>
                <div class="monitor-label">磁盘使用</div>
              </div>
            </div>
          </el-col>
          
          <el-col :xs="24" :sm="12" :md="6">
            <div class="monitor-card">
              <div class="monitor-icon">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="monitor-content">
                <div class="monitor-value">在线</div>
                <div class="monitor-label">系统状态</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
    
    <!-- 数据源编辑对话框 -->
    <el-dialog v-model="showDataSourceDialog" :title="dataSourceDialogTitle" width="500px">
      <el-form :model="dataSourceForm" label-width="100px">
        <el-form-item label="数据源名称">
          <el-input v-model="dataSourceForm.name" />
        </el-form-item>
        <el-form-item label="数据源类型">
          <el-select v-model="dataSourceForm.type" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Excel文件" value="excel" />
            <el-option label="CSV文件" value="csv" />
          </el-select>
        </el-form-item>
        <el-form-item label="连接地址">
          <el-input v-model="dataSourceForm.url" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="dataSourceForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="dataSourceForm.password" type="password" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showDataSourceDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDataSource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'SystemSettings',
  setup() {
    const activeModelTab = ref('arima')
    const showDataSourceDialog = ref(false)
    const dataSourceDialogTitle = ref('添加数据源')
    
    const systemConfig = reactive({
      systemName: '农作物产量预测系统',
      backupFrequency: 'daily',
      logRetentionDays: 30,
      maxFileSize: 10
    })
    
    const arimaConfig = reactive({
      autoSelect: true,
      maxOrder: 5,
      diffOrder: 1
    })
    
    const prophetConfig = reactive({
      seasonality: ['yearly', 'weekly'],
      changepointPriorScale: 0.05
    })
    
    const xgboostConfig = reactive({
      learningRate: 0.1,
      maxDepth: 6,
      subsample: 0.8
    })
    
    const lstmConfig = reactive({
      hiddenUnits: 128,
      sequenceLength: 12,
      learningRate: 0.001
    })
    
    const dataSourceForm = reactive({
      name: '',
      type: '',
      url: '',
      username: '',
      password: ''
    })
    
    const dataSources = ref([
      {
        id: 1,
        name: '主数据库',
        type: 'MySQL',
        status: 'active',
        lastUpdate: '2024-01-15 10:30:00'
      },
      {
        id: 2,
        name: '历史数据文件',
        type: 'Excel',
        status: 'active',
        lastUpdate: '2024-01-14 15:20:00'
      }
    ])
    
    const saveSystemConfig = () => {
      ElMessage.success('系统配置保存成功')
    }
    
    const saveModelConfig = () => {
      ElMessage.success('模型配置保存成功')
    }
    
    const resetModelConfig = () => {
      ElMessageBox.confirm('确定要重置所有模型配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 重置配置
        ElMessage.success('模型配置已重置')
      })
    }
    
    const addDataSource = () => {
      dataSourceDialogTitle.value = '添加数据源'
      Object.keys(dataSourceForm).forEach(key => {
        dataSourceForm[key] = ''
      })
      showDataSourceDialog.value = true
    }
    
    const editDataSource = (row) => {
      dataSourceDialogTitle.value = '编辑数据源'
      Object.keys(dataSourceForm).forEach(key => {
        dataSourceForm[key] = row[key] || ''
      })
      showDataSourceDialog.value = true
    }
    
    const testDataSource = (row) => {
      ElMessage.success(`测试数据源 ${row.name} 连接成功`)
    }
    
    const deleteDataSource = (row) => {
      ElMessageBox.confirm('确定要删除这个数据源吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = dataSources.value.findIndex(ds => ds.id === row.id)
        if (index > -1) {
          dataSources.value.splice(index, 1)
          ElMessage.success('数据源删除成功')
        }
      })
    }
    
    const saveDataSource = () => {
      if (!dataSourceForm.name || !dataSourceForm.type) {
        ElMessage.warning('请填写完整的数据源信息')
        return
      }
      
      ElMessage.success('数据源保存成功')
      showDataSourceDialog.value = false
    }
    
    onMounted(() => {
      // 初始化配置数据
    })
    
    return {
      activeModelTab,
      showDataSourceDialog,
      dataSourceDialogTitle,
      systemConfig,
      arimaConfig,
      prophetConfig,
      xgboostConfig,
      lstmConfig,
      dataSourceForm,
      dataSources,
      saveSystemConfig,
      saveModelConfig,
      resetModelConfig,
      addDataSource,
      editDataSource,
      testDataSource,
      deleteDataSource,
      saveDataSource
    }
  }
}
</script>

<style scoped>
.system-settings {
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

.system-config {
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

.model-config {
  margin-bottom: 30px;
}

.model-actions {
  margin-top: 20px;
  text-align: right;
}

.data-source-config {
  margin-bottom: 30px;
}

.data-source-actions {
  margin-top: 20px;
}

.system-monitor {
  margin-bottom: 30px;
}

.monitor-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  margin-bottom: 20px;
}

.monitor-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.monitor-icon .el-icon {
  font-size: 20px;
  color: #fff;
}

.monitor-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.monitor-label {
  font-size: 14px;
  color: #666;
}
</style>
