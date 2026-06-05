# Agent Studio 命名与 Docker 部署规范化变更目的

## 1. 变更背景

当前 Agent 管理平台 V1 已具备主链路能力，但工程命名和发布方式仍混合了早期开发阶段的口径：

- 对外交付名、构件名、配置前缀、环境变量、数据库名和 OpenAPI 文件名仍大量使用 `myagent`。
- 正式部署说明仍以 PowerShell 脚本、Maven 运行和 Vite dev server 为主要入口。
- Docker 当前主要用于 PostgreSQL 测试、Testcontainers 和临时验收容器，没有固化为正式发布入口。

这会导致开发、测试、交付和部署无法判断哪一套入口才是正式真相。

## 2. 本次变更目标

本次变更只收口对外交付命名、运行配置契约和正式 Docker 部署入口，不在同一个变更中做仓库目录级和 Java 根包级大重构。

本次必须达成：

- 对外产品英文名统一为 `Agent Studio`。
- 对外产品中文名统一为 `智能体工作台`。
- 工程 slug 统一为 `agent-studio`。
- Maven `groupId` 使用用户确认的 `syc`。
- 后端构件、镜像和服务名统一为 `agent-studio-api`。
- 前端 package、镜像和服务名统一为 `agent-studio-web`。
- 对外 OpenAPI 示例不得再泄漏 `com.myagent` 或 `myagent-backend`。
- Spring 配置前缀统一为 `agent.studio.*`。
- 正式环境变量前缀统一为 `AGENT_STUDIO_*`。
- Compose 默认数据库名和用户名统一为 `agent_studio`。
- 正式 Web 访问入口固定为 `http://127.0.0.1:18080`。
- 正式发布入口固定为 `docker compose up -d --build`。
- Docker Compose 默认编排 `postgres`、`api`、`web` 三个服务。

## 3. 本次不做的事情

本次不修改 Agent、Workflow、Schema、Run、Trace、Eval 等业务领域概念。

本次不重命名 `11_code/backend`、`11_code/frontend` 目录，不迁移 Java 根包 `com.myagent`。这两类属于仓库级重构，涉及 MyBatis XML、包扫描、OpenAPI 示例、测试路径和大量 import，单独拆成后续变更更合理。

本次不保留 `myagent.*` 与 `agent.studio.*` 两套运行时配置真相。旧环境变量、旧配置键、旧数据库名和旧 OpenAPI 文件名不作为正式兼容入口继续存在。

## 4. 成功标准

- 当前源码配置、构件名、镜像名、部署入口和当前发布文档均以 `Agent Studio` / `agent-studio` 为正式口径。
- OpenAI 正式部署变量只使用 `AGENT_STUDIO_OPENAI_API_KEY`、`AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
- Compose PostgreSQL 的数据库名和用户名固定为 `agent_studio`，密码由 `AGENT_STUDIO_POSTGRES_PASSWORD` 提供。
- Web 容器宿主机端口固定为 `18080`，API 不直接暴露宿主机端口。
- 浏览器通过 `http://127.0.0.1:18080` 同源访问前端静态资源和 `/api`。
- `compose.yaml` 顶层 `name` 固定为 `agent-studio`。
- Flyway 在 Compose PostgreSQL 中完成数据库迁移。
- 开发态脚本可以保留，但只能作为开发辅助，不再作为正式部署入口。
