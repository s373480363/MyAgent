package com.myagent.eval.application.command;

import java.math.BigDecimal;

/**
 * 创建验收套件命令。
 *
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 * @param nodeId 被验收节点标识
 * @param name 套件名称
 * @param goal 验收目标
 * @param passThreshold 通过率阈值
 */
public record CreateEvalSuiteCommand(
        long agentId,
        long workflowVersionId,
        String nodeId,
        String name,
        String goal,
        BigDecimal passThreshold
) {
}
