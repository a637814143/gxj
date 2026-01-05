# 数据库查询优化指南

## 📋 概述

本文档详细分析了农作物产量预测系统中的数据库查询性能问题，并提供具体的优化方案和实施步骤。

## 🔍 当前问题分析

### 1. N+1 查询问题

#### 问题描述
在多个Service中存在N+1查询问题，特别是在处理关联实体时：

**示例1: DashboardService.getSummary()**
```java
// 当前实现
List<YieldRecord> records = yieldRecordRepository.findAll();
// 后续访问 record.getCrop().getName() 和 record.getRegion().getName()
// 会为每条记录触发额外的查询
```

**示例2: ForecastHistoryService**
```java
Page<ForecastSnapshot> snapshotPage = forecastSnapshotRepository
    .findAllByOrderByCreatedAtDesc(pageRequest);
// 访问 snapshot.getRun().getCrop() 时触发额外查询
```

#### 影响范围
- DashboardService: 仪表盘数据加载
- ForecastHistoryService: 预测历史列表
- ReportService: 报告生成和列表
- ConsultationService: 咨询记录查询

### 2. 缺少索引

#### 问题描述
关键查询字段缺少数据库索引，导致全表扫描：

**高频查询字段**:
- `yield_record`: `region_id`, `crop_id`, `year`
- `weather_record`: `region_id`, `date`
- `forecast_result`: `task_id`, `target_year`
- `forecast_snapshot`: `run_id`, `created_at`
- `consultation`: `status`, `assigned_to`, `created_at`

### 3. 未使用分页查询

#### 问题描述
某些列表查询未使用分页，一次性加载所有数据：

```java
// 问题代码
List<ForecastModel> listAll() {
    return forecastModelRepository.findAll();
}

List<Report> reports = reportRepository.findAll(Sort.by(...));
```

### 4. 缺少查询结果缓存

#### 问题描述
频繁查询的静态数据（如区域、作物列表）未使用缓存：

- 区域列表（Region）
- 作物列表（Crop）
- 系统配置（SystemSetting）

## 🎯 优化方案

### 方案1: 解决N+1查询问题

#### 1.1 使用@EntityGraph

