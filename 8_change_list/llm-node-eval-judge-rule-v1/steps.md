# LLM 节点验收 Judge Rule v1 实施步骤

## 1. 文档先行

1. 更新产品文档，删除“确定性优先”和“LLM 评分只是辅助”的旧语义。
2. 更新架构文档，把 LLM 类节点 Eval 定义为 `judgeRule + judgeResult` 主导的能力验收。
3. 更新接口文档，替换 `referenceAnswer/assertions/scoreRule` 旧字段。
4. 更新 schema 文档和 DDL。
5. 更新用户手册，说明如何编写自然语言验收规则。

## 2. 数据模型

1. 修改 `eval_suite`，增加 `judge_model_offering_key` 和 `judge_temperature`。
2. 修改 `eval_case`，使用 `reference_sample_json`、`judge_rule_text`、`hard_checks_json`。
3. 删除旧 `reference_answer_json`、`assertions_json`、`score_rule_json`。
4. 修改 `eval_case_result`，使用 `hard_check_result_json`、`judge_result_json`、`judge_raw_text`、`judge_model_offering_key`、`judge_prompt_version`、`error_message`。
5. `hard_check_result_json` 必须是数组，默认值为 `[]`。
6. 不保留 EvalCaseResult 顶层 `score` 字段，分数只保存在 `judge_result_json.score`。
7. 不创建 `judge_rule_json`。
8. 不创建 `judge_raw_output_json`。
9. 当前系统旧数据会删除，不需要旧字段迁移。
10. 直接更新当前 Flyway 基线脚本和相关 Java migration，使新数据库从零迁移后只有新结构；已应用旧迁移的本地或测试库必须清空后重跑迁移。

## 3. 后端应用服务

1. 修改 EvalSuite create/update/confirm 命令和 DTO，支持 `judgeModelOfferingKey` 和 `judgeTemperature`。
2. 修改 EvalCase create/update/confirm 命令和 DTO，使用 `referenceSample`、`judgeRule`、`hardChecks`。
3. 创建 EvalCase 时默认 `confirmStatus=USER_CREATED`，只做基础结构校验，不做正式用例校验。
4. 从 NodeRun 生成 EvalCase 时默认 `confirmStatus=AI_DRAFT_PENDING`，复制输出到 `referenceSample`，不写 `judgeRule`。
5. 确认 EvalCase 时校验 `judgeRule` 非空、hardChecks 合法、EvalSuite judge 模型已配置。
6. `EvalCaseRepository.listRunnableCases` 只能返回 `USER_CONFIRMED`。
7. EvalSuite 确认和 EvalRun 执行都只能使用 `USER_CONFIRMED` 用例。
8. 删除 `DefaultEvalScoreEvaluator` 的辅助评分语义，新增正式 `EvalJudgeEvaluator`。
9. EvalRun 执行时用 hardChecks 和 judgeResult 决定 `EvalCaseResult.passed`。
10. hardChecks 失败时用例失败，并跳过 judge LLM。
11. hardChecks 失败结果中 `judgeResult`、`judgeRawText`、`judgeModelOfferingKey`、`judgePromptVersion` 都写 `null`。
12. judge 输出无法解析时用例失败。
13. passRate 按 `EvalCaseResult.passed` 统计。

## 4. Hard Checks 执行器

V1 只允许以下类型：

- `SCHEMA_VALIDATION`
- `JSON_PATH_EXISTS`
- `JSON_PATH_IN`
- `JSON_PATH_NUMBER_RANGE`
- `JSON_PATH_REGEX`
- `JSON_PATH_CONTAINS`
- `JSON_PATH_NOT_CONTAINS`

实施要求：

1. 删除或禁用 `JSON_PATH_EQUALS`。
2. 不允许把旧 assertions 执行器直接改名为 hardChecks 执行器。
3. hardChecks 只输出硬约束结果，不生成自然语言质量结论。
4. 需要单值相等语义时，使用 `JSON_PATH_IN` 且 `values` 只传一个值。
5. 每种 hardCheck 类型必须按 `design.md` 中的字段 schema 校验，字段缺失、类型错误、非法正则或未知类型都必须在确认 EvalCase 前失败。

## 5. 前端

1. 修改 EvalSuite 表单，把 judge 模型供应项和 judge 温度放在套件配置中。
2. 修改 EvalCase 表单，把自然语言验收规则作为主输入。
3. 把参考样例文案固定为“参考样例”，不得使用“标准答案”。
4. hardChecks 放入辅助或高级区域。
5. 结果页展示 `judgeResult`、`judgeRawText`、`judgePromptVersion`、`errorMessage` 和 hardCheckResults。
6. 分数只展示 `judgeResult.score`；hardChecks 失败跳过 judge 时不展示分数。
7. 从 NodeRun 生成用例后提示用户补充 judgeRule。
8. 确认用例时，如果 judgeRule 为空，前端必须阻止并显示中文错误。

## 6. 测试

1. 更新后端创建、更新、确认用例测试。
2. 增加 judgeRule 缺失不能确认测试。
3. 增加 `USER_CREATED` 不进入正式运行测试。
4. 增加 `AI_DRAFT_PENDING` 不进入正式运行测试。
5. 增加 hardChecks 失败测试。
6. 增加 hardChecks 失败后不调用 judge、judge 字段为空的测试。
7. 增加 7 种 hardChecks 类型字段 schema 校验测试。
8. 增加 judge passed/failed 影响通过率测试。
9. 增加 judge 输出非法 JSON 失败测试。
10. 增加前端表单测试。
11. 增加从 NodeRun 生成用例的页面路径测试。
12. 增加 OpenAPI 类型生成和前端 API 类型编译检查。
13. 增加数据库从空库执行 Flyway 全量迁移测试。

## 7. 禁止做法

- 禁止继续让 `assertions` 决定 LLM 类节点 Eval 的正式通过率。
- 禁止继续把 LLM judge 结果叫做“辅助评分”。
- 禁止把 `referenceSample` 当标准答案做精确匹配。
- 禁止在没有 judgeRule 的情况下确认正式 EvalCase。
- 禁止让 `USER_CREATED` 进入正式 EvalRun。
- 禁止恢复“确定性断言优先于 LLM 评分”的旧语义。
- 禁止在 EvalCase 上新增 judge 模型字段。
- 禁止新增 `judge_rule_json` 或 `judge_raw_output_json`。
- 禁止在 hardChecks 已失败时继续调用 judge LLM。
- 禁止保留 EvalCaseResult 顶层 `score` 字段。
- 禁止新增读取旧 EvalCase 字段的转换逻辑。
