# 工作流节点配置 UI 契约修复 v1 计划

## 1. 问题定性

当前实现不是后端数据层双轨真相，因为运行时只读取 `WorkflowNodeDefinition.config` 等现有字段。但它已经构成 UI 契约层面的双轨风险：

- 文档和后端执行器认为 LLM 类节点支持模型、温度和提示词模板。
- 页面没有显式表单，只把这些字段放在通用 JSON 文本框中。
- 用户通过页面无法直接理解“应该在哪里配置模型和温度”。

因此本次变更按 P1 UI 契约缺陷处理。

## 2. 总体方案

保留现有后端数据模型，以前端工作流节点属性面板为主要修改点：

1. 为每种节点类型建立前端节点配置表单模型。
2. 表单字段和 `WorkflowNodeDefinition` 做单向归一化转换，最终仍然保存为现有节点结构。
3. 对目录型引用使用后端已有目录接口加载选项，避免用户手写 key。
4. 对提示词、模型、温度等模型节点配置使用清晰表单字段。
5. 高级 JSON 视图作为本次正式交付入口保留，必须与表单共享同一份对象，不能形成独立状态。
6. CONDITION 边条件配置纳入本次 P1 范围，必须提供边级配置入口。
7. Agent 创建/编辑页模型相关字段文案纳入本次低风险同步修复。
8. Eval、External Agent 管理页和 `openapi:check` 脚本问题不纳入本次实现范围，后续拆分独立变更。

## 3. 节点配置范围

| 节点类型 | UI 必须显式支持的字段 | 数据落点 |
| --- | --- | --- |
| LLM | `userPromptTemplate`、`systemPromptTemplate`、`model`、`temperature` | `node.config` |
| REVIEW | `userPromptTemplate`、`systemPromptTemplate`、`model`、`temperature` | `node.config` |
| SUMMARY | `userPromptTemplate`、`systemPromptTemplate`、`model`、`temperature` | `node.config` |
| JAVA_METHOD | `methodKey` | `node.config.methodKey` |
| TOOL | `toolKey` | `node.config.toolKey` |
| AGENT_CALL | `targetAgentKey` | `node.config.targetAgentKey` |
| EXTERNAL_AGENT | `adapterKey`、`promptTemplate` | `node.config` |
| CONDITION | 默认边、显式条件边 `left/operator/valueType/right` | UI 新保存路径使用 `WorkflowEdgeDefinition.type/condition`；历史 `isDefault` 只读归一化 |
| START | 输入 Schema 引用 | `node.inputSchemaRef` |
| END | 输出 Schema 引用 | `node.outputSchemaRef` |
| JAVA_METHOD / TOOL | 输入 Schema 引用、输出 Schema 引用 | `node.inputSchemaRef`、`node.outputSchemaRef` |

## 4. 设计约束

- 供应商信息、Base URL 和 API Key 保持部署级配置，不进入节点表单。
- `model` 是自由文本，不做固定列表限制；可以提供默认值和 placeholder，但不能强制用户只能选平台已知模型。
- `temperature` 可为空；为空时继承 Agent 默认温度或模型供应商默认行为。有值时按后端当前约束限制在 `0` 到 `2`。
- `userPromptTemplate` 对 LLM/REVIEW/SUMMARY 是必填表单字段。
- `systemPromptTemplate` 可为空；为空时继承 Agent 默认系统提示词。
- 目录型字段必须从后端真实目录加载选项，不能在前端硬编码假数据。
- 目录型字段必须使用分页远程搜索选择器：默认 `page=1`、`pageSize=20`、按用户输入传 `keyword`，滚动或点击加载下一页；禁止只取第一页作为完整选项集，也禁止用超大 `pageSize` 假装全量加载。
- AGENT_CALL 目标 Agent 只允许选择 `status=ENABLED`、`currentPublishedWorkflowVersionId != null` 且 `agentKey` 不等于当前 Agent 的 Agent。
- AGENT_CALL 复用现有 Agent 列表接口时，请求侧使用 `status=ENABLED` 和分页/关键词参数；`currentPublishedWorkflowVersionId != null` 与排除当前 Agent 在前端选项层过滤，过滤后不能假定第一页就是完整可选集合。
- Schema 选择器不得比后端发布校验更严格；当前后端只校验 `schemaKey + version` 存在，因此 UI 可以展示 status/locked 作为提示，但不能仅因 status 或 locked 拒绝已存在 Schema 引用。
- 表单字段名、保存请求和 OpenAPI 类型必须保持一致，不手工维护第二套接口类型。

## 5. 风险控制

- 不改数据库结构，降低迁移风险。
- 不改后端运行时字段语义，避免影响已保存工作流。
- 对已有工作流草稿和历史版本，UI 打开时必须能从现有 JSON 配置中回填表单。
- 未知 `config` 字段必须保留。表单只覆盖已表单化字段，未知字段继续留在同一份 `config` 中，并可通过高级 JSON 查看/编辑；任何保存路径都不得静默丢弃未知字段。
- 已绑定但当前不可选的 `methodKey`、`toolKey`、`adapterKey`、`targetAgentKey` 或 Schema 引用必须回填为“当前绑定”占位项并保留原值；用户未主动修改该字段时，保存其他字段不得清空它。
- 保存前必须经过前端 JSON/表单归一化，避免表单值和高级 JSON 值互相覆盖产生双轨真相。

## 6. 验收重点

- 用页面真实创建一个含 START -> LLM -> END 的工作流，不手写隐藏字段名即可通过校验。
- LLM 节点配置 `model` 和 `temperature` 后，保存草稿请求体中的 `nodes[*].config` 必须包含对应字段。
- 重新打开草稿后，LLM 节点表单必须正确回填模型、温度和提示词模板。
- CONDITION 节点必须能通过 UI 配置一条默认边和至少一条显式条件边，且保存后边定义正确回填。
- CONDITION 默认边保存后必须只以 `edge.type="DEFAULT"` 表达；重新保存历史 `edge.isDefault=true` 数据时必须归一化为该写法。
- 目录选择器必须通过分页搜索找到非第一页的可选项，且当前绑定占位值必须显示中性提示并保留。
- 带未知 `config` 扩展字段的既有草稿，编辑表单字段并保存后未知字段仍然存在。
- 修改 Agent 默认模型后，未配置节点模型的 LLM 节点仍按后端回退规则运行，不在 UI 中制造第二套默认值解释。
- Docker 部署环境下仍由 `AGENT_STUDIO_OPENAI_BASE_URL` 和 `AGENT_STUDIO_OPENAI_API_KEY` 决定供应商连接信息。
