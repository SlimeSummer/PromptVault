# 阶段 0：项目骨架与数据模型

> 完成日期：2026-11-03

## 目标

1. 建立一个能跑起来的 Java + Vue 空项目，前后端能通。
2. 把后续所有阶段要用的数据表一次性设计好（SQLite）。
3. 产出可对外展示的 README 初版。

## 实现内容

### 后端

- `pom.xml`：Spring Boot 3.3.5 + MyBatis 3.0.4 + sqlite-jdbc 3.46.1.0，Java 17。
- `src/main/resources/application.yml`：端口 8080；SQLite 文件 `./data/promptvault.db`（`busy_timeout=5000`）；Hikari 连接池 `maximum-pool-size: 1`；启动时执行 `schema.sql`。
- `src/main/resources/schema.sql`：一次建全 10 张表（幂等 `CREATE TABLE IF NOT EXISTS`）：
  `lora`、`tag`、`preset`、`preset_item`、`tag_group`、`tag_group_item`、`workspace`、`workspace_item`、`setting`、`import_log`。
  设计主线是「标签是全局原子词；词组与预设都是有序标签集合；编辑区只记录插入了什么，渲染时再展开」。
- `common/`：`ApiResponse`（统一 `{code, message, data}`）、`BizException`、`GlobalExceptionHandler`（业务异常统一转 `code=1`）。
- `web/HealthController`：`GET /api/health` 返回应用名、时间、已建表数量，用作前后端联通探针。

### 前端

- `web/`：Vue 3.5 + Vite 6 + Element Plus 2.9 + vue-router，JavaScript。
- `App.vue`：左侧固定侧边栏，四个菜单项（词库可用，其余锁着并标注阶段号），底部显示后端连接状态。
- `api/http.js`：基于 `fetch` 的 `get/post/put/del` 封装，自动判断 `code !== 0` 并抛错。
- `vite.config.js`：`/api` 代理到 `localhost:8080`。

### 工程

- Maven Wrapper（`mvnw` / `mvnw.cmd`），避免依赖本机全局 Maven。
- `.gitignore`：排除 `target/`、`web/node_modules/`、`src/main/resources/static/`、`data/` 等。
- `README.md` 初版：定位、核心概念、技术栈、快速开始、项目结构。

## 关键决策

- [ADR-0001](../adr/0001-local-web-app.md)：交付形态选本地 Web 应用（Spring Boot + Vue + SQLite）。
- [ADR-0002](../adr/0002-schema-single-shot.md)：10 张表在阶段 0 一次建全，后续不改表结构。

## 踩坑 / 注意

- 本机没有全局 `mvn`，所有构建命令统一走 `.\mvnw.cmd`。
- SQLite 同一时刻只允许一个写事务，连接池必须设为 1，否则高并发下容易 `database is locked`。
- SQLite 没有正式迁移工具，所以表结构必须在阶段 0 想清楚（见 ADR-0002）。
- 时间字段统一用 `TEXT`，默认值 `datetime('now','localtime')`，Java 侧对应 `String`。

## 验证证据

- `.\mvnw.cmd spring-boot:run` 启动成功。
- `GET /api/health` 返回 `code=0`，`tables=10`。
- 前端 5173 打开后侧边栏显示「后端已连接」。
- `data/promptvault.db` 首次启动自动创建。

## 遗留

- 各业务模块的增删改查（从阶段 1 开始逐个实现）。
- 后端打包为可执行 jar、前端产物输出到 `static/` 的流程已在 README 写明，但未在阶段 0 实测。
