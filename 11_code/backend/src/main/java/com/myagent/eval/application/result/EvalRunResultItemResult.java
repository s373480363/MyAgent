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
 * @param critical 是否为关键用例
 * @param passed 是否通过
 * @param input 用例输入 JSON
 * @param referenceSample 参考样例 JSON
 * @param judgeRule 自然语言验收规则
 * @param hardChecks 硬约束配置数组
 * @param output 节点输出 JSON
 * @param hardCheckResults 硬约束执行结果
 * @param judgeResult judge 结构化结果
 * @param judgeRawText judge 原始文本输出
 * @param judgeModelOfferingKey judge 模型供应项标识
 * @param judgePromptVersion judge 提示词版本
 * @param errorMessage 失败摘要
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
        JsonNode referenceSample,
        String judgeRule,
        JsonNode hardChecks,
        JsonNode output,
        List<EvalHardCheckResultItem> hardCheckResults,
        JsonNode judgeResult,
        String judgeRawText,
        String judgeModelOfferingKey,
        String judgePromptVersion,
        String errorMessage,
        Long durationMs
) {
}
