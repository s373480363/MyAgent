# 08-Trace 追踪与调试架构设计 v1

## 1. 设计目标

Trace 是平台核心能力。每次运行必须能回答：

- 执行了哪些节点。
- 每个节点输入是什么。
- 每个节点输出是什么。
- 哪个节点失败。
- 模型、工具、Java 方法、外部 Agent 调用了什么。
- 多 Agent 调用链路如何关联。

## 2. 核心对象

| 对象 | 说明 |
|------|------|
| AgentRun | 一次 Agent 运行记录 |
| NodeRun | 一次节点执行记录 |
| TraceEvent | 运行时间线事件 |
| AgentMessage | AGENT_CALL 父子运行和 Agent 间消息 |
| EvalRun | 节点验收运行结果 |

## 3. AgentRun

保存内容：

- runId / runNo。
- agentId。
- agentKey。
- workflowVersionId。
- runType：DEBUG、API、AGENT_CALL、EVAL。
- inputJson。
- outputJson。
- status。
- errorMessage。
- startedAt / finishedAt / durationMs。

## 4. NodeRun

保存内容：

- runId。
- nodeId。
- nodeName。
- nodeType。
- inputJson 全文。
- outputJson 全文。
- schemaValidationResultJson。
- status。
- errorMessage。
- durationMs。

## 5. TraceEvent 类型

| 类型 | 说明 |
|------|------|
| MODEL_REQUEST | OpenAI 请求前，保存模型、参数、系统提示词、用户提示词全文 |
| MODEL_RESPONSE | OpenAI 响应后，保存模型输出全文、耗时、token 信息 |
| SCHEMA_VALIDATION | Schema 校验结果，失败时包含字段路径和中文错误 |
| CONDITION_DECISION | CONDITION 命中分支和下一节点 |
| JAVA_METHOD_CALL | Java 方法标识、入参、出参、耗时、异常摘要 |
| TOOL_CALL | 工具标识、参数、返回值、耗时、错误摘要 |
| EXTERNAL_AGENT_CALL | 外部 Agent 适配器、调用方式、退出码或 HTTP 状态、结果摘要 |
| AGENT_CALL | 目标 Agent、子运行编号、调用深度、结果状态 |
| EVAL_CASE_RESULT | 验收用例输出、断言结果、评分结果、失败原因 |
| NODE_ERROR | 节点失败事件 |
| RUN_FINISHED | 整次运行完成事件 |

## 6. TraceEvent 存储结构

TraceEvent 不只保存一个 payload。架构字段为：

- runId，可空，用于普通 AgentRun 时间线。
- nodeRunId，可空，用于关联节点运行。
- evalRunId，可空，用于节点验收时间线。
- eventType。
- summary：短文本摘要，用于列表和时间线展示。
- detailJson：完整详情 JSON，用于详情页查看。
- eventTime。

`summary` 必须是独立字段，避免列表页解析大 JSONB。

## 7. 全文保存策略

V1 默认保存模型提示词和模型输出全文。该策略的影响：

- 有利于调试和复盘。
- 会增加 PostgreSQL 存储体积。
- 可能保存业务敏感内容。
- 成立前提是 V1 本机或内网单用户部署，不直接暴露公网。
- 预留配置项 `myagent.trace.persist-full-model-content=true`，V1 默认开启。
- 后续若进入多人共享或公网部署，必须重新设计脱敏、清理和访问控制。

## 8. 调试页面

- 左侧：节点执行序列。
- 中间：节点输入、输出、错误、耗时。
- 右侧：TraceEvent 时间线。
- AGENT_CALL：支持父子运行跳转。
- EvalRun：支持查看失败用例、断言和节点输出。

## 9. 节点验收 Trace 关系

节点验收使用 `EvalRun` 和 `EvalCaseResult` 保存正式验收结果。

如需要统一 Trace 时间线，创建 `AgentRun(runType=EVAL)` 承载本次验收运行的运行上下文，同时 `TraceEvent.evalRunId` 关联 EvalRun。单条用例结果仍以 EvalCaseResult 为准，`EVAL_CASE_RESULT` TraceEvent 只作为时间线展示事件。