**优化前**:
```java
public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, Long> {
    Page<ForecastSnapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

**优化后**:
```java
public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, Long> {
    @EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
    Page<ForecastSnapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

**说明**: 已在部分Repository中实现，需要扩展到所有关联查询。

#### 1.2 使用JOIN FETCH查询

**创建自定义查询方法**:
```java
public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {
    
    @Query("SELECT y FROM YieldRecord y " +
           "JOIN FETCH y.crop " +
           "JOIN FETCH y.region " +
           "WHERE y.region.id = :regionId AND y.crop.id = :cropId " +
           "ORDER BY y.year ASC")
    List<YieldRecord> findByRegionIdAndCropIdWithDetails(
        @Param("regionId") Long regionId, 
        @Param("cropId") Long cropId
    );
}
```

#### 1.3 使用DTO投影

**对于只需要部分字段的查询**:
```java
public interface YieldRecordProjection {
    Long getId();
    Integer getYear();
    Double getProduction();
    String getCropName();
    String getRegionName();
}

@Query("SELECT y.id as id, y.year as year, y.production as production, " +
       "c.name as cropName, r.name as regionName " +
       "FROM YieldRecord y " +
       "JOIN y.crop c " +
       "JOIN y.region r")
List<YieldRecordProjection> findAllProjections();
```

### 方案2: 添加数据库索引

#### 2.1 创建索引迁移脚本

**文件**: `demo/src/main/resources/db/migration/V2__add_performance_indexes.sql`

```sql
-- YieldRecord表索引
CREATE INDEX idx_yield_record_region_crop ON yield_record(region_id, crop_id);
CREATE INDEX idx_yield_record_year ON yield_record(year);
CREATE INDEX idx_yield_record_region_crop_year ON yield_record(region_id, crop_id, year);

-- WeatherRecord表索引
CREATE INDEX idx_weather_record_region ON weather_record(region_id);
CREATE INDEX idx_weather_record_date ON weather_record(date);
CREATE INDEX idx_weather_record_region_date ON weather_record(region_id, date);

-- ForecastResult表索引
CREATE INDEX idx_forecast_result_task ON forecast_result(task_id);
CREATE INDEX idx_forecast_result_target_year ON forecast_result(target_year);
CREATE INDEX idx_forecast_result_task_year ON forecast_result(task_id, target_year);

-- ForecastSnapshot表索引
CREATE INDEX idx_forecast_snapshot_run ON forecast_snapshot(run_id);
CREATE INDEX idx_forecast_snapshot_created_at ON forecast_snapshot(created_at DESC);
CREATE INDEX idx_forecast_snapshot_run_period ON forecast_snapshot(run_id, period);

-- ForecastTask表索引
CREATE INDEX idx_forecast_task_model_crop_region ON forecast_task(model_id, crop_id, region_id);
CREATE INDEX idx_forecast_task_status ON forecast_task(status);

-- Consultation表索引
CREATE INDEX idx_consultation_status ON consultation(status);
CREATE INDEX idx_consultation_assigned_to ON consultation(assigned_to);
CREATE INDEX idx_consultation_created_at ON consultation(created_at DESC);
CREATE INDEX idx_consultation_status_assigned ON consultation(status, assigned_to);

-- Report表索引
CREATE INDEX idx_report_published_at ON report(published_at DESC);
CREATE INDEX idx_report_forecast_result ON report(forecast_result_id);

-- User表索引
CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_user_department ON sys_user(department_code);

-- LoginLog表索引
CREATE INDEX idx_login_log_username ON sys_login_log(username);
CREATE INDEX idx_login_log_created_at ON sys_login_log(created_at DESC);
```

#### 2.2 验证索引效果

```sql
-- 查看查询执行计划
EXPLAIN SELECT * FROM yield_record 
WHERE region_id = 1 AND crop_id = 2 
ORDER BY year ASC;

-- 查看索引使用情况
SHOW INDEX FROM yield_record;
```

### 方案3: 实现分页查询

#### 3.1 修改Repository接口

**优化前**:
```java
public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
    // 返回所有记录
}
```

**优化后**:
```java
public interface ForecastModelRepository extends JpaRepository<ForecastModel, Long> {
    Page<ForecastModel> findAll(Pageable pageable);
    
    @Query("SELECT m FROM ForecastModel m WHERE m.status = :status")
    Page<ForecastModel> findByStatus(@Param("status") String status, Pageable pageable);
}
```

#### 3.2 修改Service层

**优化前**:
```java
public List<ForecastModel> listAll() {
    return forecastModelRepository.findAll();
}
```

**优化后**:
```java
public Page<ForecastModel> listAll(Pageable pageable) {
    return forecastModelRepository.findAll(pageable);
}

// 或者提供默认分页
public Page<ForecastModel> listAll(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return forecastModelRepository.findAll(pageable);
}
```

#### 3.3 修改Controller层

**优化后**:
```java
@GetMapping
public ResponseEntity<Page<ForecastModel>> list(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Page<ForecastModel> models = forecastModelService.listAll(page, size);
    return ResponseEntity.ok(models);
}
```

### 方案4: 实现查询缓存

#### 4.1 添加缓存依赖

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### 4.2 配置缓存

**CacheConfig.java**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "regions", "crops", "forecastModels", "systemSettings"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats());
        return cacheManager;
    }
}
```

#### 4.3 使用缓存注解

**RegionService**:
```java
@Service
public class RegionService {
    
    @Cacheable(value = "regions", key = "'all'")
    public List<Region> findAll() {
        return regionRepository.findAll();
    }
    
    @Cacheable(value = "regions", key = "#id")
    public Optional<Region> findById(Long id) {
        return regionRepository.findById(id);
    }
    
    @CacheEvict(value = "regions", allEntries = true)
    public Region save(Region region) {
        return regionRepository.save(region);
    }
}
```

**CropService**:
```java
@Service
public class CropService {
    
    @Cacheable(value = "crops", key = "'all'")
    public List<Crop> findAll() {
        return cropRepository.findAll();
    }
    
