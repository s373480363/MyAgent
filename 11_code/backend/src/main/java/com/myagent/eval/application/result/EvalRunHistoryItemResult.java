package com.myagent.eval.application.result;

import com.myagent.run.domain.RunStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 验收历史对比项。
 *
 * @param evalRunId 对外验收运行编号
 * @param runId 对外 AgentRun 运行编号
 * @param status 运行状态
 * @param passRate 通过率
 * @param totalCaseCount 总用例数
 * @param passedCaseCount 通过数
 * @param failedCaseCount 失败数
 * @param criticalFailedCaseCount 关键失败数
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 * @param passRateDeltaFromPrevious 相对上一轮通过率差值
 * @param passedCaseCountDeltaFromPrevious 相对上一轮通过数差值
 * @param failedCaseCountDeltaFromPrevious 相对上一轮失败数差值
 */
public record EvalRunHistoryItemResult(
        String evalRunId,
        String runId,
        RunStatus status,
        BigDecimal passRate,
        int totalCaseCount,
        int passedCaseCount,
        int failedCaseCount,
        long criticalFailedCaseCount,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        BigDecimal passRateDeltaFromPrevious,
        int passedCaseCountDeltaFromPrevious,
        int failedCaseCountDeltaFromPrevious
) {
}
