package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.TraceEventType;

import java.time.Instant;

/**
 * TraceEvent 持久化记录。
 *
 * @param id 数据库主键
 * @param runId AgentRun 数据库主键
 * @param nodeRunId NodeRun 数据库主键
 * @param evalRunId EvalRun 数据库主键
 * @param eventType 事件类型
 * @param summary 摘要
 * @param detailJson 详情 JSON
 * @param eventTime 事件时间
 */
public record RunTraceEventRecord(
        long id,
        Long runId,
        Long nodeRunId,
        Long evalRunId,
        TraceEventType eventType,
        String summary,
        JsonNode detailJson,
        Instant eventTime
) {
}
