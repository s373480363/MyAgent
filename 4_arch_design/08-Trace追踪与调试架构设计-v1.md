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

- agentRunDbId / runId(runNo)。
- agentId。
- agentKey。
- workflowVersionId。
- runType：DEBUG、API、AGENT_CALL、EVAL。
- inputJson。
- outputJson。
- status。
- errorMessage。
- startedAt / finishedAt / durationMs。

规则：

- `workflowVersionId` 必须指向不可变 `workflow_version` 记录。
- `runType=DEBUG` 可以绑定草稿版本，但该草稿版本在运行时必须已经固化为一条不可原地覆盖的 `workflow_version` 行。
- `runType=API` 只能绑定当前发布版本。
- `runType=AGENT_CALL` 的子运行必须写 `parentRunId`，并绑定目标 Agent 当时的当前发布版本。
- `runType=EVAL` 是 v1 节点验收运行的强制配套运行记录。每次执行 `EvalRun` 时都必须同步创建一条 `AgentRun`，并绑定同一个 `workflowVersionId` 与 `nodeId`。

## 4. NodeRun

保存内容：

- agentRunDbId。
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
| CONDITION_DECISION | CONDITION 正常求值后的命中分支和下一节点；求值异常时记录失败事件 |
| JAVA_METHOD_CALL | Java 方法标识、入参、出参、耗时、异常摘要 |
| TOOL_CALL | 工具标识、参数、返回值、耗时、错误摘要 |
| EXTERNAL_AGENT_CALL | 外部 Agent 适配器、调用方式、退出码或 HTTP 状态、结果摘要 |
| AGENT_CALL | 目标 Agent、子运行编号、调用深度、结果状态 |
| EVAL_CASE_RESULT | 验收用例输出、断言结果、评分结果、失败原因 |
| NODE_ERROR | 节点失败事件 |
| RUN_FINISHED | 整次运行完成事件 |

## 6. TraceEvent 存储结构

TraceEvent 不只保存一个 payload。架构字段为：

- agentRunDbId，可空，对应数据库 `agent_run.id`，用于普通 AgentRun 时间线内部关联。
- nodeRunDbId，可空，对应数据库 `node_run.id`，用于节点运行内部关联。
- evalRunDbId，可空，对应数据库 `eval_run.id`，用于节点验收时间线内部关联。
- eventType。
- summary：短文本摘要，用于列表和时间线展示。
- detailJson：完整详情 JSON，用于详情页查看。
- eventTime。

`summary` 必须是独立字段，避免列表页解析大 JSONB。
- 对节点验收产生的 TraceEvent，v1 必须同时写入 `agentRunDbId` 和 `evalRunDbId`，禁止只写一侧关联导致 Trace 时间线出现第二套真相。

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
- 调试运行详情必须展示实际绑定的 `workflowVersionId`，并支持回看该版本的节点定义和 Schema 引用。
- 从 NodeRun 生成 EvalCase 时，页面应携带 `runId`、`nodeRunId`、`workflowVersionId` 和 `nodeId`，其中 `runId` 对应 `agent_run.run_no`，`nodeRunId` 对应 `node_run.id`，便于追溯用例来源；落库时再由后端解析并写入 `eval_case.source_agent_run_id=agent_run.id`。

## 9. 节点验收 Trace 关系

节点验收使用 `EvalRun` 和 `EvalCaseResult` 保存正式验收结果。

v1 每次节点验收运行都必须创建 `AgentRun(runType=EVAL)` 承载本次验收的运行上下文和 Trace 时间线，同时 `TraceEvent.evalRunDbId` 关联 `eval_run.id`。单条用例结果仍以 `EvalCaseResult` 为准，`EVAL_CASE_RESULT` TraceEvent 只作为时间线展示事件，不替代正式验收结果。
