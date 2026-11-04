# 阶段 3.5：词组库增强与条目空白修复

> 完成日期：2026-11-04

## 目标

用户在阶段 3 实测后提出两类问题：

1. **Bug**：词组详情页新增条目后，该条目显示为空，必须刷新重进才正常；"复制拼接结果"粘贴出来也有空条目。
2. **词组库增强**：类别（分类下拉，可在设置页自定义）、所有搜索框统一"补全 + 搜索按钮"、新增"从词组库添加"（跨组拼接导入）。

## 实现内容

### 1. 条目空白 Bug

根因：`TagGroupItemMapper.xml` 的 `findById` 只写 `SELECT * FROM tag_group_item`，没有联查 `tag` 表，新增条目接口返回的对象缺 `prompt / nameZh`；刷新后走 `listByGroup`（有联查）才正常。

修复：`findById` 改为与 `listByGroup` 一致的联查：

```sql
SELECT i.*, t.prompt, t.name_zh AS nameZh, t.category
FROM tag_group_item i LEFT JOIN tag t ON t.id = i.tag_id
WHERE i.id = #{id}
```

`PresetItemMapper.xml` 本来就带联查，无同类问题。

### 2. 词组类别（设置驱动）

- `tag_group` 新增 `category TEXT NOT NULL DEFAULT 'other'`；旧库由 `SchemaUpgrade` 用 `PRAGMA table_info` + `ALTER TABLE` 补列，再建 `idx_group_category` 索引。
- `setting` 新增 `group.categories`，默认 10 项：人物 / 穿搭 / 场景 / 事件 / 质量 / 风格 / 动作 / 表情 / 光影 / 其他。
- `SettingService.save()` 对词条分类与词组分类分别校验：标识白名单、不重复、**被使用分类禁止删除**（统计 `tag.category` / `tag_group.category`）。
- 接口：`GET /api/groups/categories`；`GET/POST /api/groups` 支持 `category` 过滤与写入；设置页 PUT 接受 `groupCategories`。

### 3. 搜索框统一规范（补全 + 搜索按钮）

新增通用组件 `web/src/components/SearchBox.vue`（`el-autocomplete` + 搜索按钮，支持 `suggest` 回调、回车/清除/选中即提交）。

- 词库搜索：原本已是 autocomplete + 按钮。
- 词组库：列表搜索框、详情内"从词库添加"（`AddTagRow.vue`）、"从词组库添加"弹窗均换用 `SearchBox`。
- LoRA 列表搜索框换用 `SearchBox`。
- 新增后端补全接口：`GET /api/groups/suggest?keyword=&limit=`、`GET /api/loras/suggest?keyword=&limit=`（前缀优先排序，limit 夹取 1–20）。

### 4. 从词组库添加（跨组导入）

- `POST /api/groups/{id}/items/import`，body `{ sourceGroupId }`。
- 服务层逐条复制来源词组的条目到目标词组：同 `(tagId, polarity)` 已存在则跳过；`sort_order` 按目标组同极性末尾追加；响应返回 `sourceName / added / skipped`。
- 自导入（`targetId == sourceId`）直接报错。
- 前端详情抽屉头部新增"从词组库添加"按钮 → 弹窗内搜索并选中词组 → 导入 → 自动刷新详情与列表。

## 踩坑 / 注意

- `schema.sql` 里不能在 `CREATE TABLE IF NOT EXISTS tag_group` 之后立刻建 `idx_group_category`：旧库表已存在、`category` 列还没补，启动即报 `no such column: category`。索引必须移到 `SchemaUpgrade`（补列之后执行）。已修正。
- PowerShell 5 里 `Invoke-RestMethod` 直接传中文 JSON 字符串会触发 `Invalid UTF-8 middle byte 0x5c`；验证设置接口时改为写入无 BOM 的 UTF-8 临时文件 + `-InFile`。
- 全局异常处理返回 HTTP 200 + `code=1`，`Invoke-RestMethod` 不会抛错，断言要看 `code`。

## 验证证据

- `clean compile` BUILD SUCCESS（37 文件）；`npm run build` 1634 modules PASS。
- 接口（8081）：
  - 新增词组（category=scene）→ 添加条目返回 `prompt` / `nameZh` 非空（Bug 修复确认）。
  - `?category=scene` 过滤命中；`/api/groups/categories` 返回 10 项；`/api/groups/suggest`、`/api/loras/suggest` 正常返回。
  - 跨组导入：重复条目跳过（`added=1, skipped=1`），负向条目权重保留；自导入 `code=1 "不能从自己导入"`。
  - 设置保存：删除在用词组分类 `code=1`；重复标识 `code=1`；成功后分类数保持 10。
- 浏览器（8081，从根路径进入）：搜索框 + 搜索按钮、类别筛选、表格类别列、详情"从词组库添加"按钮、新增条目即时显示提示词，全部 PASS。
- 测试词组与条目已清理（用户"啦啦队"恢复 3 条原始条目）；8081 已释放。

## 验收后修复（2026-11-04）

- **"从词库添加"补全下拉全空白**：`/api/tags/suggest` 返回的是 `{prompt, nameZh, category}`，而 `SearchBox` 契约要求 `{ value, sub }` 或字符串数组，直接透传导致 `value-key="value"` 取不到值，渲染成空白行。修复：`AddTagRow.vue` 新增 `suggestTags()`，映射为 `{ value: prompt, sub: nameZh }`（同词库页原有写法）。已编译并浏览器实测：输入 `cheer` 显示 3 条可读建议（cheerleader / 啦啦队员 等），选中后回填输入框并过滤表格。

## 遗留

- SPA 深链接仍需统一 forward 配置（沿用阶段 2.5b 的遗留）。
- LoRA 预设抽屉里的词条选择仍是 remote `el-select`（本身带远程补全，但无独立"搜索"按钮），未强行改成 `SearchBox`。
