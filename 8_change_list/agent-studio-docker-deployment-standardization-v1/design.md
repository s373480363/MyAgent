# Agent Studio 命名与 Docker 部署规范化设计

## 1. 设计原则

- 单一真相：正式部署只有 Docker Compose 一个入口。
- 简单优先：使用标准 Dockerfile、标准 Compose、现有 Spring Boot 和 Vite 构建能力。
- 收敛对外契约：先规范构件、镜像、配置、环境变量、数据库和发布说明。
- 控制影响面：仓库目录和 Java 根包重命名单独拆分，不和 Docker 发布改造绑在同一变更内。

## 2. 命名设计

| 项目 | 当前口径 | 本次目标 |
|------|----------|----------|
| 对外产品英文名 | MyAgent / Agent 管理平台 | Agent Studio |
| 对外产品中文名 | Agent 管理平台 | 智能体工作台 |
| 工程 slug | myagent | agent-studio |
| Maven groupId | com.myagent | syc |
| Maven artifactId | myagent-backend | agent-studio-api |
| Spring application.name | myagent-backend | agent-studio-api |
| Java 根包 | com.myagent | 本次不改，后续独立评估 `syc.agentstudio` |
| 后端源码目录 | 11_code/backend | 本次不改 |
| 前端源码目录 | 11_code/frontend | 本次不改 |
| 前端 package name | myagent-frontend | agent-studio-web |
| OpenAPI 快照文件 | myagent-openapi.json | agent-studio-openapi.json |
| OpenAPI 示例 Java 类型 | com.myagent.schema.* | syc.agentstudio.example.* |
| OpenAPI 服务名示例 | myagent-backend | agent-studio-api |
| 配置前缀 | myagent.* | agent.studio.* |
| 环境变量前缀 | MYAGENT_* | AGENT_STUDIO_* |
| 数据库名 | myagent | agent_studio |
| 数据库用户名 | myagent | agent_studio |
| Compose project | 无正式口径 | agent-studio |
| API 镜像名 | 无正式口径 | agent-studio-api |
| Web 镜像名 | 无正式口径 | agent-studio-web |

`backend`、`frontend` 和 `com.myagent` 在本次变更后仍可能作为内部历史实现名短期存在，但不得出现在当前发布文档中作为正式产品、镜像、部署、配置或数据库口径。

## 3. OpenAPI 示例契约

Java 根包 `com.myagent` 本次可以保留，但对外 OpenAPI 示例属于外部契约，不能继续暴露旧命名。

本次必须清理以下外显示例：

- 后端 DTO、Response、配置类中的 `@Schema(example = "com.myagent...")`。
- 后端 DTO、Response、配置类中的 `@Schema(example = "myagent-backend")`。
- `openapi/agent-studio-openapi.json` 中的旧示例。
- `src/api/generated/schema.ts` 中由 OpenAPI 生成出来的旧示例。

Java 类型示例统一使用：

```text
syc.agentstudio.example.SummaryInput
```

服务名示例统一使用：

```text
agent-studio-api
```

## 4. OpenAI 环境变量契约

正式 OpenAI 部署变量只允许以下三个：

| 用途 | 正式环境变量 |
|------|--------------|
| OpenAI-compatible API Key | AGENT_STUDIO_OPENAI_API_KEY |
| OpenAI-compatible Base URL | AGENT_STUDIO_OPENAI_BASE_URL |
| 默认模型 | AGENT_STUDIO_OPENAI_DEFAULT_MODEL |

以下变量不是正式入口，开发必须移除文档和 Compose 中的使用：

- `OPENAI_API_KEY`
- `SPRING_AI_OPENAI_BASE_URL`
- `MYAGENT_OPENAI_DEFAULT_MODEL`

后端配置应把 Spring AI 需要的 `spring.ai.openai.*` 属性映射到 `AGENT_STUDIO_OPENAI_*`。`spring.ai.openai.*` 是框架内部配置键，不是部署环境变量契约。

为避免 Spring Boot relaxed binding 让 `SPRING_AI_OPENAI_BASE_URL` 重新覆盖配置，后端应在非测试启动路径增加 fail-fast 检查：如果检测到 `OPENAI_API_KEY`、`SPRING_AI_OPENAI_BASE_URL` 或 `MYAGENT_OPENAI_DEFAULT_MODEL`，启动失败并返回中文错误说明。

