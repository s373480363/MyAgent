package com.myagent.eval.application.result;

import com.myagent.run.domain.RunStatus;

import java.math.BigDecimal;

/**
 * 运行验收套件返回结果。
 *
 * @param evalRunId 对外验收运行编号
 * @param runId 对外 AgentRun 运行编号
 * @param suiteId 套件主键
 * @param status 运行状态
 * @param passRate 通过率
 * @param totalCaseCount 总用例数
 * @param passedCaseCount 通过数
 * @param failedCaseCount 失败数
 * @param summary 摘要
 */
public record EvalRunResult(
        String evalRunId,
        String runId,
        long suiteId,
        RunStatus status,
        BigDecimal passRate,
        int totalCaseCount,
        int passedCaseCount,
        int failedCaseCount,
        String summary
) {
}
