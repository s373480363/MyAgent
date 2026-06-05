# 模型供应商目录与运行路由 v1 执行步骤

## 1. 数据库迁移

1. 新增 `model_provider` 表。
2. 新增 `model_offering` 表，包含 `model_key`、`display_name`、`upstream_model_name`、`default_temperature`、`status`。
3. 为 `agent_definition` 新增可空 `default_model_offering_key`。
4. 编写迁移脚本，从现有 `AGENT_STUDIO_OPENAI_*` 和旧默认模型初始化默认供应商与供应项；当需要加密初始化 API Key 时必须使用 `AGENT_STUDIO_SECRET_KEY`。
5. 编写迁移脚本，将历史工作流节点 `config.model` 转换为 `config.modelOfferingKey`，并移除 `config.model`。
6. 编写迁移脚本，将历史 Eval `scoreRule.model` 转换为 `scoreRule.modelOfferingKey`，并移除 `scoreRule.model`。
7. 迁移完成后不得保留旧模型字段作为运行表审计字段；如需审计，写入迁移报告或发布记录。

## 2. 后端领域与应用服务

1. 新增 `modelprovider` 或 `modelcatalog` 包，职责覆盖供应商和模型供应项。
2. 实现供应商创建、更新、启停、密钥更新和连接测试。
3. 实现模型供应项创建、更新、启停、详情查询和按 key 批量查询。
4. API Key 加密、解密和掩码生成必须通过统一 `ModelProviderSecretService` 处理，不允许散落在 Controller、Mapper 或网关中。
5. 普通查询 DTO 只能返回 `apiKeyConfigured`、`apiKeyMask` 等元信息。
6. `AGENT_STUDIO_SECRET_KEY` 必须作为正式 Docker 部署必填环境变量进入 `compose.yaml`，缺失时 `docker compose config` 必须失败；本地开发文档也必须说明设置方式。

## 3. 模型调用路由

1. 重构 `ModelInvocationRequest`，将模型自由文本改为 `modelOfferingKey`，不得携带已解析供应项快照。
2. 新增内部 `ResolvedModelRoute` 或等价对象，由模型模块按 `modelOfferingKey` 解析 `baseUrl`、解密后的 API Key、`upstreamModelName`。
3. 新增 `ModelRequestTracePayload` / `ModelResponseTracePayload` 或等价 Trace 安全 DTO，MODEL_REQUEST 不得直接序列化 `ModelInvocationRequest` 或 `ResolvedModelRoute`。
4. Spring AI 如果不能按请求动态切换 Base URL 和 API Key，应封装可按供应商创建客户端的工厂，不能继续使用单例部署级 `OpenAiChatModel` 作为全部运行入口。
5. MODEL_REQUEST Trace 写入供应商和供应项元数据，不写密钥、不写 Base URL。
6. LLM、REVIEW、SUMMARY 和 Eval LLM 评分统一使用同一套模型供应项解析逻辑。
7. 供应商连接测试必须复用正式 `agent.studio.runtime.default-llm-timeout-seconds` 契约或同一共享超时包装；禁止直接调用上游 SDK 而不加超时边界。
8. 供应商连接测试超时后必须中断调用并返回中文业务错误，不能让页面请求长时间挂起。

## 4. 工作流与 Agent 改造

1. Agent 创建、编辑、详情 DTO 将默认模型改为可空 `defaultModelOfferingKey` 和展示摘要。
2. 工作流节点配置将 `config.model` 改为 `config.modelOfferingKey`。
3. 发布校验校验：
   - 对每个需要调用模型的 LLM 类节点，节点配置或 Agent 默认值最终能解析到模型供应项。
   - 模型供应项存在且启用。
   - 所属供应商存在且启用。
   - 所属供应商 API Key 已配置。
4. 历史数据中的 `config.model` 只能由迁移处理，不得在运行时长期保留兜底读取。

## 5. REST 接口

1. 新增模型供应商 REST Controller，连接测试接口请求体固定为 `{ offeringKey, prompt }`。
2. 新增模型供应项 REST Controller。
3. 为模型供应项提供 `GET /api/model-offerings/{offeringKey}` 和 `GET /api/model-offerings/by-keys`，用于已绑定值回填。
4. 更新 Agent REST DTO 和 OpenAPI 示例。
5. 更新工作流草稿示例，将 LLM 类节点配置改为 `modelOfferingKey`。
6. 更新 Eval 用例示例，将 `scoreRule.model` 改为 `scoreRule.modelOfferingKey`。
7. 更新设置接口，移除 `agent.studio.openai.default-model` 的长期可编辑语义。

## 6. 前端页面

1. 新增模型供应商管理页面和导航入口。
2. 实现供应商列表、创建、编辑、启停、密钥替换、连接测试。
3. 实现模型供应项列表、创建、编辑、启停和当前绑定回填。
4. Agent 创建/编辑页改为模型供应项选择器。
5. Workflow LLM 类节点配置改为模型供应项选择器。
6. Eval 用例评分规则改为模型供应项选择器，不能继续只保留纯 JSON 模型字段入口。
7. 选择器使用分页远程搜索，不能拉全量。
8. 已绑定但当前不可选的供应项必须以“当前绑定”中性占位展示并保留，不能静默清空。

## 7. 测试

1. 增加模型供应商领域服务测试。
2. 增加 API Key 不回显测试。
3. 增加模型供应项发布校验测试。
4. 增加迁移测试，覆盖旧 `default_model`、旧 `config.model` 和旧 `scoreRule.model`。
5. 增加模型网关按不同供应商路由的测试。
6. 增加 MODEL_REQUEST Trace 不包含密钥、Base URL、密文和内部解析对象的测试。
7. 增加前端供应商页面测试。
8. 增加 Agent、Workflow、Eval 模型供应项选择器测试。
9. 增加 OpenAPI 生成和前端类型同步测试。
10. 增加供应商连接测试的后端专用覆盖：成功路径、超时路径、失败消息脱敏和不长期挂起。

## 8. 文档

1. 更新产品总纲、核心概念、工作流和节点体系文档。
2. 更新总体架构、后端架构、数据结构、接口、部署安全文档。
3. 更新 Schema/DDL 设计。
4. 更新 REST 接口设计和示例。
5. 更新发布说明或用户手册中的部署变量说明。

## 9. 禁止事项

- 禁止在节点配置中保存供应商 Base URL 或 API Key。
- 禁止把 `config.model` 和 `config.modelOfferingKey` 同时作为运行成功路径。
- 禁止把 `scoreRule.model` 和 `scoreRule.modelOfferingKey` 同时作为运行成功路径。
- 禁止把 `modelOfferingKey` 和已解析供应项快照同时作为运行事实。
- 禁止直接把 `ModelInvocationRequest` 或内部解析对象整体写入 Trace。
- 禁止把供应商列表写死在前端。
- 禁止普通更新接口用空密钥覆盖已有密钥。
- 禁止连接测试把上游完整鉴权错误或密钥内容返回页面。
- 禁止供应项不可用时自动换模型继续运行。
