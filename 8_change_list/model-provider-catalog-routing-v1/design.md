# 模型供应商目录与运行路由 v1 设计

## 1. 设计结论

模型调用的正式事实源从部署级 OpenAI 配置迁移到数据库中的模型供应商目录。

V1 核心模型只保留两类正式对象：

| 层级 | 含义 | 示例 |
| --- | --- | --- |
| `ModelProvider` | 谁提供调用入口 | OpenAI、DeepSeek、硅基流动、OpenRouter |
| `ModelOffering` | 某供应商提供的某个模型调用入口 | siliconflow.qwen2_5_72b、openrouter.qwen2_5_72b |

跨供应商共享的模型身份由 `ModelOffering.modelKey` 表达，不在 V1 单独建立 `ModelDefinition` 目录、页面或 REST API。LLM 节点保存 `modelOfferingKey`，Agent 默认值保存可空 `defaultModelOfferingKey`。页面可以按供应商和模型两级展示，但持久化只保存模型供应项引用。

## 2. 领域对象

### 2.1 ModelProvider

字段：

- `id`
- `providerKey`
- `name`
- `providerType`
- `baseUrl`
- `apiKeyCiphertext`
- `apiKeyMask`
- `status`
- `description`
- `createdAt`
- `updatedAt`

约束：

- `providerKey` 全局唯一。
- `providerType` 本次只允许 `OPENAI_COMPATIBLE`。
- `baseUrl` 必填。
- API Key 只允许通过创建、更新密钥、替换密钥接口写入。
- `apiKeyCiphertext` 是可逆密文，不是掩码、哈希或明文。
- 密钥加解密统一由后端 `ModelProviderSecretService` 处理，推荐使用 AES-GCM；加密主密钥来自部署环境变量 `AGENT_STUDIO_SECRET_KEY`，格式固定为 Base64 编码的 32 字节随机值。
- `AGENT_STUDIO_SECRET_KEY` 是正式 Docker 部署必填项，Compose 必须用必填插值注入 API 容器；缺失时 `docker compose config` 必须失败，应用不得自动生成或使用固定默认主密钥。
- 普通列表和详情接口不返回 `apiKeyCiphertext`。
- `apiKeyMask` 只用于展示，例如 `sk-...abcd` 或 `已配置`。

### 2.2 ModelOffering

字段：

- `id`
- `offeringKey`
- `providerKey`
- `modelKey`
- `displayName`
- `upstreamModelName`
- `defaultTemperature`
- `status`
- `description`
- `createdAt`
- `updatedAt`

约束：

- `offeringKey` 全局唯一，是 LLM 节点和 Agent 默认值的唯一持久化引用。
- `modelKey` 表达跨供应商共享的模型身份，不表达供应商；不同供应商可以拥有相同 `modelKey`。
- `providerKey + upstreamModelName` 在同一供应商内唯一。
- `upstreamModelName` 是真实传给 OpenAI-compatible API 的模型名。
- `status=DISABLED` 的供应项不能被新选择，已发布工作流运行时必须失败并记录清晰错误，不能静默换模型。

## 3. Agent 默认值

Agent 默认模型字段改为可选默认模型供应项：

```json
{
  "defaultModelOfferingKey": "openai.gpt_4_1_mini"
}
```

规则：

- 创建或编辑 Agent 时可以不选择 `defaultModelOfferingKey`。
- 如果选择 `defaultModelOfferingKey`，页面只允许选择启用供应商下的启用供应项。
- LLM 类节点未配置 `modelOfferingKey` 时，可以回退到 Agent 默认 `defaultModelOfferingKey`。
- 工作流没有 LLM、REVIEW、SUMMARY 或 Eval LLM 评分路径时，不要求 Agent 配置默认模型供应项。
- 发布任何需要调用模型的 LLM 类节点时，最终 `modelOfferingKey` 必须能由节点配置或 Agent 默认值解析得到；解析不到时发布失败并定位节点。
- 回退发生时 Trace 必须记录最终使用的 `modelOfferingKey`。
- Agent 默认值不保存供应商 Base URL 或 API Key。

## 4. LLM 类节点配置

LLM、REVIEW、SUMMARY 节点配置：

```json
{
  "modelOfferingKey": "openai.gpt_4_1_mini",
  "temperature": 0.2,
  "userPromptTemplate": "请处理 {inputJson}",
  "systemPromptTemplate": "可选系统提示词"
}
```

规则：

