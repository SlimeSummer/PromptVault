# 阶段 1：全局词库

> 完成日期：2026-11-03

## 目标

把散落在记事本里的提示词，变成可检索、可分类、可批量导入的原子词条库。

## 实现内容

### 后端

- `entity/Tag.java`、`mapper/TagMapper.java`、`resources/mapper/TagMapper.xml`：基本的增删改查 + 分页 + 分类统计 + 引用计数。
- `service/TagService.java`：
  - 分类枚举集中在这里：`trigger / quality / style / outfit / scene / action / expression / detail / negative / other`。
  - 新建与编辑统一做规范化：去首尾空格、剥离 `(tag:1.2)` 权重写法、限制 200 字符。
  - 批量导入：按中英文逗号、顿号、换行切分，去重后逐条插入，已存在的跳过。
- `web/TagController.java` 提供：
  - `GET /api/tags`（`keyword` / `category` / `page` / `size`）
  - `GET /api/tags/stats`、`GET /api/tags/categories`
  - `POST /api/tags`、`PUT /api/tags/{id}`、`DELETE /api/tags/{id}`
  - `POST /api/tags/batch`

### 前端

- `web/src/views/TagLibraryView.vue`：搜索（英文标签 / 中文释义）、分类过滤（带各类数量）、分页、新增/编辑弹窗、批量导入弹窗、复制提示词。
- `web/src/api/tag.js`：对应接口封装。
- 侧边栏「全局词库」菜单解锁，路由 `/tags`。

## 关键决策

- **提示词全局唯一且大小写不敏感**：`tag.prompt` 使用 `COLLATE NOCASE` + `UNIQUE`，避免 `Blue eyes` 与 `blue eyes` 变成两条。
- **批量导入自动剥离权重**：记事本里常见的 `(masterpiece:1.2)` 只保留 `masterpiece`，权重属于预设/编辑区，不属于词条本体。
- **删除前检查引用**：标签被预设、词组或编辑区引用时不允许删除，并返回引用数量。

## 踩坑 / 注意

- 批量导入如果直接按逗号切分，会把 `blue sailor collar` 之外的括号权重当成正文，必须在入库前统一清洗。
- 分类过滤依赖固定枚举，新增分类时必须同步修改后端常量与前端 `CATEGORIES`，否则前后端会不一致。
- 测试时创建的词条会污染真实词库，验收后必须清理。

## 验证证据

- 后端编译通过，接口逐条实测：新增、编辑、删除、分页、分类过滤、中英文搜索、批量导入（含权重剥离与重复跳过）。
- 浏览器实测通过：搜索、分类过滤、新增/编辑/删除、批量导入结果提示、复制。
- 测试数据已清空；服务已关闭，8080 / 5173 端口已释放。
- README 功能进度表与路线图已更新。

## 遗留

- 词条暂无"使用次数"统计（表里预留了 `usage_count`，后续可在预设/编辑区引用时累加）。
- 词条与 LoRA 的归属关系（`lora_id`）已建模，但阶段 1 的界面还没有使用它。
