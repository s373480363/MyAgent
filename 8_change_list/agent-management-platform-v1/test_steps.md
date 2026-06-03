# Agent 管理平台 V1 验收步骤

本文件记录本次变更的测试验收计划、测试用例、独立测试方案和独立测试用例。原 `8_change_list\agent-management-platform-v1\test_steps.md` 中的内容已合并到本文件，旧目录不再作为查询入口。

## 01-Agent管理平台V1测试验收计划-v1

# Agent 管理平台 V1 测试验收计划 v1

## 1. 文档目的

本文档定义 Agent 管理平台完整 V1 的测试验收计划，用于验收开发人员依据产品设计、架构设计、数据结构设计、接口设计、UI 设计和开发实施方案完成的交付内容。

本测试计划只验收平台框架能力是否正确落地，不验收某个具体业务 Agent 的业务效果。平台本身不包含固定业务逻辑，LLM 相关测试只验证调用流程、结构化输出、错误分类、Schema 校验、Trace 记录、节点验收机制和用户确认机制是否正确。

## 2. 编写依据

- `0_specifications\develop_specification.md`
- `0_specifications\ui_deisgn_specification.md`
- `3_product_design\Agent管理平台-产品设计-总纲-v1.md`
- `3_product_design\Agent管理平台-产品设计-01-产品定位与交付边界-v1.md`
- `3_product_design\Agent管理平台-产品设计-02-用户场景与功能范围-v1.md`
- `3_product_design\Agent管理平台-产品设计-03-核心概念与领域模型-v1.md`
- `3_product_design\Agent管理平台-产品设计-04-工作流画布与版本规则-v1.md`
- `3_product_design\Agent管理平台-产品设计-05-节点体系设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-06-Schema与POJO设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-07-节点验收设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-08-运行调用与Trace设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-09-信息架构与验收标准-v1.md`
- `4_arch_design\Agent管理平台-架构设计-总纲-v1.md`
- `4_arch_design\02-总体架构与模块边界-v1.md`
- `4_arch_design\05-工作流运行内核设计-v1.md`
- `4_arch_design\06-节点体系架构设计-v1.md`
- `4_arch_design\07-Schema与数据契约架构设计-v1.md`
- `4_arch_design\08-Trace追踪与调试架构设计-v1.md`
- `4_arch_design\09-Agent协作与外部Agent架构设计-v1.md`
- `4_arch_design\10-节点验收架构设计-v1.md`
- `4_arch_design\11-数据结构架构设计-v1.md`
- `4_arch_design\12-公共接口架构设计-v1.md`
- `4_arch_design\13-部署安全与质量架构设计-v1.md`
- `6_schema_design\01-数据结构设计总则-v1.md`
- `6_schema_design\04-核心业务表DDL-v1.md`
- `6_schema_design\05-运行追踪与验收表DDL-v1.md`
- `7_interface_design\01-接口设计总则-v1.md`
- `7_interface_design\02-对外REST接口-v1.md`
- `7_interface_design\03-内部应用服务接口-v1.md`
- `7_interface_design\04-运行时与注册目录接口-v1.md`
- `7_interface_design\05-响应错误分页与OpenAPI约定-v1.md`
- `8_change_list\agent-management-platform-v1\plan.md`
- `8_change_list\agent-management-platform-v1\design.md`

## 3. 验收前提

- 本次验收以前述文档定义的完整 V1 范围全部交付为前提，不按阶段性 Demo 口径判定 V1 通过。
- V1 部署边界为本机或内网单用户可信环境，不验收登录、权限、多租户和公网 SaaS 安全能力。
- V1 只支持同步运行，不验收异步任务队列、实时推送和人工审批。
- V1 节点验收只覆盖 LLM、REVIEW、SUMMARY 节点，不验收完整工作流验收能力。
- LLM 输出内容的业务质量由用户通过 EvalCase、确定性断言和可选 LLM 评分自行定义；测试人员只验收平台是否正确执行这些规则。

## 4. 验收结论口径

| 结论 | 判定标准 |
|------|----------|
| 通过 | 完整 V1 范围已实现；P0/P1 阻塞问题为 0；关键链路、异常链路、数据追溯、契约测试和质量闸门全部通过。 |
| 有条件通过 | 完整 V1 范围基本实现；不存在破坏主链路、数据一致性、版本追溯和运行排障的缺陷；仅存在不影响核心使用的 P2 问题，并已记录修复计划。 |
| 不通过 | 存在未交付的 V1 必做能力，或存在 P0/P1 缺陷，或任何质量闸门不满足。 |

缺陷优先级定义：

| 优先级 | 定义 |
|--------|------|
| P0 | 导致主链路不可用、数据不可追溯、版本规则破坏、运行状态错误、接口契约不可用或安全边界明显违背。 |
| P1 | 单个核心能力异常、异常信息不可定位、页面无法完成关键操作、Trace 不完整、验收结果不可信。 |
| P2 | 不影响主链路和数据正确性的交互、文案、展示、性能或易用性问题。 |

## 5. 测试范围

### 5.1 必测范围

- Agent 管理：创建、编辑、启停、详情、当前草稿、当前发布版本、历史版本入口。
- 工作流画布：节点库、节点配置、连线、边条件、保存草稿、校验、发布、JSON 视图、历史版本只读查看、从历史版本复制新草稿。
- Schema 管理：创建、草稿编辑、新版本创建、引用锁定、历史追溯、前端表单渲染。
- 注册目录：Java 方法、工具、外部 Agent 适配器列表、详情、启停和测试连接。
- 运行内核：同步运行、调试运行、WorkflowCompiler、MappingService、SchemaValidationService、RuntimeLimitGuard、TraceWriter。
- 节点执行器：START、END、LLM、CONDITION、JAVA_METHOD、TOOL、AGENT_CALL、EXTERNAL_AGENT、REVIEW、SUMMARY。
- 运行追踪：AgentRun、NodeRun、TraceEvent、AgentMessage、父子运行跳转、失败定位。
- 节点验收：EvalSuite、EvalCase、EvalRun、EvalCaseResult、从 NodeRun 生成用例、确认、归档、正式通过率、历史对比。
- REST API：所有 `/api` 接口、统一响应、分页、错误模型、运行类接口特殊响应规则。
- OpenAPI 契约：后端 OpenAPI 输出、前端 TypeScript 类型生成、禁止长期影子类型。
- 前端页面：信息架构定义的核心页面、中文文案、状态标签、错误定位、画布可用性。
- 数据库：Flyway 迁移、核心表、索引、唯一约束、状态约束、版本指针和运行追溯关系。
- 部署配置：OpenAI Key 不回传前端、默认模型、默认超时、最大步数、最大 Agent 调用深度、Trace 全文保存开关。

### 5.2 不测范围

- 登录、权限、角色、多租户。
- Azure OpenAI。
- 复杂 RAG、长期记忆、异步队列、人工审批。
- 完整工作流验收。
- 插件市场、Agent 模板市场。
- 自动优化提示词。
- 外部 Agent 深度托管和完整沙箱安全。
- 具体业务 Agent 的业务语义效果。

## 6. 测试策略

### 6.1 设计一致性测试

验证实现是否严格遵守设计，不允许出现双轨真相。

重点检查：

- 工作流结构唯一来源为 `workflow_version`。
- 数据契约唯一来源为 `schema_definition`。
- Java 方法可调用范围唯一来源为 `java_method_definition` 和注册目录。
- 工具可调用范围唯一来源为 `tool_definition` 和注册目录。
- 外部 Agent 可调用范围唯一来源为 `external_agent_definition` 和注册目录。
- 运行事实来源为 `agent_run`、`node_run`、`trace_event`。
- 前端类型来源为 OpenAPI，不手工维护长期影子 DTO。

### 6.2 接口契约测试

覆盖所有 REST 接口的成功、失败、参数错误、状态错误和分页场景。

重点检查：

- 所有业务接口使用 `ApiResponse<T>`。
- `success=true` 时 `data` 存在且 `error=null`。
- `success=false` 时 `data=null` 且 `error.code`、`error.message`、`error.details` 结构稳定。
- 所有用户可见错误为中文。
- 时间字段为带时区 ISO-8601 字符串。
- 分页字段统一为 `page`、`pageSize`、`total`、`items`。
- 运行记录创建成功后，即使节点失败、Schema 失败、模型失败或超时，也必须返回 `success=true`、`data.runId` 和 `data.status`。
- 只有请求非法、Agent 不存在、Agent 停用、无发布版本、无法创建运行记录等前置失败，才返回 `success=false`。

### 6.3 数据一致性测试

通过数据库断言验证版本、锁定、运行和验收关系。

重点检查：

- `agent_key` 唯一。
- 同一 Agent 下 `workflow_version.version_no` 唯一且递增。
- 同一 Agent 最多一个当前 `DRAFT`。
- 同一 Agent 最多一个当前 `PUBLISHED`。
- 保存草稿必须创建新的 `DRAFT` 行，旧草稿转为 `HISTORY`，不覆盖旧内容。
- 发布必须复制当前草稿创建新的 `PUBLISHED` 行，旧发布转为 `HISTORY`，不把草稿原地改成发布。
- 已发布工作流引用的 Schema 版本变为 `ACTIVE` 且 `locked=true`。
- `agent_run.run_no` 唯一，REST `runId` 对应 `agent_run.run_no`。
- `eval_run.run_no` 唯一，REST `evalRunId` 对应 `eval_run.run_no`。
- `nodeRunId` 在对外接口中对应 `node_run.id`，不再派生第二套编号。
- EvalCase 从 NodeRun 生成时保留来源运行、来源节点、来源版本和来源 nodeId。

### 6.4 运行内核测试

覆盖正式运行、调试运行、上下文映射、节点执行、状态传播和运行限制。

重点检查：

- 正式 API 只能运行当前发布版本。
- 调试运行只能运行已持久化的草稿版本或指定版本。
- 未保存画布不能直接运行，必须先保存生成新的 `workflowVersionId`。
- AgentRun 必须绑定实际执行的 `workflowVersionId`。
- START 将调用输入写入 `$.input`。
- END 根据输出映射生成最终输出并执行输出 Schema 校验。
- MappingService 只支持 `$`、点路径、数组下标。
- MappingService 读取路径不存在、数组越界、类型不匹配时失败。
- MappingService 写回禁止覆盖 `$.input`。
- MappingService 写回缺失中间对象时自动创建，不自动创建数组。
- Agent 运行超时、节点超时、最大步数、最大 Agent 调用深度均被强制执行。
- `FAILED`、`TIMEOUT`、`CANCELED` 语义不混用。

### 6.5 节点执行器测试

按节点类型验证配置校验、执行流程、错误分类、Trace 写入和输出写回。

| 节点 | 核心验收点 |
|------|------------|
| START | 校验输入 Schema，写入初始上下文，创建 NodeRun。 |
| END | 读取上下文，执行输出 Schema 校验，写入 AgentRun 最终输出。 |
| LLM | 普通文本模式、结构化输出模式、模型调用失败、结构化解析失败、Schema 校验失败三类错误区分。 |
| CONDITION | 声明式条件求值，命中显式分支，未命中走默认分支，字段缺失或类型错误按节点失败。 |
| JAVA_METHOD | 只能调用已注册且启用的方法，输入 Schema、JSON 转 POJO、方法调用、POJO 转 JSON、输出 Schema、Trace 全链路正确。 |
| TOOL | 只能调用已注册且启用的工具，工具输入输出 Schema 校验，失败摘要可定位。 |
| AGENT_CALL | 目标 Agent 必须已发布，禁止直接调用自己，校验调用深度，创建子 AgentRun，记录 AgentMessage，父子运行可跳转。 |
| EXTERNAL_AGENT | 适配器读取、提示词渲染、CLI/HTTP 调用、退出码或 HTTP 状态、结果解析、stdout/stderr/Git diff 采集开关、错误摘要。 |
| REVIEW | 按 LLM 类节点执行，输出审核结论、理由、评分或结构化输出，遵守 outputSchema 校验。 |
| SUMMARY | 按 LLM 类节点执行，支持文本总结或结构化摘要，遵守 outputSchema 校验。 |

### 6.6 LLM 相关测试口径

LLM 相关验收分为两层：

| 层级 | 是否作为 V1 通过条件 | 验收目标 |
|------|----------------------|----------|
| 平台流程正确性 | 是 | 验证模型配置解析、OpenAI 官方 API 调用封装、普通文本输出、结构化输出、Schema 校验、错误分类、Trace 全文保存、超时和节点验收流程。 |
| 业务语义效果 | 否 | 不由测试人员预设业务答案。若用户需要，可通过 EvalSuite 和 EvalCase 自行定义业务断言。 |

测试人员不得把“模型回答是否符合某个假想业务标准”作为 V1 框架通过条件。只有当用户已经定义或确认 EvalCase 时，测试人员才验证平台是否按这些用例和断言计算结果。