- 新保存路径只写 `modelOfferingKey`，不写 `model`。
- 历史 `config.model` 只允许在迁移中读取并转换，不能作为运行时长期兼容路径。
- 前端模型选择器只展示启用供应商下的启用供应项。
- 模型名称不再是自由文本字段，用户必须从供应项目录选择。
- 温度仍然允许为空；为空时使用 Agent 默认温度或供应项默认温度，具体优先级必须固定。

温度优先级：

```text
node.config.temperature > agent.temperature > model_offering.default_temperature > 空值
```

## 5. 运行时路由

运行时处理流程：

```text
读取节点 modelOfferingKey
  -> 若为空，读取 Agent defaultModelOfferingKey
  -> 查询 ModelOffering
  -> 查询 ModelProvider
  -> 校验供应项和供应商启用
  -> 校验供应商 API Key 已配置
  -> 生成内部 ResolvedModelRoute
  -> 使用 route.baseUrl + route.decryptedApiKey + route.upstreamModelName 调用 OpenAI-compatible API
  -> 使用 ModelRequestTracePayload / ModelResponseTracePayload 写入 Trace
```

运行时对象边界：

- `ModelInvocationRequest` 只能携带 `modelOfferingKey`、提示词、输入、温度和结构化输出标记，不允许携带 `baseUrl`、`apiKey`、`apiKeyCiphertext` 或完整供应项快照。
- `ResolvedModelRoute` 是模型模块内部对象，可包含 `baseUrl`、解密后的 API Key、`upstreamModelName`、供应商状态和供应项状态；该对象不得进入 Trace、REST DTO、前端状态、异常消息或日志。
- `ModelRequestTracePayload` 是可落库 Trace DTO，只能从白名单字段构造，不得直接 `objectMapper.valueToTree(ModelInvocationRequest)` 或序列化内部解析对象。

Trace 中允许记录：

- `providerKey`
- `providerName`
- `modelOfferingKey`
- `modelKey`
- `upstreamModelName`
- `temperature`

Trace 中禁止记录：

- `baseUrl`
- API Key 明文。
- `apiKeyCiphertext`。
- API Key 掩码以外的密钥内容。

## 6. REST API

新增接口分组：

| 接口 | 说明 |
| --- | --- |
| `GET /api/model-providers` | 查询供应商列表 |
| `POST /api/model-providers` | 创建供应商 |
| `GET /api/model-providers/{providerId}` | 查询供应商详情 |
| `PUT /api/model-providers/{providerId}` | 更新供应商非敏感字段 |
| `PUT /api/model-providers/{providerId}/secrets` | 替换或清空 API Key |
| `PUT /api/model-providers/{providerId}/status` | 启停供应商 |
| `POST /api/model-providers/{providerId}/test` | 测试供应商连接 |
| `GET /api/model-offerings` | 查询模型供应项 |
| `GET /api/model-offerings/{offeringKey}` | 按 key 查询单个模型供应项 |
| `GET /api/model-offerings/by-keys` | 按 key 批量查询模型供应项 |
| `POST /api/model-offerings` | 创建模型供应项 |
| `PUT /api/model-offerings/{offeringId}` | 更新模型供应项 |
| `PUT /api/model-offerings/{offeringId}/status` | 启停模型供应项 |

接口规则：

- 供应商详情不返回 API Key 明文。
- 普通供应商更新接口不接收 API Key，避免前端用空值覆盖历史密钥。
- 密钥必须通过 `/secrets` 接口显式替换或清空。
- 供应商连接测试请求体固定为 `{ "offeringKey": "...", "prompt": "ping" }`；`offeringKey` 必填且必须属于被测试供应商，`prompt` 可选。
- 供应商连接测试允许供应商或供应项当前为 `DISABLED`，但必须存在、归属正确且已配置 API Key。
- 供应商连接测试是轻量、有限时的探活接口，不是无界的真实业务运行。V1 必须复用正式平台 LLM 超时契约 `agent.studio.runtime.default-llm-timeout-seconds`，不能为连接测试再发明第二套独立超时真相。
- 供应商连接测试必须在超时窗口内返回成功或失败；超过超时后必须中断调用并返回中文业务错误，不能长期挂起 HTTP 请求。
- 供应商连接测试沿用与正式模型调用一致的路由解析和密钥解密边界，但不能绕开统一超时包装直接裸调上游 SDK。
- 模型供应项列表必须支持 `providerKey`、`status`、`keyword` 分页筛选。
- `GET /api/model-offerings` 用于可选列表，默认只返回可选择项；已绑定但当前不可选的供应项必须通过详情或批量查询接口回填。
- 详情和批量查询接口按 `offeringKey` 查询，不因为供应项或供应商停用而隐藏记录；响应必须包含 `selectable` 和 `unavailableReason`，用于页面显示“当前绑定”。

