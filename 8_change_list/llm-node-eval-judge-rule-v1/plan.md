# LLM 节点验收 Judge Rule v1 修改计划

## 1. 范围

本次变更覆盖 LLM、REVIEW、SUMMARY 节点验收的产品语义、数据模型、接口合同、后端执行、前端页面和验收报告。

| 模块 | 当前状态 | 目标状态 |
| --- | --- | --- |
| EvalSuite | 不承担 judge 模型统一口径 | 保存 `judgeModelOfferingKey` 和 `judgeTemperature` |
| EvalCase | `referenceAnswer + assertions + scoreRule` | `referenceSample + judgeRule + hardChecks` |
| EvalCase 状态 | `USER_CREATED` 被当作可运行状态 | 只有 `USER_CONFIRMED` 可运行 |
| EvalRun 通过率 | 确定性断言结果 | `EvalCaseResult.passed`，hardChecks 失败直接失败并跳过 judge |
| LLM 评分 | 可选辅助评分 | 正式 judge 执行 |
| 参考答案 | 容易被误解为标准答案 | 明确为参考样例 |
| 前端 | JSON 断言编辑为主 | 自然语言验收规则编辑为主 |
| 文档 | 确定性优先 | Judge Rule 主导 |

## 2. 必须更新的文档

- `3_product_design/Agent管理平台-产品设计-07-节点验收设计-v1.md`
- `3_product_design/Agent管理平台-产品设计-02-用户场景与功能范围-v1.md`
- `3_product_design/Agent管理平台-产品设计-03-核心概念与领域模型-v1.md`
- `3_product_design/Agent管理平台-产品设计-09-信息架构与验收标准-v1.md`
- `4_arch_design/10-节点验收架构设计-v1.md`
- `4_arch_design/11-数据结构架构设计-v1.md`
- `4_arch_design/13-部署安全与质量架构设计-v1.md`
- `4_arch_design/Agent管理平台-架构设计-总纲-v1.md`
- `6_schema_design/01-数据结构设计总则-v1.md`
- `6_schema_design/05-运行追踪与验收表DDL-v1.md`
- `7_interface_design/02-对外REST接口-v1.md`
- `7_interface_design/03-内部应用服务接口-v1.md`
- `14_user_manual/` 中涉及节点验收的说明

## 3. 必须更新的代码范围

- `11_code/backend/src/main/java/com/myagent/eval/`
- `11_code/backend/src/test/` 中 Eval 相关测试
- `11_code/frontend/src/features/evals/`
- OpenAPI 生成类型和前端 API 客户端

## 4. 阶段一：冻结新语义

- 将 `judgeRule` 定义为正式主判断规则。
- 将 `referenceSample` 定义为参考样例，不是标准答案。
- 将 `hardChecks` 定义为可选硬约束。
- 将 judge 模型配置固定在 EvalSuite 上。
- 将 `USER_CREATED` 和 `AI_DRAFT_PENDING` 定义为不可运行状态。
- 删除“确定性优先”和“LLM 评分只是辅助”的旧口径。

## 5. 阶段二：调整数据和接口

EvalSuite 新增或保留正式字段：

- `judgeModelOfferingKey`
- `judgeTemperature`

EvalCase 正式字段：

- `referenceSample`
- `judgeRule`
- `hardChecks`
- `critical`
- `confirmStatus`
- 来源追踪字段

EvalCaseResult 正式字段：

- `output`
- `hardCheckResults`
- `judgeResult`
- `judgeRawText`
- `judgeModelOfferingKey`
- `judgePromptVersion`
- `passed`
- `errorMessage`
- `durationMs`

数据库字段使用：

- `eval_suite.judge_model_offering_key`
- `eval_suite.judge_temperature`
- `eval_case.reference_sample_json`
- `eval_case.judge_rule_text`
- `eval_case.hard_checks_json`
- `eval_case_result.judge_raw_text`
- `eval_case_result.judge_prompt_version`
- `eval_case_result.hard_check_result_json`，数组结构，未配置时为 `[]`

