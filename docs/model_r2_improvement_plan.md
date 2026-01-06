# 模型R²值改进方案

## 问题分析

当前模型R²值为0.17，说明模型只能解释17%的数据变异，拟合效果较差。

### R²值评估标准
- **R² > 0.9**: 优秀
- **0.7 < R² ≤ 0.9**: 良好
- **0.5 < R² ≤ 0.7**: 一般
- **0.3 < R² ≤ 0.5**: 较差
- **R² ≤ 0.3**: 很差（当前情况：0.17）

## 改进方案

### 1. 数据质量改进 ⭐⭐⭐⭐⭐

#### 1.1 增加历史数据量
**问题**: 当前只有15年数据，样本量可能不足
**建议**:
- 至少需要20-30年的历史数据
- 数据越多，模型越稳定

#### 1.2 数据清洗
**检查项**:
- 异常值检测和处理
- 缺失值填充策略
- 数据平滑处理

```java
// 异常值检测示例
private List<Double> removeOutliers(List<Double> data) {
    double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double std = calculateStd(data, mean);
    
    return data.stream()
        .filter(v -> Math.abs(v - mean) <= 3 * std) // 3σ原则
        .collect(Collectors.toList());
}
```

### 2. 特征工程优化 ⭐⭐⭐⭐⭐

#### 2.1 添加更多气象特征
**当前问题**: 可能只使用了基础气象数据
**建议添加**:
- 降水量（precipitation）
- 温度（temperature）
- 日照时数（sunshine_hours）
- 湿度（humidity）
- 风速（wind_speed）
- 土壤湿度（soil_moisture）
- 极端天气事件（extreme_weather_events）

#### 2.2 特征交互
**创建组合特征**:
```java
// 温度×降水交互项
double tempPrecipInteraction = temperature * precipitation;

// 生长季节累积温度
double growingDegreeDays = calculateGDD(dailyTemperatures);

// 干旱指数
double droughtIndex = precipitation / evapotranspiration;
```

#### 2.3 滞后特征
**添加时间滞后**:
```java
// 前一年产量
double lag1Yield = previousYearYield;

// 前两年平均产量
double lag2AvgYield = (previousYearYield + twoYearsAgoYield) / 2;

// 3年移动平均
double movingAvg3 = calculateMovingAverage(yields, 3);
```

### 3. 模型算法优化 ⭐⭐⭐⭐

#### 3.1 集成学习方法
**建议**: 使用多个模型的加权平均

```java
public class EnsembleForecaster {
    public List<Double> forecast(List<Double> history, int periods) {
        // 获取各模型预测
        List<Double> lstmForecast = lstmForecaster.forecast(history, periods);
        List<Double> arimaForecast = arimaForecaster.forecast(history, periods);
        List<Double> regressionForecast = weatherRegressionForecast(history, periods);
        
        // 根据历史表现加权
        double lstmWeight = 0.4;    // R²=0.75
        double arimaWeight = 0.3;   // R²=0.65
        double regressionWeight = 0.3; // R²=0.70
        
        List<Double> ensemble = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            double weighted = lstmWeight * lstmForecast.get(i)
                            + arimaWeight * arimaForecast.get(i)
                            + regressionWeight * regressionForecast.get(i);
            ensemble.add(weighted);
        }
        return ensemble;
    }
}
```

#### 3.2 超参数调优
**LSTM模型参数**:
```java
// 当前可能的问题
int epochs = 50;          // 可能太少，建议100-200
int hiddenSize = 64;      // 可能太小，建议128-256
double learningRate = 0.001; // 可能需要调整
int lookbackWindow = 5;   // 可能太小，建议10-15

// 优化后的参数
Map<String, Object> optimizedParams = Map.of(
    "epochs", 150,
    "hiddenSize", 128,
    "learningRate", 0.0005,
    "lookbackWindow", 12,
    "dropout", 0.2,
    "batchSize", 16
);
```

#### 3.3 正则化
**防止过拟合**:
```java
// L2正则化
double l2Lambda = 0.01;
double regularizationTerm = l2Lambda * sumOfSquaredWeights;

// Dropout (LSTM)
double dropoutRate = 0.2;

// Early Stopping
int patience = 10; // 10个epoch没有改善就停止
```

### 4. 数据标准化 ⭐⭐⭐⭐

#### 4.1 特征缩放
**问题**: 不同特征的量纲差异大
**解决**:
```java
// Z-score标准化
private double[] standardize(double[] features, double mean, double std) {
    return Arrays.stream(features)
        .map(f -> (f - mean) / std)
        .toArray();
}

// Min-Max归一化
private double[] normalize(double[] features, double min, double max) {
    return Arrays.stream(features)
        .map(f -> (f - min) / (max - min))
        .toArray();
}
```

