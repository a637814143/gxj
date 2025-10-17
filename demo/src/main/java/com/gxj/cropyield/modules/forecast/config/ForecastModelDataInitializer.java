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
                "LSTM作物产量模型",
                ForecastModel.ModelType.LSTM,
                "基于 10 年历史数据训练的 LSTM 年度产量预测模型。"
        );
        ensureDefaultModel(
                "Prophet季节性模型",
                ForecastModel.ModelType.PROPHET,
                "捕捉季节波动的 Prophet 模型，适用于价格预测。"
        );
    }

    private void ensureDefaultModel(String name, ForecastModel.ModelType type, String description) {
        forecastModelRepository.findByName(name).ifPresentOrElse(existing -> {
            boolean updated = false;
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
