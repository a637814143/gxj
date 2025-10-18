package com.gxj.cropyield.modules.dataset.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatasetSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatasetSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public DatasetSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureDatasetFileColumn("dataset_yield_record", "idx_yield_dataset_file", "fk_yield_dataset_file");
        ensureDatasetFileColumn("dataset_price_record", "idx_price_dataset_file", "fk_price_dataset_file");
    }

    private void ensureDatasetFileColumn(String tableName, String indexName, String constraintName) {
        if (!columnExists(tableName, "dataset_file_id")) {
            String ddl = "ALTER TABLE " + tableName +
                    " ADD COLUMN dataset_file_id BIGINT UNSIGNED NULL AFTER id";
            executeDdl(tableName, "add dataset_file_id column", ddl);
        }
        if (!indexExists(tableName, indexName)) {
            String ddl = "ALTER TABLE " + tableName +
                    " ADD INDEX " + indexName + " (dataset_file_id)";
            executeDdl(tableName, "add dataset_file_id index", ddl);
        }
        if (!foreignKeyExists(tableName, constraintName)) {
            String ddl = "ALTER TABLE " + tableName +
                    " ADD CONSTRAINT " + constraintName +
                    " FOREIGN KEY (dataset_file_id) REFERENCES dataset_file (id) ON DELETE CASCADE";
            executeDdl(tableName, "add dataset_file foreign key", ddl);
        }
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

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    private boolean foreignKeyExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?",
                Integer.class,
                tableName,
                constraintName
        );
        return count != null && count > 0;
    }

    private void executeDdl(String tableName, String description, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            log.info("{}: {} executed", tableName, description);
        } catch (DataAccessException ex) {
            log.warn("Failed to {} for table {}: {}", description, tableName, ex.getMessage());
        }
    }
}
