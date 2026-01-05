package com.gxj.cropyield.modules.forecast.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Prophet-like 预测器
 * 实现简化版的Prophet算法：趋势 + 季节性 + 事件影响
 * 基于可加性模型: y(t) = g(t) + s(t) + h(t) + ε
 * g(t): 趋势项
 * s(t): 季节性项
 * h(t): 事件影响项
 * ε: 误差项
 */
class ProphetForecaster {

    private static final int MIN_HISTORY = 4;
    private static final double DEFAULT_CHANGEPOINT_PRIOR_SCALE = 0.05;
    private static final double DEFAULT_SEASONALITY_PRIOR_SCALE = 10.0;

    /**
     * 执行Prophet预测
     * 
     * @param historyValues 历史数据
     * @param periods 预测期数
     * @param parameters 参数配置
     * @return 预测结果
     */
    Optional<List<Double>> forecast(List<Double> historyValues, int periods, Map<String, Object> parameters) {
        if (historyValues == null || historyValues.size() < MIN_HISTORY) {
            return Optional.empty();
        }
        if (periods <= 0) {
            periods = 1;
        }

        try {
            // 提取参数
            double changepointPriorScale = extractDoubleParameter(parameters, "changepointPriorScale", DEFAULT_CHANGEPOINT_PRIOR_SCALE);
            double seasonalityPriorScale = extractDoubleParameter(parameters, "seasonalityPriorScale", DEFAULT_SEASONALITY_PRIOR_SCALE);
            int seasonalityMode = extractIntParameter(parameters, "seasonalityPeriod", 0);  // 0表示自动检测
            
            // 1. 分解趋势
            TrendComponent trend = fitTrend(historyValues, changepointPriorScale);
            
            // 2. 提取季节性
            SeasonalityComponent seasonality = fitSeasonality(historyValues, trend, seasonalityMode, seasonalityPriorScale);
            
            // 3. 生成预测
            List<Double> forecast = new ArrayList<>();
            int n = historyValues.size();
            
            for (int step = 1; step <= periods; step++) {
                int t = n + step;
                
                // 趋势预测
                double trendValue = predictTrend(trend, t);
                
                // 季节性预测
                double seasonalValue = predictSeasonality(seasonality, t);
                
                // 组合预测
                double prediction = trendValue + seasonalValue;
                
                // 确保非负
                prediction = Math.max(0, prediction);
                
                forecast.add(prediction);
            }
            
            return Optional.of(forecast);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 拟合趋势组件（分段线性趋势）
     */
    private TrendComponent fitTrend(List<Double> data, double changepointPriorScale) {
        int n = data.size();
        
        // 检测变点（简化版：使用固定间隔）
        List<Integer> changepoints = new ArrayList<>();
        int numChangepoints = Math.min(5, n / 3);
        
        if (numChangepoints > 0) {
            int interval = n / (numChangepoints + 1);
            for (int i = 1; i <= numChangepoints; i++) {
                changepoints.add(i * interval);
            }
        }
        
        // 拟合分段线性趋势
        double[] slopes = new double[changepoints.size() + 1];
        double intercept = data.get(0);
        
        // 简化实现：整体线性趋势
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += data.get(i);
            sumXY += i * data.get(i);
            sumXX += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        intercept = (sumY - slope * sumX) / n;
        
        slopes[0] = slope;
        
        return new TrendComponent(intercept, slopes, changepoints, changepointPriorScale);
    }

    /**
     * 预测趋势值
     */
    private double predictTrend(TrendComponent trend, int t) {
        return trend.intercept + trend.slopes[0] * t;
    }

    /**
     * 拟合季节性组件
     */
    private SeasonalityComponent fitSeasonality(List<Double> data, TrendComponent trend, int period, double priorScale) {
        int n = data.size();
        
        // 去除趋势
        List<Double> detrended = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double trendValue = trend.intercept + trend.slopes[0] * i;
            detrended.add(data.get(i) - trendValue);
        }
        
        // 自动检测周期
        if (period == 0) {
            period = detectSeasonalityPeriod(detrended);
        }
        
        if (period <= 1 || period > n / 2) {
            // 无明显季节性
            return new SeasonalityComponent(period, new double[0], priorScale);
        }
        
        // 计算季节性模式
        double[] seasonalPattern = new double[period];
        int[] counts = new int[period];
        
        for (int i = 0; i < detrended.size(); i++) {
            int seasonIndex = i % period;
            seasonalPattern[seasonIndex] += detrended.get(i);
            counts[seasonIndex]++;
        }
        
        // 平均化
        for (int i = 0; i < period; i++) {
            if (counts[i] > 0) {
                seasonalPattern[i] /= counts[i];
            }
        }
        
        // 中心化（使季节性和为0）
        double mean = 0;
        for (double value : seasonalPattern) {
            mean += value;
        }
        mean /= period;
        
        for (int i = 0; i < period; i++) {
            seasonalPattern[i] -= mean;
        }
        
        return new SeasonalityComponent(period, seasonalPattern, priorScale);
    }

    /**
     * 检测季节性周期
     */
    private int detectSeasonalityPeriod(List<Double> data) {
        int n = data.size();
        int maxPeriod = Math.min(12, n / 2);
        
        if (maxPeriod < 2) {
            return 0;
        }
        
        // 使用自相关检测周期
        double maxAcf = 0;
        int bestPeriod = 0;
        
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = data.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / n;
        
        if (variance == 0) {
            return 0;
        }
        
        for (int period = 2; period <= maxPeriod; period++) {
            double acf = 0;
            int count = 0;
            
            for (int i = 0; i < n - period; i++) {
                acf += (data.get(i) - mean) * (data.get(i + period) - mean);
                count++;
            }
            
            if (count > 0) {
                acf = acf / (count * variance);
                
                if (acf > maxAcf) {
                    maxAcf = acf;
                    bestPeriod = period;
                }
            }
        }
        
        // 只有当自相关足够强时才认为有季节性
        return maxAcf > 0.3 ? bestPeriod : 0;
    }

    /**
     * 预测季节性值
     */
    private double predictSeasonality(SeasonalityComponent seasonality, int t) {
        if (seasonality.period == 0 || seasonality.pattern.length == 0) {
            return 0;
        }
        
        int seasonIndex = t % seasonality.period;
        return seasonality.pattern[seasonIndex];
    }

    /**
     * 提取双精度参数
     */
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

    /**
     * 提取整数参数
     */
    private int extractIntParameter(Map<String, Object> parameters, String key, int defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 趋势组件
     */
    private static class TrendComponent {
        final double intercept;
        final double[] slopes;
        final List<Integer> changepoints;
        final double priorScale;

        TrendComponent(double intercept, double[] slopes, List<Integer> changepoints, double priorScale) {
            this.intercept = intercept;
            this.slopes = slopes;
            this.changepoints = changepoints;
            this.priorScale = priorScale;
        }
    }

    /**
     * 季节性组件
     */
    private static class SeasonalityComponent {
        final int period;
        final double[] pattern;
        final double priorScale;

        SeasonalityComponent(int period, double[] pattern, double priorScale) {
            this.period = period;
            this.pattern = pattern;
            this.priorScale = priorScale;
        }
    }
}
