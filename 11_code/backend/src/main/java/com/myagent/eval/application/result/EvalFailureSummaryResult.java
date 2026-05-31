package com.myagent.eval.application.result;

/**
 * 验收失败摘要。
 *
 * @param caseId 用例主键
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param critical 是否关键用例
 * @param reason 失败原因
 */
public record EvalFailureSummaryResult(
        long caseId,
        String caseNo,
        String title,
        boolean critical,
        String reason
) {
}
