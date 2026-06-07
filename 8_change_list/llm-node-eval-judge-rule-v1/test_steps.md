# LLM 节点验收 Judge Rule v1 验收步骤

## 1. 静态文档验收

执行：

```powershell
rg -n "确定性优先|LLM 评分是可选辅助|scoreRule|referenceAnswer|failureReason|judgeRawOutput|judge_rule_json|judge_raw_output_json|includeUnconfirmed" D:\myproject\MyAgent\3_product_design D:\myproject\MyAgent\4_arch_design D:\myproject\MyAgent\6_schema_design D:\myproject\MyAgent\7_interface_design
```

预期：

- 正式文档不再保留旧语义。
- 如果历史说明中出现 `assertions`，必须明确写成旧语义或禁止做法，不能作为当前正式接口字段。
- 不出现 `judge_rule_json`、`judge_raw_output_json`、`judgeRawOutput`、顶层 `failureReason`。
- 不出现 `includeUnconfirmed` 运行参数。
- 结果接口不出现顶层 `score` 字段；分数只在 `judgeResult.score` 中出现。
- `JSON_PATH_EQUALS` 只能以“V1 不提供”的否定语义出现，不能作为 V1 支持类型出现。

执行：

```powershell
rg -n --glob "!test_steps.md" "待用户确认|当前建议|需要用户最终确认|建议放在 EvalSuite|建议保留" D:\myproject\MyAgent\8_change_list\llm-node-eval-judge-rule-v1
```

预期：

- 变更包不再把已确认决策写成待确认问题或建议项。

执行：

```powershell
rg -n "judgeRule|referenceSample|hardChecks|hardCheckResults|judgeResult|judgeRawText|judgePromptVersion|USER_CONFIRMED" D:\myproject\MyAgent\3_product_design D:\myproject\MyAgent\4_arch_design D:\myproject\MyAgent\6_schema_design D:\myproject\MyAgent\7_interface_design D:\myproject\MyAgent\8_change_list\llm-node-eval-judge-rule-v1
```

预期：

- 新语义在产品、架构、schema、接口和变更包中都有定义。
- judge 模型字段只出现在 EvalSuite 语义中。
- 只有 `USER_CONFIRMED` 被描述为可运行状态。
- hardChecks 失败被描述为跳过 judge LLM。
- `hardCheckResults` 被描述为数组。

## 2. 后端测试

执行：

```powershell
cd D:\myproject\MyAgent\11_code\backend
.\mvnw test
```

必须覆盖：

- 创建 EvalSuite 保存 `judgeModelOfferingKey` 和 `judgeTemperature`。
- 创建 EvalCase 保存 `judgeRule`、`referenceSample`、`hardChecks`。
- 手工创建 EvalCase 默认 `USER_CREATED`，不能进入正式 EvalRun。
- 从 NodeRun 生成 EvalCase 默认 `AI_DRAFT_PENDING`，不能进入正式 EvalRun。
- 缺少 judgeRule 的用例不能确认。
- 确认后的用例状态为 `USER_CONFIRMED`。
- `listRunnableCases` 只返回 `USER_CONFIRMED`。
- 从 NodeRun 生成用例时输出进入 referenceSample。
- hardChecks 失败时用例失败。
- hardChecks 失败时不调用 judge LLM，EvalCaseResult 中 `judgeResult`、`judgeRawText`、`judgeModelOfferingKey`、`judgePromptVersion` 均为空。
- hardCheckResults 持久化和接口返回都是数组。
- 7 种 hardChecks 类型的字段 schema 均有确认校验测试。
- judgeResult.passed=false 时用例失败。
- judgeResult.passed=true 且 hardChecks 通过时用例通过。
- judge 输出非法 JSON 时用例失败。
- EvalRun 通过率按 EvalCaseResult.passed 计算。
- judge 被调用时 EvalCaseResult 保存 `judgeRawText` 和 `judgePromptVersion`。
- EvalCaseResult 不保存顶层 score。

## 3. 前端测试

执行：

```powershell
cd D:\myproject\MyAgent\11_code\frontend
npm test -- --run
npm run build
```

必须覆盖：

- EvalSuite 表单展示 judge 模型供应项和 judge 温度。
- EvalCase 表单展示自然语言验收规则主输入。
- EvalCase 表单不展示 judge 模型配置。
- 参考样例文案不是标准答案。
- hardChecks 是辅助输入。
- 缺少 judgeRule 时不能提交确认。
- 结果页展示 judge 结论、`judgeResult.score`、`errorMessage`、`judgeRawText`、`judgePromptVersion` 和逐条规则结果。
- hardChecks 失败跳过 judge 时，结果页不展示 judge 分数。

## 4. API 验收

创建 EvalSuite：

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

创建 EvalCase：

```json
{
  "caseNo": "case_001",
  "title": "摘要覆盖关键风险",
  "input": {
    "text": "..."
  },
  "referenceSample": {
    "summary": "参考样例"
  },
  "judgeRule": "判断摘要是否覆盖输入中的关键风险点，不得编造信息。",
  "hardChecks": [
    {
      "type": "JSON_PATH_EXISTS",
      "path": "$.summary"
    }
  ],
  "critical": false
}
```

预期：

- EvalCase 返回体包含 `judgeRule`、`referenceSample`、`hardChecks`。
- EvalCase 返回体不包含 `judgeModelOfferingKey` 和 `judgeTemperature`。
- EvalCase 返回 `confirmStatus=USER_CREATED`。
- 创建接口允许 `judgeRule` 为空，但确认接口必须拒绝空 `judgeRule`。
- 不再要求 `assertions` 非空。
- 不再要求 `scoreRule`。

## 5. 运行验收

执行一个已确认 EvalSuite。

预期：

- 只运行 `USER_CONFIRMED` EvalCase。
- hardChecks 通过且 judge 被调用时，EvalCaseResult 包含 `judgeResult`、`judgeRawText`、`judgePromptVersion`。
- hardChecks 失败时，EvalCaseResult 的 `judgeResult`、`judgeRawText`、`judgeModelOfferingKey`、`judgePromptVersion` 为 null。
- 通过率按 `EvalCaseResult.passed` 统计。
- hardChecks 失败的用例直接失败。
- judge 调用失败的用例失败并记录 `errorMessage`。
- 结果项不返回顶层 `score`。
- Trace 中能看到 EvalCaseResult 事件。

## 6. 数据库验收

执行空库全量迁移。

预期：

- `eval_case` 只包含 `reference_sample_json`、`judge_rule_text`、`hard_checks_json`，不包含旧字段。
- `eval_case_result.hard_check_result_json` 默认值为 `[]`。
- `eval_case_result` 不包含顶层 `score` 列。
- Java migration 不再读取或更新 `score_rule_json`。
