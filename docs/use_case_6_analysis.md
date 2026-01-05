# 用例6：配置预测模型 - 实现情况分析

## 用例概述

**用例名：** 配置预测模型  
**范围：** 农作物产量预测与可视化分析系统  
**级别：** 系统用户层（农业部门端）  
**主要参与者：** 农业部门专员（Agriculture Dept Staff）

---

## 用例要求

### 主事件流
1. 专员进入预测中心的模型管理页面，查看现有模型列表
2. 点击新增模型
3. 系统弹出配置表单，填写模型名称，选择算法类型（ARIMA、Prophet、LSTM等），设置参数
4. 专员提交配置
5. 系统对名称、参数和算法可用性进行校验，校验通过后保存配置
6. 保存成功后，新模型出现在模型列表中，可用于后续预测任务选择

### 备用事件流
1. 模型名称重复时，系统提示错误信息，专员修改后可再次提交
2. 参数不符合规则时，系统给出具体校验提示，专员修正后重新提交
3. 所选算法不可用时，系统提示该类型不可用，专员需更换算法或取消操作

---

## 实现情况分析

### ✅ 已实现部分

#### 1. 后端实现（完整）

##### 实体类 (ForecastModel.java)
```java
@Entity
@Table(name = "forecast_model")
public class ForecastModel extends BaseEntity {
    public enum ModelType {
        LSTM,
        WEATHER_REGRESSION,
        ARIMA,
        PROPHET
    }
    
    private String name;              // 模型名称
    private ModelType type;           // 算法类型
    private String description;       // 描述
    private Map<String, Object> parameters;  // 参数（JSON格式）
}
```

##### DTO (ForecastModelRequest.java)
```java
public record ForecastModelRequest(
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128位")
    String name,

    @NotNull(message = "模型类型不能为空")
    ModelType type,

    @Size(max = 512, message = "描述长度不能超过512位")
    String description,

    @NotNull(message = "模型参数不能为空")
    Map<String, Object> parameters
)
```
**✅ 包含完整的参数校验**

##### 服务层 (ForecastModelService.java)
```java
public interface ForecastModelService {
    List<ForecastModel> listAll();      // 查询所有模型
    ForecastModel create(ForecastModelRequest request);  // 创建模型
}
```

##### 控制器 (ForecastModelController.java)
```java
@RestController
@RequestMapping("/api/forecast/models")
public class ForecastModelController {
    
    @GetMapping
    public ApiResponse<List<ForecastModel>> listModels() {
        return ApiResponse.success(forecastModelService.listAll());
    }

    @PostMapping
    public ApiResponse<ForecastModel> createModel(@Valid @RequestBody ForecastModelRequest request) {
        return ApiResponse.success(forecastModelService.create(request));
    }
}
```
**✅ 提供了查询和创建接口**

##### 数据初始化 (ForecastModelDataInitializer.java)
- 系统启动时自动初始化4个预设模型
- 支持模型更新和插入

---

#### 2. 前端实现（部分）

##### API服务 (forecast.js)
```javascript
export const fetchModels = async () => {
  const { data } = await apiClient.get('/api/forecast/models')
  return extractData(data)
}
```
**✅ 已实现查询模型列表的API调用**

---

### ❌ 未实现部分

#### 1. 前端缺失的功能

##### ❌ 缺少模型管理页面
- **问题：** 没有独立的模型管理页面（ModelManagementView.vue）
- **影响：** 用户无法通过界面查看、新增、编辑模型
- **路由缺失：** router/index.js 中没有模型管理相关的路由

##### ❌ 缺少创建模型的API调用
- **问题：** forecast.js 中只有 `fetchModels()`，没有 `createModel()` 函数
- **影响：** 前端无法调用后端的创建模型接口

##### ❌ 缺少模型配置表单组件
- **问题：** 没有模型配置表单（ModelConfigDialog.vue 或类似组件）
- **影响：** 用户无法填写模型名称、选择算法类型、设置参数

##### ❌ 缺少模型列表展示
- **问题：** 没有模型列表表格或卡片展示
- **影响：** 用户无法查看现有模型

---

#### 2. 后端缺失的功能

##### ❌ 缺少模型名称唯一性校验
- **问题：** `ForecastModelServiceImpl.create()` 没有检查模型名称是否重复
- **影响：** 可能创建重名模型，不符合备用事件流要求1
- **建议：** 在 Repository 中添加 `findByName()` 方法，在创建前检查

##### ❌ 缺少参数格式校验
- **问题：** 虽然参数不能为空，但没有校验参数的具体格式和内容
- **影响：** 可能保存无效的参数配置，不符合备用事件流要求2
- **建议：** 根据不同的 ModelType 校验对应的参数结构

##### ❌ 缺少算法可用性校验
- **问题：** 没有检查所选算法类型是否可用
- **影响：** 不符合备用事件流要求3
- **建议：** 添加算法可用性检查逻辑

##### ❌ 缺少更新和删除功能
- **问题：** 只有创建功能，没有更新（PUT）和删除（DELETE）接口
- **影响：** 用户无法修改或删除已创建的模型
- **建议：** 添加 `update()` 和 `delete()` 方法

---

## 实现完整度评估

### 后端实现度：60%
- ✅ 数据模型设计完整
- ✅ 基本的查询和创建接口
- ✅ 参数校验（基础）
- ❌ 缺少业务校验（名称唯一性、参数格式、算法可用性）
- ❌ 缺少更新和删除功能

### 前端实现度：10%
- ✅ 查询模型列表的API调用
- ❌ 缺少模型管理页面
- ❌ 缺少创建模型的API调用
- ❌ 缺少模型配置表单
- ❌ 缺少模型列表展示
- ❌ 缺少路由配置

