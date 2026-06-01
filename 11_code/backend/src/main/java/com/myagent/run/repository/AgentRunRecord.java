package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;

import java.time.Instant;

/**
 * AgentRun 持久化记录。
 *
 * @param id 数据库主键
 * @param runNo 对外运行编号
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param workflowVersionId 工作流版本主键
 * @param parentRunId 父运行主键
 * @param runType 运行类型
 * @param inputJson 输入 JSON
 * @param outputJson 输出 JSON
 * @param status 运行状态
 * @param errorCode 错误码
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record AgentRunRecord(
        long id,
        String runNo,
        long agentId,
        String agentKey,
        long workflowVersionId,
        Long parentRunId,
        RunType runType,
        JsonNode inputJson,
        JsonNode outputJson,
        RunStatus status,
        String errorCode,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
