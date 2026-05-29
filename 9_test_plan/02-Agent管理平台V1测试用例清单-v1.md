# Agent 管理平台 V1 测试用例清单 v1

## 1. 文档目的

本文档将 `9_test_plan\01-Agent管理平台V1测试验收计划-v1.md` 拆解为可执行测试用例。测试人员执行验收时，应按本文档记录用例结果，并在 `12_test_result` 的验收报告中引用用例编号。

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

测试人员执行时，建议在 `12_test_result` 的报告中记录：

- 已执行用例编号。
- 每个用例的通过、不通过或未测状态。
- 失败用例关联的缺陷编号。
- 关键证据路径或截图说明。
- 未执行用例的原因。
- 回归测试覆盖范围和结果。
