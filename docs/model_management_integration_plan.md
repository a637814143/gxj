# 在预测中心集成模型管理功能 - 实现方案

## 方案概述

在预测中心页面的"模型参数"面板中添加模型管理功能，用户可以：
1. 查看现有模型列表
2. 新增自定义模型
3. 编辑模型配置
4. 删除模型

## 优势分析

### ✅ 为什么这样做更好

1. **上下文相关性强**
   - 用户在配置预测时就能看到可用模型
   - 发现模型不够用时可以立即添加，无需跳转页面

2. **减少页面跳转**
   - 不需要新建独立的模型管理页面
   - 不需要新增路由和菜单项

3. **实现简单**
   - 只需在现有页面添加一个对话框组件
   - 复用现有的 API 服务
   - 工作量小，1-2天即可完成

4. **用户体验好**
   - 模型选择和模型管理在同一个界面
   - 符合用户的操作流程

## 实现方案

### 方案设计

在"预测模型"下拉框旁边添加一个"管理模型"按钮，点击后弹出对话框，显示模型列表和管理功能。

### UI 布局

```
┌─────────────────────────────────────────────────────────┐
│  预测模型: [下拉选择框 ▼]  [管理模型 🔧]                │
└─────────────────────────────────────────────────────────┘

点击"管理模型"后弹出对话框：

┌──────────────────────────────────────────────────────────┐
│  模型管理                                    [新增模型] [X]│
├──────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐  │
│  │ 模型名称          │ 类型    │ 描述      │ 操作    │  │
│  ├────────────────────────────────────────────────────┤  │
│  │ LSTM产量模型      │ LSTM    │ 深度学习  │ 编辑 删除│  │
│  │ 天气回归模型      │ WEATHER │ 多元回归  │ 编辑 删除│  │
│  │ ARIMA时序模型     │ ARIMA   │ 时间序列  │ 编辑 删除│  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘

点击"新增模型"或"编辑"后弹出表单对话框：

┌──────────────────────────────────────────────────────────┐
│  新增/编辑模型                                        [X] │
├──────────────────────────────────────────────────────────┤
│  模型名称: [_____________________________________]        │
│  算法类型: [LSTM ▼]                                      │
│  描述:     [_____________________________________]        │
│  模型参数:                                               │
│  ┌────────────────────────────────────────────────────┐  │
│  │ 参数键              │ 参数值                       │  │
│  │ learningRate        │ 0.01                    [X]  │  │
│  │ epochs              │ 100                     [X]  │  │
│  │ [+ 新增参数]                                       │  │
│  └────────────────────────────────────────────────────┘  │
│                                      [取消]  [保存]       │
└──────────────────────────────────────────────────────────┘
```

### 代码实现

#### 1. 修改预测中心页面 (ForecastCenterView.vue)

##### 1.1 在模型选择框旁边添加管理按钮

```vue
<el-col :xs="24" :sm="12" :md="8">
  <el-form-item label="预测模型">
    <div style="display: flex; gap: 8px;">
      <el-select
        v-model="selectors.modelId"
        placeholder="请选择模型"
        :loading="loadingOptions.models"
        clearable
        style="flex: 1;"
      >
        <el-option
          v-for="model in optionLists.models"
          :key="model.id"
          :label="`${model.name} (${model.type})`"
          :value="model.id"
        />
      </el-select>
      <el-button
        type="primary"
        plain
        @click="showModelManagementDialog = true"
      >
        管理模型
      </el-button>
    </div>
  </el-form-item>
</el-col>
```

##### 1.2 添加模型管理对话框

