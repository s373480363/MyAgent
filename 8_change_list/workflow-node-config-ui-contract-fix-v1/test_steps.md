# 工作流节点配置 UI 契约修复 v1 验收步骤

## 1. 静态检查

检查工作流页面不再把 LLM 类节点核心字段只放在通用 JSON 中：

```powershell
rg -n "userPromptTemplate|systemPromptTemplate|temperature|model" 11_code/frontend/src/features/workflow
```

预期：

- 能看到这些字段对应的显式表单处理逻辑。
- 不能只出现在 placeholder 或测试 mock 中。

检查供应商信息没有进入节点配置：

```powershell
rg -n "baseUrl|apiKey|provider|supplier|AGENT_STUDIO_OPENAI_BASE_URL|AGENT_STUDIO_OPENAI_API_KEY" 11_code/frontend/src/features/workflow
```

预期：

- 工作流节点表单中不出现 OpenAI Base URL、API Key 或供应商凭证配置入口。

## 2. 前端单元测试

运行：

```powershell
cd 11_code/frontend
npm test -- --run
```

预期：

- LLM 节点表单渲染测试通过。
- LLM 节点配置保存请求体测试通过。
- 已有节点配置回填测试通过。
- JAVA_METHOD、TOOL、AGENT_CALL、EXTERNAL_AGENT 目录型字段测试通过。
- 目录型选择器分页远程搜索测试通过，非第一页选项可以被加载并选择。
- 已绑定目录值和 Schema 引用的“当前绑定”占位回填、中性提示、未修改保存保留测试通过。
- 未知 `config` 字段保留测试通过。
- 高级 JSON 与表单共享同一份对象的测试通过。
- CONDITION 默认边和显式条件边配置测试通过。
- CONDITION 默认边保存为 `edge.type="DEFAULT"` 且 UI 新保存路径不写 `isDefault` 的测试通过。
- AGENT_CALL 选择器过滤当前 Agent、未启用 Agent、未发布 Agent 的测试通过。

## 3. 前端构建

运行：

```powershell
cd 11_code/frontend
npm run build
```

预期：

- TypeScript 编译通过。
- Vite 构建通过。

## 4. 页面真实路径验收

使用 Docker 正式入口或开发入口启动系统后，在浏览器执行：

1. 创建或打开一个 Agent。
2. 进入工作流设计器。
3. 添加 START、LLM、END 节点。
4. 选中 LLM 节点。
5. 在 UI 表单中直接填写：
   - 用户提示词模板
   - 系统提示词模板
   - 模型
   - 温度
6. 不手写隐藏 JSON 键名。
7. 保存草稿。
8. 重新打开该草稿。

预期：

- LLM 节点表单字段正确回填。
- JSON 视图中 `nodes[*].config` 包含 `userPromptTemplate`、`systemPromptTemplate`、`model`、`temperature`。
- 发布校验不因缺失 `userPromptTemplate` 失败。

## 5. 运行行为验收

使用配置后的草稿进行调试运行。

预期：

- Trace 的 MODEL_REQUEST 中记录的模型为节点配置的 `model`。
- 节点未配置 `model` 时，运行回退到 Agent 默认模型。
- 节点未配置 `systemPromptTemplate` 时，运行回退到 Agent 默认系统提示词。
- 节点未配置 `temperature` 时，不应被前端强行写入伪默认值。

## 6. CONDITION 页面路径验收

在浏览器执行：

1. 创建或打开一个 Agent。
2. 进入工作流设计器。
3. 添加 START、CONDITION、两个 END 节点。
4. 从 CONDITION 连接到两个 END。
5. 将其中一条边设置为默认分支。
6. 将另一条边设置为显式条件分支，填写 `left`、`operator`、`valueType`、`right`。
7. 保存草稿。
8. 重新打开草稿。
9. 执行发布校验。

预期：

- CONDITION 节点只有一条默认边。
- 默认边在保存后的边定义中使用 `type="DEFAULT"`，UI 新保存路径不写 `isDefault`。
- 显式条件边的 `condition` 正确保存和回填。
- 显式条件边在保存后的边定义中使用 `type="CONDITION"`，并包含 `condition`。
- 发布校验不因缺少默认边或缺少条件对象失败。
- 如果打开的是历史数据，`isDefault=true` 可以被正确识别为默认边；再次保存后归一化为 `type="DEFAULT"`。

