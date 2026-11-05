# 阶段 3.5b：词组库全引用复用模型

> 完成日期：2026-11-05

## 目标

阶段 3.5a 实测后，用户又提出两件事：

1. 词组详情页要能看出词组之间的关系，并可点超链接在组之间跳转；
2. "复制拼接结果"只能复制正向提示词，负向复制不出来。

围绕第 1 点先后讨论过「单父继承 → 多父继承（图论）→ 单继承 + 手动导入」，用户明确了两条真实痛点：

- 导入项会堆在自有条目区，条目一多就看不出复用了哪些词组；
- 来源词组后续改动不会同步到当前词组。

最终拍板**方案 B：全引用**——不再区分继承与导入，所有复用都是带优先级的引用关系；详情页实时递归合并，原组改动自动生效；被引用时禁止删除。

## 实现内容

### 数据模型（引用取代继承）

- `schema.sql` 新增 `group_ref` 表：`group_id`、`ref_group_id`、`priority`、`sort_order`，`UNIQUE (group_id, ref_group_id)`，并建 `(group_id, priority, sort_order)` 与 `(ref_group_id)` 两个索引。
- `tag_group.parent_id` 标记废弃但**保留列**，避免旧库结构差异；`SchemaUpgrade` 用 `INSERT OR IGNORE ... WHERE NOT EXISTS` 把旧 `parent_id` 数据迁移成引用关系，迁移失败只记日志不阻断启动。
- `entity/GroupRef.java`（新增）：引用关系 + 联查字段 `refGroupName / refItemCount / refScope / refLoraName`。
- `TagGroup` 去掉 `parentId / parentName`，新增 `incomingRefCount`（列表展示"被引用 N"）。
- `TagGroupItem` 新增 `sourceGroupId / sourceGroupName`，标记条目来自哪个组。

### 后端

`TagGroupMapper` + XML：

- 删除 `listChildren / countChildren`；新增 `listRefs / listIncomingRefs / countIncomingRefs / countRefExists / insertRef / updateRef / deleteRef / deleteRefsByGroup / maxRefSortOrder / maxPriority`。
- `deleteRef` 带 `group_id` 条件，防止跨组误删。

`TagGroupService`（整体重写）：

- `detail(id)` 返回 `group / items（自身）/ refs / incomingRefs / refItems / effectiveItems`，列表与详情都填充 `incomingRefCount`。
- `collectRefItemsRecursive`：从当前组出发 **BFS 逐层展开**引用链，`MAX_REF_DEPTH = 10`，`visited` 防重复与成环；同一层按 `priority`、`sort_order` 排序，近层先入。
- `computeEffectiveItems`：先放引用条目、再放自身条目，`tagId|polarity` 去重，**自身 > 深度近的引用 > 深度远的引用**。
- `addRef`：拦截自引用、目标不存在、跨范围引用（`global` 只能引用 `global`，`lora` 只能引用同 `loraId`）、重复引用、成环；`priority` 缺省 `maxPriority + 1`，`sort_order` 缺省末尾追加。
- `createsCycle`：队列 BFS，判断目标组的引用链能否绕回当前组。
- `update`：修改 scope / loraId 时，检查现有引用是否全部仍在允许范围内，否则拒绝并提示；`duplicate` 不再复制引用关系。
- `delete`：**被引用保护**，`countIncomingRefs > 0` 时抛 `BizException`，消息里列出所有引用方的组名，要求先解除引用。

`TagGroupController` 新增：

```text
POST   /api/groups/{id}/refs
PUT    /api/groups/{id}/refs/{refId}
DELETE /api/groups/{id}/refs/{refId}
```

### 前端

`api/group.js`：新增 `addGroupRef / updateGroupRef / removeGroupRef`。

`GroupLibraryView.vue`（整体重写）：

- 列表名称列显示 `被引用 N` 标签。
- 详情抽屉新增「📎 引用的词组库」管理区：每行可点名称跳到被引用组的详情（同一抽屉内切换），显示条目数，`priority` 数字框可直接改，右侧"移除"。
- "添加引用"弹窗复用 `SearchBox` 补全搜索，候选表排除自身与已引用组。
- 正向 / 负向 tab 内：`AddTagRow` + 自身条目表（可改权重、删除）+ **按来源分组的只读引用条目表**，来源组名是超链接。
- 底部改为两个按钮：**复制正向 / 复制负向**，分别用 `effectiveItems` 过滤 polarity 后格式化写剪贴板。
- 删除失败且 `code=1` 时，若该组被引用则用 `ElMessageBox.alert` 展示后端返回的引用明细。

## 关键决策

见 [ADR-0008 用统一引用关系替代父子继承](../adr/0008-group-reference-model.md)。

## 踩坑 / 注意

- `deleteRef` 的 Java 接口改成了 `(id, groupId)` 两个参数，XML 仍按单参数写，编译期才发现不一致；落库条件必须带 `group_id`，否则参数对不上会误删。
- `group_ref` 是**新表**，索引可以直接写在 `schema.sql`；但给旧表补列后的索引仍必须放在 `SchemaUpgrade`（阶段 3.5a 已踩过 `no such column`）。
- 旧 `parent_id` 迁移必须在 `group_ref` 建表之后执行，且整体 `try/catch`，否则脏数据会让应用启动失败。
- 递归链最初只取了直接引用（A→B，漏掉 B→C），接口测试 `eff-pos-count` 直接失败；改为 BFS 分层后修复。这验证了"全引用"必须是**递归**语义。
- PowerShell 5 内联脚本里的 `$` 会被外层 Shell 吞掉、中文 JSON 还会触发 UTF-8 报错；测试脚本统一写成 `.ps1` 用 `-File` 执行，中文请求体写无 BOM 临时文件 + `-InFile`。
- 8081 被上一次测试残留进程占用（`Port 8081 was already in use`），先 `taskkill /PID ... /F` 再启动。
- 全局异常返回 HTTP 200 + `code=1`，断言必须看 `code` 而不是捕获异常。

## 验证证据

- 后端 `clean compile` BUILD SUCCESS（39 文件）。
- 前端 `npm run build` PASS（1634 modules，产物 `index-BdZAea54.js` / `index-DCzYC671.css`）。
- 接口（8081）11 项全 PASS：
  - `A-refs-B-add`、`B-refs-C-add`（引用关系建立）
  - `eff-pos-count=3`、`eff-neg-count=1`（递归合并条目数）
  - `eff-T2-from-own`（自身条目覆盖引用）
  - `eff-T4-from-C-via-B`（跨两层的引用条目进入生效列表）
  - `cycle-rejected`、`self-ref-rejected`、`dup-ref-rejected`
  - `delete-protected`（被引用时删除返回 `code=1` 并列出引用方）
  - `B-deleted-after-remove`（解除引用后可正常删除）
- 测试词组与条目已清理；8081 已释放。

## 遗留问题

- **浏览器 UI 尚未实测**（本次只做了接口验证）：引用区跳转、优先级修改、复制正负向、删除保护弹框需要一次完整走查。
- 详情页已拿到 `incomingRefs`，但只在删除失败时展示；尚未做常驻的"被谁引用"区块。
- `duplicate` 只复制自身条目，引用关系需手动重建（前端已有提示）。
- 引用链超过 `MAX_REF_DEPTH` 时静默截断，没有 UI 提示。
- SPA 深链接仍需统一 forward 配置（沿用阶段 2.5b）。
