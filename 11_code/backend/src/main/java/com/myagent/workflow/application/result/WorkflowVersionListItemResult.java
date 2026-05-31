package com.myagent.workflow.application.result;

import com.myagent.workflow.domain.WorkflowVersionStatus;

import java.time.Instant;

/**
 * 工作流版本列表项。
 *
 * @param workflowVersionId 工作流版本主键
 * @param versionNo 版本号
 * @param status 状态
 * @param sourceWorkflowVersionId 来源版本主键
 * @param publishedAt 发布时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record WorkflowVersionListItemResult(
        long workflowVersionId,
        int versionNo,
        WorkflowVersionStatus status,
        Long sourceWorkflowVersionId,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