#### 4.2 目标变量转换
**对于偏态分布**:
```java
// 对数转换
double logYield = Math.log(yield + 1);

// Box-Cox转换
double transformedYield = boxCoxTransform(yield, lambda);
```

### 5. 交叉验证 ⭐⭐⭐⭐

#### 5.1 时间序列交叉验证
**避免数据泄露**:
```java
public double crossValidateTimeSeries(List<Double> data, int folds) {
    int foldSize = data.size() / folds;
    List<Double> r2Scores = new ArrayList<>();
    
    for (int i = 0; i < folds; i++) {
        int trainEnd = (i + 1) * foldSize;
        List<Double> train = data.subList(0, trainEnd);
        List<Double> test = data.subList(trainEnd, 
            Math.min(trainEnd + foldSize, data.size()));
        
        // 训练和评估
        model.train(train);
        double r2 = model.evaluate(test);
        r2Scores.add(r2);
    }
    
    return r2Scores.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);
}
```

### 6. 模型诊断 ⭐⭐⭐

#### 6.1 残差分析
**检查模型假设**:
```java
public void diagnoseModel(List<Double> actual, List<Double> predicted) {
    List<Double> residuals = new ArrayList<>();
    for (int i = 0; i < actual.size(); i++) {
        residuals.add(actual.get(i) - predicted.get(i));
    }
    
    // 1. 残差均值应接近0
    double meanResidual = residuals.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);
    
    // 2. 残差方差应该恒定（同方差性）
    double variance = calculateVariance(residuals);
    
    // 3. 残差应该正态分布
    boolean isNormal = shapiroWilkTest(residuals);
    
    // 4. 残差应该无自相关
    double autocorrelation = calculateAutocorrelation(residuals, 1);
    
    System.out.println("残差均值: " + meanResidual);
    System.out.println("残差方差: " + variance);
    System.out.println("正态性: " + isNormal);
    System.out.println("自相关: " + autocorrelation);
}
```

### 7. 特定作物优化 ⭐⭐⭐

#### 7.1 作物特定模型
**问题**: 不同作物对气象因素的响应不同
**建议**:
```java
// 为每种作物训练独立模型
Map<String, ForecastModel> cropSpecificModels = Map.of(
    "小麦", wheatModel,
    "水稻", riceModel,
    "玉米", cornModel
);

// 作物特定特征权重
Map<String, Map<String, Double>> cropFeatureWeights = Map.of(
    "小麦", Map.of(
        "temperature", 0.35,
        "precipitation", 0.30,
        "sunshine", 0.25,
        "soil_moisture", 0.10
    ),
    "水稻", Map.of(
        "precipitation", 0.40,
        "temperature", 0.30,
        "humidity", 0.20,
        "sunshine", 0.10
    )
);
```

## 实施优先级

### 第一阶段（立即实施）
1. ✅ 数据质量检查和清洗
2. ✅ 添加更多气象特征
3. ✅ 特征标准化

### 第二阶段（短期）
4. ✅ 创建特征交互项
5. ✅ 添加滞后特征
6. ✅ 超参数调优

### 第三阶段（中期）
7. ✅ 实施集成学习
8. ✅ 交叉验证优化
9. ✅ 残差分析和诊断

### 第四阶段（长期）
10. ✅ 作物特定模型
11. ✅ 深度学习模型优化
12. ✅ 实时模型更新机制

## 预期效果

| 改进措施 | 预期R²提升 | 实施难度 |
|---------|-----------|---------|
| 数据清洗 | +0.05~0.10 | 低 |
| 增加特征 | +0.10~0.20 | 中 |
| 特征工程 | +0.15~0.25 | 中 |
| 超参数调优 | +0.05~0.15 | 中 |
| 集成学习 | +0.10~0.20 | 高 |
| 作物特定模型 | +0.05~0.10 | 中 |

**综合预期**: R² 从 0.17 提升到 0.60~0.80

## 快速改进建议

如果时间有限，优先实施以下3项：

### 1. 特征标准化（30分钟）
```java
// 在回归前标准化所有特征
features = standardizeFeatures(features);
```

### 2. 添加滞后特征（1小时）
```java
// 添加前一年产量作为特征
features.put("lag1_yield", previousYearYield);
```

### 3. 增加训练轮数（5分钟）
```java
// LSTM训练轮数从50增加到150
parameters.put("epochs", 150);
```

这三项改进预计可以将R²从0.17提升到0.35~0.45。

## 监控指标

除了R²，还应关注：
- **MAE** (平均绝对误差): 越小越好
- **RMSE** (均方根误差): 越小越好
- **MAPE** (平均绝对百分比误差): 越小越好
- **预测偏差**: 应接近0
- **预测区间覆盖率**: 应在90%~95%

## 参考资料

1. 《作物产量预测中的机器学习方法》
2. 《时间序列预测：原理与实践》
3. 《特征工程实战》
4. 《深度学习在农业中的应用》
