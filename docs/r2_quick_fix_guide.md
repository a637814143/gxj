# R²值快速改进指南

## 当前问题
- R² = 0.17（很差）
- 模型: 天气因子多元回归 (WEATHER_REGRESSION)
- 作物: 小麦
- 地区: 湖江市

## 快速诊断清单

### 1. 检查数据质量
```sql
-- 检查历史数据量
SELECT 
    crop_id,
    region_id,
    COUNT(*) as data_points,
    MIN(year) as start_year,
    MAX(year) as end_year
FROM yield_records
WHERE crop_id = 55 AND region_id = 88
GROUP BY crop_id, region_id;

-- 检查是否有异常值
SELECT year, yield_value
FROM yield_records
WHERE crop_id = 55 AND region_id = 88
ORDER BY yield_value DESC;

-- 检查缺失值
SELECT 
    COUNT(*) as total,
    SUM(CASE WHEN yield_value IS NULL THEN 1 ELSE 0 END) as missing
FROM yield_records
WHERE crop_id = 55 AND region_id = 88;
```

### 2. 检查气象数据
```sql
-- 检查气象特征是否完整
SELECT 
    year,
    temperature,
    precipitation,
    sunshine_hours
FROM weather_data
WHERE region_id = 88
ORDER BY year DESC;

-- 检查气象数据缺失情况
SELECT 
    COUNT(*) as total,
    SUM(CASE WHEN temperature IS NULL THEN 1 ELSE 0 END) as missing_temp,
    SUM(CASE WHEN precipitation IS NULL THEN 1 ELSE 0 END) as missing_precip
FROM weather_data
WHERE region_id = 88;
```

## 立即可实施的改进

### 改进1: 增加历史数据年限 ⭐⭐⭐⭐⭐

**问题**: 15年数据可能不足
**解决**: 
1. 收集更多历史数据（至少20-30年）
2. 如果无法获取，考虑使用相邻地区数据补充

**SQL示例**:
```sql
-- 导入更多历史数据
INSERT INTO yield_records (crop_id, region_id, year, yield_value)
VALUES 
    (55, 88, 2005, 4500),
    (55, 88, 2006, 4650),
    (55, 88, 2007, 4800);
    -- ... 更多数据
```

### 改进2: 添加更多气象特征 ⭐⭐⭐⭐⭐

**当前可能只有**: 温度、降水
**建议添加**:
- 日照时数
- 湿度
- 风速
- 土壤湿度
- 生长季节累积温度

**前端修改**: 在预测表单中添加更多气象输入字段

```vue
<!-- forecast/src/views/ForecastView.vue -->
<el-form-item label="日照时数(小时)">
  <el-input-number v-model="weatherData.sunshine_hours" :min="0" />
</el-form-item>
<el-form-item label="相对湿度(%)">
  <el-input-number v-model="weatherData.humidity" :min="0" :max="100" />
</el-form-item>
<el-form-item label="平均风速(m/s)">
  <el-input-number v-model="weatherData.wind_speed" :min="0" />
</el-form-item>
```

### 改进3: 数据标准化 ⭐⭐⭐⭐

**问题**: 不同特征量纲差异大（温度0-40°C，降水0-2000mm）
**解决**: 已在代码中实现，确保启用

检查 `LocalForecastEngine.java` 中的标准化代码：
```java
// 应该有类似这样的代码
double std = fit.featureStds.getOrDefault(key, 1d);
double normalized = std == 0 ? 0 : (projected - fit.featureMeans.getOrDefault(key, 0d)) / std;
```

### 改进4: 添加滞后特征 ⭐⭐⭐⭐

**原理**: 今年产量往往与去年产量相关

**实施步骤**:

1. 修改数据准备逻辑，添加滞后特征：

```java
// 在构建历史数据时添加
for (int i = 1; i < historyPoints.size(); i++) {
    ForecastEngineRequest.HistoryPoint current = historyPoints.get(i);
    ForecastEngineRequest.HistoryPoint previous = historyPoints.get(i - 1);
    
    // 添加前一年产量作为特征
    Map<String, Double> enhancedFeatures = new HashMap<>(current.features());
    enhancedFeatures.put("lag1_yield", previous.value());
    
    // 如果有更早的数据，添加2年前的产量
    if (i >= 2) {
        enhancedFeatures.put("lag2_yield", historyPoints.get(i - 2).value());
    }
    
    // 3年移动平均
    if (i >= 2) {
        double avg3 = (previous.value() + 
                      historyPoints.get(i - 2).value() + 
                      current.value()) / 3.0;
        enhancedFeatures.put("moving_avg_3", avg3);
    }
}
```

### 改进5: 特征交互项 ⭐⭐⭐

**原理**: 温度和降水的组合效应

```java
// 在特征构建时添加交互项
Map<String, Double> features = new HashMap<>();
features.put("temperature", temperature);
features.put("precipitation", precipitation);

// 交互项
features.put("temp_precip", temperature * precipitation);
features.put("temp_squared", temperature * temperature);
features.put("precip_squared", precipitation * precipitation);

// 干旱指数
if (precipitation > 0) {
    features.put("drought_index", temperature / precipitation);
}
```

### 改进6: 尝试不同模型 ⭐⭐⭐⭐

