# Agent Studio 命名与 Docker 部署规范化实施计划

## 1. 文档目的

本文档定义本次命名规范化和 Docker 正式部署规范化的实施计划。目标是在不改变 V1 业务语义的前提下，把对外交付命名、运行配置、数据库契约和正式部署入口收口到唯一口径。

## 2. 编写依据

- `AGENTS.md`
- `0_specifications/develop_specification.md`
- `0_specifications/code_review_specification.md`
- `3_product_design/Agent管理平台-产品设计-总纲-v1.md`
- `4_arch_design/Agent管理平台-架构设计-总纲-v1.md`
- `4_arch_design/13-部署安全与质量架构设计-v1.md`
- `13_release/02-V1部署说明-v1.md`
- 用户确认：正式命名采用 `Agent Studio` / `智能体工作台` / `agent-studio`
- 用户确认：Maven `groupId` 使用 `syc`
- 用户确认：正式 Docker 部署默认包含 PostgreSQL

## 3. 范围裁剪

开发评审指出原方案把 Docker 部署规范化和仓库级命名重构绑定在同一个 change 里，影响面过大。该判断成立。

本次变更保留以下内容：

- Maven 坐标、Spring application name、前端 package name。
- OpenAPI 文件名。
- OpenAPI 对外示例。
- Spring 配置前缀。
- 正式环境变量前缀。
- Compose PostgreSQL 数据库名、用户名和密码变量。
- Dockerfile、Compose、Web 反向代理和正式部署说明。
- 当前正式文档中的产品名和部署口径。

本次变更移出以下内容：

- `11_code/backend` 到 `11_code/api` 的目录重命名。
- `11_code/frontend` 到 `11_code/web` 的目录重命名。
- Java 根包 `com.myagent` 到 `syc.agentstudio` 的迁移。
- MyBatis XML namespace 随 Java 根包迁移的批量调整。

这些移出项后续可作为独立重构变更处理，避免影响 Docker 发布口径落地。

## 4. 实施阶段

### 阶段一：冻结正式契约

1. 确认正式产品名为 `Agent Studio`，中文名为 `智能体工作台`。
2. 确认正式部署入口为在 `D:\myproject\MyAgent\11_code` 下执行 `docker compose up -d --build`。
3. 确认正式浏览器入口为 `http://127.0.0.1:18080`。
4. 确认 API 和 PostgreSQL 不暴露宿主机端口。
5. 确认数据库名和用户名固定为 `agent_studio`。
6. 确认数据库密码只使用 `AGENT_STUDIO_POSTGRES_PASSWORD`。
7. 确认 OpenAI 正式环境变量只使用 `AGENT_STUDIO_OPENAI_*`。

### 阶段二：构件与配置命名收口

1. Maven `groupId` 改为 `syc`。
2. Maven `artifactId` 和 `name` 改为 `agent-studio-api`。
3. Spring `application.name` 改为 `agent-studio-api`。
4. 前端 `package.json` 和 `package-lock.json` name 改为 `agent-studio-web`。
5. OpenAPI 快照改名为 `agent-studio-openapi.json`。
6. 前端 OpenAPI 下载、生成、检查脚本同步改名。
7. OpenAPI 外显示例从 `com.myagent...`、`myagent-backend` 改为 `syc.agentstudio.example...`、`agent-studio-api`。
8. `myagent` Spring 配置前缀改为 `agent.studio`。
9. `MYAGENT_*` 环境变量改为 `AGENT_STUDIO_*`。

### 阶段三：OpenAI 环境变量收口

1. `application.yml` 中 Spring AI 相关配置只引用 `AGENT_STUDIO_OPENAI_API_KEY`、`AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_DEFAULT_MODEL`。
2. 删除文档和脚本中对 `OPENAI_API_KEY`、`SPRING_AI_OPENAI_BASE_URL`、`MYAGENT_OPENAI_DEFAULT_MODEL` 的正式引用。
3. 增加启动期 fail-fast 检查，阻断旧 OpenAI 环境变量。
4. 移除正式运行路径中的 `dummy-key`、`test-key` 等伪默认值，`AGENT_STUDIO_OPENAI_API_KEY` 改为正式必填。
5. 测试旧变量存在时应用启动失败，避免旧变量成为事实入口。
6. 测试缺失正式 API Key 时正式部署无法成功启动，但不要校验 Key 格式或模型名格式。

