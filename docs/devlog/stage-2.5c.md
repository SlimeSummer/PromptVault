# 阶段 2.5c：词条收藏

> 完成日期：2026-11-04

## 目标

给词条加「收藏」，并能一键筛出收藏项，方便在词库里快速找到常用词条。

## 实现内容

### 后端

- `src/main/resources/schema.sql`：新增 `tag_favorite(tag_id PRIMARY KEY, created_at)`，`CREATE TABLE IF NOT EXISTS` 幂等追加（决策见 [ADR-0006](../adr/0006-schema-incremental-tables.md)）。
- `entity/Tag.java`：新增 `favorite` 字段，仅列表联查填充，非 `tag` 表列。
- `mapper/TagMapper.java` / `mapper/TagMapper.xml`
  - `insertFavorite`（`INSERT OR IGNORE`，幂等）、`deleteFavorite`、`countFavorite`。
  - 列表联查 `EXISTS` 得出 `favorite`；`list / count` 支持 `favorite=true` 过滤。
- `service/TagService.java`
  - `toggleFavorite(id)`：存在即取消、不存在即收藏，返回新状态。
  - `delete(id)`：删除词条前先清理其收藏记录。
- `web/TagController.java`
  - `POST /api/tags/{id}/favorite` 返回 `{ favorite }`。
  - `GET /api/tags` 新增 `favorite` 参数。

### 前端

- `api/tag.js` 新增 `toggleTagFavorite`。
- `views/TagLibraryView.vue`
  - 表格首列「收藏」：空心/实心星标，点击即切换。
  - 搜索按钮旁新增「只看收藏」复选框；在只看收藏状态下取消收藏会自动刷新列表。

## 关键决策

- **收藏独立建表**：不动 `tag` 表列，偏好状态与词条本体分离（[ADR-0006](../adr/0006-schema-incremental-tables.md)）。
- **切换式接口**：一个 `POST /{id}/favorite` 完成收藏/取消，前端无需感知当前状态。

## 踩坑 / 注意

- SQLite 的 `EXISTS` 返回 0/1，MyBatis 映射到 `Boolean` 字段即可，Jackson 序列化为 `true/false`。
- `INSERT OR IGNORE` 让重复收藏天然幂等，不需要先查后插。

## 验证证据

- `.\mvnw.cmd -DskipTests clean compile` BUILD SUCCESS；`npm run build` 1627 modules PASS。
- 接口（8081，用户真实数据）：
  - 收藏 25、26 后 `favorite=true` 返回 2 条；取消 26 后剩 1 条。
  - 收藏不存在的 id=999999 返回 `code=1 词条不存在`。
  - 新建词条→收藏→删除词条后，收藏列表自动不含该词条，词条总数恢复 17。
- 浏览器：星标空心/实心渲染正确；「只看收藏」筛选正确；点击星标即变黄；刷新后状态保持。
- 测试结束后取消全部测试收藏，`favorite=true` 返回 0 条；8081 已释放；用户 8080/5173 未动。

## 遗留

- 批次 D：批量导入自动分类（规则版，预留 AI 接口）。
- 生产模式 SPA 深链接 404（沿用 2.5b 记录，后续统一处理）。
