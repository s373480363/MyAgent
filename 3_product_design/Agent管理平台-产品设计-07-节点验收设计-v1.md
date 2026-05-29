# Agent 管理平台 - 07 节点验收设计 v1

## 1. 设计定位

节点验收用于验证单个 LLM 类节点的提示词、模型参数和结构化输出是否符合用户定义的业务标准。

V1 不做完整工作流验收。完整工作流涉及多节点、多分支、Java 方法、外部 Agent、工具和内部 Agent 调用，复杂度较高，应后续单独设计。

## 2. 核心原则

| 原则 | 说明 |
|---|---|
| 用户主导 | 正式验收集由用户定义或确认。 |
| AI 辅助 | AI 可以起草用例、补充边界输入、解释失败原因。 |
| 不自动生成权威标准 | AI 生成内容默认待确认，不能直接计入正式通过率。 |
| 确定性优先 | Schema 校验和规则断言优先于 LLM 评分。 |
| 节点级优先 | V1 聚焦 LLM、REVIEW、SUMMARY 节点。 |

## 3. V1 验收对象

| 对象 | 是否支持 | 说明 |
|---|---|---|
| LLM 节点 | 支持 | 验证提示词、模型参数、输入输出结构。 |
| REVIEW 节点 | 支持 | 验证审核结论、理由和评分。 |
| SUMMARY 节点 | 支持 | 验证摘要质量和结构化输出。 |
| JAVA_METHOD 节点 | 不作为重点 | 更适合传统单元测试和集成测试。 |
| EXTERNAL_AGENT 节点 | 不作为重点 | 行为由外部工具决定，V1 只记录调用结果。 |
| 完整工作流 | 不支持 | 后续单独设计。 |

## 4. EvalSuite

EvalSuite 是节点验收集，绑定到具体 Agent、工作流版本和节点。

建议字段：

- id
- agentId
- workflowVersionId
- nodeId
- name
- goal
- passThreshold
- status
- createdAt
- updatedAt

状态：

- DRAFT
- CONFIRMED
- ARCHIVED

## 5. EvalCase

EvalCase 是单条验收用例。

建议字段：

- id
- evalSuiteId
- name
- input
- referenceAnswer
- assertionRules
- judgeRules
- isCritical
- confirmationStatus
- createdAt
- updatedAt

确认状态：

- USER_CREATED
- USER_CONFIRMED
- AI_DRAFT_PENDING
- ARCHIVED

## 6. EvalRun

EvalRun 是一次验收执行。

建议字段：

- id
- evalSuiteId
- workflowVersionId
- nodeId
- model
- startedAt
- finishedAt
- status
- totalCases
- passedCases
- failedCases
- passRate
- summary

## 7. 断言类型

| 类型 | 示例 |
|---|---|
| Schema 校验 | 输出必须符合 outputSchema。 |
| 字段等于 | `$.category == "退款"` |
| 包含 | 回复必须包含指定关键内容。 |
| 不包含 | 回复不能包含“无法处理”。 |
| 正则匹配 | 订单号必须匹配指定格式。 |
| 数值范围 | `$.score >= 80` |
| 枚举校验 | `riskLevel` 只能是 LOW、MEDIUM、HIGH。 |
| JSONPath 断言 | 指定路径的字段满足条件。 |
| LLM 评分 | 摘要准确性、礼貌性、完整性等主观质量。 |

## 8. 验收运行流程

```text
用户选择 LLM 类节点
  -> 创建或选择 EvalSuite
  -> 用户维护并确认 EvalCase
  -> 平台逐条执行节点
  -> 校验 outputSchema
  -> 执行确定性断言
  -> 可选执行 LLM 评分
  -> 生成 EvalRun
  -> 展示通过率、失败用例和失败原因
```

## 9. 从历史运行创建用例

平台应支持从 NodeRun 生成待确认 EvalCase：

- 复制节点输入为用例输入。
- 复制节点输出为参考答案。
- 用户补充断言规则。
- 状态统一默认为 `AI_DRAFT_PENDING`。
- 自动生成的 EvalCase 必须记录来源信息：`agentRunId`、`nodeRunId`、`workflowVersionId`、`nodeId`。
- 用户确认后才进入正式验收集。

## 10. AI 辅助边界

AI 可以：

- 根据节点说明生成用例草稿。
- 根据 inputSchema 和 outputSchema 补充边界输入。
- 根据失败输出解释失败原因。
- 建议新增断言规则。

AI 不可以：

- 自动创建正式 benchmark。
- 自动决定节点是否合格。
- 未经用户确认就把用例计入正式通过率。
- 自动修改提示词并发布。

## 11. 验收结果展示

验收结果应展示：

- 总用例数
- 通过数
- 失败数
- 通过率
- 关键用例失败情况
- 每条失败用例的输入、输出、失败断言、错误原因
- 历史通过率对比
