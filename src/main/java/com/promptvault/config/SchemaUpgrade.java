package com.promptvault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 轻量增量迁移：schema.sql 只做 CREATE TABLE IF NOT EXISTS，
 * 已存在的表要加列需要在这里按需 ALTER（SQLite 不支持 ADD COLUMN IF NOT EXISTS）。
 */
@Component
public class SchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("tag_group", "category",
                "ALTER TABLE tag_group ADD COLUMN category TEXT NOT NULL DEFAULT 'other'");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_group_category ON tag_group (category)");
    }

    private void ensureColumn(String table, String column, String alterSql) {
        if (hasColumn(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute(alterSql);
            log.info("已为表 {} 补充列 {}", table, column);
        } catch (Exception e) {
            log.error("为表 {} 补充列 {} 失败", table, column, e);
        }
    }

    private boolean hasColumn(String table, String column) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("PRAGMA table_info(" + table + ")");
        for (Map<String, Object> row : rows) {
            if (column.equalsIgnoreCase(String.valueOf(row.get("name")))) {
                return true;
            }
        }
        return false;
    }
}
