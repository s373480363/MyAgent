# 工作流节点配置 UI 契约修复 v1 完整验收问题清单

本文件只记录本轮完整验收已经确认的问题、证据、影响和开发处理预期。

验收时间：2026-06-03 至 2026-06-04。

## P1-01 LLM/REVIEW/SUMMARY 节点核心配置仍然只能通过节点配置 JSON 填写

### 证据

- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:499` 到 `512`：节点属性面板只提供输入 Schema 引用 JSON、输出 Schema 引用 JSON、输入映射 JSON、输出映射 JSON、节点配置 JSON。
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:646`：`userPromptTemplate`、`systemPromptTemplate`、`model`、`temperature` 只出现在 placeholder 示例中。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/LlmNodeExecutor.java:63` 和 `67`：LLM 节点实际读取 `config.model` 和 `config.temperature`。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/ReviewNodeExecutor.java:62` 和 `66`：REVIEW 节点实际读取 `config.model` 和 `config.temperature`。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/SummaryNodeExecutor.java:62` 和 `66`：SUMMARY 节点实际读取 `config.model` 和 `config.temperature`。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/AbstractNodeExecutorSupport.java:128` 和 `144`：模型类节点实际读取 `userPromptTemplate` 和 `systemPromptTemplate`。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:406` 到 `407`：发布校验要求提示词节点必须配置 `userPromptTemplate`。
- 浏览器页面实测：打开 `http://127.0.0.1:18080/agents/1/workflow`，添加并选中 LLM 节点后，可见标签只有“节点类型、节点名称、说明、节点超时、失败策略、输入 Schema 引用 JSON、输出 Schema 引用 JSON、输入映射 JSON、输出映射 JSON、节点配置 JSON”，没有模型、温度、用户提示词、系统提示词表单字段。
- 截图证据：`8_change_list/workflow-node-config-ui-contract-fix-v1/test_result/workflow-llm-node-generic-json.png`。

### 影响

这是 UI 契约层面的双轨真相：后端和文档认为节点支持模型、温度和提示词模板，页面却把这些能力隐藏在 JSON 中。用户不知道应该填写哪些键，发布校验失败后只能反查源码或文档，工作流创建流程无法自然推进。

### 开发处理预期

- LLM、REVIEW、SUMMARY 节点必须提供显式表单字段：用户提示词模板、系统提示词模板、模型、温度。
- 表单最终仍写入现有 `WorkflowNodeDefinition.config`，不得新增第二套持久化字段。
- `model` 必须是自由文本，不得做固定枚举限制。
- `temperature` 可以为空；有值时按后端约束限制在 `0` 到 `2`。
- 供应商 Base URL、API Key、供应商凭证不得进入节点表单。

## P1-02 CONDITION 分支条件没有可操作 UI，边条件契约无法配置

### 证据

- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:378` 到 `386`：`onConnect` 创建边时固定 `type: "NORMAL"`。
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:596`：缺省边定义仍回退为 `NORMAL`。
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:649`：CONDITION 节点 placeholder 提示“条件写在边 condition 中”，但页面没有边属性面板。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:292`：CONDITION 节点必须且只能有一条默认边。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:297`：显式分支必须配置条件对象。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:314` 到 `346`：后端校验 `left`、`operator`、`valueType`、`right`。
- 浏览器页面实测：添加并选中 CONDITION 节点后，属性面板仍只有通用节点 JSON 字段，提示条件写在边 `condition`，但没有任何边条件编辑入口。
- 截图证据：`8_change_list/workflow-node-config-ui-contract-fix-v1/test_result/workflow-condition-node-generic-json.png`。

### 影响

用户无法通过 UI 创建可发布的条件分支。后端要求边级条件对象，前端却只允许节点级 JSON 配置，导致工作流设计器对 CONDITION 节点基本不可用。

### 开发处理预期

- 增加边属性面板或 CONDITION 专用分支配置面板。
- 新增边时必须允许设置边类型：`NORMAL`、`DEFAULT` 或条件分支语义。
- CONDITION 节点必须支持配置且仅配置一条默认边。
- 非默认分支必须通过表单配置 `left`、`operator`、`valueType`、`right`，最终写入 `WorkflowEdgeDefinition.condition`。

## P1-03 目录型节点没有选择器，用户被迫手写内部 key

### 证据

- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:511`：目录型节点也只能通过“节点配置 JSON”填写。
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx` 当前只导入工作流草稿/版本相关 API，没有导入 `listJavaMethods`、`listTools`、`listAgents`、`listExternalAgents`、`listSchemas`。
- `11_code/frontend/src/api/domainApi.ts:66`、`216`、`272`、`290`、`308`：前端已有 Agent、Schema、Java 方法、工具、外部 Agent 列表接口，但工作流页面没有使用。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:420`：JAVA_METHOD 必须配置 `methodKey`。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:440`：TOOL 必须配置 `toolKey`。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:460`：EXTERNAL_AGENT 必须配置 `adapterKey`。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:481`：AGENT_CALL 必须配置 `targetAgentKey`。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/JavaMethodNodeExecutor.java:77`、`ToolNodeExecutor.java:67`、`ExternalAgentNodeExecutor.java:86`、`AgentCallNodeExecutor.java:128`：运行时实际读取这些 key。

### 影响

这是把后端目录事实强迫用户手写成 JSON 字符串。用户必须知道内部 key，且无法从 UI 判断 key 是否存在、是否启用、是否可调用，导致配置错误只能在发布校验或运行时暴露。

### 开发处理预期

- JAVA_METHOD 使用 Java 方法目录选择器，保存 `config.methodKey`。
- TOOL 使用工具目录选择器，保存 `config.toolKey`。
- AGENT_CALL 使用 Agent 选择器，保存 `config.targetAgentKey`，并过滤当前 Agent 自身和不可调用 Agent。
- EXTERNAL_AGENT 使用外部 Agent 适配器选择器，保存 `config.adapterKey`。
- 选择器展示名称和 key，保存时只写 key。

## P1-04 Schema 引用仍然要求手写 JSON，没有 Schema 选择器

### 证据

- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:414` 到 `415`：保存节点属性时从 `inputSchemaRefText`、`outputSchemaRefText` 解析 JSON。
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.tsx:499` 到 `503`：UI 标签是“输入 Schema 引用 JSON”和“输出 Schema 引用 JSON”。
- `11_code/backend/src/main/java/com/myagent/workflow/validation/DefaultWorkflowDraftValidationService.java:505` 到 `507`：START 输入 Schema、END 输出 Schema、JAVA_METHOD/TOOL 输入输出 Schema 是后端必需或强约束引用。
- `11_code/frontend/src/api/domainApi.ts:216`：前端已有 Schema 列表接口，但工作流页面没有使用。

### 影响

Schema 是系统内已有目录对象，工作流节点引用却要求用户手写 `{schemaKey, version}`。这会导致普通用户不知道可选 Schema、版本、状态、locked 状态，也无法避免引用错误版本。

### 开发处理预期

- START、END、JAVA_METHOD、TOOL 等需要 Schema 的节点必须提供 Schema 选择器。
- 选择器至少展示 `schemaKey`、`version`、`name`、`status`、locked 状态。
- 保存仍写入现有 `WorkflowSchemaRef`，不新增第二套字段。

## P2-05 Agent 创建/编辑页的模型相关字段语义不清，容易误导用户

### 证据

- `11_code/frontend/src/features/agents/pages/AgentsPage.tsx:197`：字段标签为“系统提示词”。
- `11_code/frontend/src/features/agents/pages/AgentsPage.tsx:200`：字段标签为“默认模型”。
- `11_code/frontend/src/features/agents/pages/AgentsPage.tsx:203`：字段标签为“温度”。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/LlmNodeExecutor.java:63`、`67`：这些 Agent 值实际是 LLM 节点未显式配置时的回退值。
- `11_code/backend/src/main/java/com/myagent/runtime/executor/AbstractNodeExecutorSupport.java:144`：`systemPromptTemplate` 为空时才回退 Agent 系统提示词。
- 浏览器页面实测：`/agents` 的创建弹窗只显示“系统提示词、默认模型、温度”，没有说明它们不是每个 Agent 都必须使用 LLM，也没有说明它们是模型类节点的默认回退值。
- 截图证据：`8_change_list/workflow-node-config-ui-contract-fix-v1/test_result/agent-create-model-default-labels.png`。

### 影响

用户会误以为每个 Agent 创建后必然需要和 LLM 交互，或者误以为这里配置的模型会覆盖每个 LLM 节点。实际架构是“Agent 级默认值 + 节点级显式覆盖”，但页面没有表达这个层级关系。

### 开发处理预期

- 标签应调整为能够表达回退语义的文案，例如“LLM 节点默认系统提示词”“LLM 节点默认模型”“LLM 节点默认温度”。
- 创建 Agent 时应明确这些字段不是无 LLM 工作流的必填条件。
- 工作流节点表单修复后，节点级模型/温度/提示词必须明确优先于 Agent 默认值。

## P2-06 评测用例的 LLM 评分规则仍然把模型、温度和提示词藏在 JSON 中

### 证据

- `11_code/frontend/src/features/evals/pages/EvalsPage.tsx:517`：评测用例表单只有“可选 LLM 评分规则 JSON”。
- `11_code/frontend/src/features/evals/pages/EvalsPage.tsx:603`：保存时直接 `parseOptionalJson(values.scoreRuleText)`。
- `11_code/backend/src/main/java/com/myagent/eval/application/DefaultEvalScoreEvaluator.java:60`：LLM 评分实际从 `scoreRule.model` 读取模型。
- `11_code/backend/src/main/java/com/myagent/eval/application/DefaultEvalScoreEvaluator.java:72`：LLM 评分实际从 `scoreRule.temperature` 读取温度。