**当前**: WEATHER_REGRESSION (R²=0.17)
**建议尝试**:

1. **LSTM模型**:
```javascript
// 前端选择LSTM
modelCode: 'LSTM'
```

2. **ARIMA模型**:
```javascript
// 前端选择ARIMA
modelCode: 'ARIMA'
```

3. **集成模型** (自动选择最佳):
```javascript
// 前端不指定模型，让系统自动选择
modelCode: null  // 或不传
```

### 改进7: 调整模型参数 ⭐⭐⭐

**LSTM参数优化**:
```javascript
// 前端传递优化参数
parameters: {
  epochs: 150,           // 增加训练轮数
  hiddenSize: 128,       // 增加隐藏层大小
  learningRate: 0.0005,  // 降低学习率
  lookbackWindow: 12,    // 增加回看窗口
  dropout: 0.2           // 添加dropout防止过拟合
}
```

## 实施步骤

### 步骤1: 数据准备（最重要）

1. **收集更多历史数据**
   - 目标: 至少20年数据
   - 来源: 统计年鉴、农业部门

2. **补充气象数据**
   - 日照时数
   - 湿度
   - 风速
   - 极端天气事件

3. **数据清洗**
```sql
-- 删除异常值（产量为0或异常高）
DELETE FROM yield_records 
WHERE yield_value = 0 OR yield_value > 10000;

-- 填充缺失值（用前后年份平均）
UPDATE yield_records yr1
SET yield_value = (
    SELECT AVG(yr2.yield_value)
    FROM yield_records yr2
    WHERE yr2.crop_id = yr1.crop_id
      AND yr2.region_id = yr1.region_id
      AND yr2.year BETWEEN yr1.year - 1 AND yr1.year + 1
      AND yr2.yield_value IS NOT NULL
)
WHERE yield_value IS NULL;
```

### 步骤2: 前端修改

修改预测表单，添加更多输入字段：

```vue
<!-- forecast/src/views/ForecastExecutionView.vue -->
<template>
  <el-form>
    <!-- 现有字段 -->
    <el-form-item label="温度(°C)">
      <el-input-number v-model="weather.temperature" />
    </el-form-item>
    <el-form-item label="降水量(mm)">
      <el-input-number v-model="weather.precipitation" />
    </el-form-item>
    
    <!-- 新增字段 -->
    <el-form-item label="日照时数(小时)">
      <el-input-number v-model="weather.sunshine_hours" :min="0" :max="24" />
    </el-form-item>
    <el-form-item label="相对湿度(%)">
      <el-input-number v-model="weather.humidity" :min="0" :max="100" />
    </el-form-item>
    <el-form-item label="平均风速(m/s)">
      <el-input-number v-model="weather.wind_speed" :min="0" />
    </el-form-item>
  </el-form>
</template>
```

### 步骤3: 尝试不同模型

在前端预测界面添加模型选择：

```vue
<el-form-item label="预测模型">
  <el-select v-model="selectedModel">
    <el-option label="自动选择(推荐)" value="" />
    <el-option label="LSTM深度学习" value="LSTM" />
    <el-option label="ARIMA时间序列" value="ARIMA" />
    <el-option label="天气回归" value="WEATHER_REGRESSION" />
    <el-option label="Prophet" value="PROPHET" />
  </el-select>
</el-form-item>
```

### 步骤4: 验证改进效果

运行预测后检查指标：

```
改进前:
- R² = 0.17
- RMSE = 0.22
- MAE = 0.18
- MAPE = 6.14%

改进目标:
- R² > 0.60 (提升3.5倍)
- RMSE < 0.15 (降低30%)
- MAE < 0.12 (降低33%)
- MAPE < 4% (降低35%)
```

## 常见问题

### Q1: 为什么R²这么低？
**可能原因**:
1. 历史数据太少（15年不够）
2. 气象特征不完整
3. 数据质量差（有异常值或缺失）
4. 模型选择不当
5. 没有考虑滞后效应

### Q2: 哪个改进最有效？
**优先级排序**:
1. 增加历史数据量 (影响最大)
2. 添加更多气象特征
3. 添加滞后特征
4. 尝试不同模型
5. 参数调优

### Q3: 需要多少数据才够？
**建议**:
- 最少: 20年
- 推荐: 30年
- 理想: 40年以上

### Q4: 如果数据有限怎么办？
**替代方案**:
1. 使用相邻地区数据
2. 使用省级或国家级数据
3. 使用迁移学习
4. 使用数据增强技术

## 预期时间表

| 改进措施 | 所需时间 | R²提升 |
|---------|---------|--------|
| 数据清洗 | 2小时 | +0.05 |
| 添加气象特征 | 4小时 | +0.15 |
| 添加滞后特征 | 2小时 | +0.10 |
| 尝试LSTM模型 | 1小时 | +0.10 |
| 参数调优 | 3小时 | +0.08 |
| **总计** | **12小时** | **+0.48** |

**预期结果**: R² 从 0.17 提升到 0.65

## 下一步行动

1. ✅ 立即检查数据质量（运行上面的SQL）
2. ✅ 收集更多历史数据
3. ✅ 补充气象特征数据
4. ✅ 尝试LSTM模型
5. ✅ 实施滞后特征

需要我帮你实施具体的代码修改吗？
