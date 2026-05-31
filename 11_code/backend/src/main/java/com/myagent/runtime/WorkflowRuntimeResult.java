package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;

/**
 * 工作流运行结果。
 *
 * @param status 运行状态
 * @param outputJson 输出 JSON
 * @param errorCode 错误码
 * @param errorMessage 错误消息
 * @param durationMs 耗时毫秒
 */
public record WorkflowRuntimeResult(
        RunStatus status,
        JsonNode outputJson,
        String errorCode,
        String errorMessage,
        long durationMs
) {
}
