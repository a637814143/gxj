<!--
  @component DataEditDialog
  @description 数据编辑对话框 - 编辑单条产量数据
  @emits save - 保存数据
  @emits close - 关闭对话框
-->
<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑数据' : '新增数据'"
    width="600px"
    @close="$emit('close')"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="140px"
    >
      <el-form-item label="年份" prop="year">
        <el-input-number
          v-model="formData.year"
          :min="1900"
          :max="2100"
          :step="1"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="作物名称" prop="cropName">
        <el-input v-model="formData.cropName" placeholder="请输入作物名称" />
      </el-form-item>
      
      <el-form-item label="作物类别" prop="cropCategory">
        <el-select v-model="formData.cropCategory" placeholder="请选择作物类别" style="width: 100%">
          <el-option label="粮食作物" value="粮食作物" />
          <el-option label="经济作物" value="经济作物" />
          <el-option label="蔬菜" value="蔬菜" />
          <el-option label="水果" value="水果" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="地区名称" prop="regionName">
        <el-input v-model="formData.regionName" placeholder="请输入地区名称" />
      </el-form-item>
      
      <el-form-item label="产量(万吨)" prop="production">
        <el-input-number
          v-model="formData.production"
          :min="0"
          :precision="2"
          :step="0.01"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="播种面积(千公顷)">
        <el-input-number
          v-model="formData.sownArea"
          :min="0"
          :precision="2"
          :step="0.01"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="平均价格(元/吨)">
        <el-input-number
          v-model="formData.averagePrice"
          :min="0"
          :precision="0"
          :step="100"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="预估收益(万元)">
        <el-input-number
          v-model="formData.estimatedRevenue"
          :min="0"
          :precision="0"
          :step="1000"
          style="width: 100%"
        />
        <div class="form-hint">
          可根据产量和价格自动计算：{{ calculatedRevenue }} 万元
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: { type: Boolean, required: true },
  data: { type: Object, default: null },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['save', 'close'])

const formRef = ref(null)

const defaultFormData = {
  year: new Date().getFullYear(),
  cropName: '',
  cropCategory: '',
  regionName: '',
  production: null,
  sownArea: null,
  averagePrice: null,
  estimatedRevenue: null
}

const formData = ref({ ...defaultFormData })

const isEdit = computed(() => !!props.data?.id)

const calculatedRevenue = computed(() => {
  if (!formData.value.production || !formData.value.averagePrice) {
    return '0.00'
  }
  // 产量(万吨) * 价格(元/吨) / 10000 = 收益(万元)
  const revenue = formData.value.production * formData.value.averagePrice / 10000
  return revenue.toFixed(2)
})

const rules = {
  year: [
    { required: true, message: '请输入年份', trigger: 'blur' },
    { type: 'number', min: 1900, max: 2100, message: '年份范围：1900-2100', trigger: 'blur' }
  ],
  cropName: [
    { required: true, message: '请输入作物名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  cropCategory: [
    { required: true, message: '请选择作物类别', trigger: 'change' }
  ],
  regionName: [
    { required: true, message: '请输入地区名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  production: [
    { required: true, message: '请输入产量', trigger: 'blur' },
    { type: 'number', min: 0, message: '产量必须大于等于0', trigger: 'blur' }
  ]
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    emit('save', { ...formData.value })
  } catch (error) {
    ElMessage.warning('请检查表单填写是否正确')
  }
}

watch(() => props.visible, (visible) => {
  if (visible) {
    if (props.data) {
      formData.value = { ...props.data }
    } else {
      formData.value = { ...defaultFormData }
    }
    formRef.value?.clearValidate()
  }
})
</script>

<style scoped>
.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
