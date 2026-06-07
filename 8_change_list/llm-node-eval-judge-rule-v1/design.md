# LLM 节点验收 Judge Rule v1 设计

## 1. 核心结论

LLM、REVIEW、SUMMARY 节点验收的主判断方式必须是自然语言 `judgeRule` + judge LLM 结构化判定，而不是传统确定性断言。

旧设计主线：

```text
assertions -> 决定 passed/passRate
scoreRule -> 可选辅助评分
referenceAnswer -> 容易被理解为标准答案
```

新设计主线：

```text
judgeRule -> 用户定义的自然语言验收规则
judgeResult -> 正式通过与否的主判定结果
hardChecks -> 可选结构化硬约束
referenceSample -> 参考样例，不是标准答案
```

本变更不保留新旧两套 Eval 判断主链。LLM 类节点 EvalRun 的 `passRate` 只能来自 `EvalCaseResult.passed`。

`EvalCaseResult.passed` 的计算规则是：

- hardChecks 失败：`passed=false`，不调用 judge LLM。
- hardChecks 通过且 `judgeResult.passed=true`：`passed=true`。
- hardChecks 通过但 judge 调用失败、输出非法或 `judgeResult.passed=false`：`passed=false`。

## 2. EvalSuite

EvalSuite 表达一组针对同一 Agent、WorkflowVersion 和节点的验收集合。judge 模型配置只属于 EvalSuite，不属于 EvalCase。

正式字段：

- `suiteId`
- `agentId`
- `workflowVersionId`
- `nodeId`
- `name`
- `goal`
- `judgeModelOfferingKey`
- `judgeTemperature`
- `passThreshold`
- `status`
- `createdAt`
- `updatedAt`

规则：

- `judgeModelOfferingKey` 必须引用已启用的模型供应项。
- 同一 EvalSuite 内所有 EvalCase 使用同一个 judge 模型，保证同一批验收使用同一把评判尺子。
- EvalSuite 确认前必须存在至少一个 `USER_CONFIRMED` EvalCase。
- EvalSuite 确认前必须存在有效 `judgeModelOfferingKey`。

## 3. EvalCase

EvalCase 表达一条 LLM 类节点能力验收场景。EvalCase 不允许配置 `judgeModelOfferingKey` 或 `judgeTemperature`。

正式字段：

- `caseId`
- `suiteId`
- `caseNo`
- `title`
- `input`
- `referenceSample`
- `judgeRule`
- `hardChecks`
- `critical`
- `confirmStatus`
- `sourceRunId`
- `sourceNodeRunId`
- `sourceWorkflowVersionId`
- `sourceNodeId`
- `description`
- `createdAt`
- `updatedAt`

### 3.1 judgeRule

`judgeRule` 是用户用自然语言写的正式验收规则，接口字段为 `judgeRule`，数据库字段为 `judge_rule_text`。

创建或更新 EvalCase 时允许 `judgeRule` 为空，因为此时用例仍是待确认状态。确认 EvalCase 为 `USER_CONFIRMED` 前，`judgeRule` 必须非空且去除空白后仍有内容。

示例：

```text
请判断实际输出是否满足以下要求：
1. 必须覆盖输入文本中的三个关键风险点。
2. 不得编造输入中不存在的政策、金额或时间。
3. 语气必须适合企业内部审查场景。
4. 如果信息不足，必须明确说明缺少哪些信息。
```

### 3.2 referenceSample

`referenceSample` 是参考样例，用于帮助 judge 理解用户期望。

它不是标准答案，不参与精确匹配，不直接决定通过失败。从 NodeRun 生成 EvalCase 时，NodeRun 输出写入 `referenceSample`。

### 3.3 confirmStatus

正式状态：

- `USER_CREATED`：用户手工创建的未确认用例，不可运行。
- `AI_DRAFT_PENDING`：从 NodeRun 或 AI 辅助流程生成的待确认用例，不可运行。
- `USER_CONFIRMED`：用户已确认的正式用例，可进入正式 EvalRun。
- `ARCHIVED`：已归档用例，不可运行。

只有 `USER_CONFIRMED` 可以进入正式 EvalRun。`USER_CREATED` 不是正式用例，只表示手工创建来源和待确认状态。

## 4. Hard Checks

`hardChecks` 是可选硬约束，只用于结构、格式和边界检查，不承担自然语言质量判断。

### 4.1 配置数组

EvalCase 的 `hardChecks` 必须是数组。未配置时为 `[]`。

每个 hardCheck 对象必须包含 `type` 字段。未知字段不参与执行；未知 `type`、字段缺失或字段类型错误必须在确认 EvalCase 前报业务错误。

### 4.2 支持类型和字段 schema

#### SCHEMA_VALIDATION

使用目标节点的 outputSchema 校验实际输出。

```json
{
  "type": "SCHEMA_VALIDATION"
}
```

字段规则：

- 不需要 `path`。
- 目标节点必须配置 outputSchema，否则确认 EvalCase 失败。

#### JSON_PATH_EXISTS

校验指定 JSONPath 字段存在。

