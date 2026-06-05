# 02 对外 REST 接口 v1

## 1. 接口总则

- 所有接口前缀统一为 `/api`。
- v1 不做登录认证。
- 所有用户可见错误使用中文。
- 业务响应统一使用 `ApiResponse<T>`。
- 正式运行只使用当前发布版本。
- 调试运行可使用草稿或指定版本。
- 本文档中的 JSON 示例除特别说明外，展示的是 `ApiResponse.data` 内层对象；实际 HTTP 响应必须按统一 `ApiResponse<T>` 包装。

## 2. Agent 管理

### 2.1 查询 Agent 列表

`GET /api/agents`

查询参数：

- `page`
- `pageSize`
- `status`
- `keyword`

返回：

```json
{
  "items": [
    {
      "agentId": 1,
      "agentKey": "summary-agent",
      "name": "摘要 Agent",
      "description": "对文本进行总结",
      "status": "ENABLED",
      "currentDraftWorkflowVersionId": 11,
      "currentPublishedWorkflowVersionId": 10,
      "updatedAt": "2026-05-27T10:00:00+08:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

### 2.2 创建 Agent

`POST /api/agents`

请求体建议：

```json
{
  "agentKey": "summary-agent",
  "name": "摘要 Agent",
  "description": "对文本进行总结",
  "systemPrompt": "你是一个专业的摘要助手。",
  "defaultModelOfferingKey": "openai.gpt_4_1_mini",
  "temperature": 0.2,
  "timeoutSeconds": 600,
  "maxSteps": 30
}
```

规则：

- `agentKey` 必填且唯一。
- `defaultModelOfferingKey` 可空；如果传入，必须引用启用供应商下的启用模型供应项。
- 创建成功后应初始化一个可编辑草稿版本。
- Agent 创建只负责基础信息，不承载工作流结构。
- `timeoutSeconds` 和 `maxSteps` 属于 Agent 级默认值；后端在创建初始草稿版本时必须将其物化进 `WorkflowVersion.runtimeOptions`，后续执行不回头读取 Agent 当前值。

### 2.3 更新 Agent

`PUT /api/agents/{agentId}`

请求体建议：

```json
{
  "name": "摘要 Agent",
  "description": "对文本进行总结",
  "systemPrompt": "你是一个专业的摘要助手。",
  "defaultModelOfferingKey": "openai.gpt_4_1_mini",
  "temperature": 0.2,
  "timeoutSeconds": 600,
  "maxSteps": 30
}
```

规则：

- `agentKey` 不允许通过此接口修改。
- `defaultModelOfferingKey` 可更新为有效模型供应项或清空；清空不直接修改已持久化 WorkflowVersion。
- 草稿与发布版本指针由工作流接口维护。
- 更新 Agent 默认值只影响后续新建或重新归一化的草稿版本，不直接改变已持久化 WorkflowVersion 的执行语义。

### 2.4 启停 Agent

`PUT /api/agents/{agentId}/status`

请求体：

```json
{
  "status": "ENABLED"
}
```

规则：

- 停用后不可作为正式 API 调用目标。
- 停用不删除历史运行记录。

### 2.5 查询 Agent 详情

`GET /api/agents/{agentId}`

返回建议：

```json
{
  "agentId": 1,
  "agentKey": "summary-agent",
  "name": "摘要 Agent",
  "description": "对文本进行总结",
  "status": "ENABLED",
  "systemPrompt": "你是一个专业的摘要助手。",
  "defaultModelOfferingKey": "openai.gpt_4_1_mini",
  "temperature": 0.2,
  "timeoutSeconds": 600,
  "maxSteps": 30,
  "currentDraftWorkflow": {
    "workflowVersionId": 11,
    "versionNo": 3,
    "status": "DRAFT",
    "updatedAt": "2026-05-27T09:30:00+08:00",
    "sourceWorkflowVersionId": 10
  },
  "currentPublishedWorkflow": {
    "workflowVersionId": 10,
    "versionNo": 2,
    "status": "PUBLISHED",
    "publishedAt": "2026-05-26T18:00:00+08:00"
  },
  "historyVersionSummary": {
    "total": 2,
    "latestWorkflowVersionId": 9,
    "latestVersionNo": 1,
    "latestPublishedAt": "2026-05-20T14:00:00+08:00"
  },
  "updatedAt": "2026-05-27T10:00:00+08:00"
}
```

规则：

- 必须返回 Agent 基础信息、当前草稿版本摘要、当前发布版本摘要和历史版本入口摘要。
- `defaultModelOfferingKey` 允许返回 `null`，表示 Agent 没有配置默认模型供应项。
- `timeoutSeconds` 和 `maxSteps` 表示 Agent 级默认值，用于创建或归一化 WorkflowVersion；不是已持久化工作流版本的最终执行真相。
- 若当前没有草稿版本或发布版本，对应字段返回 `null`。
- 历史版本的详细列表和详情通过 `workflow-versions` 相关接口查询；本接口必须提供页面进入历史版本能力所需的摘要信息。

## 3. 工作流草稿与发布

### 3.1 获取草稿

`GET /api/agents/{agentId}/workflow-draft`

返回当前草稿的完整工作流版本对象。

规则：

- 返回体中的 `runtimeOptions` 必须是完整对象 `{ timeoutSeconds, maxSteps, maxAgentCallDepth }`。
- 返回体中的 `referencedSchemaVersions` 必须是对象数组，每项为 `{ schemaId, schemaKey, version }`。

### 3.2 保存草稿

`PUT /api/agents/{agentId}/workflow-draft`

请求体建议：

```json
{
  "nodes": [],
  "edges": [],
  "runtimeOptions": {
    "timeoutSeconds": 600,
    "maxSteps": 30,
    "maxAgentCallDepth": 3
  }
}
```

规则：

- `nodes` 和 `edges` 必须符合节点与边设计。
- `runtimeOptions` 的正式字段集固定为 `timeoutSeconds`、`maxSteps`、`maxAgentCallDepth`，不允许额外字段。
- 保存时后端必须把 `runtimeOptions` 归一化为完整对象；执行阶段只读取持久化后的 `WorkflowVersion.runtimeOptions`。
- 保存草稿不会生成发布版本。
- 保存后应刷新当前草稿版本指针。
- 保存草稿必须创建新的不可变 `DRAFT` 版本，不允许覆盖旧草稿内容。
- 若当前已存在草稿版本，被替换的旧草稿必须在同一事务内转为 `HISTORY`，并更新当前草稿指针。
- 普通保存草稿接口只承载当前草稿保存语义，不通过请求体隐含表达“从历史版本复制”的语义。

### 3.3 从已有版本复制生成新草稿

`POST /api/agents/{agentId}/workflow-draft/copy-from-version`

请求体建议：

```json
{
  "sourceWorkflowVersionId": 9
}
```

返回：

```json
{
  "workflowVersionId": 13,
  "agentId": 1,
  "versionNo": 4,
  "status": "DRAFT",
  "sourceWorkflowVersionId": 9,
  "createdAt": "2026-05-27T10:10:00+08:00",
  "updatedAt": "2026-05-27T10:10:00+08:00"
}
```

规则：

- `sourceWorkflowVersionId` 必须属于当前 Agent 的已持久化工作流版本。
- 复制后必须创建新的不可变 `DRAFT` 版本，写入 `sourceWorkflowVersionId`，并更新当前草稿指针。
- 被替换的旧草稿转为 `HISTORY` 只读，源版本本身保持不变。
- 若当前已存在草稿版本，被替换的旧草稿必须在同一事务内转为 `HISTORY`，避免同一 Agent 同时存在多条当前 `DRAFT`。
- 该接口是“从版本复制生成新草稿”的唯一对外命令，不能把该语义隐含塞入普通保存草稿接口。

### 3.4 校验草稿

`POST /api/agents/{agentId}/workflow-draft/validate`

返回：

```json
{
  "valid": false,
  "errors": [
    {
      "code": "WORKFLOW_VALIDATION_FAILED",
      "message": "工作流缺少结束节点。",
      "details": [
        {
          "field": "$.nodes",
          "reason": "missing_end_node",
          "message": "至少需要一个 END 节点"
        }
      ]
    }
  ]
}
```

### 3.5 发布草稿

`POST /api/agents/{agentId}/workflow-draft/publish`

请求体建议：

```json
{
  "publishMessage": "首版发布"
}
```

返回：

```json
{
  "workflowVersionId": 12,
  "agentId": 1,
  "versionNo": 2,
  "status": "PUBLISHED",
  "publishedAt": "2026-05-27T10:00:00+08:00"
}
```

规则：

- 发布前必须完成完整校验。
- 发布后生成不可变版本。
- 旧发布版本转为历史只读。
- 若当前已存在发布版本，旧发布版本必须在同一事务内转为 `HISTORY`，并更新当前发布指针，避免同一 Agent 同时存在多条当前 `PUBLISHED`。

### 3.6 查询工作流版本列表

`GET /api/agents/{agentId}/workflow-versions`

查询参数建议：

- `page`
- `pageSize`
- `status`

返回建议包含：

- `workflowVersionId`
- `versionNo`
- `status`
- `publishedAt`
- `createdAt`
- `sourceWorkflowVersionId`

规则：

- 返回当前 Agent 的草稿、发布和历史版本摘要。
- 历史版本只允许查看和复制生成新草稿，不允许原地修改。

### 3.7 查询工作流版本详情

`GET /api/agents/{agentId}/workflow-versions/{workflowVersionId}`

返回建议包含：

- 工作流版本基础信息。
- `nodes`
- `edges`
- `runtimeOptions`
- `referencedSchemaVersions`

规则：

- 版本详情必须可用于历史回看和运行复盘。
- 对历史版本返回只读定义，不返回可变画布态。
- `runtimeOptions` 必须始终返回完整对象 `{ timeoutSeconds, maxSteps, maxAgentCallDepth }`。
- `referencedSchemaVersions` 必须始终返回对象数组，每项为 `{ schemaId, schemaKey, version }`，不允许字符串列表。

## 4. 同步运行与调试运行

### 4.1 正式同步运行

`POST /api/agents/{agentKey}/runs`

请求体：

```json
{
  "input": {
    "question": "请总结这段文本"
  }
}
```

返回：

```json
{
  "runId": "run_20260527_000001",
  "agentKey": "summary-agent",
  "workflowVersionId": 12,
  "status": "SUCCESS",
  "output": {
    "summary": "..."
  },
  "error": null,
  "durationMs": 4260
}
```

规则：

- 只使用当前发布版本。
- 运行记录创建成功后，节点失败、Schema 校验失败、模型失败或超时仍返回 `success=true`，并通过 `data.status=FAILED/TIMEOUT` 表达运行结果。
- 运行记录创建成功后必须返回 `runId`。
- 请求参数非法、Agent 不存在、Agent 停用、无发布版本、无法创建运行记录时返回 `success=false`，此时可以没有 `runId`。
- `runId` 是对外运行标识，对应数据库 `agent_run.run_no`，不是数据库自增主键。
- 运行成功与失败都应写入运行记录和 Trace。

### 4.2 调试运行

`POST /api/agents/{agentId}/debug-runs`

请求体建议：

```json
{
  "workflowVersionId": 11,
  "input": {
    "question": "请总结这段文本"
  }
}
```

规则：

- 允许运行草稿版本或显式指定版本。
- 若未提供 `workflowVersionId`，后端使用当前草稿版本。
- 调试运行必须在结果中可见实际运行版本。
- 若画布存在未保存修改，必须先保存草稿生成新的 `workflowVersionId`，再发起调试运行。

## 5. 运行查询

### 5.1 运行列表

`GET /api/runs`

查询参数：

- `page`
- `pageSize`
- `agentId`
- `agentKey`
- `runType`
- `status`
- `keyword`
- `startedAtFrom`
- `startedAtTo`

返回 `PageResponse<RunListItemDto>`。

正式字段：

- `runId`
- `agentId`
- `agentKey`
- `agentName`
- `workflowVersionId`
- `runType`
- `status`
- `startedAt`
- `finishedAt`
- `durationMs`

规则：

- 未显式传入 `runType` 时，默认只返回 `API`、`DEBUG`、`AGENT_CALL` 三类普通运行。
- 只有显式传入 `runType=EVAL` 时，才返回节点验收配套的 `AgentRun(runType=EVAL)`。
- 列表接口只返回摘要字段，不内嵌 `nodeRuns`、`traceEvents` 或大块输入输出全文。

### 5.2 运行详情

`GET /api/runs/{runId}`

返回 `RunDetailDto`：

```json
{
  "runId": "run_20260527_000001",
  "agent": {
    "agentId": 1,
    "agentKey": "summary-agent",
    "agentName": "摘要 Agent"
  },
  "workflowVersion": {
    "workflowVersionId": 12,
    "versionNo": 2,
    "status": "PUBLISHED"
  },
  "runType": "API",
  "status": "FAILED",
  "parentRunId": null,
  "evalRunId": null,
  "input": {
    "question": "请总结这段文本"
  },
  "output": null,
  "error": {
    "code": "NODE_EXECUTION_FAILED",
    "message": "节点执行失败。"
  },
  "startedAt": "2026-05-27T10:00:00+08:00",
  "finishedAt": "2026-05-27T10:00:04+08:00",
  "durationMs": 4260,
  "childRuns": [
    {
      "runId": "run_20260527_000002",
      "agentId": 2,
      "agentKey": "review-agent",
      "agentName": "审核 Agent",
      "status": "SUCCESS",
      "startedAt": "2026-05-27T10:00:01+08:00",
      "finishedAt": "2026-05-27T10:00:03+08:00",
      "durationMs": 1800
    }
  ],
  "nodeRuns": [
    {
      "nodeRunId": 101,
      "nodeId": "node_1",
      "nodeName": "摘要节点",
      "nodeType": "LLM",
      "status": "FAILED",
      "input": {
        "question": "请总结这段文本"
      },
      "output": null,
      "schemaValidationResult": {
        "valid": false,
        "errors": [
          {
            "path": "$.summary",
            "message": "字段缺失"
          }
        ]
      },
      "errorMessage": "模型输出未通过 Schema 校验。",
      "startedAt": "2026-05-27T10:00:00+08:00",
      "finishedAt": "2026-05-27T10:00:04+08:00",
      "durationMs": 4260
    }
  ],
  "traceEvents": [
    {
      "traceEventId": 1001,
      "nodeRunId": 101,
      "evalRunId": null,
      "eventType": "MODEL_REQUEST",
      "summary": "调用模型供应项 openai.gpt_4_1_mini",
      "detail": {
        "providerKey": "openai-default",
        "modelOfferingKey": "openai.gpt_4_1_mini",
        "modelKey": "gpt-4.1-mini",
        "upstreamModelName": "gpt-4.1-mini"
      },
      "eventTime": "2026-05-27T10:00:00+08:00"
    }
  ]
}
```

规则：

- `RunDetailDto` 是 v1 正式冻结契约，不再保留“由开发自行决定字段层级”的空间。
- `nodeRuns` 返回全文详情，按 `startedAt asc` 排序，不分页。
- `traceEvents` 返回全文详情，按 `eventTime asc` 排序，不分页。
- `childRuns` 只返回子运行摘要，不内嵌子运行的 `nodeRuns` 和 `traceEvents`。
- 若当前 `runId` 对应 `runType=EVAL`，`evalRunId` 必须返回关联的 `evalRunId`；此时 `nodeRuns` 可以为空，但 `traceEvents` 仍按正式契约返回。

## 6. Schema 管理

### 6.1 查询 Schema 列表

`GET /api/schemas`

建议查询参数：

- `page`
- `pageSize`
- `keyword`
- `status`
- `createdFrom`

### 6.2 创建 Schema

`POST /api/schemas`

请求体建议：

```json
{
  "schemaKey": "agent.input.summary",
  "name": "摘要输入",
  "description": "摘要 Agent 的输入结构",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "question": {
        "type": "string"
      }
    },
    "required": ["question"],
    "additionalProperties": false
  },
  "javaType": "syc.agentstudio.example.SummaryInput",
  "createdFrom": "AGENT_INPUT",
  "sourceSchemaId": null
}
```

规则：

- 若创建的是新系列，`schemaKey` 必填且新建版本号从 1 开始。
- 若基于已有版本复制，`sourceSchemaId` 可选。
- 已锁定版本不可修改，只能新建版本。

### 6.3 更新 Schema 草稿

`PUT /api/schemas/{schemaId}`

请求体建议：

```json
{
  "name": "摘要输入",
  "description": "摘要 Agent 的输入结构",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "question": {
        "type": "string"
      }
    },
    "required": ["question"],
    "additionalProperties": false
  },
  "javaType": "syc.agentstudio.example.SummaryInput"
}
```

规则：

- 仅允许更新 `status=DRAFT` 且 `locked=false` 的 Schema。
- 不允许修改 `schemaKey` 和 `version`。
- 已被发布工作流引用的 Schema 必须通过新版本演进。

### 6.4 创建 Schema 新版本

`POST /api/schemas/{schemaId}/versions`

请求体建议：

```json
{
  "name": "摘要输入",
  "description": "摘要 Agent 的输入结构 v2",
  "jsonSchema": {
    "type": "object",
    "properties": {
      "question": {
        "type": "string"
      },
      "language": {
        "type": "string"
      }
    },
    "required": ["question"],
    "additionalProperties": false
  },
  "javaType": "syc.agentstudio.example.SummaryInput"
}
```

规则：

- 基于路径中的旧版本创建同一 `schemaKey` 的下一整数版本。
- 新版本初始为 `status=DRAFT`、`locked=false`。
- 不修改旧版本内容。

### 6.5 查询 Schema 详情

`GET /api/schemas/{schemaId}`

返回建议包含：

- Schema 基础信息。
- 版本号。
- JSON Schema 内容。
- 引用状态。
- 关联工作流版本摘要。

## 7. 模型供应商与模型供应项

### 7.1 查询模型供应商列表

`GET /api/model-providers`

查询参数：

- `page`
- `pageSize`
- `status`
- `keyword`

返回项建议：

```json
{
  "providerId": 1,
  "providerKey": "openai-default",
  "name": "默认 OpenAI-compatible 供应商",
  "providerType": "OPENAI_COMPATIBLE",
  "baseUrl": "https://api.openai.com",
  "apiKeyConfigured": true,
  "apiKeyMask": "已配置",
  "status": "ENABLED"
}
```

规则：

- 不返回 API Key 明文。
- 不返回可回传的密钥占位值。

### 7.2 创建模型供应商

`POST /api/model-providers`

请求体建议：

```json
{
  "providerKey": "siliconflow",
  "name": "硅基流动",
  "providerType": "OPENAI_COMPATIBLE",
  "baseUrl": "https://api.siliconflow.cn",
  "apiKey": "只写密钥",
  "description": "OpenAI-compatible 供应商"
}
```

规则：

- `apiKey` 是只写字段。
- 创建成功响应不返回 `apiKey`。

### 7.3 更新模型供应商

`PUT /api/model-providers/{providerId}`

规则：

- 只允许更新名称、描述、Base URL 等非敏感字段。
- 不允许通过普通更新接口修改 API Key。
- 请求体缺少 API Key 时不得清空历史密钥。

### 7.4 更新模型供应商密钥

`PUT /api/model-providers/{providerId}/secrets`

请求体建议：

```json
{
  "apiKey": "只写新密钥",
  "clearApiKey": false
}
```

规则：

- `apiKey` 出现时替换密钥。
- `clearApiKey=true` 时清空密钥。
- 响应体只返回密钥配置状态，不返回明文。

### 7.5 启停模型供应商

`PUT /api/model-providers/{providerId}/status`

请求体：

```json
{
  "status": "ENABLED"
}
```

### 7.6 测试模型供应商

`POST /api/model-providers/{providerId}/test`

请求体：

```json
{
  "offeringKey": "openai.gpt_4_1_mini",
  "prompt": "ping"
}
```

返回建议：

```json
{
  "providerKey": "openai-default",
  "offeringKey": "openai.gpt_4_1_mini",
  "modelKey": "gpt-4.1-mini",
  "upstreamModelName": "gpt-4.1-mini",
  "durationMs": 320,
  "message": "模型供应商连接测试成功。"
}
```

规则：

- `offeringKey` 必填，且必须属于路径中的 `providerId`。
- `prompt` 可选；为空时后端使用 `ping`。
- 测试使用供应商已保存密钥和请求体指定的模型供应项。
- 连接测试允许供应商或供应项当前为 `DISABLED`，因为该接口用于启用前验证；但必须存在、归属正确且已配置 API Key。
- 该接口是轻量探活，不是无界的真实业务运行；必须复用正式 `agent.studio.runtime.default-llm-timeout-seconds` 超时契约或同一共享超时包装。
- 请求必须在超时窗口内返回；超时后后端必须主动结束调用并返回中文业务错误。
- 测试失败返回中文摘要。
- 不返回上游完整鉴权报文、密钥内容、Base URL 或模型原始输出全文。

### 7.7 查询模型供应项

`GET /api/model-offerings`

查询参数：

- `page`
- `pageSize`
- `providerKey`
- `status`
- `keyword`

返回项建议：

```json
{
  "offeringId": 1,
  "offeringKey": "openai.gpt_4_1_mini",
  "providerKey": "openai-default",
  "providerName": "默认 OpenAI-compatible 供应商",
  "modelKey": "gpt-4.1-mini",
  "displayName": "GPT-4.1 Mini",
  "upstreamModelName": "gpt-4.1-mini",
  "status": "ENABLED",
  "providerStatus": "ENABLED",
  "selectable": true,
  "unavailableReason": ""
}
```

规则：

- 该接口用于选择器可选列表，默认只返回启用供应商下的启用供应项。
- 不用于回填已绑定但当前不可选的供应项。

### 7.8 按 key 查询模型供应项

`GET /api/model-offerings/{offeringKey}`

规则：

- 按 `offeringKey` 精确查询。
- 即使供应项或供应商已停用，也必须返回详情，用于 Agent、Workflow、Eval 编辑页回填当前绑定值。
- 响应不得包含供应商 API Key、密文或 Base URL。
- 响应必须包含 `selectable` 和 `unavailableReason`。

### 7.9 按 key 批量查询模型供应项

`GET /api/model-offerings/by-keys`

查询参数：

- `offeringKeys`，可重复传入。

返回建议：

```json
{
  "items": [
    {
      "offeringKey": "openai.gpt_4_1_mini",
      "providerKey": "openai-default",
      "providerName": "默认 OpenAI-compatible 供应商",
      "modelKey": "gpt-4.1-mini",
      "displayName": "GPT-4.1 Mini",
      "upstreamModelName": "gpt-4.1-mini",
      "status": "DISABLED",
      "providerStatus": "ENABLED",
      "selectable": false,
      "unavailableReason": "模型供应项已停用"
    }
  ],
  "missingKeys": []
}
```

规则：

- 前端打开 Agent、Workflow 或 Eval 编辑页时，应先用该接口回填当前绑定的供应项，再加载分页可选列表。
- `missingKeys` 用于展示无法解析的历史绑定，页面必须保留原值并提示，不得静默清空。

### 7.10 创建模型供应项

`POST /api/model-offerings`

请求体建议：

```json
{
  "offeringKey": "siliconflow.qwen2_5_72b",
  "providerKey": "siliconflow",
  "modelKey": "qwen2.5-72b",
  "displayName": "Qwen2.5 72B - 硅基流动",
  "upstreamModelName": "Qwen/Qwen2.5-72B-Instruct",
  "defaultTemperature": 0.2
}
```

规则：

- `offeringKey` 是 Agent 默认值和 LLM 类节点保存的唯一模型引用。
- `modelKey` 是跨供应商模型身份字段，不要求先创建独立模型定义。
- `providerKey + upstreamModelName` 在同一供应商内不得重复。

### 7.11 更新和启停模型供应项

`PUT /api/model-offerings/{offeringId}`

`PUT /api/model-offerings/{offeringId}/status`

规则：

- 停用后不能被新工作流选择。
- 已发布工作流引用停用供应项时，运行失败并写入明确 Trace，不自动切换到其他模型。

## 8. Java 方法、工具与外部 Agent

### 8.1 Java 方法列表

`GET /api/java-methods`

### 8.2 Java 方法详情

`GET /api/java-methods/{methodId}`

### 8.3 工具列表

`GET /api/tools`

### 8.4 工具详情

`GET /api/tools/{toolId}`

### 8.5 外部 Agent 列表

`GET /api/external-agents`

### 8.6 外部 Agent 详情

`GET /api/external-agents/{adapterId}`

规则：

- 对 `CUSTOM_HTTP`，`commandJson.headers` 只返回非敏感 header 的明文值。
- 敏感 header 必须通过独立的元信息数组返回，例如：

```json
{
  "secretHeaders": [
    {
      "headerName": "Authorization",
      "secretConfigured": true
    }
  ]
}
```

- 详情接口不得返回敏感 header 明文，也不得返回可回传的掩码占位值。

### 8.7 创建外部 Agent

`POST /api/external-agents`

请求体建议：

```json
{
  "adapterKey": "local-custom-agent",
  "adapterType": "CUSTOM_CLI",
  "name": "本地自定义 Agent",
  "description": "通过本地命令行调用",
  "commandJson": {
    "command": "my-agent",
    "arguments": ["run", "--input", "{inputJson}", "--prompt", "{prompt}"],
    "resultSource": {
      "type": "STDOUT_JSON"
    }
  },
  "workingDirectory": "D:/myproject",
  "timeoutSeconds": 600,
  "captureStdout": true,
  "captureStderr": true,
  "captureGitDiff": false,
  "outputSchemaId": null
}
```

规则：

- v1 允许用户在平台内新增 `CUSTOM_CLI` 和 `CUSTOM_HTTP` 外部 Agent。
- 内置的 `CODEX_CLI` 和 `OPENCODE_CLI` 可由系统预置，也允许在页面调整非敏感配置。
- CLI `arguments` 必须是数组，不允许保存拼接后的 shell 字符串。
- 对 `CUSTOM_HTTP`，创建请求可选携带只写字段 `secretHeaders`，用于首次写入敏感 header secret。
- `secretHeaders` 中的 `secretValue` 只写不回显，创建成功后的响应体和详情接口都不得返回该值。

### 8.8 更新外部 Agent

`PUT /api/external-agents/{adapterId}`

规则：

- 允许更新名称、描述、命令配置、工作目录、超时、采集配置和输出 Schema。
- 不允许修改 `adapterKey`。
- 对 `CUSTOM_HTTP`，普通更新接口只更新非敏感 header 和结构化配置，不要求前端重新传递历史 secret。
- 普通更新接口不得因为请求体中缺少敏感 secret 而把旧值覆盖为空。
- 已被发布工作流引用的外部 Agent 更新后只影响后续运行，历史运行以 Trace 记录为准。

### 8.9 更新外部 Agent 敏感 secret

`PUT /api/external-agents/{adapterId}/secrets`

请求体建议：

```json
{
  "items": [
    {
      "headerName": "Authorization",
      "secretValue": "Bearer example-token"
    }
  ],
  "clearHeaderNames": [
    "X-Legacy-Key"
  ]
}
```

规则：

- 该接口用于维护 `CUSTOM_HTTP` 的敏感 header secret，不用于更新普通字段。
- `items` 中出现的 `headerName` 执行覆盖写入。
- `clearHeaderNames` 中出现的 `headerName` 执行显式清空，并移除对应敏感 header 定义。
- 未出现在 `items` 或 `clearHeaderNames` 中的敏感 secret 保持原值不变。
- `secretValue` 只写不回显；后续详情接口只返回 `headerName` 和 `secretConfigured`。
- 敏感 header 重命名按“清空旧 header + 写入新 header”处理，不提供隐式迁移。

### 8.10 启停外部 Agent

`PUT /api/external-agents/{adapterId}/status`

请求体：

```json
{
  "status": "ENABLED"
}
```

### 8.11 测试外部 Agent

`POST /api/external-agents/{adapterId}/test`

请求体建议：

```json
{
  "prompt": "请返回一段测试输出",
  "input": {}
}
```

规则：

- 测试连接不创建正式 AgentRun。
- 对 `CUSTOM_HTTP`，如果存在未配置 secret 的敏感 header，必须在真正发起 HTTP 请求前失败，并返回明确配置错误。
- 测试结果应返回适配器状态、退出码或 HTTP 状态、stdout/stderr 摘要、输出摘要和耗时。
- 测试失败必须返回中文错误摘要。

## 9. 节点验收

### 9.1 查询验收套件

`GET /api/eval-suites`

建议查询参数：

- `page`
- `pageSize`
- `agentId`
- `workflowVersionId`
- `nodeId`
- `status`
- `keyword`

### 9.2 创建验收套件

`POST /api/eval-suites`

请求体建议：

```json
{
  "agentId": 1,
  "workflowVersionId": 12,
  "nodeId": "node_1",
  "name": "摘要节点回归集",
  "goal": "验证摘要节点在常见输入上的稳定性",
  "passThreshold": 80
}
```

### 9.3 更新验收套件

`PUT /api/eval-suites/{suiteId}`

请求体建议：

```json
{
  "name": "摘要节点回归集",
  "goal": "验证摘要节点在常见输入上的稳定性",
  "passThreshold": 80
}
```

规则：

- 仅允许更新 `status=DRAFT` 的验收套件。
- 不允许修改已绑定的 Agent、WorkflowVersion 和 nodeId。

### 9.4 确认验收套件

`PUT /api/eval-suites/{suiteId}/confirm`

规则：

- 将 `status=DRAFT` 转为 `CONFIRMED`。
- 确认前必须至少存在一个可计入正式通过率的验收用例。

### 9.5 归档验收套件

`PUT /api/eval-suites/{suiteId}/archive`

规则：

- 将验收套件标记为 `ARCHIVED`。
- 归档后不允许再创建新的验收运行。

### 9.6 运行验收套件

`POST /api/eval-suites/{suiteId}/runs`

请求体建议：

```json
{
  "caseIds": [101, 102, 103],
  "includeUnconfirmed": false
}
```

返回：

```json
{
  "evalRunId": "eval_20260527_000001",
  "runId": "run_20260527_000010",
  "suiteId": 201,
  "status": "FAILED",
  "passRate": 66.67,
  "totalCaseCount": 3,
  "passedCaseCount": 2,
  "failedCaseCount": 1,
  "summary": "存在 1 条关键用例失败"
}
```

说明：

- 未确认用例默认不计入正式通过率。
- 关键用例失败时应使验收结果明确失败。
- `evalRunId` 是对外验收运行标识，对应数据库 `eval_run.run_no`，不是数据库自增主键。
- `runId` 是本次验收同步创建的 `AgentRun(runType=EVAL)` 的对外运行标识，对应数据库 `agent_run.run_no`。
- 每次正式验收运行都必须同步创建 `AgentRun(runType=EVAL)`，并写入 `eval_run.agent_run_id`。
- 只有 `status=CONFIRMED` 的验收套件可以执行正式验收。

### 9.7 查询验收用例

`GET /api/eval-suites/{suiteId}/cases`

查询参数：

- `page`
- `pageSize`
- `confirmStatus`
- `critical`
- `keyword`

### 9.8 从 NodeRun 生成验收用例

`POST /api/node-runs/{nodeRunId}/eval-cases`

请求体建议：

```json
{
  "suiteId": 201,
  "title": "基于历史运行生成的摘要用例",
  "description": "从一次成功调试运行复制而来"
}
```

规则：

- 后端必须复制 NodeRun 输入作为用例输入。
- 后端必须复制 NodeRun 输出作为参考答案。
- 默认生成 `confirmStatus=AI_DRAFT_PENDING`。
- 返回体中必须包含 `sourceRunId`、`sourceNodeRunId`、`sourceWorkflowVersionId` 和 `sourceNodeId`。
- 其中 `sourceRunId` 对应数据库 `agent_run.run_no`，`sourceNodeRunId` 对应数据库 `node_run.id`；数据库内部仍写入 `eval_case.source_agent_run_id -> agent_run.id` 作为外键来源。

### 9.9 创建验收用例

`POST /api/eval-suites/{suiteId}/cases`

请求体建议：

```json
{
  "caseNo": "case_001",
  "title": "普通摘要输入",
  "input": {
    "question": "请总结这段文本"
  },
  "referenceAnswer": {
    "summary": "参考摘要"
  },
  "assertions": [
    {
      "type": "JSON_PATH_EXISTS",
      "path": "$.summary"
    }
  ],
  "scoreRule": {
    "enabled": true,
    "modelOfferingKey": "openai.gpt_4_1_mini",
    "temperature": 0,
    "promptTemplate": "请根据输入、参考答案、实际输出和断言结果给出评分 JSON：{payload}"
  },
  "critical": false,
  "description": "验证摘要字段存在"
}
```

规则：

- 用户创建的用例默认 `confirmStatus=USER_CREATED`。
- AI 辅助生成的用例进入保存流程时，默认 `confirmStatus=AI_DRAFT_PENDING`，用户确认前不计入正式通过率。
- 用例详情应返回来源字段；手工创建时来源字段为空。
- `scoreRule` 为空或 `enabled=false` 时不执行 LLM 辅助评分。
- `scoreRule.modelOfferingKey` 可空；为空时使用 Agent 默认模型供应项。
- `scoreRule.model` 是旧字段，只允许迁移读取，不属于新接口契约。

### 9.10 更新验收用例

`PUT /api/eval-suites/{suiteId}/cases/{caseId}`

规则：

- 可更新标题、输入、参考答案、断言、评分规则、关键用例标记和描述。
- 更新评分规则时只能写入 `scoreRule.modelOfferingKey`，不得写入 `scoreRule.model`。
- 已归档用例不可更新。

### 9.11 查询验收用例详情

`GET /api/eval-suites/{suiteId}/cases/{caseId}`

### 9.12 确认验收用例

`PUT /api/eval-suites/{suiteId}/cases/{caseId}/confirm`

规则：

- 将 `AI_DRAFT_PENDING` 或用户未确认状态转换为 `USER_CONFIRMED`。
- 只有用户创建或确认的用例可以计入正式通过率。

### 9.13 归档验收用例

`PUT /api/eval-suites/{suiteId}/cases/{caseId}/archive`

规则：

- 归档后 `confirmStatus=ARCHIVED`。
- 归档用例不计入正式通过率。

### 9.14 查询验收运行列表

`GET /api/eval-suites/{suiteId}/runs`

查询参数：

- `page`
- `pageSize`
- `status`
- `startedAtFrom`
- `startedAtTo`

返回 `PageResponse<EvalRunListItemDto>`。

正式字段：

- `evalRunId`
- `runId`
- `suiteId`
- `workflowVersionId`
- `nodeId`
- `status`
- `passRate`
- `totalCaseCount`
- `passedCaseCount`
- `failedCaseCount`
- `startedAt`
- `finishedAt`
- `durationMs`

规则：

- 列表接口只返回验收运行摘要，不内嵌结果明细。
- `runId` 用于从验收详情跳转到统一运行时间线。

### 9.15 查询验收运行详情

`GET /api/eval-runs/{evalRunId}`

返回 `EvalRunDetailDto`：

```json
{
  "evalRunId": "eval_20260527_000001",
  "runId": "run_20260527_000010",
  "suite": {
    "suiteId": 201,
    "name": "摘要节点回归集"
  },
  "agent": {
    "agentId": 1,
    "agentKey": "summary-agent",
    "agentName": "摘要 Agent"
  },
  "workflowVersion": {
    "workflowVersionId": 12,
    "versionNo": 2
  },
  "node": {
    "nodeId": "node_1",
    "nodeName": "摘要节点",
    "nodeType": "LLM"
  },
  "status": "FAILED",
  "passThreshold": 80,
  "passRate": 66.67,
  "totalCaseCount": 3,
  "passedCaseCount": 2,
  "failedCaseCount": 1,
  "criticalFailedCaseCount": 1,
  "summary": "存在 1 条关键用例失败",
  "error": {
    "code": "EVAL_ASSERTION_FAILED",
    "message": "存在关键用例失败。"
  },
  "startedAt": "2026-05-27T10:00:00+08:00",
  "finishedAt": "2026-05-27T10:00:04+08:00",
  "durationMs": 4260,
  "historyComparison": {
    "previousEvalRunId": "eval_20260520_000003",
    "previousRunId": "run_20260520_000021",
    "previousPassRate": 100,
    "passRateDelta": -33.33,
    "passedCaseCountDelta": -1,
    "failedCaseCountDelta": 1
  },
  "failureSummary": [
    {
      "caseId": 101,
      "caseNo": "case_001",
      "title": "普通摘要输入",
      "critical": true,
      "reason": "$.summary 字段缺失"
    }
  ]
}
```

规则：

- `EvalRunDetailDto` 是 v1 正式冻结契约。
- 详情接口返回汇总、失败摘要和历史对比摘要，不内嵌完整结果明细列表。
- 结果明细统一通过 `GET /api/eval-runs/{evalRunId}/results` 查询。
- `runId` 必须始终返回，用于统一 Trace 页面跳转和排障。

### 9.16 查询验收运行结果明细

`GET /api/eval-runs/{evalRunId}/results`

查询参数：

- `page`
- `pageSize`
- `passed`
- `critical`
- `keyword`

返回 `PageResponse<EvalRunResultItemDto>`：

```json
{
  "items": [
    {
      "caseId": 101,
      "caseNo": "case_001",
      "title": "普通摘要输入",
      "confirmStatus": "USER_CONFIRMED",
      "critical": true,
      "passed": false,
      "input": {
        "question": "请总结这段文本"
      },
      "referenceAnswer": {
        "summary": "参考摘要"
      },
      "output": null,
      "assertionResults": [
        {
          "type": "JSON_PATH_EXISTS",
          "passed": false,
          "message": "$.summary 字段缺失"
        }
      ],
      "scoreResult": null,
      "errorMessage": "模型输出未通过断言。",
      "durationMs": 1280
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

规则：

- 结果明细是分页接口。
- 每条结果项返回完整排障所需字段，前端不得再回表拼接用例输入、参考答案或断言结果。

### 9.17 查询验收历史对比

`GET /api/eval-suites/{suiteId}/run-history`

返回 `PageResponse<EvalRunHistoryItemDto>`：

```json
{
  "items": [
    {
      "evalRunId": "eval_20260527_000001",
      "runId": "run_20260527_000010",
      "status": "FAILED",
      "passRate": 66.67,
      "totalCaseCount": 3,
      "passedCaseCount": 2,
      "failedCaseCount": 1,
      "criticalFailedCaseCount": 1,
      "startedAt": "2026-05-27T10:00:00+08:00",
      "finishedAt": "2026-05-27T10:00:04+08:00",
      "durationMs": 4260,
      "passRateDeltaFromPrevious": -33.33,
      "passedCaseCountDeltaFromPrevious": -1,
      "failedCaseCountDeltaFromPrevious": 1
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

规则：

- 历史对比接口是分页接口。
- 结果按 `startedAt desc` 排序。
- 每条历史项直接携带相对上一条运行的差异值，前端不得再自行推导第二套比较逻辑。

## 10. 系统设置

### 10.1 查询设置

`GET /api/settings`

返回建议为 key-value 列表，例如：

```json
{
  "items": [
    {
      "settingKey": "agent.studio.runtime.default-llm-timeout-seconds",
      "settingValue": "120",
      "valueType": "NUMBER",
      "editable": true,
      "description": "LLM 节点默认超时",
      "source": "SYSTEM_SETTING"
    }
  ]
}
```

规则：

- 只返回 v1 白名单中的运行限制设置键。
- `source` 只能是 `SYSTEM_SETTING` 或 `APPLICATION_CONFIG`。
- `settingValue` 表示当前生效值，不要求调用方自行再做优先级合并。
- 模型供应商 API Key 和 `agent.studio.trace.persist-full-model-content` 不通过该接口返回。

### 10.2 更新设置

`PUT /api/settings`

请求体建议：

```json
{
  "items": [
    {
      "settingKey": "agent.studio.runtime.default-llm-timeout-seconds",
      "settingValue": "120",
      "valueType": "NUMBER"
    }
  ]
}
```

规则：

- 只允许更新以下白名单键：
  - `agent.studio.runtime.default-agent-timeout-seconds`
  - `agent.studio.runtime.default-llm-timeout-seconds`
  - `agent.studio.runtime.default-java-method-timeout-seconds`
  - `agent.studio.runtime.default-external-agent-timeout-seconds`
  - `agent.studio.runtime.default-max-steps`
  - `agent.studio.runtime.default-max-agent-call-depth`
- 只能更新可编辑配置。
- 值类型必须与声明类型一致。
- 不可编辑项必须返回明确错误。
- `agent.studio.runtime.default-timeout-seconds` 不是 v1 合法设置键，必须拒绝写入。
- `agent.studio.openai.default-model` 只作为旧配置迁移来源，不属于该接口可编辑范围。
- 模型供应商 API Key 和 `agent.studio.trace.persist-full-model-content` 不属于该接口可编辑范围。

## 11. REST 设计结论

v1 的 REST 设计核心是：

1. 以 Agent 为主入口。
2. 以发布版本作为正式运行边界。
3. 以 Schema 作为结构契约。
4. 以 Run 和 Trace 作为排障依据。
5. 以 OpenAPI 作为前端类型唯一来源。
