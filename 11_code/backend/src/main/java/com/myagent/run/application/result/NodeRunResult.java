package com.myagent.run.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;

import java.time.Instant;

/**
 * 节点运行结果。
 *
 * @param nodeRunId NodeRun 数据库主键
 * @param nodeId 节点标识
 * @param nodeName 节点名称
 * @param nodeType 节点类型
 * @param input 输入 JSON
 * @param output 输出 JSON
 * @param schemaValidationResult Schema 校验结果
 * @param status 状态
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record NodeRunResult(
        long nodeRunId,
        String nodeId,
        String nodeName,
        String nodeType,
        JsonNode input,
        JsonNode output,
        JsonNode schemaValidationResult,
        RunStatus status,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