```json
{
  "type": "JSON_PATH_EXISTS",
  "path": "$.summary"
}
```

字段规则：

- `path` 必填，必须是非空 JSONPath 字符串。
- path 命中且值不是 missing 时通过；值为 JSON null 仍视为存在。

#### JSON_PATH_IN

校验指定 JSONPath 的值属于枚举集合。单值判断也使用该类型，并将 `values` 写成单元素数组。

```json
{
  "type": "JSON_PATH_IN",
  "path": "$.riskLevel",
  "values": ["LOW", "MEDIUM", "HIGH"]
}
```

字段规则：

- `path` 必填。
- `values` 必填，必须是非空数组。
- 命中值必须与 `values` 中任一 JSON 标量值相等。
- V1 不提供 `JSON_PATH_EQUALS`。

#### JSON_PATH_NUMBER_RANGE

校验指定 JSONPath 的数值范围。

```json
{
  "type": "JSON_PATH_NUMBER_RANGE",
  "path": "$.score",
  "min": 0,
  "max": 100
}
```

字段规则：

- `path` 必填。
- `min` 和 `max` 至少填写一个。
- `min` 和 `max` 均为闭区间边界。
- 命中值必须是 number。

#### JSON_PATH_REGEX

校验指定 JSONPath 的字符串值匹配正则。

```json
{
  "type": "JSON_PATH_REGEX",
  "path": "$.orderNo",
  "pattern": "^[A-Z0-9-]+$"
}
```

字段规则：

- `path` 必填。
- `pattern` 必填，必须是合法 Java 正则表达式。
- 命中值必须是 string。

#### JSON_PATH_CONTAINS

校验指定 JSONPath 的字符串值包含指定文本。

```json
{
  "type": "JSON_PATH_CONTAINS",
  "path": "$.summary",
  "expected": "关键风险"
}
```

字段规则：

- `path` 必填。
- `expected` 必填，必须是非空字符串。
- 命中值必须是 string。
- V1 使用大小写敏感匹配。

#### JSON_PATH_NOT_CONTAINS

校验指定 JSONPath 的字符串值不包含指定文本。

```json
{
  "type": "JSON_PATH_NOT_CONTAINS",
  "path": "$.summary",
  "expected": "无法判断"
}
```

字段规则：

- `path` 必填。
- `expected` 必填，必须是非空字符串。
- 命中值必须是 string。
- V1 使用大小写敏感匹配。

### 4.3 hardCheckResults

`hardCheckResults` 必须是数组。未配置 hardChecks 时为 `[]`。

每个结果项字段：

- `type`：hardCheck 类型，必填。
- `passed`：是否通过，必填。
- `message`：中文结果说明，必填。
- `path`：对应 JSONPath，SCHEMA_VALIDATION 可为空。
- `expected`：期望值，可为空。
- `actual`：实际值，可为空。
- `details`：补充排障信息，可为空对象。

示例：

```json
{
  "type": "JSON_PATH_EXISTS",
  "path": "$.summary",
  "passed": false,
  "message": "$.summary 字段缺失。",
  "expected": "字段存在",
  "actual": null,
  "details": {}
}
```

## 5. EvalRun 执行流程

正式流程：

```text
选择 CONFIRMED EvalSuite
  -> 读取 USER_CONFIRMED EvalCase
  -> 执行目标 LLM 类节点
  -> 执行 hardChecks
  -> hardChecks 失败则写失败结果并跳过 judge LLM
  -> hardChecks 通过则调用 judge LLM
  -> 解析 judgeResult
  -> 根据 judgeResult.passed 得出 EvalCaseResult.passed
  -> 按 EvalCaseResult.passed 计算 passRate
  -> 写 EvalRun、EvalCaseResult、TraceEvent
```

规则：

- hardChecks 失败时，用例失败，`judgeResult=null`、`judgeRawText=null`、`judgeModelOfferingKey=null`、`judgePromptVersion=null`。
- judge 调用失败、输出非 JSON、缺少 `passed` 字段时，用例失败。
- `judgeResult.passed=true` 且 hardChecks 全部通过时，用例通过。
- critical 用例失败时，即使通过率达到阈值，EvalRun 也必须失败。

## 6. Judge LLM

judge LLM 输入必须包含：

- EvalCase 输入。
- 目标节点实际输出。
- `referenceSample`。
- `judgeRule`。
- `hardCheckResults`。
- 目标节点类型和节点标识。

judge LLM 必须输出 JSON：

```json
{
  "passed": true,
  "score": 86,
  "reason": "回答覆盖了关键风险点，未发现编造内容，但第二项解释略短。",
  "criteriaResults": [
    {
      "criterion": "覆盖三个关键风险点",
      "passed": true,
      "score": 30,
      "reason": "三个风险点均已覆盖。"
    }
  ]
}
```

`score` 只存在于 `judgeResult.score` 内，不设置 EvalCaseResult 顶层 `score` 字段。

## 7. EvalCaseResult

EvalCaseResult 是单条用例结果的唯一正式结果契约。

正式字段：