### 影响

这与工作流 LLM 节点问题是同一类 UI 契约缺陷：后端支持模型和温度，但 UI 只给 JSON。评测是可选 LLM 评分，所以严重程度低于工作流节点，但仍会让用户不知道如何配置评分模型。

### 开发处理预期

- 评测用例应提供“启用 LLM 辅助评分”开关。
- 启用后提供评分提示词、模型、温度等显式字段。
- 字段仍写入现有 `scoreRule` JSON，不新增第二套后端字段。
- 模型仍为自由文本，不做固定枚举。

## P2-07 评测套件创建要求用户手填 Agent ID、WorkflowVersion ID 和节点 ID

### 证据

- `11_code/frontend/src/features/evals/pages/EvalsPage.tsx:229`：表单要求填写 `Agent ID`。
- `11_code/frontend/src/features/evals/pages/EvalsPage.tsx:232`：表单要求填写 `WorkflowVersion ID`。
- `11_code/frontend/src/features/evals/pages/EvalsPage.tsx:235`：表单要求填写 `节点 ID`。
- 浏览器页面实测：打开 `http://127.0.0.1:18080/evals`，点击“创建套件”，弹窗字段为 `Agent ID`、`WorkflowVersion ID`、`节点 ID`、套件名称、验收目标、通过率阈值。

### 影响

用户需要离开当前表单去查内部数字 ID 和节点 ID。对于评测套件这种业务对象，页面应该让用户选择 Agent、已发布/可评测版本和目标节点，而不是暴露数据库式 ID 输入。

### 开发处理预期

- Agent 使用选择器。
- WorkflowVersion 根据已选 Agent 加载版本选择器。
- 节点根据已选 WorkflowVersion 加载节点选择器，至少展示节点名称和节点类型。
- 保存请求仍提交后端需要的 `agentId`、`workflowVersionId` 和 `nodeId`，但 UI 不应要求用户手工查 ID。

## P2-08 外部 Agent 命令或 HTTP 配置只提供 JSON 输入

### 证据

- `11_code/frontend/src/features/externalAgents/pages/ExternalAgentsPage.tsx:231` 到 `237`：页面已经区分 `CODEX_CLI`、`OPENCODE_CLI`、`CUSTOM_CLI`、`CUSTOM_HTTP`。
- `11_code/frontend/src/features/externalAgents/pages/ExternalAgentsPage.tsx:248`：命令或 HTTP 配置仍然只有 `命令或 HTTP 配置 JSON`。
- `11_code/frontend/src/features/externalAgents/pages/ExternalAgentsPage.tsx:252`：敏感 Header 也要求填写 `创建时写入的敏感 Header JSON 数组`。
- `11_code/backend/src/main/java/com/myagent/externalagent/application/ExternalAgentCommandJsonCodec.java:271`、`281`、`291`：后端分别读取 HTTP method、url、bodyTemplate。
- `11_code/backend/src/main/java/com/myagent/externalagent/application/ExternalAgentCommandJsonCodec.java:233`、`243`、`261`：后端分别读取 CLI command、arguments、environment。
- `4_arch_design/09-Agent协作与外部Agent架构设计-v1.md:69` 到 `127`：架构文档已经固化第一批 CLI/HTTP 适配器命令参数和敏感 header 语义。
- 浏览器页面实测：打开 `http://127.0.0.1:18080/external-agents`，点击“创建外部 Agent”，弹窗在类型默认为“自定义 HTTP”时仍显示 `命令或 HTTP 配置 JSON` 和 `创建时写入的敏感 Header JSON 数组`。

### 影响

外部 Agent 的核心配置已经被后端结构化解释，但 UI 仍要求用户手写整段 JSON。用户必须知道不同 adapterType 对应的 JSON 结构、敏感 header 写入语义和 resultSource 格式，这会把正式管理页面退化成接口调试器。

### 开发处理预期

- 按 adapterType 渲染结构化表单。
- CUSTOM_HTTP 至少提供 method、url、普通 headers、敏感 headers、bodyTemplate、resultSource 表单。
- CUSTOM_CLI 至少提供 command、arguments、environment 表单。
- CODEX_CLI、OPENCODE_CLI 只暴露实际可配置项，不能要求用户手写无关 JSON。
- 高级 JSON 可以保留为调试入口，但不能作为唯一正式入口。

## P2-09 外部 Agent 输出 Schema 只支持手填数字 ID

### 证据

