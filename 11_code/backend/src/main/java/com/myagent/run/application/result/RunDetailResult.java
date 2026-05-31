package com.myagent.run.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;

import java.time.Instant;
import java.util.List;

/**
 * 运行详情。
 *
 * @param runId 对外运行编号
 * @param agent Agent 摘要
 * @param workflowVersion 工作流版本摘要
 * @param runType 运行类型
 * @param status 运行状态
 * @param parentRunId 父运行编号
 * @param evalRunId 验收运行编号
 * @param input 输入 JSON
 * @param output 输出 JSON
 * @param error 错误信息
 * @param nodeRuns 节点运行全文数组
 * @param traceEvents Trace 事件全文数组
 * @param childRuns 子运行摘要
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record RunDetailResult(
        String runId,
        RunAgentResult agent,
        RunWorkflowVersionResult workflowVersion,
        RunType runType,
        RunStatus status,
        String parentRunId,
        String evalRunId,
        JsonNode input,
        JsonNode output,
        RunErrorResult error,
        List<NodeRunResult> nodeRuns,
        List<TraceEventResult> traceEvents,
        List<ChildRunResult> childRuns,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
