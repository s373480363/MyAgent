# Agent管理平台 V1 完整正式验收报告 v1

## 1. 报告信息

| 项目 | 内容 |
|------|------|
| 验收对象 | Agent 管理平台 V1 |
| 验收范围 | 完整 V1 |
| 验收版本 | 2026-06-02 当前工作区源码与数据库状态 |
| 验收环境 | Windows 本机，PostgreSQL Docker 容器，后端 `8080`，前端 `5173` |
| 验收时间 | 2026-06-02 |
| 测试负责人 | Codex 测试验收 |
| 结论 | 不通过 |

## 2. 验收依据

- `8_change_list\agent-management-platform-v1\test_steps.md`
- `8_change_list\agent-management-platform-v1\test_steps.md`
- `8_change_list\agent-management-platform-v1\test_steps.md`
- `8_change_list\agent-management-platform-v1\test_steps.md`
- `8_change_list\agent-management-platform-v1\test_result\02-步骤01工程骨架与公共基线初始化验证记录-v1.md` 至 `8_change_list\agent-management-platform-v1\test_result\11-步骤17测试联调与发布准备收口验证记录-v1.md`
- `8_change_list\agent-management-platform-v1\test_result\12-V1二轮正式验收遗留问题复验记录-v1.md`

## 3. 环境与资源确认

| 资源 | 结果 | 说明 |
|------|------|------|
| PostgreSQL | 通过 | 容器 `myagent-v1-acceptance-postgres`，端口 `15432 -> 5432` |
| 后端服务 | 通过 | `http://127.0.0.1:8080`，`GET /api/ping` 正常，`/v3/api-docs` 返回 `3.1.0` |
| 前端服务 | 通过 | `http://127.0.0.1:5173` 可访问 |
| OpenAPI 输出 | 通过 | 前端 `npm run openapi:check` 通过 |
| OpenAI API Key | 已配置但环境受阻 | 当前会话已配置，用于连通性冒烟；未在任何报告正文记录明文 |
| Codex CLI | 通过 | `codex-cli 0.116.0` |
| OpenCode CLI | 通过 | `1.15.13` |
| Java 方法测试 Bean | 不满足 | 当前数据库 `java_method_definition = 0` |
| Tool 测试实现 | 不满足 | 当前数据库 `tool_definition = 0` |
| 外部 Agent 测试替身 | 通过 | 已存在 1 条成功 HTTP stub 适配器 `adapterId=3` |

## 4. 本轮实际执行结果摘要

### 4.1 自动化质量闸门

- 后端 `mvn -q test` 通过
- 前端 `npm test -- --run` 通过
- 前端 `npm run build` 通过
- 前端 `npm run openapi:check` 通过

### 4.2 已确认通过的关键项

- Agent 创建后自动生成草稿版本；未发布 Agent 正式运行返回 `TARGET_AGENT_NOT_PUBLISHED`
- Schema 锁定与新版本规则成立；锁定 `ACTIVE` Schema 不可编辑
- 正式运行失败语义成立：输入 Schema 非法时，接口仍返回 `success=true`、`runId`、`status=FAILED`，并带字段路径 `$.text`
- Run / NodeRun / Trace 链路可回放；历史成功 Run `run_20260531205516_d30f894a` 可完整看到 `MODEL_REQUEST`、`MODEL_RESPONSE`、`SCHEMA_VALIDATION`
- 外部 Agent 敏感 header 不明文回显；缺失 secret 时返回明确中文错误；本地成功 stub 适配器调用通过
- Eval 状态机与审计链路成立：
  - 空套件确认失败，返回 `INVALID_ARGUMENT`
  - `USER_CREATED` 正式用例可驱动套件确认
  - `AI_DRAFT_PENDING` 可由历史成功 `nodeRunId=2` 生成，且保留 `sourceRunId/sourceNodeRunId/sourceWorkflowVersionId/sourceNodeId`
  - `includeUnconfirmed=true` 的正式 Eval 运行请求被拒绝
  - AI 草稿补齐断言后可转为 `USER_CONFIRMED`
  - 正式 EvalRun 会创建 `evalRunId` 与关联 `runId`，并在 Run 详情中可看到 `EVAL_CASE_RESULT`
