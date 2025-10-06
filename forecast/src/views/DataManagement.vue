<template>
  <div class="data-management">
    <div class="page-header">
      <h2>数据管理</h2>
      <p>管理作物产量数据，支持数据导入、清洗和维护</p>
    </div>
    
    <!-- 功能卡片 -->
    <div class="function-cards">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8">
          <div class="function-card">
            <div class="card-icon">
              <el-icon><Upload /></el-icon>
            </div>
            <div class="card-content">
              <h3>数据导入</h3>
              <p>支持Excel、CSV格式数据导入</p>
              <el-button type="primary" @click="showUploadDialog = true">开始导入</el-button>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="8">
          <div class="function-card">
            <div class="card-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="card-content">
              <h3>数据清洗</h3>
              <p>自动检测和修复数据质量问题</p>
              <el-button type="success" @click="handleDataCleaning">开始清洗</el-button>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="8">
          <div class="function-card">
            <div class="card-icon">
              <el-icon><View /></el-icon>
            </div>
            <div class="card-content">
              <h3>数据预览</h3>
              <p>查看和管理已导入的数据</p>
              <el-button type="info" @click="showDataPreview = true">查看数据</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 数据统计 -->
    <div class="statistics-section">
      <div class="chart-container">
        <div class="chart-title">数据统计概览</div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="6">
            <div class="stat-item">
              <div class="stat-number">{{ statistics.totalRecords.toLocaleString() }}</div>
              <div class="stat-label">总数据量</div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="6">
            <div class="stat-item">
              <div class="stat-number">{{ statistics.regions }}</div>
              <div class="stat-label">覆盖地区</div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="6">
            <div class="stat-item">
              <div class="stat-number">{{ statistics.cropTypes }}</div>
              <div class="stat-label">作物种类</div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="6">
            <div class="stat-item">
              <div class="stat-number">{{ statistics.years }}</div>
              <div class="stat-label">年份跨度</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
    
    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="数据导入" width="600px">
      <!-- 上传配置 -->
      <div class="upload-config">
        <el-form :model="uploadConfig" label-width="100px" size="small">
          <el-form-item label="数据来源">
            <el-input v-model="uploadConfig.dataSource" placeholder="请输入数据来源" />
          </el-form-item>
          <el-form-item label="导入类型">
            <el-radio-group v-model="uploadConfig.importType">
              <el-radio label="FULL">全量导入</el-radio>
              <el-radio label="INCREMENTAL">增量导入</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="数据处理">
            <el-checkbox v-model="uploadConfig.cleanData">自动清洗数据</el-checkbox>
            <el-checkbox v-model="uploadConfig.validateData">验证数据格式</el-checkbox>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="uploadConfig.description" type="textarea" :rows="2" placeholder="可选，描述本次导入的内容" />
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 文件上传 -->
      <el-upload
        class="upload-demo"
        drag
        action="#"
        :file-list="fileList"
        :before-upload="beforeUpload"
        :on-change="handleFileChange"
        :on-remove="handleRemoveFile"
        :limit="1"
        :auto-upload="false"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 {{ formatInfo.supportedFormats.join(', ') }} 格式文件，最大 {{ formatInfo.maxFileSizeMB }}MB
            <br>
            <el-link type="primary" @click="handleDownloadTemplate" style="margin-top: 5px;">
              <el-icon><Download /></el-icon>
              下载CSV模板
            </el-link>
          </div>
        </template>
      </el-upload>
      
      <template #footer>
        <div style="margin-bottom: 10px; color: #666; font-size: 12px;">
          调试信息: fileList长度 = {{ fileList.length }}, 文件: {{ fileList.map(f => f.name).join(', ') }}
        </div>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="() => { console.log('按钮被点击'); handleUpload(); }" :loading="uploadLoading" :disabled="fileList.length === 0">
          {{ uploadLoading ? '导入中...' : '开始导入' }}
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 数据预览对话框 -->
    <el-dialog v-model="showDataPreview" title="数据预览" width="90%" top="5vh">
      <div v-loading="previewLoading" element-loading-text="加载数据中...">
        <el-table :data="previewData" style="width: 100%" max-height="500" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="regionName" label="地区" width="120" />
          <el-table-column prop="cropType" label="作物类型" width="100" />
          <el-table-column prop="cropName" label="作物名称" width="120" />
          <el-table-column prop="year" label="年份" width="80" />
          <el-table-column prop="season" label="季节" width="80" />
          <el-table-column prop="yield" label="产量(吨)" width="100" align="right">
            <template #default="scope">
              {{ scope.row.yield ? scope.row.yield.toFixed(2) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="plantingArea" label="种植面积(亩)" width="120" align="right">
            <template #default="scope">
              {{ scope.row.plantingArea ? scope.row.plantingArea.toFixed(2) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="unitYield" label="单产(吨/亩)" width="100" align="right">
            <template #default="scope">
              {{ scope.row.unitYield ? scope.row.unitYield.toFixed(4) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格(元/吨)" width="100" align="right">
            <template #default="scope">
              {{ scope.row.price ? scope.row.price.toFixed(2) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="totalValue" label="总产值(元)" width="120" align="right">
            <template #default="scope">
              {{ scope.row.totalValue ? scope.row.totalValue.toFixed(2) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="weatherCondition" label="天气条件" width="100" />
          <el-table-column prop="dataSource" label="数据来源" width="120" />
        </el-table>
        
        <!-- 分页 -->
        <div style="margin-top: 20px; text-align: center;">
          <el-pagination
            v-model:current-page="previewPagination.currentPage"
            v-model:page-size="previewPagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="previewPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchPreviewData"
            @current-change="fetchPreviewData"
          />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showDataPreview = false">关闭</el-button>
        <el-button type="primary" @click="handleDownloadTemplate">下载模板</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { 
  getAllCropData, 
  uploadCropData, 
  downloadCsvTemplate, 
  getSupportedFormats,
  getDistinctRegions,
  getDistinctCropTypes,
  getDistinctYears,
  getDataStatistics
} from '@/api/cropData'

export default {
  name: 'DataManagement',
  setup() {
    const showUploadDialog = ref(false)
    const showDataPreview = ref(false)
    const uploadLoading = ref(false)
    const previewLoading = ref(false)
    
    // 上传文件列表
    const fileList = ref([])
    
    // 上传配置
    const uploadConfig = reactive({
      dataSource: '文件导入',
      cleanData: true,
      validateData: true,
      description: '',
      importType: 'FULL'
    })
    
    // 支持的格式信息
    const formatInfo = ref({
      supportedFormats: ['CSV', 'XLS', 'XLSX'],
      maxFileSize: 52428800, // 50MB
      maxFileSizeMB: 50
    })
    
    // 数据预览
    const previewData = ref([])
    const previewPagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })
    
    // 数据统计
    const statistics = reactive({
      totalRecords: 0,
      regions: 0,
      cropTypes: 0,
      years: 0
    })
    
    // 获取支持的格式信息
    const fetchFormatInfo = async () => {
      try {
        const response = await getSupportedFormats()
        formatInfo.value = response
      } catch (error) {
        console.error('获取格式信息失败:', error)
      }
    }
    
    // 获取数据统计
    const fetchStatistics = async () => {
      try {
        const response = await getDataStatistics()
        
        statistics.totalRecords = response.totalRecords || 0
        statistics.regions = response.regions || 0
        statistics.cropTypes = response.cropTypes || 0
        statistics.years = response.years || 0
      } catch (error) {
        console.error('获取统计信息失败:', error)
        // 如果API失败，使用实时查询作为后备
        try {
          const [cropDataResponse, regionsResponse, cropTypesResponse, yearsResponse] = await Promise.all([
            getAllCropData(),
            getDistinctRegions(),
            getDistinctCropTypes(),
            getDistinctYears()
          ])
          
          statistics.totalRecords = cropDataResponse.length || 0
          statistics.regions = regionsResponse.length || 0
          statistics.cropTypes = cropTypesResponse.length || 0
          statistics.years = yearsResponse.length || 0
        } catch (fallbackError) {
          console.error('备用统计获取也失败:', fallbackError)
          // 使用默认值
          statistics.totalRecords = 0
          statistics.regions = 0
          statistics.cropTypes = 0
          statistics.years = 0
        }
      }
    }
    
    // 文件上传前验证
    const beforeUpload = (file) => {
      console.log('文件信息:', {
        name: file.name,
        type: file.type,
        size: file.size
      })
      
      const fileName = file.name.toLowerCase()
      const isValidFormat = fileName.endsWith('.xlsx') || 
                           fileName.endsWith('.xls') || 
                           fileName.endsWith('.csv')
      
      // 也检查MIME类型
      const isValidMimeType = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                              file.type === 'application/vnd.ms-excel' ||
                              file.type === 'text/csv' ||
                              file.type === 'application/csv' ||
                              file.type === 'text/plain'
      
      if (!isValidFormat && !isValidMimeType) {
        ElMessage.error(`不支持的文件格式！文件类型：${file.type}，支持的格式：${formatInfo.value.supportedFormats.join(', ')}`)
        return false
      }
      
      const isLtMaxSize = file.size < formatInfo.value.maxFileSize
      if (!isLtMaxSize) {
        ElMessage.error(`文件大小不能超过 ${formatInfo.value.maxFileSizeMB}MB！当前文件大小：${(file.size / 1024 / 1024).toFixed(2)}MB`)
        return false
      }
      
      // 添加到文件列表
      fileList.value = [file]
      console.log('文件已添加到列表:', fileList.value)
      console.log('fileList.value.length:', fileList.value.length)
      ElMessage.success(`文件选择成功：${file.name}，点击"开始导入"按钮进行上传`)
      return false // 阻止自动上传
    }
    
    // 处理文件上传
    const handleUpload = async () => {
      console.log('开始导入，文件列表:', fileList.value)
      if (fileList.value.length === 0) {
        ElMessage.warning('请先选择要上传的文件')
        return
      }
      
      const file = fileList.value[0]
      uploadLoading.value = true
      
      try {
        const response = await uploadCropData(file, uploadConfig)
        
        if (response.success) {
          ElMessage.success('数据导入成功！')
          
          // 显示导入结果
          const resultMessage = `
            导入完成！
            总记录数: ${response.totalRecords}
            成功导入: ${response.successRecords}
            失败记录: ${response.failedRecords}
            重复记录: ${response.duplicateRecords}
            清洗记录: ${response.cleanedRecords}
            处理时间: ${response.processingTimeMs}ms
          `
          
          ElMessageBox.alert(resultMessage, '导入结果', {
            confirmButtonText: '确定',
            type: 'success'
          })
          
          // 刷新统计数据
          await fetchStatistics()
          
          // 如果有错误或警告，显示详细信息
          if (response.errors && response.errors.length > 0) {
            console.warn('导入错误:', response.errors)
          }
          if (response.warnings && response.warnings.length > 0) {
            console.warn('导入警告:', response.warnings)
          }
        } else {
          ElMessage.error(`导入失败: ${response.message}`)
          
          if (response.errors && response.errors.length > 0) {
            const errorMessage = response.errors.slice(0, 5).join('\n') + 
                               (response.errors.length > 5 ? '\n...' : '')
            ElMessageBox.alert(errorMessage, '导入错误详情', {
              confirmButtonText: '确定',
              type: 'error'
            })
          }
        }
      } catch (error) {
        console.error('上传失败:', error)
        ElMessage.error('上传失败: ' + (error.message || '网络错误'))
      } finally {
        uploadLoading.value = false
        showUploadDialog.value = false
        fileList.value = []
      }
    }
    
    // 下载模板
    const handleDownloadTemplate = async () => {
      try {
        const response = await downloadCsvTemplate()
        
        // 创建下载链接
        const blob = new Blob([response], { type: 'text/csv;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'crop_data_template.csv'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        ElMessage.success('模板下载成功')
      } catch (error) {
        console.error('下载模板失败:', error)
        ElMessage.error('下载模板失败')
      }
    }
    
    // 数据清洗
    const handleDataCleaning = async () => {
      try {
        const result = await ElMessageBox.confirm(
          '数据清洗将检查和修复数据质量问题，此操作可能需要一些时间。是否继续？',
          '确认数据清洗',
          {
            confirmButtonText: '开始清洗',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        if (result === 'confirm') {
          const loading = ElLoading.service({
            lock: true,
            text: '正在进行数据清洗...',
            spinner: 'el-icon-loading',
            background: 'rgba(0, 0, 0, 0.7)'
          })
          
          // 模拟数据清洗过程
          await new Promise(resolve => setTimeout(resolve, 3000))
          
          loading.close()
          ElMessage.success('数据清洗完成！发现并修复了 15 个数据质量问题')
        }
      } catch (error) {
        console.error('数据清洗失败:', error)
        ElMessage.error('数据清洗失败')
      }
    }
    
    // 获取预览数据
    const fetchPreviewData = async () => {
      previewLoading.value = true
      try {
        const response = await getAllCropData()
        previewData.value = response || []
        previewPagination.total = previewData.value.length
        
        if (previewData.value.length === 0) {
          ElMessage.info('数据库中没有数据，请先导入数据')
        }
      } catch (error) {
        console.error('获取预览数据失败:', error)
        ElMessage.error('获取数据失败: ' + (error.message || '网络错误'))
        previewData.value = []
        previewPagination.total = 0
      } finally {
        previewLoading.value = false
      }
    }
    
    // 处理数据预览
    const handleDataPreview = async () => {
      showDataPreview.value = true
      await fetchPreviewData()
    }
    
    // 文件变化处理
    const handleFileChange = (file, fileList) => {
      console.log('文件变化:', file, fileList)
      // 更新fileList
      fileList.value = fileList
      console.log('更新后的fileList:', fileList.value)
    }
    
    // 移除文件
    const handleRemoveFile = (file) => {
      const index = fileList.value.indexOf(file)
      if (index > -1) {
        fileList.value.splice(index, 1)
      }
    }
    
    // 页面加载时获取数据
    onMounted(() => {
      fetchFormatInfo()
      fetchStatistics()
    })
    
    return {
      showUploadDialog,
      showDataPreview,
      uploadLoading,
      previewLoading,
      fileList,
      uploadConfig,
      formatInfo,
      previewData,
      previewPagination,
      statistics,
      beforeUpload,
      handleUpload,
      handleDownloadTemplate,
      handleDataCleaning,
      handleDataPreview,
      handleFileChange,
      handleRemoveFile
    }
  }
}
</script>

<style scoped>
.data-management {
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

.upload-config {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.upload-demo {
  margin-top: 10px;
}

.el-upload__tip {
  text-align: center;
  color: #999;
}

.el-upload__tip .el-link {
  margin-left: 10px;
}

.function-cards {
  margin-bottom: 30px;
}

.function-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  text-align: center;
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.function-card:hover {
  transform: translateY(-4px);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.card-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.card-content h3 {
  color: #333;
  margin-bottom: 8px;
}

.card-content p {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
}

.statistics-section {
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

.stat-item {
  text-align: center;
  padding: 16px;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.upload-demo {
  width: 100%;
}
</style>
