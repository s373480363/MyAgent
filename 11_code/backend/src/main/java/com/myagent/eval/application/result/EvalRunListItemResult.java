package com.myagent.eval.application.result;

import com.myagent.run.domain.RunStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 验收运行列表项。
 *
 * @param evalRunId 对外验收运行编号
 * @param runId 对外 AgentRun 运行编号
 * @param suiteId 套件主键
 * @param workflowVersionId 工作流版本主键
 * @param nodeId 节点标识
 * @param status 运行状态
 * @param passRate 通过率
 * @param totalCaseCount 总用例数
 * @param passedCaseCount 通过数
 * @param failedCaseCount 失败数
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record EvalRunListItemResult(
        String evalRunId,
        String runId,
        long suiteId,
        long workflowVersionId,
        String nodeId,
        RunStatus status,
        BigDecimal passRate,
        int totalCaseCount,
        int passedCaseCount,
        int failedCaseCount,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