### 6.7 Trace 与调试测试

验证每次运行都能复盘输入、过程、输出和失败点。

重点检查：

- 每个执行节点都有 NodeRun。
- NodeRun 保存输入、输出、状态、错误、耗时和 Schema 校验结果。
- TraceEvent 至少覆盖 MODEL_REQUEST、MODEL_RESPONSE、SCHEMA_VALIDATION、CONDITION_DECISION、JAVA_METHOD_CALL、TOOL_CALL、EXTERNAL_AGENT_CALL、AGENT_CALL、EVAL_CASE_RESULT、NODE_ERROR、RUN_FINISHED 中相关事件。
- TraceEvent 的 `summary` 和 `detailJson` 分离保存。
- 模型请求和模型响应默认保存全文。
- 工具、Java 方法结构化输入输出保存全文。
- 外部 Agent stdout、stderr、Git diff 仅在采集开关开启时保存全文。
- 运行详情页能从失败运行定位到失败节点、失败事件、字段路径和绑定版本。
- 调试运行详情展示实际绑定的 `workflowVersionId`，并支持回看该版本定义。

### 6.8 节点验收测试

验证节点验收是平台机制，不是自动业务判定。

重点检查：

- EvalSuite 绑定 Agent、WorkflowVersion、nodeId。
- EvalSuite 状态流转为 `DRAFT -> CONFIRMED -> ARCHIVED`。
- 只有 `CONFIRMED` EvalSuite 可执行正式验收。
- EvalSuite 确认前必须至少存在一个可计入正式通过率的用例。
- EvalCase 支持 USER_CREATED、USER_CONFIRMED、AI_DRAFT_PENDING、ARCHIVED。
- AI_DRAFT_PENDING 不计入正式通过率。
- 从 NodeRun 生成 EvalCase 时复制 NodeRun 输入为用例输入，复制 NodeRun 输出为参考答案，并记录来源链路。
- 执行顺序固定为 `outputSchema 校验 -> 确定性断言 -> 可选 LLM 评分`。
- LLM 评分只做辅助，不覆盖正式断言结果。
- 关键用例失败必须影响整次验收结论。
- EvalRun 支持列表、详情、结果明细和历史对比。

### 6.9 前端验收测试

验证页面是否能完成设计定义的任务流。

重点检查：

- 核心页面存在：首页、Agent 列表、Agent 详情、工作流设计器、运行调试、运行列表、运行详情、工具管理、Java 方法管理、外部 Agent 管理、Schema 管理、节点验收、系统设置。
- 核心页面文案为中文。
- 状态标签对 DRAFT、PUBLISHED、HISTORY、SUCCESS、FAILED、TIMEOUT、CANCELED、CONFIRMED、ARCHIVED 等状态语义稳定。
- 工作流设计器具备左节点库、中画布、右配置面板、顶部保存/校验/发布/调试/JSON 视图。
- 常见桌面分辨率下节点、边、配置面板不互相遮挡。
- 运行详情能快速定位失败节点。
- 错误提示能说明用户需要修正什么。
- Schema 表单、调试输入表单和验收用例输入能基于 Schema 渲染。
- JSON 查看器支持查看长文本、结构化内容和 Trace 详情。

## 7. 主流程验收场景

### 7.1 Agent 与工作流主链路

1. 创建 Agent，确认初始化当前草稿版本。
2. 编辑 Agent 基础信息，确认 `agentKey` 不可修改。
3. 创建 START、LLM、END 节点并连线。
4. 创建并引用输入 Schema 和输出 Schema。
5. 保存草稿，确认生成新的 `DRAFT` 版本。
6. 校验草稿，确认合法工作流通过。
7. 发布草稿，确认生成新的 `PUBLISHED` 版本。
8. 调用正式 API，确认只运行当前发布版本。
9. 查看运行详情，确认 AgentRun、NodeRun、TraceEvent 完整。

### 7.2 完整节点覆盖链路

构造至少一组覆盖全部 V1 节点类型的测试工作流或多条工作流：

- START -> CONDITION -> LLM -> END。
- START -> JAVA_METHOD -> END。
- START -> TOOL -> END。
- START -> AGENT_CALL -> END。
- START -> EXTERNAL_AGENT -> END。
- START -> REVIEW -> SUMMARY -> END。

每条链路均需验证：

- 节点配置保存。
- 发布校验通过或按预期失败。
- 运行后 NodeRun 顺序正确。
- 节点输入输出写入上下文。
- 运行状态和 TraceEvent 正确。

### 7.3 历史版本与复盘链路

1. 保存多个草稿版本。
2. 发布多个发布版本。
3. 从历史版本复制生成新草稿。
4. 调试旧草稿版本。
5. 修改当前草稿后确认旧运行仍可回看旧版本定义。
6. 正式运行后确认绑定当时当前发布版本。
7. 查看运行详情并跳转对应 WorkflowVersion。

### 7.4 节点验收链路

1. 选择一个 LLM、REVIEW 或 SUMMARY 节点创建 EvalSuite。
2. 手工创建 USER_CREATED EvalCase。
3. 从历史 NodeRun 生成 AI_DRAFT_PENDING EvalCase。
4. 验证 AI_DRAFT_PENDING 不计入正式通过率。
5. 确认用例后运行 EvalSuite。
6. 验证 outputSchema、断言、可选 LLM 评分执行顺序。
7. 查看 EvalRun 详情、结果明细和历史对比。

## 8. 异常流程验收场景

| 场景 | 预期 |
|------|------|
| 工作流缺少 START | 发布失败，返回节点级中文错误。 |
| 工作流存在多个 START | 发布失败，返回节点级中文错误。 |
| 工作流缺少 END | 发布失败，返回节点级中文错误。 |
| 非 START 可执行节点无入边 | 发布失败。 |
| 非 END 可执行节点无出边 | 发布失败。 |
| CONDITION 无默认分支 | 发布失败。 |
| CONDITION 条件求值字段缺失 | 运行失败，节点 FAILED，写 NODE_ERROR。 |
| LLM 缺少提示词且无法继承 Agent 默认提示词 | 发布失败。 |
| LLM 模型调用失败 | 运行 FAILED，返回 runId，写 MODEL_REQUEST、NODE_ERROR、RUN_FINISHED。 |
| LLM 结构化响应解析失败 | 运行 FAILED，错误分类不能混为 Schema 校验失败。 |
| LLM outputSchema 校验失败 | 运行 FAILED，写 SCHEMA_VALIDATION，错误包含字段路径。 |
| JAVA_METHOD 引用未启用方法 | 发布失败。 |
| JAVA_METHOD JSON 转 POJO 失败 | 节点执行失败，归类为 JAVA_METHOD 执行失败。 |
| JAVA_METHOD 输出不符合 Schema | 运行 FAILED，归类为 Schema 校验失败。 |
| TOOL 引用未启用工具 | 发布失败。 |
| EXTERNAL_AGENT 未选择适配器 | 发布失败。 |
| EXTERNAL_AGENT CLI 返回非零退出码 | 运行 FAILED，记录适配器、退出码、错误摘要。 |
| EXTERNAL_AGENT HTTP 返回失败状态 | 运行 FAILED，记录 HTTP 状态和错误摘要。 |
| AGENT_CALL 目标 Agent 未发布 | 发布失败或运行失败原因明确。 |
| AGENT_CALL 直接调用当前 Agent | 发布失败。 |
| AGENT_CALL 超过最大调用深度 | 运行失败或发布校验失败，错误语义明确。 |
| 运行超过最大步数 | 运行 TIMEOUT 或 FAILED，按实现口径记录，不能静默循环。 |
| 节点超时 | 节点和运行状态符合 TIMEOUT 语义。 |
| 父运行超时 | 活跃子运行尽力标记 CANCELED。 |
| 已锁定 Schema 被更新 | 更新失败，返回中文错误。 |
| 已归档 EvalSuite 运行 | 运行请求失败。 |
| AI_DRAFT_PENDING 用例参与正式通过率 | 不允许，若发生判为 P1。 |

## 9. 质量闸门

出现以下任一情况，V1 验收结论为不通过：

- 完整 V1 必做范围未交付。
- 版本不可变规则不成立。
- 保存草稿、复制草稿、发布版本未在同一事务内完成状态和指针更新。
- 正式运行可以绕过当前发布版本。
- 调试运行可以执行未保存的前端内存画布。
- 运行失败无法返回 `runId`。
- `runId`、`evalRunId`、`nodeRunId` 语义不一致或出现第二套运行态标识。
- Trace 无法定位节点输入、节点输出、失败节点或失败事件。
- Schema 校验无法返回字段路径。
- 发布后的工作流无法复盘历史执行。
- 前端不能通过 OpenAPI 生成稳定类型。
- 用户可见错误不是中文。
- AI_DRAFT_PENDING 验收用例被计入正式通过率。
- OpenAI Key 或敏感 HTTP Header 被普通接口明文返回。
- 存在双轨 Schema、双轨工作流定义、双轨运行记录或双轨接口类型。

## 10. 测试数据与测试替身

由于平台是框架产品，测试数据应以最小可控样例覆盖流程，不应构造复杂业务语义。

建议准备：

- 基础 Agent：用于 START、LLM、END 主链路。
- 条件 Agent：用于 CONDITION 分支。
- Java 方法测试 Bean：返回固定结构化 JSON，用于 JAVA_METHOD。
- 工具测试实现：返回固定结构化 JSON，用于 TOOL。
- 子 Agent：已发布，用于 AGENT_CALL。
- 外部 Agent 测试适配器：CLI 可用本地脚本返回固定 JSON，HTTP 可用本地测试服务返回固定 JSON。
- LLM 网关测试替身：用于稳定验证普通文本、结构化 JSON、解析失败、调用失败和超时。
- OpenAI 真实连接冒烟用例：仅验证配置和连通性，不作为业务效果判断。
- EvalSuite 样例：只使用确定性断言，例如 JSONPath 存在、字段等于、包含、不包含、枚举、数值范围。

## 11. 测试执行顺序

1. 规范和契约检查：确认文档范围、接口清单、OpenAPI 输出和数据库迁移完整。
2. 后端单元测试：领域规则、版本规则、Schema 规则、MappingService、运行状态机。
3. 后端集成测试：PostgreSQL、Flyway、MyBatis、REST API、运行记录、Trace 写入。
4. 运行内核测试：WorkflowValidator、WorkflowCompiler、RuntimeLimitGuard、NodeExecutor。
5. 节点执行测试：按 START、END、CONDITION、JAVA_METHOD、LLM、TOOL、AGENT_CALL、EXTERNAL_AGENT、REVIEW、SUMMARY 覆盖。
6. 前端测试：页面渲染、表单校验、画布保存发布、调试运行、运行详情、验收页面。
7. 端到端测试：主链路、异常链路、历史版本、运行复盘、节点验收。
8. 部署验收：配置项、OpenAI Key 不回显、默认限制、内网单用户部署说明。
9. 回归测试：修复缺陷后按影响范围回归，并复测质量闸门。

## 12. 交付物要求

开发交付 V1 验收时，应提供以下材料：

- 可运行的后端服务、前端服务和 PostgreSQL 迁移脚本。
- OpenAPI 契约文件或可访问的 OpenAPI 输出地址。
- 前端基于 OpenAPI 生成类型的命令或脚本。
- 自动化测试执行命令和结果。
- 必要的本地测试替身或测试配置。
- 部署说明和配置说明。
- 已知问题清单。
- 测试结果记录，写入 `8_change_list\agent-management-platform-v1\test_result\`。

## 13. 需澄清或准备的事项

当前设计文档中 Q01 至 Q08 均已确认，没有阻塞测试计划编写的产品问题。

正式执行验收前仍需准备以下测试资源：

- 用户将提供真实 OpenAI API Key 用于连通性冒烟；测试执行前需要提醒用户配置该 Key，且 Key 不得写入测试报告正文、日志截图或前端可见响应。
- 当前本机作为 Codex CLI 和 OpenCode CLI 的可执行环境；正式执行前需要由测试人员确认命令可调用、版本可识别、工作目录可访问。
- 需要开发提供至少一个显式注册的 Java 方法测试 Bean 和一个工具测试实现，用于 JAVA_METHOD 与 TOOL 节点的确定性流程验收。
- 已在 `8_change_list\agent-management-platform-v1\test_result\` 中补充正式测试报告模板，执行验收后应按模板记录结果。

这些事项不改变 V1 验收口径，只影响具体执行环境和测试数据准备方式。

## 14. 测试结论模板

```text
验收对象：Agent 管理平台 V1
验收范围：完整 V1
验收结论：通过 / 有条件通过 / 不通过

质量闸门：
- 版本不可变规则：
- Run/Trace 可追溯：
- Schema 字段路径错误：
- OpenAPI 类型来源：
- 节点验收确认机制：
- 敏感配置不回显：

