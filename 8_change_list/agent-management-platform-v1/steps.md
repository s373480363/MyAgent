# Agent 管理平台 V1 执行步骤

本文件记录本次变更的具体执行步骤和开发执行过程。原 `8_change_list\agent-management-platform-v1\status.md` 中的执行拆解与执行记录已合并到本文件，旧目录不再作为查询入口。

## V1 开发执行步骤拆解

# V1 开发执行步骤拆解 v1

## 1. 文档目标

本文档从开发人员执行视角，对《完整 V1 开发实施方案 v1》进一步拆解为可直接推进的开发步骤。

目标不是重复实施方案，而是把“先做什么、每一步交付什么、做到什么算该步骤完成”明确下来，避免开发过程中再自行二次拆分任务。

本文档默认遵循以下前提：

- 完整 V1 一次性交付，不做阶段性裁剪。
- 所有开发实现必须遵循 `8_change_list/agent-management-platform-v1/plan.md`、`7_interface_design/*`、`6_schema_design/*` 和 `8_change_list/agent-management-platform-v1/design.md`。
- 开发顺序必须围绕“先骨架、再数据与契约、再运行内核、再页面与验收”推进。

## 2. 执行原则

- 每一步都必须有明确产出，不允许以“先写一部分试试”代替正式实现。
- 后端以契约和领域模型为先，前端以 OpenAPI 类型和页面能力为先。
- 运行语义、版本语义、Schema 语义、Trace 语义、ID 语义必须在开发过程中保持单一来源，不允许边写边重新发明。
- 每完成一步，都要能回答三个问题：
  - 本步新落地了哪些代码与契约。
  - 本步依赖了哪些上一步已经完成的能力。
  - 本步完成后下一步开发人员可以直接接着做什么。

## 3. 总体执行顺序

1. 步骤 01：工程骨架与公共基线初始化
2. 步骤 02：数据库迁移基线与核心表落地
3. 步骤 03：公共领域模型、DTO、错误码与 OpenAPI 基线
4. 步骤 04：Schema 管理全链路
5. 步骤 05：Java 方法、工具、外部 Agent、系统设置主数据链路
6. 步骤 06：Agent 管理与工作流版本管理链路
7. 步骤 07：运行内核基础组件
8. 步骤 08：首条可运行主链路 `START -> LLM -> END`
9. 步骤 09：条件与 Java 方法链路
10. 步骤 10：工具、内部 Agent、外部 Agent、REVIEW、SUMMARY 节点链路
11. 步骤 11：运行查询、Trace 详情、调试链路完善
12. 步骤 12：前端主数据页面与 Agent 页面
13. 步骤 13：工作流设计器与调试页
14. 步骤 14：运行详情页与历史版本回看
15. 步骤 15：节点验收后端全链路
16. 步骤 16：节点验收前端全链路
17. 步骤 17：测试收口、联调收口与发布准备

## 4. 详细开发步骤

### 步骤 01：工程骨架与公共基线初始化

本步骤计划完成内容：

- 创建 `11_code/backend` 和 `11_code/frontend` 工程骨架。
- 后端接入 `Spring Boot 3.x`、`Java 21`、`MyBatis`、`Flyway`、`PostgreSQL`、`Spring AI`、`OpenAPI`、`JUnit 5`、`AssertJ`、`Testcontainers`。
- 前端接入 `React`、`TypeScript`、`Vite`、`Ant Design`、`React Router`、`TanStack Query`、`Zustand`、`React Flow`、`Monaco Editor`。
- 建立后端基础包结构和前端功能目录结构。
- 建立统一编码规范支撑：
  - UTF-8。
  - 中文日志、错误消息、Javadoc 和关键业务注释。
  - 后端模块分包与前端功能分包。

本步骤明确产出：

- 后端可启动空应用。
- 前端可启动空页面。
- 统一目录结构、构建脚本、基础配置文件、环境变量读取模板。
- 最小 README 和本地启动说明。

本步骤完成标准：

- 后端和前端都可以独立启动。
- 代码仓中已经具备实施方案定义的基础技术栈和目录结构。
- 不存在“后面再统一目录”或“后面再补依赖”的口头约定。

### 步骤 02：数据库迁移基线与核心表落地

本步骤计划完成内容：

- 建立 Flyway 迁移基线。
- 按实施方案顺序落地核心表：
  - `schema_definition`
  - `java_method_definition`
  - `tool_definition`
  - `external_agent_definition`
  - `agent_definition`
  - `workflow_version`
  - `system_setting`
  - `agent_run`
  - `node_run`
  - `trace_event`
  - `agent_message`
  - `eval_suite`
  - `eval_case`
  - `eval_run`
  - `eval_case_result`
- 补齐唯一约束、部分唯一索引、查询索引。
- 明确并实现工作流版本的单草稿、单发布约束。
- 明确并实现 `run_no`、`version_no`、`xxx_key` 等稳定业务键落库策略。

本步骤明确产出：

- 全量 V1 核心表的 Flyway SQL。
- 本地数据库可完整迁移到最新版本。
- 数据结构与 DDL 文档一致。

本步骤完成标准：

- 所有核心表都通过迁移创建，不依赖手工建表。
- 工作流版本状态约束、运行表索引、验收表索引已存在。
- 任何开发同学拿到仓库后执行迁移即可得到统一数据库结构。

### 步骤 03：公共领域模型、DTO、错误码与 OpenAPI 基线

本步骤计划完成内容：

- 建立后端公共响应模型：
  - `ApiResponse`
  - `ApiError`
  - `PageResponse`
- 建立全局异常体系和中文错误码枚举。
- 建立时间、JSON、分页、枚举转换、公用校验工具。
- 建立领域对象、Repository DTO、Application DTO、REST DTO 的分层结构。
- 建立 OpenAPI 输出基线，确保 Controller 一经创建即可进入 OpenAPI。
- 前端接入 OpenAPI 类型生成和 API Client 代码生成。

本步骤明确产出：

- 统一错误处理链路。
- 统一分页对象。
- OpenAPI 文档可输出。
- 前端已经能消费生成后的类型。

本步骤完成标准：

- 后续任何 REST 接口都不再单独发明响应模型。
- 前端不需要手工抄写后端 DTO。
- 运行类接口的 `success=true + data.status=FAILED/TIMEOUT` 特殊语义已经在基线层可承接。

### 步骤 04：Schema 管理全链路

本步骤计划完成内容：

- 实现 `SchemaApplicationService`。
- 实现 Schema 仓储、领域服务和锁定规则。
- 实现 REST 接口：
  - `GET /api/schemas`
  - `POST /api/schemas`
  - `PUT /api/schemas/{schemaId}`
  - `GET /api/schemas/{schemaId}`
  - `POST /api/schemas/{schemaId}/versions`
- 实现 Schema 草稿原地更新语义：
  - 仅 `status=DRAFT && locked=false` 可更新。
  - 创建新版本时复制旧版本内容并递增版本号。
- 实现 JSON Schema 校验能力和字段级错误返回结构。

本步骤明确产出：

- Schema 列表、详情、编辑草稿、创建新版本全链路可用。
- `SchemaValidationService` 可复用于后续运行时。

本步骤完成标准：

