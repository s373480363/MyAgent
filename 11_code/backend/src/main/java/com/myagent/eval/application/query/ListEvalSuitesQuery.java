package com.myagent.eval.application.query;

import com.myagent.eval.domain.EvalSuiteStatus;

/**
 * 查询验收套件列表参数。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 * @param nodeId 节点标识
 * @param status 套件状态
 * @param keyword 关键词
 */
public record ListEvalSuitesQuery(
        long page,
        long pageSize,
        Long agentId,
        Long workflowVersionId,
        String nodeId,
        EvalSuiteStatus status,
        String keyword
) {
}
