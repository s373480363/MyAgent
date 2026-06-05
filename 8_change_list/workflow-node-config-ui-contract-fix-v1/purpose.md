# 工作流节点配置 UI 契约修复 v1 变更目的

## 1. 变更背景

当前工作流设计器已经具备 React Flow 画布、节点库、节点属性面板、草稿保存、校验和发布能力，但节点属性面板没有把后端已经定义并执行的核心节点配置以用户可理解的表单方式暴露出来。

最直接的问题是：LLM 节点运行时支持 `userPromptTemplate`、`systemPromptTemplate`、`model`、`temperature`，但页面只提供通用 `节点配置 JSON` 输入框。用户在创建工作流时无法通过明确表单配置模型和温度，只能依赖隐藏在 placeholder 中的 JSON 示例。

这不是单纯的样式问题，而是 UI 与后端执行契约不一致的问题。它会导致用户看到的“节点可配置能力”和系统实际执行能力不一致，形成用户可见的双轨真相风险。

## 2. 本次变更目标

本次变更要把工作流节点配置 UI 从“通用 JSON 编辑”升级为“按节点类型显式配置”，至少覆盖后端发布校验和运行执行已经明确依赖的核心字段。

必须达成：

- LLM、REVIEW、SUMMARY 节点必须在 UI 表单中显式配置用户提示词模板、系统提示词模板、模型和温度。
- JAVA_METHOD 节点必须在 UI 表单中通过已注册 Java 方法目录选择 `methodKey`。
- TOOL 节点必须在 UI 表单中通过已注册工具目录选择 `toolKey`。
- AGENT_CALL 节点必须在 UI 表单中通过可调用 Agent 列表选择 `targetAgentKey`，并排除当前 Agent 自身。
- EXTERNAL_AGENT 节点必须在 UI 表单中通过外部 Agent 适配器目录选择 `adapterKey`，并支持可选提示词模板。
- START、END、JAVA_METHOD、TOOL 等节点的必填 Schema 引用不能只依赖手写 JSON，至少要提供清晰的可选择入口。
- CONDITION 节点必须提供边级分支配置 UI，支持默认边和显式条件边，不能只提示用户在边 `condition` 中手写 JSON。
- CONDITION 默认边在 UI 新保存路径中只能使用 `edge.type="DEFAULT"` 表达；历史数据中的 `edge.isDefault=true` 只允许作为读取归一化输入，不能继续作为 UI 新写入的并行事实。
- 目录型选择器必须使用后端分页列表接口做远程搜索和分页加载，不能只取第一页，也不能用超大 `pageSize` 一次性拉全量数据。
- 已有草稿或历史版本中已经绑定但当前不可选的目录值或 Schema 引用，页面必须回填为“当前绑定”占位项并保留原值，不能在打开或保存时静默清空。
- Agent 创建/编辑页必须把系统提示词、默认模型、温度说明为 LLM 类节点的默认回退值，避免用户误解为所有 Agent 都必须调用 LLM。
- 所有表单字段最终仍然写入现有 `WorkflowNodeDefinition.config`、`inputSchemaRef`、`outputSchemaRef`、`inputMapping`、`outputMapping`，不得新增第二套持久化字段。
- 高级 JSON 视图必须保留为查看、调试和未知扩展字段编辑入口，但不能作为普通用户配置核心节点能力的唯一入口。

## 3. 本次不做的事情

- 不把 OpenAI-compatible Base URL、API Key、供应商凭证放入工作流节点配置。
- 不引入每节点不同供应商、多模型供应商注册表或多 OpenAI 客户端路由。
- 不改变后端工作流版本、节点定义、运行记录和 Trace 的持久化事实来源。
- 不把模型名称做固定枚举或严格格式校验，避免 OpenAI-compatible 模型名被误判为非法。
- 不用兼容旧字段 `prompt`、`promptTemplate` 作为新的成功路径；LLM 类节点继续使用 `userPromptTemplate` 和 `systemPromptTemplate`。
- 不在本次实现 Eval 评分规则表单、Eval 套件选择器、External Agent 结构化配置表单或 `openapi:check` 非破坏性改造；这些问题保留在审计清单中，必须拆分为后续独立变更处理。

## 4. 成功标准

- 用户在工作流设计器中选择 LLM/REVIEW/SUMMARY 节点时，可以直接看到并编辑模型、温度、用户提示词模板、系统提示词模板。
- 用户不需要阅读源码、接口文档或 placeholder，就能完成一个可发布、可调试运行的 LLM 节点配置。
- 用户可以通过 UI 配置 CONDITION 默认分支和显式条件分支，并通过发布校验。
- 后端发布校验要求的字段，在 UI 中都有明确入口，不再要求用户手写隐含 JSON 键名。
- UI 表单与后端执行仍然共享同一个节点定义对象，不新增双存储和双解释逻辑。
- 对 LLM 输出、模型名和 OpenAI-compatible 供应商不做过度严格校验；真实调用失败由运行结果和 Trace 表达。
- 新增或更新的前端测试必须覆盖节点类型切换、字段回填、保存草稿请求体、目录选择器分页搜索、当前绑定占位值保留和至少一个端到端页面配置路径。
