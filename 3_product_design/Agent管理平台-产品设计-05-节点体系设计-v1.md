# Agent 管理平台 - 05 节点体系设计 v1

## 1. 节点体系定位

节点是工作流中的最小可配置能力单元。V1 中所有 Agent 能力都通过节点表达，包括模型调用、程序方法调用、外部 Agent 调用、工具调用、条件分支、审核、总结和内部 Agent 调用。

## 2. V1 节点清单

| 节点 | 职责 | 输出 |
|---|---|---|
| START | 工作流入口。 | 初始上下文。 |
| END | 工作流出口。 | AgentRunResult。 |
| LLM | 通过已注册模型供应项调用 OpenAI-compatible 模型生成文本或结构化结果。 | 模型输出。 |
| CONDITION | 根据上下文决定下一条路径。 | 命中分支。 |
| JAVA_METHOD | 调用平台显式注册的 Java 方法。 | Java 方法返回结果。 |
| EXTERNAL_AGENT | 调用 Codex、OpenCode、自定义 CLI 或 HTTP Agent。 | 外部 Agent 最终结果。 |
| TOOL | 调用平台已注册工具。 | 工具返回结果。 |
| REVIEW | 审核输入或上游输出。 | 审核结论、原因、评分。 |
| SUMMARY | 总结上下文或上游结果。 | 总结文本或结构化摘要。 |
| AGENT_CALL | 调用平台内部已发布 Agent。 | 被调用 Agent 的最终结果。 |

## 3. START 节点

职责：

- 定义 Agent 调用入口。
- 定义输入 Schema。
- 为页面调试表单和 API 输入提供结构约束。

关键配置：

- inputSchema
- 示例输入
- 初始上下文写入位置

## 4. END 节点

职责：

- 定义 Agent 最终输出。
- 将工作流上下文映射为 AgentRunResult。

关键配置：

- outputSchema
- 输出字段映射

## 5. LLM 节点

职责：

- 通过模型供应项调用 OpenAI-compatible 供应商。
- 支持普通文本输出。
- 支持配置 outputSchema 后进行结构化输出校验。
- 支持允许工具列表。

关键配置：

- 系统提示词
- 用户提示词模板
- 模型供应项
- 温度
- inputSchema
- outputSchema
- 可用工具列表
- 超时时间

## 6. CONDITION 节点

职责：

- 基于上下文执行条件判断。
- 决定下一条执行边。

关键配置：

- 条件表达式
- 默认分支
- 条件说明

规则：

- 必须有默认分支。
- 条件结果未命中显式分支时，走默认分支。这属于正常路由行为，不属于失败策略。
- 条件表达式求值异常、输入结构不满足或类型不匹配时，按节点失败处理。
- V1 中上述异常默认使用 `FAIL_FAST`。

## 7. JAVA_METHOD 节点

职责：

- 确定性调用后端显式注册的 Java 方法。
- 用于业务查询、规则校验、数据转换、内部服务调用。

关键配置：

- Java 方法标识
- 入参映射
- 出参写回
- 超时时间
- 失败策略

执行流程：

```text
读取 JavaMethodDefinition
  -> 根据 inputMapping 提取输入
  -> 校验 inputSchema
  -> JSON 转 Java POJO
  -> 调用 Java 方法
  -> POJO 转 JSON
  -> 校验 outputSchema
  -> 写回工作流上下文
  -> 写入 TraceEvent
```

安全原则：

- 只能调用已注册方法。
- 禁止用户输入任意类名、方法名或脚本。
- 方法入参和出参必须可序列化为 JSON。

## 8. EXTERNAL_AGENT 节点

职责：

- 轻量调用平台外部 Agent。
- 支持 Codex CLI、OpenCode CLI、自定义 CLI、自定义 HTTP Agent。

基础配置：

- 适配器
- 调用方式
- 提示词模板
- 输入映射
- 输出写回位置

运行配置：

- 工作目录，可选。
- 会话策略，可选。
- 超时时间，有默认值。
- outputSchema，可选。

高级配置：

- 是否采集 stdout。
- 是否采集 stderr。
- 是否采集 Git diff。
- 环境变量策略。
- 沙箱策略。
- 产物采集策略。

最小约束：

- 必须指定调用方式。
- 必须记录最终结果。
- 失败必须记录错误摘要。
- 必须有默认超时。

不强制：

- 不强制工作目录。
- 不强制完整保存 stdout 和 stderr。
- 不强制保存 Git diff。
- 不强制 outputSchema。
- 不强制环境变量白名单。
- 不强制禁止外部 Agent 高权限模式。

## 9. TOOL 节点

职责：

- 调用平台已注册工具。
- 可由工作流确定调用，也可以作为 LLM 节点允许调用的工具集合。

关键配置：

- 工具选择
- 入参映射
- 失败策略
- 输出写回

## 10. REVIEW 节点

职责：

- 对输入或上游结果进行审核。
- 本质是特殊 LLM 节点。

关键配置：

- 审核标准
- 通过条件
- 失败提示
- 是否允许继续
- 输出 Schema

输出：

- passed
- reason
- score
- suggestions

## 11. SUMMARY 节点

职责：

- 总结上下文或上游结果。
- 本质是特殊 LLM 节点。

关键配置：

- 总结目标
- 字数限制
- 输出格式
- outputSchema

## 12. AGENT_CALL 节点

职责：

- 同步调用平台内部另一个已发布 Agent。

关键配置：

- 目标 Agent
- 输入映射
- 输出写回
- 超时时间
- 失败策略

规则：

- 目标 Agent 必须已发布。
- 不能直接调用当前 Agent 自己。
- 必须限制最大调用深度。
- 必须保存父子 AgentRun 关系。

## 13. 节点通用失败策略

V1 默认失败策略为 `FAIL_FAST`。

V1 当前确认的规则：

- 节点执行异常时立即终止当前运行。
- CONDITION 节点未命中显式分支时走默认分支，不视为失败。
- CONDITION 节点条件表达式求值异常时按失败处理。
- V1 中所有节点失败都必须写入 NodeRun 和 TraceEvent。

以下策略保留为后续扩展方向，但不属于 V1 已确认能力：

- 使用默认值继续
- 重试
- 走失败分支
- 空输出继续
