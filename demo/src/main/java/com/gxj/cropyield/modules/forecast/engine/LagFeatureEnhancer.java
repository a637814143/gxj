package com.gxj.cropyield.modules.forecast.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 滞后特征增强器
 * 
 * 为历史数据添加滞后特征，提高预测准确性
 * 滞后特征包括：
 * - lag1_yield: 前一年产量
 * - lag2_yield: 前两年产量
 * - moving_avg_3: 3年移动平均
 * - moving_avg_5: 5年移动平均
 * - yield_change_rate: 产量变化率
 * - yield_volatility: 产量波动性
 */
public class LagFeatureEnhancer {
    
    /**
     * 为历史数据添加滞后特征
     * 
     * @param history 原始历史数据
     * @return 增强后的历史数据
     */
    public static List<ForecastEngineRequest.HistoryPoint> enhanceWithLagFeatures(
            List<ForecastEngineRequest.HistoryPoint> history) {
        
        if (history == null || history.size() < 2) {
            return history;
        }
        
        List<ForecastEngineRequest.HistoryPoint> enhanced = new ArrayList<>();
        
        for (int i = 0; i < history.size(); i++) {
            ForecastEngineRequest.HistoryPoint current = history.get(i);
            Map<String, Double> enhancedFeatures = new HashMap<>();
            
            // 复制原有特征
            if (current.features() != null) {
                enhancedFeatures.putAll(current.features());
            }
            
            // 添加滞后特征
            addLagFeatures(history, i, enhancedFeatures);
            
            // 添加移动平均特征
            addMovingAverageFeatures(history, i, enhancedFeatures);
            
            // 添加变化率特征
            addChangeRateFeatures(history, i, enhancedFeatures);
            
            // 添加波动性特征
            addVolatilityFeatures(history, i, enhancedFeatures);
            
            enhanced.add(new ForecastEngineRequest.HistoryPoint(
                current.period(),
                current.value(),
                enhancedFeatures
            ));
        }
        
        return enhanced;
    }
    
    /**
     * 添加滞后特征（前N年的产量）
     */
    private static void addLagFeatures(List<ForecastEngineRequest.HistoryPoint> history,
                                      int currentIndex,
                                      Map<String, Double> features) {
        // lag1: 前一年产量
        if (currentIndex >= 1) {
            Double lag1 = history.get(currentIndex - 1).value();
            if (lag1 != null) {
                features.put("lag1_yield", lag1);
            }
        }
        
        // lag2: 前两年产量
        if (currentIndex >= 2) {
            Double lag2 = history.get(currentIndex - 2).value();
            if (lag2 != null) {
                features.put("lag2_yield", lag2);
            }
        }
        
        // lag3: 前三年产量
        if (currentIndex >= 3) {
            Double lag3 = history.get(currentIndex - 3).value();
            if (lag3 != null) {
                features.put("lag3_yield", lag3);
            }
        }
    }
    
    /**
     * 添加移动平均特征
     */
    private static void addMovingAverageFeatures(List<ForecastEngineRequest.HistoryPoint> history,
                                                 int currentIndex,
                                                 Map<String, Double> features) {
        // 3年移动平均
        if (currentIndex >= 2) {
            double sum = 0;
            int count = 0;
            for (int i = Math.max(0, currentIndex - 2); i <= currentIndex; i++) {
                Double value = history.get(i).value();
                if (value != null) {
                    sum += value;
                    count++;
                }
            }
            if (count > 0) {
                features.put("moving_avg_3", sum / count);
            }
        }
        
        // 5年移动平均
        if (currentIndex >= 4) {
            double sum = 0;
            int count = 0;
            for (int i = Math.max(0, currentIndex - 4); i <= currentIndex; i++) {
                Double value = history.get(i).value();
                if (value != null) {
                    sum += value;
                    count++;
                }
            }
            if (count > 0) {
                features.put("moving_avg_5", sum / count);
            }
        }
    }
    
