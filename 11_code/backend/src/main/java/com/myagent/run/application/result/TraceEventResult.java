package com.myagent.run.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.TraceEventType;

import java.time.Instant;

/**
 * Trace 事件结果。
 *
 * @param traceEventId TraceEvent 主键
 * @param nodeRunId NodeRun 主键
 * @param evalRunId EvalRun 主键
 * @param eventType 事件类型
 * @param summary 摘要
 * @param detailJson 详情 JSON
 * @param eventTime 事件时间
 */
public record TraceEventResult(
        long traceEventId,
        Long nodeRunId,
        Long evalRunId,
        TraceEventType eventType,
        String summary,
        JsonNode detailJson,
        Instant eventTime
) {
}
