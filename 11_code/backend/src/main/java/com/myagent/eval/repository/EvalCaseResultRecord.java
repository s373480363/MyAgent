package com.myagent.eval.repository;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * EvalCaseResult 持久化记录。
 *
 * @param id 数据库主键
 * @param evalRunId EvalRun 主键
 * @param evalCaseId EvalCase 主键
 * @param outputJson 节点输出 JSON
 * @param hardCheckResultJson hardChecks 执行结果数组
 * @param judgeResultJson judge 结构化结果
 * @param judgeRawText judge 原始文本输出
 * @param judgeModelOfferingKey judge 模型供应项标识
 * @param judgePromptVersion judge 提示词版本
 * @param passed 是否通过
 * @param errorMessage 失败摘要
 * @param durationMs 耗时毫秒
 * @param createdAt 创建时间
 */
public record EvalCaseResultRecord(
        long id,
        long evalRunId,
        long evalCaseId,
        JsonNode outputJson,
        JsonNode hardCheckResultJson,
        JsonNode judgeResultJson,
        String judgeRawText,
        String judgeModelOfferingKey,
        String judgePromptVersion,
        boolean passed,
        String errorMessage,
        Long durationMs,
        Instant createdAt
) {
}
