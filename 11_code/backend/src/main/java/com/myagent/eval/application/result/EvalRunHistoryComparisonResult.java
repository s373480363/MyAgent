package com.myagent.eval.application.result;

import java.math.BigDecimal;

/**
 * 验收运行历史对比摘要。
 *
 * @param previousEvalRunId 上一次验收运行编号
 * @param previousRunId 上一次 AgentRun 运行编号
 * @param previousPassRate 上一次通过率
 * @param passRateDelta 通过率差值
 * @param passedCaseCountDelta 通过数差值
 * @param failedCaseCountDelta 失败数差值
 */
public record EvalRunHistoryComparisonResult(
        String previousEvalRunId,
        String previousRunId,
        BigDecimal previousPassRate,
        BigDecimal passRateDelta,
        int passedCaseCountDelta,
        int failedCaseCountDelta
) {
}
