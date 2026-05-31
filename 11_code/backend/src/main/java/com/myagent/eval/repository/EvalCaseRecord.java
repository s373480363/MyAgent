package com.myagent.eval.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.eval.domain.EvalCaseConfirmStatus;

import java.time.Instant;

/**
 * EvalCase 持久化记录。
 *
 * @param id 数据库主键
 * @param suiteId 验收套件主键
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param inputJson 用例输入
 * @param referenceAnswerJson 参考答案
 * @param assertionsJson 确定性断言配置
 * @param scoreRuleJson 可选评分规则
 * @param critical 是否关键用例
 * @param confirmStatus 确认状态
 * @param sourceAgentRunId 来源 AgentRun 数据库主键
 * @param sourceNodeRunId 来源 NodeRun 数据库主键
 * @param sourceWorkflowVersionId 来源工作流版本主键
 * @param sourceNodeId 来源节点标识
 * @param description 用例说明
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record EvalCaseRecord(
        long id,
        long suiteId,
        String caseNo,
        String title,
        JsonNode inputJson,
        JsonNode referenceAnswerJson,
        JsonNode assertionsJson,
        JsonNode scoreRuleJson,
        boolean critical,
        EvalCaseConfirmStatus confirmStatus,
        Long sourceAgentRunId,
        Long sourceNodeRunId,
        Long sourceWorkflowVersionId,
        String sourceNodeId,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
