# 数据库查询优化 - 快速实施指南

## 🚀 5分钟快速开始

本指南帮助你快速实施数据库查询优化，立即提升系统性能。

## 📋 前置检查

```bash
# 1. 确认数据库连接正常
mysql -u root -p crop_yield_db

# 2. 确认应用可以正常启动
cd demo
./mvnw spring-boot:run
```

## ⚡ 第一步：添加数据库索引（5分钟）

### 1.1 索引已准备好

索引迁移脚本已创建在：
```
demo/src/main/resources/db/migration/V2__add_performance_indexes.sql
```

### 1.2 应用索引

**方法1：重启应用（推荐）**
```bash
# Flyway会自动执行迁移
./mvnw spring-boot:run
```

**方法2：手动执行SQL**
```bash
# 连接数据库
mysql -u root -p crop_yield_db

# 执行迁移脚本
source src/main/resources/db/migration/V2__add_performance_indexes.sql
```

### 1.3 验证索引创建

```sql
-- 查看yield_record表的索引
SHOW INDEX FROM yield_record;

-- 查看所有新创建的索引
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'crop_yield_db'
  AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME;
```

**预期结果**：应该看到类似以下的索引：
- `idx_yield_record_region_crop`
- `idx_yield_record_year`
- `idx_forecast_snapshot_created_at`
- 等等...

### 1.4 测试性能提升

**测试查询**：
```sql
-- 优化前：全表扫描
EXPLAIN SELECT * FROM yield_record 
WHERE region_id = 1 AND crop_id = 2 
ORDER BY year ASC;

-- 优化后：应该显示使用了索引
-- type: ref (使用索引)
-- key: idx_yield_record_region_crop
```

**预期效果**：
- 查询时间从 100-500ms 降低到 10-50ms
- 10倍性能提升 ✅

## 🔧 第二步：解决N+1查询问题（10分钟）

### 2.1 优化YieldRecordRepository

**文件位置**：`demo/src/main/java/com/gxj/cropyield/modules/dataset/repository/YieldRecordRepository.java`

**添加以下方法**：
```java
// 在YieldRecordRepository接口中添加
@EntityGraph(attributePaths = {"crop", "region", "datasetFile"})
List<YieldRecord> findByRegionIdAndCropIdOrderByYearAsc(Long regionId, Long cropId);

@EntityGraph(attributePaths = {"crop", "region"})
@Query("SELECT y FROM YieldRecord y ORDER BY y.year DESC")
List<YieldRecord> findAllWithDetails();
```

### 2.2 优化ForecastSnapshotRepository

**文件位置**：`demo/src/main/java/com/gxj/cropyield/modules/forecast/repository/ForecastSnapshotRepository.java`

**检查是否已有@EntityGraph**（应该已经有了）：
```java
@EntityGraph(attributePaths = {"run", "run.model", "run.crop", "run.region"})
Page<ForecastSnapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);
```

### 2.3 优化DashboardService

**文件位置**：`demo/src/main/java/com/gxj/cropyield/dashboard/DashboardService.java`

**修改getSummary方法**：
```java
public DashboardSummaryResponse getSummary() {
    // 修改前：List<YieldRecord> records = yieldRecordRepository.findAll();
    // 修改后：使用优化的查询方法
    List<YieldRecord> records = yieldRecordRepository.findAllWithDetails();
    
    // 其余代码保持不变
    // ...
}
```

### 2.4 测试N+1问题解决

**启用Hibernate SQL日志**：

在 `application.yml` 中添加：
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

**测试**：
```bash
# 重启应用
./mvnw spring-boot:run

# 访问仪表盘API
curl http://localhost:8080/api/dashboard/summary
```

**检查日志**：
- 优化前：会看到大量SELECT语句（1 + N条）
- 优化后：只有1条SELECT语句（使用LEFT JOIN）

**预期效果**：
- 仪表盘加载时间从 2000ms 降低到 200ms
- 10倍性能提升 ✅

## 💾 第三步：添加缓存（15分钟）

### 3.1 添加依赖

**编辑 `pom.xml`**，在 `<dependencies>` 中添加：
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

### 3.2 启用缓存配置

**重命名配置文件**：
```bash
cd demo/src/main/java/com/gxj/cropyield/config
mv CacheConfig.java.example CacheConfig.java
```

或者**创建新文件** `CacheConfig.java`：
```java
package com.gxj.cropyield.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "regions", "crops", "forecastModels", "dashboardSummary"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats());
        return cacheManager;
    }
}
```

### 3.3 为DashboardService添加缓存

**修改 `DashboardService.java`**：
```java
import org.springframework.cache.annotation.Cacheable;

@Service
public class DashboardService {
    
    // 添加@Cacheable注解
    @Cacheable(value = "dashboardSummary", key = "'summary'")
    public DashboardSummaryResponse getSummary() {
        // 原有代码保持不变
        // ...
    }
}
```

