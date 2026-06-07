# LLM 节点验收 Judge Rule v1 开发说明

## 1. 本次变更的本质

本次变更不是字段改名，也不是在现有断言系统旁边补一个 LLM 评分功能。

本次变更要修正节点验收的产品语义：LLM、REVIEW、SUMMARY 节点验收不是传统接口测试，不应该主要依赖精确断言判断输出是否等于某个固定值。它的目标是验证用户配置的 LLM 类节点，在给定输入下是否达到用户用自然语言描述的业务预期。

旧主线：

```text
EvalCase.input
  -> 执行目标节点
  -> assertions 判断是否通过
  -> scoreRule 可选执行 LLM 辅助评分
  -> passRate 由 assertions 结果统计
```

新主线：

```text
EvalCase.input
  -> 执行目标 LLM 类节点
  -> hardChecks 执行结构化硬约束
  -> hardChecks 失败则直接失败并跳过 judge LLM
  -> judge LLM 根据 judgeRule 判断实际输出是否达标
  -> judgeResult 决定用例是否通过
  -> passRate 由 EvalCaseResult.passed 统计
```

开发人员必须把 `judgeRule` 和 `judgeResult` 当成正式验收流程，而不是辅助展示。

## 2. 需要推翻的旧理解

旧理解中，`assertions` 是正式通过率来源，`scoreRule` 是可选 LLM 辅助评分，`referenceAnswer` 是评分上下文。这套模型更接近传统接口测试，不适合 LLM 类节点能力验收。

本变更后，以下旧结论不再成立：

- `assertions` 决定 LLM 类节点 Eval 的正式通过率。
- `scoreRule` 是可选辅助评分。
- `referenceAnswer` 是参考答案或期望答案。
- Schema 校验和规则断言优先于 LLM judge。
- 从 NodeRun 复制出来的输出可以被当作精确标准答案。
- 手工创建后无需用户确认即可进入运行。

新的正式结论是：

- `judgeRule` 是用户定义的自然语言验收规则。
- `referenceSample` 是参考样例，不是标准答案。
- `hardChecks` 是可选硬约束，只验证结构、格式、枚举、范围等稳定条件。
- `judgeResult` 是正式评判结果。
- `EvalCaseResult.passed` 由 hardChecks 和 `judgeResult.passed` 得出；hardChecks 失败时不调用 judge。
- 只有 `USER_CONFIRMED` EvalCase 可以进入正式 EvalRun。

## 3. 核心字段语义

### 3.1 EvalSuite

EvalSuite 表达一组针对同一 Agent、WorkflowVersion 和节点的验收集合。

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

`judgeModelOfferingKey` 放在 EvalSuite 上，不放在 EvalCase 上。原因是同一个验收集应使用同一把评判尺子；如果每条 EvalCase 都能选择不同 judge 模型，同一套件内的评判口径会不稳定。

### 3.2 EvalCase

EvalCase 表达一条 LLM 类节点能力验收场景。

正式字段：

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

`judgeRule` 是正式用例确认前的必填字段。没有 `judgeRule` 的 EvalCase 不能进入正式 EvalRun。

`referenceSample` 只能用于给 judge LLM 提供参考，不得参与精确匹配，不得在 UI 中称为标准答案、期望答案或权威答案。

`hardChecks` 可以为空。它只处理硬约束，不承担自然语言质量判断。

### 3.3 EvalCaseResult

EvalCaseResult 必须能解释一次用例为什么通过或失败。

正式字段：

- `output`
- `hardCheckResults`
- `judgeResult`
- `judgeRawText`
- `judgeModelOfferingKey`
- `judgePromptVersion`
- `passed`
- `errorMessage`
- `durationMs`

字段规则：

