package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Agent 消息持久化记录。
 *
 * @param id 数据库主键
 * @param parentRunId 父运行主键
 * @param childRunId 子运行主键
 * @param sourceAgentId 来源 Agent 主键
 * @param targetAgentId 目标 Agent 主键
 * @param inputJson 输入 JSON
 * @param outputJson 输出 JSON
 * @param summary 摘要
 * @param createdAt 创建时间
 */
public record AgentMessageRecord(
        long id,
        long parentRunId,
        long childRunId,
        long sourceAgentId,
        long targetAgentId,
        JsonNode inputJson,
        JsonNode outputJson,
        String summary,
        Instant createdAt
) {
}
