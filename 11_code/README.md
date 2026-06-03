# MyAgent V1 代码目录

本目录承载 MyAgent V1 的正式实现代码。当前代码范围已经覆盖 V1 主链路，不再是“步骤 01 工程骨架”阶段。

## 目录结构

```text
11_code/
  backend/   Spring Boot 后端工程
  frontend/  React + Vite 前端工程
  scripts/   本地启动与联调辅助脚本
```

## 当前实现范围

- 后端：
  - Schema 管理
  - Java 方法、工具与外部 Agent 主数据
  - Agent 与 WorkflowVersion 管理
  - DebugRun、Run、Trace、NodeRun 查询
  - EvalSuite、EvalCase、EvalRun、EvalCaseResult
  - Flyway 迁移、OpenAPI 输出、Testcontainers 测试
- 前端：
  - Settings、Schemas、External Agents、Agents
  - Workflow 画布与版本管理
  - Debug、Runs、Evals
  - API 类型统一消费 OpenAPI 生成产物
- 联调与契约：
  - 浏览器默认通过相对路径访问 `/api`
  - Vite dev server 代理 `/api`、`/v3/api-docs`、`/swagger-ui.html`
  - OpenAPI 快照与 TypeScript 类型通过统一脚本刷新

## 环境要求

- Node.js `24.x` 或兼容版本
- npm `11.x` 或兼容版本
- JDK `21`
- Maven `3.9.11`
- PostgreSQL `15+`
- Docker Desktop

## 标准启动方式

### 后端

标准入口：

```powershell
cd D:\myproject\MyAgent
powershell -NoProfile -ExecutionPolicy Bypass -File .\11_code\scripts\start-backend-local.ps1
```

该脚本会固定使用仓库内 `9_dependency\tools` 下的 JDK 21 与 Maven 3.9.11。

可选覆盖项：

- `MYAGENT_DATASOURCE_URL`
- `MYAGENT_DATASOURCE_USERNAME`
- `MYAGENT_DATASOURCE_PASSWORD`
- `MYAGENT_SERVER_PORT`
- `OPENAI_API_KEY`
- `SPRING_AI_OPENAI_BASE_URL`
- `MYAGENT_OPENAI_DEFAULT_MODEL`

### 前端

标准入口：

```powershell
cd D:\myproject\MyAgent
powershell -NoProfile -ExecutionPolicy Bypass -File .\11_code\scripts\start-frontend-dev.ps1
```

默认访问地址：

- [http://127.0.0.1:5173](http://127.0.0.1:5173)

默认浏览器访问语义：

- 页面只请求相对路径 `/api`
- 本地开发通过 Vite 代理转发到 `http://127.0.0.1:8080`
- `MYAGENT_BACKEND_DEV_TARGET` 可作为本地排障覆盖项，临时改写 Vite 代理目标，例如指向 `http://127.0.0.1:18081`
- 上述覆盖项只影响 dev server 代理，不改变浏览器默认仍通过相对路径 `/api` 访问后端的正式口径
- `VITE_API_BASE_URL` 仅作为显式覆盖项，用于特殊联调环境，不是标准启动前提

## OpenAPI 产物

唯一契约来源是后端 `/v3/api-docs`。

前端保留两个派生产物：

- `frontend/openapi/myagent-openapi.json`
- `frontend/src/api/generated/schema.ts`

刷新命令：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm run openapi:refresh
```

如后端不在默认 `http://127.0.0.1:8080`，可先设置：

```powershell
$env:MYAGENT_OPENAPI_BASE_URL='http://127.0.0.1:18080'
```

一致性检查：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm run openapi:check
```

## 常用验证命令

后端测试：

```powershell
cd D:\myproject\MyAgent\11_code\backend
$env:JAVA_HOME='D:\myproject\MyAgent\9_dependency\tools\jdk21\jdk-21.0.11+10'
$env:Path='D:\myproject\MyAgent\9_dependency\tools\jdk21\jdk-21.0.11+10\bin;D:\myproject\MyAgent\9_dependency\tools\maven-3.9.11\apache-maven-3.9.11\bin;' + $env:Path
mvn -q test
```

前端测试与构建：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm test -- --run
npm run build
```

## 当前说明

- 后端 `local` profile 会启用 Flyway 并依赖 PostgreSQL。
- Docker Desktop 用于 PostgreSQL Testcontainers 测试与本地验收辅助。
- Poe 可作为 OpenAI-compatible 网关；如必须验证供应商强制结构化输出，需要使用支持 `response_format` 的兼容服务。
