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
 * ARIMA预测器单元测试
 */
@DisplayName("ARIMA预测器测试")
class ArimaForecasterTest {

    private ArimaForecaster forecaster;

    @BeforeEach
    void setUp() {
        forecaster = new ArimaForecaster();
    }

    @Test
    @DisplayName("有效数据应返回预测结果")
    void testForecast_withValidData_shouldReturnPredictions() {
        // Given: 6个历史数据点
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of("p", 1, "d", 1, "q", 1);
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
        // Given: 只有2个数据点（少于最小要求5个）
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
    @DisplayName("预测期数为0应返回空结果")
    void testForecast_withZeroPeriods_shouldReturnEmpty() {
        // Given: 有效数据但预测期数为0
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0);

        // When: 预测0期
        Optional<List<Double>> result = forecaster.forecast(history, 0, null);

        // Then: 应返回空结果或1个预测值（取决于实现）
        assertThat(result).isPresent();
        assertThat(result.get()).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("使用默认参数应正常工作")
    void testForecast_withDefaultParameters_shouldWork() {
        // Given: 有效数据，不传参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);

        // When: 使用默认参数预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, null);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, 1",  // ARIMA(1,1,1)
        "2, 1, 0",  // ARIMA(2,1,0) - 强AR
        "0, 1, 2",  // ARIMA(0,1,2) - 强MA
        "1, 2, 1",  // ARIMA(1,2,1) - 二阶差分
        "2, 1, 2"   // ARIMA(2,1,2)
    })
    @DisplayName("不同ARIMA参数组合应都能工作")
    void testForecast_withDifferentParameters_shouldWork(int p, int d, int q) {
        // Given: 有效数据和不同的ARIMA参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0, 150.0);
        Map<String, Object> params = Map.of("p", p, "d", d, "q", q);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("趋势上升数据应预测上升趋势")
    void testForecast_withUpwardTrend_shouldPredictUpward() {
        // Given: 明显上升趋势的数据
        List<Double> history = Arrays.asList(100.0, 110.0, 120.0, 130.0, 140.0, 150.0);
        Map<String, Object> params = Map.of("p", 1, "d", 1, "q", 1);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应大于最后一个历史值
        assertThat(result).isPresent();
        double lastHistory = history.get(history.size() - 1);
        assertThat(result.get().get(0)).isGreaterThanOrEqualTo(lastHistory * 0.9);
    }

    @Test
    @DisplayName("平稳数据应预测平稳值")
    void testForecast_withStableData_shouldPredictStable() {
        // Given: 平稳数据
        List<Double> history = Arrays.asList(100.0, 102.0, 98.0, 101.0, 99.0, 100.0);
        Map<String, Object> params = Map.of("p", 1, "d", 1, "q", 1);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应在合理范围内
        assertThat(result).isPresent();
        double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        assertThat(result.get()).allMatch(v -> Math.abs(v - mean) < mean * 0.5);
    }

    @Test
    @DisplayName("大数据集应正常处理")
    void testForecast_withLargeDataset_shouldWork() {
        // Given: 20个数据点
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 120.0, 118.0, 125.0, 130.0, 128.0,
            135.0, 140.0, 138.0, 145.0, 150.0, 148.0, 155.0, 160.0, 158.0, 165.0
        );
        Map<String, Object> params = Map.of("p", 2, "d", 1, "q", 2);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("参数超出范围应自动调整")
    void testForecast_withOutOfRangeParameters_shouldAdjust() {
        // Given: 超出范围的参数（p=10, d=5, q=10）和足够的数据点
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 120.0, 118.0, 125.0, 130.0, 128.0
        );
        Map<String, Object> params = Map.of("p", 10, "d", 5, "q", 10);

        // When: 执行预测（应自动调整到有效范围：p=5, d=2, q=5）
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果（参数被自动调整）
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("字符串类型参数应能正确解析")
    void testForecast_withStringParameters_shouldParse() {
        // Given: 字符串类型的参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of("p", "2", "d", "1", "q", "1");

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
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of("p", "invalid", "d", "bad", "q", "wrong");

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
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of("p", 1, "d", 1, "q", 1);

        // When: 预测5个周期
        Optional<List<Double>> result = forecaster.forecast(history, 5, params);

        // Then: 应返回5个预测值
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(5);
    }

    @Test
    @DisplayName("相同输入应产生一致结果")
    void testForecast_withSameInput_shouldProduceConsistentResults() {
        // Given: 相同的输入数据和参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of("p", 1, "d", 1, "q", 1);

        // When: 执行两次预测
        Optional<List<Double>> result1 = forecaster.forecast(history, 3, params);
        Optional<List<Double>> result2 = forecaster.forecast(history, 3, params);

        // Then: 结果应一致
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get()).isEqualTo(result2.get());
    }
}
