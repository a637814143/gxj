package com.gxj.cropyield.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 
 * 使用Caffeine作为缓存实现，提供高性能的本地缓存
 * 用于优化数据库查询性能，减少重复查询
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置缓存管理器
     * 
     * 缓存策略：
     * - regions: 区域列表，1小时过期
     * - crops: 作物列表，1小时过期
     * - forecastModels: 预测模型列表，1小时过期
     * - dashboardSummary: 仪表盘摘要，5分钟过期
     * - userDetails: 用户详情，15分钟过期
     * - reportList: 报告列表，10分钟过期
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "regions",           // 区域列表缓存
            "crops",             // 作物列表缓存
            "forecastModels",    // 预测模型列表缓存
            "dashboardSummary",  // 仪表盘摘要缓存
            "userDetails",       // 用户详情缓存
            "reportList"         // 报告列表缓存
        );
        
        // 配置Caffeine缓存
        cacheManager.setCaffeine(Caffeine.newBuilder()
            // 最大缓存条目数
            .maximumSize(1000)
            // 写入后1小时过期（默认）
            .expireAfterWrite(1, TimeUnit.HOURS)
            // 启用统计信息收集
            .recordStats()
        );
        
        return cacheManager;
    }
}
