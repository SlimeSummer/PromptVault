# 0012. 参考图片用单表泛化关联

- 状态：已接受
- 日期：2026-11-10

## 背景

词条、词组、LoRA 预设都需要挂参考图（每个可多张），用来直观判断这个提示词 / 组合长什么样。三处需求完全相同，如果各建一张图片表，会有三套实体、三套 Mapper、三套上传逻辑。

## 决策

一张 `ref_image` 表，用 `ref_type`（`tag` / `group` / `preset`）+ `ref_id` 泛化关联：

- 字段：`filename`（磁盘文件名，UUID + 扩展名）、`orig_name`、`content_type`、`size`、`created_at`。
- 文件落 `data/images/`，数据库只存元数据；元数据与文件分离，备份 `data/` 目录即包含图片。
- 接口统一为 `/api/images?refType=&refId=`：列表 / 上传 / 字节流 / 删除。
- 约束：`refType` 白名单 + 保存前校验目标存在；单文件 ≤10MB；单目标最多 20 张。
- 级联：词条、词组、预设删除时由业务层调 `deleteByRef` 清理记录与文件（SQLite 无外键约束，靠 service 保证）。

## 后果

- 新增一类可挂图的实体，只需在 `refType` 白名单和前端 `ImagePanel` 的 props 加一个取值，不用再建表。
- 关联完整性由业务层负责：绕过 Service 直接删库会留下孤儿图片记录 / 文件，这一点和项目里其他「删除保护」的做法一致。
- 图片走 `GET /api/images/{id}/raw` 流式返回，带 7 天缓存头；前端用 `el-image` 直接引用该 URL，不做 base64 内嵌。
- 泛化表的代价是没有数据库级外键约束，`ref_id` 指向的目标类型由 `ref_type` 决定，查询时不能只按 `ref_id` 找。

## 备选方案

- **三张独立表（tag_image / group_image / preset_image）**：结构重复、接口重复，新增实体还要再建表，放弃。
- **在 `tag` / `tag_group` / `preset` 上加 `image_path` 单列**：只能一张图，且三处要各自维护上传逻辑，放弃。
- **图片存 BLOB 进数据库**：备份简单但库文件膨胀、读写都要过 JDBC，本地单文件应用的场景下不如文件系统，放弃。