- Schema 版本行为与 `WorkflowVersion` 不混淆。
- 锁定规则已经落地，不是文档约定。
- 后续工作流节点已经可以引用真实 Schema。

### 步骤 05：Java 方法、工具、外部 Agent、系统设置主数据链路

本步骤计划完成内容：

- 实现 `JavaMethodApplicationService`、`ToolApplicationService`、`ExternalAgentApplicationService`、`SettingApplicationService`。
- 实现注册目录模型和查询链路。
- 实现 REST 接口：
  - Java 方法：列表、详情。
  - 工具：列表、详情。
  - 外部 Agent：列表、详情、创建、编辑、启停、测试。
  - 系统设置：查询、更新。
- 实现外部 Agent 的配置模型、敏感信息回显规则、命令参数数组规则。
- 实现外部 Agent 测试能力，但不创建正式 `AgentRun`。

本步骤明确产出：

- 所有工作流节点依赖的主数据目录已可查询。
- 外部 Agent 平台内维护能力已具备。
- 系统默认模型、默认超时等基础设置可落库。

本步骤完成标准：

- 工作流设计器后续所需的节点目录与引用对象都已经存在。
- 外部 Agent 详情和测试规则符合接口设计。
- 前端已能依赖这些接口构建主数据页面。

### 步骤 06：Agent 管理与工作流版本管理链路

本步骤计划完成内容：

- 实现 `AgentApplicationService` 与 `WorkflowApplicationService`。
- 实现 Agent 基础信息管理、启停、详情。
- 实现工作流草稿、历史版本、发布版本的完整版本模型。
- 实现 REST 接口：
  - `GET /api/agents`
  - `POST /api/agents`
  - `PUT /api/agents/{agentId}`
  - `PUT /api/agents/{agentId}/status`
  - `GET /api/agents/{agentId}`
  - `GET /api/agents/{agentId}/workflow-draft`
  - `PUT /api/agents/{agentId}/workflow-draft`
  - `POST /api/agents/{agentId}/workflow-draft/copy-from-version`
  - `POST /api/agents/{agentId}/workflow-draft/validate`
  - `POST /api/agents/{agentId}/workflow-draft/publish`
  - `GET /api/agents/{agentId}/workflow-versions`
  - `GET /api/agents/{agentId}/workflow-versions/{workflowVersionId}`
- 实现三条关键事务链路：
  - 保存草稿。
  - 从版本复制生成新草稿。
  - 发布草稿。

本步骤明确产出：

- Agent 页面需要的全部后端能力。
- 工作流历史版本查看、只读打开、复制生成新草稿能力。
- 当前草稿指针、当前发布指针和旧版本转 `HISTORY` 的事务一致性。

本步骤完成标准：

- 任意一次保存草稿或发布后，都不会出现两个当前 `DRAFT` 或两个当前 `PUBLISHED`。
- 调试运行前置所需的工作流版本固化能力已具备。
- Agent 详情能完整表达草稿、发布、历史入口。

### 步骤 07：运行内核基础组件

本步骤计划完成内容：

- 实现运行时骨架接口和基础实现：
  - `WorkflowValidator`
  - `WorkflowCompiler`
  - `WorkflowContext`
  - `NodeExecutorRegistry`
  - `MappingService`
  - `SchemaValidationService`
  - `TraceWriter`
  - `RuntimeLimitGuard`
- 冻结并落实运行态 ID 语义：
  - `runId = agent_run.run_no`
  - `evalRunId = eval_run.run_no`
  - `nodeRunId = node_run.id`
- 实现 JSONPath 受控子集映射。
- 实现 NodeRun 与 TraceEvent 的创建、完成、写事件机制。
- 实现运行超时、节点超时、最大步数、最大调用深度守卫。

本步骤明确产出：

- 后续所有节点执行器共用的运行时骨架。
- 统一的 Trace 写入方式。
- 统一的 Schema 校验入口。

本步骤完成标准：

- 运行态不再依赖页面逻辑或 Controller 逻辑解释。
- 映射规则与技术口径清单完全一致。
- 后续节点执行器只需要关注自己的执行逻辑，不再自行处理公共运行问题。

### 步骤 08：首条可运行主链路 `START -> LLM -> END`

本步骤计划完成内容：

- 实现 `RunApplicationService` 基础版本。
- 实现同步运行与调试运行主流程。
- 实现节点执行器：
  - `START`
  - `LLM`
  - `END`
- 实现 `OpenAiModelGateway` 和模型配置解析。
- 实现正式运行与调试运行接口：
  - `POST /api/agents/{agentKey}/runs`
  - `POST /api/agents/{agentId}/debug-runs`
- 确保运行记录创建成功后，失败也返回 `runId`。
- 确保调试运行绑定实际 `workflowVersionId`。

本步骤明确产出：

- 第一条真正可端到端执行的 Agent 主链路。
- 运行记录、NodeRun、TraceEvent 落库。
- 运行结果可通过 `runId` 回看。

本步骤完成标准：

- `START -> LLM -> END` 可执行成功、失败、超时三类结果。
- LLM 普通文本输出与结构化输出模式都能工作。
- Trace 中可看到请求、响应、Schema 校验和失败摘要。

### 步骤 09：条件与 Java 方法链路

本步骤计划完成内容：

- 实现节点执行器：
  - `CONDITION`
  - `JAVA_METHOD`
- 实现 `JavaMethodRegistry`、`JavaMethodInvoker`。
- 实现条件边求值、默认分支选择和失败处理。
- 实现 Java 方法 JSON/POJO 转换、输入输出 Schema 校验、错误分类。
- 完成第二批主链路：
  - `START -> CONDITION -> END`
  - `START -> JAVA_METHOD -> END`

本步骤明确产出：

- 结构化条件判断能力。
- Java 方法节点全链路执行能力。

本步骤完成标准：

- 条件节点分支行为可复盘。
- Java 方法节点严格区分 Schema 校验失败与执行失败。
- 已能覆盖多数非 LLM 辅助处理节点的核心运行模式。

### 步骤 10：工具、内部 Agent、外部 Agent、REVIEW、SUMMARY 节点链路

本步骤计划完成内容：

- 实现节点执行器：
  - `TOOL`
  - `AGENT_CALL`
  - `EXTERNAL_AGENT`
  - `REVIEW`
  - `SUMMARY`
- 实现 `ToolRegistry`、`ToolExecutor`。
- 实现 `AgentCallApplicationService`。
- 实现 `ExternalAgentRegistry`、`ExternalAgentAdapter`、`ExternalAgentResult` 标准结果模型。
- 实现父子运行关系记录、级联取消、级联超时传播。
- 实现外部 Agent 的 `resultSource` 提取逻辑。

本步骤明确产出：

- 所有 V1 节点类型完整可执行。
- 内部 Agent 协作与外部 Agent 调用链路打通。
- `REVIEW`、`SUMMARY` 作为 LLM 变体节点可投入使用。

本步骤完成标准：

- `AGENT_CALL` 有稳定的父子 `AgentRun` 关系。
- `EXTERNAL_AGENT` 输出解析与 stdout/stderr 语义不混淆。
- 整个工作流节点集合满足 V1 范围。

### 步骤 11：运行查询、Trace 详情、调试链路完善

本步骤计划完成内容：

