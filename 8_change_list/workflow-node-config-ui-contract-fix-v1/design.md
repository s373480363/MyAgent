# 工作流节点配置 UI 契约修复 v1 设计

## 1. 设计结论

工作流节点属性面板必须以“节点类型”为单位呈现业务配置表单。通用 JSON 编辑器不能再作为核心节点配置的唯一入口。

本次设计保持现有后端节点定义不变：

```json
{
  "nodeId": "llm-1",
  "type": "LLM",
  "inputSchemaRef": {},
  "outputSchemaRef": {},
  "inputMapping": {},
  "outputMapping": {},
  "timeoutSeconds": 120,
  "failurePolicy": "FAIL_FAST",
  "config": {}
}
```

UI 表单只是这份结构的可视化编辑器，不新增并行数据模型。

## 2. LLM 类节点设计

LLM、REVIEW、SUMMARY 使用同一组模型调用配置：

| UI 字段 | 是否必填 | 写入字段 | 说明 |
| --- | --- | --- | --- |
| 用户提示词模板 | 是 | `config.userPromptTemplate` | 支持 `{inputJson}`、`{agentKey}`、`{nodeId}` |
| 系统提示词模板 | 否 | `config.systemPromptTemplate` | 为空时继承 Agent 默认系统提示词 |
| 模型 | 否 | `config.model` | 为空时继承 Agent 默认模型 |
| 温度 | 否 | `config.temperature` | 为空时继承 Agent 默认温度 |

模型字段使用普通输入框，不使用固定枚举。原因是当前部署支持 OpenAI-compatible API，模型名称不是平台能静态穷举的固定结果。

温度字段使用数字输入，前端仅做基础范围约束，不能把空值转换成 `0` 或其他伪默认值。

## 3. 目录型节点设计

目录型节点不得要求用户手写 key：

| 节点类型 | UI 控件 | 选项来源 |
| --- | --- | --- |
| JAVA_METHOD | Java 方法选择器 | Java 方法目录接口 |
| TOOL | 工具选择器 | 工具目录接口 |
| AGENT_CALL | 目标 Agent 选择器 | Agent 列表接口，过滤当前 Agent 和不可调用 Agent |
| EXTERNAL_AGENT | 外部 Agent 适配器选择器 | 外部 Agent 目录接口 |

选择器展示名称和 key，保存时只写入 key，保持后端契约稳定。

目录选择器过滤规则必须和后端发布校验一致：

- JAVA_METHOD：只请求或展示 `status=ENABLED` 的 Java 方法。
- TOOL：只请求或展示 `status=ENABLED` 的工具。
- EXTERNAL_AGENT：只请求或展示 `status=ENABLED` 的外部 Agent 适配器。
- AGENT_CALL：只展示 `status=ENABLED`、`currentPublishedWorkflowVersionId != null`，且 `agentKey` 不等于当前 Agent 的 Agent。

如果后端校验规则后续变化，前端选择器过滤规则必须同步改动，不能形成另一套“可选项”事实。

目录选择器加载契约：

- 选择器必须使用现有分页列表接口做远程搜索，不允许只取第一页后当作完整目录，也不允许把 `pageSize` 调成超大值规避分页。
- 首次打开下拉时请求 `page=1`、`pageSize=20`；用户输入关键词时传 `keyword` 并重新从第一页加载；用户滚动到底部或点击加载更多时请求下一页。
- Java 方法、工具、外部 Agent 可以在请求中传 `status=ENABLED`；AGENT_CALL 可以在请求中传 `status=ENABLED`，但 `currentPublishedWorkflowVersionId != null` 和排除当前 Agent 必须在前端选项层过滤，因为现有 Agent 列表接口没有对应查询参数。
- AGENT_CALL 过滤后如果当前页可展示项不足，不能认为没有更多可选项；只要接口返回的 `total` 和当前页信息表明仍有后续页，就必须允许继续加载或通过关键词搜索定位。
- 选择器可以缓存同一类目录在相同 `keyword` 和过滤条件下的结果，但缓存只用于减少请求，不能改变分页、搜索和过滤语义。

当前绑定占位值规则：

- 打开已有草稿或历史版本时，如果当前 `methodKey`、`toolKey`、`adapterKey`、`targetAgentKey` 不在当前可选结果中，UI 必须根据配置中已有 key 合成一个“当前绑定”占位项并回填。
- 在没有按 key 详情接口的情况下，前端不能把“未加载详情”或“当前分页/搜索未命中”直接表述为失效；占位文案应保持中性，例如“当前绑定：xxx（未加载详情或当前结果未命中）”。
- 只有当前端已经有明确证据确认对象未启用、未发布或不存在时，才允许把该占位项标记为失效；本次不为此新增临时后端接口。
- 用户未主动修改该选择器时，保存其他字段必须保留原 key；不能因为当前可选列表没有它就清空字段。
- 用户主动修改该选择器后，新值必须来自当前有效选项；必填字段被主动清空时应阻止保存草稿或给出明确字段错误。

## 4. Schema 引用设计

Schema 引用不能只靠手写 JSON。至少应提供 Schema 选择器，展示：

- `schemaKey`
- `version`
- `name`
- `status`
- 是否 locked

