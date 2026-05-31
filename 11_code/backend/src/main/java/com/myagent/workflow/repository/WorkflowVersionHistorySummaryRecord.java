package com.myagent.workflow.repository;

import java.time.Instant;

/**
 * 工作流历史版本摘要记录。
 *
 * @param total 历史版本总数
 * @param latestWorkflowVersionId 最近历史版本主键
 * @param latestVersionNo 最近历史版本号
 * @param latestPublishedAt 最近历史发布时间
 */
public record WorkflowVersionHistorySummaryRecord(
        long total,
        Long latestWorkflowVersionId,
        Integer latestVersionNo,
        Instant latestPublishedAt
) {
}
