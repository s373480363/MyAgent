package com.myagent.eval.application.result;

import com.myagent.eval.domain.EvalSuiteStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 验收套件列表项。
 *
 * @param suiteId 验收套件主键
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 * @param nodeId 节点标识
 * @param name 套件名称
 * @param goal 验收目标
 * @param passThreshold 通过率阈值
 * @param status 套件状态
 * @param updatedAt 更新时间
 */
public record EvalSuiteListItemResult(
        long suiteId,
        long agentId,
        long workflowVersionId,
        String nodeId,
        String name,
        String goal,
        BigDecimal passThreshold,
        EvalSuiteStatus status,
        Instant updatedAt
) {
}