- `11_code/frontend/src/features/externalAgents/pages/ExternalAgentsPage.tsx:262`：输出 Schema 使用 `InputNumber`，标签是“输出 Schema ID”。
- `11_code/frontend/src/features/externalAgents/pages/ExternalAgentsPage.tsx:357` 和 `377`：保存时直接提交 `outputSchemaId`。
- 浏览器页面实测：外部 Agent 创建弹窗显示 `输出 Schema ID`，没有 Schema 选择器。

### 影响

外部 Agent 的输出 Schema 是可被目录化选择的对象，但 UI 要求用户手填内部 ID。这个问题不影响外部 Agent 的命令配置执行，但会降低 Schema 绑定的可用性，并增加错误引用风险。

### 开发处理预期

- 输出 Schema 应使用 Schema 选择器。
- 选择器展示 Schema Key、版本、名称、状态。
- 保存时仍提交后端当前需要的 ID 或按接口契约提交引用；不要新增第二套解释逻辑。

## P2-10 前端测试没有覆盖本次暴露的核心用户路径

### 证据

- `11_code/frontend/src/features/workflow/pages/WorkflowPage.test.tsx:75` 到 `89`：现有测试只校验页面渲染、保存按钮和基本保存请求。
- `11_code/frontend/src/features/workflow/pages/WorkflowPage.test.tsx:101` 到 `103`：只读版本测试只校验标题和按钮。
- 本轮实际运行 `npm test -- --run` 结果为 7 个测试文件、13 个测试全部通过，但 LLM 节点字段、目录选择器、Schema 选择器、CONDITION 边条件配置仍然缺失。

### 影响

测试通过不能证明工作流编辑器可用。当前测试没有覆盖“用户不手写 JSON 创建可发布工作流”的真实路径，因此无法拦截本次问题。

### 开发处理预期

- 增加 LLM/REVIEW/SUMMARY 节点字段渲染和保存请求体测试。
- 增加已有节点 `config` 回填到表单的测试。
- 增加 JAVA_METHOD、TOOL、AGENT_CALL、EXTERNAL_AGENT 选择器测试。
- 增加 Schema 选择器测试。
- 增加 CONDITION 默认边和条件分支表单测试。
- 至少增加一条真实页面路径测试：不手写隐藏 JSON 完成 START -> LLM -> END 草稿配置。

## P3-11 `openapi:check` 在不同步时会留下被刷新后的生成文件

### 证据

- `11_code/frontend/scripts/check-openapi-sync.ps1:12` 到 `13`：脚本先备份 OpenAPI 文件和生成类型文件。
- `11_code/frontend/scripts/check-openapi-sync.ps1:15`：脚本执行 `npm run openapi:refresh`。
- `11_code/frontend/scripts/check-openapi-sync.ps1:23` 到 `24`：不同步时直接 `exit 1`。
- `11_code/frontend/scripts/check-openapi-sync.ps1:26` 到 `27`：`finally` 只删除临时目录，没有恢复原文件。

### 影响

这个脚本名义上是检查，实际会在失败时修改工作区。开发或 CI 调用后，可能留下未预期的生成文件变更，影响问题定位。

### 开发处理预期

- `openapi:check` 应在任何退出路径恢复备份文件。
- 真正需要刷新生成物时继续使用 `openapi:refresh`。
- 检查脚本只报告不同步，不应改变最终工作区状态。

## 九项审核结论

1. 是否增加技术债务：是。P1-01 到 P1-04 是明确技术债，后端契约已经存在，UI 没有承载。
2. 是否增加不必要的项目复杂度：是。用户被迫理解 JSON 键、内部 key、内部 ID，复杂度转嫁给使用者。
3. 是否存在兼容、止血、最小影响等隐藏技术债务方案：是。把核心字段放在 placeholder 或高级 JSON 中，本质是止血式入口，不是正式 UI 契约。
4. 是否存在双轨真相：是。后端运行/校验契约和前端可见表单不一致。
5. 是否满足干净、一步到位：否。当前不能让用户通过正常表单完成可发布工作流。
6. 是否存在过度设计：本轮主要不是过度设计，而是通用 JSON 面板替代业务表单导致的设计不足。
7. 是否已经添加详细注释说明：代码注释不能替代表单契约。后续新增表单转换、回填、未知字段保留策略时需要补充必要注释。
8. 是否符合一开始设定的计划：不符合 `workflow-node-config-ui-contract-fix-v1` 已写明的目标；显式节点表单、选择器、条件分支 UI 都未实现。
9. 是否错误地把 LLM 返回结果当成固定结果：当前发现的问题不是严格验证 LLM 输出导致流程失败，而是 LLM 配置入口缺失。后续修复时必须继续避免固定模型枚举、固定供应商枚举、固定 LLM 输出格式验证；真实调用失败应由运行结果和 Trace 表达。
