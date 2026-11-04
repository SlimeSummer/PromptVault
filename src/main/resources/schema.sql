-- PromptVault 数据库结构（幂等，可重复执行）
-- 设计要点：标签(tag)是全局原子词；词组(group)与预设(preset)都是"有序标签集合"；
--          编辑区(workspace)只记录"插入了什么"，渲染时动态展开，从而支持整块/解绑。

CREATE TABLE IF NOT EXISTS lora (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    source        TEXT    NOT NULL DEFAULT 'third_party',  -- self_trained / third_party
    base_model    TEXT,                                    -- sd15 / sdxl / pony / illustrious / noobai / other
    trigger_words TEXT,                                    -- 主要触发词的文本快照，便于列表展示
    note          TEXT,
    cover_path    TEXT,
    created_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS tag (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    prompt     TEXT    NOT NULL COLLATE NOCASE,  -- 实际写入 ComfyUI 的英文标签
    name_zh    TEXT,                             -- 中文释义/别名，用于中文搜索
    category   TEXT    NOT NULL DEFAULT 'other', -- trigger/quality/style/outfit/scene/action/expression/detail/negative/other
    lora_id    INTEGER,                          -- 归属 LoRA，NULL 表示通用标签
    note       TEXT,
    usage_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE (prompt)
);

CREATE INDEX IF NOT EXISTS idx_tag_category ON tag (category);
CREATE INDEX IF NOT EXISTS idx_tag_lora ON tag (lora_id);

-- 词条收藏：独立于 tag，不给 tag 加列；删除词条时由业务层联动清理
CREATE TABLE IF NOT EXISTS tag_favorite (
    tag_id     INTEGER PRIMARY KEY,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS preset (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    lora_id    INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    status     TEXT    NOT NULL DEFAULT 'draft',  -- draft / verified / deprecated
    note       TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_preset_lora ON preset (lora_id);

CREATE TABLE IF NOT EXISTS preset_item (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    preset_id  INTEGER NOT NULL,
    tag_id     INTEGER NOT NULL,
    polarity   TEXT    NOT NULL DEFAULT 'pos',  -- pos / neg
    weight     REAL    NOT NULL DEFAULT 1.0,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_preset_item_preset ON preset_item (preset_id, sort_order);

CREATE TABLE IF NOT EXISTS tag_group (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    kind       TEXT    NOT NULL DEFAULT 'normal',  -- normal / global_negative
    scope      TEXT    NOT NULL DEFAULT 'global',  -- global / lora
    lora_id    INTEGER,
    note       TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_group_scope ON tag_group (scope, lora_id);

CREATE TABLE IF NOT EXISTS tag_group_item (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id   INTEGER NOT NULL,
    tag_id     INTEGER NOT NULL,
    polarity   TEXT    NOT NULL DEFAULT 'pos',
    weight     REAL    NOT NULL DEFAULT 1.0,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_group_item_group ON tag_group_item (group_id, sort_order);

CREATE TABLE IF NOT EXISTS workspace (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    lora_id    INTEGER,
    preset_id  INTEGER,
    note       TEXT,
    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS workspace_item (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    workspace_id     INTEGER NOT NULL,
    item_type        TEXT    NOT NULL,               -- tag / group / preset
    ref_id           INTEGER NOT NULL,               -- 对应 tag/group/preset 的 id
    polarity         TEXT    NOT NULL DEFAULT 'pos', -- pos / neg（对 tag 类型生效）
    weight           REAL    NOT NULL DEFAULT 1.0,
    section_override TEXT,                           -- 手动调整分区；NULL 表示跟随 tag.category
    sort_order       INTEGER NOT NULL DEFAULT 0,
    created_at       TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_ws_item_ws ON workspace_item (workspace_id, sort_order);

CREATE TABLE IF NOT EXISTS setting (
    key        TEXT PRIMARY KEY,
    value      TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS import_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    source_name  TEXT,
    raw_positive TEXT,
    raw_negative TEXT,
    parsed_count INTEGER NOT NULL DEFAULT 0,
    created_at   TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);