- 实现运行查询接口：
  - `GET /api/runs`
  - `GET /api/runs/{runId}`
- 完成运行详情聚合模型：
  - AgentRun 基础信息
  - NodeRun 列表与详情
  - TraceEvent 时间线
  - 父子运行关系
  - 实际绑定的 `workflowVersionId`
- 补齐运行失败、超时、取消原因和字段级 Schema 错误展示对象。
- 完善调试运行过程中的版本固化与历史复盘逻辑。

本步骤明确产出：

- 后端运行查询能力完整可用。
- 运行详情页所需 DTO 完整闭环。

本步骤完成标准：

- 任意运行都可按 `runId` 查询到完整详情。
- `workflowVersionId`、失败节点、NodeRun 输入输出、Trace 全文都能回看。
- 从运行详情进入后续验收创建流程时，ID 语义已经固定。

### 步骤 12：前端主数据页面与 Agent 页面

本步骤计划完成内容：

- 落地前端公共能力：
  - 路由框架
  - 全局布局
  - 通用表格、表单、JSON 查看器、状态标签
  - API 错误处理与空态、加载态
- 完成页面：
  - `Settings`
  - `Schemas`
  - `Methods`
  - `Tools`
  - `External Agents`
  - `Agents` 列表与详情
- 接入 OpenAPI 生成类型和 API Client。

本步骤明确产出：

- 主数据页面全部可用。
- Agent 详情可以展示当前草稿、当前发布和历史版本入口。

本步骤完成标准：

- 主数据页面与对应 REST 接口全部联调通过。
- 前端没有手工维护的长期影子类型。
- 页面文案、错误提示、状态标签与后端口径一致。

### 步骤 13：工作流设计器与调试页

本步骤计划完成内容：

- 完成 `Workflow` 设计器页面：
  - 画布渲染
  - 节点拖拽与连线
  - 节点属性编辑
  - 节点库
  - JSON 视图
  - 保存草稿
  - 校验工作流
  - 发布工作流
  - 从历史版本复制生成新草稿
- 完成 `Debug` 调试页：
  - 输入表单
  - 发起调试运行
  - 展示运行摘要
  - 展示实际绑定的 `workflowVersionId`

本步骤明确产出：

- 前端工作流主操作链路完整可用。
- 设计器和调试页联动打通。

本步骤完成标准：

- 用户能完成从编辑草稿到发布、再到调试的完整前端操作闭环。
- 历史版本只能只读打开，不能误改。
- 未保存画布不能直接调试运行，必须先固化版本。

### 步骤 14：运行详情页与历史版本回看

本步骤计划完成内容：

- 完成 `Runs` 列表页与详情页。
- 展示 NodeRun 列表、节点详情、TraceEvent 时间线、父子运行跳转。
- 支持按 `workflowVersionId` 回看对应历史版本定义。
- 在运行详情页中提供从 NodeRun 生成验收用例的入口，并确保传递 `node_run.id`。

本步骤明确产出：

- 运行排障页面能力完整。
- 历史执行回看与版本回看闭环。

本步骤完成标准：

- 运行详情页能独立完成排障，不需要开发辅助查库。
- 所有运行态 ID 语义在前端页面上都已落实，无第二套编号。

### 步骤 15：节点验收后端全链路

本步骤计划完成内容：

- 实现 `EvalApplicationService` 与 `EvalCaseApplicationService`。
- 实现 REST 接口：
  - `GET /api/eval-suites`
  - `POST /api/eval-suites`
  - `PUT /api/eval-suites/{suiteId}`
  - `PUT /api/eval-suites/{suiteId}/confirm`
  - `PUT /api/eval-suites/{suiteId}/archive`
  - `GET /api/eval-suites/{suiteId}/cases`
  - `POST /api/eval-suites/{suiteId}/cases`
  - `GET /api/eval-suites/{suiteId}/cases/{caseId}`
  - `PUT /api/eval-suites/{suiteId}/cases/{caseId}`
  - `PUT /api/eval-suites/{suiteId}/cases/{caseId}/confirm`
  - `PUT /api/eval-suites/{suiteId}/cases/{caseId}/archive`
  - `POST /api/node-runs/{nodeRunId}/eval-cases`
  - `POST /api/eval-suites/{suiteId}/runs`
  - `GET /api/eval-suites/{suiteId}/runs`
  - `GET /api/eval-runs/{evalRunId}`
  - `GET /api/eval-runs/{evalRunId}/results`
  - `GET /api/eval-suites/{suiteId}/run-history`
- 实现验收执行顺序：
  - outputSchema 校验
  - 确定性断言
  - 可选 LLM 评分
- 实现来源追踪字段：
  - `sourceAgentRunDbId`
  - `sourceRunId`
  - `sourceNodeRunId`
  - `sourceWorkflowVersionId`
  - `sourceNodeId`
- 实现 EvalRun、EvalCaseResult、历史对比统计。
- 统一决定并实现 Eval 运行的 Trace 关联策略。

本步骤明确产出：

- 节点验收后端能力完整上线。
- Eval 套件、用例、运行、结果明细、历史对比全闭环。

本步骤完成标准：

- AI 草稿用例和正式确认用例统计口径严格分开。
- 关键用例失败会影响整次验收结论。
- EvalRun 能独立查询，来源链路可追溯到原始运行和原始节点。

### 步骤 16：节点验收前端全链路

本步骤计划完成内容：

- 完成 `Evals` 页面：
  - 套件列表、创建、更新、确认、归档
  - 用例列表、创建、更新、确认、归档
  - 从 NodeRun 生成用例
  - EvalRun 列表
  - EvalRun 详情
  - 结果明细
  - 历史对比
- 展示关键失败原因、通过率、失败断言、LLM 评分结果。
- 展示来源字段与来源跳转能力。

本步骤明确产出：

- 节点验收页面全链路可操作。
- 运行详情到验收、验收到历史对比的页面闭环成立。

本步骤完成标准：

- 用户无需开发介入即可维护验收套件、运行验收、分析失败。
- EvalRun 和 EvalCaseResult 的前端视图与后端对象一一对应。

### 步骤 17：测试收口、联调收口与发布准备

本步骤计划完成内容：

- 按实施方案顺序完成后端测试：
  - 领域对象单元测试
  - Schema 校验测试
  - 工作流发布校验测试
  - 运行内核测试
  - 节点执行器测试
  - 数据库集成测试
  - OpenAPI 契约测试
  - 异常与错误码测试
- 按实施方案顺序完成前端测试：
  - 页面渲染测试
  - 表单校验测试
  - 画布保存与发布测试
  - 调试运行测试
  - 运行详情测试
  - 契约类型生成测试
- 执行后端前端联调、关键路径回归、数据回溯检查。
- 输出发布说明、部署说明、测试结果和遗留问题清单。

本步骤明确产出：

- 完整测试结果。
- 发布前检查清单。
- 文档收口内容。

本步骤完成标准：

- 实施方案中的质量闸门全部通过。
- 没有未解释的运行语义分叉。
- 发布准备材料完整可交付架构师和测试负责人复核。

## 5. 步骤间依赖关系

