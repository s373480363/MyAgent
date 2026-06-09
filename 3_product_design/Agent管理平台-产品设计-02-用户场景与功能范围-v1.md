# Agent 管理平台 - 02 用户场景与功能范围 v1

## 1. 目标用户

| 用户 | 关注点 |
|---|---|
| 平台创建者 | 平台扩展性、执行稳定性、追踪记录、后端服务能力。 |
| Agent 设计者 | 通过画布创建 Agent，配置模型、节点、工具和 Agent 调用链路。 |
| Agent 调用者 | 通过页面或接口调用已发布 Agent，获得同步结果和失败原因。 |

## 2. 典型场景

| 场景 | 用户目标 | 平台能力 |
|---|---|---|
| 创建客服 Agent | 接收用户问题，分析意图，生成回复并总结结果。 | LLM、CONDITION、SUMMARY。 |
| 创建审核 Agent | 对文本或任务结果进行质量审核。 | REVIEW、CONDITION。 |
| 维护模型供应商 | 在页面维护 OpenAI-compatible 供应商、密钥状态和模型供应项。 | ModelProvider、ModelOffering。 |
| 调用业务 Java 方法 | 查询业务数据、执行规则校验、格式化对象。 | JAVA_METHOD。 |
| 调用外部 Agent | 调用 Codex、OpenCode、自研 CLI Agent 或 HTTP Agent。 | EXTERNAL_AGENT。 |
| 创建多 Agent 协作 | 主 Agent 调用分析 Agent、总结 Agent 或审核 Agent。 | AGENT_CALL。 |
| 维护节点验收集 | 为提示词准备输入、参考样例和自然语言验收规则。 | EvalSuite、EvalCase、EvalRun。 |
| 调试工作流 | 查看分支选择、节点输出、失败位置。 | AgentRun、NodeRun、TraceEvent。 |
| 对外提供 Agent 服务 | 将平台内已发布 Agent 暴露为同步 API。 | REST API、同步运行。 |

## 3. V1 功能范围

### 3.1 Agent 管理

- 创建 Agent。
- 编辑 Agent 名称、标识、描述、系统提示词、可选默认模型供应项、温度、运行限制。
- 启用或停用 Agent。
- 查看 Agent 当前草稿、当前发布版本和历史版本。
- 进入工作流设计、调试运行、运行记录页面。

### 3.2 模型供应商管理

- 创建、编辑、启停 OpenAI-compatible 模型供应商。
- 通过只写密钥表单维护供应商 API Key，普通列表和详情不回显明文。
- 手工维护模型供应项，并通过 `modelKey` 表达跨供应商模型身份。
- 支持同一模型身份被多个供应商提供。
- Agent 默认值和 LLM 类节点只能选择模型供应项，不能直接填写供应商 Base URL 或 API Key。

### 3.3 工作流画布

- 从节点库拖拽节点。
- 连接节点边。
- 配置节点参数。
- 配置边条件。
- 保存草稿。
- 校验工作流。
- 发布工作流版本。
- 查看或编辑 JSON 视图。

### 3.4 运行执行

- 页面同步运行 Agent。
- REST API 同步调用 Agent。
- 保存 AgentRun、NodeRun、TraceEvent。
- 返回最终结果、运行状态、运行编号、耗时和错误摘要。

### 3.5 多 Agent 协作

- 通过 AGENT_CALL 节点调用平台内部已发布 Agent。
- 保存父子运行关系。
- 在运行详情中查看调用链路。
- 限制最大 Agent 调用深度。

### 3.6 Java 方法调用

- 展示后端显式注册的 Java 方法。
- 工作流通过 JAVA_METHOD 节点选择方法。
- 配置输入映射和输出写回。
- 执行前后进行 Schema 校验。
- 记录方法调用 Trace。

### 3.7 外部 Agent 调用

- 支持 Codex CLI、OpenCode CLI、自定义 CLI、自定义 HTTP Agent。
- 通过 EXTERNAL_AGENT 节点配置适配器、提示词模板、输入映射和输出写回。
- 默认只做轻量调用和结果接入。
- 高级调试能力可选，例如 stdout、stderr、Git diff、产物采集。

### 3.8 Schema 管理

- 维护可复用 JSON Schema。
- 支持 Schema 版本。
- 支持节点输入输出引用 Schema。
- 支持 Java POJO 与 Schema 建立映射。
- 支持前端基于 Schema 渲染输入表单。

### 3.9 节点验收

- 为单个 LLM 类节点创建验收集。
- 用户手工维护验收用例。
- 支持从历史运行转为待确认用例。
- 支持可选 hardChecks 硬约束。
- 支持 judge LLM 按用户定义的自然语言规则输出结构化验收结论。
- 支持修改提示词后重跑验收集。

### 3.10 追踪调试

- 查看运行列表。
- 查看运行详情。
- 查看节点输入、输出、状态、耗时、错误。
- 查看 Trace 时间线。
- 从父运行跳转到子运行。

## 4. V1 不包含功能

- 用户登录和权限。
- 多租户。
- 复杂 RAG。
- 长期记忆。
- 异步任务队列。
- 人工审批。
- 完整工作流验收。
- 插件市场。
- 自动生成权威 benchmark。
- 自动优化提示词。