## 7. 页面设计

新增或调整页面：

- `/model-providers`：模型供应商和供应项管理。
- `/agents` 创建/编辑弹窗：默认模型改为可选模型供应项选择器。
- `/agents/{agentId}/workflow`：LLM 类节点模型字段改为模型供应项选择器。
- `/settings`：不再展示 `agent.studio.openai.default-model` 作为长期可编辑项。

页面规则：

- 创建供应商时可输入 API Key。
- 编辑供应商详情时只展示密钥是否已配置和掩码，不展示明文。
- 模型供应项必须归属于一个供应商，并保存 `modelKey` 表达跨供应商模型身份。
- LLM 节点选择器显示供应商、供应项名称、上游模型名和状态。
- Agent 和 Workflow 的模型供应项选择器使用分页远程搜索；打开已有对象时，先按当前绑定 `offeringKey` 批量查询详情，再加载可选列表。

## 8. Eval LLM 评分

Eval 用例 `scoreRule` 中的模型字段统一改为模型供应项：

```json
{
  "enabled": true,
  "modelOfferingKey": "openai.gpt_4_1_mini",
  "temperature": 0,
  "promptTemplate": "请根据输入给出评分 JSON：{payload}"
}
```

规则：

- `scoreRule` 为空或 `enabled=false` 时不执行 LLM 辅助评分。
- `enabled=true` 时，模型供应项优先级为 `scoreRule.modelOfferingKey > agent.defaultModelOfferingKey`。
- 如果最终无法解析到 `modelOfferingKey`，LLM 辅助评分结果记为 `FAILED` 并返回中文错误；确定性断言结果不被覆盖。
- `scoreRule.model` 是旧字段，只允许在迁移中转换为 `scoreRule.modelOfferingKey`，不能作为运行时长期读取路径。
- Eval 评分调用和 LLM、REVIEW、SUMMARY 节点共用同一套模型供应项解析、密钥解密、Trace 安全 DTO 和网关调用逻辑。

## 9. 迁移规则

迁移必须一步到位，不能长期保留两套运行事实。

迁移输入：

- `AGENT_STUDIO_OPENAI_BASE_URL`
- `AGENT_STUDIO_OPENAI_API_KEY`
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`
- `agent_definition.default_model`
- `workflow_version.nodes_json[*].config.model`
- `eval_case.score_rule_json.model`
- `system_setting['agent.studio.openai.default-model']`

迁移输出：

- `model_provider`
- `model_offering`
- `agent_definition.default_model_offering_key`
- `workflow_version.nodes_json[*].config.modelOfferingKey`
- `eval_case.score_rule_json.modelOfferingKey`

迁移后：

- 新代码不再读取 `system_setting['agent.studio.openai.default-model']` 作为默认模型。
- 新代码不再读取节点 `config.model` 作为成功运行路径。
- 新代码不再读取 `scoreRule.model` 作为成功运行路径。
- `AGENT_STUDIO_OPENAI_*` 仅用于初始化默认供应商，不作为长期运行时路由配置。
- 不保留旧字段作为审计字段；如需追溯迁移映射，应写入迁移测试报告或发布记录，不在运行表中保留第二套模型事实。

## 10. 安全规则

- API Key 不通过普通 GET 接口返回。
- API Key 必须以可逆密文存储，不能明文落库，也不能只保存掩码导致运行时不可调用。
- API Key 不写入 Trace。
- API Key 不写入前端状态快照、OpenAPI 示例或测试断言输出。
- MODEL_REQUEST Trace 必须使用 Trace 安全 DTO，不允许直接序列化模型调用请求或内部解析对象。
- 测试供应商连接时，失败消息返回中文摘要，不返回完整上游鉴权报文。
- 测试供应商连接时，失败消息除中文摘要外，还必须满足“有限时返回”；不能依赖底层 SDK 默认网络超时导致页面长时间无响应。
- 本次仍按本机/内网单用户部署边界处理，不扩展用户权限模型。

## 11. 禁止事项

- 禁止把 Base URL、API Key 放进工作流节点 `config`。
- 禁止 LLM 节点同时保存 `model` 和 `modelOfferingKey` 作为两个成功路径。
- 禁止用 `providerKey + modelKey` 分别独立解释运行目标。
- 禁止把 `modelOfferingKey` 和“已解析供应项快照”同时作为运行事实。
- 禁止供应商密钥通过普通详情接口回显。
- 禁止发布校验在模型供应项不可用时自动换成默认模型。
- 禁止把模型供应商做成前端枚举或硬编码列表。
