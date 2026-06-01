# MyAgent V1 代码目录

本目录用于承载 MyAgent V1 的正式实现代码。当前已完成开发执行步骤 `01：工程骨架与公共基线初始化`。

## 目录结构

```text
11_code/
  backend/   Spring Boot 后端工程
  frontend/  React + Vite 前端工程
  scripts/   后续开发与本地辅助脚本目录
```

## 当前已落地内容

- 后端工程骨架：
  - `Spring Boot 3.5.x`
  - `Java 21`
  - `MyBatis`
  - `Flyway`
  - `PostgreSQL`
  - `Spring AI`
  - `OpenAPI`
  - `JUnit 5`
  - `Testcontainers`
- 前端工程骨架：
  - `React`
  - `TypeScript`
  - `Vite`
  - `Ant Design`
  - `React Router`
  - `TanStack Query`
  - `Zustand`
  - `React Flow`
  - `Monaco Editor`
- 后端统一响应模型、错误模型、全局异常处理与 OpenAPI 基线。
- 前端全局布局、路由骨架、统一 HTTP Client 与模块占位页。

## 本地启动说明

### 前端

在 `11_code/frontend` 目录执行：

```powershell
npm install
npm run dev
```

默认访问地址：

- [http://localhost:5173](http://localhost:5173)

### 后端

在 `11_code/backend` 目录执行：

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

注意：

- `local` profile 会启用 Flyway，并连接本地 PostgreSQL。
- 默认配置也会启用 Flyway，后端启动时由 Flyway 自动执行数据库迁移。
- 如果本机未准备 PostgreSQL，`mvn spring-boot:run -Dspring-boot.run.profiles=local` 将无法完成正常启动。
- 真实模型调用通过 `OPENAI_API_KEY`、`SPRING_AI_OPENAI_BASE_URL` 和 `MYAGENT_OPENAI_DEFAULT_MODEL` 配置；使用 Poe 时 `SPRING_AI_OPENAI_BASE_URL` 应为 `https://api.poe.com`。

默认地址：

- [http://localhost:8080/api/ping](http://localhost:8080/api/ping)
- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 环境要求

- Node.js `24.x` 或兼容版本
- npm `11.x` 或兼容版本
- JDK `21`
- Maven `3.9+`
- PostgreSQL `15+`

## 当前说明

- 当前仓库环境已具备 Node.js 与 npm。
- 当前仓库环境默认未内置 Maven，且系统默认 `java -version` 仍可能是 JDK 17。
- 本次步骤已通过临时下载的 JDK 21 与 Maven 完成后端 `mvn test` 验证。
- 后端 `local` 启动仍依赖 PostgreSQL，不能把该命令理解为“开箱即跑”。