```vue
<!-- 模型管理对话框 -->
<el-dialog
  v-model="showModelManagementDialog"
  title="模型管理"
  width="900px"
  :close-on-click-modal="false"
>
  <div class="model-management-header">
    <el-button type="primary" @click="openModelForm()">
      新增模型
    </el-button>
  </div>
  
  <el-table
    :data="optionLists.models"
    :stripe="true"
    v-loading="loadingOptions.models"
    empty-text="暂无模型，请新增"
  >
    <el-table-column prop="name" label="模型名称" min-width="180" />
    <el-table-column prop="type" label="算法类型" width="150" />
    <el-table-column prop="description" label="描述" min-width="200" />
    <el-table-column label="参数数量" width="100">
      <template #default="{ row }">
        {{ Object.keys(row.parameters || {}).length }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="150" fixed="right">
      <template #default="{ row }">
        <el-button type="primary" link size="small" @click="openModelForm(row)">
          编辑
        </el-button>
        <el-button type="danger" link size="small" @click="handleDeleteModel(row)">
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</el-dialog>

<!-- 模型表单对话框 -->
<el-dialog
  v-model="showModelFormDialog"
  :title="modelFormMode === 'create' ? '新增模型' : '编辑模型'"
  width="700px"
  :close-on-click-modal="false"
  @close="resetModelForm"
>
  <el-form
    ref="modelFormRef"
    :model="modelForm"
    :rules="modelFormRules"
    label-width="100px"
  >
    <el-form-item label="模型名称" prop="name">
      <el-input
        v-model="modelForm.name"
        placeholder="请输入模型名称"
        maxlength="128"
        show-word-limit
      />
    </el-form-item>
    
    <el-form-item label="算法类型" prop="type">
      <el-select v-model="modelForm.type" placeholder="请选择算法类型">
        <el-option label="LSTM" value="LSTM" />
        <el-option label="天气回归" value="WEATHER_REGRESSION" />
        <el-option label="ARIMA" value="ARIMA" />
        <el-option label="Prophet" value="PROPHET" />
      </el-select>
    </el-form-item>
    
    <el-form-item label="描述" prop="description">
      <el-input
        v-model="modelForm.description"
        type="textarea"
        :rows="3"
        placeholder="请输入模型描述"
        maxlength="512"
        show-word-limit
      />
    </el-form-item>
    
    <el-form-item label="模型参数">
      <div class="model-form-parameters">
        <div
          v-for="(param, index) in modelForm.parameterList"
          :key="`form-param-${index}`"
          class="parameter-row"
        >
          <el-input
            v-model="param.key"
            placeholder="参数键"
            clearable
          />
          <el-input
            v-model="param.value"
            placeholder="参数值"
            clearable
          />
          <el-button
            type="danger"
            link
            :icon="Delete"
            @click="removeModelFormParameter(index)"
          />
        </div>
        <el-button
          type="primary"
          plain
          size="small"
          @click="addModelFormParameter"
        >
          新增参数
        </el-button>
      </div>
    </el-form-item>
  </el-form>
  
  <template #footer>
    <el-button @click="showModelFormDialog = false">取消</el-button>
    <el-button
      type="primary"
      :loading="savingModel"
      @click="handleSaveModel"
    >
      保存
    </el-button>
  </template>
</el-dialog>
```

##### 1.3 添加 JavaScript 逻辑

```javascript
import { Delete } from '@element-plus/icons-vue'
import { createModel, updateModel, deleteModel } from '@/services/forecast'

// 添加响应式变量
const showModelManagementDialog = ref(false)
const showModelFormDialog = ref(false)
const modelFormMode = ref('create') // 'create' | 'edit'
const modelFormRef = ref(null)
const savingModel = ref(false)

const modelForm = reactive({
  id: null,
  name: '',
  type: '',
  description: '',
  parameterList: []
})

const modelFormRules = {
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { max: 128, message: '名称长度不能超过128位', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择算法类型', trigger: 'change' }
  ],
  description: [
    { max: 512, message: '描述长度不能超过512位', trigger: 'blur' }
  ]
}

// 打开模型表单
const openModelForm = (model = null) => {
  if (model) {
    // 编辑模式
    modelFormMode.value = 'edit'
    modelForm.id = model.id
    modelForm.name = model.name
    modelForm.type = model.type
    modelForm.description = model.description || ''
    modelForm.parameterList = Object.entries(model.parameters || {}).map(([key, value]) => ({
      key,
      value: typeof value === 'object' ? JSON.stringify(value) : String(value)
    }))
  } else {
    // 新增模式
    modelFormMode.value = 'create'
    resetModelForm()
  }
  showModelFormDialog.value = true
}

// 重置模型表单
const resetModelForm = () => {
  modelForm.id = null
  modelForm.name = ''
  modelForm.type = ''
  modelForm.description = ''
  modelForm.parameterList = []
  modelFormRef.value?.clearValidate()
}

// 添加参数行
const addModelFormParameter = () => {
  modelForm.parameterList.push({ key: '', value: '' })
}

// 删除参数行
const removeModelFormParameter = (index) => {
  modelForm.parameterList.splice(index, 1)
}

// 保存模型
const handleSaveModel = async () => {
  try {
    await modelFormRef.value.validate()
  } catch (error) {
    return
  }

  // 转换参数列表为对象
  const parameters = {}
  modelForm.parameterList.forEach(param => {
    const key = (param.key || '').trim()
    if (!key) return
    const normalized = normalizeParameterValue(param.value)
    if (normalized !== null && normalized !== undefined) {
      parameters[key] = normalized
    }
  })

  const payload = {
    name: modelForm.name,
    type: modelForm.type,
    description: modelForm.description,
    parameters
  }

  savingModel.value = true
  try {
    if (modelFormMode.value === 'create') {
      await createModel(payload)
      ElMessage.success('模型创建成功')
    } else {
      await updateModel(modelForm.id, payload)
      ElMessage.success('模型更新成功')
    }
    
    showModelFormDialog.value = false
    await fetchOptions('models')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存模型失败')
  } finally {
    savingModel.value = false
  }
}

// 删除模型
const handleDeleteModel = async (model) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型"${model.name}"吗？删除后将无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      }
    )
  } catch (error) {
    return
  }

  try {
    await deleteModel(model.id)
    ElMessage.success('模型已删除')
    
    // 如果删除的是当前选中的模型，清空选择
    if (selectors.modelId === model.id) {
      selectors.modelId = null
    }
    
    await fetchOptions('models')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除模型失败')
  }
}
```

