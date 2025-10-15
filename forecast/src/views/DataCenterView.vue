<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据集管理</span>
          <div class="card-actions">
            <el-button type="primary" @click="openUpload">上传数据</el-button>
            <el-button @click="fetchDatasets" :loading="datasetLoading">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="datasets" v-loading="datasetLoading" empty-text="暂无导入记录">
        <el-table-column prop="name" label="数据集名称" min-width="200" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ datasetTypeMap[row.type] ?? row.type ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>导入数据记录</span>
          <el-button @click="fetchYieldRecords" :loading="yieldLoading">刷新</el-button>
        </div>
      </template>
      <el-table :data="yieldRecords" v-loading="yieldLoading" empty-text="暂未导入数据">
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="regionName" label="地区" min-width="140" />
        <el-table-column prop="cropName" label="作物" min-width="140" />
        <el-table-column prop="production" label="总产量 (吨)" width="140">
          <template #default="{ row }">
            {{ formatNumber(row.production) }}
          </template>
        </el-table-column>
        <el-table-column prop="yieldPerHectare" label="单产 (吨/公顷)" width="160">
          <template #default="{ row }">
            {{ formatNumber(row.yieldPerHectare) }}
          </template>
        </el-table-column>
        <el-table-column prop="averagePrice" label="平均价格 (元/公斤)" width="180">
          <template #default="{ row }">
            {{ formatNumber(row.averagePrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="dataSource" label="数据来源" min-width="160" show-overflow-tooltip />
      </el-table>
      <div v-if="importWarnings.length" class="warnings">
        <el-alert
          v-for="(warning, index) in importWarnings"
          :key="`${index}-${warning}`"
          type="warning"
          :title="warning"
          show-icon
          :closable="false"
        />
      </div>
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="导入数据文件" width="520px" @closed="resetUploadForm">
      <el-form label-width="88px" class="upload-form">
        <el-form-item label="数据类型">
          <el-select v-model="uploadForm.type">
            <el-option v-for="item in datasetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集名称">
          <el-input v-model="uploadForm.name" placeholder="默认使用文件名" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            placeholder="可选，最长 256 字"
            :rows="3"
            maxlength="256"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            class="upload-block"
            drag
            :auto-upload="false"
            :file-list="uploadFileList"
            accept=".xls,.xlsx"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">仅支持 Excel 文件（.xls/.xlsx）</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false" :disabled="uploading">取 消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading" :disabled="uploadFileList.length === 0">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const datasets = ref([])
const datasetLoading = ref(false)
const yieldRecords = ref([])
const yieldLoading = ref(false)
const importWarnings = ref([])

const datasetTypeMap = {
  YIELD: '产量',
  PRICE: '价格',
  WEATHER: '气象',
  SOIL: '土壤'
}

const datasetTypeOptions = [
  { value: 'YIELD', label: '产量' },
  { value: 'PRICE', label: '价格' },
  { value: 'WEATHER', label: '气象' },
  { value: 'SOIL', label: '土壤' }
]

const uploadDialogVisible = ref(false)
const uploadFileList = ref([])
const uploading = ref(false)
const uploadForm = reactive({
  type: 'YIELD',
  name: '',
  description: ''
})

const formatDate = value => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatNumber = value => {
  if (value === null || value === undefined) {
    return '-'
  }
  const number = Number(value)
  if (Number.isNaN(number)) {
    return value
  }
  return number.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

const fetchDatasets = async () => {
  datasetLoading.value = true
  try {
    const { data } = await axios.get(`${API_BASE}/api/datasets/files`)
    datasets.value = data?.data ?? []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载数据集列表失败')
  } finally {
    datasetLoading.value = false
  }
}

const fetchYieldRecords = async () => {
  yieldLoading.value = true
  try {
    const { data } = await axios.get(`${API_BASE}/api/yields`)
    yieldRecords.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载导入数据失败')
  } finally {
    yieldLoading.value = false
  }
}

const beforeUpload = file => {
  const lowerName = file.name?.toLowerCase?.() ?? ''
  const isExcel = lowerName.endsWith('.xls') || lowerName.endsWith('.xlsx') || (file.type ?? '').includes('sheet')
  if (!isExcel) {
    ElMessage.error('仅支持 Excel 文件（.xls/.xlsx）')
  }
  return isExcel
}

const handleFileChange = (file, fileList) => {
  const target = file.raw ?? file
  if (!beforeUpload(target)) {
    uploadFileList.value = []
    return
  }
  uploadFileList.value = fileList.slice(-1)
}

const handleFileRemove = file => {
  uploadFileList.value = uploadFileList.value.filter(item => item.uid !== file.uid)
}

const submitUpload = async () => {
  if (!uploadFileList.value.length) {
    ElMessage.warning('请选择要导入的文件')
    return
  }
  const [fileItem] = uploadFileList.value
  const formData = new FormData()
  formData.append('file', fileItem.raw)
  formData.append('type', uploadForm.type)
  if (uploadForm.name.trim()) {
    formData.append('name', uploadForm.name.trim())
  }
  if (uploadForm.description.trim()) {
    formData.append('description', uploadForm.description.trim())
  }

  uploading.value = true
  try {
    const { data } = await axios.post(`${API_BASE}/api/data-import/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const result = data?.data
    importWarnings.value = result?.warnings ?? []
    ElMessage.success(`导入完成，新增 ${result?.insertedRows ?? 0} 条，更新 ${result?.updatedRows ?? 0} 条`) 
    uploadDialogVisible.value = false
    await Promise.all([fetchDatasets(), fetchYieldRecords()])
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '导入失败'
    ElMessage.error(message)
  } finally {
    uploading.value = false
  }
}

const openUpload = () => {
  uploadDialogVisible.value = true
  importWarnings.value = []
}

const resetUploadForm = () => {
  uploadForm.type = 'YIELD'
  uploadForm.name = ''
  uploadForm.description = ''
  uploadFileList.value = []
}

onMounted(async () => {
  await Promise.all([fetchDatasets(), fetchYieldRecords()])
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.upload-form {
  padding-right: 8px;
}

.upload-block {
  width: 100%;
}

.upload-icon {
  font-size: 32px;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.warnings {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}
</style>
