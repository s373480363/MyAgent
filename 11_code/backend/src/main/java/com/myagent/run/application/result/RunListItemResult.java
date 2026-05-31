package com.myagent.run.application.result;

import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;

import java.time.Instant;

/**
 * 运行列表项。
 *
 * @param runId 对外运行编号
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param agentName Agent 名称
 * @param runType 运行类型
 * @param status 运行状态
 * @param workflowVersionId 工作流版本主键
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record RunListItemResult(
        String runId,
        long agentId,
        String agentKey,
        String agentName,
        RunType runType,
        RunStatus status,
        long workflowVersionId,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
