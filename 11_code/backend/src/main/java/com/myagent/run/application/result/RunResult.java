package com.myagent.run.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;

/**
 * 同步运行结果。
 *
 * @param runId 对外运行编号
 * @param agentKey Agent 业务标识
 * @param workflowVersionId 工作流版本主键
 * @param status 运行状态
 * @param output 输出 JSON
 * @param error 错误信息
 * @param durationMs 耗时毫秒
 */
public record RunResult(
        String runId,
        String agentKey,
        long workflowVersionId,
        RunStatus status,
        JsonNode output,
        RunErrorResult error,
        long durationMs
) {
}
