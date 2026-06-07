# Agent 管理平台 - 07 节点验收设计 v1

## 1. 设计定位

节点验收用于验证单个 LLM 类节点的提示词、模型参数和结构化输出是否符合用户定义的业务预期。

V1 不做完整工作流验收。完整工作流涉及多节点、多分支、Java 方法、外部 Agent、工具和内部 Agent 调用，复杂度较高，应后续单独设计。

节点验收的核心不是传统接口断言，而是通过用户预先写好的自然语言验收规则，判断目标 LLM 节点的实际输出是否达到预期能力。

## 2. 核心原则

| 原则 | 说明 |
|---|---|
| 用户主导 | 正式验收规则由用户定义或确认。 |
| Judge Rule 主导 | 自然语言 `judgeRule` 是 LLM 节点验收的主判断依据。 |
| 参考样例不是标准答案 | 从历史运行复制的输出只能作为 `referenceSample`，不能当作固定答案。 |
| 硬约束辅助 | `hardChecks` 只用于结构、格式、枚举、范围等硬约束，不承担自然语言质量判断。 |
| 可追溯 | 每次验收必须保存 judge 模型、规则、原始输出、结构化结论和失败原因。 |
| 节点级优先 | V1 聚焦 LLM、REVIEW、SUMMARY 节点。 |

## 3. V1 验收对象

| 对象 | 是否支持 | 说明 |
|---|---|---|
| LLM 节点 | 支持 | 验证提示词、模型参数、输出结构和自然语言质量。 |
| REVIEW 节点 | 支持 | 验证审核结论、理由、风险识别和输出质量。 |
| SUMMARY 节点 | 支持 | 验证摘要质量、事实覆盖、表达约束和结构化输出。 |
| JAVA_METHOD 节点 | 不作为重点 | 更适合传统单元测试和集成测试。 |
| EXTERNAL_AGENT 节点 | 不作为重点 | 行为由外部系统决定，V1 只记录调用结果。 |
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
- judgeModelOfferingKey
- judgeTemperature
- passThreshold
- status
- createdAt
- updatedAt

状态：

- DRAFT
- CONFIRMED
- ARCHIVED

说明：

- `judgeModelOfferingKey` 表示本验收集使用的 judge LLM 模型供应项。
- 同一 EvalSuite 内的 EvalCase 默认使用同一个 judge 模型，保证同一批验收使用同一评判口径。
- `passThreshold` 表示 EvalRun 的整体通过率阈值。

## 5. EvalCase

EvalCase 是单条 LLM 节点能力验收场景。

建议字段：

- id
- evalSuiteId
- caseNo
- title
- input
- referenceSample
- judgeRule
- hardChecks
- critical
- confirmStatus
- sourceRunId
- sourceNodeRunId
- sourceWorkflowVersionId
- sourceNodeId
- description
- createdAt
- updatedAt

确认状态：

- USER_CREATED
- USER_CONFIRMED
- AI_DRAFT_PENDING
- ARCHIVED

字段说明：

- `input` 是执行目标节点时使用的输入。
- `referenceSample` 是参考样例，可来自用户手写或历史 NodeRun 输出，不是标准答案。
- `judgeRule` 是自然语言验收规则，是 LLM 节点验收的主判断依据。
- `hardChecks` 是可选硬约束，用于结构化输出、字段存在、枚举、范围、正则等检查。
- `critical=true` 的用例失败时，整体 EvalRun 应失败。
- 只有 `USER_CONFIRMED` 用例可以进入正式 EvalRun；`USER_CREATED` 和 `AI_DRAFT_PENDING` 都必须先确认。

## 6. Judge Rule

`judgeRule` 必须用自然语言说明用户期望目标节点具备的能力。

示例：

```text
请判断实际输出是否满足以下要求：
1. 必须覆盖输入文本中的三个关键风险点。
2. 不得编造输入中不存在的政策、金额或时间。
3. 语气必须适合企业内部审查场景。
4. 如果信息不足，必须明确说明缺少哪些信息。
```

用户确认 EvalCase 前必须填写 `judgeRule`。

## 7. Hard Checks