    @Cacheable(value = "crops", key = "#id")
    public Optional<Crop> findById(Long id) {
        return cropRepository.findById(id);
    }
    
    @CacheEvict(value = "crops", allEntries = true)
    public Crop save(Crop crop) {
        return cropRepository.save(crop);
    }
}
```

### 方案5: 优化DashboardService

#### 5.1 当前问题

```java
// 问题：一次性加载所有记录，并且存在N+1查询
List<YieldRecord> records = yieldRecordRepository.findAll();
```

#### 5.2 优化方案

**创建专用的Repository方法**:
```java
public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {
    
    @Query("SELECT y FROM YieldRecord y " +
           "JOIN FETCH y.crop " +
           "JOIN FETCH y.region " +
           "ORDER BY y.year DESC")
    List<YieldRecord> findAllWithDetails();
    
    @Query("SELECT new com.gxj.cropyield.dashboard.dto.YieldRecordSummary(" +
           "y.year, c.name, r.name, r.level, " +
           "SUM(y.production), SUM(y.sownArea)) " +
           "FROM YieldRecord y " +
           "JOIN y.crop c " +
           "JOIN y.region r " +
           "GROUP BY y.year, c.name, r.name, r.level " +
           "ORDER BY y.year DESC")
    List<YieldRecordSummary> findSummaryData();
}
```

**优化DashboardService**:
```java
@Service
public class DashboardService {
    
    @Cacheable(value = "dashboardSummary", key = "'summary'")
    public DashboardSummaryResponse getSummary() {
        // 使用优化后的查询方法
        List<YieldRecord> records = yieldRecordRepository.findAllWithDetails();
        
        // 或者使用DTO投影
        List<YieldRecordSummary> summaries = yieldRecordRepository.findSummaryData();
        
        // ... 处理逻辑
    }
}
```

### 方案6: 批量查询优化

#### 6.1 使用IN查询替代循环查询

**优化前**:
```java
List<ForecastResult> results = new ArrayList<>();
for (Long taskId : taskIds) {
    forecastResultRepository.findByTaskId(taskId)
        .ifPresent(results::add);
}
```

**优化后**:
```java
List<ForecastResult> results = forecastResultRepository.findByTaskIdIn(taskIds);
```

**Repository方法**:
```java
public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {
    List<ForecastResult> findByTaskIdIn(List<Long> taskIds);
}
```

## 📊 实施优先级

### 高优先级 (立即实施)

1. **添加数据库索引** ⚠️⚠️⚠️
   - 影响: 所有查询性能
   - 工作量: 1-2小时
   - 风险: 低

2. **解决DashboardService的N+1问题** ⚠️⚠️
   - 影响: 仪表盘加载速度
   - 工作量: 2-3小时
   - 风险: 低

3. **解决ForecastHistoryService的N+1问题** ⚠️⚠️
   - 影响: 预测历史列表性能
   - 工作量: 1-2小时
   - 风险: 低

### 中优先级 (近期实施)

4. **实现查询缓存** ⚠️
   - 影响: 减少数据库负载
   - 工作量: 3-4小时
   - 风险: 中（需要考虑缓存失效策略）

5. **添加分页查询** ⚠️
   - 影响: 大数据量场景性能
   - 工作量: 4-6小时
   - 风险: 中（需要修改前端）

### 低优先级 (长期优化)

6. **使用DTO投影优化** 
   - 影响: 减少数据传输量
   - 工作量: 6-8小时
   - 风险: 低

7. **实现读写分离**
   - 影响: 高并发场景性能
   - 工作量: 1-2天
   - 风险: 高

## 🔧 实施步骤

### 步骤1: 添加数据库索引 (30分钟)

```bash
# 1. 创建迁移脚本
touch demo/src/main/resources/db/migration/V2__add_performance_indexes.sql

# 2. 添加索引SQL（见方案2.1）

# 3. 重启应用，Flyway自动执行迁移
./mvnw spring-boot:run

