package com.myagent.eval.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.eval.domain.EvalCaseConfirmStatus;

import java.time.Instant;

/**
 * 验收用例详情。
 *
 * @param caseId 用例主键
 * @param suiteId 套件主键
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param input 用例输入 JSON
 * @param referenceSample 参考样例 JSON
 * @param judgeRule 自然语言验收规则
 * @param hardChecks 硬约束配置数组
 * @param critical 是否为关键用例
 * @param confirmStatus 确认状态
 * @param sourceRunId 来源运行编号
 * @param sourceNodeRunId 来源 NodeRun 主键
 * @param sourceWorkflowVersionId 来源工作流版本主键
 * @param sourceNodeId 来源节点标识
 * @param description 用例说明
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record EvalCaseResult(
        long caseId,
        long suiteId,
        String caseNo,
        String title,
        JsonNode input,
        JsonNode referenceSample,
        String judgeRule,
        JsonNode hardChecks,
        boolean critical,
        EvalCaseConfirmStatus confirmStatus,
        String sourceRunId,
        Long sourceNodeRunId,
        Long sourceWorkflowVersionId,
        String sourceNodeId,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
