# LLM 节点验收 Judge Rule v1 状态

当前状态：真实验收完成，`llm-node-eval-judge-rule-v1` 主链通过，建议通过本次变更验收；同时发现 1 项非阻塞基线文档/接口描述偏差，建议单独修订。

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

## 非阻塞基线偏差
- `/api/settings` 实际返回 6 项，但正式发布文档和运行时 OpenAPI 描述仍写 7 项。
- 该问题不阻塞 `llm-node-eval-judge-rule-v1` 主链验收，但不满足基线文档与实现完全一致的要求。

## 当前建议
- 本次变更按主链能力验收通过处理。
- 将 `/api/settings` 的“6/7 项”偏差单独登记并修订文档/描述。
