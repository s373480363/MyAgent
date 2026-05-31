# 步骤 05 Java 方法、工具、外部 Agent、系统设置主数据链路验证记录 v1

## 1. 验证范围

本记录对应 `10_plan_process/02-V1开发执行步骤拆解-v1.md` 中的步骤 05：Java 方法、工具、外部 Agent、系统设置主数据链路。

## 2. 已完成内容

- 后端已实现 `/api/settings` 的白名单查询与更新能力。
- 后端已移除旧运行时键 `myagent.runtime.default-timeout-seconds`，并改用冻结后的 6 个运行时细分键。
- 后端已实现 Java 方法目录只读查询接口：
  - `GET /api/java-methods`
  - `GET /api/java-methods/{methodId}`
- 后端已实现工具目录只读查询接口：
  - `GET /api/tools`
  - `GET /api/tools/{toolId}`
- 后端已实现外部 Agent 管理接口：
  - 列表
  - 详情
  - 创建
  - 编辑
  - 单独更新 secret
  - 启停
  - 测试
- 外部 Agent 已按架构要求实现单一敏感 header 语义：
  - 详情接口不回显敏感值
  - 普通更新接口不覆盖 secret
  - `/secrets` 支持覆盖、清空、保留旧值
  - 缺少 secret 时，测试接口会在真正外呼前失败
- OpenAPI 已导出主数据模块专用响应契约，并已重新生成前端 TypeScript 类型。

## 3. 自动化验证结果

- 后端 `mvn test` 通过。
- 后端测试结果：`Tests run: 32, Failures: 0, Errors: 0, Skipped: 2`。
- 前端 `npm run openapi:generate` 通过。
- 前端 `npm run build` 通过。

## 4. 新增测试覆盖

- `DefaultSettingApplicationServiceTests`
  - 返回 7 个白名单设置项，并区分 `SYSTEM_SETTING` 与 `APPLICATION_CONFIG`
  - 更新白名单设置后正确写入覆盖值
  - 非法键拒绝
  - 旧键拒绝
  - 值类型不匹配拒绝
- `DefaultExternalAgentApplicationServiceTests`
  - 敏感 header 不回显明文
  - 普通更新不覆盖已有 secret
  - `/secrets` 支持覆盖、清空、保留旧值
  - 缺少 secret 时 `testExternalAgent` 在真正外呼前失败
  - 普通更新接口拒绝直接提交 secretValue
- `MasterDataOpenApiContractTests`
  - 主数据接口必须导出具体响应契约
  - `CreateExternalAgentRequest`、`UpdateExternalAgentRequest`、`UpdateSettingsRequest` 的可选字段语义保持正确

## 5. 当前环境限制

- 当前机器 Docker 环境不可用，因此 `PostgresMigrationTests` 的 2 个 Testcontainers 用例继续跳过。
- 前端构建仍会提示 bundle 体积较大，这是当前依赖规模带来的 Vite 警告，不影响本步骤验收。
- 外部 Agent 测试执行器的真实 CLI/HTTP 调用未在自动化测试中对接外部进程或外部服务，本步骤通过单元测试优先锁定配置语义、secret 语义和前置失败规则。

## 6. 结论

步骤 05 已完成，主数据目录、系统设置和外部 Agent 维护链路满足当前实施方案要求，可以进入步骤 06。