- 设置项查询未返回 OpenAI Key；系统设置白名单展示正常
- `/evals`、`/eval-runs/{evalRunId}` 页面可按最新 DTO 正常渲染，无旧契约字段阻塞错误

### 4.3 已确认不满足或受阻的关键项

- 当前交付环境没有任何 Java 方法主数据，也没有任何 Tool 主数据，导致 `JAVA_METHOD` / `TOOL` 节点无法执行正式验收
- 当前机器到 `https://api.openai.com` 的真实 HTTPS 握手失败，导致 LLM 真实连通冒烟与基于真实模型的正式成功路径无法成立
- 不可达外部 HTTP Agent 的失败摘要仍返回 `调用外部 HTTP Agent 失败：null`，失败不可定位
- Eval 页面中 `ARCHIVED` 用例仍展示可点击的“编辑/确认”按钮，前后端状态语义不一致

## 5. 质量闸门检查

| 质量闸门 | 结果 | 说明 |
|----------|------|------|
| 完整 V1 必做范围已交付 | 不通过 | `java_method_definition = 0`，`tool_definition = 0`，V1 必测节点无法完整验收 |
| 版本不可变规则成立 | 通过 | Agent / WorkflowVersion / Schema 的版本与锁定规则已复验 |
| 正式运行不能绕过当前发布版本 | 通过 | 未发布 Agent 正式运行被前置拒绝 |
| 运行失败仍返回 `runId` | 通过 | 输入 Schema 非法运行、Eval 运行均保留运行记录语义 |
| `runId` / `evalRunId` / `nodeRunId` 语义一致 | 通过 | EvalRun 与 AgentRun、NodeRun 的编号映射清晰稳定 |
| Trace 可定位输入、失败节点与失败事件 | 通过 | Eval 运行中可看到 `MODEL_REQUEST`、`NODE_ERROR`、`EVAL_CASE_RESULT` |
| Schema 校验返回字段路径 | 通过 | 非法输入运行返回 `$.text` |
| 前端类型来源于 OpenAPI | 通过 | `npm run openapi:check` 通过，前端页面按最新契约渲染 |
| AI_DRAFT_PENDING 不计入正式通过率 | 通过 | `includeUnconfirmed=true` 被拒绝；正式套件确认前要求正式用例 |
| OpenAI Key / 敏感 Header 不明文回显 | 通过 | 设置接口未返回 Key；外部 Agent 详情只返回 `secretConfigured` |
| 外部 Agent 失败摘要可定位 | 不通过 | 不可达 HTTP 适配器仍返回 `errorMessage=\"调用外部 HTTP Agent 失败：null\"` |
| LLM 真实连通冒烟成立 | 受阻 | 环境到 OpenAI 官方地址握手失败，无法完成真实成功链路确认 |

说明：

- 按测试计划口径，只要存在任一质量闸门不满足，本次完整 V1 验收结论即为“不通过”。

## 6. 关键执行记录

### 6.1 主数据与数据库事实

2026-06-02 数据库实查结果：

| 表 | 数量 |
|----|------|
| `java_method_definition` | `0` |
| `tool_definition` | `0` |
| `external_agent_definition` | `3` |
| `agent_definition` | `2` |
| `workflow_version` | `4` |
| `schema_definition` | `3` |
| `eval_suite` | `1` |
| `eval_case` | `2` |
| `eval_run` | `1` |

### 6.2 运行与验收关键 ID

- 历史成功正式运行：`run_20260531205516_d30f894a`
- 历史成功目标节点：`nodeRunId=2`，`nodeId=llm`
- 输入校验失败运行：`run_20260602165539_b6f2cc4c`
- EvalSuite：`suiteId=1`
- USER_CREATED 用例：`caseId=1`
- 从历史 NodeRun 生成的 AI 草稿用例：`caseId=2`
- 正式 EvalRun：`eval_20260602171314_959e3c01`
- Eval 关联 AgentRun：`run_20260602171314_8b7c9a3b`

### 6.3 Eval 正式运行观察

- 正式 EvalRun 请求返回：
  - `success=true`
  - `status=FAILED`
  - `passRate=0.00`
  - `totalCaseCount=1`
- EvalRun 详情返回：
  - `failureSummary[0].reason = 节点执行超过运行或节点超时。`
