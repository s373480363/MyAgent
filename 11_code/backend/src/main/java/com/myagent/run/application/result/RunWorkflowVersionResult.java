package com.myagent.run.application.result;

import com.myagent.workflow.domain.WorkflowVersionStatus;

/**
 * 运行详情中的工作流版本摘要。
 *
 * @param workflowVersionId 工作流版本主键
 * @param versionNo 版本号
 * @param status 版本状态
 */
public record RunWorkflowVersionResult(long workflowVersionId, int versionNo, WorkflowVersionStatus status) {
}
