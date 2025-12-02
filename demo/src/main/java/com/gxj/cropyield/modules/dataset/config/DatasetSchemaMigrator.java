package com.gxj.cropyield.modules.dataset.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 数据集管理模块的配置类，配置数据集管理相关的基础设施与框架行为。
 * <p>核心方法：run、ensureDatasetFileColumn、ensureImportJobColumn、columnExists、indexExists、foreignKeyExists、executeDdl。</p>
 */

@Component
public class DatasetSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatasetSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public DatasetSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureDatasetFileTable();
        ensureDatasetFileColumn("dataset_yield_record", "idx_yield_dataset_file", "fk_yield_dataset_file");
        ensureDatasetFileColumn("dataset_weather_record", "idx_weather_dataset_file", "fk_weather_dataset_file");
        ensureImportJobColumn();
    }

    private void ensureDatasetFileTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS dataset_file (" +
                " id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY," +
                " name VARCHAR(128) NOT NULL," +
                " type VARCHAR(32) NOT NULL," +
                " storage_path VARCHAR(256) NOT NULL," +
                " description VARCHAR(256)," +
                " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                " updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                " UNIQUE KEY uq_dataset_file_name (name)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '原始数据文件登记'";
        executeDdl("dataset_file", "create dataset_file table", ddl);
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

    private void ensureImportJobColumn() {
        String tableName = "data_import_job";
        if (!columnExists(tableName, "dataset_file_id")) {
            String ddl = "ALTER TABLE " + tableName +
                    " ADD COLUMN dataset_file_id BIGINT UNSIGNED NULL AFTER dataset_type";
            executeDdl(tableName, "add dataset_file_id column", ddl);
        }
        if (!indexExists(tableName, "idx_import_dataset_file")) {
            String ddl = "ALTER TABLE " + tableName +
                    " ADD INDEX idx_import_dataset_file (dataset_file_id)";
            executeDdl(tableName, "add dataset_file index", ddl);
        }
        if (!foreignKeyExists(tableName, "fk_import_dataset_file")) {
            String ddl = "ALTER TABLE " + tableName +
                    " ADD CONSTRAINT fk_import_dataset_file FOREIGN KEY (dataset_file_id) REFERENCES dataset_file (id) ON DELETE SET NULL";
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