fail-fast 校验类和对应测试中可以出现上述旧变量的字符串字面量，但只能用于拒绝旧变量，不能用于读取配置、设置默认值或形成成功启动路径。

正式部署对三个变量的必填/默认口径必须冻结如下：

- `AGENT_STUDIO_OPENAI_API_KEY`：正式部署必填，不允许在共享主配置、Compose 或正式文档中使用 `dummy-key`、`test-key` 等伪值兜底。
- `AGENT_STUDIO_OPENAI_BASE_URL`：可以提供明确默认值，但默认值必须是真实可用的 OpenAI-compatible 入口，例如 `https://api.openai.com`。
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`：可以提供明确默认值，但默认值必须是真实模型名。

启动期对 OpenAI 配置的校验只应收口到“旧变量禁止”和“正式 API Key 不得缺失/空白”。不要对 Key 格式、模型名格式或 Base URL 可达性做启动期严格校验，更不要在启动期发起真实 LLM 调用探测，否则会把 OpenAI-compatible 场景错误当成非法配置。

## 5. PostgreSQL 契约

Compose PostgreSQL 的数据库名和用户名固定：

| 项目 | 值 |
|------|----|
| POSTGRES_DB | agent_studio |
| POSTGRES_USER | agent_studio |
| API JDBC 数据库 | jdbc:postgresql://postgres:5432/agent_studio |
| API 数据库用户名 | agent_studio |

数据库密码只通过一个正式变量提供：

```text
AGENT_STUDIO_POSTGRES_PASSWORD
```

Compose 同时把该变量传给 `postgres` 和 `api`。不再额外提供 `POSTGRES_USER`、`POSTGRES_DB`、`AGENT_STUDIO_DATASOURCE_USERNAME`、`AGENT_STUDIO_DATASOURCE_URL` 作为 Compose 正式覆盖项。

如需非 Compose 外部数据库部署，后续单独设计，不纳入本次默认发布口径。

## 6. 正式访问入口

正式宿主机访问入口固定为：

```text
http://127.0.0.1:18080
```

端口契约：

| 服务 | 容器内端口 | 宿主机端口 | 说明 |
|------|------------|------------|------|
| web | 80 | 18080 | 唯一正式浏览器入口 |
| api | 8080 | 不暴露 | 只允许 web 容器内网代理访问 |
| postgres | 5432 | 不暴露 | 只允许 api 容器内网访问 |

Web 容器负责：

- `/` 返回前端静态资源。
- `/api` 代理到 `http://api:8080`。
- `/v3/api-docs` 代理到 `http://api:8080`。
- `/swagger-ui.html`、`/swagger-ui`、`/actuator` 按同一规则代理到 API。

通过正式入口 `http://127.0.0.1:18080/v3/api-docs` 取得的 OpenAPI 文档，`servers.url` 必须仍然是正式外部访问地址，而不能退化成不带端口的 `http://127.0.0.1`，也不能暴露内部地址 `http://api:8080`。该要求必须通过反向代理转发 Host/Port 头和后端转发头配置共同保证，不能靠前端手工改写 OpenAPI 快照。

## 7. Docker 部署设计

正式部署入口为仓库根目录：

```powershell
docker compose up -d --build
```

Compose 包含三个服务：

| 服务 | 镜像 | 职责 |
|------|------|------|
| postgres | postgres:16-alpine | 提供 V1 主数据库，持久化到 Docker volume |
| api | agent-studio-api | 运行 Spring Boot API，启动时由 Flyway 执行迁移 |
| web | agent-studio-web | 提供前端静态资源，并反向代理到 api |

后端 Dockerfile 继续使用 `11_code/backend` 作为构建上下文。前端 Dockerfile 继续使用 `11_code/frontend` 作为构建上下文。源码目录重命名不在本次变更中执行。

`compose.yaml` 必须写入顶层项目名：

```yaml
name: agent-studio
```

该配置用于冻结 Docker 网络、volume 和容器前缀，避免默认使用仓库目录名生成另一套部署命名。

## 8. 文档范围

当前正式文档必须同步检查：

- `README.md`
- `3_product_design`
- `4_arch_design`
- `6_schema_design`
- `7_interface_design`
- `11_code/README.md`
- `13_release`
- `14_user_manual`

历史验收记录和旧变更记录可以保留旧名称作为历史事实，但不得作为当前发布入口或当前命名依据。
