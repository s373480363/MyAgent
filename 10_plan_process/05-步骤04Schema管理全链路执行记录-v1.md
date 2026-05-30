# 步骤 04 Schema 管理全链路执行记录 v1

## 1. 执行结论

步骤 04 已完成，可以进入步骤 05：Java 方法、工具、外部 Agent、系统设置主数据链路。

## 2. 本步骤落地内容

- 已新增 Schema 领域模型：
  - `SchemaCreatedFrom`
  - `SchemaStatus`
  - `SchemaDefinition`
- 已新增 Schema 应用服务：
  - `SchemaApplicationService`
  - `DefaultSchemaApplicationService`
  - 创建 Schema
  - 更新 `status=DRAFT && locked=false` 的 Schema 草稿
  - 查询 Schema 列表和详情
  - 基于旧版本创建同一 `schemaKey` 的下一整数版本
  - 锁定 Schema 版本
- 已新增 Schema 仓储链路：
  - `SchemaRepository`
  - `SchemaMapper`
  - `SchemaRecord`
  - `MyBatisSchemaRepository`
  - `mapper/schema/SchemaMapper.xml`
- 已新增 JSON 类型处理器：
  - `JsonNodeTypeHandler`
  - `InstantTypeHandler`
- 已新增 Schema 校验能力：
  - `SchemaDefinitionValidator` 用于保存前校验 JSON Schema 定义。
  - `SchemaValidationService` 用于运行时按 Schema 引用校验业务载荷。
  - `DefaultSchemaValidationService` 返回字段路径、关键字和中文错误消息。
- 已新增 Schema REST 接口：
  - `GET /api/schemas`
  - `POST /api/schemas`
  - `PUT /api/schemas/{schemaId}`
  - `GET /api/schemas/{schemaId}`
  - `POST /api/schemas/{schemaId}/versions`
- 已修正 Schema OpenAPI 契约输出：
  - Schema 列表接口返回 `SchemaPageApiResponse`。
  - Schema 详情、创建、更新和创建新版本接口返回 `SchemaDetailApiResponse`。
  - `description`、`javaType`、`sourceSchemaId` 按接口语义保持可选。
- 已修正前端 OpenAPI 生成脚本：
  - `openapi-typescript` 增加 `--default-non-nullable=false`，避免带默认值的可选字段被误生成成必填。
- 已重新生成前端 OpenAPI 类型：
  - `11_code/frontend/src/api/generated/schema.ts`

## 3. 设计边界说明

- `sourceSchemaId` 当前仅在 `POST /api/schemas` 输入侧用于表达“基于已有 Schema 创建”的来源参数，并校验来源 Schema 存在。
- 当前数据结构设计中的 `schema_definition` 没有 `source_schema_id` 字段，因此本步骤不自行新增第二套血缘字段，避免偏离 `6_schema_design`。
- Schema 草稿保存语义与 WorkflowVersion 保持分离：Schema 的 `DRAFT` 允许原地更新，WorkflowVersion 仍是不允许原地覆盖的不可变版本模型。

## 4. 验证结果

- 后端 `mvn test` 已通过。
- 前端 `npm run openapi:generate` 已通过。
- 前端 `npm run build` 已通过。
- 生成类型已确认引用：
  - `SchemaDetailApiResponse`
  - `SchemaPageApiResponse`
- 生成类型已确认以下字段为可选：
  - `description`
  - `javaType`
  - `sourceSchemaId`

## 5. 环境说明

- 当前终端未直接提供 `mvn` 命令，后端验证通过仓库内 `.tools` 下的 Maven 发行版和 JDK 21 完成。
- `local` profile 启动依赖本地 PostgreSQL；当前机器 `localhost:5432` 未提供 PostgreSQL，因此本步骤导出 OpenAPI 时使用默认 profile。
- `PostgresMigrationTests` 仍会在 Docker 环境不可用时跳过，这是本机容器环境限制，不影响本步骤 Schema 管理链路结论。

## 6. 后续衔接

步骤 05 可以直接复用当前 Schema 管理能力，为 Java 方法、工具、外部 Agent 和系统设置主数据提供真实 Schema 引用与运行时校验入口。
