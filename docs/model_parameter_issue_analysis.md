# 模型参数不生效问题分析与解决

## 问题描述

用户在预测中心页面添加模型参数（如 learningRate、seed、epochs）后，预测结果与不添加参数时完全相同，参数没有生效。

## 问题根源

### 核心问题
`LocalForecastEngine.java` 只使用了 `futureWeatherFeatures` 参数，完全忽略了其他所有参数（learningRate、seed、epochs等）。

### 具体原因

1. **`Dl4jLstmForecaster` 类使用硬编码值**：
   - Learning rate: 在 `buildNetwork()` 方法中硬编码为 `new Adam(0.01)`
   - Epochs: 计算为 `Math.max(40, Math.min(200, sampleCount * 15))` - 不可配置
   - Seed: 硬编码为 `.seed(42)`

2. **参数未传递到LSTM预测器**：
   - `LocalForecastEngine.forecast()` 调用 `lstmForecaster.forecast(historyValues, forecastPeriods)` 时没有传递参数
   - `Dl4jLstmForecaster.forecast()` 方法不接受参数

3. **参数传递流程**：
   ```
   前端用户输入参数 ✅
   ↓
   ForecastExecutionRequest.parameters() ✅
   ↓
   invokeEngine(history, run, userParameters) ✅
   ↓
   ForecastEngineRequest.parameters() ✅
   ↓
   LocalForecastEngine.forecast(request) ✅
   ↓
   lstmForecaster.forecast(historyValues, forecastPeriods) ❌ 参数丢失
   ```

## 解决方案

### 1. 修改 `Dl4jLstmForecaster.java`

#### 添加常量和参数支持
```java
private static final double DEFAULT_LEARNING_RATE = 0.01;
private static final int DEFAULT_SEED = 42;

// 添加重载方法
Optional<List<Double>> forecast(List<Double> historyValues, int periods) {
    return forecast(historyValues, periods, null);
}

Optional<List<Double>> forecast(List<Double> historyValues, int periods, Map<String, Object> parameters) {
    // 方法实现
}
```

#### 修改网络构建方法
```java
private MultiLayerNetwork buildNetwork(double learningRate, int seed) {
    MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
        .seed(seed)  // 使用参数
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(learningRate))  // 使用参数
        .list(...)
        .build();
    return new MultiLayerNetwork(configuration);
}
```

#### 添加参数提取方法
```java
private double extractDoubleParameter(Map<String, Object> parameters, String key, double defaultValue) {
    if (parameters == null || !parameters.containsKey(key)) {
        return defaultValue;
    }
    Object value = parameters.get(key);
    if (value instanceof Number) {
        return ((Number) value).doubleValue();
    }
    try {
        return Double.parseDouble(value.toString());
    } catch (NumberFormatException ex) {
        return defaultValue;
    }
}

private Integer extractIntParameter(Map<String, Object> parameters, String key, Integer defaultValue) {
    // 类似实现
}
```

#### 在训练中使用参数
```java
// 提取参数
double learningRate = extractDoubleParameter(parameters, "learningRate", DEFAULT_LEARNING_RATE);
int seed = extractIntParameter(parameters, "seed", DEFAULT_SEED);
Integer epochsParam = extractIntParameter(parameters, "epochs", null);

// 设置随机种子
if (parameters != null && parameters.containsKey("seed")) {
    Nd4j.getRandom().setSeed(seed);
}

// 构建网络
MultiLayerNetwork network = buildNetwork(learningRate, seed);

// 使用参数中的epochs或计算值
int epochs = epochsParam != null ? epochsParam : Math.max(40, Math.min(200, sampleCount * 15));
for (int epoch = 0; epoch < epochs; epoch++) {
    network.fit(trainingData);
}
```

### 2. 修改 `LocalForecastEngine.java`

#### 传递参数到LSTM预测器
```java
// 修改前
Optional<List<Double>> lstmForecast = lstmForecaster.forecast(historyValues, forecastPeriods);

// 修改后
Optional<List<Double>> lstmForecast = lstmForecaster.forecast(historyValues, forecastPeriods, request.parameters());
```

#### 更新评估方法
```java
// 修改前
private ForecastEvaluation evaluateLstmPerformance(List<Double> historyValues) {
    // ...
    Optional<List<Double>> forecast = lstmForecaster.forecast(training, 1);
}

// 修改后
private ForecastEvaluation evaluateLstmPerformance(List<Double> historyValues, Map<String, Object> parameters) {
    // ...
    Optional<List<Double>> forecast = lstmForecaster.forecast(training, 1, parameters);
}
```

## 支持的参数

修复后，系统现在支持以下参数：

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `learningRate` | Double | 0.01 | Adam优化器的学习率 |
| `seed` | Integer | 42 | 随机种子，用于结果可复现 |
| `epochs` | Integer | 自动计算 | 训练轮数，默认根据样本数量计算 |

## 测试验证

### 测试步骤
1. 创建两个预测任务，使用相同的数据但不同的参数
2. 验证预测结果是否不同
3. 使用相同的参数（包括seed）验证结果是否一致

### 测试示例

**测试A - 默认参数**：
```json
{
  "regionId": 1,
  "cropId": 1,
  "modelId": 1,
  "parameters": {}
}
```

**测试B - 低学习率**：
```json
{
  "regionId": 1,
  "cropId": 1,
  "modelId": 1,
  "parameters": {
    "learningRate": 0.001,
    "seed": 42
  }
}
```

**测试C - 高学习率**：
```json
{
  "regionId": 1,
  "cropId": 1,
  "modelId": 1,
  "parameters": {
    "learningRate": 0.1,
    "seed": 42
  }
}
```

**预期结果**：
- 测试A、B、C的预测结果应该不同（因为学习率不同）
- 重复运行测试B应该得到相同结果（因为seed固定）

## 修改的文件

1. `demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/Dl4jLstmForecaster.java`
   - 添加参数支持
   - 修改网络构建方法
   - 添加参数提取方法

2. `demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java`
   - 传递参数到LSTM预测器
   - 更新评估方法签名

## 总结

问题已解决。参数从前端到后端的传递一直是正常的，问题在于：
1. LSTM预测器使用硬编码值而不是参数
2. LocalForecastEngine没有将参数传递给LSTM预测器

修复后，用户在前端设置的 learningRate、seed、epochs 参数现在会正确应用到LSTM模型训练中，不同的参数值会产生不同的预测结果。
