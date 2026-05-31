# 步骤 06 Agent 管理与工作流版本管理链路执行记录 v1

## 1. 执行结论

步骤 06 已完成，可以进入步骤 07：运行内核基础组件。

## 2. 本步骤落地内容

- 已新增 Agent 主数据管理链路：
  - `AgentApplicationService`
  - `DefaultAgentApplicationService`
  - `AgentRepository`
  - `AgentMapper`
  - `GET /api/agents`
  - `POST /api/agents`
  - `PUT /api/agents/{agentId}`
  - `PUT /api/agents/{agentId}/status`
  - `GET /api/agents/{agentId}`
- 已新增工作流草稿与版本管理链路：
  - `WorkflowApplicationService`
  - `DefaultWorkflowApplicationService`
  - `WorkflowVersionRepository`
  - `WorkflowVersionMapper`
  - `WorkflowDraftValidationService`
  - `GET /api/agents/{agentId}/workflow-draft`
  - `PUT /api/agents/{agentId}/workflow-draft`
  - `POST /api/agents/{agentId}/workflow-draft/copy-from-version`
  - `POST /api/agents/{agentId}/workflow-draft/validate`
  - `POST /api/agents/{agentId}/workflow-draft/publish`
  - `GET /api/agents/{agentId}/workflow-versions`
  - `GET /api/agents/{agentId}/workflow-versions/{workflowVersionId}`
- 已完成工作流版本不可变语义落地：
  - 保存草稿时创建新的不可变 `DRAFT`
  - 旧当前草稿在同一事务内转为 `HISTORY`
  - 复制历史版本时通过显式命令接口创建新草稿，并写入 `sourceWorkflowVersionId`
  - 发布草稿时复制当前草稿生成新的不可变 `PUBLISHED`
  - 旧当前发布版本在同一事务内转为 `HISTORY`
  - 发布后当前草稿指针保持不变
- 已冻结 `runtimeOptions` 单一语义：
  - 后端稳定类型：`WorkflowRuntimeOptions`
  - 保存草稿时只接受 `timeoutSeconds`、`maxSteps`、`maxAgentCallDepth`
  - 缺失字段按 Agent 默认值和系统设置归一化补齐
  - 复制版本与发布版本时原样复制，不回头读取 Agent 当前默认值重算
- 已冻结 `referencedSchemaVersions` 单一语义：
  - 后端稳定类型：`ReferencedSchemaVersion`
  - 落库结构固定为对象数组 `{ schemaId, schemaKey, version }`
  - 保存草稿时由后端根据节点直接 Schema 引用自动派生
  - 派生结果会去重并按 `schemaKey`、`version` 稳定排序
- 已完成工作流草稿发布校验基础规则：
  - 单一 `START`
  - 至少一个 `END`
  - 非 `START` 节点必须有入边
  - 非 `END` 节点必须有出边
  - `CONDITION` 节点必须且只能有一条默认边
  - `START` / `END` / `JAVA_METHOD` / `TOOL` 的 SchemaRef 必填规则
  - `LLM` / `REVIEW` / `SUMMARY` 的提示词规则
  - `AGENT_CALL` 自调用、未发布、未启用规则
  - `JAVA_METHOD` / `TOOL` / `EXTERNAL_AGENT` 引用存在性与启用状态规则
- 已新增 Agent 与工作流 OpenAPI 具体响应契约：
  - `AgentPageApiResponse`
  - `AgentDetailApiResponse`
  - `WorkflowDraftApiResponse`
  - `WorkflowVersionPageApiResponse`
  - `WorkflowVersionDetailApiResponse`
  - `WorkflowValidationApiResponse`
  - `WorkflowPublishApiResponse`
- 已重新生成前端 OpenAPI 类型：
  - `11_code/frontend/src/api/generated/schema.ts`

## 3. 数据库与版本语义收口

- 已把 `11_code/backend/src/main/resources/db/migration/V1__create_core_tables.sql` 中的 `workflow_version.runtime_options_json` 改为应用层显式写入，不再依赖数据库默认 `{}`。
- `referenced_schema_versions_json` 仍保留数据库默认空数组 `[]` 作为防御，但正常写入路径均显式写入派生结果。
- 工作流版本持久化层已不再把 `runtimeOptions` 与 `referencedSchemaVersions` 当作动态 `Map<String, Object>` 传递。

## 4. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run openapi:generate` 已通过。
- 前端 `npm run build` 已通过。
- 新增前端生成类型已包含 Agent 与工作流版本管理接口。

## 5. 环境说明

- 后端验证使用仓库内 `.tools` 下的 JDK 21 与 Maven 3.9.11 完成。
- 导出 OpenAPI 时临时启动本地后端服务，不依赖本地 PostgreSQL 即可完成契约刷新。
- `PostgresMigrationTests` 仍因当前机器 Docker 不可用而跳过，不影响本步骤主结论。

## 6. 后续衔接

步骤 07 可以直接复用当前 Agent、WorkflowVersion、Schema 快照和发布校验能力，继续实现运行时 `WorkflowValidator`、`WorkflowCompiler`、`TraceWriter` 与 `RuntimeLimitGuard`。
