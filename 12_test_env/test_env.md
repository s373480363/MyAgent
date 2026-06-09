## 说明

1. 本文档用于记录测试人员在验收过程中需要的测试环境信息。
2. 执行真实验收前必须同时阅读 `AGENTS.md` 和 `0_specifications/test_specification.md`。

## 真实业务验收入口

- 真实业务验收必须使用正式 Docker 部署入口。
- 正式 Docker 部署命令在 `D:\myproject\MyAgent\11_code` 目录执行。
- 前端可使用本机浏览器访问 Docker 暴露的正式前端地址。
- 不允许用手动启动的后端或前端服务替代正式 Docker 入口完成业务验收。

## 自动化测试入口

后端自动化测试使用本机工具链执行，不需要另起 Maven 容器。

```powershell
$env:JAVA_HOME = (Resolve-Path 'D:\myproject\MyAgent\9_dependency\tools\jdk21\jdk-21.0.11+10').Path
$mvnBin = (Resolve-Path 'D:\myproject\MyAgent\9_dependency\tools\maven-3.9.11\apache-maven-3.9.11\bin').Path
$env:PATH = "$env:JAVA_HOME\bin;$mvnBin;$env:PATH"

cd D:\myproject\MyAgent\11_code\backend
mvn clean test
```

执行后端全量测试前必须确认 Docker Desktop 正常运行。部分后端测试会通过 Testcontainers 临时启动 PostgreSQL 容器；如果 Docker 不可访问，这部分测试会被跳过，不能作为完整后端自动化验收结论。

如确需在 Maven Docker 容器内执行后端测试，必须挂载宿主机 Docker socket；否则 Testcontainers 无法访问 Docker。

前端自动化测试使用本机 Node/npm 执行。

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm test -- --run
npm run build
npm run openapi:check
```

## LLM 交互

当需要测试和 LLM 的交互时请使用 Poe 进行测试。

对接文档：https://creator.poe.com/docs/external-applications/openai-compatible-api
API_Key:sk-poe-ArQR5TVd3cnfOv_-O0QP0Ds2udWrSz0iFHPyXh2IQr4
Model:gpt-5.4-mini

API Key 仅用于本机验收环境，不得写入验收报告、错误记录或对外说明。
