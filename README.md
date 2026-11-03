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
| 全局词库（增删改查 / 分类过滤 / 中英文搜索 / 批量导入） | 🚧 开发中 |
| LoRA 档案与预设（正负向 / 状态 / 复制派生） | ⏳ 待开发 |
| 词组库（经验组合管理） | ⏳ 待开发 |
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
│   ├── common/ApiResponse.java  # 统一响应结构
│   └── web/HealthController.java
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql               # 10 张表的结构定义（幂等）
└── web/                         # Vue 3 前端
    ├── package.json
    ├── vite.config.js           # /api 代理到 8080
    └── src/
```

## 数据与备份

- 数据库文件：`data/promptvault.db`（首次启动自动创建）
- 该目录已在 `.gitignore` 中排除，**不会被提交到仓库**
- 迁移或备份时，直接复制 `data/` 目录即可

## 开发路线图

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 项目骨架与数据模型 | ✅ |
| 1 | 全局词库 | 🚧 |
| 2 | LoRA 档案与预设 | ⏳ |
| 3 | 词组库 | ⏳ |
| 4 | 结构化编辑区 | ⏳ |
| 5 | PNG 元数据导入 | ⏳ |
| 6 | AI 生成参考 Prompt | ⏳ |

> 每个阶段完成后都会更新本文档的功能清单与路线图状态。
