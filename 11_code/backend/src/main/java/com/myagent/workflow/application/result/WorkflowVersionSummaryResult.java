package com.myagent.workflow.application.result;

import com.myagent.workflow.domain.WorkflowVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 工作流版本摘要。
 */
@Schema(name = "WorkflowVersionSummaryResult", description = "工作流版本摘要。")
public final class WorkflowVersionSummaryResult {

    /**
     * 工作流版本主键。
     */
    @Schema(description = "工作流版本主键。", example = "11")
    private final Long workflowVersionId;

    /**
     * 版本号。
     */
    @Schema(description = "版本号。", example = "3")
    private final Integer versionNo;

    /**
     * 版本状态。
     */
    @Schema(description = "版本状态。")
    private final WorkflowVersionStatus status;

    /**
     * 来源版本主键。
     */
    @Schema(description = "来源版本主键。", example = "10")
    private final Long sourceWorkflowVersionId;

    /**
     * 发布时间。
     */
    @Schema(description = "发布时间。")
    private final Instant publishedAt;

    /**
     * 最近更新时间。
     */
    @Schema(description = "最近更新时间。")
    private final Instant updatedAt;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间。")
    private final Instant createdAt;

    /**
     * 构造版本摘要。
     *
     * @param workflowVersionId 工作流版本主键
     * @param versionNo 版本号
     * @param status 版本状态
     * @param sourceWorkflowVersionId 来源版本主键
     * @param publishedAt 发布时间
     * @param updatedAt 最近更新时间
     * @param createdAt 创建时间
     */
    public WorkflowVersionSummaryResult(
            Long workflowVersionId,
            Integer versionNo,
            WorkflowVersionStatus status,
            Long sourceWorkflowVersionId,
            Instant publishedAt,
            Instant updatedAt,
            Instant createdAt
    ) {
        this.workflowVersionId = workflowVersionId;
        this.versionNo = versionNo;
        this.status = status;
        this.sourceWorkflowVersionId = sourceWorkflowVersionId;
        this.publishedAt = publishedAt;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    public Long getWorkflowVersionId() {
        return workflowVersionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public WorkflowVersionStatus getStatus() {
        return status;
    }

    public Long getSourceWorkflowVersionId() {
        return sourceWorkflowVersionId;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
