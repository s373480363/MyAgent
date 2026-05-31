package com.myagent.workflow.application.query;

/**
 * 工作流版本详情查询。
 *
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 */
public record GetWorkflowVersionQuery(long agentId, long workflowVersionId) {
}
