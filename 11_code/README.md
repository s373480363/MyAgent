# Agent Studio V1 代码目录

本目录承载 Agent Studio V1 的正式代码实现。

## 目录结构

```text
11_code/
  backend/   Spring Boot API 工程
  frontend/  React + Vite Web 工程
  scripts/   开发态启动与联调脚本
```

## 正式部署入口

唯一正式部署入口：

```powershell
cd D:\myproject\MyAgent\11_code
docker compose up -d --build
```

正式部署文件：

- `compose.yaml`
- `.env`

唯一正式浏览器入口：

- [http://127.0.0.1:18080](http://127.0.0.1:18080)

正式部署契约：

- Web 通过 `18080:80` 对外暴露。
- API 不直接暴露宿主机端口。
- PostgreSQL 不直接暴露宿主机端口。
- Compose 顶层项目名固定为 `agent-studio`。
- `AGENT_STUDIO_SECRET_KEY` 是正式部署必填项，必须是 Base64 编码的 32 字节随机值；缺失时 `docker compose config` 必须失败。

## 开发态入口

后端开发脚本：

```powershell
cd D:\myproject\MyAgent\11_code
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-backend-local.ps1
```

后端开发态可选覆盖项：

- `AGENT_STUDIO_DATASOURCE_URL`
- `AGENT_STUDIO_DATASOURCE_USERNAME`
- `AGENT_STUDIO_DATASOURCE_PASSWORD`
- `AGENT_STUDIO_SERVER_PORT`
- `AGENT_STUDIO_SECRET_KEY`
- `AGENT_STUDIO_OPENAI_BASE_URL`
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`

前端开发脚本：

```powershell
cd D:\myproject\MyAgent\11_code
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-frontend-dev.ps1
```

默认开发入口：

- [http://127.0.0.1:5173](http://127.0.0.1:5173)

开发态代理说明：

- 浏览器仍通过相对路径访问 `/api`。
- Vite dev server 默认代理到 `http://127.0.0.1:8080`。
- `AGENT_STUDIO_BACKEND_DEV_TARGET` 只用于开发态临时改写代理目标。

## OpenAPI 产物

唯一契约来源是后端 `/v3/api-docs`。

前端保留两个派生产物：

- `frontend/openapi/agent-studio-openapi.json`
- `frontend/src/api/generated/schema.ts`

刷新命令：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm run openapi:refresh
```

默认从正式 Web 入口下载 OpenAPI；如需指向本地开发态 API，可显式设置：

```powershell
$env:AGENT_STUDIO_OPENAPI_BASE_URL='http://127.0.0.1:8080'
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
