package com.myagent.eval.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.eval.domain.EvalCaseConfirmStatus;

/**
 * 验收结果与用例联表记录。
 *
 * @param resultId 结果主键
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
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param inputJson 用例输入 JSON
 * @param referenceSampleJson 参考样例 JSON
 * @param judgeRuleText 自然语言验收规则
 * @param hardChecksJson 硬约束配置数组
 * @param critical 是否为关键用例
 * @param confirmStatus 确认状态
 */
public record EvalCaseResultJoinedRecord(
        long resultId,
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
        String caseNo,
        String title,
        JsonNode inputJson,
        JsonNode referenceSampleJson,
        String judgeRuleText,
        JsonNode hardChecksJson,
        boolean critical,
        EvalCaseConfirmStatus confirmStatus
) {
}
