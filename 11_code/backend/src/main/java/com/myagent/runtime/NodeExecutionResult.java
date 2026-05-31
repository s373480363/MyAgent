package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;

/**
 * 节点执行结果。
 *
 * @param status 节点状态
 * @param outputJson 节点输出
 * @param selectedEdgeId 条件节点命中的边标识
 * @param errorCode 错误码
 * @param errorMessage 错误消息
 * @param durationMs 执行耗时
 */
public record NodeExecutionResult(
        RunStatus status,
        JsonNode outputJson,
        String selectedEdgeId,
        String errorCode,
        String errorMessage,
        Long durationMs
) {

    /**
     * 构造成功结果。
     *
     * @param outputJson 节点输出
     * @param durationMs 执行耗时
     * @return 节点执行结果
     */
    public static NodeExecutionResult success(JsonNode outputJson, long durationMs) {
        return new NodeExecutionResult(RunStatus.SUCCESS, outputJson, null, null, null, durationMs);
    }

    /**
     * 构造带分支的成功结果。
     *
     * @param outputJson 节点输出
     * @param selectedEdgeId 命中的边标识
     * @param durationMs 执行耗时
     * @return 节点执行结果
     */
    public static NodeExecutionResult success(JsonNode outputJson, String selectedEdgeId, long durationMs) {
        return new NodeExecutionResult(RunStatus.SUCCESS, outputJson, selectedEdgeId, null, null, durationMs);
    }

    /**
     * 构造失败结果。
     *
     * @param status 失败状态
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param durationMs 执行耗时
     * @return 节点执行结果
     */
    public static NodeExecutionResult failure(RunStatus status, String errorCode, String errorMessage, long durationMs) {
        return new NodeExecutionResult(status, null, null, errorCode, errorMessage, durationMs);
    }
}