不使用：

- `judge_rule_json`
- `judge_raw_output_json`
- 顶层 `failureReason`
- 顶层 `judgeRawOutput`

## 6. 阶段三：重构后端执行

- 删除 `DefaultEvalScoreEvaluator` 的辅助评分定位，改为正式 `EvalJudgeEvaluator`。
- 重写 EvalRun 主链：执行目标节点、执行 hardChecks；hardChecks 失败则写失败结果并跳过 judge；hardChecks 通过后调用 judge LLM、解析 judgeResult、生成 EvalCaseResult。
- `passRate` 只能按 `EvalCaseResult.passed` 统计。
- `EvalCaseRepository.listRunnableCases` 只能返回 `USER_CONFIRMED`。
- 确认 EvalSuite 时只能统计 `USER_CONFIRMED` 用例。
- 确认 EvalCase 时校验 `judgeRule` 非空、hardChecks 合法、Suite judge 模型已配置。
- 创建 EvalCase 时只做基础结构校验，不做正式用例校验；正式校验只发生在确认阶段和运行前。
- hardChecks 失败时用例失败，并记录 `hardCheckResults` 和 `errorMessage`；此时 `judgeResult`、`judgeRawText`、`judgeModelOfferingKey`、`judgePromptVersion` 都为 `null`。
- judge 调用失败、输出非法 JSON、缺少 `passed` 字段时用例失败。

## 7. 阶段四：重构前端体验

- EvalSuite 表单负责选择 judge 模型供应项和 judge 温度。
- EvalCase 表单以自然语言 `judgeRule` 为核心输入。
- `referenceSample` 文案固定为“参考样例”，不得显示为“标准答案”或“期望答案”。
- `hardChecks` 放在高级区域或辅助约束区域。
- 从 NodeRun 生成用例后，页面必须提示用户补充 judgeRule 后才能确认。
- 验收结果页面展示 judge 逐条规则结果、`judgeRawText`、`judgePromptVersion` 和 `errorMessage`；分数只展示 `judgeResult.score`。

## 8. 阶段五：测试和验收

- 增加 judgeRule 校验测试。
- 增加 `USER_CREATED` 不可运行测试。
- 增加 `AI_DRAFT_PENDING` 不可运行测试。
- 增加 hardChecks 失败直接失败测试。
- 增加 hardChecks 失败跳过 judge 且 judge 字段为空的测试。
- 增加 judge 通过和失败的 EvalRun 汇总测试。
- 增加 judge 输出非法 JSON 失败测试。
- 增加从 NodeRun 生成用例后必须补充 judgeRule 的测试。
- 增加前端表单和结果展示测试。
- 增加静态文档检索，确认字段命名没有双轨。

## 9. 风险和处理

- judge LLM 有波动性，因此必须保留 `judgeRule`、`judgeResult`、`judgeRawText`、`judgeModelOfferingKey` 和 `judgePromptVersion`，方便人工复核。
- judge 模型供应项未配置会导致 Eval 无法执行，因此 EvalSuite 确认前必须校验。
- judgeRule 写得过于模糊会降低判定质量，因此前端应提供写法引导，但不得自动确认 AI 生成规则。
- 当前代码以 assertions 决定主链，本次必须整体重写执行路径，不能只改字段名。

## 10. 用户澄清状态

当前没有需要继续向用户澄清的必答问题。以下决策已经冻结：

- judge 模型配置放在 EvalSuite。
- `referenceAnswer` 改为 `referenceSample`。
- `judgeRule` 使用文本字段存储。
- `EvalCaseResult` 使用 `judgeRawText`、`judgePromptVersion`、`errorMessage`。
- 只有 `USER_CONFIRMED` 用例可运行。
- V1 hardChecks 不提供 `JSON_PATH_EQUALS`。
- hardChecks 失败时跳过 judge LLM。
- 不保留 EvalCaseResult 顶层 `score`，分数只来自 `judgeResult.score`。
- 当前系统旧数据会删除，不做旧字段迁移。
