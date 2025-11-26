package com.gxj.cropyield.modules.forecast.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 清理并补全预测模型表结构，兼容历史的类型值与新增约束。
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
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS enabled TINYINT(1) NOT NULL DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS granularity VARCHAR(32) NOT NULL DEFAULT 'YEARLY'");
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS history_window INT NOT NULL DEFAULT 5");
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS forecast_horizon INT NOT NULL DEFAULT 1");
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS crop_scope VARCHAR(128) NOT NULL DEFAULT '全部作物'");
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS region_scope VARCHAR(128) NOT NULL DEFAULT '全部区域'");
            jdbcTemplate.execute("ALTER TABLE forecast_model ADD COLUMN IF NOT EXISTS hyper_parameters VARCHAR(512)");
        } catch (DataAccessException ex) {
            log.warn("Unable to ensure forecast_model columns", ex);
        }

        try {
            int migratedLstm = jdbcTemplate.update("UPDATE forecast_model SET type = 'MACHINE_LEARNING' WHERE type = 'LSTM'");
            int migratedWeather = jdbcTemplate.update("UPDATE forecast_model SET type = 'TIME_SERIES' WHERE type = 'WEATHER_REGRESSION'");
            int filledEnabled = jdbcTemplate.update("UPDATE forecast_model SET enabled = 1 WHERE enabled IS NULL");
            if (migratedLstm + migratedWeather + filledEnabled > 0) {
                log.info("Normalized forecast_model data. LSTM->{}, WEATHER->{}, enabledFilled:{}", migratedLstm, migratedWeather, filledEnabled);
            }
        } catch (DataAccessException ex) {
            log.warn("Unable to normalize forecast_model data", ex);
        }
    }
}
