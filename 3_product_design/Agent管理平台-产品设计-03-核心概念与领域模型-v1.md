# Agent 管理平台 - 03 核心概念与领域模型 v1

## 1. Agent 定义

Agent 是平台中的可配置执行单元。

```text
Agent = 工作流模板 + 节点能力 + 可选默认模型供应项配置 + 工具配置 + 运行约束 + 追踪记录
```

Agent 不只是提示词。它可以包含 LLM 调用、Java 方法调用、外部 Agent 调用、工具调用、条件判断、审核、总结和内部 Agent 调用。

## 2. 核心领域对象

| 对象 | 中文名称 | 定义 |
|---|---|---|
| `AgentDefinition` | Agent 定义 | Agent 基础信息、可选默认模型供应项配置、当前发布工作流版本、运行状态。 |
| `ModelProvider` | 模型供应商 | OpenAI-compatible 调用入口、供应商状态和密钥配置元信息。 |
| `ModelOffering` | 模型供应项 | 某供应商提供的某模型调用入口，是 Agent 和 LLM 节点引用的运行目标。 |
| `WorkflowDefinition` | 工作流定义 | Agent 内部节点、边、条件和运行约束。 |
| `WorkflowVersion` | 工作流版本 | 草稿、发布版本和历史版本。 |
| `NodeDefinition` | 节点定义 | 节点类型、配置、输入映射、输出映射、执行参数。 |
| `EdgeDefinition` | 边定义 | 节点之间的连接关系，包括普通边、条件边和结束边。 |
| `ToolDefinition` | 工具定义 | 可调用工具的名称、描述、参数 Schema、返回 Schema。 |
| `JavaMethodDefinition` | Java 方法定义 | 可被工作流调用的后端 Java 方法。 |
| `ExternalAgentDefinition` | 外部 Agent 定义 | Codex、OpenCode、自定义 CLI 或 HTTP Agent 适配器。 |
| `SchemaDefinition` | Schema 定义 | 可复用 JSON Schema 和版本信息。 |
| `AgentRun` | Agent 运行记录 | 一次 Agent 调用的输入、输出、状态、耗时和错误。 |
| `NodeRun` | 节点运行记录 | 单个节点执行的输入、输出、状态、耗时和错误。 |
| `TraceEvent` | 追踪事件 | 模型请求、工具调用、方法调用、外部 Agent 调用、分支决策等事件。 |
| `AgentMessage` | Agent 消息 | 内部 Agent 调用产生的结构化输入和输出。 |
| `EvalSuite` | 节点验收集 | 绑定到某个 LLM 类节点的验收集。 |
| `EvalCase` | 节点验收用例 | 用户定义或确认的输入、参考答案、断言规则。 |
| `EvalRun` | 节点验收运行 | 一次验收执行的结果、通过率、失败原因。 |

## 3. AgentDefinition

建议包含：

- id
- agentKey
- name
- description
- status
- systemPrompt
- defaultModelOfferingKey
- temperature
- maxSteps
- timeoutSeconds
- currentDraftWorkflowVersionId
- currentPublishedWorkflowVersionId
- createdAt
- updatedAt

## 4. WorkflowVersion

建议包含：

- id
- agentId
- version
- status
- nodes
- edges
- maxSteps
- timeoutSeconds
- referencedSchemaVersions
- publishedAt
- createdAt
- updatedAt

状态：

- `DRAFT`：草稿，可编辑，可调试。
- `PUBLISHED`：已发布，不可变，可正式调用。
- `HISTORY`：历史发布版本，只读。

## 5. NodeDefinition

建议包含：

- nodeId
- type
- name
- description
- inputSchemaRef
- outputSchemaRef
- inputMapping
- outputMapping
- config
- timeoutSeconds
- failurePolicy

## 6. EdgeDefinition

建议包含：

- edgeId
- sourceNodeId
- targetNodeId
- type
- condition
- isDefault
- description

边类型：

- 普通边
- 条件边
- 默认边
- 结束边

## 7. 运行对象关系

```text
AgentDefinition
  -> WorkflowVersion
      -> NodeDefinition
      -> EdgeDefinition

AgentRun
  -> NodeRun
  -> TraceEvent
  -> AgentMessage
```

## 8. 版本不可变原则

- 发布后的 WorkflowVersion 不允许原地修改。
- 发布后的 WorkflowVersion 引用的 Schema 版本必须只读。
- AgentRun 必须绑定具体 WorkflowVersion。
- NodeRun 和 TraceEvent 必须能追溯对应节点定义和 Schema 版本。

## 9. 双轨真相禁止原则

平台只允许一个主契约来源：

- 工作流结构以 WorkflowVersion 为准。
- 数据结构以 SchemaDefinition 为准。
- 模型调用入口以 ModelOffering 为准。
- Java 方法可调用范围以 JavaMethodDefinition 为准。
- 外部 Agent 可调用范围以 ExternalAgentDefinition 为准。
- 运行状态以 AgentRun、NodeRun、TraceEvent 为准。