- `caseId`
- `caseNo`
- `title`
- `confirmStatus`
- `critical`
- `input`
- `referenceSample`
- `output`
- `hardCheckResults`
- `judgeResult`
- `judgeRawText`
- `judgeModelOfferingKey`
- `judgePromptVersion`
- `passed`
- `errorMessage`
- `durationMs`

字段语义：

- `hardCheckResults` 是数组；未配置 hardChecks 时为 `[]`。
- `judgeResult` 保存 judge LLM 解析后的结构化 JSON，内部允许包含 `score`、`reason` 和 `criteriaResults`。
- `judgeResult=null` 表示 judge 没有产出结构化结果，可能是 hardChecks 失败跳过，也可能是 judge 调用失败或输出非法。
- `judgeRawText` 保存 judge LLM 原始文本输出；hardChecks 失败跳过 judge 时为 `null`。
- `judgeModelOfferingKey` 保存实际调用 judge 时使用的模型供应项；hardChecks 失败跳过 judge 时为 `null`。
- `judgePromptVersion` 保存实际调用 judge 时使用的提示词版本；hardChecks 失败跳过 judge 时为 `null`。
- `errorMessage` 是顶层执行错误或失败摘要字段。
- 不提供顶层 `failureReason`。
- 不提供 `judgeRawOutput` 对象字段。
- 不提供顶层 `score`，前端展示分数时读取 `judgeResult.score`。

## 8. 接口设计

### 8.1 创建 EvalSuite

```json
{
  "agentId": 1,
  "workflowVersionId": 12,
  "nodeId": "node_1",
  "name": "摘要节点回归集",
  "goal": "验证摘要节点在常见输入上的稳定性",
  "judgeModelOfferingKey": "provider.model",
  "judgeTemperature": 0,
  "passThreshold": 80
}
```

### 8.2 创建 EvalCase

```json
{
  "caseNo": "case_001",
  "title": "摘要必须覆盖关键风险",
  "input": {
    "text": "..."
  },
  "referenceSample": {
    "summary": "参考摘要样例"
  },
  "judgeRule": "判断摘要是否覆盖所有关键风险点，不得编造输入中不存在的信息。",
  "hardChecks": [
    {
      "type": "JSON_PATH_EXISTS",
      "path": "$.summary"
    }
  ],
  "critical": false,
  "description": "验证摘要能力"
}
```

创建 EvalCase 返回 `confirmStatus=USER_CREATED`。它必须经过确认接口变为 `USER_CONFIRMED` 后才能运行。

### 8.3 从 NodeRun 生成 EvalCase

规则：

- 复制 NodeRun 输入到 `input`。
- 复制 NodeRun 输出到 `referenceSample`。
- `judgeRule` 默认为空。
- `hardChecks` 默认为空数组。
- `confirmStatus=AI_DRAFT_PENDING`。
- 用户补充 `judgeRule` 后才能确认。

## 9. 前端设计

EvalSuite 表单配置：

- 验收目标。
- judge 模型供应项。
- judge 温度。
- 通过率阈值。

EvalCase 表单主区域：

- 用例编号。
- 标题。
- 输入 JSON。
- 自然语言验收规则。
- 参考样例 JSON。
- 关键用例。

EvalCase 辅助区域：

- hardChecks 结构化编辑器。
- 描述。

结果展示：

- 通过/失败。
- `errorMessage`。
- `judgeResult.score`。
- `judgeResult.reason`。
- 逐条规则结果。
- hardCheckResults。
- judgeRawText。
- judgeModelOfferingKey。
- judgePromptVersion。

hardChecks 失败导致跳过 judge 时，前端展示 hardCheckResults 和 errorMessage，不展示 judge 分数。

## 10. 数据处理

当前系统旧数据会删除，不做旧字段迁移。

本变更执行硬切换：更新当前正式 DDL 和 Flyway 基线脚本，使新安装数据库直接得到新结构。已经应用旧 Flyway 的本地或测试数据库必须清空后重新执行迁移，不在本变更中保留旧字段转换链路。

正式落库字段：

- `eval_suite.judge_model_offering_key`
- `eval_suite.judge_temperature`
- `eval_case.reference_sample_json`
- `eval_case.judge_rule_text`
- `eval_case.hard_checks_json`
- `eval_case_result.hard_check_result_json`
- `eval_case_result.judge_result_json`
- `eval_case_result.judge_raw_text`
- `eval_case_result.judge_model_offering_key`
- `eval_case_result.judge_prompt_version`
- `eval_case_result.error_message`

不再作为正式字段使用：

- `reference_answer_json`
- `assertions_json`
- `score_rule_json`
- `judge_rule_json`
- `judge_raw_output_json`
- `eval_case_result.score`

## 11. 与旧语义的关系

旧语义被本变更取代：

- `assertions` 不再是 LLM 类节点 EvalCase 主字段。
- `scoreRule` 不再是 LLM 类节点正式评分字段。
- “确定性优先”不再适用于 LLM 类节点验收。
- “LLM 评分不覆盖正式断言”不再是目标状态，因为 judge LLM 本身就是正式判断流程的一部分。

开发实现必须重写 Eval 执行主链，不能只把旧 assertions 改名为 hardChecks。