- 步骤 01 未完成，不进入任何业务实现。
- 步骤 02 和步骤 03 是所有后续功能的前置。
- 步骤 04、05、06 完成后，运行内核和前端主页面才具备稳定依赖对象。
- 步骤 07 是步骤 08、09、10 的共同前提。
- 步骤 08、09、10 完成后，步骤 11 的运行详情聚合才有完整数据。
- 步骤 12、13、14 依赖后端契约和运行链路已经稳定。
- 步骤 15 必须在运行链路与 Trace 链路稳定后开展。
- 步骤 16 必须以后端验收对象和接口已经稳定为前提。
- 步骤 17 只在所有功能链路完成后统一收口。

## 6. 当前建议执行方式

- 实际开发时以本文件作为任务拆解基线，以 `8_change_list/agent-management-platform-v1/plan.md` 作为范围和语义基线。
- 每完成一个步骤，都应在 `8_change_list\agent-management-platform-v1\status.md` 下补充执行状态或评审结果，避免后续人员重复判断当前进度。
- 架构师审核时，优先检查以下三类内容：
  - 每个步骤的产出是否足以支撑下一步骤直接开工。
  - 是否有步骤把两个不同语义的对象混成一套实现。
  - 是否有步骤遗漏完整 V1 已承诺的页面能力、接口能力或运行语义。


## 03-步骤02数据库迁移基线与核心表落地执行记录-v1

# 步骤 02 数据库迁移基线与核心表落地执行记录 v1

## 1. 执行结论

步骤 02 已完成，可以进入步骤 03：公共领域模型、DTO、错误码与 OpenAPI 基线。

## 2. 本步骤落地内容

- 已新增 Flyway V1 基线迁移脚本：`11_code/backend/src/main/resources/db/migration/V1__create_core_tables.sql`。
- 已创建完整 V1 核心表：
  - `schema_definition`
  - `java_method_definition`
  - `tool_definition`
  - `external_agent_definition`
  - `agent_definition`
  - `workflow_version`
  - `system_setting`
  - `agent_run`
  - `node_run`
  - `trace_event`
  - `agent_message`
  - `eval_suite`
  - `eval_case`
  - `eval_run`
  - `eval_case_result`
- 已按物理外键方案落地跨表关系。
- 已落地工作流版本约束：
  - 同一 Agent 下 `version_no` 唯一。
  - 同一 Agent 同时最多一个 `DRAFT`。
  - 同一 Agent 同时最多一个 `PUBLISHED`。
- 已落地运行态 ID 语义：
  - `agent_run.run_no` 作为对外 `runId`。
  - `eval_run.run_no` 作为对外 `evalRunId`。
  - `node_run.id` 作为从 NodeRun 生成 EvalCase 接口使用的 `nodeRunId`。
  - 未为 `node_run` 新增第二套业务编号。

## 3. 验证结果

- 后端 `mvn test` 已通过。
- `DatabaseMigrationContractTests` 已覆盖核心表、关键唯一约束、物理外键和运行态 ID 语义。
- `PostgresMigrationTests` 已编写为真实 PostgreSQL 迁移集成测试；当前开发机 Docker Desktop 未运行，因此本次由 Testcontainers 自动跳过。

## 4. 后续衔接

步骤 03 可以直接基于当前数据库表结构继续实现公共领域模型、DTO、错误码与 OpenAPI 基线。

真实 PostgreSQL 迁移验证应在 Docker Desktop 或本地 PostgreSQL 可用后补跑一次，用于确认 Flyway 脚本在真实数据库上的执行结果。


## 04-步骤03公共领域模型DTO错误码与OpenAPI基线执行记录-v1

# 步骤 03 公共领域模型、DTO、错误码与 OpenAPI 基线执行记录 v1

## 1. 执行结论

步骤 03 已完成，可以进入步骤 04：Schema 管理全链路。

## 2. 本步骤落地内容

- 已新增后端公共分页与通用工具基线：
  - `11_code/backend/src/main/java/com/myagent/common/page/PageQuery.java`
  - `11_code/backend/src/main/java/com/myagent/common/page/PageResult.java`
  - `11_code/backend/src/main/java/com/myagent/common/util/CodeEnum.java`
  - `11_code/backend/src/main/java/com/myagent/common/util/EnumUtils.java`
  - `11_code/backend/src/main/java/com/myagent/common/util/JsonUtils.java`
  - `11_code/backend/src/main/java/com/myagent/common/util/TimeUtils.java`
  - `11_code/backend/src/main/java/com/myagent/common/util/ValidationUtils.java`
- 已补齐公共包结构预留：
  - `common.application`
  - `common.domain`
  - `common.repository`
  - `common.web.dto`
- 已扩展公共错误码枚举 `ErrorCode`，补齐后续主数据、协作调用、验收和配置链路所需的稳定错误码。
- 已将 `PageResponse` 与应用层 `PageResult` 建立转换入口。
- 已将 OpenAPI 基线显式注册公共模型，保证 `ApiResponse`、`ApiError`、`PageResponse` 进入契约输出。
- 已新增后端契约测试：
  - `ApiContractTests`
  - `GlobalExceptionHandlerTests`
  - `OpenApiContractTests`
- 已新增前端 OpenAPI 生成脚本与生成目录：
  - `11_code/frontend/scripts/download-openapi.ps1`
  - `11_code/frontend/src/api/generated/schema.ts`
  - `11_code/frontend/src/api/generated/package-info.ts`
- 已将前端 `httpClient.ts` 改为引用生成类型入口，并保留结构化错误语义。
- 已将 `openapi/` 加入 `.gitignore`，避免下载的 OpenAPI 中间产物污染工作区。

## 3. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run build` 已通过。
- 前端构建过程仅提示 bundle 偏大警告，不影响本步骤验收。

## 4. 环境说明

- 当前终端未直接提供 `mvn` 命令，后端验证通过仓库内 `9_dependency\tools` 下的 Maven 发行版和 JDK 21 完成。
- `PostgresMigrationTests` 仍会在 Docker 环境不可用时跳过，这属于本机容器环境限制，不影响本步骤公共契约基线结论。

## 5. 后续衔接

步骤 04 可以直接基于当前公共契约和分页基线继续实现 Schema 管理全链路。


## 05-步骤04Schema管理全链路执行记录-v1

# 步骤 04 Schema 管理全链路执行记录 v1

## 1. 执行结论

步骤 04 已完成，可以进入步骤 05：Java 方法、工具、外部 Agent、系统设置主数据链路。

## 2. 本步骤落地内容

- 已新增 Schema 领域模型：
  - `SchemaCreatedFrom`
  - `SchemaStatus`
  - `SchemaDefinition`
- 已新增 Schema 应用服务：
  - `SchemaApplicationService`
  - `DefaultSchemaApplicationService`
  - 创建 Schema
  - 更新 `status=DRAFT && locked=false` 的 Schema 草稿
  - 查询 Schema 列表和详情
  - 基于旧版本创建同一 `schemaKey` 的下一整数版本
  - 锁定 Schema 版本
- 已新增 Schema 仓储链路：
  - `SchemaRepository`
  - `SchemaMapper`
  - `SchemaRecord`
  - `MyBatisSchemaRepository`
  - `mapper/schema/SchemaMapper.xml`
- 已新增 JSON 类型处理器：
  - `JsonNodeTypeHandler`
  - `InstantTypeHandler`
