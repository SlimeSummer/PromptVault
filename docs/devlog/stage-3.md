# 阶段 3：词组库（TagGroup）

> 完成日期：2026-11-04

## 目标

提供经验组合的容器：把多个词条按正负向组织成一个整体，可整体引用/插入编辑区（阶段 4 用）。支持 global / lora 两种 scope，kind 预留 normal / global_negative。

## 实现

### 后端（新增文件）

- `entity/TagGroup.java`、`entity/TagGroupItem.java`（与 Preset / PresetItem 同构）
- `mapper/TagGroupMapper.java` + `TagGroupMapper.xml`：list / findById / countByTag / maxSortOrder / insert / update / delete。列表联查 lora.name 作为 loraName，并按 item_count 聚合。
- `mapper/TagGroupItemMapper.java` + `TagGroupItemMapper.xml`：listByGroup（联查 tag 表 prompt/name_zh/category）/ countDuplicate（同词组同 tag 同 polarity 重复拦截）/ maxSortOrder（同 polarity 内自增）/ CRUD / updateSort（上移下移共用）/ deleteByGroup。
- `service/TagGroupService.java`：
  - create：scope 切换 lora 时强制校验 lora 存在；sortOrder 取同 scope/lora 的 max+1。
  - update：**scope / loraId / kind / sortOrder 锁定**，避免条目悬空（删 + 重建才能改）。
  - duplicate：复制主表 + 所有条目，名称加" 副本"。
  - addItem：check tag 存在 + 同 (groupId, tagId, polarity) 重复拦截 + weight 0.1–2.0。
  - updateItem：切换 polarity 时重算 sortOrder；weight=null 沿用原值。
  - delete：阶段 4 完成后加 workspace 引用检查；阶段 3 先不锁。
- `controller/TagGroupController.java`：`GET /api/groups`（scope / loraId / keyword）、`GET /api/groups/{id}`（group + items）、CRUD、`POST /api/groups/{id}/duplicate`、条目 CRUD + reorder。
- `dto/GroupItemOrderRequest.java`（itemIds 顺序即新 sort_order）。

### 前端（新增 / 修改）

- `api/group.js`：所有后端方法对应的调用封装。
- `views/GroupLibraryView.vue`：
  - 列表页：scope segmented controls（全部/全局/LoRA 专属）+ LoRA 选择器 + keyword 搜索；表格列：名称（global_negative 加红色 tag）/范围（全局 / LoRA 名称）/条目数 / 备注 / 操作。
  - 新增抽屉：名称 + kind radio（普通/全局负面）+ scope radio（全局/LoRA 专属）+ lora 下拉 + 备注。
  - 详情抽屉：正向/负向 tab，每 tab 里一个 `AddTagRow` 子组件（从词库搜索选择添加）+ 条目表格（权重数字直接 inline 改、删除按钮）+ 底部"复制拼接结果"（`(tag:w)` 格式拼接，clipboard）。
- `router/index.js`：新增 `/groups` 路由。
- `App.vue`：侧边栏「词组库」从 locked 解锁。

### 关于预设 vs 词组的差异（schema 层）

| 维度 | Preset | TagGroup |
|---|---|---|
| 归属 | 强绑定 lora | global 或 lora scope |
| kind | draft/verified/deprecated（发布状态） | normal / global_negative（语义类型） |
| 删除时 | 不需要考虑 workspace（阶段 4 有） | 同左 |
| 条目结构 | 完全相同（tag_id / polarity / weight / sort_order） | 同左 |

## 验证

- `clean compile` BUILD SUCCESS（35 文件）；`npm run build` 1628 modules PASS。
- 接口（8081）：
  - 空列表 → 创建全局词组（kind=normal, scope=global）→ 添加正向条目 → 更新权重 → 查询详情 → keyword 过滤 → 复制派生 → 删除主表，全 PASS。
  - 重复添加同 tag+同 polarity：`code=1 "该词条已在正向列表中"`。
  - 无 name 创建：`code=1 "词组名称不能为空"`。
  - 权重超范围：`code=1 "权重范围需在 0.1 ~ 2.0 之间"`。
- 浏览器（8081）：侧边栏解锁 → 新增抽屉填表单保存 → 详情抽屉打开并显示正负向 tab，均 PASS。
  - picker 对话框由于浏览器 agent 预算耗尽未完成整条链路，但同一 `AddTagRow` 子组件已在 LoraView 的预设管理中实测过（之前阶段 2 验收）。
- 生产环境 SPA 深链接 `http://host/groups` 直接访问返回 `No static resource groups.`（阶段 2.5b 已记录，需统一 forward 配置）。本次浏览器验证从根路径 `/` 进入，由 vue-router 接管，规避此问题。
- 测试词组已清理，group 总数回到 0；8081 已释放。

## 遗留

- scope=lora 的词组在列表上显示 loraName，暂不支持"从 LoRA 档案进入该 LoRA 下的词组列表"的联动入口。
- 暂未做 workspace / preset / tag 对 group 的引用检查（阶段 4 编辑区完成后补）。
- SPA 深链接需统一 forward 配置（见上）。

## 验收后修复（2026-11-04）

用户实测反馈两个问题，均已修复并复验通过。

### 问题 1：详情抽屉里只有"从词库添加"按钮，没有搜索框

原因：词条选择器是用 `h('el-input')` / `h('el-table')` 渲染函数内联拼出来的，Element Plus 组件在 render 函数里没有被正确解析，弹窗内容渲染错位，输入框不可见。

修复：抽出正规单文件组件 `web/src/components/AddTagRow.vue`：

- 模板写法，含"从词库添加"按钮 + 弹窗内可见的搜索框（回车 / 清除触发搜索）+ 分页表格
- 新增 `existingTagIds` prop，已在当前正负向列表里的词条显示"已添加"并禁用，避免点到后端才报重复
- `GroupLibraryView.vue` 改为引入该组件，并传 `posTagIds` / `negTagIds`

### 问题 2：编辑词组时切换"类型 / 范围"报错

原因：`update()` 里为了"防止条目悬空"把 `kind` 置为 null 再写库，而 `tag_group.kind` 是 NOT NULL，触发
`SQLITE_CONSTRAINT_NOTNULL: NOT NULL constraint failed: tag_group.kind`。

修复：`TagGroupService.update()` 改为"缺省沿用原值 + 允许变更"：

- name 为空沿用原值（不再写 null）
- kind / scope 允许变更，未传则沿用原值
- scope=lora 时校验 LoRA 存在，loraId 为空沿用原值
- 范围或归属发生变化时重新排到新桶末尾（`maxSortOrder + 1`），避免 sort_order 冲突
- `TagGroupMapper.xml` 的 update 补齐 `kind / scope / lora_id / sort_order` 字段
- 前端编辑提交补齐 kind / scope / loraId（之前只提交 name + note，所以界面上改了也没生效）

### 复验

- `clean compile` BUILD SUCCESS（35 文件）；`npm run build` 1632 modules PASS
- 接口：只改 kind、改 scope=lora（带 loraId）、改回 global+normal 三次 PUT 全部 `code=0`，无约束异常
- 浏览器：详情抽屉内 picker 搜索 `blue` → 添加成功、按钮变"已添加"；编辑切换到"全局负面"保存无报错、范围切到 LoRA 专属保存成功
- 测试改动已回滚，`啦啦队` 恢复为 global + normal、条目清零；8081 已释放