保存时仍写入 `WorkflowSchemaRef`：

```json
{
  "schemaKey": "input",
  "version": 1
}
```

复杂输入映射和输出映射可以继续保留 JSON 编辑，但应配合更清晰的标签、校验错误定位和示例，不再把 Schema 引用本身也完全交给用户手写。

Schema 选择器过滤规则：

- 当前后端发布校验只要求 `schemaKey + version` 存在。
- 因此前端不得仅因 `status` 或 locked 状态拒绝一个已存在 Schema 引用。
- `status` 和 locked 必须展示给用户，用于理解风险；如果产品要禁止引用 ARCHIVED 或未激活 Schema，必须同步修改后端发布校验，不能只在前端拦截。
- 已绑定的 `{schemaKey, version}` 如果没有出现在当前分页或搜索结果中，UI 必须用该引用本身合成“当前绑定”占位项并保留；是否真实存在由后端发布校验给出最终结论，前端不得在打开或保存草稿时静默删除。

## 5. CONDITION 边配置设计

CONDITION 的条件不属于节点 `config`，而属于出边 `WorkflowEdgeDefinition`。

UI 必须提供边级配置入口，至少支持：

| UI 字段 | 写入字段 | 说明 |
| --- | --- | --- |
| 是否默认分支 | `edge.type="DEFAULT"` | 每个 CONDITION 节点必须且只能有一条默认边 |
| 左值 JSONPath | `edge.condition.left` | 非默认边必填 |
| 操作符 | `edge.condition.operator` | 使用后端已支持的操作符 |
| 右值类型 | `edge.condition.valueType` | `STRING`、`NUMBER`、`BOOLEAN`、`JSON` |
| 右值 | `edge.condition.right` | 除 `EXISTS` 外必填 |

新增边时可以默认创建 `NORMAL` 边，但当源节点是 CONDITION 时，用户必须能把边改为默认边或显式条件边。保存和重新打开草稿后，边条件必须正确回填。

默认边单一写法：

- UI 新建或编辑保存 CONDITION 默认边时，只写 `edge.type="DEFAULT"`，不再写入 `edge.isDefault=true`。
- UI 新建或编辑保存 CONDITION 显式条件边时，写 `edge.type="CONDITION"` 和 `edge.condition`，不写 `edge.isDefault`。
- 历史数据读取时，如果 `edge.type="DEFAULT"` 或 `edge.isDefault=true` 任一条件成立，UI 按后端当前运行语义把该边展示为默认边。
- 保存历史数据时必须归一化为新写法：默认边只保留 `type="DEFAULT"`，显式条件边只保留 `type="CONDITION"` 和 `condition`，删除 UI 负责范围内的 `isDefault`。
- 归一化后如果同一个 CONDITION 节点出现多条默认边，UI 必须阻止保存并定位到具体出边，不能用前端自行选择其中一条的方式掩盖冲突。

## 6. 高级 JSON 设计

高级 JSON 视图是本次正式交付入口，必须保留，并满足：

- 高级 JSON 和表单共享同一个 `WorkflowNodeDefinition`。
- 切换节点或保存前必须统一归一化，不能出现表单显示值和 JSON 保存值不一致。
- 对 LLM 类节点，表单字段优先作为用户入口；高级 JSON 仅用于查看或编辑暂未表单化的扩展字段。
- 未知 `config` 字段必须保留。表单字段只覆盖自己负责的已知 key，不能重建一个只包含表单字段的新 `config` 后覆盖原对象。
- 可选字段从有值变为空值时，只删除该字段自己的 key，不得影响未知字段。
- 节点类型变化时也不得静默丢弃未知字段；如果需要清理不兼容字段，必须给用户明确确认。

## 7. Agent 默认值文案设计

Agent 创建/编辑页中的系统提示词、默认模型、温度不是所有工作流的必填能力，而是 LLM/REVIEW/SUMMARY 节点未显式配置时的默认回退值。

UI 文案应调整为：

- `LLM 节点默认系统提示词`
- `LLM 节点默认模型`
- `LLM 节点默认温度`

详情页展示也使用同一口径。该调整只改变 UI 语义表达，不改变后端字段名。

## 8. 不变量

- 运行时事实来源仍是持久化后的 `workflow_version.nodes_json`。
- 正式运行和调试运行仍然绑定具体 `workflow_version.id`。
- Agent 默认模型、系统提示词和温度仍是节点缺省值来源，不直接覆盖已持久化节点显式配置。
- OpenAI Base URL、API Key 和供应商凭证仍属于部署级配置，不通过工作流节点、系统设置页或运行接口暴露。

## 9. 为什么必须这样设计

当前错误的根源不是后端不能运行，而是 UI 没有承载用户必须理解的业务配置。把模型、温度、提示词模板藏在 JSON 中会导致：

- 用户不知道 LLM 节点到底支持哪些配置。
- 发布校验失败后用户只能反查文档或源码。
- Agent 默认模型和节点覆盖模型的层级关系无法被页面表达。
- 架构文档说“按节点类型展示配置项”，实际页面却只提供通用 JSON，形成可见契约不一致。

因此本次必须一步到位修成按节点类型的显式表单，而不是只在 JSON 输入框旁边补一段说明文字。
