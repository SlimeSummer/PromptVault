# 批次 2.5a：设置页与自定义分类 / 底模

> 完成日期：2026-11-03

## 目标

解决体验反馈中的第 2、3、8 项：

1. 新增「设置」页面；
2. 词条分类可增删改，不再写死在代码里；
3. LoRA 底模可自定义，例如新增 anima。

## 实现内容

### 后端

- `entity`/`dto`：新增 `CategoryOption`（`value` + `label`）、`SettingsRequest`。
- `SettingMapper` + `SettingMapper.xml`：读取全部设置、按键读取、`INSERT ... ON CONFLICT` 更新。
- `SettingService`：
  - 分类与底模以 JSON 形式存入 `setting` 表（键 `tag.categories`、`lora.base_models`）；
  - 缺省时回退到内置默认值（原 10 个分类、6 个底模）；
  - 保存时校验：分类标识格式、显示名称长度、唯一性、数量上限；
  - 删除仍被词条使用的分类会被拒绝，并返回使用数量。
- `SettingController`：`GET /api/settings`、`PUT /api/settings`。
- `TagService` 不再持有分类常量，校验与统计统一走 `SettingService`。
- `GET /api/tags/categories` 改为返回 `{value, label}` 列表。

### 前端

- 新增 `web/src/views/SettingsView.vue`：
  - 词条分类表格：显示名称可编辑、标识仅新增时可填、词条数展示、上移/下移、删除（被使用时置灰）；
  - 底模标签列表：添加 / 移除；
  - 顶部统一「保存设置」。
- `web/src/api/setting.js`：设置读取、保存、分类选项读取。
- 侧边栏「编辑区」下方新增「设置」菜单，路由 `/settings`。
- `TagLibraryView`：分类下拉、新建/编辑表单、批量导入的「归类为」全部改为动态数据，加载失败时回退内置默认值。
- `LoraView`：底模下拉改为读取设置。

## 关键决策

- [ADR-0005](../adr/0005-settings-driven-categories.md)：分类与底模存入 `setting` 表；分类采用稳定的 `value` + 可改的 `label`；被使用的分类禁止删除。
- 没有新增数据表，因此不需要迁移老库。

## 踩坑 / 注意

- 验收时 8080 被 IDEA 中运行的旧实例占用（启动于 20:05，未加载新代码）。经确认后关闭旧实例，用 `mvnw spring-boot:run` 启动最新代码。
- 本机 5173 已有用户自己的 Vite 实例，新开的测试实例自动落到 5174；浏览器验收走的是 5173 上的热更新代码，结果有效。
- 验收时数据库里已有用户自己录入的 17 条词条和 1 个 LoRA。**没有清理这些真实数据**，只清理了本批次测试产生的词条与设置项。

## 验证证据

- `.\mvnw.cmd -DskipTests compile` 通过；`npm run build` 通过（1627 modules）。
- 接口实测：
  - 默认设置返回 10 个分类、6 个底模；
  - 新增 `hairstyle` 分类与 `anima` 底模后，`/api/tags/categories` 能返回；
  - 用 `hairstyle` 分类创建词条成功；
  - 删除仍被使用的 `hairstyle` 被拒绝，提示 1 条词条；
  - 删除测试词条后可正常移除该分类；
  - 非法标识 `bad value` 被拒绝；
  - 最终恢复默认分类与底模。
- 浏览器实测：
  - 设置页显示 10 行分类与 sd15/sdxl 底模；
  - 新增「发型 / hairstyle」并保存成功；
  - 词库「新增词条」的分类下拉出现「发型（0）」。
- 8080 与 5174 已释放；5173 保留的是用户自己的 Vite 进程，未关闭。

## 遗留

- 批次 B：搜索按钮、搜索补全、列宽拖动、表头排序。
- 批次 C：词条收藏。
- 批次 D：批量导入自动分类（规则版）。
- 自定义分类暂不参与后续自动分类规则。
