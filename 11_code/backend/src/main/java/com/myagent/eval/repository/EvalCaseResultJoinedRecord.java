package com.myagent.eval.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.eval.domain.EvalCaseConfirmStatus;

/**
 * 验收结果与用例联表记录。
 *
 * @param resultId 结果主键
 * @param evalRunId EvalRun 主键
 * @param evalCaseId EvalCase 主键
 * @param outputJson 输出 JSON
 * @param assertionResultJson 断言结果 JSON
 * @param scoreResultJson 评分结果 JSON
 * @param passed 是否通过
 * @param errorMessage 错误消息
 * @param durationMs 耗时毫秒
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param inputJson 用例输入
 * @param referenceAnswerJson 参考答案
 * @param assertionsJson 断言定义
 * @param critical 是否关键用例
 * @param confirmStatus 确认状态
 */
public record EvalCaseResultJoinedRecord(
        long resultId,
        long evalRunId,
        long evalCaseId,
        JsonNode outputJson,
        JsonNode assertionResultJson,
        JsonNode scoreResultJson,
        boolean passed,
        String errorMessage,
        Long durationMs,
        String caseNo,
        String title,
        JsonNode inputJson,
        JsonNode referenceAnswerJson,
        JsonNode assertionsJson,
        boolean critical,
        EvalCaseConfirmStatus confirmStatus
) {
}
