# 步骤 06 Agent 管理与工作流版本管理链路验证记录 v1

## 1. 验证范围

本记录对应 `10_plan_process/02-V1开发执行步骤拆解-v1.md` 中的步骤 06：Agent 管理与工作流版本管理链路。

## 2. 已完成内容

- 后端已实现 Agent 列表、创建、更新、启停和详情接口。
- 后端已实现工作流草稿获取、保存、校验、发布、历史版本列表和版本详情接口。
- Agent 创建时会自动初始化首个不可变草稿版本。
- 保存草稿、复制草稿、发布草稿三条事务链路已全部落地：
  - 新版本创建
  - 旧当前版本转 `HISTORY`
  - Agent 指针更新
- `runtimeOptions` 已统一为完整三字段对象：
  - `timeoutSeconds`
  - `maxSteps`
  - `maxAgentCallDepth`
- `referencedSchemaVersions` 已统一为固定对象数组：
  - `schemaId`
  - `schemaKey`
  - `version`
- 工作流发布校验已覆盖节点结构、条件边、Schema 引用和主数据引用的基础规则。
- OpenAPI 已导出 Agent / Workflow 模块专用响应契约，并已重新生成前端 TypeScript 类型。

## 3. 自动化验证结果

- 后端 `mvn test` 通过。
- 后端测试结果：`Tests run: 36, Failures: 0, Errors: 0, Skipped: 2`。
- 前端 `npm run openapi:generate` 通过。
- 前端 `npm run build` 通过。

## 4. 新增测试覆盖

- `DefaultAgentApplicationServiceTests`
  - 创建 Agent 时初始化首个草稿版本
  - 初始草稿的 `runtimeOptions` 会物化 Agent 默认总超时、默认最大步数和系统默认最大调用深度
- `DefaultWorkflowApplicationServiceTests`
  - 保存草稿时补齐不完整 `runtimeOptions`
  - `runtimeOptions` 出现未知字段直接拒绝
  - 保存草稿时正确派生、去重并稳定排序 `referencedSchemaVersions`
  - 从历史版本复制生成新草稿时原样复制 `runtimeOptions`
  - 发布草稿时原样复制 `runtimeOptions`
  - 发布后修改 Agent 默认值不会回头影响已持久化版本的运行约束
- `AgentWorkflowOpenApiContractTests`
  - Agent 与工作流接口必须导出具体响应契约，不退回泛型统一包装占位
- `DatabaseMigrationContractTests`
  - `workflow_version.runtime_options_json` 不再允许数据库默认 `{}`，必须由应用层显式写入

## 5. 当前环境限制

- 当前机器 Docker 环境不可用，因此 `PostgresMigrationTests` 的 2 个 Testcontainers 用例继续跳过。
- 前端构建仍会提示 bundle 体积较大，这是当前依赖规模带来的 Vite 警告，不影响本步骤验收。
- 本步骤按执行拆解只完成后端链路、OpenAPI 契约和前端类型刷新，`Agents` 页面与 `Workflow` 设计器页面的真实交互联调保留在后续前端步骤实现。

## 6. 结论

步骤 06 已完成，Agent 管理与工作流版本管理后端能力满足当前实施方案要求，可以进入步骤 07。