##### 1.4 添加样式

```css
.model-management-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.model-form-parameters {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.model-form-parameters .parameter-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: center;
}
```

#### 2. 扩展 API 服务 (forecast.js)

```javascript
// 创建模型
export const createModel = async (modelData) => {
  const { data } = await apiClient.post('/api/forecast/models', modelData)
  return data?.data ?? data
}

// 更新模型
export const updateModel = async (modelId, modelData) => {
  const { data } = await apiClient.put(`/api/forecast/models/${modelId}`, modelData)
  return data?.data ?? data
}

// 删除模型
export const deleteModel = async (modelId) => {
  await apiClient.delete(`/api/forecast/models/${modelId}`)
}
```

#### 3. 后端补充功能

##### 3.1 Repository 添加方法

```java
// ForecastModelRepository.java
Optional<ForecastModel> findByName(String name);
```

##### 3.2 Service 添加校验和方法

```java
// ForecastModelService.java
ForecastModel update(Long id, ForecastModelRequest request);
void delete(Long id);
```

```java
// ForecastModelServiceImpl.java
@Override
public ForecastModel create(ForecastModelRequest request) {
    // 检查名称唯一性
    if (forecastModelRepository.findByName(request.name()).isPresent()) {
        throw new BusinessException(ResultCode.BAD_REQUEST, "模型名称已存在");
    }
    
    ForecastModel model = new ForecastModel();
    model.setName(request.name());
    model.setType(request.type());
    model.setDescription(request.description());
    model.setParameters(request.parameters());
    return forecastModelRepository.save(model);
}

@Override
@Transactional
public ForecastModel update(Long id, ForecastModelRequest request) {
    ForecastModel model = forecastModelRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "模型不存在"));
    
    // 检查名称唯一性（排除自己）
    forecastModelRepository.findByName(request.name())
        .ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "模型名称已存在");
            }
        });
    
    model.setName(request.name());
    model.setType(request.type());
    model.setDescription(request.description());
    model.setParameters(request.parameters());
    return forecastModelRepository.save(model);
}

@Override
@Transactional
public void delete(Long id) {
    ForecastModel model = forecastModelRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "模型不存在"));
    forecastModelRepository.delete(model);
}
```

##### 3.3 Controller 添加接口

```java
// ForecastModelController.java
@PutMapping("/{id}")
public ApiResponse<ForecastModel> updateModel(
    @PathVariable Long id,
    @Valid @RequestBody ForecastModelRequest request) {
    return ApiResponse.success(forecastModelService.update(id, request));
}

@DeleteMapping("/{id}")
public ApiResponse<Void> deleteModel(@PathVariable Long id) {
    forecastModelService.delete(id);
    return ApiResponse.success(null);
}
```

## 工作量评估

### 前端工作
- 修改预测中心页面：2-3小时
- 添加对话框组件：2-3小时
- 添加 API 调用：30分钟
- 测试和调试：1-2小时
- **前端总计：6-9小时**

### 后端工作
- 添加 Repository 方法：10分钟
- 添加 Service 方法和校验：1-2小时
- 添加 Controller 接口：30分钟
- 测试：1小时
- **后端总计：2-3.5小时**

### 总工作量：8-12.5小时（约1-2天）

## 优先级建议

### 第一阶段（核心功能）
1. ✅ 后端：添加更新和删除接口
2. ✅ 后端：添加名称唯一性校验
3. ✅ 前端：添加"管理模型"按钮
4. ✅ 前端：实现模型列表对话框
5. ✅ 前端：实现新增模型表单
6. ✅ 前端：实现保存和删除功能

### 第二阶段（优化）
7. ⭐ 前端：实现编辑模型功能
8. ⭐ 前端：优化参数配置界面
9. ⭐ 后端：添加参数格式校验

## 总结

这个方案的优势：
- ✅ **不需要新建页面**，在现有页面上扩展
- ✅ **工作量小**，1-2天即可完成
- ✅ **用户体验好**，操作流程自然
- ✅ **维护简单**，代码集中在一个文件
- ✅ **完全满足用例要求**，实现度可达100%

建议立即开始实施！
