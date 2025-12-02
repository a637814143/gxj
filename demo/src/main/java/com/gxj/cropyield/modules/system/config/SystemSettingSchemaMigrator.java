package com.gxj.cropyield.modules.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 系统设置模块的配置类，配置系统设置相关的基础设施与框架行为。
 * <p>核心方法：run、tableExists、createTable、ensureColumn、columnExists、ensureForeignKey、foreignKeyExists、executeDdl。</p>
 */

@Component
public class SystemSettingSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public SystemSettingSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            createTable();
        }

        ensureColumn("default_region_id", "BIGINT UNSIGNED NULL AFTER id");
        ensureColumn("notify_email", "VARCHAR(128) NULL AFTER default_region_id");
        ensureColumn("cluster_enabled", "TINYINT(1) NOT NULL DEFAULT 1 AFTER notify_email");
        ensureColumn("pending_change_count", "INT NOT NULL DEFAULT 0 AFTER cluster_enabled");
        ensureColumn("security_strategy", "VARCHAR(64) NULL AFTER pending_change_count");
        ensureColumn("public_visible", "TINYINT(1) NOT NULL DEFAULT 0 AFTER security_strategy");
        ensureColumn("public_title", "VARCHAR(128) NULL AFTER public_visible");
        ensureColumn("public_summary", "VARCHAR(512) NULL AFTER public_title");
        ensureColumn("public_audience", "VARCHAR(64) NULL AFTER public_summary");
        ensureColumn("public_level", "VARCHAR(32) NULL AFTER public_audience");
        ensureColumn("public_start_at", "DATETIME NULL AFTER public_level");
        ensureColumn("public_end_at", "DATETIME NULL AFTER public_start_at");
        ensureForeignKey();
    }

    private boolean tableExists() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_setting'",
            Integer.class
        );
        return count != null && count > 0;
    }

    private void createTable() {
        String ddl = "CREATE TABLE system_setting ("
            + " id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
            + " default_region_id BIGINT UNSIGNED NULL,"
            + " notify_email VARCHAR(128),"
            + " cluster_enabled TINYINT(1) NOT NULL DEFAULT 1,"
            + " pending_change_count INT NOT NULL DEFAULT 0,"
            + " security_strategy VARCHAR(64),"
            + " public_visible TINYINT(1) NOT NULL DEFAULT 0,"
            + " public_title VARCHAR(128),"
            + " public_summary VARCHAR(512),"
            + " public_audience VARCHAR(64),"
            + " public_level VARCHAR(32),"
            + " public_start_at DATETIME NULL,"
            + " public_end_at DATETIME NULL,"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + " updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
            + ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统配置项'";
        executeDdl("create system_setting table", ddl);
    }

    private void ensureColumn(String columnName, String definition) {
        if (columnExists(columnName)) {
            return;
        }
        String ddl = "ALTER TABLE system_setting ADD COLUMN " + columnName + " " + definition;
        executeDdl("add column " + columnName, ddl);
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_setting' AND COLUMN_NAME = ?",
            Integer.class,
            columnName
        );
        return count != null && count > 0;
    }

    private void ensureForeignKey() {
        if (!columnExists("default_region_id")) {
            return;
        }

        if (foreignKeyExists()) {
            return;
        }

        String ddl = "ALTER TABLE system_setting ADD CONSTRAINT fk_system_setting_region"
            + " FOREIGN KEY (default_region_id) REFERENCES base_region (id) ON DELETE SET NULL";
        executeDdl("add default_region foreign key", ddl);
    }

    private boolean foreignKeyExists() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_setting'"
                + " AND CONSTRAINT_NAME = 'fk_system_setting_region'",
            Integer.class
        );
        return count != null && count > 0;
    }

    private void executeDdl(String description, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            log.info("system_setting: {} executed", description);
        } catch (DataAccessException ex) {
            log.warn("system_setting: failed to {} - {}", description, ex.getMessage());
        }
    }
}

