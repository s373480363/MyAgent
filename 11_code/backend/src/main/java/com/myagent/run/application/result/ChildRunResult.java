package com.myagent.run.application.result;

import com.myagent.run.domain.RunStatus;

import java.time.Instant;

/**
 * 子运行摘要。
 *
 * @param runId 子运行编号
 * @param agent 子运行 Agent 摘要
 * @param status 子运行状态
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 * @param summary 消息摘要
 */
public record ChildRunResult(
        String runId,
        RunAgentResult agent,
        RunStatus status,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        String summary
) {
}