- 对应 AgentRun 详情返回：
  - `runType=EVAL`
  - `evalRunId=eval_20260602171314_959e3c01`
  - `nodeRunId=9`
  - `nodeRun.status=TIMEOUT`
  - Trace 含 `MODEL_REQUEST`、`NODE_ERROR`、`EVAL_CASE_RESULT`

### 6.4 真实 OpenAI 连通性环境事实

- 后端日志显示对 `https://api.openai.com/v1/chat/completions` 的调用发生：
  - `SSLHandshakeException: Remote host terminated the handshake`
  - `Connection reset`
- 进程外直连探测同样失败：
  - `Invoke-WebRequest https://api.openai.com` 超时
  - `Invoke-WebRequest https://api.openai.com/v1/models` 超时

判定：

- 该问题应归类为“当前验收环境外联阻断”，不能直接归因为框架业务逻辑缺陷
- 但它客观上阻断了本次完整 V1 对 LLM 真实成功链路的正式验收

## 7. 阻断项与缺陷清单

| 编号 | 类型 | 优先级 | 标题 | 当前状态 | 说明 |
|------|------|--------|------|----------|------|
| BLK-001 | 交付阻断 | P0 | Java 方法与 Tool 测试主数据未交付 | 打开 | 当前数据库 `java_method_definition = 0`、`tool_definition = 0`，`JAVA_METHOD` / `TOOL` 节点无法完成完整 V1 正式验收 |
| BLK-002 | 环境阻断 | 阻断 | 当前机器到 OpenAI 官方地址 HTTPS 握手失败 | 打开 | 真实 LLM 成功链路无法在当前正式环境完成；不直接归因于框架缺陷 |
| BUG-001 | 产品缺陷 | P1 | 外部 HTTP Agent 不可达时失败摘要为 `null` | 打开 | `POST /api/external-agents/1/test` 返回 `errorMessage=\"调用外部 HTTP Agent 失败：null\"`，不满足“失败可定位” |
| BUG-002 | 产品缺陷 | P2 | Eval 页面中 ARCHIVED 用例仍显示可操作按钮 | 打开 | 前端展示“编辑/确认”按钮，但后端已拒绝更新；前后端状态语义不一致 |

## 8. 自动化测试结果

| 类型 | 命令 | 结果 |
|------|------|------|
| 后端测试 | `mvn -q test` | 通过 |
| 前端测试 | `npm test -- --run` | 通过 |
| 前端构建 | `npm run build` | 通过 |
| 契约闸门 | `npm run openapi:check` | 通过 |

## 9. 剩余风险

| 风险 | 等级 | 说明 | 建议 |
|------|------|------|------|
| LLM 真实成功链路未在当前正式环境闭环 | 高 | 现有验证只能证明失败路径与 Trace 语义，不能证明真实官方 OpenAI 成功路径 | 修复外联握手或提供可访问的合规网关后，重跑 LLM / REVIEW / SUMMARY 正向正式验收 |
| JAVA_METHOD / TOOL 节点未完成正式验证 | 高 | 当前不是“测试失败”，而是“交付前置未满足” | 由开发补齐至少 1 个启用的 Java 方法与 1 个启用的 Tool 后重跑对应 P0 用例 |
| Eval UI 状态控制与后端不完全一致 | 低 | 用户可看到实际不可执行的按钮 | 前端按 `confirmStatus=ARCHIVED` 禁用或隐藏编辑/确认操作 |

## 10. 最终结论

```text
本次完整 V1 正式验收结论：不通过

结论依据：
1. V1 必测范围中的 JAVA_METHOD / TOOL 主数据未交付，导致对应节点无法完成正式验收。
2. 当前验收环境到 OpenAI 官方地址的真实 HTTPS 握手失败，阻断了 LLM 真实成功链路的完整验收。
3. 外部 HTTP Agent 不可达时失败摘要仍返回 null，不满足“失败可定位”的质量要求。

是否允许发布：否

建议重新申请终验的前置条件：
1. 由开发补齐至少 1 个启用的 Java 方法与 1 个启用的 Tool。
2. 修复当前机器到 OpenAI 官方地址或等效合规网关的真实连通性。
3. 修复外部 Agent 失败摘要问题，并回归 Eval 页面归档用例按钮状态。
```
