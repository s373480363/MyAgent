# 工作流节点配置 UI 契约修复 v1 执行步骤

## 1. 梳理后端节点配置契约

1. 读取 `DefaultWorkflowDraftValidationService` 中各节点必填字段。
2. 读取各 `NodeExecutor` 中实际读取的 `config` 字段。
3. 形成前端节点配置表单字段清单。
4. 确认供应商信息、Base URL 和 API Key 不进入节点配置。

## 2. 调整工作流节点属性面板

1. 将当前单一 `NodePropertyPanel` 拆分为通用配置区和节点类型配置区。
2. 通用配置区保留：
   - 节点类型
   - 节点名称
   - 说明
   - 节点超时
   - 失败策略
3. 节点类型配置区按类型渲染：
   - LLM/REVIEW/SUMMARY 模型调用表单
   - JAVA_METHOD 方法选择表单
   - TOOL 工具选择表单
   - AGENT_CALL 目标 Agent 选择表单
   - EXTERNAL_AGENT 适配器选择和提示词模板表单
   - START/END/需要 Schema 的节点展示 Schema 引用选择入口
   - CONDITION 节点展示出边分支配置入口

## 3. 实现配置归一化

1. 从 `WorkflowNodeDefinition` 生成表单初始值。
2. 从表单值生成新的 `WorkflowNodeDefinition`。
3. 对空字符串做统一处理：
   - 可选字段空字符串不写入 `config`。
   - 必填字段空字符串阻止保存或给出明确错误。
4. 确保已存在的节点配置重新打开后能够回填。
5. 高级 JSON 必须保留，并与表单同步同一份 `config`。
6. 未知 `config` 字段必须从原始节点配置中保留，表单只覆盖已知字段，不能静默丢弃扩展字段。

## 4. 实现 CONDITION 边配置

1. 增加边选择状态或边属性面板。
2. 当选中源节点为 CONDITION 的出边时，展示默认分支和条件分支配置。
3. 保证每个 CONDITION 节点只能设置一条默认边。
4. 非默认条件边必须填写 `left`、`operator`、`valueType`、`right`，其中 `EXISTS` 可以不填 `right`。
5. 保存时默认边只写入 `WorkflowEdgeDefinition.type="DEFAULT"`，不写入节点 `config`，也不再写入 `isDefault`。
6. 保存时显式条件边写入 `WorkflowEdgeDefinition.type="CONDITION"` 和 `condition`，不写入节点 `config`，也不再写入 `isDefault`。
7. 重新打开草稿或历史版本时，若历史边满足 `type="DEFAULT"` 或 `isDefault=true` 任一条件，按默认边回填；再次保存时归一化为只使用 `type="DEFAULT"`。
8. 归一化后如果同一 CONDITION 节点存在多条默认边，阻止保存并提示具体边，不能自动丢弃或改写其中一条。

## 5. 接入目录数据

1. 为 Java 方法、工具、Agent、外部 Agent 和 Schema 选择器实现统一的分页远程搜索能力。
2. 首次打开选择器时使用 `page=1`、`pageSize=20` 请求现有列表接口。
3. 用户输入关键词时传 `keyword` 并从第一页重新加载；用户滚动到底部或点击加载更多时请求下一页。
4. Java 方法选择器请求或展示 `status=ENABLED` 的 `methodKey` 选项。
5. 工具选择器请求或展示 `status=ENABLED` 的 `toolKey` 选项。
6. Agent 选择器请求 `status=ENABLED` 的 Agent，再在前端过滤 `currentPublishedWorkflowVersionId != null` 且 `agentKey` 不等于当前 Agent；过滤后仍必须支持继续加载后续页和关键词搜索。
7. 外部 Agent 选择器请求或展示 `status=ENABLED` 的 `adapterKey` 选项。
8. Schema 选择器复用 Schema 列表接口加载 Schema 引用选项；展示 status/locked，但不得仅因 status/locked 拒绝后端当前允许的已存在 Schema 引用。
9. 打开已有草稿或历史版本时，如果已绑定值不在当前有效选项中，必须用原 key 或 `{schemaKey, version}` 合成“当前绑定”占位项并回填。
10. 用户未主动修改当前绑定占位字段时，保存其他字段必须保留原值；用户主动修改该字段后，新值必须来自有效选项，必填字段不能被清空保存。

## 6. 前端校验与错误提示

1. LLM/REVIEW/SUMMARY 的 `userPromptTemplate` 必填。
2. `temperature` 只允许空值或 `0` 到 `2`。
3. `model` 不做固定枚举限制。
4. 目录型字段在新建或主动变更时必须选择有效选项；历史当前绑定占位值允许原样保留，并以“未加载详情或当前结果未命中”为主的中性提示展示。
5. 保存草稿前发现前端配置错误时，用中文错误提示定位到字段。
6. CONDITION 边条件错误必须定位到具体边和字段。

## 7. 更新 Agent 默认值文案

1. 将 Agent 创建/编辑页“系统提示词”改为“LLM 节点默认系统提示词”。
2. 将“默认模型”改为“LLM 节点默认模型”。
3. 将“温度”改为“LLM 节点默认温度”。
4. 详情页保持同一口径。
5. 不改后端字段名和请求体字段名。

## 8. 更新测试

1. 增加 LLM 节点表单渲染测试。
2. 增加 LLM 节点模型和温度保存请求体测试。
3. 增加已有 `config` 回填到表单的测试。
4. 增加目录型节点选择器渲染和保存测试。
5. 增加目录型节点选择器分页远程搜索测试，覆盖非第一页选项可以被加载和选择。
6. 增加当前绑定目录值和 Schema 引用的回填、中性提示、未修改保存保留测试。
7. 增加工作流页面真实配置路径测试，覆盖不手写 JSON 完成 START -> LLM -> END 草稿配置。
8. 增加未知 `config` 字段保留测试。
9. 增加高级 JSON 与表单共享同一份对象的测试。
10. 增加 CONDITION 默认边和显式条件边配置、保存、回填测试，覆盖 UI 新保存路径不写 `isDefault`。
11. 增加 AGENT_CALL 选择器过滤当前 Agent、未启用 Agent、未发布 Agent 的测试。

## 9. 更新文档

1. 更新 `4_arch_design/03-前端架构设计-v1.md` 中工作流节点属性面板说明。
2. 更新 `4_arch_design/06-节点体系架构设计-v1.md` 中节点配置 UI 约束。
3. 如接口示例需要补充节点 `config` 示例，同步更新 `7_interface_design/02-对外REST接口-v1.md`。
4. 在本变更 `test_result` 中记录真实验收过程。

## 10. 禁止事项

- 禁止把 Base URL、API Key、供应商凭证加入节点表单。
- 禁止新增一套独立于 `WorkflowNodeDefinition.config` 的前端持久化字段。
- 禁止把 `model` 做成固定枚举必选。
- 禁止只补提示文案而不提供表单字段。
- 禁止继续要求普通用户通过手写 JSON 完成后端必填配置。
- 禁止把 CONDITION 边条件写入节点 `config`。
- 禁止静默丢弃未知 `config` 字段。
