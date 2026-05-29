# 05 响应、错误、分页与 OpenAPI 约定 v1

## 1. 统一响应模型

### 1.1 ApiResponse

所有业务接口统一采用以下包装：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

字段约束：

- `success`：布尔值，表示业务是否成功。
- `data`：成功时返回业务对象，失败时为 `null`。
- `error`：失败时返回错误对象，成功时为 `null`。

运行类接口的特殊约定：

- 运行记录已创建后，即使节点执行失败、Schema 校验失败、模型失败或超时，也返回 `success=true`。
- 此时 `data.runId` 必须存在，`data.status` 为 `FAILED` 或 `TIMEOUT`，`data.error` 保存运行级错误摘要。
- 只有未创建运行记录的前置失败才返回 `success=false`。

### 1.2 ApiError

```json
{
  "code": "SCHEMA_VALIDATION_FAILED",
  "message": "字段 `question` 不能为空。",
  "details": [
    {
      "field": "$.input.question",
      "reason": "required",
      "message": "请输入问题"
    }
  ]
}
```

建议字段：

- `code`：稳定机器码。
- `message`：中文用户提示。
- `details`：结构化明细，可空。

## 2. 错误码分组

| 分组 | 示例错误码 | 说明 |
|------|------------|------|
| 参数错误 | `INVALID_ARGUMENT` | 请求参数不合法 |
| 资源不存在 | `AGENT_NOT_FOUND` | 指定对象不存在 |
| 状态非法 | `AGENT_DISABLED` | 对象存在但状态不可用 |
| 工作流校验 | `WORKFLOW_VALIDATION_FAILED` | 草稿或发布前校验失败 |
| Schema 校验 | `SCHEMA_VALIDATION_FAILED` | 输入输出不符合 JSON Schema |
| 运行失败 | `NODE_EXECUTION_FAILED` | 节点执行失败 |
| 超时 | `RUN_TIMEOUT` | 运行或节点超时 |
| 方法调用 | `JAVA_METHOD_EXECUTION_FAILED` | Java 方法调用失败 |
| 工具调用 | `TOOL_CALL_FAILED` | 工具执行失败 |
| 外部 Agent | `EXTERNAL_AGENT_CALL_FAILED` | 外部 Agent 调用失败 |
| 协作调用 | `TARGET_AGENT_NOT_PUBLISHED` | 目标 Agent 未发布 |
| 验收 | `EVAL_CASE_UNCONFIRMED` | 验收用例未确认 |
| 配置 | `SETTING_NOT_EDITABLE` | 系统设置不可编辑 |

## 3. 分页模型

### 3.1 PageResponse

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

约定：

- `items` 为当前页数据。
- `page` 从 1 开始。
- `pageSize` 默认 20。
- `total` 为总记录数。

### 3.2 列表查询参数

统一支持以下基础参数：

- `page`
- `pageSize`
- `status`
- `keyword`
- `startedAtFrom`
- `startedAtTo`

具体接口可以在此基础上增加领域专属筛选字段，但不得改变基础语义。

## 4. 字段命名约定

- ID 字段统一使用 `xxxId`。
- 业务键统一使用 `xxxKey`。
- 对外运行标识统一使用 `runId`，对应数据库 `agent_run.run_no`。
- 对外验收运行标识统一使用 `evalRunId`，对应数据库 `eval_run.run_no`。
- 数据库内部自增主键不作为运行标识暴露。
- 时间字段统一使用 `createdAt`、`updatedAt`、`startedAt`、`finishedAt`。
- 版本字段统一使用 `versionNo`。
- 布尔字段统一使用 `isXxx` 或明确语义的形容词。

## 5. OpenAPI 约定

### 5.1 强制纳入

以下内容必须进入 OpenAPI：

- 请求体。
- 响应体。
- 错误体。
- 分页对象。
- 枚举。
- 嵌套对象。
- 运行结果对象。

### 5.2 生成规则

- 后端 Controller 作为 OpenAPI 源头。
- 前端类型由 OpenAPI 自动生成。
- 禁止手工复制 Java DTO 到前端。
- 禁止前端自建长期维护的影子类型。

### 5.3 标注要求

- 每个接口必须有稳定 `operationId`。
- 每个请求和响应模型必须有清晰 `schema` 名称。
- 每个枚举值必须在文档中给出中文含义。

## 6. 校验信息返回

字段级校验失败时，`details` 至少包含：

- `field`：字段路径。
- `reason`：失败类型。
- `message`：中文提示。

必要时可追加：

- `expected`：期望值。
- `actual`：实际值。
- `hint`：修复建议。

## 7. 调试与排障原则

- 运行记录已创建后的运行类接口必须返回 `runId`。
- 未创建运行记录的前置失败返回 `success=false`，此时可以没有 `runId`。
- 失败信息不应只给堆栈，不应只给技术术语。
- 结构校验失败必须指出具体路径。
- 外部调用失败必须指出目标对象和失败阶段。
