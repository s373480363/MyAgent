package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.api.ApiError;
import com.myagent.run.domain.RunStatus;

import java.util.List;

/**
 * 节点执行结果。
 *
 * @param status 节点状态
 * @param nodeRunDbId NodeRun 数据库主键
 * @param outputJson 节点输出
 * @param schemaValidationResultJson Schema 校验结果
 * @param selectedEdgeId 条件节点命中的边标识
 * @param errorCode 错误码
 * @param errorMessage 错误消息
 * @param errorDetails 字段级错误明细
 * @param durationMs 执行耗时
 */
public record NodeExecutionResult(
        RunStatus status,
        Long nodeRunDbId,
        JsonNode outputJson,
        JsonNode schemaValidationResultJson,
        String selectedEdgeId,
        String errorCode,
        String errorMessage,
        List<ApiError.Detail> errorDetails,
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
        return new NodeExecutionResult(RunStatus.SUCCESS, null, outputJson, null, null, null, null, null, durationMs);
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
        return new NodeExecutionResult(RunStatus.SUCCESS, null, outputJson, null, selectedEdgeId, null, null, null, durationMs);
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
        return failure(status, errorCode, errorMessage, null, durationMs);
    }

    /**
     * 构造带字段级明细的失败结果。
     *
     * @param status 失败状态
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param errorDetails 字段级错误明细
     * @param durationMs 执行耗时
     * @return 节点执行结果
     */
    public static NodeExecutionResult failure(
            RunStatus status,
            String errorCode,
            String errorMessage,
            List<ApiError.Detail> errorDetails,
            long durationMs
    ) {
        return new NodeExecutionResult(status, null, null, null, null, errorCode, errorMessage, errorDetails, durationMs);
    }

    /**
     * 构造保留节点输出的失败结果。
     *
     * @param status 失败状态
     * @param outputJson 已产生节点输出
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param errorDetails 字段级错误明细
     * @param durationMs 执行耗时
     * @return 节点执行结果
     */
    public static NodeExecutionResult failureWithOutput(
            RunStatus status,
            JsonNode outputJson,
            String errorCode,
            String errorMessage,
            List<ApiError.Detail> errorDetails,
            long durationMs
    ) {
        return new NodeExecutionResult(status, null, outputJson, null, null, errorCode, errorMessage, errorDetails, durationMs);
    }

    /**
     * 返回携带 NodeRun 主键的结果副本。
     *
     * @param nodeRunDbId NodeRun 数据库主键
     * @return 节点执行结果
     */
    public NodeExecutionResult withNodeRunDbId(long nodeRunDbId) {
        return withNodeRunMetadata(nodeRunDbId, schemaValidationResultJson);
    }

    /**
     * 返回携带 NodeRun 主键与 Schema 校验结果的结果副本。
     *
     * @param nodeRunDbId NodeRun 数据库主键
     * @param schemaValidationResultJson Schema 校验结果
     * @return 节点执行结果
     */
    public NodeExecutionResult withNodeRunMetadata(long nodeRunDbId, JsonNode schemaValidationResultJson) {
        return new NodeExecutionResult(
                status,
                nodeRunDbId,
                outputJson,
                schemaValidationResultJson,
                selectedEdgeId,
                errorCode,
                errorMessage,
                errorDetails,
                durationMs
        );
    }
}
