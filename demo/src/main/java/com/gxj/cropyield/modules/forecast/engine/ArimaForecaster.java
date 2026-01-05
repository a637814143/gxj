package com.gxj.cropyield.modules.forecast.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ARIMA (AutoRegressive Integrated Moving Average) 预测器
 * 实现基础的ARIMA(p,d,q)模型用于时间序列预测
 */
class ArimaForecaster {

    private static final int DEFAULT_P = 1;  // AR阶数
    private static final int DEFAULT_D = 1;  // 差分阶数
    private static final int DEFAULT_Q = 1;  // MA阶数
    private static final int MIN_HISTORY = 5;

    /**
     * 执行ARIMA预测
     * 
     * @param historyValues 历史数据
     * @param periods 预测期数
     * @param parameters 参数配置 (p, d, q)
     * @return 预测结果
     */
    Optional<List<Double>> forecast(List<Double> historyValues, int periods, Map<String, Object> parameters) {
        if (historyValues == null || historyValues.size() < MIN_HISTORY) {
            return Optional.empty();
        }
        if (periods <= 0) {
            periods = 1;
        }

        // 提取ARIMA参数
        int p = extractIntParameter(parameters, "p", DEFAULT_P);
        int d = extractIntParameter(parameters, "d", DEFAULT_D);
        int q = extractIntParameter(parameters, "q", DEFAULT_Q);

        // 参数验证
        p = Math.max(0, Math.min(p, 5));
        d = Math.max(0, Math.min(d, 2));
        q = Math.max(0, Math.min(q, 5));

        try {
            // 1. 差分处理（I部分）
            List<Double> differenced = applyDifferencing(historyValues, d);
            
            if (differenced.isEmpty()) {
                return Optional.empty();
            }

            // 2. 拟合ARMA模型
            ArmaModel model = fitArma(differenced, p, q);
            
            if (model == null) {
                return Optional.empty();
            }

            // 3. 预测差分后的值
            List<Double> differencedForecast = predictArma(model, differenced, periods);

            // 4. 逆差分恢复原始尺度
            List<Double> forecast = reverseDifferencing(differencedForecast, historyValues, d);

            // 5. 确保预测值为正
            for (int i = 0; i < forecast.size(); i++) {
                if (forecast.get(i) < 0) {
                    forecast.set(i, Math.max(0, historyValues.get(historyValues.size() - 1) * 0.9));
                }
            }

            return Optional.of(forecast);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 应用差分
     */
    private List<Double> applyDifferencing(List<Double> data, int d) {
        List<Double> result = new ArrayList<>(data);
        
        for (int order = 0; order < d; order++) {
            if (result.size() <= 1) {
                break;
            }
            List<Double> differenced = new ArrayList<>();
            for (int i = 1; i < result.size(); i++) {
                differenced.add(result.get(i) - result.get(i - 1));
            }
            result = differenced;
        }
        
        return result;
    }

    /**
     * 逆差分
     */
    private List<Double> reverseDifferencing(List<Double> differenced, List<Double> original, int d) {
        if (d == 0) {
            return new ArrayList<>(differenced);
        }

        List<Double> result = new ArrayList<>(differenced);
        
        for (int order = 0; order < d; order++) {
            List<Double> integrated = new ArrayList<>();
            double lastValue = original.get(original.size() - d + order);
            
            for (double diff : result) {
                double value = lastValue + diff;
                integrated.add(value);
                lastValue = value;
            }
            result = integrated;
        }
        
        return result;
    }

    /**
     * 拟合ARMA模型
     */
    private ArmaModel fitArma(List<Double> data, int p, int q) {
        if (data.size() < Math.max(p, q) + 1) {
            return null;
        }

        // 使用最小二乘法估计AR参数
        double[] arCoeffs = estimateArCoefficients(data, p);
        
        // 计算残差
        List<Double> residuals = calculateResiduals(data, arCoeffs, p);
        
        // 使用最小二乘法估计MA参数
        double[] maCoeffs = estimateMaCoefficients(residuals, q);
        
        // 计算均值
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return new ArmaModel(arCoeffs, maCoeffs, mean, residuals);
    }

    /**
     * 估计AR系数
     */
    private double[] estimateArCoefficients(List<Double> data, int p) {
        if (p == 0) {
            return new double[0];
        }

        int n = data.size();
        double[] coeffs = new double[p];
        
        // 使用Yule-Walker方程估计AR系数
        double[] acf = calculateAutocorrelation(data, p);
        
        // 简化实现：使用一阶AR
        if (p >= 1 && acf.length > 1) {
            coeffs[0] = acf[1];
        }
        
        // 高阶AR系数设为较小值
        for (int i = 1; i < p; i++) {
            coeffs[i] = acf[Math.min(i + 1, acf.length - 1)] * 0.5;
        }
        
        return coeffs;
    }

    /**
     * 计算自相关系数
     */
    private double[] calculateAutocorrelation(List<Double> data, int maxLag) {
        int n = data.size();
        double mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double[] acf = new double[maxLag + 1];
        
        // 计算方差
        double variance = 0;
        for (double value : data) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= n;
        
        if (variance == 0) {
            return acf;
        }
        
        // 计算自相关
        for (int lag = 0; lag <= maxLag && lag < n; lag++) {
            double sum = 0;
            for (int i = 0; i < n - lag; i++) {
                sum += (data.get(i) - mean) * (data.get(i + lag) - mean);
            }
            acf[lag] = sum / (n * variance);
        }
        
        return acf;
    }

    /**
     * 计算残差
     */
    private List<Double> calculateResiduals(List<Double> data, double[] arCoeffs, int p) {
        List<Double> residuals = new ArrayList<>();
        
        for (int i = p; i < data.size(); i++) {
            double predicted = 0;
            for (int j = 0; j < arCoeffs.length; j++) {
                predicted += arCoeffs[j] * data.get(i - j - 1);
            }
            residuals.add(data.get(i) - predicted);
        }
        
        return residuals;
    }

    /**
     * 估计MA系数
     */
    private double[] estimateMaCoefficients(List<Double> residuals, int q) {
        if (q == 0 || residuals.isEmpty()) {
            return new double[0];
        }

        double[] coeffs = new double[q];
        
        // 简化实现：使用残差的自相关
        double[] acf = calculateAutocorrelation(residuals, q);
        
        for (int i = 0; i < q && i + 1 < acf.length; i++) {
            coeffs[i] = -acf[i + 1] * 0.5;  // 负号是MA的特性
        }
        
        return coeffs;
    }

    /**
     * 使用ARMA模型预测
     */
    private List<Double> predictArma(ArmaModel model, List<Double> history, int periods) {
        List<Double> forecast = new ArrayList<>();
        List<Double> extended = new ArrayList<>(history);
        List<Double> errors = new ArrayList<>(model.residuals);
        
        for (int step = 0; step < periods; step++) {
            double prediction = model.mean;
            
            // AR部分
            for (int i = 0; i < model.arCoeffs.length; i++) {
                int index = extended.size() - i - 1;
                if (index >= 0) {
                    prediction += model.arCoeffs[i] * (extended.get(index) - model.mean);
                }
            }
            
            // MA部分（使用历史误差）
            for (int i = 0; i < model.maCoeffs.length; i++) {
                int index = errors.size() - i - 1;
                if (index >= 0) {
                    prediction += model.maCoeffs[i] * errors.get(index);
                }
            }
            
            forecast.add(prediction);
            extended.add(prediction);
            errors.add(0.0);  // 未来误差假设为0
        }
        
        return forecast;
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
     * ARMA模型数据结构
     */
    private static class ArmaModel {
        final double[] arCoeffs;
        final double[] maCoeffs;
        final double mean;
        final List<Double> residuals;

        ArmaModel(double[] arCoeffs, double[] maCoeffs, double mean, List<Double> residuals) {
            this.arCoeffs = arCoeffs;
            this.maCoeffs = maCoeffs;
            this.mean = mean;
            this.residuals = residuals;
        }
    }
}
