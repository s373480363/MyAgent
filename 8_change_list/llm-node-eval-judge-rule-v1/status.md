# LLM 节点验收 Judge Rule v1 状态

当前状态：真实验收完成，待整改复验。

## 最新状态（2026-06-07）

验收报告：`test_result/2026-06-07-真实验收报告-v1.md`

本轮真实验收使用正式 Docker 入口 `http://127.0.0.1:18080` 完成。核心 Eval judge 链路已通过：`referenceSample + judgeRule + hardChecks` 可创建、确认、运行；只有 `USER_CONFIRMED` 用例进入正式 EvalRun；hardChecks 通过后调用 judge LLM；hardChecks 失败时跳过 judge 且 judge 字段为空；结果分数只出现在 `judgeResult.score`。

本轮不建议直接标记为“验收通过”，原因是仍存在待整改项：

- P1：Docker 初始模型供应商 `baseUrl=https://api.poe.com/v1` 与当前 Spring AI 调用方式不兼容，首次真实模型调用返回 404；通过正式接口改为 `https://api.poe.com` 后链路通过。
- P1：前端 OpenAPI generated schema 未同步，`npm run openapi:check` 发现 `schema.ts` 从旧 `referenceAnswer/assertions/scoreRule` 生成到新 `referenceSample/judgeRule/hardChecks`。
- P2：部分正式文档仍残留旧“参考答案/断言/评分”语义。
- P2：`14_user_manual/README.md` 为空，缺少用户操作说明。

整改后需要复验 Docker 空库启动、OpenAPI generated schema、文档一致性和一次真实 EvalRun。

## 已确认

- 当前 Eval 设计把 LLM 类节点验收理解成传统断言测试，这是方向性错误。
- LLM、REVIEW、SUMMARY 节点验收的主判断方式应是自然语言规则 + judge LLM。
- 确定性检查只能作为辅助硬约束。
- `referenceAnswer` 改为 `referenceSample`。
- `judgeRule` 使用文本字段存储，数据库字段为 `judge_rule_text`。
- judge 模型供应项和 judge 温度放在 EvalSuite，不放在 EvalCase。
- EvalCaseResult 使用 `judgeRawText`、`judgePromptVersion`、`errorMessage`。
- 只有 `USER_CONFIRMED` EvalCase 可以进入正式 EvalRun。
- V1 hardChecks 不提供 `JSON_PATH_EQUALS`，单值判断使用 `JSON_PATH_IN`。
- hardChecks 失败时用例直接失败并跳过 judge LLM。
- `hardCheckResults` 是数组，数据库默认值为 `[]`。
- 分数只存在于 `judgeResult.score`，不保留 EvalCaseResult 顶层 `score`。
- 当前系统旧数据会删除，因此不需要旧字段迁移。

## 已同步正式文档

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

## 开发前必须遵守的收口结论

1. EvalCase 不允许出现 `judgeModelOfferingKey` 或 `judgeTemperature`。
2. `judgeRule` 不允许出现文本和 JSON 两种存储形态，只能使用 `judge_rule_text`。
3. EvalCaseResult 不允许同时出现 `failureReason`、`errorMessage`、`judgeRawOutput`、`judgeRawText` 多套命名；正式字段是 `errorMessage` 和 `judgeRawText`。
4. `USER_CREATED` 和 `AI_DRAFT_PENDING` 都不是可运行状态。
5. hardChecks 不得复用完整旧 assertions 语义。
6. EvalRun 主链必须从 assertions 通过率改为 judgeResult 通过率。
7. hardChecks 失败时不得调用 judge LLM，judge 相关结果字段必须为空。
8. 开发必须直接更新当前 Flyway 基线和 Java migration，空库重建，不实现旧字段转换链路。

## 后续状态流转

- 开发完成后，将状态更新为“开发完成，待架构复验”。
- 架构复验通过后，将状态更新为“架构复验通过，待用户验收”。
- 用户验收通过后，将状态更新为“验收完成”。

## 本轮开发验收结果

- 代码层级验收记录：`test_result/2026-06-06-代码层级验收记录-v1.md`
- 后端 `mvn clean test` 已通过：`119` 个测试，`0` 失败，`0` 错误。
- 前端 `npm test -- --run` 已通过：`10` 个测试文件，`30` 个测试用例，`0` 失败。
- 前端 `npm run build` 已通过。
- 静态检索已确认正式文档和变更包未再保留旧冲突契约字段。

## 当前结论

本变更当前已完成开发实现和开发侧代码层级验收，可进入架构复验与测试人员真实验收阶段。
