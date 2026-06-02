# Agent管理平台 V1 完整正式验收复验报告 v1

## 1. 报告信息

| 项目 | 内容 |
|------|------|
| 验收对象 | Agent 管理平台 V1 |
| 验收范围 | 完整 V1 |
| 验收版本 | 2026-06-02 当前工作区源码与现有数据库状态 |
| 验收环境 | Windows 本机，PostgreSQL Docker 容器，后端 `8080`，前端 `5173` |
| 验收时间 | 2026-06-02 |
| 测试负责人 | Codex 测试验收 |
| 本轮结论 | 不通过 |

## 2. 本轮复验目标

本轮复验是在用户提供可用 Poe OpenAI-compatible API Key 并完成用户级持久环境变量配置后，对完整 V1 做一次真实链路复验，重点确认：

- 上一轮“官方 OpenAI 外联阻塞”是否可由 Poe 兼容网关替代收口
- 当前源码的自动化质量闸门是否仍然通过
- LLM 正向成功链路、结构化输出链路和失败链路是否真实可运行
- 前端关键页面与运行详情页是否仍可正常联调
- 完整 V1 的剩余阻塞项是否已经消失

## 3. 验收依据

- `9_test_plan\01-Agent管理平台V1测试验收计划-v1.md`
- `9_test_plan\02-Agent管理平台V1测试用例清单-v1.md`
- `9_test_plan\03-Agent管理平台V1独立测试方案-v1.md`
- `9_test_plan\04-Agent管理平台V1独立测试用例清单-v1.md`
- `12_test_result\13-Agent管理平台V1完整正式验收报告-v1.md`

## 4. 环境与资源确认

| 资源 | 结果 | 说明 |
|------|------|------|
| PostgreSQL | 通过 | 容器 `myagent-v1-acceptance-postgres`，端口 `15432 -> 5432` |
| 后端服务 | 通过 | 本轮重新以 Poe 环境变量启动，监听 `8080` |
| 前端服务 | 通过 | 当前源码 Vite dev server 监听 `5173` |
| Poe API Key | 通过 | 已配置到当前 Windows 用户级持久环境变量；报告不记录明文 |
| Poe 网关 | 通过 | `SPRING_AI_OPENAI_BASE_URL=https://api.poe.com` |
| 默认模型 | 通过 | 后端设置接口显示 `myagent.openai.default-model = gpt-5.4` |
| Codex CLI | 通过 | `codex-cli 0.116.0` |
| OpenCode CLI | 通过 | `1.15.13` |

## 5. 本轮实际执行结果摘要

### 5.1 自动化质量闸门

| 类型 | 命令 | 结果 |
|------|------|------|
| 后端测试 | `mvn -q test` | 通过 |
| 前端测试 | `npm test -- --run` | 通过 |
| 前端构建 | `npm run build` | 通过 |
| OpenAPI 闸门 | `npm run openapi:check` | 通过 |

补充观察：

- 前端生产构建仍有 Vite 大 chunk 提示，但不构成当前阻塞
- 浏览器控制台仍有 Ant Design 废弃用法告警，但未出现 `Failed to fetch`、旧契约字段错误或阻塞性运行时异常

### 5.2 Poe 真实 LLM 成功链路

本轮重新启动后端后，系统已真实继承 Poe 持久环境变量。通过现有正式发布 Agent：

- `agentKey = acceptance-agent-20260531205515`
- 发布版本 `workflowVersionId = 3`

执行正式运行：

- 请求：`POST /api/agents/acceptance-agent-20260531205515/runs`
- 输入：`{"input":{"text":"Acceptance smoke through Poe."}}`
- 结果：`success=true`
- 运行号：`run_20260602181916_8c80691b`
- 状态：`SUCCESS`
- 输出：

```json
{
  "ok": true,
  "summary": "The text says the acceptance smoke test was performed through Poe."
}
```

对应运行详情已确认：

- `START -> LLM -> END` 三个 `NodeRun` 全部成功
- `LLM` 节点输入 Schema 校验通过
- `LLM` 节点输出 Schema 校验通过
- `END` 节点输出 Schema 校验通过
- `Trace` 中存在 `MODEL_REQUEST`、`MODEL_RESPONSE`、`SCHEMA_VALIDATION`、`RUN_FINISHED`
- `MODEL_RESPONSE.rawText` 为纯 JSON 文本，可被当前实现直接解析

结论：

- 之前“官方 OpenAI Key 不可用导致 LLM 成功链路无法验收”的阻塞，已经在**当前 Poe 验收口径**下被关闭
- 当前项目在 Poe 兼容网关下，至少已被本轮真实复验证明可以跑通一条正式的结构化输出成功链路

### 5.3 失败链路与错误语义

对同一正式发布 Agent 使用非法输入：

- 请求：`POST /api/agents/acceptance-agent-20260531205515/runs`
- 输入：`{"input":{}}`
- 结果：`success=true`
- 运行号：`run_20260602182154_05632b62`
- 状态：`FAILED`
- 错误码：`SCHEMA_VALIDATION_FAILED`
- 错误字段路径：`$.text`

