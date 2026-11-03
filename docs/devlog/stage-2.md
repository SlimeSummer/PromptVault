# 阶段 2：LoRA 档案与预设

> 完成日期：2026-11-03

## 目标

1. 给每个 LoRA 建一份档案（来源、底模、触发词）。
2. 支持一个 LoRA 下多个预设；预设是角色基线，正向与负向标签都能存。
3. 预设内支持权重、排序、正负向互转、复制派生。

## 实现内容

### 后端

- 实体：`entity/Lora.java`、`entity/Preset.java`、`entity/PresetItem.java`。
- Mapper：`LoraMapper`、`PresetMapper`、`PresetItemMapper` 及对应 XML。
  - `PresetItem` 查询时 `JOIN tag`，直接带出 `prompt / name_zh / category`，前端不用二次请求。
  - 列表查询用子查询带出 `preset_count` / `item_count`，避免 N+1。
- `service/LoraService.java`：
  - 名称唯一；来源仅 `self_trained / third_party`。
  - 删除前检查该 LoRA 下的预设数量，有预设则拒绝删除。
  - 删除成功后把 `tag.lora_id` 置空，避免标签留下悬空归属。
- `service/PresetService.java`：
  - 预设状态：`draft / verified / deprecated`。
  - 条目权重限制在 0.1 ~ 2.0，四舍五入到两位小数。
  - 同预设内「同标签 + 同极性」不允许重复。
  - 正负向互转时，条目会移到目标分区的末尾。
  - 复制预设：连同全部条目一起复制，名称追加「副本」，状态重置为 `draft`。
  - 排序接口按传入的 `itemIds` 顺序重写 `sort_order`。
- `web/LoraController.java`、`web/PresetController.java`：
  - `GET/POST /api/loras`、`GET/PUT/DELETE /api/loras/{id}`、`GET /api/loras/{id}/presets`
  - `POST /api/presets`、`GET/PUT/DELETE /api/presets/{id}`、`POST /api/presets/{id}/duplicate`
  - `POST /api/presets/{id}/items`、`PUT/DELETE /api/presets/{id}/items/{itemId}`、`PUT /api/presets/{id}/items/order`

### 前端

- `web/src/views/LoraView.vue`，三段式交互：
  1. LoRA 列表：搜索、新增/编辑弹窗、删除、预设数量徽标。
  2. 预设抽屉：某个 LoRA 下的预设列表，可新建、复制、删除。
  3. 预设编辑抽屉：基础信息（名称/状态/备注）+ 正向分区 + 负向分区。
- 编辑抽屉能力：
  - 从全局词库远程搜索（支持中英文）并添加为正/负向；
  - 每条标签可调权重（0.1–2.0）、上移/下移、转正/负向、移除；
  - 底部「拼接预览」按 `(tag:weight)` 规则生成正向/负向文本，可一键复制。
- `web/src/api/lora.js`：接口封装。
- 侧边栏「LoRA 档案」解锁，新增 `/loras` 路由。

## 关键决策

- [ADR-0003](../adr/0003-preset-as-character-baseline.md)：预设只做角色基线（身份 + 穿搭 + 画风锚点），不含动作/场景/表情。
- 复制预设不复制"已验证"状态：副本一律回到 `draft`，避免未经测试的派生被误用。
- 排序接口采用「整体重写」而不是单条交换：条目数量不大，整体写入更简单，也不担心 `sort_order` 冲突。

## 踩坑 / 注意

- **局部更新必须保留未传字段**：最初 `updateItem` 在只传 `polarity` 时会把 `weight` 当成 null 默认成 1.0，导致"转负向"会悄悄重置权重。已改为"未传则沿用原值"。
- `sort_order` 在正负向分区之间共享，因此前端每次排序都会把「正向列表 + 负向列表」拼成一个完整顺序提交，避免两个分区互相穿插。
- 浏览器自动化第一次"添加词条"失败，原因是测试代理没有真正选中下拉选项，不是应用问题；重新按"先确认 select 显示 masterpiece，再点添加"复测通过。
- 嵌套两层抽屉在 Element Plus 中需要 `append-to-body`，否则遮罩和层级会出问题。

## 验证证据

- `.\mvnw.cmd -DskipTests compile` 通过。
- `npm run build` 通过（1624 modules）。
- 接口全流程实测：
  - 7 个词条 → LoRA #2 → 预设 → 6 正向 + 1 负向；
  - 权重改为 1.25；转负向再转回，权重仍为 1.25；
  - 排序后第一条 id 与提交顺序一致；
  - 复制预设得到 7 条条目副本，状态为 `draft`；
  - 有预设时删除 LoRA 被拦截，提示预设数量。
  - 清理后 `tags=0, loras=0`。
- 浏览器实测：
  - LoRA 行预设数量显示 2，预设抽屉列出两个预设；
  - 编辑抽屉显示 6 正、1 负；
  - 权重改 1.2 后拼接预览变为 `(anna dooley:1.2)`；
  - 添加 `masterpiece` 后正向变 7 条；
  - 转负向后正向 5 条、负向 2 条，权重保留。
- 服务已关闭，8080 / 5173 端口已释放。

## 遗留

- LoRA 封面（`cover_path`）只在表里预留，界面尚未使用。
- 预设内条目目前用"上移/下移"，拖拽排序留到阶段 4 的编辑区统一实现。
- 预设与词组的负向词合并策略尚未在代码中落地（见 ADR-0004）。