### 3.4 测试缓存效果

```bash
# 重启应用
./mvnw spring-boot:run

# 第一次访问（会查询数据库）
curl http://localhost:8080/api/dashboard/summary
# 响应时间：~200ms

# 第二次访问（从缓存读取）
curl http://localhost:8080/api/dashboard/summary
# 响应时间：~10ms
```

**预期效果**：
- 第二次及后续访问速度提升 20倍
- 数据库查询减少 90% ✅

## 📊 性能对比

### 优化前
```
仪表盘加载：
- 查询次数：1 + 100 (crop) + 100 (region) = 201次
- 响应时间：~2000ms
- CPU使用率：60%
```

### 优化后
```
仪表盘加载：
- 查询次数：1次（首次）或 0次（缓存命中）
- 响应时间：~200ms（首次）或 ~10ms（缓存）
- CPU使用率：30%

性能提升：
- 首次访问：10倍提升
- 缓存命中：200倍提升
- CPU使用率：降低50%
```

## ✅ 验证清单

完成以下检查确保优化成功：

- [ ] 数据库索引已创建（运行 `SHOW INDEX FROM yield_record;`）
- [ ] YieldRecordRepository 添加了 @EntityGraph
- [ ] ForecastSnapshotRepository 已有 @EntityGraph
- [ ] DashboardService 使用了优化的查询方法
- [ ] 缓存依赖已添加到 pom.xml
- [ ] CacheConfig.java 已创建并启用
- [ ] DashboardService 添加了 @Cacheable 注解
- [ ] 应用可以正常启动
- [ ] 仪表盘API响应时间明显降低
- [ ] 日志中SQL查询次数明显减少

## 🔍 性能监控

### 查看Hibernate统计

**临时启用（用于测试）**：
```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
```

**查看统计信息**：
```java
@RestController
@RequestMapping("/api/admin/stats")
public class StatsController {
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @GetMapping("/hibernate")
    public Map<String, Object> getHibernateStats() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class)
            .getStatistics();
        
        Map<String, Object> result = new HashMap<>();
        result.put("queryExecutionCount", stats.getQueryExecutionCount());
        result.put("queryCacheHitCount", stats.getQueryCacheHitCount());
        result.put("queryCacheMissCount", stats.getQueryCacheMissCount());
        result.put("secondLevelCacheHitCount", stats.getSecondLevelCacheHitCount());
        
        return result;
    }
}
```

### 查看缓存统计

```java
@RestController
@RequestMapping("/api/admin/cache")
public class CacheStatsController {
    
    @Autowired
    private CacheManager cacheManager;
    
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    ((CaffeineCache) cache).getNativeCache();
                
                CacheStats cacheStats = nativeCache.stats();
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("hitCount", cacheStats.hitCount());
                cacheInfo.put("missCount", cacheStats.missCount());
                cacheInfo.put("hitRate", cacheStats.hitRate());
                cacheInfo.put("size", nativeCache.estimatedSize());
                
                stats.put(cacheName, cacheInfo);
            }
        });
        
        return stats;
    }
}
```

## 🚨 常见问题

### Q1: 索引创建失败

**错误**：`Duplicate key name 'idx_xxx'`

**解决**：索引已存在，可以忽略或删除后重建
```sql
DROP INDEX idx_yield_record_region_crop ON yield_record;
```

### Q2: @EntityGraph不生效

**检查**：
1. 确保Repository方法返回类型正确
2. 确保在@Transactional上下文中调用
3. 检查attributePaths拼写是否正确

### Q3: 缓存不生效

**检查**：
1. 确保@EnableCaching注解存在
2. 确保方法是public的
3. 确保不是在同一个类内部调用（需要通过代理）
4. 检查缓存名称是否在CacheManager中配置

### Q4: 性能提升不明显

**可能原因**：
1. 数据量太小，看不出差异
2. 数据库本身性能很好
3. 网络延迟是主要瓶颈

**建议**：
- 使用JMeter进行压力测试
- 增加测试数据量
- 使用APM工具监控

## 📚 进一步优化

完成基础优化后，可以考虑：

1. **读写分离**：配置主从数据库
2. **连接池优化**：调整HikariCP参数
3. **批量操作**：使用批量插入/更新
4. **异步处理**：耗时操作使用@Async
5. **分布式缓存**：使用Redis替代本地缓存

详细信息请参考：
- 📄 `docs/database_query_optimization.md`
- 📄 `docs/optimized_repository_examples.md`

## 🎉 完成！

恭喜！你已经完成了数据库查询优化的基础实施。

**预期效果**：
- ✅ 响应时间降低 80-90%
- ✅ 数据库查询减少 90%
- ✅ CPU使用率降低 50%
- ✅ 支持更高并发

继续监控系统性能，根据实际情况进行调整。