- 已新增 Schema 校验能力：
  - `SchemaDefinitionValidator` 用于保存前校验 JSON Schema 定义。
  - `SchemaValidationService` 用于运行时按 Schema 引用校验业务载荷。
  - `DefaultSchemaValidationService` 返回字段路径、关键字和中文错误消息。
- 已新增 Schema REST 接口：
  - `GET /api/schemas`
  - `POST /api/schemas`
  - `PUT /api/schemas/{schemaId}`
  - `GET /api/schemas/{schemaId}`
  - `POST /api/schemas/{schemaId}/versions`
- 已修正 Schema OpenAPI 契约输出：
  - Schema 列表接口返回 `SchemaPageApiResponse`。
  - Schema 详情、创建、更新和创建新版本接口返回 `SchemaDetailApiResponse`。
  - `description`、`javaType`、`sourceSchemaId` 按接口语义保持可选。
- 已修正前端 OpenAPI 生成脚本：
  - `openapi-typescript` 增加 `--default-non-nullable=false`，避免带默认值的可选字段被误生成成必填。
- 已重新生成前端 OpenAPI 类型：
  - `11_code/frontend/src/api/generated/schema.ts`

## 3. 设计边界说明

- `sourceSchemaId` 当前仅在 `POST /api/schemas` 输入侧用于表达“基于已有 Schema 创建”的来源参数，并校验来源 Schema 存在。
- 当前数据结构设计中的 `schema_definition` 没有 `source_schema_id` 字段，因此本步骤不自行新增第二套血缘字段，避免偏离 `6_schema_design`。
- Schema 草稿保存语义与 WorkflowVersion 保持分离：Schema 的 `DRAFT` 允许原地更新，WorkflowVersion 仍是不允许原地覆盖的不可变版本模型。

## 4. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run openapi:generate` 已通过。
- 前端 `npm run build` 已通过。
- 生成类型已确认引用：
  - `SchemaDetailApiResponse`
  - `SchemaPageApiResponse`
- 生成类型已确认以下字段为可选：
  - `description`
  - `javaType`
  - `sourceSchemaId`

## 5. 环境说明

- 当前终端未直接提供 `mvn` 命令，后端验证通过仓库内 `9_dependency\tools` 下的 Maven 发行版和 JDK 21 完成。
- `local` profile 启动依赖本地 PostgreSQL；当前机器 `localhost:5432` 未提供 PostgreSQL，因此本步骤导出 OpenAPI 时使用默认 profile。
- `PostgresMigrationTests` 仍会在 Docker 环境不可用时跳过，这是本机容器环境限制，不影响本步骤 Schema 管理链路结论。

## 6. 后续衔接

步骤 05 可以直接复用当前 Schema 管理能力，为 Java 方法、工具、外部 Agent 和系统设置主数据提供真实 Schema 引用与运行时校验入口。


## 06-步骤05Java方法工具外部Agent系统设置主数据链路执行记录-v1

# 步骤 05 Java 方法、工具、外部 Agent、系统设置主数据链路执行记录 v1

## 1. 执行结论

步骤 05 已完成，可以进入步骤 06：Agent 管理与工作流版本管理链路。

## 2. 本步骤落地内容

- 已新增系统设置主数据链路：
  - `SettingApplicationService`
  - `DefaultSettingApplicationService`
  - `SettingsCatalog`
  - `SystemSettingRepository`
  - `SystemSettingMapper`
  - `GET /api/settings`
  - `PUT /api/settings`
- 已冻结 `/api/settings` 白名单与配置来源语义：
  - 只允许 7 个白名单键进入 `system_setting` 与 `/api/settings`
  - `source` 只返回 `SYSTEM_SETTING` 或 `APPLICATION_CONFIG`
  - `myagent.runtime.default-timeout-seconds` 已作为废弃键直接拒绝
  - 非白名单键、类型不匹配、不可编辑项会返回明确错误
- 已完成启动配置键改造：
  - `11_code/backend/src/main/resources/application.yml`
  - 移除旧键 `myagent.runtime.default-timeout-seconds`
  - 改为：
    - `myagent.runtime.default-agent-timeout-seconds`
    - `myagent.runtime.default-llm-timeout-seconds`
    - `myagent.runtime.default-java-method-timeout-seconds`
    - `myagent.runtime.default-external-agent-timeout-seconds`
    - `myagent.runtime.default-max-steps`
    - `myagent.runtime.default-max-agent-call-depth`
- 已新增 Java 方法目录只读链路：
  - `JavaMethodApplicationService`
  - `JavaMethodRepository`
  - `JavaMethodMapper`
  - `GET /api/java-methods`
  - `GET /api/java-methods/{methodId}`
- 已新增工具目录只读链路：
  - `ToolApplicationService`
  - `ToolRepository`
  - `ToolMapper`
  - `GET /api/tools`
  - `GET /api/tools/{toolId}`
- 已新增外部 Agent 全链路：
  - `ExternalAgentApplicationService`
  - `ExternalAgentRepository`
  - `ExternalAgentMapper`
  - `ExternalAgentCommandJsonCodec`
  - `ExternalAgentTestExecutor`
  - `GET /api/external-agents`
  - `GET /api/external-agents/{adapterId}`
  - `POST /api/external-agents`
  - `PUT /api/external-agents/{adapterId}`
  - `PUT /api/external-agents/{adapterId}/secrets`
  - `PUT /api/external-agents/{adapterId}/status`
  - `POST /api/external-agents/{adapterId}/test`
- 已完成外部 Agent 敏感 header 语义收口：
  - 普通详情不回显敏感值，不返回掩码占位值
  - 普通更新接口不覆盖 secret
  - secret 只能走 `/api/external-agents/{adapterId}/secrets`
  - `/secrets` 支持覆盖写入、显式清空、未出现值保持不变
  - `POST /api/external-agents/{adapterId}/test` 会在真正外呼前检查 `secretConfigured`
- 已新增主数据模块 OpenAPI 契约：
  - `SettingsListApiResponse`
  - `JavaMethodPageApiResponse`
  - `JavaMethodDetailApiResponse`
  - `ToolPageApiResponse`
  - `ToolDetailApiResponse`
  - `ExternalAgentPageApiResponse`
  - `ExternalAgentDetailApiResponse`
  - `ExternalAgentTestApiResponse`
- 已重新生成前端 OpenAPI 类型：
  - `11_code/frontend/src/api/generated/schema.ts`

## 3. 设计边界说明

- 步骤 05 只完成 Java 方法和工具的主数据查询链路，不提前实现运行时 `JavaMethodRegistry`、`JavaMethodInvoker`、`ToolRegistry`、`ToolExecutor`。这些能力按步骤拆解保留在后续运行时步骤实现。
- 外部 Agent 的敏感 secret 没有采用“掩码回传保留旧值”协议，也没有做旧键兼容回退；当前实现只有一套正式语义。
- `CUSTOM_HTTP` 的敏感 header 内部存储与普通 header 已分离处理：
  - 普通 header 保存在 `commandJson.headers`
  - 敏感 header 定义和 secret 内部字段独立维护
  - 对外详情接口只暴露 `headerName` 和 `secretConfigured`

