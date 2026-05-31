# 步骤 05 Java 方法、工具、外部 Agent、系统设置主数据链路执行记录 v1

## 1. 执行结论

步骤 05 已完成，可以进入步骤 06：Agent 管理与工作流版本管理链路。

## 2. 本步骤落地内容

- 已新增系统设置主数据链路：
  - `SettingApplicationService`
  - `DefaultSettingApplicationService`
  - `SettingsCatalog`
  - `SystemSettingRepository`
  - `SystemSettingMapper`
  - `GET /api/settings`
  - `PUT /api/settings`
- 已冻结 `/api/settings` 白名单与配置来源语义：
  - 只允许 7 个白名单键进入 `system_setting` 与 `/api/settings`
  - `source` 只返回 `SYSTEM_SETTING` 或 `APPLICATION_CONFIG`
  - `myagent.runtime.default-timeout-seconds` 已作为废弃键直接拒绝
  - 非白名单键、类型不匹配、不可编辑项会返回明确错误
- 已完成启动配置键改造：
  - `11_code/backend/src/main/resources/application.yml`
  - 移除旧键 `myagent.runtime.default-timeout-seconds`
  - 改为：
    - `myagent.runtime.default-agent-timeout-seconds`
    - `myagent.runtime.default-llm-timeout-seconds`
    - `myagent.runtime.default-java-method-timeout-seconds`
    - `myagent.runtime.default-external-agent-timeout-seconds`
    - `myagent.runtime.default-max-steps`
    - `myagent.runtime.default-max-agent-call-depth`
- 已新增 Java 方法目录只读链路：
  - `JavaMethodApplicationService`
  - `JavaMethodRepository`
  - `JavaMethodMapper`
  - `GET /api/java-methods`
  - `GET /api/java-methods/{methodId}`
- 已新增工具目录只读链路：
  - `ToolApplicationService`
  - `ToolRepository`
  - `ToolMapper`
  - `GET /api/tools`
  - `GET /api/tools/{toolId}`
- 已新增外部 Agent 全链路：
  - `ExternalAgentApplicationService`
  - `ExternalAgentRepository`
  - `ExternalAgentMapper`
  - `ExternalAgentCommandJsonCodec`
  - `ExternalAgentTestExecutor`
  - `GET /api/external-agents`
  - `GET /api/external-agents/{adapterId}`
  - `POST /api/external-agents`
  - `PUT /api/external-agents/{adapterId}`
  - `PUT /api/external-agents/{adapterId}/secrets`
  - `PUT /api/external-agents/{adapterId}/status`
  - `POST /api/external-agents/{adapterId}/test`
- 已完成外部 Agent 敏感 header 语义收口：
  - 普通详情不回显敏感值，不返回掩码占位值
  - 普通更新接口不覆盖 secret
  - secret 只能走 `/api/external-agents/{adapterId}/secrets`
  - `/secrets` 支持覆盖写入、显式清空、未出现值保持不变
  - `POST /api/external-agents/{adapterId}/test` 会在真正外呼前检查 `secretConfigured`
- 已新增主数据模块 OpenAPI 契约：
  - `SettingsListApiResponse`
  - `JavaMethodPageApiResponse`
  - `JavaMethodDetailApiResponse`
  - `ToolPageApiResponse`
  - `ToolDetailApiResponse`
  - `ExternalAgentPageApiResponse`
  - `ExternalAgentDetailApiResponse`
  - `ExternalAgentTestApiResponse`
- 已重新生成前端 OpenAPI 类型：
  - `11_code/frontend/src/api/generated/schema.ts`

## 3. 设计边界说明

- 步骤 05 只完成 Java 方法和工具的主数据查询链路，不提前实现运行时 `JavaMethodRegistry`、`JavaMethodInvoker`、`ToolRegistry`、`ToolExecutor`。这些能力按步骤拆解保留在后续运行时步骤实现。
- 外部 Agent 的敏感 secret 没有采用“掩码回传保留旧值”协议，也没有做旧键兼容回退；当前实现只有一套正式语义。
- `CUSTOM_HTTP` 的敏感 header 内部存储与普通 header 已分离处理：
  - 普通 header 保存在 `commandJson.headers`
  - 敏感 header 定义和 secret 内部字段独立维护
  - 对外详情接口只暴露 `headerName` 和 `secretConfigured`

## 4. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run openapi:generate` 已通过。
- 前端 `npm run build` 已通过。
- 前端生成类型已包含新增主数据接口：
  - `/api/settings`
  - `/api/java-methods`
  - `/api/tools`
  - `/api/external-agents`
  - `/api/external-agents/{adapterId}/secrets`
  - `/api/external-agents/{adapterId}/test`

## 5. 环境说明

- 后端验证使用仓库内 `.tools` 下的 JDK 21 与 Maven 3.9.11 完成。
- 导出 OpenAPI 时使用默认 profile 启动后端；当前步骤不依赖本地 PostgreSQL 即可完成契约导出和前端类型刷新。
- `PostgresMigrationTests` 仍因本机 Docker 环境不可用而跳过，不影响本步骤主数据链路结论。

## 6. 后续衔接

步骤 06 可以直接复用当前主数据目录与系统设置能力，为 Agent 基础信息管理、工作流草稿保存、历史版本查询和版本发布校验提供真实引用对象。
