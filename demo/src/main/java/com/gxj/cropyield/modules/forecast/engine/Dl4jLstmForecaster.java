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
    private static final int MAX_WINDOW_SIZE = 12;
    private static final double EPSILON = 1e-9;
    private static final double DEFAULT_LEARNING_RATE = 0.01;
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

        int windowSize = Math.min(MAX_WINDOW_SIZE, Math.max(MIN_WINDOW_SIZE, historyValues.size() / 2));
        if (windowSize >= historyValues.size()) {
            windowSize = historyValues.size() - 1;
        }
        if (windowSize < MIN_WINDOW_SIZE) {
            return Optional.empty();
        }

        double[] scaledSeries = historyValues.stream()
            .mapToDouble(v -> (v - min) / range)
            .toArray();
        int sampleCount = scaledSeries.length - windowSize;
        if (sampleCount <= 0) {
            return Optional.empty();
        }

        DataSet trainingData = buildTrainingSet(scaledSeries, windowSize, sampleCount);
        
        // Extract parameters
        double learningRate = extractDoubleParameter(parameters, "learningRate", DEFAULT_LEARNING_RATE);
        int seed = extractIntParameter(parameters, "seed", DEFAULT_SEED);
        Integer epochsParam = extractIntParameter(parameters, "epochs", null);
        
        // Set random seed if provided
        if (parameters != null && parameters.containsKey("seed")) {
            Nd4j.getRandom().setSeed(seed);
        }
        
        MultiLayerNetwork network = buildNetwork(learningRate, seed);
        network.init();
        network.setListeners(new ScoreIterationListener(Math.max(10, sampleCount)));
        
        // Use epochs from parameters if provided, otherwise use calculated value
        int epochs = epochsParam != null ? epochsParam : Math.max(40, Math.min(200, sampleCount * 15));
        for (int epoch = 0; epoch < epochs; epoch++) {
            network.fit(trainingData);
        }

        List<Double> normalizedWorking = new ArrayList<>();
        for (double value : scaledSeries) {
            normalizedWorking.add(value);
        }
        List<Double> forecasts = new ArrayList<>();
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
        return buildNetwork(DEFAULT_LEARNING_RATE, DEFAULT_SEED);
    }

    private MultiLayerNetwork buildNetwork(double learningRate, int seed) {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(learningRate))
            .list(
                new LSTM.Builder()
                    .activation(Activation.TANH)
                    .nIn(1)
                    .nOut(32)
                    .build(),
                new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(32)
                    .nOut(1)
                    .build()
            )
            .build();
        return new MultiLayerNetwork(configuration);
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
