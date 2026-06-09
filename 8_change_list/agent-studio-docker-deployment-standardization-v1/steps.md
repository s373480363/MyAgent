# Agent Studio 命名与 Docker 部署规范化执行步骤

## 1. 当前状态

当前状态：方案修订完成，待开发执行。

本文件定义开发人员后续实施本变更时应按顺序执行的步骤。本次不做源码目录重命名和 Java 根包重命名。

## 2. 执行步骤

### 步骤 01：建立变更基线

1. 阅读本目录下 `purpose.md`、`design.md`、`plan.md`、`developer_guidance.md`、`test_steps.md`。
2. 确认本次只做对外交付命名、配置契约和 Docker 正式部署。
3. 确认 `11_code/backend`、`11_code/frontend`、`com.myagent` 本次不改。

### 步骤 02：后端构件命名

1. Maven `groupId` 改为 `syc`。
2. Maven `artifactId` 改为 `agent-studio-api`。
3. Maven `name` 改为 `agent-studio-api`。
4. Spring `application.name` 改为 `agent-studio-api`。
5. 保持 Java 根包 `com.myagent` 不变。
6. 执行后端测试，确认构件命名变化不影响启动和测试。

### 步骤 03：前端构件命名

1. `package.json` name 改为 `agent-studio-web`。
2. `package-lock.json` name 同步改为 `agent-studio-web`。
3. OpenAPI 快照文件改为 `openapi/agent-studio-openapi.json`。
4. 更新 `openapi:download`、`openapi:generate`、`openapi:refresh`、`openapi:check`。
5. 清理 OpenAPI 外显示例中的 `com.myagent` 和 `myagent-backend`。
6. Java 类型示例统一改为 `syc.agentstudio.example.SummaryInput`。
7. 服务名示例统一改为 `agent-studio-api`。
8. 重新刷新 OpenAPI 快照和前端生成类型。
9. 执行前端测试和构建。

### 步骤 04：配置键和环境变量

1. Spring 配置前缀从 `myagent` 改为 `agent.studio`。
2. 后端环境变量从 `MYAGENT_*` 改为 `AGENT_STUDIO_*`。
3. OpenAI 正式变量只保留：
   - `AGENT_STUDIO_OPENAI_API_KEY`
   - `AGENT_STUDIO_OPENAI_BASE_URL`
   - `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`
4. `application.yml` 的 `spring.ai.openai.*` 只引用上述 `AGENT_STUDIO_OPENAI_*` 变量。
5. 移除主配置和 Compose 中的 `dummy-key`、`test-key` 等伪默认值，正式部署要求 `AGENT_STUDIO_OPENAI_API_KEY` 非空。
6. 增加启动期检查，发现 `OPENAI_API_KEY`、`SPRING_AI_OPENAI_BASE_URL`、`MYAGENT_OPENAI_DEFAULT_MODEL` 时直接失败。
7. 启动期不要对 Key 格式、模型名格式或 Base URL 可达性做严格校验。
8. 设置白名单键从 `myagent.*` 改为 `agent.studio.*`。

### 步骤 05：数据库和 Flyway

1. 默认数据库名改为 `agent_studio`。
2. Compose 数据库用户名固定为 `agent_studio`。
3. Compose 数据库密码只读取 `AGENT_STUDIO_POSTGRES_PASSWORD`。
4. API 容器 JDBC URL 固定为 `jdbc:postgresql://postgres:5432/agent_studio`。
5. 新增 Flyway 迁移，把 `system_setting.setting_key` 旧键更新为新键。
6. 确认应用运行时不读取旧 `myagent.*` 设置键。

### 步骤 06：Docker Compose 正式部署

1. 新增 `11_code/compose.yaml`。
2. `11_code/compose.yaml` 顶层写入 `name: agent-studio`。
3. 新增 `11_code/backend/Dockerfile`。
4. 新增 `11_code/frontend/Dockerfile`。
5. 新增 `11_code/frontend/nginx.conf`。
6. Compose 服务固定为 `postgres`、`api`、`web`。
7. Web 服务固定映射 `18080:80`。
8. API 服务不暴露宿主机端口。
9. PostgreSQL 服务不暴露宿主机端口。
10. Web 代理 `/api`、`/v3/api-docs`、`/swagger-ui.html`、`/swagger-ui`、`/actuator` 到 `api:8080`。
11. Web 代理补齐 Host/Port 转发头；后端补齐 forward headers 配置，确保 `/v3/api-docs` 的 `servers.url` 输出正式入口 `http://127.0.0.1:18080`。

### 步骤 07：开发脚本口径调整

1. 保留 PowerShell 启动脚本时，只标注为开发态入口。
2. 脚本环境变量同步改为 `AGENT_STUDIO_*`。
3. 正式部署文档不再把 PowerShell 脚本、Maven 直跑或 Vite dev server 写成发布方式。

### 步骤 08：文档收口

1. 更新 `README.md`。
2. 更新 `11_code/README.md`。
3. 更新 `13_release/01-V1发布前检查清单-v1.md`。
4. 更新 `13_release/02-V1部署说明-v1.md`。
5. 更新 `3_product_design`、`4_arch_design`、`6_schema_design`、`7_interface_design` 中当前有效文档。
6. 更新 `14_user_manual` 中已有的启动和访问说明。

### 步骤 09：完整验证

1. 执行后端测试。
2. 执行前端测试。
3. 执行前端生产构建。
4. 执行 OpenAPI 刷新和一致性检查。
5. 验证 OpenAPI 示例不包含 `com.myagent` 或 `myagent-backend`。
6. 执行 `docker compose config` 并确认 `name: agent-studio`。
7. 在 `D:\myproject\MyAgent\11_code` 下执行 `docker compose up -d --build`。
8. 访问 `http://127.0.0.1:18080/actuator/health`。
9. 访问 `http://127.0.0.1:18080/api/settings`。
10. 访问 `http://127.0.0.1:18080/v3/api-docs`。
11. 验证 PostgreSQL 迁移和设置键。

## 3. 禁止事项

- 禁止同时保留 `myagent.*` 与 `agent.studio.*` 作为运行时正式配置。
- 禁止同时保留 `MYAGENT_*` 与 `AGENT_STUDIO_*` 作为正式环境变量。
- 禁止继续使用 `OPENAI_API_KEY` 或 `SPRING_AI_OPENAI_BASE_URL` 作为正式入口。
- 禁止通过 `dummy-key`、`test-key` 等伪值让正式部署在缺失真实 OpenAI Key 时继续启动。
- 禁止 OpenAPI 外显示例继续出现 `com.myagent` 或 `myagent-backend`。
- 禁止省略 `11_code/compose.yaml` 顶层 `name: agent-studio`。
- 禁止靠手工修改 `agent-studio-openapi.json` 或 `schema.ts` 修补 `servers.url`。
- 禁止把 Vite dev server 写成正式部署入口。
- 禁止本次变更顺手迁移 Java 根包或源码目录。
- 禁止在 Docker Compose 未实际通过时把状态写为完成。