### 阶段四：数据库与设置键收口

1. Compose 固定 `POSTGRES_DB=agent_studio`。
2. Compose 固定 `POSTGRES_USER=agent_studio`。
3. Compose 要求 `AGENT_STUDIO_POSTGRES_PASSWORD`，并同时注入 PostgreSQL 和 API。
4. API 容器内 JDBC URL 固定为 `jdbc:postgresql://postgres:5432/agent_studio`。
5. 系统设置白名单键从 `myagent.*` 改为 `agent.studio.*`。
6. 新增 Flyway 迁移，把 `system_setting.setting_key` 中旧键更新为新键。
7. 应用运行时不再读取旧 `myagent.*` 设置键。

### 阶段五：Docker 正式部署落地

1. 新增 `11_code/compose.yaml`。
2. `11_code/compose.yaml` 顶层写入 `name: agent-studio`。
3. 新增 `11_code/backend/Dockerfile`。
4. 新增 `11_code/frontend/Dockerfile`。
5. 新增 `11_code/frontend/nginx.conf`。
6. Compose 服务固定为 `postgres`、`api`、`web`。
7. Web 服务固定映射 `18080:80`。
8. API 和 PostgreSQL 不映射宿主机端口。
9. Web 代理 `/api`、`/v3/api-docs`、`/swagger-ui.html`、`/swagger-ui`、`/actuator`。
10. PostgreSQL 使用命名 volume 持久化。
11. Compose 不写入真实 OpenAI API Key，也不允许用伪值兜底。
12. 通过正式 Web 入口取得的 OpenAPI `servers.url` 必须保持 `http://127.0.0.1:18080`。

### 阶段六：文档收口

1. 更新根 README。
2. 更新 `11_code/README.md`。
3. 更新 `13_release/02-V1部署说明-v1.md`。
4. 更新 `13_release/01-V1发布前检查清单-v1.md`。
5. 更新 `3_product_design`、`4_arch_design`、`6_schema_design`、`7_interface_design` 中涉及命名、配置、部署和数据库口径的当前文档。
6. 如 `14_user_manual` 已有用户入口说明，也必须同步更新。

### 阶段七：验证与验收

1. 后端测试通过。
2. 前端测试和生产构建通过。
3. OpenAPI 刷新和一致性检查通过。
4. Docker Compose 配置校验通过。
5. Docker Compose project 名校验为 `agent-studio`。
6. OpenAPI 示例不再包含 `com.myagent` 或 `myagent-backend`。
7. Docker Compose 构建和启动通过。
8. `http://127.0.0.1:18080/actuator/health` 返回 `UP`。
9. `http://127.0.0.1:18080/api/settings` 可访问。
10. `http://127.0.0.1:18080/v3/api-docs` 中 `servers.url` 为 `http://127.0.0.1:18080`。
11. PostgreSQL 中 Flyway 迁移完成；fresh install 允许 `system_setting` 为空，但已有记录时设置键必须为 `agent.studio.*`。

## 5. 风险控制

- 不做目录和 Java 包名迁移，避免本次部署变更被 MyBatis namespace、包扫描、测试路径等问题拖住。
- 对旧 OpenAI 环境变量 fail-fast，避免 Spring Boot relaxed binding 形成隐藏入口。
- 固定 Web 宿主机端口为 `18080`，避免测试命令和 Compose 实现不一致。
- 固定 Compose 数据库名和用户名，避免部署文档、psql 验收和 API 配置各自解释。
- 固定 Compose 顶层 `name: agent-studio`，避免 Docker 运行态命名退回仓库目录名。
- 清理 OpenAPI 外显示例，避免内部旧包名继续出现在对外契约中。
- 静态命名验收必须覆盖 `6_schema_design` 和 `7_interface_design`，避免正式契约文档残留旧口径。
