# 模型供应商目录与运行路由 v1 状态

当前状态：开发完成，且已完成一轮真实的从正式部署到页面验收的复验。

## 已确认决策

- 变更名称：`model-provider-catalog-routing-v1`
- V1 只支持 `OPENAI_COMPATIBLE` 供应商。
- 运行事实只保留 `modelOfferingKey`，不保留旧 `model` 字段和解析后快照双轨事实。
- V1 只实现 `model_provider` 与 `model_offering`，不引入独立 `ModelDefinition` 目录。
- 模型调用请求、内部路由解析对象、Trace 白名单 DTO 严格分离。
- API Key 以可逆密文落库，`AGENT_STUDIO_SECRET_KEY` 为正式部署必填主密钥，缺失时 `docker compose config` 直接失败。
- 供应商连接测试复用正式 LLM 超时契约 `agent.studio.runtime.default-llm-timeout-seconds`，不新增第二套 provider-test timeout 配置。

## 已完成事项

- 后端已完成模型供应商、模型供应项、运行时路由解析、Trace 安全 DTO、可逆密钥加解密、Eval 路由收口和供应商连接测试统一超时修复。
- 前端已完成模型供应商页面、模型供应项选择器、Agent 默认模型供应项选择、Workflow LLM 节点选择、Eval 评分模型选择，以及供应商连接测试失败提示补齐。
- OpenAPI 文档、前端生成类型、部署文档、发布检查清单和变更文档已经同步。
- 后端自动化测试、后端打包、前端既有页面测试、前端构建均已通过。
- 已执行 `docker compose down -v` fresh install、`docker compose up --build -d` 正式入口重建、手工 API 联调和浏览器真实页面路径验收。
- 供应商连接测试的成功路径与超时路径都已在真实部署页面复验：
  - 成功时页面展示结果 JSON，并提示“连接测试已完成。”
  - 超时时页面展示中文错误“模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。”

## 当前验收结论

- 这次变更已经收口到“模型供应商 + 模型供应项 + `modelOfferingKey` 路由”的唯一运行事实。
- 架构师指出的 `provider test` 超时旁路问题已修复，当前实现没有再引入双规真相或保守兼容止血方案。
- 当前仓库中的正式验收结论以 `test_result/2026-06-05-供应商连接测试统一超时修复验收记录-v1.md` 为准。

## 非阻塞说明

- 如果后续要补“真实第三方供应商有效密钥”的联调记录，需要在独立环境提供真实密钥后另行执行；这不影响本次 V1 目录与路由收口验收结论。
