# Agent 管理平台 - 08 运行调用与 Trace 设计 v1

## 1. 运行模式

V1 只支持同步调用。

调用方发起请求后等待 Agent 执行完成，并获得最终结果。平台必须提供默认超时，避免同步请求无限等待。

## 2. 页面调试运行

用户可以在 Agent 详情页或工作流画布中发起调试运行。

规则：

- 可运行草稿版本。
- 可运行已发布版本。
- 页面必须明确显示运行的是草稿还是发布版本。
- 输入表单根据 START 节点 inputSchema 渲染。
- 调试运行也写入 AgentRun，但标记为调试运行。
- 当调试运行基于草稿发起时，系统必须先固化本次运行对应的不可变工作流定义，再写入 AgentRun；后续草稿编辑不能影响历史运行追溯。

## 3. API 同步调用

正式 API 调用只使用当前发布版本。

请求示例：

```http
POST /api/agents/{agentKey}/runs
Content-Type: application/json
```

```json
{
  "input": {
    "question": "请总结这段文本",
    "content": "..."
  }
}
```

响应示例：

```json
{
  "runId": "run_20260527_000001",
  "agentKey": "summary-agent",
  "status": "SUCCESS",
  "output": {
    "summary": "..."
  },
  "durationMs": 4260
}
```

## 4. 运行状态

| 状态 | 说明 |
|---|---|
| PENDING | 运行记录已创建，尚未开始执行。 |
| RUNNING | 工作流正在执行。 |
| SUCCESS | 成功到达 END 节点并产生最终输出。 |
| FAILED | 节点执行失败、模型失败、方法失败、工具失败或校验失败。 |
| TIMEOUT | 超过配置超时时间。 |
| CANCELED | 运行被取消，V1 可以先保留状态。 |

## 5. 执行链路

```text
用户调用 Agent
  -> 读取 AgentDefinition
  -> 读取 WorkflowVersion
  -> 创建 AgentRun
  -> 将 WorkflowDefinition 编译为 LangGraph4j 图
  -> LangGraph4j 执行节点和边
  -> 节点内部调用 OpenAI、Java 方法、外部 Agent、工具或内部 Agent
  -> 执行前后进行 Schema 校验
  -> 写入 NodeRun 和 TraceEvent
  -> 到达 END 节点
  -> 更新 AgentRun
  -> 返回 AgentRunResult
```

## 6. AgentRun

建议字段：

- id
- agentId
- agentKey
- workflowVersionId
- parentRunId
- runType
- status
- input
- output
- errorMessage
- startedAt
- finishedAt
- durationMs

`runType` 可选：

- DEBUG
- API
- AGENT_CALL
- EVAL

## 7. NodeRun

建议字段：

- id
- runId
- nodeId
- nodeName
- nodeType
- status
- input
- output
- errorMessage
- startedAt
- finishedAt
- durationMs
- schemaValidationResult

## 8. TraceEvent

TraceEvent 用于记录运行过程中的关键事件。

建议字段：

- id
- runId
- nodeRunId
- eventType
- eventTime
- summary
- detail

事件类型：

| 类型 | 说明 |
|---|---|
| MODEL_REQUEST | 即将调用 OpenAI。 |
| MODEL_RESPONSE | OpenAI 返回结果。 |
| TOOL_CALL | 调用工具。 |
| JAVA_METHOD_CALL | 调用 Java 方法。 |
| EXTERNAL_AGENT_CALL | 调用外部 Agent。 |
| SCHEMA_VALIDATION | 输入或输出 Schema 校验。 |
| CONDITION_DECISION | 条件节点选择分支。 |
| AGENT_CALL | 调用平台内部 Agent。 |
| EVAL_CASE_RESULT | 单条验收用例结果。 |
| NODE_ERROR | 节点失败。 |
| RUN_FINISHED | 整次运行结束。 |

## 9. Agent 间调用

AGENT_CALL 节点执行流程：

```text
Agent A 执行到 AGENT_CALL
  -> 平台读取目标 Agent B
  -> 确认 Agent B 已发布
  -> 创建子 AgentRun
  -> 同步执行 Agent B
  -> 将结果写回 Agent A 上下文
  -> Agent A 继续执行
```

约束：

- 目标 Agent 必须已发布。
- 不能直接递归调用自己。
- 最大调用深度默认 3。
- 父子运行关系必须可追踪。

## 10. 外部 Agent 调用

EXTERNAL_AGENT 节点执行流程：

```text
进入 EXTERNAL_AGENT 节点
  -> 提取输入
  -> 渲染提示词模板
  -> 选择适配器
  -> 调用外部 Agent
  -> 收集最终结果和错误摘要
  -> 可选执行 outputSchema 校验
  -> 写回上下文
  -> 写入 NodeRun 和 TraceEvent
```

V1 最小记录：

- 适配器
- 调用方式
- 运行状态
- 最终结果摘要
- 错误摘要
- 耗时

## 11. 调试页面

运行详情页面应展示：

- AgentRun 基础信息。
- 节点执行顺序。
- 每个节点的输入、输出、状态、耗时、错误。
- Trace 时间线。
- 父子 AgentRun 跳转。
- Schema 校验失败字段。
- 外部 Agent 错误摘要。
