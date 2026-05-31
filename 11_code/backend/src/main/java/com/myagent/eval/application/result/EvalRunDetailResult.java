package com.myagent.eval.application.result;

import com.myagent.run.application.result.RunErrorResult;
import com.myagent.run.domain.RunStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 验收运行详情。
 *
 * @param evalRunId 对外验收运行编号
 * @param runId 对外 AgentRun 运行编号
 * @param suite 套件摘要
 * @param agent Agent 摘要
 * @param workflowVersion 工作流版本摘要
 * @param node 节点摘要
 * @param status 运行状态
 * @param passThreshold 通过率阈值
 * @param passRate 通过率
 * @param totalCaseCount 总用例数
 * @param passedCaseCount 通过数
 * @param failedCaseCount 失败数
 * @param criticalFailedCaseCount 关键失败数
 * @param summary 摘要
 * @param error 错误信息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 * @param historyComparison 历史对比
 * @param failureSummary 失败摘要
 */
public record EvalRunDetailResult(
        String evalRunId,
        String runId,
        EvalSuiteSummaryResult suite,
        EvalAgentSummaryResult agent,
        EvalWorkflowVersionSummaryResult workflowVersion,
        EvalNodeSummaryResult node,
        RunStatus status,
        BigDecimal passThreshold,
        BigDecimal passRate,
        int totalCaseCount,
        int passedCaseCount,
        int failedCaseCount,
        long criticalFailedCaseCount,
        String summary,
        RunErrorResult error,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        EvalRunHistoryComparisonResult historyComparison,
        List<EvalFailureSummaryResult> failureSummary
) {
}
