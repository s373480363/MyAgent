package com.myagent.eval.application.result;

/**
 * 验收关联工作流版本摘要。
 *
 * @param workflowVersionId 工作流版本主键
 * @param versionNo 版本号
 */
public record EvalWorkflowVersionSummaryResult(long workflowVersionId, int versionNo) {
}