### 总体实现度：35%

---

## 需要补充的功能清单

### 高优先级（核心功能）

#### 1. 前端 - 模型管理页面
- [ ] 创建 `ModelManagementView.vue` 页面
- [ ] 添加路由配置 `/models`
- [ ] 实现模型列表表格展示
- [ ] 添加"新增模型"按钮

#### 2. 前端 - 模型配置表单
- [ ] 创建 `ModelConfigDialog.vue` 组件
- [ ] 实现表单字段：
  - 模型名称（必填，最大128字符）
  - 算法类型（下拉选择：LSTM、WEATHER_REGRESSION、ARIMA、PROPHET）
  - 描述（可选，最大512字符）
  - 参数配置（JSON编辑器或动态表单）
- [ ] 实现表单校验
- [ ] 实现提交和取消功能

#### 3. 前端 - API服务扩展
在 `forecast.js` 中添加：
```javascript
export const createModel = async (modelData) => {
  const { data } = await apiClient.post('/api/forecast/models', modelData)
  return data?.data ?? data
}

export const updateModel = async (modelId, modelData) => {
  const { data } = await apiClient.put(`/api/forecast/models/${modelId}`, modelData)
  return data?.data ?? data
}

export const deleteModel = async (modelId) => {
  await apiClient.delete(`/api/forecast/models/${modelId}`)
}
```

#### 4. 后端 - 业务校验增强
在 `ForecastModelServiceImpl.java` 中添加：
```java
@Override
public ForecastModel create(ForecastModelRequest request) {
    // 1. 检查名称唯一性
    if (forecastModelRepository.findByName(request.name()).isPresent()) {
        throw new BusinessException(ResultCode.BAD_REQUEST, "模型名称已存在");
    }
    
    // 2. 校验参数格式
    validateParameters(request.type(), request.parameters());
    
    // 3. 检查算法可用性
    if (!isAlgorithmAvailable(request.type())) {
        throw new BusinessException(ResultCode.BAD_REQUEST, "所选算法类型不可用");
    }
    
    ForecastModel model = new ForecastModel();
    model.setName(request.name());
    model.setType(request.type());
    model.setDescription(request.description());
    model.setParameters(request.parameters());
    return forecastModelRepository.save(model);
}

private void validateParameters(ModelType type, Map<String, Object> parameters) {
    // 根据不同的模型类型校验参数
    switch (type) {
        case LSTM -> validateLSTMParameters(parameters);
        case ARIMA -> validateARIMAParameters(parameters);
        case PROPHET -> validateProphetParameters(parameters);
        case WEATHER_REGRESSION -> validateRegressionParameters(parameters);
    }
}

private boolean isAlgorithmAvailable(ModelType type) {
    // 检查算法是否可用（可以从配置文件读取）
    return true; // 简化实现
}
```

#### 5. 后端 - Repository扩展
在 `ForecastModelRepository.java` 中添加：
```java
Optional<ForecastModel> findByName(String name);
```

#### 6. 后端 - 更新和删除接口
在 `ForecastModelController.java` 中添加：
```java
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

---

### 中优先级（增强功能）

#### 7. 前端 - 模型编辑功能
- [ ] 在模型列表中添加"编辑"按钮
- [ ] 复用 `ModelConfigDialog.vue` 组件
- [ ] 实现编辑模式的数据回填

#### 8. 前端 - 模型删除功能
- [ ] 在模型列表中添加"删除"按钮
- [ ] 实现删除确认对话框
- [ ] 调用删除API

#### 9. 前端 - 参数配置优化
- [ ] 根据不同算法类型显示不同的参数表单
- [ ] 提供参数说明和示例
- [ ] 实现参数预设模板

#### 10. 后端 - 模型使用统计
- [ ] 统计每个模型被使用的次数
- [ ] 在模型列表中显示使用次数
- [ ] 防止删除正在使用的模型

---

### 低优先级（辅助功能）

#### 11. 前端 - 模型搜索和筛选
- [ ] 按名称搜索
- [ ] 按算法类型筛选
- [ ] 按创建时间排序

#### 12. 前端 - 模型导入导出
- [ ] 导出模型配置为JSON
- [ ] 从JSON导入模型配置

#### 13. 后端 - 模型版本管理
- [ ] 支持同一模型的多个版本
- [ ] 版本切换和回滚

---

## 建议的实现顺序

### 第一阶段：核心功能（满足基本用例要求）
1. 后端：添加名称唯一性校验
2. 后端：添加 `findByName()` Repository方法
3. 前端：创建模型管理页面和路由
4. 前端：实现模型列表展示
5. 前端：创建模型配置表单组件
6. 前端：添加 `createModel()` API调用
7. 前端：集成表单和列表，实现完整的创建流程

### 第二阶段：完善功能（满足备用事件流）
8. 后端：添加参数格式校验
9. 后端：添加算法可用性校验
10. 前端：实现错误提示和处理

### 第三阶段：扩展功能（提升用户体验）
11. 后端：添加更新和删除接口
12. 前端：实现编辑和删除功能
13. 前端：优化参数配置界面

---

## 结论

**当前状态：** 用例6"配置预测模型"的实现度约为 **35%**

**主要问题：**
1. 前端几乎完全缺失（只有查询API调用）
2. 后端缺少关键的业务校验逻辑
3. 没有更新和删除功能

**建议：**
- 优先实现第一阶段的核心功能，使用例能够基本运行
- 然后补充第二阶段的校验逻辑，满足备用事件流要求
- 最后根据实际需求实现第三阶段的扩展功能

**预计工作量：**
- 第一阶段：2-3天（前端为主）
- 第二阶段：1天（后端校验）
- 第三阶段：1-2天（完善功能）
- 总计：4-6天