结论：

- 正式运行在节点执行失败时仍保留 `runId`
- 错误字段路径和错误分类语义符合测试计划要求

### 5.4 前端联调结果

本轮实际联调确认：

- `/agents` 页面可正常渲染 Agent 列表
- `/runs` 页面可正常渲染最新运行记录，包含本轮 Poe 成功运行
- `/runs/run_20260602181916_8c80691b` 可正常打开运行详情抽屉
- 运行详情 `Trace` 页签可正常展示并展开 `MODEL_RESPONSE.detail`
- `/settings` 页面只展示白名单配置项，未回显任何 API Key
- `/evals` 套件列表中，`ARCHIVED` 套件的套件级操作按钮已禁用

## 6. 本轮新增确认的剩余问题

### 6.1 BLK-001：JAVA_METHOD / TOOL 仍未满足完整 V1 交付前提

当前环境事实：

- `GET /api/java-methods?page=1&pageSize=20` 返回 `total=0`
- `GET /api/tools?page=1&pageSize=20` 返回 `total=0`

进一步代码复核事实：

- 主代码中未发现任何 `@RegisteredJavaMethod` 的正式测试 Bean；该注解仅出现在测试代码中
- `JavaMethodController` 与 `ToolController` 当前仅提供目录查询接口，测试侧无法通过正式管理接口自行补录这两类主数据

判定：

- 这不是“某条用例失败”，而是“完整 V1 必测范围前置资源仍未交付完整”
- 按测试计划口径，这一项仍然构成 **P0 交付阻塞**

### 6.2 BUG-001：外部 HTTP Agent 不可达时失败摘要仍不可定位

本轮复验：

- 请求：`POST /api/external-agents/1/test`
- 返回：`success=true`
- 返回体中 `data.errorMessage = "调用外部 HTTP Agent 失败：null"`

判定：

- 问题仍然存在
- 失败摘要仍然不满足“可定位”的质量要求
- 保持 **P1**

### 6.3 BUG-002：归档 EvalCase 行内按钮仍与后端语义不一致

本轮前端联调观察：

- `/evals` 套件列表中，`ARCHIVED` 套件的套件级按钮已经禁用
- 但进入归档套件详情后，归档用例 `case_20260602171301_667af1b2` 的行内 `编辑 / 确认` 按钮仍然可点击

判定：

- 套件级状态控制已修复，但用例级状态控制仍未完全收口
- 保持 **P2**

## 7. 质量闸门检查

| 质量闸门 | 结果 | 说明 |
|----------|------|------|
| 完整 V1 必做范围已交付 | 不通过 | `JAVA_METHOD / TOOL` 主数据仍为 `0`，对应节点无法完成正式全量验收 |
| 正式运行不能绕过当前发布版本 | 通过 | 本轮正式成功链路绑定 `workflowVersionId = 3` |
| 运行失败仍返回 `runId` | 通过 | 非法输入运行返回 `run_20260602182154_05632b62` |
| `runId / evalRunId / nodeRunId` 语义一致 | 通过 | 本轮 `RunDetail`、`NodeRun`、`Trace` 关联稳定 |
| Trace 可定位输入、模型请求、模型响应与结束事件 | 通过 | 本轮 Poe 成功链路已完整复核 |
| Schema 校验返回字段路径 | 通过 | 非法输入错误路径为 `$.text` |
| 前端类型来源于 OpenAPI | 通过 | `npm run openapi:check` 通过 |
| 敏感配置不明文回显 | 通过 | 设置接口与设置页面均未暴露 API Key |
| 外部 Agent 失败摘要可定位 | 不通过 | 仍返回 `调用外部 HTTP Agent 失败：null` |
| LLM 真实成功链路成立 | 通过 | 本轮 Poe 兼容网关下真实成功运行通过 |

## 8. 结论

```text
本轮完整 V1 正式验收复验结论：不通过

结论依据：
1. 上一轮“官方 OpenAI 外联阻塞”在当前 Poe 验收口径下已解决，真实 LLM 成功链路、结构化输出、Trace 与失败链路均已复验通过。
2. 但完整 V1 的 P0 前提仍未满足：JAVA_METHOD / TOOL 主数据仍未交付，导致对应节点无法完成正式全量验收。
3. 外部 HTTP Agent 失败摘要仍不可定位，未满足失败可排障的质量要求。

当前更准确的状态口径：
- “Poe 兼容网关下的真实 LLM 验收阻塞已解除。”
- “完整 V1 仍未达到最终通过条件。”
```

## 9. 建议的下一步

1. 由开发补齐至少 1 个正式可用的 `Java Method` 主数据、至少 1 个正式可用的 `Tool` 主数据，并确保对应实现已在主代码中交付。
2. 修复外部 HTTP Agent 不可达时的失败摘要问题，至少返回可识别的异常原因、目标地址或底层错误摘要。
3. 收口归档 `EvalCase` 的前端按钮状态，确保归档用例不再展示可执行的 `编辑 / 确认` 操作。
4. 完成以上三项后，再按 `9_test_plan` 对完整 V1 做最终全量终验。