# 4. 验证索引创建
mysql -u root -p crop_yield_db
SHOW INDEX FROM yield_record;
```

### 步骤2: 优化Repository查询 (1-2小时)

```java
// 1. 修改YieldRecordRepository
@EntityGraph(attributePaths = {"crop", "region"})
List<YieldRecord> findAll();

// 2. 修改ForecastSnapshotRepository（已部分完成）
@EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
Page<ForecastSnapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);

// 3. 修改其他Repository...
```

### 步骤3: 实现缓存 (2-3小时)

```java
// 1. 添加依赖到pom.xml
// 2. 创建CacheConfig.java
// 3. 在Service层添加@Cacheable注解
// 4. 测试缓存效果
```

### 步骤4: 添加分页支持 (3-4小时)

```java
// 1. 修改Repository接口
// 2. 修改Service层方法
// 3. 修改Controller层
// 4. 更新前端调用（如需要）
```

## 📈 性能测试

### 测试场景

#### 场景1: 仪表盘加载

**优化前**:
```
查询次数: 1 + N (N为记录数)
响应时间: ~2000ms (1000条记录)
```

**优化后**:
```
查询次数: 1
响应时间: ~200ms (1000条记录)
性能提升: 10倍
```

#### 场景2: 预测历史列表

**优化前**:
```
查询次数: 1 + N*3 (每条记录3个关联查询)
响应时间: ~1500ms (100条记录)
```

**优化后**:
```
查询次数: 1
响应时间: ~150ms (100条记录)
性能提升: 10倍
```

### 测试工具

```java
// 使用JMeter进行压力测试
// 配置：
// - 线程数: 50
// - 循环次数: 100
// - 测试接口: /api/dashboard/summary

// 使用Hibernate统计
spring.jpa.properties.hibernate.generate_statistics=true
spring.jpa.properties.hibernate.use_sql_comments=true
```

## 🎯 预期效果

### 性能提升

| 优化项 | 优化前 | 优化后 | 提升 |
|--------|--------|--------|------|
| 仪表盘加载 | 2000ms | 200ms | 10倍 |
| 预测历史列表 | 1500ms | 150ms | 10倍 |
| 报告列表 | 1000ms | 100ms | 10倍 |
| 数据库查询次数 | 1+N | 1 | N倍 |

### 资源使用

| 指标 | 优化前 | 优化后 | 改善 |
|------|--------|--------|------|
| 数据库连接数 | 高 | 低 | 50% |
| CPU使用率 | 60% | 30% | 50% |
| 内存使用 | 稳定 | 稳定 | - |
| 响应时间 | 1-2s | 100-200ms | 80% |

## ⚠️ 注意事项

### 1. 缓存失效策略

```java
// 确保数据更新时清除缓存
@CacheEvict(value = "regions", allEntries = true)
public Region save(Region region) {
    return regionRepository.save(region);
}
```

### 2. 索引维护

```sql
-- 定期检查索引使用情况
SELECT * FROM information_schema.statistics 
WHERE table_schema = 'crop_yield_db';

-- 删除未使用的索引
DROP INDEX idx_unused ON table_name;
```

### 3. 分页查询注意事项

- 前端需要支持分页参数
- 考虑总数查询的性能影响
- 使用游标分页处理大数据量

### 4. @EntityGraph使用限制

- 不能与动态查询一起使用
- 可能导致笛卡尔积问题
- 需要根据实际情况选择JOIN FETCH或@EntityGraph

## 📚 参考资源

- [Spring Data JPA Performance](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query)
- [Hibernate Performance Tuning](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#performance)
- [MySQL Index Optimization](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)

## ✅ 检查清单

- [ ] 创建数据库索引迁移脚本
- [ ] 执行索引迁移并验证
- [ ] 修改YieldRecordRepository添加@EntityGraph
- [ ] 修改DashboardService使用优化查询
- [ ] 修改ForecastHistoryService使用优化查询
- [ ] 添加缓存依赖和配置
- [ ] 为Region和Crop服务添加缓存
- [ ] 实现分页查询支持
- [ ] 更新API文档
- [ ] 进行性能测试
- [ ] 监控生产环境性能指标
