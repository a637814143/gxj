package com.gxj.cropyield.modules.forecast.engine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper around Smile's Holt-Winters implementation with an additive fallback
 * in case the runtime dependency is not available.
 */
class SmileHoltWintersForecaster {

    private static final double DEFAULT_ALPHA = 0.3;
    private static final double DEFAULT_BETA = 0.1;
    private static final double DEFAULT_GAMMA = 0.3;

    Optional<List<Double>> forecast(List<Double> historyValues, int periods) {
        if (historyValues == null || historyValues.size() < 3) {
            return Optional.empty();
        }
        if (periods <= 0) {
            periods = 1;
        }
        try {
            return Optional.of(runSmileHoltWinters(historyValues, periods));
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            return Optional.of(fallbackAdditive(historyValues, periods));
        }
    }

    private List<Double> runSmileHoltWinters(List<Double> historyValues, int periods)
        throws ReflectiveOperationException {
        Class<?> smoothingClass = Class.forName("smile.timeseries.ExponentialSmoothing");
        int seasonLength = guessSeasonLength(historyValues.size());
        double[] data = historyValues.stream().mapToDouble(Double::doubleValue).toArray();
        Method fitMethod = findFitMethod(smoothingClass);
        Object model;
        if (fitMethod.getParameterCount() == 2) {
            model = fitMethod.invoke(null, data, seasonLength);
        } else if (fitMethod.getParameterCount() == 3) {
            model = fitMethod.invoke(null, data, seasonLength, true);
        } else {
            throw new NoSuchMethodException("Unsupported ExponentialSmoothing.fit signature");
        }
        Method forecastMethod = model.getClass().getMethod("forecast", int.class);
        double[] forecast = (double[]) forecastMethod.invoke(model, periods);
        List<Double> results = new ArrayList<>(forecast.length);
        for (double value : forecast) {
            results.add(value);
        }
        return results;
    }

    private Method findFitMethod(Class<?> smoothingClass) throws NoSuchMethodException {
        for (Method method : smoothingClass.getMethods()) {
            if (!"fit".equals(method.getName())) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length >= 2 && types[0].isArray() && types[0].getComponentType() == double.class
                && types[1] == int.class) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("fit(double[], int[, ...]) not found on Smile ExponentialSmoothing");
    }

    private List<Double> fallbackAdditive(List<Double> historyValues, int periods) {
        int n = historyValues.size();
        int seasonLength = guessSeasonLength(n);
        double level = historyValues.get(0);
        double trend = historyValues.get(Math.min(1, n - 1)) - historyValues.get(0);
        double[] seasonal = initialSeasonalComponents(historyValues, seasonLength, level);
        for (int i = 0; i < n; i++) {
            double value = historyValues.get(i);
            int index = i % seasonLength;
            double prevLevel = level;
            level = DEFAULT_ALPHA * (value - seasonal[index]) + (1 - DEFAULT_ALPHA) * (level + trend);
            trend = DEFAULT_BETA * (level - prevLevel) + (1 - DEFAULT_BETA) * trend;
            seasonal[index] = DEFAULT_GAMMA * (value - level) + (1 - DEFAULT_GAMMA) * seasonal[index];
        }
        List<Double> results = new ArrayList<>(periods);
        for (int i = 1; i <= periods; i++) {
            int index = (n + i - 1) % seasonLength;
            double forecast = level + trend * i + seasonal[index];
            if (!Double.isFinite(forecast)) {
                forecast = historyValues.get(n - 1);
            }
            results.add(forecast);
        }
        return results;
    }

    private double[] initialSeasonalComponents(List<Double> historyValues, int seasonLength, double baseLevel) {
        double[] seasonal = new double[seasonLength];
        for (int i = 0; i < seasonLength; i++) {
            double sum = 0d;
            int count = 0;
            for (int j = i; j < historyValues.size(); j += seasonLength) {
                sum += historyValues.get(j);
                count++;
            }
            double mean = count > 0 ? sum / count : 0d;
            seasonal[i] = mean - baseLevel;
        }
        return seasonal;
    }

    private int guessSeasonLength(int size) {
        if (size <= 2) {
            return Math.max(2, size);
        }
        int capped = Math.min(12, size / 2);
        return Math.max(2, capped);
    }
}