缺陷统计：
- P0：
- P1：
- P2：

剩余风险：
- 

结论说明：
- 
```


## 02-Agent管理平台V1测试用例清单-v1

# Agent 管理平台 V1 测试用例清单 v1

## 1. 文档目的

本文档将 `8_change_list\agent-management-platform-v1\test_steps.md` 拆解为可执行测试用例。测试人员执行验收时，应按本文档记录用例结果，并在 `8_change_list\agent-management-platform-v1\test_result` 的验收报告中引用用例编号。

本文档只验证平台框架能力，不验证具体业务 Agent 的业务语义效果。

## 2. 用例字段说明

| 字段 | 说明 |
|------|------|
| 用例编号 | 稳定编号，用于测试报告、缺陷和回归追踪。 |
| 优先级 | P0、P1、P2，与测试计划中的缺陷优先级口径一致。 |
| 前置条件 | 执行该用例前必须具备的环境、数据或配置。 |
| 操作步骤 | 测试人员需要执行的关键动作。 |
| 预期结果 | 判定通过的标准。 |
| 证据 | 建议保留的截图、接口响应、数据库记录、日志或 Trace。 |

## 3. 执行前资源确认

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| ENV-001 | P0 | PostgreSQL 可用，后端迁移脚本已提供。 | 执行数据库迁移。 | 所有核心表创建成功，关键唯一约束、状态约束和索引存在。 | Flyway 结果、数据库表结构截图或 SQL 查询结果。 |
| ENV-002 | P0 | 后端服务包或源码已提供。 | 启动后端服务。 | 后端启动成功，无启动期异常；配置项被正确加载。 | 启动日志、健康检查结果。 |
| ENV-003 | P0 | 前端服务包或源码已提供。 | 启动前端服务并访问首页。 | 前端可访问，页面无白屏和明显运行时错误。 | 页面截图、浏览器控制台截图。 |
| ENV-004 | P0 | 用户提供真实 OpenAI API Key。 | 配置 OpenAI API Key 后执行一次模型连通性冒烟。 | 连接成功；测试报告只记录是否已配置，不记录 Key 明文。 | 冒烟结果、脱敏日志。 |
| ENV-005 | P1 | 本机安装 Codex CLI。 | 执行 Codex CLI 版本或帮助命令。 | 命令可执行，版本或帮助信息可识别。 | 命令输出。 |
| ENV-006 | P1 | 本机安装 OpenCode CLI。 | 执行 OpenCode CLI 版本或帮助命令。 | 命令可执行，版本或帮助信息可识别。 | 命令输出。 |
| ENV-007 | P0 | 开发已提供测试 Java 方法 Bean。 | 查看 Java 方法目录。 | 至少存在一个启用的测试 Java 方法，含输入 Schema 和输出 Schema。 | `/api/java-methods` 响应、页面截图。 |
| ENV-008 | P0 | 开发已提供测试 Tool 实现。 | 查看工具目录。 | 至少存在一个启用的测试 Tool，含输入 Schema 和输出 Schema。 | `/api/tools` 响应、页面截图。 |
| ENV-009 | P1 | 后端已输出 OpenAPI。 | 获取 OpenAPI 文件或访问 OpenAPI 地址。 | OpenAPI 可访问，包含请求、响应、错误、分页、枚举和运行结果对象。 | OpenAPI 文件或页面截图。 |
| ENV-010 | P1 | 前端生成类型脚本已提供。 | 执行前端类型生成命令。 | 类型生成成功，前端无手写长期影子 DTO 作为主要契约来源。 | 命令输出、生成文件路径。 |

## 4. Agent 管理用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| AGT-001 | P0 | 后端和前端可用。 | 创建一个新 Agent，填写 agentKey、名称、描述、默认模型和运行限制。 | Agent 创建成功，返回 agentId，agentKey 唯一。 | 接口响应、Agent 列表截图。 |
| AGT-002 | P0 | 已创建 Agent。 | 查询 Agent 详情。 | 详情包含基础信息、当前草稿版本摘要、当前发布版本摘要和历史版本入口摘要。 | `/api/agents/{agentId}` 响应。 |
| AGT-003 | P0 | 已创建 Agent。 | 检查数据库 `agent_definition.current_draft_workflow_version_id`。 | 创建 Agent 后初始化一个可编辑草稿版本。 | 数据库查询结果。 |
| AGT-004 | P1 | 已创建 Agent。 | 编辑名称、描述、系统提示词、默认模型、温度、超时和最大步数。 | 可编辑字段更新成功。 | 接口响应、详情页截图。 |
| AGT-005 | P0 | 已创建 Agent。 | 尝试通过更新接口修改 agentKey。 | 修改失败或字段被忽略；agentKey 保持不变。 | 接口响应、数据库查询结果。 |
| AGT-006 | P0 | 已创建并启用 Agent。 | 停用 Agent 后发起正式 API 调用。 | 前置失败，返回 `success=false`，错误为中文，允许没有 runId。 | 接口响应。 |
| AGT-007 | P1 | 已停用 Agent。 | 启用 Agent。 | Agent 状态变为 ENABLED，历史运行记录不被删除。 | 页面截图、运行列表查询结果。 |
| AGT-008 | P1 | 存在多个 Agent。 | 按状态和关键字筛选 Agent 列表。 | 列表分页、筛选字段和总数正确。 | `/api/agents` 响应。 |

## 5. Schema 管理用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| SCH-001 | P0 | 后端服务可用。 | 创建一个新的输入 Schema。 | 创建成功，schemaKey 唯一，version 从 1 开始。 | `/api/schemas` 响应、数据库记录。 |
| SCH-002 | P0 | 存在 `status=DRAFT` 且 `locked=false` 的 Schema。 | 更新 Schema 名称、描述、jsonSchema 和 javaType。 | 更新成功，不允许修改 schemaKey 和 version。 | 接口响应、数据库记录。 |
| SCH-003 | P0 | 存在一个 Schema。 | 基于旧 Schema 创建新版本。 | 新版本使用同一 schemaKey，下一个整数 version，旧版本内容不变。 | 接口响应、数据库记录。 |
| SCH-004 | P0 | 工作流已发布并引用某 Schema。 | 查看被引用 Schema。 | 被引用 Schema 为 `ACTIVE` 且 `locked=true`。 | 数据库记录、Schema 详情。 |
| SCH-005 | P0 | 存在 `locked=true` 的 Schema。 | 尝试更新该 Schema。 | 更新失败，返回中文错误。 | 接口响应。 |
| SCH-006 | P1 | 存在多个 Schema。 | 按状态、来源和关键字查询 Schema 列表。 | 查询结果和分页信息正确。 | `/api/schemas` 响应。 |
| SCH-007 | P1 | Schema 详情可访问。 | 查看 Schema 详情。 | 详情包含基础信息、版本号、JSON Schema、引用状态和关联工作流摘要。 | 页面截图、接口响应。 |
| SCH-008 | P0 | 调试页面可访问，START 节点引用输入 Schema。 | 打开调试页面。 | 输入表单基于 START 节点 inputSchema 渲染。 | 页面截图。 |

## 6. 工作流画布与版本用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| WFV-001 | P0 | 已创建 Agent。 | 打开工作流设计器。 | 页面包含左侧节点库、中间画布、右侧配置面板、顶部操作栏。 | 页面截图。 |
| WFV-002 | P0 | 工作流设计器可用。 | 拖拽 START、LLM、END 节点并连线。 | 节点和边可创建，布局和配置可保存。 | 页面截图、保存响应。 |
| WFV-003 | P0 | 已配置合法 START->LLM->END。 | 保存草稿。 | 创建新的不可变 DRAFT 版本，当前草稿指针更新。 | 接口响应、数据库记录。 |
| WFV-004 | P0 | 已存在当前 DRAFT。 | 再次保存草稿。 | 新建 DRAFT；旧 DRAFT 在同一事务内转为 HISTORY，内容不被覆盖。 | 数据库记录。 |
| WFV-005 | P0 | 已保存合法草稿。 | 校验草稿。 | 返回 valid=true 或等价成功结果。 | 校验接口响应。 |
| WFV-006 | P0 | 已保存合法草稿。 | 发布草稿。 | 创建新的 PUBLISHED 版本，当前发布指针更新；当前草稿指针保持设计约定。 | 发布响应、数据库记录。 |
| WFV-007 | P0 | 已存在当前 PUBLISHED。 | 再次发布新草稿。 | 旧 PUBLISHED 转为 HISTORY，新 PUBLISHED 成为当前发布版本。 | 数据库记录。 |
| WFV-008 | P0 | 已存在历史版本。 | 从历史版本复制生成新草稿。 | 新建 DRAFT，写入 sourceWorkflowVersionId，源版本不变。 | 接口响应、数据库记录。 |
| WFV-009 | P0 | 已存在历史版本。 | 尝试原地编辑历史版本。 | 历史版本只读，不允许原地修改。 | 页面行为、接口响应。 |
| WFV-010 | P1 | 已存在多个版本。 | 查询工作流版本列表。 | 返回 DRAFT、PUBLISHED、HISTORY 摘要和分页信息。 | `/workflow-versions` 响应。 |
| WFV-011 | P1 | 已存在工作流版本。 | 查询指定版本详情。 | 返回 nodes、edges、runtimeOptions、referencedSchemaVersions。 | 版本详情响应。 |
| WFV-012 | P0 | 画布存在未保存修改。 | 直接发起调试运行。 | 系统要求先保存草稿，不能执行前端内存中的未保存画布。 | 页面提示或接口响应。 |
| WFV-013 | P1 | 工作流设计器可用。 | 打开 JSON 视图。 | JSON 视图展示当前工作流定义，内容与节点、边配置一致。 | 页面截图。 |

## 7. 工作流发布校验用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| VAL-001 | P0 | 草稿无 START 节点。 | 执行发布或发布前校验。 | 校验失败，返回中文错误和节点级或字段级定位。 | 接口响应。 |
| VAL-002 | P0 | 草稿存在多个 START 节点。 | 执行发布或发布前校验。 | 校验失败，提示只能存在一个 START。 | 接口响应。 |
| VAL-003 | P0 | 草稿无 END 节点。 | 执行发布或发布前校验。 | 校验失败，提示至少需要一个 END。 | 接口响应。 |
| VAL-004 | P0 | 非 START 节点无入边。 | 执行发布或发布前校验。 | 校验失败，指出对应节点。 | 接口响应。 |
| VAL-005 | P0 | 非 END 节点无出边。 | 执行发布或发布前校验。 | 校验失败，指出对应节点。 | 接口响应。 |
| VAL-006 | P0 | CONDITION 无默认分支。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |
| VAL-007 | P0 | LLM 无提示词且不能继承 Agent 默认提示词。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |
| VAL-008 | P0 | JAVA_METHOD 引用禁用方法。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |
| VAL-009 | P0 | TOOL 引用禁用工具。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |
| VAL-010 | P0 | EXTERNAL_AGENT 未选择适配器。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |
| VAL-011 | P0 | AGENT_CALL 目标 Agent 未发布。 | 执行发布或发布前校验。 | 校验失败或运行前明确失败，不能静默通过。 | 接口响应。 |
| VAL-012 | P0 | AGENT_CALL 目标为当前 Agent。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |
| VAL-013 | P0 | 节点引用不存在的 Schema。 | 执行发布或发布前校验。 | 校验失败并指明引用问题。 | 接口响应。 |
| VAL-014 | P1 | 节点 timeoutSeconds 小于等于 0。 | 执行发布或发布前校验。 | 校验失败。 | 接口响应。 |

## 8. REST API 与 OpenAPI 契约用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| API-001 | P0 | 任一成功业务接口可调用。 | 调用成功接口。 | 响应包含 `success=true`、`data`、`error=null`。 | 接口响应。 |
| API-002 | P0 | 可构造业务失败。 | 调用失败接口。 | 响应包含 `success=false`、`data=null`、中文 `error.message` 和稳定 `error.code`。 | 接口响应。 |
| API-003 | P1 | 可构造字段校验失败。 | 提交缺失必填字段的请求。 | `error.details` 包含字段路径、原因和中文提示。 | 接口响应。 |
| API-004 | P0 | 已发布 Agent 可运行。 | 调用正式运行接口并让节点业务失败。 | 运行记录创建后仍返回 `success=true`、`data.runId`、`data.status=FAILED`。 | 接口响应、数据库记录。 |
| API-005 | P0 | Agent 无发布版本。 | 调用正式运行接口。 | 前置失败，返回 `success=false`，可以没有 runId。 | 接口响应。 |
| API-006 | P1 | 存在列表接口数据。 | 调用带 page、pageSize 的列表接口。 | 返回 items、page、pageSize、total，分页语义正确。 | 接口响应。 |
| API-007 | P1 | OpenAPI 输出可访问。 | 检查 operationId。 | 关键接口 operationId 稳定且存在。 | OpenAPI 文档。 |
| API-008 | P1 | OpenAPI 输出可访问。 | 检查错误体、分页体、枚举和嵌套对象。 | 全部纳入 OpenAPI。 | OpenAPI 文档。 |
| API-009 | P0 | 前端源码可检查或类型生成可执行。 | 检查前端 API 类型来源。 | 前端类型由 OpenAPI 生成，未长期维护影子 DTO。 | 生成命令输出、代码检查记录。 |
| API-010 | P1 | 有时间字段响应。 | 检查时间字段格式。 | 时间为带时区 ISO-8601 字符串。 | 接口响应。 |

## 9. 运行内核与映射用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| RUN-001 | P0 | Agent 有当前发布版本。 | 调用正式运行接口。 | 运行绑定 `currentPublishedWorkflowVersionId`。 | 接口响应、数据库记录。 |
| RUN-002 | P0 | Agent 有当前草稿版本。 | 调用调试运行接口且不指定 workflowVersionId。 | 运行绑定当前草稿指针指向的已持久化版本。 | 接口响应、数据库记录。 |
| RUN-003 | P0 | Agent 有多个历史版本。 | 指定某个同 Agent 版本发起调试运行。 | 运行绑定指定 workflowVersionId。 | 接口响应、数据库记录。 |
| RUN-004 | P0 | 配置 START inputSchema。 | 使用合法输入运行。 | START 校验通过，`$.input` 写入工作流上下文。 | NodeRun 输入、Trace。 |
| RUN-005 | P0 | 配置 START inputSchema。 | 使用缺少必填字段的输入运行。 | Schema 校验失败，返回 runId，错误包含字段路径。 | 接口响应、Trace。 |
| RUN-006 | P0 | 节点 inputMapping 使用点路径。 | 运行工作流。 | 映射成功，节点输入符合预期。 | NodeRun inputJson。 |
| RUN-007 | P0 | 节点 inputMapping 使用数组下标。 | 运行工作流。 | 数组下标映射成功。 | NodeRun inputJson。 |
| RUN-008 | P0 | 节点 inputMapping 指向不存在路径。 | 运行工作流。 | 节点失败，错误为中文，写 NODE_ERROR。 | 接口响应、Trace。 |
| RUN-009 | P0 | outputMapping 试图覆盖 `$.input`。 | 发布或运行工作流。 | 被拒绝或运行失败，不能覆盖 `$.input`。 | 校验响应或运行响应。 |
| RUN-010 | P0 | outputMapping 写入缺失中间对象。 | 运行工作流。 | 缺失中间对象自动创建，输出写回成功。 | 运行详情。 |
| RUN-011 | P0 | outputMapping 需要自动创建数组。 | 运行工作流。 | 写回失败，不自动创建数组。 | 运行响应、Trace。 |
| RUN-012 | P0 | 配置最大步数较小并构造循环。 | 运行工作流。 | 达到限制后终止，状态和错误语义明确。 | 运行响应、Trace。 |
| RUN-013 | P0 | 配置节点超时。 | 触发节点超时。 | 节点和运行状态符合 TIMEOUT 语义，不混为普通 FAILED。 | 运行响应、NodeRun。 |
| RUN-014 | P0 | 配置运行超时。 | 触发运行超时。 | AgentRun 状态为 TIMEOUT，写 RUN_FINISHED。 | 数据库记录、Trace。 |

## 10. 节点执行器用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| NOD-001 | P0 | START->END 工作流可发布。 | 执行最小工作流。 | START 和 END 均产生 NodeRun，运行 SUCCESS。 | 运行详情。 |
| NOD-002 | P0 | LLM 节点未配置 outputSchema。 | 执行 LLM 普通文本模式。 | 节点成功，输出按普通文本模式写回。 | NodeRun、Trace。 |
| NOD-003 | P0 | LLM 节点配置 outputSchema。 | 执行 LLM 结构化输出模式。 | 输出通过 Schema 校验后写回。 | NodeRun、SCHEMA_VALIDATION。 |
| NOD-004 | P0 | LLM 网关测试替身可模拟调用失败。 | 触发模型调用失败。 | 错误归类为模型调用失败，运行 FAILED，返回 runId。 | 运行响应、Trace。 |
| NOD-005 | P0 | LLM 网关测试替身可返回非结构化内容。 | 触发结构化解析失败。 | 错误归类为结构化响应解析失败，不混为 Schema 校验失败。 | 运行响应、Trace。 |
| NOD-006 | P0 | LLM 网关测试替身返回结构化但不符合 Schema 的内容。 | 运行 LLM 节点。 | 错误归类为 Schema 校验失败，字段路径明确。 | SCHEMA_VALIDATION。 |
| NOD-007 | P0 | CONDITION 有显式分支和默认分支。 | 输入命中显式分支。 | 选择显式分支，写 CONDITION_DECISION。 | Trace、NodeRun。 |
| NOD-008 | P0 | CONDITION 有默认分支。 | 输入不命中显式分支但可正常求值。 | 走默认分支，状态 SUCCESS。 | Trace、NodeRun。 |
| NOD-009 | P0 | CONDITION 依赖字段缺失。 | 运行条件节点。 | 节点 FAILED，不走默认分支掩盖错误。 | 运行响应、Trace。 |
| NOD-010 | P0 | 测试 Java 方法已注册且启用。 | 执行 JAVA_METHOD 节点。 | 输入校验、POJO 转换、方法调用、输出校验、写回均成功。 | NodeRun、JAVA_METHOD_CALL。 |
| NOD-011 | P0 | 测试 Java 方法可模拟转换失败。 | 触发 JSON 转 POJO 或 POJO 转 JSON 失败。 | 归类为 JAVA_METHOD 执行失败。 | 运行响应、Trace。 |
| NOD-012 | P0 | 测试 Tool 已注册且启用。 | 执行 TOOL 节点。 | 工具调用成功，输入输出 Schema 校验通过。 | NodeRun、TOOL_CALL。 |
| NOD-013 | P0 | 子 Agent 已发布。 | 执行 AGENT_CALL 节点。 | 创建子 AgentRun，写 parentRunId 和 AgentMessage，父子可跳转。 | 运行详情、数据库记录。 |
| NOD-014 | P0 | 构造 AGENT_CALL 深度超过限制。 | 执行工作流。 | 运行失败或超时语义明确，不能无限递归。 | 运行响应、Trace。 |
| NOD-015 | P0 | Custom CLI 外部 Agent 测试适配器可用。 | 执行 EXTERNAL_AGENT CLI 节点。 | 调用成功，输出按 resultSource 解析并写回。 | NodeRun、EXTERNAL_AGENT_CALL。 |
| NOD-016 | P0 | Custom HTTP 外部 Agent 测试适配器可用。 | 执行 EXTERNAL_AGENT HTTP 节点。 | 调用成功，记录 HTTP 状态和结果摘要。 | NodeRun、Trace。 |
| NOD-017 | P0 | 外部 Agent 可模拟失败。 | 触发 CLI 非零退出码或 HTTP 失败状态。 | 运行 FAILED，记录适配器、退出码或 HTTP 状态、错误摘要。 | Trace。 |
| NOD-018 | P1 | REVIEW 节点可运行。 | 执行 REVIEW 节点。 | 按 LLM 类节点处理，输出结论、原因、评分或结构化结果。 | NodeRun、Trace。 |
| NOD-019 | P1 | SUMMARY 节点可运行。 | 执行 SUMMARY 节点。 | 按 LLM 类节点处理，输出文本或结构化摘要。 | NodeRun、Trace。 |

## 11. Run、Trace 与复盘用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| TRC-001 | P0 | 任一工作流运行成功。 | 查询运行详情。 | 包含 AgentRun 基础信息、NodeRun 列表、TraceEvent 列表、最终输出。 | `/api/runs/{runId}` 响应。 |
| TRC-002 | P0 | 任一节点执行成功。 | 查看 NodeRun。 | NodeRun 包含 nodeId、nodeName、nodeType、inputJson、outputJson、status、durationMs。 | 运行详情。 |
| TRC-003 | P0 | 发生 Schema 校验失败。 | 查看 Trace。 | 存在 SCHEMA_VALIDATION 事件，包含字段路径和中文错误。 | TraceEvent detailJson。 |
| TRC-004 | P0 | LLM 节点执行。 | 查看 Trace。 | 存在 MODEL_REQUEST 和 MODEL_RESPONSE，默认保存提示词和输出全文。 | TraceEvent detailJson。 |
| TRC-005 | P0 | Java 方法节点执行。 | 查看 Trace。 | 存在 JAVA_METHOD_CALL，包含方法标识、入参、出参、耗时或异常摘要。 | TraceEvent。 |
| TRC-006 | P0 | Tool 节点执行。 | 查看 Trace。 | 存在 TOOL_CALL，包含工具标识、参数、返回值和耗时。 | TraceEvent。 |
| TRC-007 | P0 | External Agent 节点执行。 | 查看 Trace。 | 存在 EXTERNAL_AGENT_CALL，包含适配器、调用方式、退出码或 HTTP 状态、结果摘要。 | TraceEvent。 |
| TRC-008 | P0 | AgentCall 节点执行。 | 查看 Trace 和运行详情。 | 存在 AGENT_CALL 事件，父子运行关系可跳转。 | 页面截图、接口响应。 |
| TRC-009 | P0 | 任一运行结束。 | 查看 Trace。 | 存在 RUN_FINISHED 事件。 | TraceEvent。 |
| TRC-010 | P1 | 存在大量 Trace detailJson。 | 查看运行列表和详情。 | 列表读取 summary，详情读取 detailJson，页面可用。 | 页面截图、接口响应。 |
| TRC-011 | P0 | 历史运行绑定旧 workflowVersionId。 | 保存新草稿后回看历史运行。 | 历史运行仍可还原当时的版本定义和 Schema 引用。 | 运行详情、版本详情。 |

## 12. 节点验收用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| EVL-001 | P0 | 存在 LLM、REVIEW 或 SUMMARY 节点的已持久化版本。 | 创建 EvalSuite。 | EvalSuite 绑定 agentId、workflowVersionId、nodeId，状态为 DRAFT。 | 接口响应、数据库记录。 |
| EVL-002 | P0 | 存在 DRAFT EvalSuite。 | 创建 USER_CREATED EvalCase。 | 用例创建成功，默认可计入正式通过率。 | 接口响应。 |
| EVL-003 | P0 | 存在成功 NodeRun。 | 从 NodeRun 生成 EvalCase。 | 用例状态为 AI_DRAFT_PENDING，复制 NodeRun 输入和输出，记录来源链路。 | 接口响应、数据库记录。 |
| EVL-004 | P0 | 存在 AI_DRAFT_PENDING 用例。 | 查询或运行验收。 | 未确认用例默认不计入正式通过率。 | EvalRun 结果。 |
| EVL-005 | P0 | 存在 AI_DRAFT_PENDING 用例。 | 确认用例。 | confirmStatus 变为 USER_CONFIRMED。 | 接口响应。 |
| EVL-006 | P0 | EvalSuite 无可计入正式通过率的用例。 | 确认 EvalSuite。 | 确认失败，返回中文错误。 | 接口响应。 |
| EVL-007 | P0 | EvalSuite 有正式用例。 | 确认 EvalSuite。 | 状态变为 CONFIRMED。 | 接口响应。 |
| EVL-008 | P0 | EvalSuite 已 CONFIRMED。 | 运行验收。 | 创建 EvalRun，生成 EvalCaseResult，返回通过率、通过数、失败数。 | 接口响应、数据库记录。 |
| EVL-009 | P0 | EvalCase 配置 outputSchema 和确定性断言。 | 运行验收。 | 执行顺序为 outputSchema 校验、确定性断言、可选 LLM 评分。 | EvalRun 明细、Trace。 |
| EVL-010 | P0 | 存在 critical=true 用例。 | 让关键用例失败。 | 整次验收结果明确失败或风险提示，不被整体通过率掩盖。 | EvalRun 详情。 |
| EVL-011 | P1 | 存在多次 EvalRun。 | 查询验收历史对比。 | 返回历史通过率、通过数、失败数和变化信息。 | `/run-history` 响应。 |
| EVL-012 | P1 | EvalSuite 已 ARCHIVED。 | 尝试运行验收。 | 请求失败，不允许创建新 EvalRun。 | 接口响应。 |
| EVL-013 | P1 | EvalCase 已 ARCHIVED。 | 尝试更新用例。 | 更新失败。 | 接口响应。 |
| EVL-014 | P1 | EvalRun 已存在。 | 查询 EvalRun 结果明细。 | 返回 caseId、title、passed、critical、output、assertionResults、scoreResult、errorMessage。 | 接口响应。 |

## 13. 前端页面用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| UI-001 | P1 | 前端可访问。 | 打开首页。 | 展示 Agent 数量、最近运行、失败运行和快捷入口。 | 页面截图。 |
| UI-002 | P1 | 前端可访问。 | 访问 Agent 列表。 | 可查看、搜索、创建、编辑和停用 Agent。 | 页面截图。 |
| UI-003 | P1 | 已有 Agent。 | 访问 Agent 详情。 | 展示基础信息、当前草稿、当前发布版本、历史入口和运行入口。 | 页面截图。 |
| UI-004 | P0 | 已有 Agent。 | 打开工作流设计器。 | 左节点库、中画布、右配置面板、顶部操作栏可用。 | 页面截图。 |
| UI-005 | P1 | 工作流设计器可用。 | 在常见桌面分辨率下操作画布。 | 节点、边和配置面板不互相遮挡。 | 页面截图。 |
| UI-006 | P1 | 工作流设计器有校验错误。 | 执行校验。 | 顶部摘要和节点或字段定位同时提示错误。 | 页面截图。 |
| UI-007 | P1 | 调试页面可访问。 | 打开调试页面。 | 输入表单基于 START inputSchema 渲染，页面显示实际运行版本。 | 页面截图。 |
| UI-008 | P0 | 存在失败运行。 | 打开运行详情。 | 能快速定位失败节点、错误信息、Trace 时间线和字段路径。 | 页面截图。 |
| UI-009 | P1 | 存在父子运行。 | 在运行详情跳转父子运行。 | 跳转关系正确。 | 页面截图。 |
| UI-010 | P1 | Schema 页面可访问。 | 创建、编辑、查看 Schema。 | 页面展示版本、锁定和引用状态。 | 页面截图。 |
| UI-011 | P1 | Eval 页面可访问。 | 管理 Suite、Case、Run。 | 可查看验收集、用例、运行结果和历史对比。 | 页面截图。 |
| UI-012 | P1 | 系统设置页面可访问。 | 查看和更新可编辑配置。 | 可编辑项可更新，不可编辑项返回明确中文错误。 | 页面截图、接口响应。 |
| UI-013 | P1 | 页面出现空数据。 | 查看空态。 | 空态说明下一步操作，不只是“暂无数据”。 | 页面截图。 |
| UI-014 | P1 | 页面出现错误。 | 查看错误态。 | 错误文案说明哪里错、怎么修正。 | 页面截图。 |
| UI-015 | P1 | 页面存在状态标签。 | 检查 DRAFT、PUBLISHED、HISTORY、SUCCESS、FAILED、TIMEOUT、CANCELED 等状态。 | 状态色和文字语义稳定，不只依赖颜色。 | 页面截图。 |

## 14. 安全与配置边界用例

| 用例编号 | 优先级 | 前置条件 | 操作步骤 | 预期结果 | 证据 |
|----------|--------|----------|----------|----------|------|
| SEC-001 | P0 | 已配置 OpenAI API Key。 | 调用设置查询接口和查看设置页。 | API Key 不通过接口和页面明文返回。 | 接口响应、页面截图。 |
| SEC-002 | P0 | Custom HTTP 外部 Agent 配置了敏感 header。 | 查询外部 Agent 详情。 | 敏感 header 不明文回显。 | 接口响应。 |
| SEC-003 | P0 | Custom CLI 外部 Agent 可创建。 | 创建 command_json。 | CLI arguments 必须是数组，不保存拼接后的 shell 字符串。 | 接口请求和数据库记录。 |
| SEC-004 | P0 | CONDITION 节点可配置。 | 尝试配置脚本或函数表达式。 | 不支持任意脚本、过滤表达式和函数调用。 | 接口响应。 |
| SEC-005 | P0 | Java 方法节点可配置。 | 尝试通过页面输入任意类名或方法名绕过注册目录。 | 不允许绕过注册目录。 | 页面行为、接口响应。 |
| SEC-006 | P1 | Trace 全文保存开关默认开启。 | 执行 LLM 节点并查看 Trace。 | 默认保存全文；测试报告不记录敏感 Key。 | Trace 截图、脱敏报告记录。 |

## 15. 回归测试选择规则

| 变更类型 | 必须回归的用例范围 |
|----------|--------------------|
| 工作流版本或发布逻辑变更 | WFV、VAL、RUN、TRC 全部 P0 用例。 |
| Schema 或 MappingService 变更 | SCH、RUN、NOD、EVL 中涉及 Schema 的 P0 用例。 |
| 运行状态机变更 | RUN、NOD、TRC 全部 P0 用例。 |
| LLM 网关或结构化输出变更 | NOD-002 至 NOD-006、TRC-004、EVL-009。 |
| Java 方法或 Tool 目录变更 | ENV-007、ENV-008、NOD-010 至 NOD-012。 |
| AGENT_CALL 变更 | NOD-013、NOD-014、TRC-008。 |
| EXTERNAL_AGENT 变更 | ENV-005、ENV-006、NOD-015 至 NOD-017、SEC-003。 |
| Eval 变更 | EVL 全部 P0 用例。 |
| OpenAPI 或前端类型变更 | API-007 至 API-009，以及相关页面用例。 |

## 16. 执行记录建议

测试人员执行时，建议在 `8_change_list\agent-management-platform-v1\test_result` 的报告中记录：

- 已执行用例编号。
- 每个用例的通过、不通过或未测状态。
- 失败用例关联的缺陷编号。
- 关键证据路径或截图说明。
- 未执行用例的原因。
- 回归测试覆盖范围和结果。


## 03-Agent管理平台V1独立测试方案-v1

# Agent 管理平台 V1 独立测试方案 v1

## 1. 文档定位

本文档是站在独立测试视角编写的 V1 新测试方案。

本方案不复述也不依赖 `8_change_list\agent-management-platform-v1\test_steps.md` 中既有测试计划的结构和分组方式，而是按“平台是否真的可验收交付”的逻辑组织验收。

本方案直接以以下事实为前提：

- 验收环境中的 OpenAI 与外部 Agent 走真实联通，不使用 Mock 代替正式验收结论。
- 默认模型名称与凭证可配置，测试前由用户提供真实配置。
- UI 与 API 同权，任何一侧不成立都不能判定对应能力通过。
- 必须建立一套正式基线 EvalSuite / EvalCase，作为后续回归和版本对比基线。
- 性能门槛属于本次验收范围，不是后补项。

## 2. 编写依据

- `0_specifications\develop_specification.md`
- `3_product_design\Agent管理平台-产品设计-总纲-v1.md`
- `3_product_design\Agent管理平台-产品设计-04-工作流画布与版本规则-v1.md`
- `3_product_design\Agent管理平台-产品设计-05-节点体系设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-06-Schema与POJO设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-07-节点验收设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-08-运行调用与Trace设计-v1.md`
- `3_product_design\Agent管理平台-产品设计-09-信息架构与验收标准-v1.md`
- `4_arch_design\Agent管理平台-架构设计-总纲-v1.md`
- `4_arch_design\08-Trace追踪与调试架构设计-v1.md`
- `4_arch_design\10-节点验收架构设计-v1.md`
- `4_arch_design\12-公共接口架构设计-v1.md`
- `4_arch_design\13-部署安全与质量架构设计-v1.md`
- `7_interface_design\05-响应错误分页与OpenAPI约定-v1.md`
- `8_change_list\agent-management-platform-v1\plan.md`
- `8_change_list\agent-management-platform-v1\design.md`

## 3. 测试目标

本方案不以“页面都能点开”作为验收目标，而以以下五个问题是否成立作为验收标准：

1. 平台契约是否单一且稳定。
2. 工作流运行是否在成功、失败、超时、取消场景下都语义正确。
3. 任何一次运行、任意一个节点、任意一次验收结果是否都可追溯。
4. UI 展示与 API/数据库语义是否一致，不存在第二套真相。
5. 在真实 OpenAI 与真实外部 Agent 联通条件下，平台是否达到最低可用性能门槛。

## 4. 独立测试原则

### 4.1 先验语义，再验功能

优先验证版本不可变、Schema 锁定、运行状态、ID 语义、Trace 完整性等平台语义，再验证页面交互是否顺畅。

### 4.2 先验负向，再验正向

先验证“系统何时必须拒绝、必须失败、必须报清楚”，再验证理想链路。

### 4.3 UI 与 API 同权

每个关键能力至少同时经过以下两条路径中的两条：

- 页面操作触发。
- 直接 API 调用。

结论原则：

- UI 成功但 API 语义错误，不通过。
- API 成功但 UI 无法正确表达，不通过。

### 4.4 真链路优先

OpenAI、外部 Agent、数据库迁移、OpenAPI 生成、前端类型生成均按真实链路验收，不用局部假对象替代最终结论。

### 4.5 回归可复用

本方案中的关键链路必须沉淀为后续可重复执行的验收样本，尤其是基线 EvalSuite 和性能样本。

## 5. 验收范围

### 5.1 必测范围

- Agent 管理
- 工作流画布与版本管理
- Schema 管理与锁定
- Java 方法目录与执行
- 工具目录与执行
- 外部 Agent 适配与执行
- 内部 Agent 调用
- START、END、LLM、CONDITION、JAVA_METHOD、TOOL、AGENT_CALL、EXTERNAL_AGENT、REVIEW、SUMMARY 节点
- 同步运行与调试运行
- AgentRun、NodeRun、TraceEvent、AgentMessage
- EvalSuite、EvalCase、EvalRun、EvalCaseResult
- 系统设置
- OpenAPI 契约与前端类型生成

### 5.2 明确不在本次通过判定中的内容

- LLM 业务效果的绝对优劣排名
- 公网 SaaS 安全边界
- 登录、权限、多租户
- 完整工作流智能质量评分

说明：

LLM 输出质量需要验证“是否满足用户定义断言与 Schema”，但不以主观文风作为通过前提。

## 6. 验收分层模型

### 6.1 L0 合同层

验证对象：

- Flyway 迁移
- 表结构、唯一约束、部分唯一索引
- OpenAPI 文档
- 前端自动生成类型
- 枚举、错误码、分页、字段命名

通过标准：

- 接口、前端类型、运行返回体、错误体使用同一套契约。
- 不存在前后端影子 DTO。
- `runId`、`evalRunId`、`nodeRunId` 语义与设计一致。

### 6.2 L1 配置层

验证对象：

- OpenAI 配置
- 默认模型设置
- 超时和运行限制
- 外部 Agent 适配器配置

通过标准：

- 配置项可保存、可读取、可生效。
- 密钥不通过前端返回。
- 运行限制修改后对实际执行生效。

### 6.3 L2 主数据层

验证对象：

- Schema
- Java 方法
- Tool
- External Agent
- AgentDefinition
- WorkflowVersion

通过标准：

- 状态、唯一性、锁定规则、版本规则都成立。
- 引用关系变化能正确影响“是否允许修改/发布/运行”。

### 6.4 L3 运行层

验证对象：

- 调试运行
- API 正式运行
- 节点执行器
- 运行状态传播

通过标准：

- 主链路成功。
- 失败、超时、取消、结构化解析失败、Schema 校验失败都能正确分类。
- 运行类接口在运行记录已创建后必须返回 `runId`。

### 6.5 L4 追溯层

验证对象：

- AgentRun
- NodeRun
- TraceEvent
- AgentMessage
- EvalRun / EvalCaseResult

通过标准：

- 任一失败都能定位到节点、字段路径、目标对象、运行版本。
- 历史运行可回看绑定版本和来源链路。

### 6.6 L5 体验层

验证对象：

- 页面可达性
- 页面文案
- 画布操作
- 运行详情可诊断性
- 验收结果可解释性

通过标准：

- 用户不依赖数据库查询即可完成常规调试和失败定位。

## 7. 核心验收轴

### 7.1 版本与不可变性

重点检查：

- 每个 Agent 同时最多一个 DRAFT。
- 每个 Agent 同时最多一个 PUBLISHED。
- 历史版本只读。
- 调试运行若基于草稿，运行时必须先固化不可变版本。
- 历史运行永远绑定当时的 `workflowVersionId`。

不通过判定：

- 草稿后改动影响历史运行回看结果。
- 历史版本被原地编辑。
- 保存草稿、复制草稿、发布三条链路的状态迁移不一致。

### 7.2 Schema 与映射

重点检查：

- Schema 草稿原地更新与锁定版本新建规则。
- JSONPath 受控子集生效。
- 输入映射、输出映射、类型冲突、数组越界、路径缺失失败规则正确。
- Schema 校验失败返回字段路径、失败原因、中文提示。

不通过判定：

- 映射规则在不同节点执行器中不一致。
- 已发布工作流引用的 Schema 仍可被破坏性修改。
- 字段级错误无法定位。

### 7.3 运行状态与错误分类

重点检查：

- `SUCCESS`、`FAILED`、`TIMEOUT`、`CANCELED` 语义独立。
- OpenAI 调用失败、结构化解析失败、Schema 校验失败必须可区分。
- Java 方法执行失败与 Schema 失败必须可区分。
- 外部 Agent 失败必须包含适配器、退出码或 HTTP 状态、错误摘要。

不通过判定：

- 运行状态混用。
- 错误只给技术堆栈，没有中文业务说明。

### 7.4 Trace 与可诊断性

重点检查：

- NodeRun 输入输出全文保存。
- MODEL_REQUEST / MODEL_RESPONSE / SCHEMA_VALIDATION / NODE_ERROR 等关键事件齐全。
- 列表页读摘要，详情页读明细。
- 父子 AgentRun 跳转、来源链路、EvalRun 关联完整。

不通过判定：

- 失败时无法定位失败节点。
- 运行详情无法回看实际绑定版本。
- 从 NodeRun 生成 EvalCase 缺少来源链路。

### 7.5 UI/API 一致性

重点检查：

- 页面字段、状态、错误提示、版本指针、运行编号与 API 一致。
- 页面展示的 runId 与接口返回 runId 一致。
- 页面中的 nodeRunId 传递使用数据库主键，不派生第二套编号。

不通过判定：

- 页面展示状态与实际接口状态不一致。
- 页面能操作但接口语义错误，或接口可用但页面无法闭环。

## 8. 验收环境要求

### 8.1 基础环境

- 前端、后端、PostgreSQL 均按正式 V1 部署方式启动。
- Flyway 从空库完成初始化。
- OpenAI 凭证与默认模型由用户在测试开始前提供。
- 至少准备 1 个可用 CLI 外部 Agent 和 1 个可用 HTTP 外部 Agent。

### 8.2 测试数据准备

至少准备以下对象：

- 3 个 Schema：简单文本、结构化对象、嵌套数组对象。
- 2 个 Java 方法：一个正常返回，一个稳定报错。
- 2 个 Tool：一个正常返回，一个稳定报错。
- 2 个 External Agent 适配器：一个 CLI，一个 HTTP。
- 3 个 Agent：
  - 基础摘要 Agent
  - 条件分支 Agent
  - 互调 Agent

### 8.3 基线模型说明

- 默认模型名称写入测试记录，但在本方案中不预先硬编码。
- 如测试期间切换模型，必须在测试结果中记录切换时间、模型名称、影响范围。

## 9. 主链路验收场景

### 9.1 最小可运行链路

目标：

验证平台最小闭环可运行。

链路：

- 创建 Agent
- 创建 START -> LLM -> END 工作流
- 保存草稿
- 发布
- UI 调试运行
- API 正式运行
- 查看 Run 详情与 Trace

通过标准：

- UI 与 API 都返回成功。
- `workflowVersionId` 一致且可回看。
- Run 详情可看到完整 NodeRun 和模型 Trace。

### 9.2 条件分支链路

目标：

验证 CONDITION 节点语义。

覆盖点：

- 命中显式分支
- 未命中显式分支走默认分支
- 条件表达式输入类型不匹配
- 缺少默认分支导致无法发布

### 9.3 Java 方法链路

目标：

验证 JAVA_METHOD 的注册、安全和错误归类。

覆盖点：

- 正常调用
- 输入 Schema 不通过
- JSON 转 POJO 失败
- 方法抛异常
- 输出 Schema 不通过

### 9.4 Tool 链路

目标：

验证 TOOL 的注册调用和失败处理。

覆盖点：

- 已启用工具调用成功
- 工具参数不合法
- 工具执行失败
- 工具返回值不符合 Schema

### 9.5 External Agent 链路

目标：

验证 EXTERNAL_AGENT 在真实外部环境中的可用性和可诊断性。

覆盖点：

- CLI 适配器成功
- HTTP 适配器成功
- 超时
- 非零退出码
- HTTP 非 2xx
- `resultSource` 可提取
- `resultSource` 提取失败后退化为文本摘要

### 9.6 Agent 互调链路

目标：

验证 AGENT_CALL 的发布依赖、父子关系和状态传播。

覆盖点：

- 调用已发布 Agent 成功
- 目标 Agent 未发布
- 直接调用自己
- 深度超过上限
- 子运行超时导致父运行按 `TIMEOUT` 处理

### 9.7 REVIEW / SUMMARY 链路

目标：

验证 LLM 类变体节点的结构化输出与断言可用性。

覆盖点：

- 文本输出
- 结构化输出
- 输出 Schema 校验失败
- 生成可用于 Eval 的样本

## 10. 负向验收场景

### 10.1 发布前拦截

必须阻断发布的场景：

- 缺少 START
- 缺少 END
- CONDITION 无默认分支
- LLM 缺少必要提示词
- JAVA_METHOD 引用未启用方法
- EXTERNAL_AGENT 未选择适配器
- AGENT_CALL 指向自己
- 引用对象不存在
- 输入/输出映射字段缺失

### 10.2 运行时失败

必须清晰失败的场景：

- OpenAI 调用失败
- 结构化响应解析失败
- Schema 校验失败
- Java 方法执行失败
- Tool 执行失败
- External Agent 执行失败
- 最大步数超限
- Agent 调用深度超限

### 10.3 历史追溯失败防线

必须验证：

- 草稿修改不会污染历史调试运行
- 历史发布版本不会被新发布覆盖内容
- 运行详情始终能回看实际绑定版本

## 11. UI 与 API 联合验收方法

### 11.1 联合验收规则

每个关键能力至少执行以下动作：

- 先通过 UI 创建或触发。
- 再通过 API 查询结果或再次触发。
- 最后核对页面展示与接口返回。

### 11.2 重点联合对象

- Agent 状态
- WorkflowVersion 指针
- Schema 锁定状态
- Run 状态
- NodeRun 列表
- TraceEvent 时间线
- EvalSuite / EvalCase / EvalRun 状态

### 11.3 一票否决项

- 页面展示当前发布版本，但 API 返回的不是同一版本。
- 页面显示运行成功，但接口状态为失败或超时。
- 运行详情无法用页面定位 API 中的失败字段或失败节点。

## 12. 基线 EvalSuite / EvalCase 方案

### 12.1 目标

建立一套最小但正式的节点验收基线，覆盖 LLM、REVIEW、SUMMARY 三类节点，供后续版本回归复用。

### 12.2 基线套件规划

至少建立以下 3 个 EvalSuite：

1. 结构化分类套件
2. 审核结论套件
3. 摘要输出套件

### 12.3 结构化分类套件

建议绑定：

- 节点类型：LLM
- 输出类型：结构化 JSON

至少包含以下用例：

- 正常输入，输出完整结构
- 缺少关键信息时，字段允许为空但结构不能乱
- 高相似输入稳定分类
- 关键字段枚举值合法
- 关键用例：禁止输出未定义字段类型

### 12.4 审核结论套件

建议绑定：

- 节点类型：REVIEW
- 输出字段：`passed`、`reason`、`score`、`suggestions`

至少包含以下用例：

- 明确通过案例
- 明确拒绝案例
- 边界模糊案例
- 关键用例：必须输出通过/拒绝结论，不能只给解释

### 12.5 摘要输出套件

建议绑定：

- 节点类型：SUMMARY
- 输出模式：文本或结构化摘要

至少包含以下用例：

- 短文本摘要
- 长文本摘要
- 含噪声信息摘要
- 关键用例：必须覆盖核心事实，不能输出“无法总结”

### 12.6 用例数量建议

初始正式基线建议：

- 每个 Suite 8 到 12 条用例。
- 每个 Suite 至少 2 条关键用例。
- 总正式基线不少于 24 条已确认用例。

### 12.7 确认规则

- AI 生成草稿默认 `AI_DRAFT_PENDING`。
- 只有 `USER_CONFIRMED` 用例计入正式通过率。
- 每次模型调整、提示词调整、Schema 调整后都必须重跑基线 Suite。

## 13. 性能验收方案

### 13.1 性能验收原则

本次性能验收不追求极限压测，而验证平台在单用户/内网 V1 目标下是否达到“可持续使用”的最低门槛。

### 13.2 必测性能项

- 工作流发布耗时
- 调试运行端到端耗时
- API 同步运行端到端耗时
- 运行详情加载耗时
- Trace 明细加载耗时
- EvalRun 执行耗时
- 外部 Agent 超时控制准确性

### 13.3 建议性能门槛

以下门槛作为本方案默认验收基线，若测试前产品方另行确认更严格门槛，以更严格值为准。

1. 普通 START -> LLM -> END 同步运行：
   目标：在真实 OpenAI 环境下，90% 请求总耗时不超过 15 秒。
2. START -> JAVA_METHOD -> END：
   目标：90% 请求总耗时不超过 3 秒。
3. START -> EXTERNAL_AGENT -> END：
   目标：在外部 Agent 正常响应时，90% 请求总耗时不超过 20 秒。
4. 运行详情页：
   目标：包含 20 个以内 NodeRun、200 条以内 TraceEvent 的详情页首次可用加载不超过 3 秒。
5. EvalRun：
   目标：单套 10 条已确认用例运行完成时间不超过 120 秒。
6. 超时控制：
   目标：当节点超时阈值设置为 N 秒时，实际超时终止时间应落在 N 到 N+5 秒范围内。

### 13.4 性能异常判定

以下情况判为不通过或需整改：

- 平均值好看，但 P90 或 P95 长尾严重失控。
- 超时阈值配置存在明显失效。
- Run 成功但详情页因 Trace 过大无法打开。
- EvalRun 可以执行但无法在可接受时间内得到结果。

## 14. 数据检查与证据要求

每类关键验收都必须至少保留一种可复核证据：

- API 请求与响应样本
- 页面截图
- Run 详情截图
- TraceEvent 摘要与关键 detailJson
- 数据库关键记录截图或导出
- 性能统计表

重点证据对象：

- `workflowVersionId`
- `runId`
- `evalRunId`
- `nodeRunId`
- 失败字段路径
- 子运行编号
- 外部 Agent 退出码 / HTTP 状态

## 15. 准入与准出

### 15.1 测试准入

进入正式验收前必须满足：

- 前后端可部署并连通数据库。
- Flyway 可从空库执行完成。
- OpenAPI 可生成且前端类型已同步。
- OpenAI 与外部 Agent 凭证已配置。
- 至少一条基础工作流可发布。

### 15.2 测试准出

判定“通过”必须同时满足：

- P0 场景全部通过。
- P1 场景无阻断性缺陷。
- 核心基线 EvalSuite 全部可执行。
- 正式基线通过率达到 Suite 设定阈值，且无关键用例失败。
- 性能门槛达到要求。
- 关键证据已留存。

判定“有条件通过”适用条件：

- 主链路通过。
- 可追溯性成立。
- 仅存在不影响当前上线目标的 P2 问题，且有明确整改计划。

判定“不通过”适用条件：

- 版本不可变规则不成立。
- 运行状态或运行 ID 语义混乱。
- Schema 校验和字段路径无法定位。
- Trace 无法支撑失败定位。
- UI/API 任一侧不能闭环。
- 性能门槛明显未达标。

## 16. 执行顺序建议

1. 合同层检查：迁移、OpenAPI、前端类型、错误模型。
2. 配置层检查：设置项、真实联通、超时配置。
3. 主数据检查：Schema、方法、工具、外部 Agent、Agent。
4. 工作流与版本检查：草稿、发布、历史版本、复制草稿。
5. 运行主链路检查：最小链路、条件、方法、工具、外部 Agent、互调。
6. 追溯检查：Run、NodeRun、Trace、版本回看、来源链路。
7. Eval 基线建立与执行。
8. 性能与稳定性检查。
9. 缺陷复验与最终结论。

## 17. 重点风险提示

- 如果真实 OpenAI 模型在测试期间变更，LLM 输出稳定性会波动，必须记录模型版本与时间。
- 外部 Agent 的不稳定性不能成为平台错误分类缺失的借口；平台仍需给出清晰失败摘要。
- 如果 Trace 全文保存开启，性能验收必须覆盖“大 Trace 详情可读性”，否则上线后调试页可能不可用。
- 如果页面运行成功但无法解释为什么成功或失败，本质上仍是不通过。

## 18. 最终输出物

本方案执行完成后，至少输出以下结果：

- 测试执行记录
- 缺陷清单
- 基线 EvalSuite / EvalCase 清单
- 性能统计结果
- 验收结论：通过 / 有条件通过 / 不通过
- 风险余项列表


## 04-Agent管理平台V1独立测试用例清单-v1

# Agent 管理平台 V1 独立测试用例清单 v1

## 1. 文档定位

本文档是 [03-Agent管理平台V1独立测试方案-v1.md](./03-Agent管理平台V1独立测试方案-v1.md) 的配套执行清单。

本文档只服务于独立测试方案，不覆盖、不改写 `8_change_list\agent-management-platform-v1\test_steps.md` 中已有测试用例文档。

## 2. 使用说明

### 2.1 用例编号规则

- `C`：合同层
- `CFG`：配置层
- `MD`：主数据层
- `WF`：工作流与版本层
- `RT`：运行层
- `TR`：追溯层
- `EV`：节点验收层
- `UI`：UI / API 一致性层
- `PF`：性能层

### 2.2 优先级说明

- `P0`：不通过即整体验收不通过
- `P1`：必须完成，允许先修复再复验
- `P2`：优化项或补充验证项

### 2.3 执行记录要求

每条用例执行时至少补充以下证据之一：

- API 请求与响应样本
- 页面截图
- Run 详情截图
- TraceEvent 明细截图
- 数据库关键记录截图
- 性能统计数据

## 3. 覆盖概览

| 分组 | 目标 | 用例数 |
|------|------|------:|
| 合同层 | 验证迁移、契约、错误模型和 ID 语义 | 6 |
| 配置层 | 验证真实联通配置和运行限制生效 | 5 |
| 主数据与版本层 | 验证主数据、Schema、工作流版本规则 | 11 |
| 运行层 | 验证节点执行、失败分类和状态传播 | 20 |
| 追溯与验收层 | 验证 Run / Trace / Eval 的回溯能力 | 10 |
| 性能层 | 验证最小可用性能门槛 | 7 |
| 合计 |  | 59 |

## 4. 用例清单

### 4.1 合同层

| 编号 | 优先级 | 用例标题 | 前置条件 | 核心步骤 | 预期结果 / 关键检查点 |
|------|------|------|------|------|------|
| C-01 | P0 | Flyway 空库初始化 | 准备空 PostgreSQL 数据库 | 1. 启动后端。<br>2. 观察 Flyway 执行。<br>3. 检查核心表是否创建完成。 | Flyway 全量成功；核心表、索引、版本表存在；无手工建表依赖。 |
| C-02 | P0 | 唯一约束与部分唯一索引验证 | 系统已启动 | 1. 重复创建 `agentKey`、`schemaKey + version`、`toolKey`、`methodKey`。<br>2. 为同一 Agent 尝试制造两条 DRAFT / 两条 PUBLISHED。 | 唯一约束生效；同一 Agent 同时最多一条 DRAFT、一条 PUBLISHED；错误提示中文可理解。 |
| C-03 | P0 | OpenAPI 输出完整性 | 后端运行正常 | 1. 导出 OpenAPI。<br>2. 检查请求、响应、错误、分页、枚举、运行结果对象。<br>3. 抽查关键接口 `operationId`。 | OpenAPI 覆盖完整；关键模型不缺失；`operationId` 稳定；运行类接口契约完整。 |
| C-04 | P0 | 前端类型生成一致性 | OpenAPI 可用 | 1. 根据 OpenAPI 生成前端类型。<br>2. 执行前端构建。<br>3. 抽查运行、Eval、Trace 等关键页面使用的类型。 | 类型生成成功；关键页面能编译通过；不存在必须手工维护的长期影子类型。 |
| C-05 | P0 | 统一响应包装语义验证 | 系统已启动 | 1. 调用一个普通 CRUD 成功接口。<br>2. 调用一个普通参数错误接口。<br>3. 调用一个“运行记录已创建后失败”的运行接口。 | 普通成功返回 `success=true`；普通前置失败返回 `success=false`；运行创建后失败返回 `success=true` 且含 `runId`、`status`、运行级错误摘要。 |
| C-06 | P0 | 运行标识语义冻结验证 | 已产生 AgentRun、NodeRun、EvalRun | 1. 查看 `GET /api/runs/{runId}`。<br>2. 查看 `GET /api/eval-runs/{evalRunId}`。<br>3. 通过 `POST /api/node-runs/{nodeRunId}/eval-cases` 创建用例。<br>4. 对照数据库。 | `runId` 对应 `agent_run.run_no`；`evalRunId` 对应 `eval_run.run_no`；`nodeRunId` 对应 `node_run.id`；无第二套运行编号。 |

### 4.2 配置层

| 编号 | 优先级 | 用例标题 | 前置条件 | 核心步骤 | 预期结果 / 关键检查点 |
|------|------|------|------|------|------|
| CFG-01 | P0 | 系统设置读写与回显 | 已登录到前端设置页或可直接调 API | 1. 读取系统设置。<br>2. 修改默认模型、超时、最大步数、最大深度。<br>3. 再次读取。 | 设置保存成功；读取值与保存值一致；前端与 API 展示一致。 |
| CFG-02 | P0 | OpenAI 密钥不回传前端 | 已配置 OpenAI 凭证 | 1. 打开设置页。<br>2. 抓取设置接口返回。<br>3. 检查页面网络请求与页面显示。 | 前端不返回 OpenAI Key 明文；页面不展示真实密钥；后端能正常使用该 Key 发起真实调用。 |
| CFG-03 | P0 | 默认模型与超时配置生效 | 已配置真实 OpenAI | 1. 设置默认模型与 LLM 超时。<br>2. 运行未显式覆盖模型/超时的 LLM 节点。<br>3. 再次修改配置复测。 | 运行使用最新默认模型；超时设置可实际影响运行行为；运行记录与 Trace 中可观察到生效结果。 |
| CFG-04 | P0 | 最大步数与最大调用深度限制生效 | 已准备循环或深度互调样本 Agent | 1. 设置较小最大步数。<br>2. 运行循环或长链路工作流。<br>3. 设置较小调用深度。<br>4. 运行多层 AGENT_CALL。 | 超限时正确失败；状态与错误信息清晰；不会无限执行。 |
| CFG-05 | P1 | 外部 Agent 适配器连接测试 | 已准备 1 个 CLI 和 1 个 HTTP 外部 Agent | 1. 分别创建 CLI / HTTP 适配器。<br>2. 执行连接测试接口。<br>3. 页面查看测试结果。 | 连接测试结果可返回；成功和失败摘要明确；前端与接口展示一致。 |

### 4.3 主数据与版本层

| 编号 | 优先级 | 用例标题 | 前置条件 | 核心步骤 | 预期结果 / 关键检查点 |
|------|------|------|------|------|------|
| MD-01 | P0 | Schema 草稿原地更新 | 已创建 DRAFT Schema | 1. 编辑同一条 DRAFT Schema。<br>2. 连续保存两次。<br>3. 查询版本记录。 | DRAFT Schema 原地更新；未无故新增版本号；页面和接口显示一致。 |
| MD-02 | P0 | Schema 锁定后新版本演进 | 已有被发布工作流引用的 Schema | 1. 发布引用该 Schema 的工作流。<br>2. 尝试修改被引用 Schema。<br>3. 创建新版本。 | 已锁定 Schema 不可破坏性修改；只能通过新版本演进；历史运行仍可追溯旧版本。 |
| MD-03 | P1 | Java 方法目录可见性与启用状态 | 已注册至少 2 个 Java 方法 | 1. 查询方法列表与详情。<br>2. 观察启用状态。<br>3. 在工作流中尝试选择方法。 | 已注册方法可见；禁用方法不能被有效引用或发布；详情包含入参/出参 Schema 信息。 |
| MD-04 | P1 | Tool 目录可见性与启用状态 | 已注册至少 2 个 Tool | 1. 查询工具列表与详情。<br>2. 在节点中选择工具。 | 工具状态可见；不可用工具不能通过发布校验；参数/返回契约清晰。 |
| MD-05 | P1 | External Agent 适配器创建、编辑、启停 | 已准备测试目标 | 1. 创建适配器。<br>2. 编辑关键字段。<br>3. 启停状态切换。<br>4. 查询列表与详情。 | 创建、编辑、启停成功；状态一致；停用适配器不能被正常发布引用。 |
| MD-06 | P1 | Agent 创建、编辑、停用 | 系统已启动 | 1. 创建 Agent。<br>2. 编辑基础信息。<br>3. 停用 Agent。<br>4. 再次查询与调用。 | Agent 数据正确保存；停用后状态清晰；不允许按已启用逻辑继续正式运行。 |
| WF-01 | P0 | 最小工作流创建、保存、发布 | 已有可用 Schema 和模型配置 | 1. 通过画布创建 START -> LLM -> END。<br>2. 保存草稿。<br>3. 执行发布校验。<br>4. 发布。 | 草稿保存成功；发布成功；生成不可变 PUBLISHED 版本；Agent 指针更新正确。 |
| WF-02 | P0 | 缺少 START 禁止发布 | 已进入工作流设计器 | 1. 创建不含 START 的工作流。<br>2. 尝试发布。 | 发布被阻断；错误信息明确指出缺少 START。 |
| WF-03 | P0 | 缺少 END 禁止发布 | 已进入工作流设计器 | 1. 创建不含 END 的工作流。<br>2. 尝试发布。 | 发布被阻断；错误信息明确指出缺少 END。 |
| WF-04 | P0 | CONDITION 无默认分支禁止发布 | 已创建 CONDITION 节点 | 1. 配置显式条件边但不配置默认分支。<br>2. 尝试发布。 | 发布被阻断；错误信息明确指出默认分支缺失。 |
| WF-05 | P0 | AGENT_CALL 自调用禁止发布 | 已创建 AGENT_CALL 节点 | 1. 目标 Agent 选择当前 Agent 自己。<br>2. 尝试发布。 | 发布被阻断；错误信息明确说明不能直接调用自己。 |
| WF-06 | P0 | 从历史版本复制生成新草稿 | Agent 已存在历史版本 | 1. 查看版本列表。<br>2. 选择历史版本执行复制。<br>3. 查询当前草稿。 | 新草稿创建成功；记录 `sourceWorkflowVersionId`；历史版本保持只读。 |

### 4.4 运行层

| 编号 | 优先级 | 用例标题 | 前置条件 | 核心步骤 | 预期结果 / 关键检查点 |
|------|------|------|------|------|------|
| RT-01 | P0 | UI 调试运行最小链路成功 | 已发布最小工作流 | 1. 在调试页填写输入。<br>2. 发起调试运行。<br>3. 打开运行详情。 | 调试运行成功；返回 `runId`；详情展示实际绑定 `workflowVersionId`、NodeRun、Trace。 |
| RT-02 | P0 | API 正式运行最小链路成功 | Agent 已发布 | 1. 调用 `POST /api/agents/{agentKey}/runs`。<br>2. 查询运行详情。 | 正式运行成功；只能绑定发布版本；返回输出、状态、耗时、`workflowVersionId`。 |
| RT-03 | P0 | 调试运行固化草稿版本 | 存在可调试草稿 | 1. 用草稿发起调试运行。<br>2. 运行后修改草稿内容。<br>3. 回看第一次运行详情。 | 第一次运行绑定的 `workflowVersionId` 不变；历史回看内容不受后续草稿修改影响。 |
| RT-04 | P0 | 运行记录创建后失败仍返回 runId | 准备一个稳定失败节点 | 1. 发起会在节点执行阶段失败的运行。<br>2. 观察接口返回。 | 接口返回 `success=true`；`data.runId` 存在；`status` 为 `FAILED` 或 `TIMEOUT`；错误摘要可读。 |
| RT-05 | P1 | LLM 普通文本输出成功 | 配置不带 `outputSchemaRef` 的 LLM 节点 | 1. 运行文本模式 LLM 节点。<br>2. 查看输出与 Trace。 | 以普通文本模式执行；输出保存成功；有 MODEL_REQUEST / MODEL_RESPONSE。 |
| RT-06 | P0 | LLM 结构化输出成功 | 配置带 `outputSchemaRef` 的 LLM 节点 | 1. 发起结构化输出运行。<br>2. 查看节点输出与 Schema 校验记录。 | 结构化输出成功；输出符合 Schema；Trace 可见结构化调用与校验事件。 |
| RT-07 | P0 | LLM 模型调用失败分类正确 | 制造 OpenAI 调用失败条件 | 1. 发起 LLM 运行。<br>2. 查看接口返回、NodeRun、Trace。 | 运行为 `FAILED`；失败分类为模型调用失败；有中文错误摘要。 |
| RT-08 | P0 | LLM 结构化解析失败分类正确 | 构造模型输出非预期结构 | 1. 发起结构化输出运行。<br>2. 查看错误分类。 | 失败被归类为结构化响应解析失败，而不是普通 Schema 失败或模型失败。 |
| RT-09 | P0 | LLM Schema 校验失败分类正确 | 构造输出字段与 Schema 不匹配 | 1. 发起结构化输出运行。<br>2. 查看错误明细。 | 失败被归类为 Schema 校验失败；错误包含字段路径和中文说明。 |
| RT-10 | P1 | CONDITION 显式分支命中 | 已配置多分支 CONDITION | 1. 输入满足显式条件的数据。<br>2. 运行并查看 Trace。 | 命中正确显式分支；Trace 记录分支决策；运行继续执行。 |
| RT-11 | P1 | CONDITION 默认分支命中 | 已配置默认分支 | 1. 输入不命中任何显式条件的数据。<br>2. 运行并查看 Trace。 | 走默认分支；不视为失败；Trace 记录默认路由。 |
| RT-12 | P0 | CONDITION 条件求值异常失败 | 构造类型不匹配或缺字段输入 | 1. 发起运行。<br>2. 查看节点状态和错误摘要。 | 节点失败；运行为 `FAILED`；错误清晰说明条件求值异常。 |
| RT-13 | P0 | JAVA_METHOD 输入 Schema 失败 | 已配置方法节点与输入 Schema | 1. 提交不符合 inputSchema 的输入。<br>2. 查看错误返回。 | 运行失败；归类为 Schema 校验失败；错误包含字段路径。 |
| RT-14 | P0 | JAVA_METHOD JSON/POJO 转换失败 | 准备会触发序列化或反序列化失败的方法 | 1. 发起运行。<br>2. 查看错误分类。 | 归类为 `JAVA_METHOD` 执行失败，不误归类为 Schema 失败。 |
| RT-15 | P0 | JAVA_METHOD 方法内部异常处理 | 准备稳定抛异常的方法 | 1. 发起运行。<br>2. 查看 NodeRun 和 Trace。 | 运行失败；错误摘要可定位 methodKey 和失败原因；Trace 记录方法调用事件。 |
| RT-16 | P0 | JAVA_METHOD 输出 Schema 失败 | 准备返回值不满足 outputSchema 的方法 | 1. 发起运行。<br>2. 查看错误明细。 | 归类为 Schema 校验失败；返回字段路径和中文错误。 |
| RT-17 | P1 | TOOL 参数与返回值校验 | 已配置 Tool 节点 | 1. 用正确参数运行。<br>2. 用非法参数运行。<br>3. 用非法返回值样本运行。 | 成功路径通过；非法参数与非法返回值都能被正确拦截并分类。 |
| RT-18 | P1 | TOOL 执行失败处理 | 准备稳定失败工具 | 1. 发起工具运行。<br>2. 查看错误和 Trace。 | 运行为 `FAILED`；错误摘要明确；Trace 记录工具调用失败。 |
| RT-19 | P0 | EXTERNAL_AGENT CLI 成功调用 | 已准备 CLI 适配器 | 1. 配置 CLI 节点。<br>2. 发起运行。<br>3. 查看结果与 Trace。 | 节点成功；记录适配器、调用方式、耗时、结果摘要；输出写回正确。 |
| RT-20 | P0 | EXTERNAL_AGENT CLI 非零退出码处理 | 已准备会返回非零退出码的 CLI 场景 | 1. 发起运行。<br>2. 查看接口返回和 Trace。 | 运行为 `FAILED`；错误摘要包含退出码；前端可定位失败原因。 |
| RT-21 | P0 | EXTERNAL_AGENT 超时处理 | 已准备可控超时的外部 Agent | 1. 设置较短超时。<br>2. 发起运行。 | 运行为 `TIMEOUT`；超时语义独立；父运行和节点状态一致。 |
| RT-22 | P0 | EXTERNAL_AGENT HTTP 成功调用 | 已准备 HTTP 适配器 | 1. 配置 HTTP 节点。<br>2. 发起运行。 | 节点成功；记录 HTTP 方式、状态、结果摘要；输出写回正确。 |
| RT-23 | P0 | EXTERNAL_AGENT HTTP 非 2xx 处理 | 已准备会返回非 2xx 的 HTTP 场景 | 1. 发起运行。<br>2. 查看错误摘要。 | 运行为 `FAILED`；错误包含 HTTP 状态码和摘要；分类正确。 |
| RT-24 | P1 | EXTERNAL_AGENT `resultSource` 提取失败回退 | 已配置 `resultSource` | 1. 构造无法按 `resultSource` 提取业务输出的返回。<br>2. 发起运行。 | 系统退化为统一文本摘要；不把 stdout / stderr 自动当业务输出。 |
| RT-25 | P0 | AGENT_CALL 成功互调 | 已准备已发布子 Agent | 1. 运行带 AGENT_CALL 的父 Agent。<br>2. 查看父子运行详情。 | 父运行成功；子运行创建成功；父子关系、深度、结果状态都可追踪。 |
| RT-26 | P0 | AGENT_CALL 目标未发布处理 | 目标 Agent 仅有草稿无发布版本 | 1. 尝试发布或运行引用该目标的工作流。 | 发布前校验或运行时失败原因明确；不会静默调用未发布 Agent。 |
| RT-27 | P0 | AGENT_CALL 深度超限处理 | 已准备多层互调链路 | 1. 设置较小最大深度。<br>2. 发起运行。 | 到达限制时正确失败；错误说明深度超限；无无限互调。 |
| RT-28 | P0 | AGENT_CALL 子运行超时传播 | 已准备会超时的子 Agent | 1. 发起父 Agent 运行。<br>2. 观察父子状态。 | 子运行为 `TIMEOUT` 时，父节点和父运行按 `TIMEOUT` 处理；状态不混用。 |
| RT-29 | P1 | REVIEW 节点结构化输出成功 | 已配置 REVIEW 节点和输出 Schema | 1. 提交通过样本。<br>2. 提交拒绝样本。 | 输出包含 `passed`、`reason`、`score`、`suggestions` 等字段；结构化校验通过。 |
| RT-30 | P1 | SUMMARY 节点文本与结构化输出成功 | 已配置 SUMMARY 节点 | 1. 执行文本摘要。<br>2. 执行结构化摘要。 | 文本模式与结构化模式都能执行；结构化模式通过 Schema 校验。 |

### 4.5 追溯与验收层

| 编号 | 优先级 | 用例标题 | 前置条件 | 核心步骤 | 预期结果 / 关键检查点 |
|------|------|------|------|------|------|
| TR-01 | P0 | Run 详情展示绑定版本 | 已存在历史运行 | 1. 打开运行详情页。<br>2. 查看接口详情返回。 | 页面与接口都展示实际绑定 `workflowVersionId`；可回看对应版本定义。 |
| TR-02 | P0 | NodeRun 输入输出与全文 Trace 保存 | 已完成包含多节点的运行 | 1. 打开 NodeRun 详情。<br>2. 打开相关 TraceEvent。 | `NodeRun.inputJson`、`outputJson` 保存全文；关键 TraceEvent 存在明细和摘要。 |
| TR-03 | P0 | Schema 字段级错误可追溯 | 已准备触发 Schema 校验失败的样本 | 1. 发起运行。<br>2. 查看接口、页面、Trace。 | 能定位到 `runId`、`nodeRunId`、`nodeId`、字段路径、失败原因；页面可直接展示。 |
| TR-04 | P1 | 父子 AgentRun 链路追溯 | 已执行成功互调 | 1. 打开父运行详情。<br>2. 跳转子运行。<br>3. 回看 Trace。 | 父子运行跳转通畅；链路信息一致；子运行版本可回看。 |
| EV-01 | P0 | EvalSuite 创建、确认、归档流转 | 已准备可验收节点 | 1. 创建 EvalSuite。<br>2. 更新套件信息。<br>3. 确认。<br>4. 归档。 | 状态流转正确；归档后不可继续新的正式运行；页面与接口一致。 |
| EV-02 | P0 | EvalCase 确认状态与正式通过率关系 | 已创建 USER_CONFIRMED 和 AI_DRAFT_PENDING 用例 | 1. 在同一套件中混合创建两类用例。<br>2. 发起正式验收运行。 | 仅 `USER_CONFIRMED` 用例计入正式通过率；`AI_DRAFT_PENDING` 不计入。 |
| EV-03 | P0 | 从 NodeRun 生成 EvalCase 的来源链路 | 已有可用 NodeRun | 1. 调用 `POST /api/node-runs/{nodeRunId}/eval-cases`。<br>2. 打开生成的用例详情。 | 用例记录 `sourceRunId`、`sourceNodeRunId`、`sourceWorkflowVersionId`、`sourceNodeId`；默认状态为待确认。 |
| EV-04 | P0 | 基线 EvalSuite 正式执行 | 已准备已确认基线用例 | 1. 发起正式 EvalRun。<br>2. 查询详情和结果明细。 | 产生独立 `evalRunId`；汇总通过率、失败数、摘要正确；结果可回放。 |
| EV-05 | P0 | 关键用例失败阻断整体验收通过 | 套件中存在关键用例 | 1. 构造普通用例多数通过、关键用例失败场景。<br>2. 发起 EvalRun。 | 即使整体通过率达标，只要关键用例失败，整次验收应提示失败。 |
| EV-06 | P1 | EvalRun 历史对比 | 已存在同一 Suite 多次运行 | 1. 查询历史运行。<br>2. 打开历史对比。 | 能查看通过率变化、失败用例变化和摘要；对比对象准确。 |
| UI-01 | P0 | Agent / Version 状态 UI 与 API 一致 | 已有草稿、发布、历史版本 | 1. 在页面查看版本摘要。<br>2. 调用 Agent 和版本接口。 | 当前草稿、当前发布、历史版本摘要一致；页面不出现第二套版本真相。 |
| UI-02 | P0 | Run / Trace 页面与 API 一致 | 已完成多类运行 | 1. 打开运行列表和详情页。<br>2. 调用运行接口。 | 页面中的状态、耗时、失败节点、Trace 时间线与 API 一致。 |
| UI-03 | P1 | 中文错误提示与可修复性 | 已准备多类失败样本 | 1. 触发发布失败、运行失败、Schema 失败。<br>2. 检查页面和接口错误文案。 | 错误消息为中文；能说明用户应修复什么；不是只有技术堆栈。 |

### 4.6 性能层

| 编号 | 优先级 | 用例标题 | 前置条件 | 核心步骤 | 预期结果 / 关键检查点 |
|------|------|------|------|------|------|
| PF-01 | P1 | 工作流发布耗时 | 已准备标准工作流样本 | 1. 连续执行多次保存和发布。<br>2. 记录端到端耗时。 | 发布过程稳定；无明显异常长尾；满足测试前确认的发布门槛。 |
| PF-02 | P0 | START -> LLM -> END 同步运行 P90 | 已配置真实 OpenAI | 1. 连续执行不少于 20 次相同运行。<br>2. 统计总耗时。 | 90% 请求总耗时不超过 15 秒，或满足测试前确认的更严格门槛。 |
| PF-03 | P1 | START -> JAVA_METHOD -> END 同步运行 P90 | 已准备稳定 Java 方法样本 | 1. 连续执行不少于 20 次。<br>2. 统计总耗时。 | 90% 请求总耗时不超过 3 秒。 |
| PF-04 | P1 | START -> EXTERNAL_AGENT -> END 同步运行 P90 | 已准备稳定外部 Agent | 1. 连续执行不少于 20 次。<br>2. 统计总耗时。 | 90% 请求总耗时不超过 20 秒。 |
| PF-05 | P0 | 运行详情页加载性能 | 已准备包含 20 个以内 NodeRun、200 条以内 TraceEvent 的运行 | 1. 打开运行详情页。<br>2. 记录首次可用时间。 | 首次可用加载不超过 3 秒；页面可正常展开 NodeRun 与 Trace 明细。 |
| PF-06 | P1 | 10 条正式用例 EvalRun 耗时 | 已准备 10 条已确认正式用例 | 1. 发起 EvalRun。<br>2. 记录完成耗时。 | 单套 10 条正式用例运行完成时间不超过 120 秒。 |
| PF-07 | P0 | 超时控制精度 | 已准备可控超时节点 | 1. 将节点超时设置为 N 秒。<br>2. 发起运行并记录实际终止时间。 | 实际超时终止时间落在 N 到 N+5 秒范围；状态为 `TIMEOUT`。 |
| PF-08 | P2 | 连续运行稳定性 | 已准备最小成功链路 | 1. 连续执行不少于 30 次运行。<br>2. 统计失败率和异常分布。 | 无异常积累、无明显内存或连接泄漏迹象；失败应有可解释原因。 |

## 5. 正式基线建议执行集

正式验收时，以下用例建议作为第一轮必执行基线：

- `C-01` 到 `C-06`
- `CFG-01` 到 `CFG-04`
- `MD-01`
- `MD-02`
- `WF-01` 到 `WF-06`
- `RT-01` 到 `RT-04`
- `RT-06` 到 `RT-09`
- `RT-13` 到 `RT-16`
- `RT-19` 到 `RT-28`
- `TR-01` 到 `TR-03`
- `EV-01` 到 `EV-05`
- `UI-01`
- `UI-02`
- `PF-02`
- `PF-05`
- `PF-07`

## 6. 建议输出表

执行本清单时，建议最终至少形成以下结果：

- 用例执行结果表
- 缺陷清单
- 关键证据附件索引
- 性能统计表
- 基线 EvalSuite / EvalCase 执行结果



