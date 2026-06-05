# 架构单一真相清理 v1 验收步骤

## 1. 静态清理验收

执行：

```powershell
rg -n "AGENT_STUDIO_OPENAI" D:\myproject\MyAgent
rg -n "isDefault" D:\myproject\MyAgent
rg -n "persist-full-model-content" D:\myproject\MyAgent
```

预期：

- 除本变更包和历史验收记录外，正式代码、正式文档、测试和发布说明中不再命中。
- 如果历史验收记录中保留命中，必须只出现在 `test_result` 历史记录里，并在最终验收报告中说明不属于当前正式真相。

执行：

```powershell
rg -n "assertionRules|judgeRules|isCritical|confirmationStatus" D:\myproject\MyAgent\3_product_design D:\myproject\MyAgent\4_arch_design D:\myproject\MyAgent\7_interface_design D:\myproject\MyAgent\14_user_manual
```

预期：

- 正式产品、架构、接口和用户文档中不再把旧字段作为 EvalCase 正式字段。

## 2. 后端测试

在后端目录执行：

```powershell
cd D:\myproject\MyAgent\11_code\backend
.\mvnw test
```

预期：

- 所有后端测试通过。
- 不存在依赖 `AGENT_STUDIO_OPENAI_*` 的配置契约测试。
- 不存在构造 `WorkflowEdgeDefinition.setIsDefault(...)` 的测试。
- 不存在通过 Spring Boot OpenAI starter 自动配置静态默认模型客户端的测试。

## 3. 前端测试

在前端目录执行：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm test -- --run
npm run build
```

预期：

- 前端测试通过。
- 工作流页面测试不再构造 `isDefault` 历史数据。
- 工作流页面通过 metadata 获得节点类型和条件枚举。
- 构建通过。

## 4. OpenAPI 契约验收

执行项目现有 OpenAPI 生成或校验命令。

预期：

- 生成的 `schema.ts` 不包含 `isDefault`。
- OpenAPI 包含 `GET /api/platform-metadata` 只读接口。
- EvalCase 请求和响应仍使用当前正式字段。

## 5. Fresh install 启动验收

清理本地 `AGENT_STUDIO_OPENAI_*` 环境变量后启动：

```powershell
Remove-Item Env:AGENT_STUDIO_OPENAI_API_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AGENT_STUDIO_OPENAI_BASE_URL -ErrorAction SilentlyContinue
Remove-Item Env:AGENT_STUDIO_OPENAI_DEFAULT_MODEL -ErrorAction SilentlyContinue
docker compose config
docker compose up -d --build
```

预期：

- `docker compose config` 成功。
- `docker compose up -d --build` 成功。
- 页面可访问。
- 数据库中不会因为缺少环境变量而自动创建默认模型供应商。

## 6. 模型调用失败语义验收

在没有模型供应商或没有可用模型供应项时，执行需要模型调用的流程。

预期：

- 发布校验或运行调用失败。
- 错误消息明确说明需要先配置模型供应商和模型供应项。
- 不出现默认 OpenAI、默认模型或隐式回退。

## 7. 工作流默认边验收

创建 CONDITION 节点和默认边，保存草稿并读取。

预期：

- 默认边 JSON 只包含 `type="DEFAULT"`。
- 显式条件边 JSON 包含 `type="CONDITION"` 和 `condition`。
- 响应、请求、数据库 JSON 和前端状态中都不出现 `isDefault`。

## 8. Metadata 接口验收

请求：

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:18080/api/platform-metadata
```

预期：

- 返回 `nodeTypes`、`modelNodeTypes`、`edgeTypes`、`conditionOperators`、`conditionValueTypes`、`evalAssertionTypes`。
- 每个枚举项包含 `value` 和中文 `label`。
- 前端页面使用该接口结果渲染选项。

## 9. 文档验收

检查以下文档已同步：

- `0_specifications/develop_specification.md`
- `3_product_design/`
- `4_arch_design/`
- `6_schema_design/`
- `7_interface_design/`
- `README.md`
- `11_code/README.md`
- `13_release/`
- `14_user_manual/`

预期：

- 不再存在本次清理对象的旧真相。
- getter/setter 不需要 Javadoc 的规则已经写入开发规范。
