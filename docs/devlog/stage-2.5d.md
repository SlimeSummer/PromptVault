# 阶段 2.5d：批量导入自动分类（规则版）

> 完成日期：2026-11-04

## 目标

在全局词库批量导入词条时，支持"自动判断种类"——用户勾选复选框后每条词条根据关键词规则自动归入对应分类，避免一个个改。分类器采用纯关键词匹配，不接 AI，未来可替换。

## 实现内容

### 后端

- `service/TagClassifier.java`（新建）
  - 独立规则引擎，词典用 `Map<String, Set<String>>`，默认覆盖 10 个分类（quality / trigger / style / outfit / scene / action / expression / detail / negative / other）。
  - `classify(prompt)`：负面优先（避免被其他规则抢走）、大小写不敏感、未命中归 `other`。
- `dto/TagBatchRequest.java`：新增 `Boolean autoCategory`，默认 false；契约形态不变（JSON body，text/category/loraId/autoCategory）。
- `service/TagService.batchImport`：
  - 引入 `autoCategory` 分支；true 时跳过 category 字段和校验。
  - 每条独立分类后写入；返回结构新增 `byCategory: { [category]: { inserted, skipped } }` 与 `autoCategory` 标记。

### 前端

- `views/TagLibraryView.vue`
  - 批量导入弹窗在"归类为"上方新增"自动判断种类"复选框；勾选后归类下拉禁用。
  - 结果区除总数/新增/跳过外，追加按分类的明细列表（`el-tag` + 新增/跳过计数）。
  - 响应式：`batchAutoCategory` ref、`onAutoCategoryToggle`、`submitBatch` 传 `autoCategory` 字段。

## 关键决策

- **规则版先行**：按用户选择，先落纯关键词词典（约 80 个关键词，覆盖常见场景），保留 `classify` 方法签名以便未来换成 AI 调用（可作为另一个 Classifier 实现注入）。
- **契约稳定**：`TagBatchRequest` 只追加字段，不改变现有字段形态（text 仍是字符串，不是数组）；autoCategory=false 时行为完全兼容旧版。

## 踩坑 / 注意

- `Boolean.TRUE.equals(null)` = false，autoCategory 不传递时按旧逻辑走。
- 词典关键词里 `3d` 同时出现在 `style` 和 `negative`，靠"负面优先"分支解决——先判 negative，命中即归负面。
- El-checkbox 的 disabled 状态需 DOM 检查 `is-disabled` class，视觉上就是灰显不可点。
- 用户自己的 8080 实例和测试用 8081 共用同一个 SQLite 文件（`jdbc:sqlite:./data/promptvault.db`），测试期间数据会自然增长，测试后只清理自己的新增条目。

## 验证证据

- `.\mvnw.cmd -DskipTests clean compile` BUILD SUCCESS（27 个源文件，新增 TagClassifier）；`npm run build` 1627 modules PASS。
- 接口（8081）：
  - 自动分类：14 条混合 prompt（masterpiece / blue eyes / smile / standing / dress / ugly / deformed / bad anatomy / cyberspace / solo focus...）分别落入 quality / detail / expression / action / outfit / negative / trigger / other，负面优先正确。
  - 非自动兼容：`category=quality` 时全部归 quality；不传 category 且不勾 autoCategory 时默认 other。
  - 返回结构含 `autoCategory=true` 与 `byCategory` 按分类明细。
- 浏览器（8081）：
  - 复选框勾选 → select 获得 `is-disabled` class 不可交互 ✅
  - 自动分类导入 → 6 条分别归入 6 个不同分类，byCategory 明细正确 ✅（masterpiece→质量词, blue eyes→细节, smile→表情, deformed→负面, school uniform→穿搭, cyberspace→其他）
  - 取消勾选 + 选质量词 → 6 条全归质量词 ✅
- 测试数据清理：zz_test_*、negative 测试词条均删除；用户真实数据（8080 上）未动。
- 临时 8081 已关闭释放。

## 遗留

- 批次 D 是规则版，AI 自动分类作为可选后续——若需接入，只需替换 `TagClassifier` 的实现（Spring 自动装配），`classify` 方法签名不变，对上层透明。
- 规则词典是硬编码，后续可考虑移到 `setting` 表让用户自定义。
- 生产模式 SPA 深链接 404（沿用 2.5b 记录，后续统一 forward 配置）。
