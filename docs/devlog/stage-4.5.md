# 阶段 4.5：画师类别 / 参考图片 / 编辑区记忆

> 完成日期：2026-11-10

## 目标

1. 新增「画师」词条类别：编辑区按 anima 语法输出 `@画师名`（如 `@kantoku`），并支持单独调权重。
2. 给词条、词组、LoRA 预设挂参考图片（每个条目可多张），能直接看到对应视觉效果。
3. 编辑区切换页面后回来，停留在离开时选中的那个编辑区，而不是被重置为第一个。

## 实现内容

### 画师类别

- `SettingService`：默认分类新增 `artist`（画师），排在 `negative` 之前。
- `SchemaUpgrade`：老库已有 `tag.categories` 时解析 JSON 并补种 `artist`；用 marker `tag.categories.artist_seeded` 保证只补一次，用户主动删除后不会被反复加回。
- `WorkspaceService`：
  - 画师不进普通分区（`positiveSections()` 同时排除 `negative` 与 `artist`），`normalizeSection` 拒绝把条目塞进画师分区。
  - 预设、编辑区标签、词组三个来源分别拦截 `category = artist`，统一走 `addArtist` 收集：去重键为小写画师名，存储时剥掉前导 `@`，输出时统一拼回一个 `@`，避免 `@@kantoku`。
  - 同一画师若先来自预设 / 词组、后来自编辑区，升级为「编辑区」来源并 `removable = true`，同时权重以编辑区为准，保证用户能移除并调整自己选的那个。
  - 权重沿用普通条目的 `(prompt:weight)` 规则，画师输出为 `(@kantoku:1.5)`；权重 1 时只输出 `@kantoku`。
  - 响应新增 `artists` / `artistCount`；`positiveCount` 仍只统计普通分区，画师单独计数。
- `WorkspaceArchiveService.sanitizeSection`：恢复存档时不允许把 `artist` 当成普通分区覆盖。
- 前端 `WorkspaceView.vue`：负向卡片上方新增画师卡片，`+ 选择画师` 复用标签弹窗并锁定 `category=artist`；编辑区来源的画师带权重输入框，其他来源只读显示 `×权重`。

### 参考图片

- `schema.sql` 新增 `ref_image`（`ref_type` + `ref_id` 泛化关联，`tag` / `group` / `preset` 共用一张表），幂等建表即可，旧库启动时自动创建。
- `ImageService`：文件存 `data/images/`（相对工作目录），文件名 UUID + 扩展名（无扩展名按 content-type 推断）；单文件 ≤10MB、单目标最多 20 张；保存前校验目标存在。
- `ImageController`：`/api/images` 列表 / 上传 / 字节流 / 删除，字节流带 7 天缓存头。
- 级联清理：`TagService`（含批量删除）、`TagGroupService`、`PresetService` 删除时调用 `ImageService.deleteByRef`，同时删记录与磁盘文件。
- 前端新增 `ImagePanel.vue`（上传 + 缩略图网格 + 点击放大 + 删除），接入三个入口：全局词库行「图片」、词组库行「图片」、LoRA 档案 → 预设管理 → 预设行「图片」。
- `api/http.js` 新增 `postForm`，并在请求头里过滤 `undefined`，避免 multipart 的 `Content-Type` 被写死导致 boundary 丢失。

### 编辑区记忆

- `WorkspaceView.vue` 用 `localStorage`（键 `pv.workspace.currentId`）记录当前编辑区 id：`watch(currentId)` 写入，`loadWorkspaces()` 无显式选中时读取恢复；原编辑区已被删除则回退到列表第一个。

## 关键决策

- [ADR-0011 画师作为独立类别，输出 @画师名](../adr/0011-artist-category-at-syntax.md)
- [ADR-0012 参考图片用单表泛化关联](../adr/0012-reference-images-generic-table.md)

## 踩坑 / 注意

- 只在 `DEFAULT_CATEGORIES` 加 `artist` 覆盖不到老库：老库的分类来自 `setting.tag.categories`，必须写迁移。
- 画师不能走普通分区：既要在分区列表里排除，也要在 `normalizeSection` 里挡住手动 `sectionOverride`，否则会出现空白的「画师」分区。
- 画师权重改完正向文本不刷新：`onArtistWeightChange` 只调了更新接口，没重新拉详情；浏览器验收时发现，补一次 `loadDetail()` 后正常。
- 图片删除要同时处理「记录在、文件没了」的情况，`delete` 里对文件缺失容忍，避免删不掉的脏记录。
- 中文 JSON 在 PowerShell 5 下仍要按 UTF-8 字节写入或用 `curl --data-binary` 传临时文件，否则接口收到乱码。

## 验证证据

- 后端 `.\mvnw.cmd -q -DskipTests clean compile` 退出码 0。
- 前端 `npm run build` 成功（1640 modules）。
- 接口（8081）：
  - `/api/tags/categories` 已包含 `{value: artist, label: 画师}`，说明老库补种生效。
  - 画师入编辑区：`artistCount = 1`，普通分区计数全 0，`positiveText = [@kantoku]`；权重改 1.5 后 `positiveText = [(@kantoku:1.5)]`；移除后 `artistCount = 0`、正向文本清空。
  - 图片：上传返回记录（`filename` / `contentType` / `size`），列表可查；`GET /api/images/{id}/raw` 返回 `Content-Type: image/png` + `Cache-Control: max-age=604800`；删除后列表为空。
- 浏览器验收：
  - 画师卡片位于负向卡片上方，显示 `@kantoku` 与移除按钮；权重改 2 后预览即时显示 `(@kantoku:2)`。
  - 从编辑区切到词组库再回来，下拉框仍停留在原编辑区。
  - 词库 / 词组库 / LoRA 预设三处「图片」弹窗均正常，缩略图可放大预览，预设弹窗能浮在抽屉之上。
- 测试数据（临时编辑区、画师词条、演示图片）已清理；8081 已释放。

## 遗留

- 图片仅本地存储，没有压缩 / 缩略图生成，大图多时会直接按原文件读盘；数量级上来后可考虑加缩略图。
- 画师只做了编辑区展示与权重，词库里还没有「只看画师」的独立筛选入口。
- 编辑区记忆是浏览器本地状态，换浏览器 / 清缓存后回到第一个编辑区。
