# 模型供应商目录与运行路由 v1 验收步骤

## 1. 静态检查

检查节点配置中不再把新写入模型保存为自由文本：

```powershell
rg -n "\"model\"|modelOfferingKey|defaultModelOfferingKey" 11_code/backend/src/main 11_code/frontend/src
```

预期：

- 新保存路径使用 `modelOfferingKey`。
- `config.model` 只允许出现在迁移、历史审计说明或拒绝旧字段的测试中。

检查前端和接口不暴露密钥：

```powershell
rg -n "apiKey|apiKeyCiphertext|secretValue|AGENT_STUDIO_OPENAI_API_KEY" 11_code/frontend/src 7_interface_design
```

预期：

- 前端请求可以包含只写密钥字段。
- 前端响应类型、列表、详情和 OpenAPI 示例不得返回 API Key 明文。

检查 MODEL_REQUEST Trace 不直接序列化模型请求或内部解析对象：

```powershell
rg -n "valueToTree\\(request\\)|valueToTree\\(resolved|ResolvedModelRoute|apiKeyCiphertext|baseUrl" 11_code/backend/src/main/java/com/myagent/runtime 11_code/backend/src/main/java/com/myagent/model
```

预期：

- `valueToTree(request)` 不得用于 MODEL_REQUEST Trace。
- `ResolvedModelRoute` 只能出现在模型模块内部解析和调用逻辑中，不得出现在 Trace 写入代码中。
- Trace payload 不包含 `apiKeyCiphertext`、解密 API Key 或 Base URL。

检查 Docker 正式部署密钥变量：

```powershell
Remove-Item Env:AGENT_STUDIO_SECRET_KEY -ErrorAction SilentlyContinue
$env:AGENT_STUDIO_POSTGRES_PASSWORD='present'
$env:AGENT_STUDIO_OPENAI_API_KEY='present'
docker compose config
```

预期：

- 命令失败，并明确提示 `AGENT_STUDIO_SECRET_KEY is required`。
- `compose.yaml` 不允许给 `AGENT_STUDIO_SECRET_KEY` 配置默认值。

## 2. 后端测试

运行：

```powershell
cd 11_code/backend
mvn test
```

预期覆盖：

- 创建供应商。
- 更新供应商非敏感字段不改变密钥。
- 单独替换和清空 API Key。
- 供应商列表和详情不返回密钥明文。
- 创建模型供应项。
- 启停供应商和供应项。
- 发布校验拒绝不存在、停用或未配置密钥的模型供应项。
- LLM、REVIEW、SUMMARY、Eval 评分都按模型供应项路由。
- MODEL_REQUEST Trace 不包含 API Key、`apiKeyCiphertext`、Base URL 或内部解析对象。
- 供应商连接测试存在专用后端覆盖，至少验证成功、超时中断、中文错误摘要和不泄露敏感信息。

## 3. 前端测试

运行：

```powershell
cd 11_code/frontend
npm test -- --run
```

预期覆盖：

- 模型供应商列表、创建、编辑、启停。
- API Key 输入只在创建或密钥更新表单出现，详情不回显明文。
- 模型供应项列表、创建、编辑、启停。
- Agent 默认模型供应项选择器分页搜索、保存和清空。
- Workflow LLM 类节点模型供应项选择器分页搜索、保存和当前绑定保留。
- Eval 评分规则模型供应项选择器分页搜索、保存和当前绑定保留。

## 4. 构建检查

运行：

```powershell
cd 11_code/frontend
npm run build
```

预期：

- TypeScript 编译通过。
- Vite 构建通过。

后端构建：

```powershell
cd 11_code/backend
mvn package
```

预期：

- 编译、测试和打包通过。

## 5. 数据库迁移验收

准备旧数据：

- `AGENT_STUDIO_OPENAI_BASE_URL`
- `AGENT_STUDIO_OPENAI_API_KEY`
- `AGENT_STUDIO_OPENAI_DEFAULT_MODEL`
- `agent_definition.default_model`
- 工作流节点 `config.model`

执行迁移后检查：

```sql
select provider_key, provider_type, base_url, status from model_provider;
select offering_key, provider_key, model_key, upstream_model_name, status from model_offering;
select agent_key, default_model_offering_key from agent_definition;
```

预期：

- 默认供应商创建成功。
- 默认模型供应项创建成功。
- Agent 默认模型已迁移到 `default_model_offering_key`；没有历史默认模型的 Agent 保持为空。
- 工作流节点 `config.model` 已迁移到 `config.modelOfferingKey`。
- Eval `scoreRule.model` 已迁移到 `scoreRule.modelOfferingKey`。
- 迁移后运行表中不再保留 `config.model` 或 `scoreRule.model` 作为审计字段。

