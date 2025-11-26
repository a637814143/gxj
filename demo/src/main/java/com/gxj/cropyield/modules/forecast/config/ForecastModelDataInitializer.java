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
                ForecastModel.ModelType.MACHINE_LEARNING,
                "使用 DeepLearning4j 训练的 LSTM 神经网络，根据历史时间序列生成逐期产量预测。",
                ForecastModel.Granularity.QUARTERLY,
                8,
                2,
                "默认隐藏层 64，学习率 0.001"
        );
        ensureDefaultModel(
                "天气因子多元回归模型",
                ForecastModel.ModelType.TIME_SERIES,
                "结合逐年气象特征与历史产量进行多元线性回归，量化天气变化对产量的影响。",
                ForecastModel.Granularity.YEARLY,
                5,
                1,
                "特征标准化，岭回归系数 0.15"
        );
    }

    private void ensureDefaultModel(String name,
                                    ForecastModel.ModelType type,
                                    String description,
                                    ForecastModel.Granularity granularity,
                                    int historyWindow,
                                    int forecastHorizon,
                                    String hyperParameters) {
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
            if (existing.getGranularity() != granularity) {
                existing.setGranularity(granularity);
                updated = true;
            }
            if (!Objects.equals(existing.getHistoryWindow(), historyWindow)) {
                existing.setHistoryWindow(historyWindow);
                updated = true;
            }
            if (!Objects.equals(existing.getForecastHorizon(), forecastHorizon)) {
                existing.setForecastHorizon(forecastHorizon);
                updated = true;
            }
            if (!Objects.equals(existing.getHyperParameters(), hyperParameters)) {
                existing.setHyperParameters(hyperParameters);
                updated = true;
            }
            if (existing.getCropScope() == null) {
                existing.setCropScope("全部作物");
                updated = true;
            }
            if (existing.getRegionScope() == null) {
                existing.setRegionScope("全部区域");
                updated = true;
            }
            if (existing.getEnabled() == null) {
                existing.setEnabled(Boolean.TRUE);
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
            model.setGranularity(granularity);
            model.setHistoryWindow(historyWindow);
            model.setForecastHorizon(forecastHorizon);
            model.setHyperParameters(hyperParameters);
            model.setCropScope("全部作物");
            model.setRegionScope("全部区域");
            model.setEnabled(Boolean.TRUE);
            forecastModelRepository.save(model);
            log.info("Inserted default forecast model [{}]", name);
        });
    }
}
