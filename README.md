# PromptVault

> ComfyUI 提示词经验库 —— 把记事本里散落的「咒语」，变成可检索、可组装、可复用的结构化资产。

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-3-003B57?logo=sqlite&logoColor=white)
![Status](https://img.shields.io/badge/status-WIP-orange)

---

## 为什么做这个

用 ComfyUI 出图时，真正卡住人的往往不是模型，而是提示词：

- 每个 LoRA 都有自己的触发词、穿搭方案和"脾气"，10 个 LoRA 就是 10 套使用经验，久了必乱；
- 一组好用的提示词动辄二三十个标签，堆在一起像黑盒，改一个细节要逐个翻找，容易改漏改错；
- 脑子里有画面，手上却写不出对应标签 —— 提示词像 SQL / 汇编，卡在"编码"环节；
- 经验只能记在记事本里，像流水账，时间久了自己都看不懂。

PromptVault 想把这三件事拆开解决：**存得住、找得到、拼得快**。

## 核心概念

| 概念 | 说明 |
| --- | --- |
| **词库 Tag** | 全局原子提示词，带中文释义与分类，支持中英文搜索 |
| **LoRA 档案** | 一个模型的专属档案：来源、底模、触发词、备注 |
| **预设 Preset** | 某个 LoRA 的「角色基线」：身份 + 穿搭 + 画风锚点，含正向与负向，**故意不含动作/场景/表情** |
| **词组 Group** | 你的经验组合，如 `cheerleader, pom pom, bow, jumping, arms up`，可整块插入、可解绑、可继续打磨 |
| **编辑区 Workspace** | 九分区组装台：触发词 / 质量 / 风格 / 穿搭 / 场景 / 动作 / 表情 / 细节 / 负面 |

组装逻辑：`预设（角色基线）+ 词库/词组（动作、场景、表情、细节）→ 自动拼接 → 一键复制到 ComfyUI`。

## 功能进度

| 模块 | 状态 |
| --- | --- |
| 项目骨架 · SQLite 数据模型 · 前后端联通 | ✅ 已完成 |
| 全局词库（增删改查 / 分类过滤 / 中英文搜索 / 批量导入） | ✅ 已完成 |
| LoRA 档案与预设（正负向 / 状态 / 排序 / 权重 / 复制派生） | ✅ 已完成 |
| 系统设置（自定义词条分类 / 自定义 LoRA 底模） | ✅ 已完成 |
| 词库交互（搜索补全 / 搜索按钮 / 表头排序 / 列宽拖动） | ✅ 已完成 |
| 词条收藏（星标 / 只看收藏筛选） | ✅ 已完成 |
| 批量导入自动分类（规则版） | ✅ 已完成 |
| 词条批量删除 | ✅ 已完成 |
| 词组库（经验组合管理） | 🚧 开发中 |
| 结构化编辑区（九分区 / 拖拽排序 / 权重 / 导出） | ⏳ 待开发 |
| ComfyUI PNG 元数据反向导入 | ⏳ 待开发 |
| AI 自然语言生成参考 Prompt | ⏳ 待开发 |

## 技术栈

| 层级 | 选型 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.3.5 · MyBatis 3.0.4 |
| 数据库 | SQLite（单文件，零安装） |
| 前端 | Vue 3.5 · Vite 6 · Element Plus 2.9 |
| 构建 | Maven Wrapper（`mvnw`）· npm |

## 快速开始

环境要求：**JDK 17+**、**Node.js 18+**。

```bash
# 1. 启动后端（默认 8080）
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# 2. 启动前端（另开终端，默认 5173）
cd web
npm install
npm run dev
```

打开 <http://localhost:5173>，页面右上角显示「后端已连接」即表示链路正常。

生产构建：

```bash
cd web && npm run build       # 产物输出到 src/main/resources/static
./mvnw package                # 打成单个可执行 jar
```

## 项目结构

```text
PromptVault/
├── pom.xml                      # Maven 配置
├── mvnw / mvnw.cmd              # Maven Wrapper
├── src/main/java/com/promptvault/
│   ├── PromptVaultApplication.java
│   ├── common/                  # 统一响应、业务异常、全局异常处理
│   ├── dto/                     # 请求对象
│   ├── entity/                  # 数据实体
│   ├── mapper/                  # MyBatis Mapper 接口
│   ├── service/                 # 业务逻辑
│   └── web/                     # REST 接口
├── src/main/resources/
│   ├── application.yml
│   ├── schema.sql               # 11 张表的结构定义（幂等）
│   └── mapper/                  # MyBatis SQL（Tag / Lora / Preset / PresetItem / Setting）
├── docs/
│   ├── README.md                # 开发记录的写作约定与模板
│   ├── devlog/                  # 每个阶段一页开发日志
│   └── adr/                     # 架构决策记录
└── web/                         # Vue 3 前端
    ├── package.json
    ├── vite.config.js           # /api 代理到 8080
    └── src/
        ├── api/                 # 请求封装
        ├── router/              # 路由
        ├── views/               # 页面（词库、LoRA 档案、设置）
        ├── App.vue              # 侧边栏布局
        └── main.js
```

## 开发记录

本项目不只保留"最终代码"，也保留"怎么长出来的过程"：

| 载体 | 记录内容 | 入口 |
| --- | --- | --- |
| 开发日志 | 每个阶段的目标、改动、踩坑与验证证据 | [docs/devlog](docs/devlog/) |
| 架构决策记录（ADR） | 重大技术/产品选择的背景与后果 | [docs/adr](docs/adr/) |
| 写作约定 | 何时写、怎么写、模板 | [docs/README.md](docs/README.md) |

- 阶段日志：[阶段 0 · 项目骨架](docs/devlog/stage-0.md) · [阶段 1 · 全局词库](docs/devlog/stage-1.md) · [阶段 2 · LoRA 档案与预设](docs/devlog/stage-2.md) · [阶段 2.5a · 设置驱动分类](docs/devlog/stage-2.5a.md) · [阶段 2.5b · 词库交互](docs/devlog/stage-2.5b.md) · [阶段 2.5c · 词条收藏](docs/devlog/stage-2.5c.md) · [阶段 2.5d · 自动分类](docs/devlog/stage-2.5d.md) · [阶段 2.5e · 批量删除](docs/devlog/stage-2.5e.md)
- 决策记录：[ADR-0001 本地 Web 应用](docs/adr/0001-local-web-app.md) · [ADR-0002 一次建全表结构](docs/adr/0002-schema-single-shot.md) · [ADR-0003 预设是角色基线](docs/adr/0003-preset-as-character-baseline.md) · [ADR-0004 负向词三层合并](docs/adr/0004-negative-prompt-layers.md) · [ADR-0005 设置驱动分类](docs/adr/0005-settings-driven-categories.md) · [ADR-0006 Schema 幂等增量](docs/adr/0006-schema-incremental-tables.md)

> README 记录"现在是什么样"，devlog 记录"怎么变成这样"，ADR 记录"为什么这样定"。

## 接口一览

统一响应 `{ code, message, data }`，`code = 0` 表示成功。

| 模块 | 接口 |
| --- | --- |
| 健康检查 | `GET /api/health` |
| 全局词库 | `GET/POST /api/tags`、`PUT/DELETE /api/tags/{id}`、`POST /api/tags/batch`、`POST /api/tags/batch-delete`、`GET /api/tags/stats`、`GET /api/tags/categories`、`GET /api/tags/suggest`、`POST /api/tags/{id}/favorite` |
| LoRA 档案 | `GET/POST /api/loras`、`GET/PUT/DELETE /api/loras/{id}`、`GET /api/loras/{id}/presets` |
| 预设 | `POST /api/presets`、`GET/PUT/DELETE /api/presets/{id}`、`POST /api/presets/{id}/duplicate` |
| 预设条目 | `POST /api/presets/{id}/items`、`PUT/DELETE /api/presets/{id}/items/{itemId}`、`PUT /api/presets/{id}/items/order` |
| 系统设置 | `GET/PUT /api/settings` |

> 词库与 LoRA 列表支持 `sort` / `order` 查询参数（字段白名单校验），用于表头排序。

## 数据与备份

- 数据库文件：`data/promptvault.db`（首次启动自动创建）
- 该目录已在 `.gitignore` 中排除，**不会被提交到仓库**
- 迁移或备份时，直接复制 `data/` 目录即可

## 开发路线图

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 项目骨架与数据模型 | ✅ |
| 1 | 全局词库 | ✅ |
| 2 | LoRA 档案与预设 | ✅ |
| 2.5a | 设置页 · 自定义分类与底模 | ✅ |
| 2.5b | 词库交互（搜索按钮 · 搜索补全 · 表头排序 · 列宽拖动） | ✅ |
| 2.5c | 词条收藏 | ✅ |
| 2.5d | 批量导入自动分类（规则版） | ✅ |
| 2.5e | 词条批量删除 | ✅ |
| 3 | 词组库 | 🚧 |
| 4 | 结构化编辑区 | ⏳ |
| 5 | PNG 元数据导入 | ⏳ |
| 6 | AI 生成参考 Prompt | ⏳ |

> 每个阶段完成后都会更新本文档的功能清单与路线图状态。