## 6. API 验收

执行：

```powershell
Invoke-RestMethod -Uri 'http://127.0.0.1:18080/api/model-providers'
Invoke-RestMethod -Uri 'http://127.0.0.1:18080/api/model-offerings'
Invoke-RestMethod -Uri 'http://127.0.0.1:18080/api/model-offerings/by-keys?offeringKeys=openai.gpt_4_1_mini'
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:18080/api/model-providers/1/test' -ContentType 'application/json' -Body '{"offeringKey":"openai.gpt_4_1_mini","prompt":"ping"}'
```

预期：

- 列表返回分页结构。
- 供应商响应只包含 `apiKeyConfigured` 或 `apiKeyMask`，不包含明文 API Key。
- 按 key 查询能返回已绑定供应项详情，即使该供应项或供应商当前不可选。
- 供应商连接测试必须使用请求体中的 `offeringKey`，响应不包含 API Key、Base URL、上游完整鉴权报文或模型原始输出全文。
- 供应商连接测试必须在 `agent.studio.runtime.default-llm-timeout-seconds` 对应窗口内返回；超时场景返回中文业务错误，不能长时间挂起。

创建供应商：

```json
{
  "providerKey": "siliconflow",
  "name": "硅基流动",
  "providerType": "OPENAI_COMPATIBLE",
  "baseUrl": "https://api.siliconflow.cn",
  "apiKey": "只写密钥"
}
```

预期：

- 创建成功。
- 返回体不包含 `apiKey` 明文。

## 7. 页面验收

在正式入口打开：

```text
http://127.0.0.1:18080
```

验收路径：

1. 进入模型供应商页面。
2. 创建一个 OpenAI-compatible 供应商。
3. 创建一个模型供应项。
4. 创建或编辑 Agent，选择默认模型供应项。
5. 进入工作流设计器，选中 LLM 节点，选择模型供应项。
6. 保存草稿并重新打开。
7. 发布校验。

预期：

- 供应商密钥不回显。
- Agent 默认模型供应项正确回填。
- LLM 节点模型供应项正确回填。
- 保存草稿请求体中包含 `modelOfferingKey`。
- 发布校验通过。

补充路径：

- 创建不含 LLM、REVIEW、SUMMARY 的工作流时，Agent 默认模型供应项允许为空，发布不因模型缺失失败。
- 创建含 LLM 类节点且节点未配置模型供应项、Agent 默认模型供应项也为空的工作流时，发布失败并定位到具体节点。

## 8. 运行验收

执行一个包含 LLM 节点的调试运行。

预期：

- MODEL_REQUEST Trace 包含 `providerKey`、`modelOfferingKey`、`upstreamModelName`。
- Trace 不包含 API Key、`apiKeyCiphertext`、Base URL 或 `ResolvedModelRoute`。
- 更换节点模型供应项后，Trace 反映新的供应项。
- 停用供应项后再次运行失败，并返回清晰中文错误，不自动换成默认模型。
- Eval LLM 评分开启后使用 `scoreRule.modelOfferingKey` 或 Agent 默认模型供应项，不读取 `scoreRule.model`。

## 8.1 供应商连接测试超时复验

执行一个可稳定触发上游超时或黑洞地址的供应商连接测试。

预期：

- 接口在正式 LLM 默认超时窗口内返回，不无限等待。
- 返回中文业务错误，不暴露 API Key、Base URL、完整上游错误报文或底层客户端实现细节。
- 失败只影响当前测试请求，不写入第二套运行事实，也不改变供应商或供应项配置。

## 9. 文档验收

执行：

```powershell
rg -n "OpenAI 官方 API|agent.studio.openai.default-model|defaultModel|config.model|scoreRule.model|model_definition|ModelDefinition|modelOfferingKey|ModelProvider|ModelOffering|模型供应商|模型供应项" 3_product_design 4_arch_design 6_schema_design 7_interface_design 8_change_list/model-provider-catalog-routing-v1
```

预期：

- 永久文档不再把“单 OpenAI 官方 API + 自由文本模型名”作为最终事实。
- `agent.studio.openai.default-model` 只作为迁移来源或历史配置说明出现。
- 新节点、Agent 和接口文档都使用 `modelOfferingKey`。
- `ModelDefinition` / `model_definition` 不再作为 V1 正式对象出现。

## 10. 架构自检

验收时逐条确认：

- 是否增加技术债务。
- 是否增加不必要复杂度。
- 是否存在兼容、止血、最小影响等隐藏技术债务。
- 是否存在双轨真相。
- 是否满足干净、一步到位。
- 是否过度设计。
- 是否添加详细注释说明。
- 是否符合本变更计划。
- 是否错误地把 LLM 返回、模型名或供应商响应当作固定结果。
