# 产量预测模型与参数说明报告

## 1. 预测请求与可配置参数
- **场景定位**：预测运行通过 `ForecastExecutionRequest` 下发，必须指定区域、作物、模型，以及预测长度与可选历史窗口，附带参数字典。`forecastPeriods` 被后端限制在 1~3 步内，`historyYears` 用于截取最近若干年的训练窗口，`frequency` 默认为 YEARLY。参数字典会合并系统生成的窗口与频率信息，再传入引擎。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/dto/ForecastExecutionRequest.java†L11-L18】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L135-L168】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L421-L467】
- **外生气象特征**：当模型类型为 `WEATHER_REGRESSION` 时，服务会从已入库的气象记录构建历史特征，并在有可用预报时填充 `futureWeatherFeatures`，再一起放入参数字典供引擎回归预测使用。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L431-L447】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L478-L520】
- **引擎入参结构**：引擎请求体包含模型代码、时间频率、预测步数、带可选特征的历史序列以及参数字典，后续所有算法均基于该统一结构处理。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/ForecastEngineRequest.java†L9-L18】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L461-L467】

## 2. 模型实现思路
本地预测引擎 `LocalForecastEngine` 以“多候选模型 + 评价择优”为核心，针对是否具备气象特征分两条路径：
- **气象回归路线**（`WEATHER_REGRESSION`）：
  - 先筛选包含完整特征和年份的历史样本，计算特征均值/标准差并做标准化回归拟合；必要时使用岭回归和留一验证稳定系数。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L140-L213】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L372-L448】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L492-L565】
  - 对未来步长，先查找用户/系统提供的 `futureWeatherFeatures`，否则依据历史趋势外推气象特征，再将标准化后的特征代入回归系数生成预测，并与线性趋势结果比较 R² 后择优输出。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L400-L448】
- **无气象特征路线**（默认）：
  - 生成多个候选：双指数平滑（含网格搜索超参获得的误差指标）、线性趋势回归，以及在历史序列长度足够时的 LSTM 结果。每个候选都记录 RMSE/MAPE 评分。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L174-L233】
  - 根据评分选出表现最佳的候选；若全部不可用则回退到简单指数平滑。随后基于历史振幅计算置信区间，为每个预测点输出上下界。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L233-L259】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L244-L259】

## 3. 数据流与训练依据
1. **历史数据截取与单位统一**：服务层根据区域+作物筛选全部产量记录，按 `historyYears` 截取最近窗口，并记录度量标签/单位，保证输入统一量纲。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L123-L158】
2. **特征组装**：历史序列被包装为 `HistoryPoint`，其中可附带同年份的气象特征；未来气象特征通过参数字典提供。频率、窗口等元信息也被放入参数字典，便于引擎使用统一配置。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L421-L467】
3. **训练/推理与评价**：引擎依据模型类型走对应路线，自动做窗口清洗、趋势/回归/深度模型拟合，并计算 MAE、RMSE、MAPE、R² 等指标作为评估依据。运行结果及指标回填到 `ForecastRun`，同时保存历史+预测序列与快照，便于后续报表和可视化使用。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L140-L259】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L158-L244】

## 4. 方法优势与适用性
- **组合式建模**：同时提供趋势、平滑、深度与带外生特征的回归候选，针对不同数据形态自动择优，减少人工调参成本。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L174-L233】
- **可控预测范围**：统一限制预测步长与历史窗口，避免超出可解释区间，并通过置信区间输出上下界供风险评估。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L154-L259】
- **气象驱动能力**：支持利用历史气象特征与未来预报（或趋势外推）做回归，适合受气候显著影响的作物场景。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L431-L520】【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/engine/LocalForecastEngine.java†L372-L448】
- **统一输出链路**：预测结果、指标与元数据统一写入 `ForecastRun`、`ForecastRunSeries` 与快照表，方便前端仪表盘和报告直接消费。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L158-L244】
