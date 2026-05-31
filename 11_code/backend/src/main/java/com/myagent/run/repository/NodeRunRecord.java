package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;

import java.time.Instant;

/**
 * NodeRun 持久化记录。
 *
 * @param id 数据库主键
 * @param runId AgentRun 数据库主键
 * @param nodeId 节点标识
 * @param nodeName 节点名称
 * @param nodeType 节点类型
 * @param inputJson 输入 JSON
 * @param outputJson 输出 JSON
 * @param schemaValidationResultJson Schema 校验结果 JSON
 * @param status 运行状态
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record NodeRunRecord(
        long id,
        long runId,
        String nodeId,
        String nodeName,
        String nodeType,
        JsonNode inputJson,
        JsonNode outputJson,
        JsonNode schemaValidationResultJson,
        RunStatus status,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
