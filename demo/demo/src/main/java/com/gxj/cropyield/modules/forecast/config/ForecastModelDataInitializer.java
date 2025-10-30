package com.gxj.cropyield.modules.forecast.config;

import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
/**
 * 预测管理模块的配置类，配置预测管理相关的基础设施与框架行为。
 * <p>核心方法：run、ensureDefaultModel。</p>
 */

@Component
public class ForecastModelDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ForecastModelDataInitializer.class);

    private final ForecastModelRepository forecastModelRepository;

    public ForecastModelDataInitializer(ForecastModelRepository forecastModelRepository) {
        this.forecastModelRepository = forecastModelRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDefaultModel(
                "DeepLearning4j LSTM 产量模型",
                ForecastModel.ModelType.LSTM,
                "使用 DeepLearning4j 训练的 LSTM 神经网络，根据历史时间序列生成逐期产量预测。"
        );
        ensureDefaultModel(
                "Smile Holt-Winters 季节模型",
                ForecastModel.ModelType.PROPHET,
                "基于 Smile 库 Holt-Winters 平滑算法的季节性模型，支持趋势与季节性叠加预测。"
        );
        ensureDefaultModel(
                "天气因子多元回归模型",
                ForecastModel.ModelType.WEATHER_REGRESSION,
                "基于年度与关键生育期气象特征的多元线性回归模型，可区分夏收/秋收作物并结合天气趋势推演未来产量。"
        );
    }

    private void ensureDefaultModel(String name, ForecastModel.ModelType type, String description) {
        forecastModelRepository.findByType(type).or(() -> forecastModelRepository.findByName(name)).ifPresentOrElse(existing -> {
            boolean updated = false;
            if (!Objects.equals(existing.getName(), name)) {
                existing.setName(name);
                updated = true;
            }
            if (existing.getType() != type) {
                existing.setType(type);
                updated = true;
            }
            if (!Objects.equals(existing.getDescription(), description)) {
                existing.setDescription(description);
                updated = true;
            }
            if (updated) {
                forecastModelRepository.save(existing);
                log.info("Updated default forecast model [{}]", name);
            }
        }, () -> {
            ForecastModel model = new ForecastModel();
            model.setName(name);
            model.setType(type);
            model.setDescription(description);
            forecastModelRepository.save(model);
            log.info("Inserted default forecast model [{}]", name);
        });
    }
}
