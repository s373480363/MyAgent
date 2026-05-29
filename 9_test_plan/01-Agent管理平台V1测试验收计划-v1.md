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
- `8_dev_plan\01-完整V1开发实施方案-v1.md`
- `10_plan_process\01-V1技术实现问题列表-v1.md`

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
- 测试结果记录，写入 `12_test_result\`。

## 13. 需澄清或准备的事项

当前设计文档中 Q01 至 Q08 均已确认，没有阻塞测试计划编写的产品问题。

正式执行验收前仍需准备以下测试资源：

- 用户将提供真实 OpenAI API Key 用于连通性冒烟；测试执行前需要提醒用户配置该 Key，且 Key 不得写入测试报告正文、日志截图或前端可见响应。
- 当前本机作为 Codex CLI 和 OpenCode CLI 的可执行环境；正式执行前需要由测试人员确认命令可调用、版本可识别、工作目录可访问。
- 需要开发提供至少一个显式注册的 Java 方法测试 Bean 和一个工具测试实现，用于 JAVA_METHOD 与 TOOL 节点的确定性流程验收。
- 已在 `12_test_result\` 中补充正式测试报告模板，执行验收后应按模板记录结果。

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
