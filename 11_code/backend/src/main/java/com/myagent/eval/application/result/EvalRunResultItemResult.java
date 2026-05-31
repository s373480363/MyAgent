package com.myagent.eval.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.eval.domain.EvalCaseConfirmStatus;

import java.util.List;

/**
 * 验收运行结果明细项。
 *
 * @param caseId 用例主键
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param confirmStatus 确认状态
 * @param critical 是否关键用例
 * @param passed 是否通过
 * @param input 输入 JSON
 * @param referenceAnswer 参考答案
 * @param output 输出 JSON
 * @param assertionResults 断言结果
 * @param scoreResult 评分结果
 * @param errorMessage 错误消息
 * @param durationMs 耗时毫秒
 */
public record EvalRunResultItemResult(
        long caseId,
        String caseNo,
        String title,
        EvalCaseConfirmStatus confirmStatus,
        boolean critical,
        boolean passed,
        JsonNode input,
        JsonNode referenceAnswer,
        JsonNode output,
        List<EvalAssertionResultItem> assertionResults,
        JsonNode scoreResult,
        String errorMessage,
        Long durationMs
) {
}
