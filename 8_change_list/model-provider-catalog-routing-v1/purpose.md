# 模型供应商目录与运行路由 v1 变更目的

## 1. 背景

当前系统的 LLM 调用能力只支持一个部署级 OpenAI-compatible 连接。`AGENT_STUDIO_OPENAI_BASE_URL`、`AGENT_STUDIO_OPENAI_API_KEY` 和 `AGENT_STUDIO_OPENAI_DEFAULT_MODEL` 由部署环境提供，页面只能在 Agent 或 LLM 节点上填写模型名、温度和提示词。

这个实现可以跑通单供应商 V1，但不符合平台型 Agent 产品的目标。用户预期是在页面中维护模型供应商和模型列表，并在 Agent 默认值或 LLM 类节点中选择具体供应项，而不是让供应商连接信息隐藏在部署配置中。

## 2. 目标

本次变更要把模型调用从“单部署配置”升级为“页面可维护的模型供应商目录 + 模型供应项路由”。

必须达成：

- 用户可以在页面创建、编辑、启停 OpenAI-compatible 模型供应商。
- 用户可以在每个供应商下维护模型供应项。
- 系统能表达“同一个模型身份被不同供应商提供”的场景。
- LLM、REVIEW、SUMMARY 节点不再手写模型名，而是选择一个已启用的模型供应项。
- Agent 默认模型不再是自由文本，而是可选的默认模型供应项引用。
- 运行时通过模型供应项解析供应商、Base URL、API Key 和上游模型名。
- 供应商 API Key 只写不回显，普通列表和详情接口不得返回明文。
- 现有 `AGENT_STUDIO_OPENAI_*` 部署变量迁移为默认供应商和默认模型供应项的初始化来源。

## 3. 非目标

- 本次不支持 Azure OpenAI、Anthropic 原生协议、Ollama 原生协议或其他非 OpenAI-compatible 协议。
- 本次不做用户级私有 API Key。
- 本次不做登录、角色、权限、多租户。
- 本次不从供应商接口自动同步模型列表，模型供应项由页面手工维护。
- 本次不把 Base URL 或 API Key 放进工作流节点配置。
- 本次不继续把 `agent.studio.openai.default-model` 作为长期正式模型配置入口。

## 4. 成功标准

- 页面有模型供应商管理入口，可维护供应商、供应商密钥状态和模型供应项。
- Agent 创建/编辑页可选择默认模型供应项，未使用 LLM 类节点的 Agent 不强制配置默认模型供应项。
- 工作流 LLM 类节点选择模型供应项，并继续配置温度和提示词。
- 保存后的节点配置使用 `modelOfferingKey`，不再把自由文本 `model` 作为新写入成功路径。
- 发布校验会校验 LLM 类节点最终解析到的 `modelOfferingKey` 存在、启用，并且所属供应商启用且已配置 API Key。
- 运行 Trace 能记录 `providerKey`、`modelOfferingKey`、`upstreamModelName` 和请求模型名，不记录 API Key。
- 迁移后旧环境变量只负责初始化默认供应商，不再作为运行时模型路由事实源。
