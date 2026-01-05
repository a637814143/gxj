# 农作物产量预测LSTM模型架构详解

## 目录
1. [系统整体架构](#系统整体架构)
2. [LSTM模型结构](#lstm模型结构)
3. [实现思路](#实现思路)
4. [核心算法](#核心算法)
5. [技术栈](#技术栈)
6. [数据流程](#数据流程)

---

## 系统整体架构

### 1. 模块结构

```
forecast/
├── controller/          # 控制层：处理HTTP请求
│   ├── ForecastExecutionController    # 执行预测
│   ├── ForecastHistoryController      # 预测历史
│   ├── ForecastModelController        # 模型管理
│   └── ForecastTaskController         # 任务管理
├── service/            # 服务层：业务逻辑
│   ├── ForecastExecutionService       # 预测执行服务
│   ├── ForecastHistoryService         # 历史记录服务
│   ├── ForecastModelService           # 模型管理服务
│   └── ModelRegistryService           # 模型注册服务
├── engine/             # 引擎层：预测算法核心
│   ├── LocalForecastEngine            # 本地预测引擎（主引擎）
│   ├── Dl4jLstmForecaster            # LSTM预测器
│   ├── ForecastEngineClient          # 引擎客户端
│   ├── ForecastEngineRequest         # 引擎请求DTO
│   └── ForecastEngineResponse        # 引擎响应DTO
├── entity/             # 实体层：数据模型
│   ├── ForecastModel                  # 预测模型
│   ├── ForecastRun                    # 预测运行记录
│   ├── ForecastResult                 # 预测结果
│   ├── ForecastTask                   # 预测任务
│   └── ForecastSnapshot               # 预测快照
└── repository/         # 数据访问层：数据库操作
```

### 2. 预测算法体系

系统支持**4种预测算法**，根据模型类型自动选择或组合：

| 算法 | 类型 | 适用场景 | 特点 |
|------|------|----------|------|
| **LSTM** | 深度学习 | 复杂非线性趋势 | 可学习长期依赖，参数可调 |
| **WEATHER_REGRESSION** | 统计回归 | 天气相关预测 | 结合气象特征，可解释性强 |
| **DOUBLE_EXPONENTIAL** | 时间序列 | 有趋势的数据 | 简单快速，适合短期预测 |
| **LINEAR_TREND** | 线性回归 | 线性趋势数据 | 计算简单，基准模型 |

---

## LSTM模型结构

### 1. 网络架构

```
输入层 → LSTM层 → 输出层
  ↓        ↓        ↓
[1]     [32]      [1]
```

#### 详细结构

```java
MultiLayerNetwork:
├── Layer 0: LSTM
│   ├── 输入维度 (nIn): 1          // 单变量时间序列
│   ├── 输出维度 (nOut): 32        // 32个LSTM单元
│   ├── 激活函数: TANH             // 双曲正切
│   └── 权重初始化: XAVIER         // Xavier初始化
│
└── Layer 1: RnnOutputLayer
    ├── 输入维度 (nIn): 32         // 来自LSTM层
    ├── 输出维度 (nOut): 1         // 单值输出
    ├── 激活函数: IDENTITY         // 线性激活（回归任务）
    └── 损失函数: MSE              // 均方误差
```

### 2. 超参数配置

| 参数 | 默认值 | 可配置 | 说明 |
|------|--------|--------|------|
| **learningRate** | 0.01 | ✅ | Adam优化器学习率 |
| **seed** | 42 | ✅ | 随机种子（可复现性） |
| **epochs** | 自动计算 | ✅ | 训练轮数 |
| **windowSize** | 2-12 | ❌ | 滑动窗口大小（自动） |
| **hiddenUnits** | 32 | ❌ | LSTM隐藏单元数 |
| **optimizer** | Adam | ❌ | 优化算法 |
| **weightInit** | XAVIER | ❌ | 权重初始化方法 |

#### Epochs自动计算逻辑

```java
epochs = Math.max(40, Math.min(200, sampleCount * 15))
```

- 最小值：40轮
- 最大值：200轮
- 动态值：样本数 × 15

**示例**：
- 5个样本 → 75轮
- 10个样本 → 150轮
- 20个样本 → 200轮（上限）

---

## 实现思路

### 1. 设计理念

#### 1.1 滑动窗口预测

LSTM使用**滑动窗口**方法进行时间序列预测：

```
历史数据: [y1, y2, y3, y4, y5, y6, y7, y8]
窗口大小: 3

训练样本:
样本1: [y1, y2, y3] → y4
样本2: [y2, y3, y4] → y5
样本3: [y3, y4, y5] → y6
样本4: [y4, y5, y6] → y7
样本5: [y5, y6, y7] → y8
```

#### 1.2 窗口大小自适应

```java
windowSize = Math.min(MAX_WINDOW_SIZE, Math.max(MIN_WINDOW_SIZE, historyValues.size() / 2))
```

- **MIN_WINDOW_SIZE = 2**：最小窗口
- **MAX_WINDOW_SIZE = 12**：最大窗口（约1年月度数据）
- **自适应规则**：历史数据长度的一半，但限制在2-12之间

**示例**：
- 6个历史点 → 窗口=3
- 20个历史点 → 窗口=10
- 30个历史点 → 窗口=12（上限）

### 2. 数据预处理

#### 2.1 归一化（Min-Max Scaling）

```java
normalized = (value - min) / (max - min)
```

**目的**：
- 将数据缩放到 [0, 1] 区间
- 加速神经网络收敛
- 避免梯度爆炸/消失

**示例**：
```
原始数据: [100, 150, 200, 250, 300]
min = 100, max = 300, range = 200

归一化后: [0.0, 0.25, 0.5, 0.75, 1.0]
```

#### 2.2 反归一化

```java
denormalized = prediction * range + min
```

预测完成后，将结果转换回原始尺度。

### 3. 训练数据构建

#### 3.1 数据集结构

```java
DataSet {
    features: [sampleCount, 1, windowSize]    // 输入特征
    labels: [sampleCount, 1, windowSize]      // 目标标签
    labelMask: [sampleCount, windowSize]      // 标签掩码
}
```

#### 3.2 标签掩码机制

**为什么需要掩码？**

LSTM是序列到序列模型，但我们只需要预测序列的最后一个值：

```
输入序列: [y1, y2, y3]
输出序列: [?, ?, y4]  ← 只有最后一个位置有标签

labelMask: [0, 0, 1]  ← 只计算最后一个位置的损失
```

**代码实现**：
```java
for (int i = 0; i < sampleCount; i++) {
    // 输入：窗口内的所有值
    for (int j = 0; j < windowSize; j++) {
        features.putScalar(new int[]{i, 0, j}, scaledSeries[i + j]);
    }
    // 标签：窗口后的下一个值（只在最后一个位置）
    labels.putScalar(new int[]{i, 0, windowSize - 1}, scaledSeries[i + windowSize]);
    // 掩码：只有最后一个位置为1
    labelMask.putScalar(new int[]{i, windowSize - 1}, 1d);
}
```

### 4. 多步预测策略

#### 4.1 递归预测（Recursive Forecasting）

预测多个未来时间点时，使用**递归策略**：

```
步骤1: 用历史数据预测 t+1
步骤2: 将 t+1 加入历史，预测 t+2
步骤3: 将 t+2 加入历史，预测 t+3
...
```

**代码实现**：
```java
List<Double> normalizedWorking = new ArrayList<>(scaledSeries);
for (int step = 0; step < periods; step++) {
    // 1. 取最后windowSize个值作为输入
    double[] window = getLastWindow(normalizedWorking, windowSize);
    
    // 2. 预测下一个值
    double prediction = predict(network, window);
    
    // 3. 将预测值加入工作序列
    normalizedWorking.add(prediction);
    
    // 4. 反归一化并保存
    forecasts.add(denormalize(prediction));
}
```

#### 4.2 预测误差累积

递归预测的**缺点**：误差会累积

```
预测1期: 误差 = ε₁
预测2期: 误差 = ε₁ + ε₂
预测3期: 误差 = ε₁ + ε₂ + ε₃
```

因此系统限制**最多预测3期**。

---

## 核心算法

### 1. LSTM单元原理

LSTM（Long Short-Term Memory）通过**门控机制**解决长期依赖问题：

```
遗忘门 (Forget Gate):  决定丢弃哪些信息
输入门 (Input Gate):   决定存储哪些新信息
输出门 (Output Gate):  决定输出哪些信息
```

#### 数学公式

```
ft = σ(Wf·[ht-1, xt] + bf)     # 遗忘门
it = σ(Wi·[ht-1, xt] + bi)     # 输入门
C̃t = tanh(WC·[ht-1, xt] + bC)  # 候选记忆
Ct = ft ⊙ Ct-1 + it ⊙ C̃t      # 更新记忆
ot = σ(Wo·[ht-1, xt] + bo)     # 输出门
ht = ot ⊙ tanh(Ct)             # 隐藏状态
```

### 2. Adam优化器

Adam（Adaptive Moment Estimation）结合了动量和自适应学习率：

```
mt = β1·mt-1 + (1-β1)·gt       # 一阶矩估计（动量）
vt = β2·vt-1 + (1-β2)·gt²      # 二阶矩估计（RMSprop）
m̂t = mt / (1-β1^t)             # 偏差修正
v̂t = vt / (1-β2^t)             # 偏差修正
θt = θt-1 - α·m̂t / (√v̂t + ε)  # 参数更新
```

**默认参数**：
- α (learningRate) = 0.01
- β1 = 0.9
- β2 = 0.999
- ε = 1e-8

### 3. 模型评估指标

系统使用4个指标评估预测性能：

#### 3.1 MAE（平均绝对误差）

```
MAE = (1/n) Σ|yi - ŷi|
```

**含义**：预测值与实际值的平均偏差
**单位**：与原始数据相同
**越小越好**

#### 3.2 RMSE（均方根误差）

```
RMSE = √[(1/n) Σ(yi - ŷi)²]
```

**含义**：预测误差的标准差
**特点**：对大误差更敏感
**越小越好**

#### 3.3 MAPE（平均绝对百分比误差）

```
MAPE = (100/n) Σ|yi - ŷi| / |yi|
```

**含义**：相对误差的百分比
**单位**：百分比（%）
**越小越好**

#### 3.4 R²（决定系数）

```
R² = 1 - (SSres / SStot)
SSres = Σ(yi - ŷi)²
SStot = Σ(yi - ȳ)²
```

**含义**：模型解释数据变异的比例
**范围**：(-∞, 1]
- R² = 1：完美预测
- R² = 0：与均值预测相同
- R² < 0：比均值预测还差

---

## 技术栈

### 1. 深度学习框架

**DeepLearning4j (DL4J)**
- Java原生深度学习框架
- 支持分布式训练
- 与Spring Boot无缝集成

**核心依赖**：
```xml
<dependency>
    <groupId>org.deeplearning4j</groupId>
    <artifactId>deeplearning4j-core</artifactId>
</dependency>
<dependency>
    <groupId>org.nd4j</groupId>
    <artifactId>nd4j-native-platform</artifactId>
</dependency>
```

### 2. 数值计算库

**ND4J（N-Dimensional Arrays for Java）**
- 类似NumPy的Java库
- 支持CPU和GPU加速
- 提供张量运算

### 3. 其他算法

#### 3.1 双指数平滑（Holt's Method）

```java
level = α·value + (1-α)·(level + trend)
trend = β·(level - prevLevel) + (1-β)·trend
forecast = level + trend·h
```

**参数**：
- α = 0.6（水平平滑系数）
- β = 0.4（趋势平滑系数）

#### 3.2 线性趋势预测

```java
y = a + b·x
b = (nΣxy - ΣxΣy) / (nΣx² - (Σx)²)
a = (Σy - bΣx) / n
```

#### 3.3 天气回归预测

```java
y = β₀ + β₁·temp + β₂·rainfall + β₃·sunshine + ...
```

使用**正规方程**求解：
```
β = (XᵀX)⁻¹Xᵀy
```

支持**岭回归**（Ridge Regression）防止过拟合：
```
β = (XᵀX + λI)⁻¹Xᵀy
```

---

## 数据流程

### 1. 完整预测流程

```
用户请求
    ↓
ForecastExecutionController
    ↓
ForecastExecutionService.runForecast()
    ├─ 1. 验证输入（区域、作物、模型）
    ├─ 2. 加载历史产量数据
    ├─ 3. 数据清洗和预处理
    ├─ 4. 创建ForecastRun记录
    ├─ 5. 调用预测引擎
    │      ↓
    │   invokeEngine()
    │      ├─ 构建参数（用户参数 + 系统参数）
    │      ├─ 加载天气特征（如果需要）
    │      ├─ 构建ForecastEngineRequest
    │      └─ 调用LocalForecastEngine
    │             ↓
    │          LocalForecastEngine.forecast()
    │             ├─ 数据清洗（sanitizeHistory）
    │             ├─ 解析模型类型
    │             ├─ 根据模型类型选择算法：
    │             │   ├─ WEATHER_REGRESSION → 天气回归
    │             │   ├─ LSTM → 强制使用LSTM
    │             │   └─ 其他 → 多算法比较
    │             │          ├─ DOUBLE_EXPONENTIAL
    │             │          ├─ LINEAR_TREND
    │             │          ├─ LSTM
    │             │          └─ 选择RMSE最低的
    │             │
    │             └─ 如果选择LSTM：
    │                    ↓
    │                 Dl4jLstmForecaster.forecast()
    │                    ├─ 1. 数据归一化
    │                    ├─ 2. 计算窗口大小
    │                    ├─ 3. 构建训练集
    │                    ├─ 4. 提取参数
    │                    ├─ 5. 构建网络
    │                    ├─ 6. 训练模型
    │                    ├─ 7. 递归预测
    │                    └─ 8. 反归一化
    │                           ↓
    │                    返回预测结果
    │             ↓
    │          返回ForecastEngineResponse
    │      ↓
    │   返回预测结果
    ├─ 6. 更新ForecastRun状态
    ├─ 7. 保存预测序列（ForecastRunSeries）
    ├─ 8. 创建预测快照（ForecastSnapshot）
    ├─ 9. 持久化预测结果（ForecastResult）
    └─ 10. 返回响应
           ↓
    ForecastExecutionResponse
```

### 2. 数据实体关系

```
ForecastModel (预测模型)
    ↓ 1:N
ForecastTask (预测任务)
    ↓ 1:N
ForecastRun (预测运行)
    ↓ 1:N
    ├─ ForecastRunSeries (时间序列数据)
    ├─ ForecastSnapshot (预测快照)
    └─ ForecastResult (预测结果)
```

### 3. 关键数据结构

#### ForecastEngineRequest
```java
record ForecastEngineRequest(
    String modelCode,              // 模型类型代码
    String frequency,              // 预测频率（YEARLY/QUARTERLY）
    int forecastPeriods,           // 预测期数
    List<HistoryPoint> history,    // 历史数据点
    Map<String, Object> parameters // 用户参数
)
```

#### ForecastEngineResponse
```java
record ForecastEngineResponse(
    String requestId,                    // 请求ID
    List<ForecastPoint> forecast,        // 预测点列表
    EvaluationMetrics metrics            // 评估指标
)

record ForecastPoint(
    String period,      // 时间周期
    Double value,       // 预测值
    Double lowerBound,  // 置信区间下界
    Double upperBound   // 置信区间上界
)
```

---

## 总结

### 系统特点

1. **多算法支持**：LSTM、天气回归、双指数平滑、线性趋势
2. **自动算法选择**：根据历史数据自动选择最优算法
3. **参数可配置**：支持learningRate、seed、epochs等参数调整
4. **完整评估体系**：MAE、RMSE、MAPE、R²四个指标
5. **模块化设计**：清晰的分层架构，易于扩展
6. **Java原生实现**：无需Python环境，部署简单

### 技术亮点

1. **自适应窗口**：根据数据量自动调整LSTM窗口大小
2. **递归预测**：支持多步预测
3. **数据归一化**：提高模型训练效率
4. **标签掩码**：精确控制损失计算
5. **参数自动调优**：epochs根据样本数自动计算
6. **模型评估**：交叉验证评估模型性能

### 适用场景

- ✅ 农作物产量预测
- ✅ 时间序列预测
- ✅ 趋势分析
- ✅ 短期预测（1-3期）
- ⚠️ 长期预测（误差累积）
- ⚠️ 突变数据（需要更多历史数据）