## 4. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run openapi:generate` 已通过。
- 前端 `npm run build` 已通过。
- 前端生成类型已包含新增主数据接口：
  - `/api/settings`
  - `/api/java-methods`
  - `/api/tools`
  - `/api/external-agents`
  - `/api/external-agents/{adapterId}/secrets`
  - `/api/external-agents/{adapterId}/test`

## 5. 环境说明

- 后端验证使用仓库内 `9_dependency\tools` 下的 JDK 21 与 Maven 3.9.11 完成。
- 导出 OpenAPI 时使用默认 profile 启动后端；当前步骤不依赖本地 PostgreSQL 即可完成契约导出和前端类型刷新。
- `PostgresMigrationTests` 仍因本机 Docker 环境不可用而跳过，不影响本步骤主数据链路结论。

## 6. 后续衔接

步骤 06 可以直接复用当前主数据目录与系统设置能力，为 Agent 基础信息管理、工作流草稿保存、历史版本查询和版本发布校验提供真实引用对象。


## 07-步骤06Agent管理与工作流版本管理链路执行记录-v1

# 步骤 06 Agent 管理与工作流版本管理链路执行记录 v1

## 1. 执行结论

步骤 06 已完成，可以进入步骤 07：运行内核基础组件。

## 2. 本步骤落地内容

- 已新增 Agent 主数据管理链路：
  - `AgentApplicationService`
  - `DefaultAgentApplicationService`
  - `AgentRepository`
  - `AgentMapper`
  - `GET /api/agents`
  - `POST /api/agents`
  - `PUT /api/agents/{agentId}`
  - `PUT /api/agents/{agentId}/status`
  - `GET /api/agents/{agentId}`
- 已新增工作流草稿与版本管理链路：
  - `WorkflowApplicationService`
  - `DefaultWorkflowApplicationService`
  - `WorkflowVersionRepository`
  - `WorkflowVersionMapper`
  - `WorkflowDraftValidationService`
  - `GET /api/agents/{agentId}/workflow-draft`
  - `PUT /api/agents/{agentId}/workflow-draft`
  - `POST /api/agents/{agentId}/workflow-draft/copy-from-version`
  - `POST /api/agents/{agentId}/workflow-draft/validate`
  - `POST /api/agents/{agentId}/workflow-draft/publish`
  - `GET /api/agents/{agentId}/workflow-versions`
  - `GET /api/agents/{agentId}/workflow-versions/{workflowVersionId}`
- 已完成工作流版本不可变语义落地：
  - 保存草稿时创建新的不可变 `DRAFT`
  - 旧当前草稿在同一事务内转为 `HISTORY`
  - 复制历史版本时通过显式命令接口创建新草稿，并写入 `sourceWorkflowVersionId`
  - 发布草稿时复制当前草稿生成新的不可变 `PUBLISHED`
  - 旧当前发布版本在同一事务内转为 `HISTORY`
  - 发布后当前草稿指针保持不变
- 已冻结 `runtimeOptions` 单一语义：
  - 后端稳定类型：`WorkflowRuntimeOptions`
  - 保存草稿时只接受 `timeoutSeconds`、`maxSteps`、`maxAgentCallDepth`
  - 缺失字段按 Agent 默认值和系统设置归一化补齐
  - 复制版本与发布版本时原样复制，不回头读取 Agent 当前默认值重算
- 已冻结 `referencedSchemaVersions` 单一语义：
  - 后端稳定类型：`ReferencedSchemaVersion`
  - 落库结构固定为对象数组 `{ schemaId, schemaKey, version }`
  - 保存草稿时由后端根据节点直接 Schema 引用自动派生
  - 派生结果会去重并按 `schemaKey`、`version` 稳定排序
- 已完成工作流草稿发布校验基础规则：
  - 单一 `START`
  - 至少一个 `END`
  - 非 `START` 节点必须有入边
  - 非 `END` 节点必须有出边
  - `CONDITION` 节点必须且只能有一条默认边
  - `START` / `END` / `JAVA_METHOD` / `TOOL` 的 SchemaRef 必填规则
  - `LLM` / `REVIEW` / `SUMMARY` 的提示词规则
  - `AGENT_CALL` 自调用、未发布、未启用规则
  - `JAVA_METHOD` / `TOOL` / `EXTERNAL_AGENT` 引用存在性与启用状态规则
- 已新增 Agent 与工作流 OpenAPI 具体响应契约：
  - `AgentPageApiResponse`
  - `AgentDetailApiResponse`
  - `WorkflowDraftApiResponse`
  - `WorkflowVersionPageApiResponse`
  - `WorkflowVersionDetailApiResponse`
  - `WorkflowValidationApiResponse`
  - `WorkflowPublishApiResponse`
- 已重新生成前端 OpenAPI 类型：
  - `11_code/frontend/src/api/generated/schema.ts`

## 3. 数据库与版本语义收口

- 已把 `11_code/backend/src/main/resources/db/migration/V1__create_core_tables.sql` 中的 `workflow_version.runtime_options_json` 改为应用层显式写入，不再依赖数据库默认 `{}`。
- `referenced_schema_versions_json` 仍保留数据库默认空数组 `[]` 作为防御，但正常写入路径均显式写入派生结果。
- 工作流版本持久化层已不再把 `runtimeOptions` 与 `referencedSchemaVersions` 当作动态 `Map<String, Object>` 传递。

## 4. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run openapi:generate` 已通过。
- 前端 `npm run build` 已通过。
- 新增前端生成类型已包含 Agent 与工作流版本管理接口。

## 5. 环境说明

- 后端验证使用仓库内 `9_dependency\tools` 下的 JDK 21 与 Maven 3.9.11 完成。
- 导出 OpenAPI 时临时启动本地后端服务，不依赖本地 PostgreSQL 即可完成契约刷新。
- `PostgresMigrationTests` 仍因当前机器 Docker 不可用而跳过，不影响本步骤主结论。

## 6. 后续衔接

步骤 07 可以直接复用当前 Agent、WorkflowVersion、Schema 快照和发布校验能力，继续实现运行时 `WorkflowValidator`、`WorkflowCompiler`、`TraceWriter` 与 `RuntimeLimitGuard`。


## 08-步骤07至17完整V1运行前端验收链路执行记录-v1

# 步骤 07 至 17 完整 V1 运行、前端与验收链路执行记录 v1

## 1. 执行结论

本轮已在步骤 06 之后继续推进运行内核、节点执行器、运行查询、前端页面与节点验收链路。

当前代码已具备完整 V1 主链路的工程实现基础，可以继续进入架构师复核和联调问题收口。

## 2. 本轮重点收口内容

- 运行内核已落地：
  - `WorkflowCompiler`
  - `WorkflowRuntimeEngine`
  - `MappingService`
  - `TraceWriter`
  - `RuntimeLimitGuard`
  - `WorkflowVersionSnapshot`
- 运行态 ID 语义已按文档实现：
  - `runId = agent_run.run_no`
  - `evalRunId = eval_run.run_no`
  - `nodeRunId = node_run.id`
- 节点执行器已覆盖 V1 节点集合：
  - `START`
  - `END`
  - `CONDITION`
  - `JAVA_METHOD`
  - `LLM`
  - `TOOL`
  - `AGENT_CALL`
  - `EXTERNAL_AGENT`
  - `REVIEW`
  - `SUMMARY`
