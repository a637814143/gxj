# 天气 API 接入建议

## 现有集成现状
- 后端实时天气模块 `WeatherServiceImpl` 已通过彩云天气（Caiyun Weather）REST 接口拉取实时观测，包括温度、风速、湿度、AQI 等指标，并在 `WeatherController` 中对外提供接口，供前端天气中心展示。【F:demo/src/main/java/com/gxj/cropyield/modules/weather/service/impl/WeatherServiceImpl.java†L21-L117】【F:demo/src/main/java/com/gxj/cropyield/modules/weather/controller/WeatherController.java†L24-L60】
- 预测产量流程在构建气象特征时只读取 `dataset_weather_record` 中的历史观测，未来年份通过内生线性外推获得，不会自动消费实时服务的数据。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L419-L522】

因此，要在预测中真正“考虑未来天气”，需要新增一个可靠的预报数据源，并把其结果转换成与 `summarizeWeatherFeatures` 相同的年度特征结构。

## 候选预报 API 对比
| 数据源 | 覆盖范围与精度 | 主要能力 | 优势 | 注意事项 |
| --- | --- | --- | --- | --- |
| **和风天气（QWeather）企业版** | 中国大陆县级/格点，小时级 24-72h，日级 7-15 天；气候趋势支持 30-90 天 | 日常气象、农业气象、极端天气预警、历史气象 | 文档完善，接口稳定；支持中文行政区编码，易与现有区域表关联；支持按农业场景聚合的指标（积温、日照等） | 免费额度有限，需申请开发者 Key；部分高级功能需付费套餐 |
| **国家气象信息中心（CMA）开放数据服务** | 全国站点级观测与预报 | 逐日/逐小时预报、历史再分析、专业指数 | 官方权威，长期可用；适合需要高可信度的业务沟通 | 需要完成数据使用备案；接口为 FTP/HTTP 文件下载，需自行解析 GRIB/文本格式；开发成本较高 |
| **ECMWF/ERA5 再分析 + 运营预报** | 全球 0.25°-0.5° 格点 | 中长期（10-45 天）预报、再分析 | 提供长周期气候趋势，适合情景模拟与科研 | 数据量大，需要结合 Copernicus API；时效性较慢，且对网络环境要求高 |

> 推荐优先选择 **和风天气企业版**：其 API 形态与现有彩云调用模式类似，易于快速对接，并提供日级、小时级预报满足未来气象特征聚合需求。对于需要官方背书的场景，可在后续补充 CMA 数据作为校准或兜底。

## 接入和风天气的实施要点
1. **配置管理**  
   - 在 `WeatherProperties` 中新增 `QWeatherProperties` 配置类，包含 `key`, `baseUrl`, `timeout` 等字段，并在 `WeatherConfiguration` 里暴露对应的 `RestTemplate` Bean。【F:demo/src/main/java/com/gxj/cropyield/modules/weather/config/WeatherConfiguration.java†L13-L32】【F:demo/src/main/java/com/gxj/cropyield/modules/weather/config/WeatherProperties.java†L11-L88】
2. **客户端封装**  
   - 新建 `QWeatherForecastClient` 封装 `https://api.qweather.com/v7/weather/7d`, `15d`, `grid-weather` 等接口，统一签名、重试与错误处理。
3. **特征聚合流程扩展**  
   - 在 `ForecastExecutionServiceImpl.buildWeatherFeatureMap` 中，当目标年份超出历史观测范围时优先调用 `QWeatherForecastClient` 获取逐日预报，按既有窗口（冬闲、春播等）聚合温度、降水、日照，覆盖 `finalWeatherFeatures` 中对应年份的默认外推结果。【F:demo/src/main/java/com/gxj/cropyield/modules/forecast/service/impl/ForecastExecutionServiceImpl.java†L451-L522】
4. **数据追溯**  
   - 将预报结果写入扩展表（如 `weather_forecast_feature`）或在 `WeatherRecord` 上增加 `dataSource=forecast-qweather` 标识，方便历史复算和审计。

## 进阶：多数据源融合
- **短期准确性**：和风天气提供的逐日预报可满足 15 天内的精准预测。
- **中长期趋势**：CMA 或 ECMWF 预报可作为“偏旱/偏涝”情景的基线，通过权重融合生成不同场景的特征集。
- **人工兜底**：为业务专家保留手动导入口，与 `dataSource=manual` 区分，确保在缺少预报的区域仍可参与预测。

通过上述组合，可以向导师解释我们既保留了现有的实时监测能力，又规划了可实施的未来天气接入方案。