    /**
     * 添加变化率特征
     */
    private static void addChangeRateFeatures(List<ForecastEngineRequest.HistoryPoint> history,
                                             int currentIndex,
                                             Map<String, Double> features) {
        // 年度变化率
        if (currentIndex >= 1) {
            Double current = history.get(currentIndex).value();
            Double previous = history.get(currentIndex - 1).value();
            if (current != null && previous != null && previous != 0) {
                double changeRate = (current - previous) / previous;
                features.put("yield_change_rate", changeRate);
            }
        }
        
        // 两年平均变化率
        if (currentIndex >= 2) {
            Double current = history.get(currentIndex).value();
            Double twoYearsAgo = history.get(currentIndex - 2).value();
            if (current != null && twoYearsAgo != null && twoYearsAgo != 0) {
                double avgChangeRate = (current - twoYearsAgo) / (2 * twoYearsAgo);
                features.put("avg_change_rate_2", avgChangeRate);
            }
        }
    }
    
    /**
     * 添加波动性特征（标准差）
     */
    private static void addVolatilityFeatures(List<ForecastEngineRequest.HistoryPoint> history,
                                             int currentIndex,
                                             Map<String, Double> features) {
        // 3年波动性
        if (currentIndex >= 2) {
            List<Double> values = new ArrayList<>();
            for (int i = Math.max(0, currentIndex - 2); i <= currentIndex; i++) {
                Double value = history.get(i).value();
                if (value != null) {
                    values.add(value);
                }
            }
            if (values.size() >= 2) {
                double volatility = calculateStandardDeviation(values);
                features.put("yield_volatility_3", volatility);
            }
        }
        
        // 5年波动性
        if (currentIndex >= 4) {
            List<Double> values = new ArrayList<>();
            for (int i = Math.max(0, currentIndex - 4); i <= currentIndex; i++) {
                Double value = history.get(i).value();
                if (value != null) {
                    values.add(value);
                }
            }
            if (values.size() >= 2) {
                double volatility = calculateStandardDeviation(values);
                features.put("yield_volatility_5", volatility);
            }
        }
    }
    
    /**
     * 计算标准差
     */
    private static double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        
        double mean = values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * 为预测期添加滞后特征
     * 
     * @param history 历史数据
     * @param forecastPeriod 预测期数（1, 2, 3...）
     * @param previousForecast 之前的预测值
     * @return 预测期的特征
     */
    public static Map<String, Double> createForecastPeriodFeatures(
            List<ForecastEngineRequest.HistoryPoint> history,
            int forecastPeriod,
            List<Double> previousForecast) {
        
        Map<String, Double> features = new HashMap<>();
        
        if (history.isEmpty()) {
            return features;
        }
        
        // 使用最后的历史值和之前的预测值作为滞后特征
        int historySize = history.size();
        
        // lag1: 如果是第一期预测，用最后的历史值；否则用上一期预测值
        if (forecastPeriod == 1) {
            features.put("lag1_yield", history.get(historySize - 1).value());
        } else if (forecastPeriod <= previousForecast.size() + 1) {
            features.put("lag1_yield", previousForecast.get(forecastPeriod - 2));
        }
        
        // lag2
        if (forecastPeriod == 1 && historySize >= 2) {
            features.put("lag2_yield", history.get(historySize - 2).value());
        } else if (forecastPeriod == 2) {
            features.put("lag2_yield", history.get(historySize - 1).value());
        } else if (forecastPeriod > 2 && forecastPeriod <= previousForecast.size() + 1) {
            features.put("lag2_yield", previousForecast.get(forecastPeriod - 3));
        }
        
        // 移动平均（使用最近的历史值和预测值）
        List<Double> recentValues = new ArrayList<>();
        for (int i = Math.max(0, historySize - 3); i < historySize; i++) {
            recentValues.add(history.get(i).value());
        }
        for (int i = 0; i < Math.min(forecastPeriod - 1, previousForecast.size()); i++) {
            recentValues.add(previousForecast.get(i));
        }
        if (recentValues.size() >= 3) {
            double sum = recentValues.stream()
                .limit(3)
                .mapToDouble(Double::doubleValue)
                .sum();
            features.put("moving_avg_3", sum / 3.0);
        }
        
        return features;
    }
}
