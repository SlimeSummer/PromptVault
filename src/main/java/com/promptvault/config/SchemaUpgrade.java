package com.promptvault.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量增量迁移：schema.sql 只做 CREATE TABLE IF NOT EXISTS，
 * 已存在的表要加列需要在这里按需 ALTER（SQLite 不支持 ADD COLUMN IF NOT EXISTS）。
 */
@Component
public class SchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    private static final String KEY_TAG_CATEGORIES = "tag.categories";
    private static final String KEY_ARTIST_SEEDED = "tag.categories.artist_seeded";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SchemaUpgrade(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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

        ensureArtistCategory();
    }

    /**
     * 老库的分类设置里补上「画师」分类。用 marker 只补一次，
     * 之后用户主动删除不会再被加回来。
     */
    private void ensureArtistCategory() {
        try {
            if (settingValue(KEY_ARTIST_SEEDED) != null) {
                return;
            }
            String raw = settingValue(KEY_TAG_CATEGORIES);
            if (raw != null && !raw.isBlank()) {
                List<Map<String, Object>> categories = objectMapper.readValue(
                        raw, new TypeReference<List<Map<String, Object>>>() {
                        });
                boolean exists = categories.stream()
                        .anyMatch(item -> "artist".equals(String.valueOf(item.get("value"))));
                if (!exists) {
                    Map<String, Object> artist = new LinkedHashMap<>();
                    artist.put("value", "artist");
                    artist.put("label", "画师");
                    int negativeIndex = -1;
                    for (int i = 0; i < categories.size(); i++) {
                        if ("negative".equals(String.valueOf(categories.get(i).get("value")))) {
                            negativeIndex = i;
                            break;
                        }
                    }
                    if (negativeIndex >= 0) {
                        categories.add(negativeIndex, artist);
                    } else {
                        categories.add(artist);
                    }
                    jdbcTemplate.update(
                            "UPDATE setting SET value = ?, updated_at = datetime('now','localtime') WHERE key = ?",
                            objectMapper.writeValueAsString(categories), KEY_TAG_CATEGORIES);
                    log.info("已在词条分类中补充「画师」分类");
                }
            }
            jdbcTemplate.update(
                    "INSERT OR REPLACE INTO setting (key, value, updated_at) VALUES (?, '1', datetime('now','localtime'))",
                    KEY_ARTIST_SEEDED);
        } catch (Exception e) {
            log.warn("补充画师分类失败（可忽略）：{}", e.getMessage());
        }
    }

    private String settingValue(String key) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT value FROM setting WHERE key = ?", key);
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0).get("value");
        return value == null ? null : value.toString();
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
