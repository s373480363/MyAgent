package com.myagent.eval.repository;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * EvalCaseResult 持久化记录。
 *
 * @param id 数据库主键
 * @param evalRunId EvalRun 数据库主键
 * @param evalCaseId EvalCase 数据库主键
 * @param outputJson 节点输出
 * @param assertionResultJson 断言结果
 * @param scoreResultJson 评分结果
 * @param passed 是否通过
 * @param errorMessage 错误消息
 * @param durationMs 耗时毫秒
 * @param createdAt 创建时间
 */
public record EvalCaseResultRecord(
        long id,
        long evalRunId,
        long evalCaseId,
        JsonNode outputJson,
        JsonNode assertionResultJson,
        JsonNode scoreResultJson,
        boolean passed,
        String errorMessage,
        Long durationMs,
        Instant createdAt
) {
}
