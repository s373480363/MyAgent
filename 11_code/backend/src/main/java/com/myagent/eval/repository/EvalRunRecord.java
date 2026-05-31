package com.myagent.eval.repository;

import com.myagent.run.domain.RunStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * EvalRun 持久化记录。
 *
 * @param id 数据库主键
 * @param runNo 对外验收运行编号
 * @param suiteId 验收套件主键
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 * @param nodeId 被验收节点标识
 * @param agentRunId 关联 AgentRun 数据库主键
 * @param status 运行状态
 * @param totalCaseCount 总用例数
 * @param passedCaseCount 通过用例数
 * @param failedCaseCount 失败用例数
 * @param passRate 通过率
 * @param summary 摘要
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 * @param durationMs 耗时毫秒
 */
public record EvalRunRecord(
        long id,
        String runNo,
        long suiteId,
        long agentId,
        long workflowVersionId,
        String nodeId,
        long agentRunId,
        RunStatus status,
        int totalCaseCount,
        int passedCaseCount,
        int failedCaseCount,
        BigDecimal passRate,
        String summary,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
}
