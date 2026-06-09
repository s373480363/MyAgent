# LLM 节点验收 Judge Rule v1 状态

当前状态：真实验收完成，`llm-node-eval-judge-rule-v1` 主链通过；本轮已补齐 Eval 核心类中文 Javadoc/流程注释，并将 `/api/settings` 当前正式文档与运行时描述统一修订为 6 项白名单设置。

## 最新验收记录
- `test_result/2026-06-08-环境修复后按标准流程真实终验报告-v2.md`

## 本轮已确认通过
- 正式入口严格使用 `11_code/compose.yaml` + `11_code/.env` + `http://127.0.0.1:18080`
- `docker compose up -d --build`、健康检查、OpenAPI 入口、Flyway `1/2/3/4` 迁移通过
- 后端自动化：`mvn clean test` 通过，`Tests run: 122 / Failures: 0 / Errors: 0 / Skipped: 0`
- Testcontainers 真实连接 Docker Desktop，并实际拉起 PostgreSQL
- 前端自动化：`npm test -- --run`、`npm run build`、`npm run openapi:check` 通过
- 正式文档静态检索通过，旧 `referenceAnswer/assertions/scoreRule/judge_rule_json/includeUnconfirmed` 等冲突契约未再作为正式语义出现
- 真实业务链路通过：
  - 空 `judgeRule` 用例允许创建，但确认拒绝
  - 从 `NodeRun` 生成用例时，`referenceSample` 复制自节点输出，状态为 `AI_DRAFT_PENDING`
  - 只有 `USER_CONFIRMED` 用例进入正式 `EvalRun`
  - hardChecks 通过时真实调用 judge LLM，并保存 `judgeResult`、`judgeRawText`、`judgePromptVersion`
  - hardChecks 失败时真实跳过 judge，judge 相关字段为 `null`
  - `hardCheckResults` 为数组
  - 分数只出现在 `judgeResult.score`
- 正式前端页面 `/`、`/evals`、套件详情、运行详情、结果明细已实机核验，浏览器控制台无 `error`/`warn`
- 数据库核验通过：
  - `eval_case` 仅保留 `reference_sample_json`、`judge_rule_text`、`hard_checks_json`
  - `eval_case_result.hard_check_result_json` 默认值为 `[]`
  - `eval_case_result` 无顶层 `score` 列

## 本轮补充修复
- 已补齐 `DefaultEvalHardCheckEvaluator`、`DefaultEvalJudgeEvaluator`、`EvalCaseFormalValidationService`、`DefaultEvalCaseApplicationService` 的中文类/字段/方法 Javadoc 与关键流程注释。
- 已将 `/api/settings` 的当前正式描述从“7 项白名单设置”统一修订为“6 项白名单设置”，覆盖后端 OpenAPI 注解、前端生成产物、发布文档与本状态文档。

## 当前建议
- 本次变更可按主链能力与规范整改均完成处理。
- 历史验收报告保留原始记录，不作为当前正式契约来源。
