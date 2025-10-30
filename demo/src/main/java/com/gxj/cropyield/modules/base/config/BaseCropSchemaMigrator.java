package com.gxj.cropyield.modules.base.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the legacy base_crop table carries the harvest_season column so the
 * seasonal regression features can load even when the database was created
 * before the column existed. The bootstrap runs on every startup and adds the
 * column with a safe default when missing.
 */
@Component
public class BaseCropSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BaseCropSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public BaseCropSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureHarvestSeasonColumn();
    }

    private void ensureHarvestSeasonColumn() {
        String tableName = "base_crop";
        String columnName = "harvest_season";
        if (columnExists(tableName, columnName)) {
            return;
        }
        String ddl = "ALTER TABLE " + tableName
                + " ADD COLUMN " + columnName
                + " VARCHAR(32) NOT NULL DEFAULT 'ANNUAL' AFTER category";
        executeDdl(tableName, "add harvest_season column", ddl);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private void executeDdl(String tableName, String description, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            log.info("{}: {} executed", tableName, description);
        } catch (DataAccessException ex) {
            log.warn("{}: failed to {} - {}", tableName, description, ex.getMessage());
        }
    }
}
