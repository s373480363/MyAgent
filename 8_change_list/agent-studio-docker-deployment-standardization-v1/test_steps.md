# Agent Studio 命名与 Docker 部署规范化验收步骤

## 1. 验收目标

本验收用于确认本次变更已经把对外交付命名、OpenAI 环境变量、PostgreSQL 契约、宿主机访问入口和 Docker Compose 部署方式收口为唯一真相。

Docker 环境不可用时，本变更不能判定为完整通过。

## 2. 静态命名验收

### 2.1 当前正式范围旧名检查

```powershell
rg -n "MYAGENT|myagent-backend|myagent-frontend|myagent-openapi|jdbc:postgresql://.*myagent|myagent\\.(openai|runtime|trace)" README.md 3_product_design 4_arch_design 6_schema_design 7_interface_design 13_release 14_user_manual compose.yaml 11_code/backend/src/main/resources 11_code/frontend/package.json 11_code/frontend/package-lock.json 11_code/frontend/openapi 11_code/frontend/scripts
```

验收口径：

- 当前正式源码配置、构件名、脚本、发布文档和契约文档不得再把旧名作为正式命名使用。
- `com.myagent` Java 包名在本次变更中允许保留。
- `com.myagent` 不允许出现在 OpenAPI 示例、OpenAPI JSON 和前端生成契约中。
- `OPENAI_API_KEY`、`SPRING_AI_OPENAI_BASE_URL`、`MYAGENT_OPENAI_DEFAULT_MODEL` 如出现在 fail-fast 校验类或对应测试中，不按本项失败；但它们不得出现在正式配置、Compose、发布文档、前端生成契约或成功启动路径中。
- 历史验收记录和旧变更记录不纳入本项失败判断。

### 2.2 新命名检查

```powershell
rg -n "Agent Studio|智能体工作台|agent-studio|agent_studio|AGENT_STUDIO|agent\\.studio" README.md 3_product_design 4_arch_design 6_schema_design 7_interface_design 11_code 13_release 14_user_manual compose.yaml
```

验收口径：

- 新产品名、构件名、镜像名、数据库名、配置前缀和环境变量前缀均能在对应文件中找到。

## 3. OpenAI 环境变量验收

```powershell
rg -n "\bOPENAI_API_KEY\b|\bSPRING_AI_OPENAI_BASE_URL\b|\bMYAGENT_OPENAI_DEFAULT_MODEL\b" README.md 13_release compose.yaml 11_code/backend/src/main/resources 11_code/frontend
```

验收口径：

- 不应再出现旧 OpenAI 环境变量作为正式配置。
- fail-fast 校验类和测试文件中可以出现旧变量字符串，但只能用于拒绝旧变量。
- 正式运行路径不得再以 `dummy-key`、`test-key` 等伪值作为成功启动前提。

还必须检查后端 Java 中旧变量的出现位置：

```powershell
rg -n "\bOPENAI_API_KEY\b|\bSPRING_AI_OPENAI_BASE_URL\b|\bMYAGENT_OPENAI_DEFAULT_MODEL\b" 11_code/backend/src/main/java 11_code/backend/src/test
```

验收口径：

- 匹配结果只能来自旧变量 fail-fast 校验类和对应测试。
- 不得出现在配置绑定类、默认值、成功路径初始化逻辑或业务调用逻辑中。

还必须补充一次启动失败验证：

```powershell
docker compose run --rm -e OPENAI_API_KEY=invalid-old-key api
```

验收口径：

- API 应启动失败，并输出中文错误，说明旧环境变量不是正式入口。

还必须补充一次正式 API Key 缺失验证：

```powershell
Remove-Item Env:AGENT_STUDIO_OPENAI_API_KEY -ErrorAction SilentlyContinue
$env:AGENT_STUDIO_POSTGRES_PASSWORD='agent_studio_dev_password'
docker compose config
```

验收口径：

- 命令应失败，并明确提示 `AGENT_STUDIO_OPENAI_API_KEY` 缺失。
- 不允许通过共享主配置或 Compose 默认值把缺失 Key 伪装成成功部署。

## 4. 后端验收

```powershell
cd D:\myproject\MyAgent\11_code\backend
mvn -q test
```

验收口径：

- 后端测试全部通过。
- `groupId=syc` 和 `artifactId=agent-studio-api` 不影响测试。
- 设置白名单只接受 `agent.studio.*`。

## 5. 前端验收

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm test -- --run
npm run build
```

验收口径：

- 前端测试通过。
- TypeScript 编译通过。
- Vite 生产构建通过。
- package name 为 `agent-studio-web`。

## 6. OpenAPI 验收

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm run openapi:refresh
npm run openapi:check
```

验收口径：