`hardChecks` 只用于硬约束，不用于自然语言质量判断。

适合的场景：

- 输出必须符合 outputSchema。
- 指定 JSONPath 字段必须存在。
- 字段值必须属于枚举。
- 数值必须在范围内。
- 文本必须匹配正则。
- 文本不得包含禁止词。

不适合的场景：

- 摘要全文完全等于某个样本。
- 开放式回答完全等于某段参考文本。
- 长自然语言解释逐字匹配。

V1 hardChecks 支持类型和必填字段：

- `SCHEMA_VALIDATION`：只需要 `type`，目标节点必须配置 outputSchema。
- `JSON_PATH_EXISTS`：需要 `type` 和 `path`。
- `JSON_PATH_IN`：需要 `type`、`path` 和非空数组 `values`。
- `JSON_PATH_NUMBER_RANGE`：需要 `type`、`path`，并且 `min` 和 `max` 至少一个存在。
- `JSON_PATH_REGEX`：需要 `type`、`path` 和合法 Java 正则 `pattern`。
- `JSON_PATH_CONTAINS`：需要 `type`、`path` 和非空字符串 `expected`。
- `JSON_PATH_NOT_CONTAINS`：需要 `type`、`path` 和非空字符串 `expected`。

V1 不提供 `JSON_PATH_EQUALS`，需要稳定枚举或状态判断时使用 `JSON_PATH_IN`。

hardCheckResults 是数组。每项至少包含 `type`、`passed`、`message`，可包含 `path`、`expected`、`actual` 和 `details`。

## 8. EvalRun

EvalRun 是一次验收执行。

建议字段：

- id
- evalSuiteId
- workflowVersionId
- nodeId
- judgeModelOfferingKey
- startedAt
- finishedAt
- status
- totalCases
- passedCases
- failedCases
- passRate
- summary

通过率根据 EvalCaseResult 的 `passed` 统计。hardChecks 失败时用例直接失败并跳过 judge LLM。

## 9. 验收运行流程

```text
用户选择 LLM 类节点
  -> 创建或选择 EvalSuite
  -> 用户维护 EvalCase 输入、judgeRule、referenceSample 和 hardChecks
  -> 用户确认 EvalCase
  -> 平台逐条执行目标节点
  -> 执行 hardChecks
  -> hardChecks 失败则直接生成失败结果
  -> hardChecks 通过后调用 judge LLM
  -> judge LLM 输出结构化 judgeResult
  -> 生成 EvalRun
  -> 展示通过率、失败用例、judge 原因和逐条规则结果
```

## 10. 从历史运行创建用例

平台应支持从 NodeRun 生成待确认 EvalCase：

- 复制节点输入为用例输入。
- 复制节点输出为 `referenceSample`。
- `judgeRule` 默认为空，必须由用户补充。
- `hardChecks` 默认为空数组。
- 状态统一默认为 `AI_DRAFT_PENDING`。
- 自动生成的 EvalCase 必须记录来源信息：`sourceRunId`、`sourceNodeRunId`、`sourceWorkflowVersionId`、`sourceNodeId`。
- 用户补充 `judgeRule` 并确认后才进入正式验收集。

## 11. AI 辅助边界

AI 可以：

- 根据节点说明建议验收规则草稿。
- 根据 inputSchema 和 outputSchema 补充边界输入。
- 在 EvalRun 中作为 judge LLM 判断输出是否满足 `judgeRule`。
- 根据失败输出解释失败原因。

AI 不可以：

- 未经用户确认就创建正式验收用例。
- 自动把一次 NodeRun 输出当标准答案。
- 自动确认 EvalCase。
- 自动修改提示词并发布。

## 12. 验收结果展示

验收结果应展示：

- 总用例数
- 通过数
- 失败数
- 通过率
- 关键用例失败情况
- 每条失败用例的输入、实际输出、参考样例、judge 结论、失败原因
- 每条规则的判断结果
- hardChecks 结果
- judge 模型供应项
- judge 原始输出
- 历史通过率对比

hardChecks 失败跳过 judge 时，不展示 judge 分数、judge 原因和 judge 原始输出。分数只读取 `judgeResult.score`。