## 7. 回归验收

检查非 LLM 节点：

- JAVA_METHOD 必须可以通过 UI 选择已注册方法。
- TOOL 必须可以通过 UI 选择已注册工具。
- AGENT_CALL 必须可以通过 UI 选择可调用 Agent，且不能选择当前 Agent 自己、未启用 Agent、没有当前发布版本的 Agent。
- EXTERNAL_AGENT 必须可以通过 UI 选择已启用外部 Agent 适配器。
- START/END/需要 Schema 的节点必须可以通过 UI 选择 Schema 引用。
- Schema 选择器展示 status/locked，但不得比后端发布校验额外拒绝已存在 Schema 引用。

## 8. 目录选择器分页与当前绑定占位验收

构造或准备目录数据，使目标 Java 方法、工具、Agent、外部 Agent 或 Schema 不在第一页结果中。

页面执行：

1. 打开对应节点的目录选择器。
2. 不调整 `pageSize` 为超大值。
3. 通过关键词搜索或加载下一页找到目标项。
4. 选择目标项并保存草稿。

预期：

- 请求使用分页参数，例如 `page=1&pageSize=20`，加载更多时请求下一页。
- 非第一页的有效选项可以被选择并保存为对应 key 或 `{schemaKey, version}`。
- AGENT_CALL 不展示当前 Agent 自己、未启用 Agent、没有当前发布版本的 Agent。
- AGENT_CALL 即使第一页过滤后没有足够选项，也仍然可以继续加载后续页或通过关键词搜索定位。

构造或打开一个已经绑定失效值的草稿，例如已停用的 `methodKey`、`toolKey`、`adapterKey`、未发布的 `targetAgentKey`，或列表当前搜索结果中不存在的 `{schemaKey, version}`。

预期：

- 页面回填为“当前绑定：xxx（未加载详情或当前结果未命中）”这类中性占位项。
- 只有当前端已有明确证据确认对象无效时，才允许显示失效告警。
- 用户未修改该字段时，保存其他字段不会清空原 key 或 Schema 引用。
- 用户主动修改该字段后，新值必须来自有效选项；必填字段被清空时必须阻止保存或给出明确字段错误。

## 9. 未知 config 与高级 JSON 保留验收

构造或打开一个包含未知 `config` 字段的节点，例如：

```json
{
  "userPromptTemplate": "请处理 {inputJson}",
  "xExperimentalField": "must_keep"
}
```

在 UI 中只修改已表单化字段并保存。

预期：

- 保存后的 `config.xExperimentalField` 仍然存在。
- 高级 JSON 视图和表单值来自同一份 `config`，不存在两套状态互相覆盖。
- 高级 JSON 视图是正式可见入口，不能在本次实现中移除。

## 10. Agent 默认值文案验收

检查 Agent 创建、编辑、详情页。

预期：

- 系统提示词字段文案表达为 LLM 节点默认系统提示词。
- 默认模型字段文案表达为 LLM 节点默认模型。
- 温度字段文案表达为 LLM 节点默认温度。
- 页面不暗示无 LLM 工作流也必须配置模型。

## 11. 文档验收

检查以下文档已经同步：

```powershell
rg -n "节点属性|节点配置|LLM|CONDITION|userPromptTemplate|model|temperature|methodKey|toolKey|targetAgentKey|adapterKey|condition|未知 config|分页远程搜索|当前绑定|isDefault|高级 JSON" 4_arch_design 7_interface_design 8_change_list/workflow-node-config-ui-contract-fix-v1
```

预期：

- 文档明确 UI 表单与后端节点配置契约一致。
- 文档明确供应商信息不进入节点配置。
- 文档没有把手写 JSON 作为普通用户配置核心节点能力的唯一方式。
- 文档明确 CONDITION 属于本次范围。
- 文档明确 CONDITION 默认边 UI 新保存路径只使用 `type="DEFAULT"`，历史 `isDefault` 只作为读取归一化输入。
- 文档明确未知 `config` 字段必须保留。
- 文档明确目录选择器必须分页远程搜索并保留当前绑定占位值。
- 文档明确高级 JSON 是本次正式保留入口。

## 12. 验收结论

验收完成后，在 `test_result` 下记录：

- 静态检查结果。
- 前端测试结果。
- 构建结果。
- 页面真实路径截图或文字记录。
- 是否仍存在需要后续独立变更处理的工作流 UI 问题。
