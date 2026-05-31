# 09-Agent 协作与外部 Agent 架构设计 v1

## 1. AGENT_CALL 定位

AGENT_CALL 用于平台内部 Agent 之间的同步调用。它调用的是已发布的内部 Agent，不是外部工具，也不是模型自由决定的 Tool Calling。

## 2. AGENT_CALL 组件

| 组件 | 职责 |
|------|------|
| AgentCallNodeExecutor | 执行 AGENT_CALL 节点 |
| AgentCallApplicationService | 校验目标 Agent、调用深度、超时并创建子运行 |
| AgentMessage | 记录父子运行和 Agent 间输入输出 |

## 3. AGENT_CALL 流程

```text
进入 AGENT_CALL 节点
  -> 校验目标 Agent 已发布
  -> 校验目标 Agent 不是当前 Agent 自己
  -> 校验调用深度 <= 3
  -> 根据 inputMapping 生成目标 Agent 输入
  -> 创建子 AgentRun，runType=AGENT_CALL，写 parentRunId
  -> 同步执行目标 Agent 当前发布版本
  -> 将子运行输出写回父 WorkflowContext
  -> 写 AgentMessage、NodeRun、AGENT_CALL TraceEvent
```

## 4. AGENT_CALL 失败规则

- 目标 Agent 未发布：发布校验失败。
- 目标 Agent 运行失败：默认当前节点失败。
- 子运行超时：当前节点 TIMEOUT 或 FAILED。
- 父运行超时：子运行应标记 CANCELED 或 FAILED。
- 运行详情必须支持父子运行跳转。

## 5. EXTERNAL_AGENT 定位

EXTERNAL_AGENT 用于接入平台之外的 Agent，例如 Codex、OpenCode、自研 CLI Agent 或 HTTP Agent。平台只负责统一调用、结果写回、错误可见和必要调试记录。

## 6. 外部 Agent 适配器

| 适配器 | 说明 |
|--------|------|
| Codex CLI | 调用本机 Codex 命令行 |
| OpenCode CLI | 调用本机 OpenCode 命令行 |
| Custom CLI | 调用用户注册的命令行 Agent 或脚本 |
| Custom HTTP | 调用远程或本地 HTTP Agent 服务 |

## 7. EXTERNAL_AGENT 组件

| 组件 | 职责 |
|------|------|
| ExternalAgentDefinition | 外部 Agent 适配器定义 |
| ExternalAgentRegistry | 维护可用适配器 |
| ExternalAgentAdapter | 统一适配器接口 |
| CliExternalAgentAdapter | 执行 CLI 命令，收集退出码、stdout、stderr |
| HttpExternalAgentAdapter | 调用 HTTP 服务，收集状态码和响应 |
| ExternalAgentNodeExecutor | 执行节点输入映射、调用适配器、结果写回 |

## 8. EXTERNAL_AGENT 配置

| 配置层级 | 配置项 | 是否必填 |
|----------|--------|----------|
| 基础配置 | 适配器、提示词模板、输入映射、输出写回位置 | 是 |
| 运行配置 | 工作目录、超时时间、输出 Schema | 超时时间有默认值，其他可选 |
| 调试配置 | 采集 stdout、采集 stderr、采集 Git diff、会话 ID | 否 |

## 9. 第一批适配器命令参数

### 9.1 统一调用参数

| 参数 | 说明 |
|------|------|
| prompt | 渲染后的提示词全文 |
| workingDirectory | 工作目录，未配置时使用平台默认工作目录 |
| timeoutSeconds | 超时时间，默认 600 秒 |
| captureStdout | 是否采集 stdout |
| captureStderr | 是否采集 stderr |
| captureGitDiff | 是否采集 Git diff |
| environment | 可选环境变量覆盖 |

### 9.2 Codex CLI

| 字段 | 默认值 / 规则 |
|------|---------------|
| adapterType | CODEX_CLI |
| command | `codex` |
| arguments | `exec --json --full-auto "{prompt}"` |
| workingDirectory | 节点配置或平台默认工作目录 |
| resultSource | 优先读取 stdout 最后一条结构化结果；无法解析时使用 stdout 文本摘要 |

实际命令模板在实现时必须支持参数数组，禁止简单字符串拼接执行。

### 9.3 OpenCode CLI

| 字段 | 默认值 / 规则 |
|------|---------------|
| adapterType | OPENCODE_CLI |
| command | `opencode` |
| arguments | `run --format json "{prompt}"` |
| workingDirectory | 节点配置或平台默认工作目录 |
| resultSource | 优先读取 JSON 输出；无法解析时使用 stdout 文本摘要 |

### 9.4 Custom CLI

| 字段 | 默认值 / 规则 |
|------|---------------|
| adapterType | CUSTOM_CLI |
| command | 用户注册命令 |
| arguments | 用户注册参数模板，支持 `{prompt}`、`{inputJson}` 占位 |
| workingDirectory | 节点配置或平台默认工作目录 |
| resultSource | stdout、文件路径或固定 JSON 字段，由适配器定义指定 |

### 9.5 Custom HTTP

| 字段 | 默认值 / 规则 |
|------|---------------|
| adapterType | CUSTOM_HTTP |
| method | POST |
| url | 用户注册 URL |
| headers | 用户注册 headers，敏感值不回显 |
| body | `{ "prompt": "...", "input": {} }` |
| timeoutSeconds | 默认 600 秒 |
| resultSource | HTTP 响应 JSON 或文本 |

### 9.6 敏感 header 与 secret 更新语义

- 普通详情接口中，敏感 header 只返回 `headerName` 和 `secretConfigured` 等元信息，不返回明文，也不返回可回传的掩码占位值。
- 创建外部 Agent 时，允许通过只写字段一次性写入敏感 header secret；响应体和后续详情接口都不回显 secret。
- `PUT /api/external-agents/{adapterId}` 只更新结构化配置和非敏感字段，不要求前端重新传递历史 secret，也不会因为请求体缺少 secret 而覆盖旧值。
- 敏感 header secret 必须通过 `PUT /api/external-agents/{adapterId}/secrets` 单独更新；`items` 执行覆盖写入，`clearHeaderNames` 执行显式清空，未出现的 secret 保持不变。
- 删除敏感 header 时，后端必须同步删除已保存 secret；重命名按“删除旧 header + 新增新 header”处理，必须重新写入 secret。
- 如果定义了敏感 header 但 `secretConfigured=false`，则连接测试和正式运行都必须在真正发起外部请求前失败，并返回明确的配置错误。

## 10. EXTERNAL_AGENT 流程

```text
进入 EXTERNAL_AGENT 节点
  -> 根据 inputMapping 提取输入
  -> 渲染 promptTemplate
  -> 读取 ExternalAgentDefinition
  -> 选择 CLI 或 HTTP 适配器
  -> 按默认或节点超时执行外部 Agent
  -> 收集最终结果、退出码或 HTTP 状态、错误摘要
  -> 如配置 outputSchema，则校验输出
  -> 根据 outputMapping 写回 WorkflowContext
  -> 写 NodeRun、EXTERNAL_AGENT_CALL TraceEvent
```

## 11. 安全边界

- EXTERNAL_AGENT 属于 v1 架构范围，但平台不做完整沙箱托管。
- 必须有默认超时。
- 失败必须记录适配器、退出码或 HTTP 状态、错误摘要。
- stdout/stderr/Git diff 采集是可选调试能力。
- v1 部署边界仍是本机或内网信任环境。
