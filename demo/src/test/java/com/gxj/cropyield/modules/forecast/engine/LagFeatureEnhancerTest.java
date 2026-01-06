package com.gxj.cropyield.modules.forecast.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 滞后特征增强器测试
 */
class LagFeatureEnhancerTest {

    @Test
    void testEnhanceWithLagFeatures() {
        // 准备测试数据：5年历史产量
        List<ForecastEngineRequest.HistoryPoint> history = new ArrayList<>();
        history.add(createHistoryPoint("2019", 4500.0));
        history.add(createHistoryPoint("2020", 4650.0));
        history.add(createHistoryPoint("2021", 4800.0));
        history.add(createHistoryPoint("2022", 4700.0));
        history.add(createHistoryPoint("2023", 4900.0));

        // 执行增强
        List<ForecastEngineRequest.HistoryPoint> enhanced = 
            LagFeatureEnhancer.enhanceWithLagFeatures(history);

        // 验证结果
        assertNotNull(enhanced);
        assertEquals(5, enhanced.size());

        // 验证第3年（2021）的滞后特征
        ForecastEngineRequest.HistoryPoint point2021 = enhanced.get(2);
        Map<String, Double> features = point2021.features();
        
        // 应该有lag1（2020年产量）
        assertTrue(features.containsKey("lag1_yield"));
        assertEquals(4650.0, features.get("lag1_yield"), 0.01);
        
        // 应该有lag2（2019年产量）
        assertTrue(features.containsKey("lag2_yield"));
        assertEquals(4500.0, features.get("lag2_yield"), 0.01);
        
        // 应该有3年移动平均
        assertTrue(features.containsKey("moving_avg_3"));
        double expectedAvg = (4500.0 + 4650.0 + 4800.0) / 3.0;
        assertEquals(expectedAvg, features.get("moving_avg_3"), 0.01);
        
        // 应该有变化率
        assertTrue(features.containsKey("yield_change_rate"));
        double expectedRate = (4800.0 - 4650.0) / 4650.0;
        assertEquals(expectedRate, features.get("yield_change_rate"), 0.001);

        System.out.println("✅ 滞后特征测试通过");
        System.out.println("2021年特征数量: " + features.size());
        System.out.println("lag1_yield: " + features.get("lag1_yield"));
        System.out.println("lag2_yield: " + features.get("lag2_yield"));
        System.out.println("moving_avg_3: " + features.get("moving_avg_3"));
        System.out.println("yield_change_rate: " + features.get("yield_change_rate"));
    }

    @Test
    void testEnhanceWithMinimalData() {
        // 测试最少数据（2年）
        List<ForecastEngineRequest.HistoryPoint> history = new ArrayList<>();
        history.add(createHistoryPoint("2022", 4500.0));
        history.add(createHistoryPoint("2023", 4650.0));

        List<ForecastEngineRequest.HistoryPoint> enhanced = 
            LagFeatureEnhancer.enhanceWithLagFeatures(history);

        assertNotNull(enhanced);
        assertEquals(2, enhanced.size());

        // 第2年应该有lag1
        ForecastEngineRequest.HistoryPoint point2023 = enhanced.get(1);
        assertTrue(point2023.features().containsKey("lag1_yield"));
        assertEquals(4500.0, point2023.features().get("lag1_yield"), 0.01);

        System.out.println("✅ 最少数据测试通过");
    }

    @Test
    void testEnhanceWithExistingFeatures() {
        // 测试已有气象特征的情况
        List<ForecastEngineRequest.HistoryPoint> history = new ArrayList<>();
        
        Map<String, Double> weather1 = new HashMap<>();
        weather1.put("temperature", 25.0);
        weather1.put("precipitation", 800.0);
        history.add(new ForecastEngineRequest.HistoryPoint("2022", 4500.0, weather1));
        
        Map<String, Double> weather2 = new HashMap<>();
        weather2.put("temperature", 26.0);
        weather2.put("precipitation", 850.0);
        history.add(new ForecastEngineRequest.HistoryPoint("2023", 4650.0, weather2));

        List<ForecastEngineRequest.HistoryPoint> enhanced = 
            LagFeatureEnhancer.enhanceWithLagFeatures(history);

        // 验证原有特征保留
        ForecastEngineRequest.HistoryPoint point2023 = enhanced.get(1);
        Map<String, Double> features = point2023.features();
        
        assertTrue(features.containsKey("temperature"));
        assertTrue(features.containsKey("precipitation"));
        assertTrue(features.containsKey("lag1_yield"));
        
        assertEquals(26.0, features.get("temperature"), 0.01);
        assertEquals(850.0, features.get("precipitation"), 0.01);
        assertEquals(4500.0, features.get("lag1_yield"), 0.01);

        System.out.println("✅ 保留原有特征测试通过");
        System.out.println("总特征数: " + features.size());
    }

    private ForecastEngineRequest.HistoryPoint createHistoryPoint(String period, Double value) {
        return new ForecastEngineRequest.HistoryPoint(period, value, new HashMap<>());
    }
}
