# 架构单一真相强制落地 v1 验收步骤

## 1. 静态检索

在仓库根目录执行：

```powershell
cd D:\myproject\MyAgent
rg -n "AGENT_STUDIO_OPENAI" 0_specifications 3_product_design 4_arch_design 5_ui_design 6_schema_design 7_interface_design 11_code 13_release 14_user_manual README.md
rg -n "isDefault" 0_specifications 3_product_design 4_arch_design 5_ui_design 6_schema_design 7_interface_design 11_code 13_release 14_user_manual README.md
rg -n "persist-full-model-content" 0_specifications 3_product_design 4_arch_design 5_ui_design 6_schema_design 7_interface_design 11_code 13_release 14_user_manual README.md
```

预期：

- 当前正式代码和正式文档无命中。
- 允许历史变更包和历史验收记录保留命中，但不得作为当前正式依据。

## 2. OpenAPI 契约检查

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm run openapi:refresh
npm run openapi:check
```

预期：

- `schema.ts` 不包含 `isDefault`。
- OpenAPI 包含 `GET /api/platform-metadata`。
- OpenAPI 不包含 `AGENT_STUDIO_OPENAI_*` 示例或说明。

## 3. 后端自动化测试

```powershell
cd D:\myproject\MyAgent\11_code\backend
$env:JAVA_HOME='D:\myproject\MyAgent\9_dependency\tools\jdk21\jdk-21.0.11+10'
$env:Path='D:\myproject\MyAgent\9_dependency\tools\jdk21\jdk-21.0.11+10\bin;D:\myproject\MyAgent\9_dependency\tools\maven-3.9.11\apache-maven-3.9.11\bin;' + $env:Path
mvn clean test
```

验收报告必须记录：

- Tests run
- Failures
- Errors
- Skipped

预期：

- `Skipped=0`。
- Testcontainers 真实连接 Docker Desktop。
- 配置契约测试确认不再依赖 `AGENT_STUDIO_OPENAI_*`。

## 4. 前端自动化测试和构建

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm test -- --run
npm run build
```

预期：

- 测试通过。
- 构建通过。
- 工作流页面测试不再构造 `isDefault`。
- metadata 加载失败场景有明确错误展示测试。

## 5. Docker 空库 fresh install 验收

在正式 Docker 入口执行，清理旧 volume 后重建：

```powershell
cd D:\myproject\MyAgent\11_code
docker compose down -v
docker compose config
docker compose up -d --build
```

预期：

- 不设置任何 `AGENT_STUDIO_OPENAI_*` 时 `docker compose config` 成功。
- `postgres`、`api`、`web` 启动成功。
- `http://127.0.0.1:18080` 可访问。
- 后端健康检查为 UP。
- 数据库中 `model_provider` 和 `model_offering` 可以为空。
- 数据库中不存在 `openai-default` 或自动创建的默认供应项。

## 6. 页面配置供应商验收

使用正式 Web 入口：

1. 打开 `http://127.0.0.1:18080`。
2. 进入模型供应商页面。
3. 创建 OpenAI-compatible 供应商。
4. 通过密钥接口保存 API Key。
5. 创建模型供应项。
6. 执行供应商连接测试。

预期：

- 首次进入页面时空状态清晰。
- 用户可以通过页面完成供应商和供应项配置。
- 普通查询接口不回显 API Key。

## 7. 未配置模型时的失败验收

在空模型目录状态下：

1. 创建 Agent。
2. 创建含 LLM 节点的工作流。
3. 尝试发布或运行。
4. 尝试创建需要 judge 模型的 EvalSuite 或运行 Eval。

预期：

- 系统不在启动期失败。
- 到模型相关业务动作时失败。
- 错误为明确中文说明，提示配置模型供应商和模型供应项。
- 不出现默认模型、默认供应商或隐式回退。

## 8. 工作流默认边验收

1. 创建 CONDITION 节点。
2. 创建一条默认边和至少一条显式条件边。
3. 保存草稿。
4. 查询草稿详情。
5. 发布工作流。

预期：

- 默认边 JSON 只包含 `type="DEFAULT"` 作为默认语义。
- 请求、响应、数据库 JSON、OpenAPI、前端状态中不出现 `isDefault`。
- 多条默认边时发布校验失败并定位具体 CONDITION 节点。

## 9. Metadata 接口验收

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:18080/api/platform-metadata
```

预期：

- 返回 `nodeTypes`、`modelNodeTypes`、`edgeTypes`、`conditionOperators`、`conditionValueTypes`、`hardCheckTypes`。
- 每个选项包含 `value` 和中文 `label`。
- 前端工作流和 Eval 页面选项来自该接口。
- 人为让接口失败时，页面显示明确错误，不使用本地硬编码业务枚举兜底。

## 10. 结构化 UI 验收

通过正式 Web 入口验证：

- LLM/REVIEW/SUMMARY 节点核心配置可通过表单完成。
- CONDITION 边条件可通过表单完成。
- JAVA_METHOD、TOOL、AGENT_CALL、EXTERNAL_AGENT 可通过选择器完成核心配置。
- 普通用户不需要编辑高级 JSON 即可完成主流程。
- 高级 JSON 如保留，不绕过表单校验。

## 11. 文档一致性验收

检查以下文档：

- `0_specifications/develop_specification.md`
- `4_arch_design/`
- `6_schema_design/`
- `7_interface_design/`
- `11_code/README.md`
- `13_release/`
- `14_user_manual/`

预期：

- 文档统一表达“系统先启动，用户页面配置供应商”。
- 文档不再描述 fresh install 默认供应商。
- 文档不再描述 `isDefault`。
- 文档不再描述 `agent.studio.trace.persist-full-model-content`。
- 开发规范已调整 getter/setter Javadoc 规则。
