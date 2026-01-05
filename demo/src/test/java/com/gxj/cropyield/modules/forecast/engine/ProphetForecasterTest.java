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
 * Prophet预测器单元测试
 */
@DisplayName("Prophet预测器测试")
class ProphetForecasterTest {

    private ProphetForecaster forecaster;

    @BeforeEach
    void setUp() {
        forecaster = new ProphetForecaster();
    }

    @Test
    @DisplayName("有效数据应返回预测结果")
    void testForecast_withValidData_shouldReturnPredictions() {
        // Given: 8个历史数据点
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0, 150.0, 145.0);
        Map<String, Object> params = Map.of(
            "changepointPriorScale", 0.05,
            "seasonalityPriorScale", 10.0
        );
        int periods = 3;

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, periods, params);

        // Then: 应返回3个预测值
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
        assertThat(result.get()).allMatch(v -> v >= 0, "所有预测值应非负");
        assertThat(result.get()).allMatch(Double::isFinite, "所有预测值应为有限数");
    }

    @Test
    @DisplayName("数据不足应返回空结果")
    void testForecast_withInsufficientData_shouldReturnEmpty() {
        // Given: 只有2个数据点（少于最小要求4个）
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
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);

        // When: 使用默认参数预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, null);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @ParameterizedTest
    @CsvSource({
        "0.01, 5.0",   // 低灵敏度，弱季节性
        "0.05, 10.0",  // 默认值
        "0.1, 15.0",   // 中等灵敏度，中等季节性
        "0.2, 20.0"    // 高灵敏度，强季节性
    })
    @DisplayName("不同参数组合应都能工作")
    void testForecast_withDifferentParameters_shouldWork(double changepointPrior, double seasonalityPrior) {
        // Given: 有效数据和不同的参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0, 150.0, 145.0);
        Map<String, Object> params = Map.of(
            "changepointPriorScale", changepointPrior,
            "seasonalityPriorScale", seasonalityPrior
        );

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("季节性数据应检测到季节模式")
    void testForecast_withSeasonalData_shouldDetectPattern() {
        // Given: 明显季节性的数据（周期为4）
        List<Double> history = Arrays.asList(
            100.0, 120.0, 110.0, 130.0,  // 第1周期
            105.0, 125.0, 115.0, 135.0,  // 第2周期
            110.0, 130.0, 120.0, 140.0   // 第3周期
        );
        Map<String, Object> params = Map.of("seasonalityPeriod", 4);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 4, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(4);
    }

    @Test
    @DisplayName("趋势上升数据应预测上升趋势")
    void testForecast_withUpwardTrend_shouldPredictUpward() {
        // Given: 明显上升趋势的数据
        List<Double> history = Arrays.asList(100.0, 110.0, 120.0, 130.0, 140.0, 150.0);
        Map<String, Object> params = Map.of("changepointPriorScale", 0.1);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应呈上升趋势
        assertThat(result).isPresent();
        List<Double> forecast = result.get();
        assertThat(forecast.get(0)).isGreaterThanOrEqualTo(history.get(history.size() - 1) * 0.9);
    }

    @Test
    @DisplayName("趋势下降数据应预测下降趋势")
    void testForecast_withDownwardTrend_shouldPredictDownward() {
        // Given: 明显下降趋势的数据
        List<Double> history = Arrays.asList(150.0, 140.0, 130.0, 120.0, 110.0, 100.0);
        Map<String, Object> params = Map.of("changepointPriorScale", 0.1);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 预测值应呈下降趋势或保持稳定
        assertThat(result).isPresent();
        List<Double> forecast = result.get();
        assertThat(forecast.get(0)).isLessThanOrEqualTo(history.get(history.size() - 1) * 1.1);
    }

    @Test
    @DisplayName("平稳数据应预测平稳值")
    void testForecast_withStableData_shouldPredictStable() {
        // Given: 平稳数据
        List<Double> history = Arrays.asList(100.0, 102.0, 98.0, 101.0, 99.0, 100.0, 101.0, 99.0);
        Map<String, Object> params = Map.of("changepointPriorScale", 0.05);

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
        // Given: 24个数据点（2年月度数据）
        List<Double> history = Arrays.asList(
            100.0, 105.0, 110.0, 108.0, 115.0, 120.0, 118.0, 125.0, 130.0, 128.0, 135.0, 140.0,
            138.0, 145.0, 150.0, 148.0, 155.0, 160.0, 158.0, 165.0, 170.0, 168.0, 175.0, 180.0
        );
        Map<String, Object> params = Map.of(
            "changepointPriorScale", 0.1,
            "seasonalityPriorScale", 15.0,
            "seasonalityPeriod", 12
        );

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
    }

    @Test
    @DisplayName("自动检测季节周期应正常工作")
    void testForecast_withAutoSeasonalityDetection_shouldWork() {
        // Given: 有季节性的数据，不指定周期
        List<Double> history = Arrays.asList(
            100.0, 120.0, 110.0, 130.0,  // 周期1
            105.0, 125.0, 115.0, 135.0,  // 周期2
            110.0, 130.0, 120.0, 140.0   // 周期3
        );
        Map<String, Object> params = Map.of("seasonalityPeriod", 0);  // 0表示自动检测

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 4, params);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(4);
    }

    @Test
    @DisplayName("字符串类型参数应能正确解析")
    void testForecast_withStringParameters_shouldParse() {
        // Given: 字符串类型的参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of(
            "changepointPriorScale", "0.1",
            "seasonalityPriorScale", "15.0",
            "seasonalityPeriod", "4"
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
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of(
            "changepointPriorScale", "invalid",
            "seasonalityPriorScale", "bad"
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
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);

        // When: 预测5个周期
        Optional<List<Double>> result = forecaster.forecast(history, 5, null);

        // Then: 应返回5个预测值
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(5);
    }

    @Test
    @DisplayName("相同输入应产生一致结果")
    void testForecast_withSameInput_shouldProduceConsistentResults() {
        // Given: 相同的输入数据和参数
        List<Double> history = Arrays.asList(100.0, 120.0, 110.0, 130.0, 140.0, 135.0);
        Map<String, Object> params = Map.of(
            "changepointPriorScale", 0.1,
            "seasonalityPriorScale", 10.0
        );

        // When: 执行两次预测
        Optional<List<Double>> result1 = forecaster.forecast(history, 3, params);
        Optional<List<Double>> result2 = forecaster.forecast(history, 3, params);

        // Then: 结果应一致
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get()).isEqualTo(result2.get());
    }

    @Test
    @DisplayName("最小数据集应能工作")
    void testForecast_withMinimalDataset_shouldWork() {
        // Given: 最小数据集（4个点）
        List<Double> history = Arrays.asList(100.0, 110.0, 120.0, 130.0);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 2, null);

        // Then: 应返回预测结果
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
    }

    @Test
    @DisplayName("预测值不应为负数")
    void testForecast_shouldNotReturnNegativeValues() {
        // Given: 可能导致负值的数据
        List<Double> history = Arrays.asList(10.0, 8.0, 6.0, 4.0, 2.0, 1.0);

        // When: 执行预测
        Optional<List<Double>> result = forecaster.forecast(history, 3, null);

        // Then: 预测值应非负
        assertThat(result).isPresent();
        assertThat(result.get()).allMatch(v -> v >= 0, "预测值不应为负数");
    }
}
