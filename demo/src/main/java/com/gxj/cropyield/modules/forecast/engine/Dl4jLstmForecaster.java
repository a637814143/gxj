package com.gxj.cropyield.modules.forecast.engine;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
 * 预测管理模块的业务组件，封装预测管理的算法或执行流程。
 */

class Dl4jLstmForecaster {

    private static final int MIN_WINDOW_SIZE = 2;
    private static final int MAX_WINDOW_SIZE = 10;  // 降低到10
    private static final double EPSILON = 1e-9;
    private static final double DEFAULT_LEARNING_RATE = 0.001;  // 提高学习率加快收敛
    private static final int DEFAULT_HIDDEN_SIZE = 32;  // 降低到32，减少计算量
    private static final int DEFAULT_EPOCHS = 20;  // 大幅降低到20
    private static final double DEFAULT_DROPOUT = 0.1;  // 降低dropout
    private static final int DEFAULT_SEED = 42;

    Optional<List<Double>> forecast(List<Double> historyValues, int periods) {
        return forecast(historyValues, periods, null);
    }

    Optional<List<Double>> forecast(List<Double> historyValues, int periods, Map<String, Object> parameters) {
        if (historyValues == null || historyValues.size() <= MIN_WINDOW_SIZE) {
            return Optional.empty();
        }
        if (periods <= 0) {
            periods = 1;
        }

        double min = historyValues.stream().mapToDouble(Double::doubleValue).min().orElse(0d);
        double max = historyValues.stream().mapToDouble(Double::doubleValue).max().orElse(min);
        double range = Math.max(max - min, EPSILON);
        if (range <= EPSILON) {
            return Optional.of(repeat(historyValues.get(historyValues.size() - 1), periods));
        }

        // 改进窗口大小计算：更保守的策略
        int windowSize = Math.min(MAX_WINDOW_SIZE, Math.max(MIN_WINDOW_SIZE, historyValues.size() / 3));
        if (windowSize >= historyValues.size()) {
            windowSize = Math.max(MIN_WINDOW_SIZE, historyValues.size() - 1);
        }
        if (windowSize < MIN_WINDOW_SIZE) {
            return Optional.empty();
        }

        // 使用更稳健的归一化方法（Min-Max归一化）
        double[] scaledSeries = historyValues.stream()
            .mapToDouble(v -> (v - min) / range)
            .toArray();
        int sampleCount = scaledSeries.length - windowSize;
        if (sampleCount <= 0) {
            return Optional.empty();
        }

        DataSet trainingData = buildTrainingSet(scaledSeries, windowSize, sampleCount);
        
        // Extract parameters with improved defaults
        double learningRate = extractDoubleParameter(parameters, "learningRate", DEFAULT_LEARNING_RATE);
        int hiddenSize = extractIntParameter(parameters, "hiddenSize", DEFAULT_HIDDEN_SIZE);
        double dropout = extractDoubleParameter(parameters, "dropout", DEFAULT_DROPOUT);
        int seed = extractIntParameter(parameters, "seed", DEFAULT_SEED);
        Integer epochsParam = extractIntParameter(parameters, "epochs", null);
        
        // Set random seed if provided
        if (parameters != null && parameters.containsKey("seed")) {
            Nd4j.getRandom().setSeed(seed);
        }
        
        MultiLayerNetwork network = buildNetwork(learningRate, hiddenSize, dropout, seed);
        network.init();
        network.setListeners(new ScoreIterationListener(Math.max(10, sampleCount)));
        
        // 改进的epoch计算：基于数据量和样本数
        int epochs = epochsParam != null ? epochsParam : calculateOptimalEpochs(sampleCount, historyValues.size());
        
        // 训练模型，添加早停机制
        double previousScore = Double.MAX_VALUE;
        int noImprovementCount = 0;
        for (int epoch = 0; epoch < epochs; epoch++) {
            network.fit(trainingData);
            double currentScore = network.score();
            
            // 早停：如果连续3轮没有改善，提前停止
            if (epoch > 5 && currentScore >= previousScore * 0.999) {
                noImprovementCount++;
                if (noImprovementCount >= 3) {
                    break;
                }
            } else {
                noImprovementCount = 0;
            }
            previousScore = currentScore;
        }

        // 生成预测值，添加平滑处理
        List<Double> normalizedWorking = new ArrayList<>();
        for (double value : scaledSeries) {
            normalizedWorking.add(value);
        }
        List<Double> forecasts = new ArrayList<>();
        double lastActual = historyValues.get(historyValues.size() - 1);
        
        for (int step = 0; step < periods; step++) {
            double[] window = new double[windowSize];
            for (int i = 0; i < windowSize; i++) {
                window[i] = normalizedWorking.get(normalizedWorking.size() - windowSize + i);
            }
            double prediction = predict(network, window);
            if (!Double.isFinite(prediction)) {
                return Optional.empty();
            }
            prediction = clamp(prediction, 0d, 1d);
            double denormalized = prediction * range + min;
            if (!Double.isFinite(denormalized)) {
                return Optional.empty();
            }
            
            // 添加平滑处理：限制预测值的变化幅度
            if (step == 0) {
                // 第一个预测值不应该与最后一个实际值相差太大
                double maxChange = range * 0.3; // 最多变化30%的范围
                if (Math.abs(denormalized - lastActual) > maxChange) {
                    denormalized = lastActual + Math.signum(denormalized - lastActual) * maxChange;
                }
            } else {
                // 后续预测值不应该与前一个预测值相差太大
                double maxChange = range * 0.2;
                double lastForecast = forecasts.get(forecasts.size() - 1);
                if (Math.abs(denormalized - lastForecast) > maxChange) {
                    denormalized = lastForecast + Math.signum(denormalized - lastForecast) * maxChange;
                }
            }
            
            forecasts.add(denormalized);
            normalizedWorking.add(prediction);
        }
        return Optional.of(forecasts);
    }

