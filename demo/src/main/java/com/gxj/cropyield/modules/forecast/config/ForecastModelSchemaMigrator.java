package com.gxj.cropyield.modules.forecast.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 清理已经移除的预测模型类型，避免遗留数据阻塞应用启动。
 */
@Component
public class ForecastModelSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ForecastModelSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public ForecastModelSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int removed = jdbcTemplate.update(
                    "DELETE FROM forecast_model WHERE type NOT IN (?, ?)",
                    "LSTM",
                    "WEATHER_REGRESSION"
            );
            if (removed > 0) {
                log.info("Removed {} forecast models with deprecated types", removed);
            }
        } catch (DataAccessException ex) {
            log.warn("Unable to prune deprecated forecast models", ex);
        }
    }
}
