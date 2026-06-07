## 说明

1. 本文档用于记录测试人员在验收过程中需要的测试环境信息。
2. 执行真实验收前必须同时阅读 `AGENTS.md` 和 `0_specifications/test_specification.md`。

## 真实业务验收入口

- 真实业务验收必须使用正式 Docker 部署入口。
- 前端可使用本机浏览器访问 Docker 暴露的正式前端地址。
- 不允许用手动启动的后端或前端服务替代正式 Docker 入口完成业务验收。

## LLM 交互

当需要测试和 LLM 的交互时请使用 Poe 进行测试。

对接文档：https://creator.poe.com/docs/external-applications/openai-compatible-api
API_Key:sk-poe-ArQR5TVd3cnfOv_-O0QP0Ds2udWrSz0iFHPyXh2IQr4
Model:gpt-5.4-mini

API Key 仅用于本机验收环境，不得写入验收报告、错误记录或对外说明。