    private DataSet buildTrainingSet(double[] scaledSeries, int windowSize, int sampleCount) {
        var features = Nd4j.create(sampleCount, 1, windowSize);
        var labels = Nd4j.create(sampleCount, 1, windowSize);
        var labelMask = Nd4j.zeros(sampleCount, windowSize);
        for (int i = 0; i < sampleCount; i++) {
            for (int j = 0; j < windowSize; j++) {
                features.putScalar(new int[]{i, 0, j}, scaledSeries[i + j]);
            }
            labels.putScalar(new int[]{i, 0, windowSize - 1}, scaledSeries[i + windowSize]);
            labelMask.putScalar(new int[]{i, windowSize - 1}, 1d);
        }
        DataSet dataSet = new DataSet(features, labels);
        dataSet.setLabelsMaskArray(labelMask);
        return dataSet;
    }

    private MultiLayerNetwork buildNetwork() {
        return buildNetwork(DEFAULT_LEARNING_RATE, DEFAULT_HIDDEN_SIZE, DEFAULT_DROPOUT, DEFAULT_SEED);
    }

    private MultiLayerNetwork buildNetwork(double learningRate, int seed) {
        return buildNetwork(learningRate, DEFAULT_HIDDEN_SIZE, DEFAULT_DROPOUT, seed);
    }

    private MultiLayerNetwork buildNetwork(double learningRate, int hiddenSize, double dropout, int seed) {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(learningRate))
            .list(
                new LSTM.Builder()
                    .activation(Activation.TANH)
                    .nIn(1)
                    .nOut(hiddenSize)  // 使用可配置的隐藏层大小
                    .dropOut(dropout)  // 添加dropout防止过拟合
                    .build(),
                // 移除第二层LSTM，加快训练速度
                new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(hiddenSize)  // 直接从第一层连接
                    .nOut(1)
                    .build()
            )
            .build();
        return new MultiLayerNetwork(configuration);
    }
    
    /**
     * 计算最优训练轮数
     * 改进版：考虑数据量和样本数
     */
    private int calculateOptimalEpochs(int sampleCount, int totalDataPoints) {
        // 数据越少，需要更多epochs来学习模式
        // 但要避免过拟合，所以设置上限
        if (sampleCount < 5) {
            return 25;  // 极少样本，需要更多训练
        } else if (sampleCount < 10) {
            return 20;
        } else if (sampleCount < 20) {
            return 18;
        } else {
            return 15;  // 大数据集用较少epochs避免过拟合
        }
    }
    
    /**
     * 重载方法保持向后兼容
     */
    private int calculateOptimalEpochs(int sampleCount) {
        return calculateOptimalEpochs(sampleCount, sampleCount + MAX_WINDOW_SIZE);
    }

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

    private Integer extractIntParameter(Map<String, Object> parameters, String key, Integer defaultValue) {
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

    private double predict(MultiLayerNetwork network, double[] window) {
        var input = Nd4j.create(1, 1, window.length);
        for (int i = 0; i < window.length; i++) {
            input.putScalar(new int[]{0, 0, i}, window[i]);
        }
        var output = network.output(input, false);
        return output.getDouble(0, 0, window.length - 1);
    }

    private List<Double> repeat(double value, int periods) {
        List<Double> results = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            results.add(value);
        }
        return results;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
