# 节点验收用户手册

## 1. 节点验收是什么

节点验收用于验证 LLM、REVIEW、SUMMARY 等节点在给定输入下，是否达到了你期望的业务效果。

正式验收不是传统“标准答案逐字匹配”测试，而是：

- 用 `judgeRule` 描述自然语言验收规则
- 用 `referenceSample` 提供参考样例
- 用 `hardChecks` 提供可选硬约束
- 用 judge LLM 给出正式通过或失败结论

## 2. 如何创建 EvalSuite

创建验收集时，需要填写：

- 绑定的工作流版本和目标节点
- 验收目标
- judge 模型供应项
- judge 温度
- 通过率阈值

judge 模型配置只属于 EvalSuite，不属于 EvalCase。

## 3. 如何创建 EvalCase

每条验收用例至少包含：

- 用例编号
- 标题
- 输入 JSON

建议同时维护：

- `referenceSample`：参考样例，用来帮助 judge 理解你的期望
- `judgeRule`：自然语言验收规则，正式判定的核心依据
- `hardChecks`：结构、字段、枚举、范围、正则等可选硬约束

## 4. referenceSample、judgeRule、hardChecks 的区别

`referenceSample`

- 是参考样例，不是标准答案
- 可以来自人工整理，也可以来自历史 NodeRun 输出
- 不做逐字精确匹配

`judgeRule`

- 是正式验收主规则
- 需要用自然语言写清“什么样的输出算达标”
- EvalCase 确认前必须非空

`hardChecks`

- 是辅助硬约束
- 适合校验字段存在、枚举、范围、正则、Schema 等稳定事实
- 不承担自然语言质量主判断

## 5. 为什么创建后还要确认

手工创建的用例默认状态是 `USER_CREATED`。

从 NodeRun 生成的草稿用例默认状态是 `AI_DRAFT_PENDING`。

这两种状态都不能进入正式 EvalRun。只有确认为 `USER_CONFIRMED` 后，系统才会把该用例纳入正式通过率统计。

确认前，系统会校验：

- `judgeRule` 是否已填写
- `hardChecks` 结构是否合法
- EvalSuite 的 judge 模型配置是否有效

## 6. 如何从 NodeRun 生成草稿用例

当你从历史 NodeRun 生成用例时，系统会：

- 复制 NodeRun 输入到用例 `input`
- 复制 NodeRun 输出到 `referenceSample`
- 将 `judgeRule` 留空，等待你补充
- 将状态设为 `AI_DRAFT_PENDING`

你需要补充 `judgeRule`，再执行确认。

## 7. 如何运行 EvalRun

运行正式验收时，系统只会执行 `USER_CONFIRMED` 状态的用例。

单条用例的执行顺序是：

1. 执行目标节点
2. 执行 `hardChecks`
3. 如果 `hardChecks` 全部通过，再调用 judge LLM
4. 根据 `judgeResult.passed` 生成最终通过或失败结果

## 8. 如何解读结果

结果页的核心字段有：

- `passed`：该用例是否通过
- `errorMessage`：失败摘要或执行错误
- `hardCheckResults`：逐条硬约束结果
- `judgeResult`：judge LLM 的结构化正式结论
- `judgeRawText`：judge 原始输出
- `judgeModelOfferingKey`：本次 judge 使用的模型供应项
- `judgePromptVersion`：本次 judge 使用的提示词版本

分数只看 `judgeResult.score`。结果项没有顶层 `score` 字段。

## 9. hardChecks 失败为什么会跳过 judge

`hardChecks` 失败表示这条用例已经违反了明确的硬约束，因此系统不会继续调用 judge LLM。

这时结果会表现为：

- `passed=false`
- `judgeResult=null`
- `judgeRawText=null`
- `judgeModelOfferingKey=null`
- `judgePromptVersion=null`
- `errorMessage` 会说明已跳过 judge LLM

这种设计的目的是让失败路径更简单、更稳定，也方便你先修结构问题，再看自然语言质量问题。