- `hardCheckResults` 是数组，未配置 hardChecks 时为 `[]`。
- `judgeResult` 是结构化判定结果，内部可以包含 `score`、`reason` 和 `criteriaResults`。
- hardChecks 失败跳过 judge 时，`judgeResult`、`judgeRawText`、`judgeModelOfferingKey`、`judgePromptVersion` 都为 `null`。
- `judgeRawText` 用于保留 judge LLM 原始输出，便于人工复核和排障。
- `judgePromptVersion` V1 固定为 `JUDGE_RULE_V1`。
- `errorMessage` 是顶层错误或失败摘要字段。
- 不提供顶层 `failureReason`。
- 不提供 `judgeRawOutput` 对象字段。
- 不提供顶层 `score`，分数只读取 `judgeResult.score`。

## 4. Judge Rule 的执行语义

`judgeRule` 是用户用自然语言写的验收规则。

示例：

```text
请判断实际输出是否满足以下要求：
1. 必须覆盖输入文本中的三个关键风险点。
2. 不得编造输入中不存在的政策、金额或时间。
3. 语气必须适合企业内部审查场景。
4. 如果信息不足，必须明确说明缺少哪些信息。
```

执行时，系统应把以下内容交给 judge LLM：

- EvalCase 输入。
- 目标节点实际输出。
- `referenceSample`。
- `judgeRule`。
- `hardCheckResults`。
- 目标节点标识和类型。

judge LLM 必须输出 JSON。最小结构：

```json
{
  "passed": true,
  "score": 86,
  "reason": "回答覆盖了关键风险点，未发现编造内容。",
  "criteriaResults": [
    {
      "criterion": "覆盖关键风险点",
      "passed": true,
      "score": 30,
      "reason": "三个风险点均已覆盖。"
    }
  ]
}
```

如果 judge 调用失败、输出不是 JSON、缺少 `passed` 字段，EvalCaseResult 必须失败，不能默认通过。

## 5. Hard Checks 的定位

hardChecks 是硬约束，不是主评价规则。

V1 只支持：

- `SCHEMA_VALIDATION`
- `JSON_PATH_EXISTS`
- `JSON_PATH_IN`
- `JSON_PATH_NUMBER_RANGE`
- `JSON_PATH_REGEX`
- `JSON_PATH_CONTAINS`
- `JSON_PATH_NOT_CONTAINS`

适合 hardChecks 的场景：

- 输出必须符合 outputSchema。
- 某个 JSONPath 字段必须存在。
- 某个字段必须属于枚举。
- 数值必须在范围内。
- 文本必须匹配正则。
- 文本必须包含或不包含某些稳定关键词。

不适合 hardChecks 的场景：

- 摘要全文必须完全等于某段参考文本。
- 开放式回答必须逐字匹配。
- 解释类输出必须和历史输出完全一致。
- 用 JSONPath 精确相等判断自然语言质量。

V1 不提供 `JSON_PATH_EQUALS`。如果需要稳定单值判断，使用 `JSON_PATH_IN` 且 `values` 只传一个值。

V1 hardChecks 配置 schema 必须按 `design.md` 第 4 节实现：

- `SCHEMA_VALIDATION`：只需要 `type`，目标节点必须配置 outputSchema。
- `JSON_PATH_EXISTS`：需要 `path`。
- `JSON_PATH_IN`：需要 `path` 和非空数组 `values`。
- `JSON_PATH_NUMBER_RANGE`：需要 `path`，且 `min` 和 `max` 至少一个存在。
- `JSON_PATH_REGEX`：需要 `path` 和合法 Java 正则 `pattern`。
- `JSON_PATH_CONTAINS`：需要 `path` 和非空字符串 `expected`。
- `JSON_PATH_NOT_CONTAINS`：需要 `path` 和非空字符串 `expected`。

hardCheckResults 必须是数组。每项至少包含 `type`、`passed`、`message`，可包含 `path`、`expected`、`actual`、`details`。

## 6. 从 NodeRun 生成 EvalCase

从 NodeRun 生成用例时，系统只做样本采集，不创建正式验收规则。

规则：