- 本轮专门移除了两个运行时 mock/echo 缺口：
  - `AGENT_CALL` 已改为创建子 `AgentRun`、同步执行目标 Agent 当前发布版本、写入 `AgentMessage` 并传播子运行状态。
  - `EXTERNAL_AGENT` 已改为读取外部 Agent 目录、校验启用状态、执行 CLI/HTTP 适配器、按 `ExternalAgentResult` 语义输出结果。
- 外部 Agent 敏感 header 语义已延伸到正式运行：
  - 运行前调用 `assertSecretsConfigured`
  - 缺少 secret 时在真正外呼前失败
  - 不使用掩码值或隐式保留协议
- 外部 Agent 结果解析已按冻结口径收口：
  - 优先提取 `outputJson`
  - 提取不到结构化 JSON 时退化为文本摘要作为业务输出
  - `stdout` / `stderr` 只作为调试字段，不自动等于业务输出
- 模型网关已具备正式 OpenAI 调用路径：
  - 优先使用 Spring AI `OpenAiChatModel`
  - `outputSchemaRef` 存在时使用结构化输出模式
  - 结构化输出解析失败会作为独立中文错误进入节点失败链路
  - 无 OpenAI Bean 时保留网关级本地测试回退，保证无密钥环境可执行自动化测试
- 运行查询与 Trace 回看已落地：
  - `GET /api/runs`
  - `GET /api/runs/{runId}`
  - 运行详情返回 NodeRun、TraceEvent、父子运行入口和实际绑定的 `workflowVersionId`
- 节点验收后端链路已落地：
  - EvalSuite / EvalCase / EvalRun / EvalCaseResult
  - EvalRun 创建时同步创建 `AgentRun(runType=EVAL)`
  - Eval Trace 同时写入 `run_id` 和 `eval_run_id`
  - 断言执行顺序为 `outputSchema 校验 -> 确定性断言 -> 可选 LLM 评分`
- 前端页面已从占位入口推进到 V1 管理台入口：
  - Settings
  - Schemas
  - Methods
  - Tools
  - External Agents
  - Agents
  - Workflow
  - Runs
  - Evals
- 前端文案已移除步骤 01 骨架占位语义，改为完整 V1 控制台口径。

## 3. 关键文件

- `11_code/backend/src/main/java/com/myagent/runtime/executor/AgentCallNodeExecutor.java`
- `11_code/backend/src/main/java/com/myagent/runtime/executor/ExternalAgentNodeExecutor.java`
- `11_code/backend/src/main/java/com/myagent/externalagent/application/ExternalAgentTestExecutor.java`
- `11_code/backend/src/main/java/com/myagent/externalagent/application/ExternalAgentCommandJsonCodec.java`
- `11_code/backend/src/main/java/com/myagent/model/DefaultOpenAiModelGateway.java`
- `11_code/backend/src/test/java/com/myagent/runtime/executor/NodeExecutorRuntimeSemanticsTests.java`
- `11_code/frontend/src/app/layout/AppLayout.tsx`
- `11_code/frontend/src/features/home/pages/HomePage.tsx`

## 4. 当前仍需联调关注

- 当前机器 Docker 不可用，PostgreSQL Testcontainers 迁移测试仍跳过，需要在具备 Docker 的环境补跑。
- 前端 Vite 构建提示单包体积超过 500 kB，属于当前管理台依赖体积警告，不阻塞 V1 功能验收。
- OpenAI 真实调用路径依赖运行环境正确配置 Spring AI / OpenAI API Key；无密钥环境会走本地回退以保证测试稳定。

## 5. 后续衔接

下一步建议由架构师按完整 V1 文档复核以下重点：

- `AGENT_CALL` 子运行状态传播、父子运行详情回看是否满足最终展示口径。
- `EXTERNAL_AGENT` 的文本摘要回退是否满足所有适配器场景。
- 前端 Workflow 设计器目前以契约级 JSON 编辑器承接接口能力，是否需要在本轮继续提升到完整画布交互。


## 09-步骤12至17前端闭环与联调收口执行记录-v1

# 步骤 12 至 17 前端闭环与联调收口执行记录 v1

## 1. 执行范围

本轮按照 `8_change_list/agent-management-platform-v1/steps.md` 中步骤 12 至步骤 17，继续补齐完整 V1 前端页面、调试、运行详情、节点验收和测试收口能力。

## 2. 已完成内容

- 补齐前端 `domainApi.ts` REST 调用封装，覆盖 Agent、Schema、External Agent、Workflow、Debug、Run、EvalCase、EvalRun 等后续页面所需接口。
- `Schemas` 页面补齐创建、详情、编辑 DRAFT、基于旧版本创建新版本。
- `External Agents` 页面补齐创建、详情、编辑普通配置、启停、连接测试、敏感 Header 单独维护。
- `Agents` 页面补齐创建、编辑、启停、详情、当前草稿、当前发布版本、历史版本入口、工作流和调试跳转。
- `Workflow` 页面从契约级 JSON 编辑改为 React Flow 画布设计器，支持节点库、节点拖拽与连线、节点属性面板、运行约束、保存草稿、校验、发布、JSON 视图、历史版本只读打开、复制历史版本为草稿。
- 新增 `Debug` 页面，支持基于已持久化 WorkflowVersion 发起调试运行，并展示 `runId` 与实际绑定的 `workflowVersionId`。
- `Runs` 页面补齐 `runId` 深链、NodeRun 输入输出、Schema 校验结果、TraceEvent、父子运行跳转、历史版本回看、从 `node_run.id` 创建验收用例入口。
- `Evals` 页面补齐套件更新、用例创建/更新/确认/归档、运行详情、结果明细、失败摘要、历史对比、来源运行跳转。
- `Methods` 和 `Tools` 页面补齐详情抽屉。
- 新增前端测试基线，覆盖共享 JSON 组件和 `Settings` 页面白名单接口渲染。

## 3. 关键文件

