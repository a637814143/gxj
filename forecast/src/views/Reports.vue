<template>
  <div class="reports">
    <div class="page-header">
      <h2>报告管理</h2>
      <p>生成和管理分析报告，支持多种格式导出</p>
    </div>
    
    <!-- 报告生成 -->
    <div class="report-generation">
      <div class="chart-container">
        <div class="chart-title">生成新报告</div>
        <el-form :model="reportForm" label-width="120px">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="报告类型">
                <el-select v-model="reportForm.type" placeholder="请选择报告类型" style="width: 100%">
                  <el-option label="产量分析报告" value="yield" />
                  <el-option label="预测分析报告" value="prediction" />
                  <el-option label="地区对比报告" value="comparison" />
                  <el-option label="综合评估报告" value="comprehensive" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="12">
              <el-form-item label="报告标题">
                <el-input v-model="reportForm.title" placeholder="请输入报告标题" />
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="12">
              <el-form-item label="选择地区">
                <el-select v-model="reportForm.region" placeholder="请选择地区" style="width: 100%">
                  <el-option label="全部地区" value="all" />
                  <el-option label="华北地区" value="north" />
                  <el-option label="华东地区" value="east" />
                  <el-option label="华南地区" value="south" />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="12">
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="reportForm.dateRange"
                  type="yearrange"
                  range-separator="至"
                  start-placeholder="开始年份"
                  end-placeholder="结束年份"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="报告描述">
            <el-input
              v-model="reportForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入报告描述"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="generateReport" :loading="generating">
              <el-icon><Document /></el-icon>
              生成报告
            </el-button>
            <el-button @click="resetReportForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 报告列表 -->
    <div class="report-list">
      <div class="chart-container">
        <div class="chart-title">报告列表</div>
        
        <!-- 搜索和筛选 -->
        <div class="search-bar">
          <el-form :model="searchForm" inline>
            <el-form-item>
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索报告标题"
                prefix-icon="Search"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.type" placeholder="报告类型" style="width: 150px">
                <el-option label="全部类型" value="" />
                <el-option label="产量分析" value="yield" />
                <el-option label="预测分析" value="prediction" />
                <el-option label="地区对比" value="comparison" />
                <el-option label="综合评估" value="comprehensive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchReports">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        
        <!-- 报告表格 -->
        <el-table :data="reportList" :loading="loading" stripe>
          <el-table-column prop="title" label="报告标题" min-width="200" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="scope">
              <el-tag :type="getTypeTagType(scope.row.type)">
                {{ getTypeText(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="region" label="地区" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column prop="fileSize" label="文件大小" width="100" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewReport(scope.row)">查看</el-button>
              <el-button size="small" type="success" @click="downloadReport(scope.row)">下载</el-button>
              <el-button size="small" type="danger" @click="deleteReport(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
    
    <!-- 报告预览对话框 -->
    <el-dialog v-model="showPreview" title="报告预览" width="80%" top="5vh">
      <div class="report-preview">
        <div class="preview-header">
          <h3>{{ currentReport.title }}</h3>
          <p>{{ currentReport.description }}</p>
        </div>
        
        <div class="preview-content">
          <div class="placeholder-content">
            <el-icon><Document /></el-icon>
            <p>报告内容预览</p>
            <p class="placeholder-text">功能开发中...</p>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showPreview = false">关闭</el-button>
        <el-button type="primary" @click="downloadReport(currentReport)">下载报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'DataReports',
  setup() {
    const loading = ref(false)
    const generating = ref(false)
    const showPreview = ref(false)
    const currentReport = ref({})
    
    const reportForm = reactive({
      type: '',
      title: '',
      region: '',
      dateRange: [],
      description: ''
    })
    
    const searchForm = reactive({
      keyword: '',
      type: ''
    })
    
    const reportList = ref([
      {
        id: 1,
        title: '2023年华北地区小麦产量分析报告',
        type: 'yield',
        region: '华北地区',
        status: 'completed',
        createdAt: '2024-01-15 10:30:00',
        fileSize: '2.5MB'
      },
      {
        id: 2,
        title: '2024年作物产量预测报告',
        type: 'prediction',
        region: '全国',
        status: 'completed',
        createdAt: '2024-01-14 15:20:00',
        fileSize: '3.2MB'
      },
      {
        id: 3,
        title: '华东地区与华南地区产量对比分析',
        type: 'comparison',
        region: '华东、华南',
        status: 'generating',
        createdAt: '2024-01-13 09:15:00',
        fileSize: '-'
      }
    ])
    
    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 3
    })
    
    const getTypeTagType = (type) => {
      const typeMap = {
        'yield': 'primary',
        'prediction': 'success',
        'comparison': 'warning',
        'comprehensive': 'info'
      }
      return typeMap[type] || 'info'
    }
    
    const getTypeText = (type) => {
      const typeMap = {
        'yield': '产量分析',
        'prediction': '预测分析',
        'comparison': '地区对比',
        'comprehensive': '综合评估'
      }
      return typeMap[type] || '未知'
    }
    
    const getStatusTagType = (status) => {
      const statusMap = {
        'completed': 'success',
        'generating': 'warning',
        'failed': 'danger'
      }
      return statusMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'completed': '已完成',
        'generating': '生成中',
        'failed': '失败'
      }
      return statusMap[status] || '未知'
    }
    
    const generateReport = () => {
      if (!reportForm.type || !reportForm.title) {
        ElMessage.warning('请填写完整的报告信息')
        return
      }
      
      generating.value = true
      
      // 模拟报告生成
      setTimeout(() => {
        const newReport = {
          id: Date.now(),
          title: reportForm.title,
          type: reportForm.type,
          region: reportForm.region,
          status: 'generating',
          createdAt: new Date().toLocaleString(),
          fileSize: '-'
        }
        
        reportList.value.unshift(newReport)
        pagination.total++
        
        generating.value = false
        ElMessage.success('报告生成任务已提交')
        resetReportForm()
      }, 1000)
    }
    
    const resetReportForm = () => {
      Object.keys(reportForm).forEach(key => {
        if (key === 'dateRange') {
          reportForm[key] = []
        } else {
          reportForm[key] = ''
        }
      })
    }
    
    const searchReports = () => {
      ElMessage.success('搜索功能开发中...')
    }
    
    const resetSearch = () => {
      searchForm.keyword = ''
      searchForm.type = ''
    }
    
    const viewReport = (report) => {
      currentReport.value = report
      showPreview.value = true
    }
    
    const downloadReport = (report) => {
      if (report.status !== 'completed') {
        ElMessage.warning('报告尚未生成完成')
        return
      }
      ElMessage.success('报告下载功能开发中...')
    }
    
    const deleteReport = (report) => {
      ElMessageBox.confirm('确定要删除这个报告吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = reportList.value.findIndex(r => r.id === report.id)
        if (index > -1) {
          reportList.value.splice(index, 1)
          pagination.total--
          ElMessage.success('报告删除成功')
        }
      })
    }
    
    const handleSizeChange = (size) => {
      pagination.pageSize = size
    }
    
    const handleCurrentChange = (page) => {
      pagination.currentPage = page
    }
    
    onMounted(() => {
      // 初始化数据
    })
    
    return {
      loading,
      generating,
      showPreview,
      currentReport,
      reportForm,
      searchForm,
      reportList,
      pagination,
      getTypeTagType,
      getTypeText,
      getStatusTagType,
      getStatusText,
      generateReport,
      resetReportForm,
      searchReports,
      resetSearch,
      viewReport,
      downloadReport,
      deleteReport,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.reports {
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

.report-generation {
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

.report-list {
  margin-bottom: 30px;
}

.search-bar {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.report-preview {
  min-height: 400px;
}

.preview-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e9ecef;
}

.preview-header h3 {
  color: #333;
  margin-bottom: 8px;
}

.preview-header p {
  color: #666;
  font-size: 14px;
}

.preview-content {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
}

.placeholder-content {
  text-align: center;
  color: #999;
}

.placeholder-content .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.placeholder-text {
  font-size: 12px;
  margin-top: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-bar .el-form {
    flex-direction: column;
  }
  
  .search-bar .el-form-item {
    margin-right: 0;
    margin-bottom: 10px;
  }
  
  .pagination-container {
    text-align: center;
  }
}
</style>
