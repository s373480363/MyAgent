# Agent Studio 命名与 Docker 部署规范化状态

当前状态：验收完成。

## 1. 本轮已完成事项

- 正式部署入口固定为在 `D:\myproject\MyAgent\11_code` 下执行 `docker compose up -d --build`。
- 正式访问入口固定为 [http://127.0.0.1:18080](http://127.0.0.1:18080)。
- `11_code/compose.yaml` 顶层固定为 `name: agent-studio`。
- PostgreSQL 正式契约固定为 `agent_studio` / `agent_studio`。
- 正式 OpenAI 变量只保留 `AGENT_STUDIO_OPENAI_*`。
- `AGENT_STUDIO_OPENAI_API_KEY` 已改为正式部署必填，缺失时 `docker compose config` 直接失败。
- 旧 OpenAI 变量在非测试启动路径会 fail-fast。
- Nginx 代理与后端 forward headers 已收口，`/v3/api-docs` 真实输出的 `servers[0].url` 为 `http://127.0.0.1:18080`。
- OpenAPI 快照与前端生成类型已从真实运行态重新刷新。
- 发布说明、发布前检查清单、仓库 README 和代码目录 README 已同步到当前正式契约。

## 2. 本轮验收结论

- 后端 `mvn -q test` 通过。
- 前端 `npm test -- --run` 通过。
- 前端 `npm run build` 通过。
- 前端 `npm run openapi:refresh` 和 `npm run openapi:check` 通过。
- Docker 真实部署验收通过：
  - 缺失 `AGENT_STUDIO_OPENAI_API_KEY` 时，`docker compose config` 失败。
  - 设置正式 Key 后，在 `D:\myproject\MyAgent\11_code` 下执行 `docker compose up -d --build` 可启动 `postgres`、`api`、`web`。
  - `/actuator/health` 返回 `UP`。
  - `/api/settings` 返回 7 个白名单设置项。
  - `/v3/api-docs` 返回的 `servers[0].url` 为 `http://127.0.0.1:18080`。
  - `flyway_schema_history` 为 `1, 2, 3`。
  - fresh install 场景下 `system_setting` 为空表。

## 3. 仍保留但不阻塞本次验收的事项

- 前端生产构建仍有 Vite chunk size warning，但不影响本次正式契约、部署入口和 OpenAPI 验收结论。

## 4. 验收记录

- 详细命令与结果见 `test_result/2026-06-03-验收记录.md`。