- NodeRun 输入复制到 `input`。
- NodeRun 输出复制到 `referenceSample`。
- `judgeRule` 为空。
- `hardChecks` 默认为空数组。
- `confirmStatus=AI_DRAFT_PENDING`。
- 写入来源字段。

用户必须补充 `judgeRule` 后才能确认用例。系统不得自动生成 judgeRule 后直接确认用例。

## 7. 手工创建 EvalCase

手工创建用例默认 `confirmStatus=USER_CREATED`。

`USER_CREATED` 只表示用户手工创建且尚未确认，它不是可运行状态。创建接口可以保存空 `judgeRule`，但不能把该用例计入 EvalSuite 确认条件，也不能进入 EvalRun。

正式校验发生在确认用例时：

- `judgeRule` 必须非空。
- `hardChecks` 如果存在，必须只使用 V1 支持类型。
- EvalSuite 必须配置有效 `judgeModelOfferingKey`。
- 用例确认后状态变为 `USER_CONFIRMED`。

## 8. EvalRun 执行流程

正式执行流程：

```text
读取 CONFIRMED EvalSuite
  -> 读取 USER_CONFIRMED EvalCase
  -> 执行目标 LLM 类节点
  -> 执行 hardChecks
  -> hardChecks 失败则用例失败并跳过 judge LLM
  -> hardChecks 通过则调用 judge LLM
  -> 解析 judgeResult
  -> 根据 judgeResult.passed 得到用例 passed
  -> critical 用例失败则 EvalRun 失败
  -> 按 passed 统计 passRate
  -> 写 EvalRun、EvalCaseResult、TraceEvent
```

通过规则：

- hardChecks 失败时，用例失败且不调用 judge LLM，judge 相关字段写 `null`。
- judgeResult.passed=false 时，用例失败。
- judgeResult.passed=true 且 hardChecks 通过时，用例通过。
- judge 调用失败或结果无法解析时，用例失败。
- critical 用例失败时，即使 passRate 达标，EvalRun 也失败。

## 9. 后端实施要点

需要重构 `11_code/backend/src/main/java/com/myagent/eval/` 下的应用服务、DTO、仓储、执行器和测试。

主要动作：

- 修改 EvalSuite create/update/confirm 命令和 DTO，使用 `judgeModelOfferingKey` 和 `judgeTemperature`。
- 修改 EvalCase create/update/confirm 命令和 DTO，使用 `referenceSample`、`judgeRule`、`hardChecks`。
- 创建 EvalCase 时默认 `USER_CREATED`，但不做正式用例校验。
- 修改 `EvalCaseRepository.listRunnableCases`，只返回 `USER_CONFIRMED`。
- 修改 EvalSuite 确认逻辑，只统计 `USER_CONFIRMED` 用例。
- 删除 `DefaultEvalScoreEvaluator` 的辅助评分定位，新增正式 `EvalJudgeEvaluator`。
- 修改 EvalRun 执行逻辑，用 `EvalCaseResult.passed` 统计通过率。
- 修改 formal validation：确认 EvalCase 前必须有非空 `judgeRule`。
- 修改 hardChecks 校验和执行逻辑，确保它只作为硬约束。
- 修改 repository 和 DDL 字段，删除或替换旧 `reference_answer_json`、`assertions_json`、`score_rule_json`。
- 修改 EvalCaseResult 持久化，保存 `hard_check_result_json`、`judge_result_json`、`judge_raw_text`、`judge_model_offering_key`、`judge_prompt_version`、`error_message`。
- `hard_check_result_json` 默认值必须是 `[]`，不是 `{}`。
- 不保留 EvalCaseResult 顶层 `score` 字段。
- 当前系统旧数据会删除，不需要旧字段迁移。
- 直接更新当前 Flyway 基线脚本和相关 Java migration，使空库重建后只有新结构；已经应用旧迁移的本地或测试数据库必须清空后重跑。

## 10. 前端实施要点

需要重构 `11_code/frontend/src/features/evals/`。

