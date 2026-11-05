# 阶段 4：结构化编辑区（动态分区 / 权重 / 导出 / 存档）

> 完成日期：2026-11-05

## 目标

1. 提供组装台：预设（角色基线）+ 词库 / 词组 → 自动拼接 → 一键复制到 ComfyUI。
2. 处理一致性问题：README 最初写「九分区」，但分类自阶段 2.5a 起可在设置页自定义增删，**分区不能写死**。
3. 首轮实测后的三项修复：编辑区存档（保存 / 读取 / 导入导出）、负向条目移除、弹窗重复搜索按钮。

## 实现内容

### 数据模型

- `workspace`、`workspace_item` 两张表阶段 0 已建好，本阶段才真正启用。
- `schema.sql` 新增 `workspace_archive`（全新表，幂等建表即可，无需 `SchemaUpgrade`）：`name / note / source_workspace_id / source_workspace_name / item_count / payload(JSON) / created_at`。

### 后端

- 实体 / DTO：`Workspace`、`WorkspaceItem`、`WorkspaceArchive`、`WorkspaceItemRequest`、`WorkspaceOrderRequest`、`WorkspaceBlock`、`WorkspaceArchiveRequest`。
- Mapper：`WorkspaceMapper`（联查 lora/preset 名称与条目数）、`WorkspaceItemMapper`（联查 tag 取 prompt/nameZh/category，`countDuplicate` 防同条目同极性重复）、`WorkspaceArchiveMapper`（列表不查 payload，避免大 JSON 全量拉出）。
- `WorkspaceService`（核心）：
  - `render(id)`：正向分区实时读取 `setting.tag.categories`（排除 `negative`），负向独立成层；分类未知 / 被删回退 `other`（无 `other` 回退第一个分类）。
  - 预设展开成 `locked=true` 的 preset 块（`sortOrder=-100000`，恒在最前，不参与拖拽）；负向三层合并（全局负面词组 → 预设 → 编辑区），按 prompt 小写去重、先出现者优先。
  - 词组行复用 `TagGroupService.effectiveItems`（阶段 3.5b 的递归引用合并），按分类聚合成 group 块（`unbindable=true`），支持「解绑」成独立标签。
  - 导出：分区顺序 + 分区内 sortOrder 拼接，权重格式 `(prompt:weight)`，`BigDecimal.stripTrailingZeros` 去尾零，weight≈1 只输出 prompt。
  - 换 LoRA 时若含旧 LoRA 专属词组则拒绝；词条 / 词组被编辑区引用时删除保护（`TagGroupMapper.countWorkspaceRefs`、`PresetMapper.countWorkspaceRefs`）。
  - 负向条目返回 `workspaceItemId / removable`：仅「编辑区」来源可单独移除。
- `WorkspaceArchiveService`：
  - 保存：把 workspace 配置 + 条目引用 + 当时展开的正负向文本序列化成 JSON 快照。
  - 读取：覆盖式恢复当前编辑区；失效的 LoRA / 预设 / 词条 / 跨 LoRA 词组自动跳过并在结果里计数。
  - 导入 JSON 只入库存档列表（不直接覆盖编辑区）；导出由前端取详情 payload 写 `.json` 文件。
- 接口：`/api/workspaces`（CRUD / items / order / unbind）、`/api/archives`（列表 / 详情 / 保存 / 导入 / 读取 / 删除）。

### 前端

- `web/src/api/workspace.js`：编辑区 + 存档全部接口。
- `web/src/views/WorkspaceView.vue`：
  - 顶部编辑区下拉 + 新建；meta-bar（名称 / LoRA / 预设 / 编辑 / 添加标签 / 添加词组 / 保存存档 / 读取存档 / 预览 / 复制正向 / 复制负向 / 删除）。
  - 动态分区卡片 grid；标签块带权重 `el-input-number`，词组块可解绑，预设块锁定。
  - 拖拽排序：仅未锁定块参与，调用 `PUT /items/order` 重新编号。
  - 负向面板：只读合并列表，来源标签区分（编辑区 / 全局负面 / 预设 / 词组），「编辑区」条目有「移除」。
  - 存档弹窗：列表（名称 / 来源编辑区 / 条目数 / 备注 / 保存时间）+ 读取 / 导出 / 删除 + 导入 JSON。
  - 修复：删掉「添加标签」「添加词组」弹窗里 SearchBox 之外的第二个「搜索」按钮。

## 关键决策

- [ADR-0009 编辑区分区跟随词条分类，不写死九分区](../adr/0009-dynamic-sections-follow-categories.md)
- [ADR-0010 编辑区存档采用独立快照表](../adr/0010-workspace-archive-snapshot.md)

## 踩坑 / 注意

- `fetchTagCategories` 在 `@/api/setting` 而非 `@/api/tag`，前端构建报错后修正。
- 读取存档恢复词组时，LoRA 专属词组必须与目标编辑区 LoRA 一致；初版漏了 `loraId == null` 的边界，会把 LoRA 词组错误恢复到无 LoRA 的编辑区，已修。
- `SearchBox` 自带搜索按钮，弹窗里不能再放第二个——这次是重复按钮的来源。
- 浏览器直连 8081 偶发一次 404（构建产物更新时点问题），重进即恢复，未复现。
- PowerShell 5 中文 JSON 仍需无 BOM 临时文件或 UTF-8 字节写入，沿用阶段 3.5b 的方式。

## 验证证据

- 后端 `clean compile` BUILD SUCCESS。
- 前端 `npm run build` PASS（1637 modules）。
- 接口（8081）：
  - 编辑区主流程 25 项全 PASS（动态分区、预设 / 词组展开、负向三层合并、拖拽排序、解绑、导出、权重）。
  - 存档与负向回归全 PASS：保存（快照含 items 与文本）→ 删除负向条目（negCount 10→9）→ 读取存档恢复（added=2, skipped=0）→ 导入生成新存档 → 删除清理。
  - 负向 `removable` 仅对「编辑区」来源为 true，全局负面 / 预设来源为 false。
- 浏览器验收：动态分区生效（人物 / 物品 / 视角等自定义分类）；负向「编辑区」行有移除、其他来源无；保存 / 读取存档按钮与弹窗正常；添加标签弹窗仅一个搜索按钮。
- 测试数据已清理；8081 已释放。

## 遗留

- **编辑区全局平铺排序不做**：用户确认「在设置里调整分类顺序即可实现大类顺序」。
- 全负向词组加入编辑区后没有正向块，其负向条目来源是「词组·X」不可单条移除，页面上也没有可移除整个词组的块——极端场景，待用户反馈再补。
- 读取存档是覆盖式恢复，不支持「读取为新编辑区」；如需要可后补。
- ComfyUI 直接对接（把生成提示词写进正面 / 负面输入框）：官方仅支持 `POST /prompt` 触发生成，无「写当前界面输入框」接口；浏览器直连有 CORS，需后端代理。单独立项，先只做了可行性分析。
