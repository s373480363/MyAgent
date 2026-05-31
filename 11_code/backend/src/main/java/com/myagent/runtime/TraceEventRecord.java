package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.TraceEventType;

/**
 * Trace 事件记录。
 *
 * @param agentRunDbId AgentRun 数据库主键
 * @param nodeRunDbId NodeRun 数据库主键
 * @param evalRunDbId EvalRun 数据库主键
 * @param eventType 事件类型
 * @param summary 摘要
 * @param detailJson 详情 JSON
 */
public record TraceEventRecord(
        Long agentRunDbId,
        Long nodeRunDbId,
        Long evalRunDbId,
        TraceEventType eventType,
        String summary,
        JsonNode detailJson
) {
}
