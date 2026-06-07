# LLM 节点验收 Judge Rule v1 变更目的

## 1. 背景

当前节点验收设计把 Eval 理解成传统测试：用户准备输入、参考答案、确定性断言，系统执行节点后主要用断言判断是否通过，LLM 评分只是可选辅助。

这个理解不适合 LLM 节点。

用户配置 LLM、REVIEW、SUMMARY 节点时，真正要验收的是“这个节点在给定输入下是否达到自然语言预期”。例如摘要是否抓住重点、审查是否识别风险、回复是否语气合适、总结是否保留关键事实。这类能力无法主要依赖 JSONPath 精确断言判断。

因此，本次变更要把 LLM 节点验收从“断言主导”重构为“自然语言 judge rule 主导”。

## 2. 目标

本次变更要建立新的 Eval 语义：

- EvalCase 是一条 LLM 节点能力验收场景，不是传统断言测试用例。
- 用户必须用自然语言写明验收规则，即 `judgeRule`。
- 执行目标节点后，系统必须把输入、参考样例、实际输出和 `judgeRule` 交给 judge LLM 判断是否达标。
- EvalRun 的正式通过率主要来自 judge LLM 的结构化判定结果。
- 原确定性断言不再作为主判断方式，只保留为可选硬约束，即 `hardChecks`。
- `referenceAnswer` 不再表达标准答案，只表达参考样例。
- 从 NodeRun 生成 EvalCase 时，复制输出只能作为参考样例，不得生成精确断言。
- judge LLM 必须输出结构化 JSON，包含是否通过、分数、失败原因和逐条规则判断。
- Eval 结果必须可追溯 judge 使用的模型供应项、规则文本、提示词版本和原始输出。

## 3. 非目标

- 本次不验收 JAVA_METHOD、TOOL、EXTERNAL_AGENT 等非 LLM 节点的传统确定性测试能力。
- 本次不要求 judge LLM 绝对稳定，也不把 judge 结果当不可质疑的权威。
- 本次不自动生成用户的验收规则。
- 本次不把 `referenceAnswer` 恢复为固定标准答案。
- 本次不保留“确定性断言优先于 LLM 评分”的旧语义。
- 本次不继续使用 `scoreRule` 表达主评价规则。

## 4. 成功标准

- 产品、架构、接口和 schema 文档都明确：LLM 节点验收以 `judgeRule` 为主。
- 创建 EvalCase 时可以暂存空 `judgeRule`；确认正式 EvalCase 前必须存在有效 `judgeRule`。
- EvalRun 的用例通过与否来自 judge 结构化结果，并受可选 `hardChecks` 失败影响。
- hardChecks 失败时用例直接失败并跳过 judge LLM。
- `assertions` 不再作为正式 EvalCase 主字段。
- `scoreRule` 不再作为可选辅助评分主概念。
- 前端页面引导用户填写自然语言验收规则，而不是主要编辑断言 JSON。
- 从 NodeRun 生成的用例默认携带参考样例和来源信息，仍为待确认状态，用户确认前必须补充 `judgeRule`。
- 验收报告能展示 judge 结论、`judgeResult.score`、逐条规则结果、失败原因和硬约束结果。
