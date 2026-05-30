# 步骤 04 Schema 管理全链路验证记录 v1

## 1. 验证范围

本记录对应 `10_plan_process/02-V1开发执行步骤拆解-v1.md` 中的步骤 04：Schema 管理全链路。

## 2. 已完成内容

- 后端已实现 Schema 应用服务、仓储、REST 接口和运行时校验服务。
- 后端已实现 JSON Schema 保存前校验，并能返回字段级中文错误明细。
- 后端已实现 Schema 草稿原地更新规则：仅允许 `status=DRAFT && locked=false`。
- 后端已实现基于旧版本创建同一 `schemaKey` 下一整数版本，且不修改旧版本内容。
- OpenAPI 已输出 Schema 专用响应契约：
  - `SchemaDetailApiResponse`
  - `SchemaPageApiResponse`
- 前端 OpenAPI 类型已重新生成，且可选字段不会被误生成为必填字段。

## 3. 自动化验证结果

- 后端 `mvn test` 通过。
- 后端测试结果：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 2`。
- 前端 `npm run openapi:generate` 通过。
- 前端 `npm run build` 通过。

## 4. 新增测试覆盖

- `DefaultSchemaApplicationServiceTests`
  - 创建 Schema 时版本从 1 开始。
  - 更新草稿时拒绝已锁定或非 DRAFT 版本。
  - 创建新版本时复用 `schemaKey` 并递增版本号。
- `DefaultSchemaValidationServiceTests`
  - 校验失败时返回字段路径和中文错误。
  - 载荷符合 Schema 时返回通过结果。
- `SchemaOpenApiContractTests`
  - Schema 接口必须导出具体响应契约，不能退回泛型 `ApiResponse` 占位。
  - Schema 请求体中 `description`、`javaType`、`sourceSchemaId` 保持可选语义。

## 5. 当前环境限制

- 当前机器 Docker 环境不可用，`PostgresMigrationTests` 中依赖 Testcontainers 的 2 个真实 PostgreSQL 用例被跳过。
- 当前机器没有本地 PostgreSQL 服务，`local` profile 启动会因为无法连接 `localhost:5432` 失败；默认 profile 可用于 OpenAPI 导出和基础启动验证。
- 前端构建提示 bundle 超过 500 KB，这是当前依赖体积导致的 Vite 警告，不影响本步骤验收。

## 6. 结论

步骤 04 已完成，Schema 管理全链路满足当前开发实施方案要求，可以进入步骤 05。