- `openapi/agent-studio-openapi.json` 能从真实 `/v3/api-docs` 下载生成。
- `src/api/generated/schema.ts` 由同一份 OpenAPI 快照生成。
- 再次执行 `openapi:check` 不产生差异。
- 通过正式入口下载的 OpenAPI 中，`servers.url` 必须为 `http://127.0.0.1:18080`。

还必须检查外显示例：

```powershell
rg -n "example\\s*=\\s*\\\"(com\\.myagent|myagent-backend)" 11_code/backend/src/main/java
rg -n "com\\.myagent|myagent-backend" 11_code/frontend/openapi/agent-studio-openapi.json
rg -n "com\\.myagent|myagent-backend" 11_code/frontend/src/api/generated/schema.ts
```

验收口径：

- 三条命令均不应返回匹配结果。
- Java 类型示例应使用 `syc.agentstudio.example.*`。
- 服务名示例应使用 `agent-studio-api`。

还必须检查 OpenAPI 正式入口地址：

```powershell
curl.exe -sS http://127.0.0.1:18080/v3/api-docs
```

验收口径：

- 返回 JSON 中 `servers[0].url` 必须为 `http://127.0.0.1:18080`。
- 不允许退化成 `http://127.0.0.1`。
- 不允许出现 `http://api:8080` 等内部地址。

## 7. Docker Compose 配置验收

```powershell
cd D:\myproject\MyAgent
docker compose config
```

验收口径：

- Compose 配置解析通过。
- 输出中存在 `name: agent-studio`。
- 服务包含 `postgres`、`api`、`web`。
- Web 端口固定为 `18080:80`。
- API 不暴露宿主机端口。
- PostgreSQL 不暴露宿主机端口。
- PostgreSQL 数据库名和用户名固定为 `agent_studio`。
- PostgreSQL 密码来自 `AGENT_STUDIO_POSTGRES_PASSWORD`。

建议显式执行：

```powershell
docker compose config | Select-String -Pattern '^name: agent-studio$'
```

验收口径：

- 命令必须匹配到 `name: agent-studio`。

## 8. Docker Compose 启动验收

```powershell
cd D:\myproject\MyAgent
$env:AGENT_STUDIO_POSTGRES_PASSWORD='agent_studio_dev_password'
docker compose up -d --build
```

验收口径：

- 三个服务均正常启动。
- API 容器日志显示 Flyway 迁移成功或数据库 schema 已是最新。
- Web 容器正常提供静态资源。
- PostgreSQL 使用持久化 volume。

## 9. 接口与代理验收

```powershell
curl.exe -sS -D - http://127.0.0.1:18080/actuator/health
curl.exe -sS -D - http://127.0.0.1:18080/api/settings
curl.exe -sS -D - http://127.0.0.1:18080/v3/api-docs
```

验收口径：

- `/actuator/health` 返回 `UP`。
- `/api/settings` 通过 Web 代理访问成功。
- `/v3/api-docs` 通过 Web 代理访问成功。
- 浏览器访问 `http://127.0.0.1:18080/` 可以打开 Agent Studio 页面。

## 10. 数据库验收

```powershell
docker compose exec postgres psql -U agent_studio -d agent_studio -c "select version from flyway_schema_history order by installed_rank;"
docker compose exec postgres psql -U agent_studio -d agent_studio -c "select setting_key from system_setting order by setting_key;"
```

验收口径：

- 数据库名为 `agent_studio`。
- 数据库用户名为 `agent_studio`。
- Flyway 迁移记录存在。
- fresh install 时 `system_setting` 允许为空。
- 如表内已有记录，`system_setting.setting_key` 必须使用 `agent.studio.*`，不得保留作为当前配置的 `myagent.*`。
- 旧键迁移路径必须由自动化测试覆盖，不能只依赖 fresh install 现场 SQL。

## 11. 发布文档验收

检查以下正式文档：

- `README.md`
- `3_product_design`
- `4_arch_design`
- `6_schema_design`
- `7_interface_design`
- `11_code/README.md`
- `13_release/01-V1发布前检查清单-v1.md`
- `13_release/02-V1部署说明-v1.md`
- `14_user_manual`

验收口径：

- 正式部署入口只写 Docker Compose。
- PowerShell 脚本和 Vite dev server 只作为开发态说明。
- PostgreSQL、API、Web 三服务拓扑表达清楚。
- 正式访问入口固定为 `http://127.0.0.1:18080`。
- 敏感配置通过环境变量注入，不进入前端和系统设置接口。

## 12. 通过条件

- 静态命名验收通过。
- OpenAI 旧变量 fail-fast 验收通过。
- OpenAPI 外显示例旧名清理验收通过。
- Compose project 名验收通过。
- 后端、前端、OpenAPI 验收通过。
- Docker Compose 构建、启动、健康检查、Web 代理和数据库验收通过。
- 当前发布文档不存在正式入口双轨。
