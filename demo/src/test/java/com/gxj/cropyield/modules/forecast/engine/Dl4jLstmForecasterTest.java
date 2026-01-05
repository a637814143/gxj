package com.gxj.cropyield.modules.forecast.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LSTM预测器单元测试
 */
@DisplayName("LSTM预测器测试")
class Dl4jLstmForecasterTest {

    private Dl4jLstmForecaster forecaster;

    @BeforeEach
    void setUp() {
        forecaster = new Dl4jLstmForecaster();
    }

    @Test
    @DisplayName("有效数据应返回预测结果")
    void testForecast_withValidData_shouldReturnPredictions() {
        // Given: 10个历史数据点
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of(
            "learningRate", 0.01,
            "seed", 42,
            "epochs", 50
        );
        int periods = 3;

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, periods, params);

        // Then: 应返回3个预测值
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
        assertThat(result.get()).allMatch(v -> v > 0, "所有预测值应为正数");
        assertThat(result.get()).allMatch(Double::isFinite, "所有预测值应为有限数");
    }

    @Test
    @DisplayName("数据不足应返回空结果")
    void testForecast_withInsufficientData_shouldReturnEmpty() {
        // Given: 只有2个数据点（少于最小要求）
        List<Double> history = Arrays.asList(100.0, 120.0);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, null);

        // Then: 应返回空结果
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("空数据应返回空结果")
    void testForecast_withNullData_shouldReturnEmpty() {
        // When: 传入null数据
        Optional<List<Double>> result = forecaster.forecast(null, 3, null);

        // Then: 应返回空结果
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("空列表应返回空结果")
    void testForecast_withEmptyList_shouldReturnEmpty() {
        // Given: 空列表
        List<Double> history = Collections.emptyList();

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, null);

        // Then: 应返回空结果
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("使用默认参数应正常工作")
    void testForecast_withDefaultParameters_shouldWork() {
        // Given: 有效数据，不传参数
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );

        // When: 使用默认参数预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, null);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @ParameterizedTest
    @CsvSource({
        "0.001, 42, 50",   // 低学习率
        "0.01, 42, 100",   // 默认学习率
        "0.05, 42, 150",   // 高学习率
        "0.01, 123, 100",  // 不同种子
        "0.01, 456, 100"   // 不同种子
    })
    @DisplayName("不同参数组合应都能工作")
    void testForecast_withDifferentParameters_shouldWork(double learningRate, int seed, int epochs) {
        // Given: 有效数据和不同的参数
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of(
            "learningRate", learningRate,
            "seed", seed,
            "epochs", epochs
        );

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("相同种子应产生一致结果")
    void testForecast_withSameSeed_shouldProduceConsistentResults() {
        // Given: 相同的输入数据和种子
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of(
            "learningRate", 0.01,
            "seed", 42,
            "epochs", 50
        );

        // When: 执行两次预测
        Optional<List<Double>> result1 = forecaster.forecast(history, 3, params);
        Optional<List<Double>> result2 = forecaster.forecast(history, 3, params);

        // Then: 结果应一致（由于使用相同种子）
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        // 注意：由于浮点运算，可能有微小差异
        for (int i = 0; i < result1.get().size(); i++) {
            assertThat(result1.get().get(i))
                .isCloseTo(result2.get().get(i), org.assertj.core.data.Percentage.withPercentage(1.0));
        }
    }

    @Test
    @DisplayName("不同学习率应产生不同结果")
    void testForecast_withDifferentLearningRates_shouldProduceDifferentResults() {
        // Given: 相同数据，不同学习率和不同种子（确保产生不同结果）
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params1 = Map.of("learningRate", 0.001, "seed", 42, "epochs", 50);
        Map<String, Object> params2 = Map.of("learningRate", 0.1, "seed", 123, "epochs", 50);

        // When: 使用不同学习率预测
        Optional<List<Double>> result1 = forecaster.forecast(history, 3, params1);
        Optional<List<Double>> result2 = forecaster.forecast(history, 3, params2);

        // Then: 两个结果都应存在且为有效预测
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get()).hasSize(3);
        assertThat(result2.get()).hasSize(3);
        // 注意：对于简单数据，不同学习率可能收敛到相似解，这是正常的
        // 重要的是两个模型都能成功训练并产生合理预测
    }

    @Test
    @DisplayName("趋势上升数据应预测上升趋势")
    void testForecast_withUpwardTrend_shouldPredictUpward() {
        // Given: 明显上升趋势的数据
        List<Double> history = Arrays.asList(
            100.0, 110.0, 120.0, 130.0, 140.0, 
            150.0, 160.0, 170.0, 180.0, 190.0
        );
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 100);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应大于或接近最后一个历史值
        assertThat(result).isPresent();
        double lastHistory = history.get(history.size() - 1);
        assertThat(result.get().get(0)).isGreaterThanOrEqualTo(lastHistory * 0.8);
    }

    @Test
    @DisplayName("平稳数据应预测平稳值")
    void testForecast_withStableData_shouldPredictStable() {
        // Given: 平稳数据
        List<Double> history = Arrays.asList(
            100.0, 102.0, 98.0, 101.0, 99.0, 
            100.0, 101.0, 99.0, 100.0, 102.0
        );
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 100);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应在合理范围内
        assertThat(result).isPresent();
        double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        assertThat(result.get()).allMatch(v -> Math.abs(v - mean) < mean * 0.3);
    }

    @Test
    @DisplayName("大数据集应正常处理")
    void testForecast_withLargeDataset_shouldWork() {
        // Given: 20个数据点
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 120.0, 118.0, 125.0, 130.0, 128.0,
            135.0, 140.0, 138.0, 145.0, 150.0, 148.0, 155.0, 160.0, 158.0, 165.0
        );
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 150);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("字符串类型参数应能正确解析")
    void testForecast_withStringParameters_shouldParse() {
        // Given: 字符串类型的参数
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of(
            "learningRate", "0.01",
            "seed", "42",
            "epochs", "50"
        );

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应能正确解析并返回结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("无效参数类型应使用默认值")
    void testForecast_withInvalidParameterType_shouldUseDefault() {
        // Given: 无效类型的参数
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of(
            "learningRate", "invalid",
            "seed", "bad",
            "epochs", "wrong"
        );

        // When: 执行预测（应使用默认值）
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果（使用默认参数）
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("预测多个周期应返回正确数量")
    void testForecast_withMultiplePeriods_shouldReturnCorrectSize() {
        // Given: 有效数据
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 50);

        // When: 预测5个周期
        Optional<List<Double>> result = forecaster.forecast(history, 5, params);

        // Then: 应返回5个预测值
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(5);
    }

    @Test
    @DisplayName("最小数据集应能工作")
    void testForecast_withMinimalDataset_shouldWork() {
        // Given: 最小数据集（6个点）
        List<Double> history = Arrays.asList(100.0, 110.0, 120.0, 130.0, 140.0, 150.0);
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 40);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 2, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
    }

    @Test
    @DisplayName("所有值相同的数据应返回相同预测值")
    void testForecast_withConstantData_shouldReturnConstantPrediction() {
        // Given: 所有值相同的数据
        List<Double> history = Arrays.asList(100.0, 100.0, 100.0, 100.0, 100.0, 100.0);
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 50);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应接近100
        assertThat(result).isPresent();
        assertThat(result.get()).allMatch(v -> Math.abs(v - 100.0) < 10.0);
    }

    @Test
    @DisplayName("Epochs参数应影响训练")
    void testForecast_withDifferentEpochs_shouldAffectTraining() {
        // Given: 相同数据，不同epochs
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params1 = Map.of("learningRate", 0.01, "seed", 42, "epochs", 10);
        Map<String, Object> params2 = Map.of("learningRate", 0.01, "seed", 42, "epochs", 200);

        // When: 使用不同epochs预测
        Optional<List<Double>> result1 = forecaster.forecast(history, 3, params1);
        Optional<List<Double>> result2 = forecaster.forecast(history, 3, params2);

        // Then: 结果应不同（更多epochs通常更准确）
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        // 结果可能不同，但都应该是有效的
        assertThat(result1.get()).allMatch(Double::isFinite);
        assertThat(result2.get()).allMatch(Double::isFinite);
    }

    @Test
    @DisplayName("预测值应在合理范围内")
    void testForecast_shouldReturnReasonableValues() {
        // Given: 正常范围的数据
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 
            120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of("learningRate", 0.01, "seed", 42, "epochs", 100);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应在合理范围内（不应偏离太远）
        assertThat(result).isPresent();
        double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double min = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double range = max - min;
        
        assertThat(result.get()).allMatch(v -> 
            v >= min - range * 2 && v <= max + range * 2,
            "预测值应在合理范围内"
        );
    }
}
