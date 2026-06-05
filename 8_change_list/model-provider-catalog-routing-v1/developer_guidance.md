# 模型供应商目录与运行路由 v1 开发说明

## 1. 架构预期

本次不是给 LLM 节点多加两个字段。正确目标是把模型调用入口产品化：

- 供应商是系统资源。
- 模型供应项是可运行入口，同时用 `modelKey` 表达跨供应商模型身份。
- Agent 和 LLM 节点只引用模型供应项。

节点不能保存 Base URL、API Key 或供应商密钥。否则工作流版本会携带敏感部署信息，并且同一个密钥会散落在多个历史版本中，后续无法安全轮换。

## 2. 为什么保存 modelOfferingKey

不同供应商可以提供同一个开源模型，因此不能简单认为模型归属于供应商。

但运行时真正需要的不是抽象模型名，而是一个可调用入口：

```text
供应商 + 上游模型名 + Base URL + API Key + 状态
```

`modelOfferingKey` 正好表达这个组合。页面可以展示供应商和模型两级选择，但持久化保存一个供应项引用，避免 `providerKey` 和 `modelKey` 分别解释后产生不一致。

V1 不做独立 `ModelDefinition` 目录。原因是当前没有独立消费方，也没有必要让用户维护一套脱离供应商的模型主数据。跨供应商同模型场景通过不同供应项共享同一个 `modelKey` 解决，例如 `openrouter.qwen2_5_72b` 和 `siliconflow.qwen2_5_72b` 都可以写 `modelKey=qwen2.5-72b`。

## 3. 迁移不是兼容分支

历史 `config.model` 和 `agent_definition.default_model` 只能在迁移阶段读取。

迁移完成后：

- 新保存路径写 `modelOfferingKey`。
- 发布校验按 `modelOfferingKey` 校验。
- 运行时按 `modelOfferingKey` 路由。
- 不能保留“如果没有 `modelOfferingKey` 就继续读 `model`”的长期成功路径。
- Eval 评分的旧 `scoreRule.model` 同样只能在迁移阶段读取，迁移后写 `scoreRule.modelOfferingKey`。

如果历史数据无法迁移，应在发布校验或运行前给出明确错误，要求用户重新选择模型供应项。

## 4. 密钥处理

API Key 的规则必须严格：

- 创建供应商时可以提交 API Key。
- 替换密钥必须走独立 `/secrets` 接口。
- 普通更新接口不接收 API Key。
- 普通查询接口不返回 API Key。
- OpenAPI 示例不得出现密钥明文。
- Trace 不得记录密钥。
- 数据库存储的是可逆密文 `apiKeyCiphertext`，不是掩码、哈希或明文。
- 掩码 `apiKeyMask` 只用于展示，不能作为运行时密钥来源。
- `AGENT_STUDIO_SECRET_KEY` 固定为 Base64 编码的 32 字节随机值，用于 API Key 可逆加密。
- `AGENT_STUDIO_SECRET_KEY` 是正式 Docker 部署必填项，必须由部署者提供；缺失时 `docker compose config` 就应失败，后端不得在正式部署中自动生成主密钥或使用固定默认值。

供应商连接测试的请求体固定为：

```json
{
  "offeringKey": "openai.gpt_4_1_mini",
  "prompt": "ping"
}
```

连接测试不能自动取 Agent 默认模型或供应商下第一条供应项。`offeringKey` 必须属于被测试供应商，`prompt` 为空时后端使用 `ping`。

这里还要再补一个架构要求：连接测试不是普通工作流运行，但它也不是可以无限等待的“裸 SDK 调用”。它的职责是帮助用户验证“这条供应商入口是否可用”，因此必须是一个轻量、有限时、可预测返回的探活动作。

V1 不允许为它再引入一套新的超时配置键。当前系统已经有正式的 `agent.studio.runtime.default-llm-timeout-seconds`，这就是模型调用的统一时间边界。连接测试必须复用这个契约，或者复用同一套共享超时包装。这样做的原因很简单：如果正式节点调用和连接测试各有一套超时真相，后面页面调试、运行排障和配置解释都会分裂。

开发实现时应满足三点：

- `POST /api/model-providers/{providerId}/test` 必须在超时窗口内返回。
- 超时后必须主动中断当前调用，并返回中文业务错误。
- 不能继续保持“应用服务直接调用上游 SDK，完全依赖底层默认网络超时”的实现。

本项目当前是本机或内网单用户部署，不代表可以把密钥明文返回前端。

## 5. 运行路由

运行时必须先解析供应项，再调用模型：

```text
modelOfferingKey
  -> ModelOffering
  -> ModelProvider
  -> provider.baseUrl + provider.apiKey + offering.upstreamModelName
```

实现时必须拆成三类对象：

- `ModelInvocationRequest`：调用请求，只保存 `modelOfferingKey`、提示词、输入、温度和结构化输出标记。
- `ResolvedModelRoute`：模型模块内部解析对象，可以包含 Base URL 和解密 API Key，但不能进入 Trace、日志、REST DTO 或前端状态。
- `ModelRequestTracePayload`：Trace 安全 DTO，只保存 `providerKey`、`providerName`、`modelOfferingKey`、`modelKey`、`upstreamModelName`、`temperature` 等白名单字段。

当前执行器直接 `objectMapper.valueToTree(request)` 写 Trace 的方式必须改掉。即使 request 当前不含密钥，也不能形成“以后往 request 加内部字段就自动泄露”的结构。

如果现有 Spring AI `OpenAiChatModel` 是单例并绑定启动配置，就不能继续作为全部供应商的唯一入口。需要封装一个按供应商配置创建或复用客户端的工厂。

当前这条要求也适用于供应商连接测试。不要因为它是“测试接口”就绕开正式模型调用的共享边界。否则页面上的“测试连接”会比真实节点调用更不稳定，最后形成一条没有超时治理、没有一致错误语义的旁路。

## 6. 禁止解释

以下实现不符合本方案：

- 在节点表单里直接放 Base URL 和 API Key。
- 只把模型输入框改成下拉框，但仍保存 `config.model`。
- 同时支持 `model` 和 `modelOfferingKey` 两条运行成功路径。
- 同时支持 `scoreRule.model` 和 `scoreRule.modelOfferingKey` 两条运行成功路径。
- 让 `ModelInvocationRequest` 同时携带 `modelOfferingKey` 和已解析供应项快照。
- 把 `ModelInvocationRequest` 或 `ResolvedModelRoute` 整体序列化进 Trace。
- 供应商不可用时自动回退到默认供应商。
- API Key 在详情接口中以掩码字段伪装成可回传值。
- 模型供应商列表写死在前端代码。
- 用超大 `pageSize` 拉全量模型供应项。
- 让供应商连接测试直接裸调上游直到底层网络自己超时。