- `11_code/frontend/src/api/domainApi.ts`
- `11_code/frontend/src/app/router.tsx`
- `11_code/frontend/src/features/schemas/pages/SchemasPage.tsx`
- `11_code/frontend/src/features/externalAgents/pages/ExternalAgentsPage.tsx`
- `11_code/frontend/src/features/agents/pages/AgentsPage.tsx`
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx`
- `11_code/frontend/src/features/workflow/pages/DebugPage.tsx`
- `11_code/frontend/src/features/runs/pages/RunsPage.tsx`
- `11_code/frontend/src/features/evals/pages/EvalsPage.tsx`
- `11_code/frontend/src/features/methods/pages/MethodsPage.tsx`
- `11_code/frontend/src/features/tools/pages/ToolsPage.tsx`
- `11_code/frontend/src/shared/components/JsonBlock.tsx`
- `11_code/frontend/src/shared/components/JsonTextArea.tsx`
- `11_code/frontend/vitest.config.ts`

## 4. 验证结果

- 前端 `npm test` 通过。
- 前端 `npm run build` 通过。
- 后端 `mvn -q test` 通过。
- 浏览器打开验证通过：首页和 Workflow 路由可加载；本地后端未启动时 Workflow 页面按预期展示接口请求失败状态。

## 5. 环境与遗留关注

- 前端构建仍有 Vite chunk 超过 500 kB 的警告，主要来自当前管理台依赖和 React Flow / Ant Design 打包体积，不阻塞 V1 功能验收。
- 前端测试运行时 jsdom 输出 `Window.getComputedStyle() with pseudo-elements` 未实现提示，测试退出码为 0，不影响当前断言结果。
- 当前机器 Docker 不可用，后端 Testcontainers PostgreSQL 用例继续按既有跳过策略处理；需要在具备 Docker 的环境补跑数据库容器集成测试。


## 10-步骤17测试联调与发布准备收口执行记录-v1

# 步骤 17 测试联调与发布准备收口执行记录 v1

## 1. 执行范围

本轮按照 `8_change_list/agent-management-platform-v1/steps.md` 的步骤 17 继续收口，范围包括：

- 前端专项测试补强。
- API 路径契约测试补强。
- 前端生产构建验证。
- 后端回归测试验证。
- 发布准备材料输出。
- 遗留环境项记录。

## 2. 本轮新增测试

- `WorkflowPage.test.tsx`
  - 验证工作流画布入口、节点库、保存草稿、校验、发布按钮存在。
  - 验证保存草稿时提交 `nodes`、`edges` 和版本运行约束。
  - 验证历史版本只读打开时不显示保存草稿入口。
- `DebugPage.test.tsx`
  - 验证调试运行提交到 `runDebugAgent`。
  - 验证调试运行摘要展示 `runId` 和实际绑定 `workflowVersionId`。
- `RunsPage.test.tsx`
  - 验证运行详情展示 `node_run.id`。
  - 验证“生成验收用例”入口传递 NodeRun 对象，不派生第二套 NodeRun 编号。
- `EvalsPage.test.tsx`
  - 验证 EvalRun 详情、结果明细、失败原因和来源 Run 展示。
- `domainApi.test.ts`
  - 验证关键路径不偏离正式 REST 契约。
  - 覆盖 `/workflow-draft/validate`、`/workflow-versions/{workflowVersionId}`、`/debug-runs`、`/node-runs/{nodeRunId}/eval-cases`、`GET /api/runs` 查询参数。

## 3. 本轮代码修正

- 修复 `WorkflowPage` 在未打开“运行约束”Tab 时保存草稿会提交空 `runtimeOptions` 的问题。
- `RunDetailView` 导出为可单测组件，便于稳定验证运行详情中的 NodeRun ID 语义。

## 4. 架构审查修复收口

本轮根据架构审查意见继续收口运行契约、Trace 契约和调试可信度：

- 运行引擎接入运行总超时和节点级超时，节点执行使用真实 `Future.get(timeout)` 包装阻塞调用。
- 节点 Schema 校验结果写入 NodeRun 和 `SCHEMA_VALIDATION` Trace，Schema 失败保留字段路径并透传到同步运行错误明细。
- LLM/REVIEW/SUMMARY 删除旧 `prompt` 业务口径，统一使用 `userPromptTemplate/systemPromptTemplate`。
- CONDITION 条件非法、缺路径、类型不匹配或操作符不支持时直接失败，不再落入默认分支。
- AgentRun 持久化 `error_code`，运行详情从持久化错误码返回。
- 模型类、Java 方法、工具和外部 Agent Trace 补齐完整结构化输入输出。
- 运行详情 `childRuns` 按冻结接口返回子运行摘要字段。
- OpenAI 客户端未配置时明确失败，移除隐藏模型成功回退。
- 默认启用 Flyway 迁移，测试配置单独关闭以保持本地单元测试隔离。

## 5. 架构复核专项追加修复

根据后续架构复核意见，本轮继续处理普通运行、调试运行、AGENT_CALL 与 Eval 运行之间的语义分叉：

- 抽出 `NodeExecutionRunner`，由普通运行和 Eval 共用节点执行协调逻辑，统一负责 NodeRun start/finish、节点超时、Schema 校验结果、`NODE_ERROR` Trace 与失败状态。
- Eval 节点验收执行时传入 `EvalCase.inputJson` 作为已解析节点输入，不再二次应用目标节点 `inputMapping`。
- 新增 `ActiveChildRunRegistry`，AGENT_CALL 节点执行期间登记活跃子运行，父节点超时或中断时级联把未完成子运行标记为 `CANCELED`。
- 对外 Trace DTO 从 `detailJson` 收口为 `detail`，`TraceEventResult.evalRunId` 返回 `eval_run.run_no` 字符串，不暴露内部数据库主键。
- REVIEW/SUMMARY 与 LLM 保持一致，在模型返回后立即写入 `MODEL_RESPONSE`，再执行 outputSchema 校验，避免 Schema 失败时丢失原始模型输出。
- 新增 `EvalScoreEvaluator`，非空 `scoreRule` 会调用模型执行辅助评分并写入 `scoreResult`；评分失败只记录失败信息，不覆盖确定性断言结论。
- 补齐 `WorkflowNodeDefinition` getter/setter 中文 Javadoc，并为 Eval 评分器新增单元测试覆盖空规则、正常评分和评分失败。

## 6. 已生成发布材料

- `13_release/01-V1发布前检查清单-v1.md`
- `13_release/02-V1部署说明-v1.md`
- `13_release/03-V1遗留问题清单-v1.md`

## 7. V1 整改专项修复

根据最终架构审核整改要求，本轮继续完成以下正式修复：

- 修复 `workflow_version.runtime_options_json` 和 `referenced_schema_versions_json` 的 JSONB 读回风险：`WorkflowRuntimeOptions`、`ReferencedSchemaVersion` 增加 Jackson 显式构造注解，JSON TypeHandler 使用带模块发现的 Jackson mapper，并补充 JSON 反序列化测试。
- 引入 `org.bsc.langgraph4j:langgraph4j-core`，运行引擎改为由 LangGraph4j `StateGraph` 编排节点流转，不再使用自研 `while (currentNode != null)` 作为正式调度路径。
- 保留 `NodeExecutionRunner` 作为节点内执行协调器，继续统一 NodeRun、Trace、Schema、timeout 和 AGENT_CALL 级联取消语义。
- 新增 `JavaMethodRegistry`、`JavaMethodInvoker` 和 `@RegisteredJavaMethod`，`JAVA_METHOD` 节点改为只通过显式注册目录和调用边界执行。
- 新增 `ToolRegistry`、`ToolExecutor`、`EchoToolExecutor`、`StaticJsonToolExecutor`，`TOOL` 节点不再硬编码 executorType 分支。
- REVIEW/SUMMARY 补齐 inputSchema 校验，与 LLM 节点保持一致。
- 节点超时执行改用 Spring 管理的受限 `ExecutorService`，不再每个节点创建独立 `newSingleThreadExecutor()`。

## 8. 当前验证状态

- 前端 `npm test -- --run` 已通过。
- 前端 `npm run build` 已通过。
- 后端 `mvn -q test` 已通过，Docker/Testcontainers 已实际启动 PostgreSQL 16 容器并执行 Flyway 迁移验证。
- 最终验证结果已记录到 `8_change_list/agent-management-platform-v1/test_result/11-步骤17测试联调与发布准备收口验证记录-v1.md`。

## 9. 遗留环境项

- 未配置真实 OpenAI API Key 的环境不能声明真实模型调用链路已完成验收，只能验证接口契约和模型未配置错误链路。