页面主体验应改变：

- EvalSuite 表单配置 judge 模型供应项和 judge 温度。
- EvalCase 表单主输入是自然语言 `judgeRule`。
- `referenceSample` 显示为参考样例，不能显示为标准答案。
- `hardChecks` 放在辅助约束或高级设置区域。
- 从 NodeRun 生成用例后，页面提示用户补充 judgeRule。
- 确认用例时，如果 judgeRule 为空，前端应阻止并显示中文错误。
- EvalRun 结果展示 judge 结论、`judgeResult.score`、`errorMessage`、逐条规则结果、hardChecks 结果、`judgeRawText` 和 `judgePromptVersion`。
- hardChecks 失败跳过 judge 时，不展示 judge 分数。

前端不应继续把断言 JSON 作为普通用户的主操作入口。

## 11. 接口和文档同步

以下正式文档必须按本语义同步：

- `3_product_design/Agent管理平台-产品设计-07-节点验收设计-v1.md`
- `4_arch_design/10-节点验收架构设计-v1.md`
- `6_schema_design/05-运行追踪与验收表DDL-v1.md`
- `7_interface_design/02-对外REST接口-v1.md`
- `7_interface_design/03-内部应用服务接口-v1.md`

接口合同应使用：

- `referenceSample`
- `judgeRule`
- `hardChecks`
- `judgeResult`
- `judgeRawText`
- `judgeModelOfferingKey`
- `judgePromptVersion`
- `errorMessage`

接口合同不应继续使用：

- `referenceAnswer`
- `assertions`
- `scoreRule`
- `assertionResults`
- 顶层 `failureReason`
- `judgeRawOutput`
- 顶层 `score`

## 12. 测试要求

后端测试必须覆盖：

- 创建 EvalCase 保存 judgeRule。
- `USER_CREATED` 用例不能进入正式 EvalRun。
- `AI_DRAFT_PENDING` 用例不能进入正式 EvalRun。
- 缺少 judgeRule 的用例不能确认。
- 从 NodeRun 生成用例时输出进入 referenceSample。
- hardChecks 失败时用例失败。
- hardChecks 失败时不调用 judge，judge 相关字段为空。
- judgeResult.passed=false 时用例失败。
- judgeResult.passed=true 且 hardChecks 通过时用例通过。
- judge 输出非法 JSON 时用例失败。
- EvalRun 通过率按 EvalCaseResult.passed 计算。
- critical 用例失败导致 EvalRun 失败。

前端测试必须覆盖：

- EvalSuite 表单展示 judge 模型配置。
- EvalCase 表单展示自然语言验收规则主输入。
- 参考样例文案不是标准答案。
- hardChecks 是辅助输入。
- 缺少 judgeRule 时不能确认。
- 结果页展示 judge 结论、`judgeResult.score`、`errorMessage`、逐条规则结果、`judgeRawText`。

静态检查必须确认正式文档和接口不再保留旧语义。

## 13. 禁止做法

- 禁止继续让 `assertions` 决定 LLM 类节点 Eval 的正式通过率。
- 禁止继续把 LLM judge 叫做辅助评分。
- 禁止把 `referenceSample` 当标准答案做精确匹配。
- 禁止在没有 judgeRule 的情况下确认正式 EvalCase。
- 禁止把 hardChecks 重新做成主判断。
- 禁止 hardChecks 失败后继续调用 judge LLM。
- 禁止隐藏 judge 调用失败。
- 禁止让 judge 模型走默认供应商或默认模型。
- 禁止只改字段名，不改执行语义。
- 禁止把 judge 模型字段放到 EvalCase。
- 禁止新增 `judge_rule_json` 或 `judge_raw_output_json`。
- 禁止保留 EvalCaseResult 顶层 `score` 字段。

## 14. 用户澄清状态

当前没有需要继续向用户澄清的必答问题。开发人员应按本文档和正式设计文档直接实现。
