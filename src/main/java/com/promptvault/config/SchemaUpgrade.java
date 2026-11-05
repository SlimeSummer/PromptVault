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

        // 统一引用表：幂等创建 + 旧 parent_id 数据迁移
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS group_ref (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "group_id INTEGER NOT NULL," +
                "ref_group_id INTEGER NOT NULL," +
                "priority INTEGER NOT NULL DEFAULT 0," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "UNIQUE (group_id, ref_group_id)" +
                ")");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_group_ref_group ON group_ref (group_id, priority, sort_order)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_group_ref_ref ON group_ref (ref_group_id)");

        // 旧 parent_id 迁移：只迁未迁过的（group_ref 里没有的）
        try {
            jdbcTemplate.update(
                "INSERT OR IGNORE INTO group_ref (group_id, ref_group_id, priority, sort_order) " +
                "SELECT id, parent_id, 0, 0 FROM tag_group WHERE parent_id IS NOT NULL " +
                "AND NOT EXISTS (SELECT 1 FROM group_ref gr WHERE gr.group_id = tag_group.id AND gr.ref_group_id = tag_group.parent_id)"
            );
            log.info("旧父级继承数据已迁移进 group_ref 表");
        } catch (Exception e) {
            log.warn("迁移旧父级继承数据失败（可忽略）：{}", e.getMessage());
        }
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
