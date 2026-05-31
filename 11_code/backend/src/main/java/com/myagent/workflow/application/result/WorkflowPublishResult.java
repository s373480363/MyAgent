package com.myagent.workflow.application.result;

import com.myagent.workflow.domain.WorkflowVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 工作流发布结果。
 */
@Schema(name = "WorkflowPublishResult", description = "工作流发布结果。")
public final class WorkflowPublishResult {

    /**
     * 工作流版本主键。
     */
    @Schema(description = "工作流版本主键。", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long workflowVersionId;

    /**
     * Agent 主键。
     */
    @Schema(description = "所属 Agent 主键。", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long agentId;

    /**
     * 版本号。
     */
    @Schema(description = "版本号。", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int versionNo;

    /**
     * 状态。
     */
    @Schema(description = "状态。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final WorkflowVersionStatus status;

    /**
     * 来源版本主键。
     */
    @Schema(description = "来源版本主键。", example = "11")
    private final Long sourceWorkflowVersionId;

    /**
     * 发布时间。
     */
    @Schema(description = "发布时间。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Instant publishedAt;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Instant createdAt;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Instant updatedAt;

    /**
     * 构造发布结果。
     *
     * @param workflowVersionId 工作流版本主键
     * @param agentId Agent 主键
     * @param versionNo 版本号
     * @param status 状态
     * @param sourceWorkflowVersionId 来源版本主键
     * @param publishedAt 发布时间
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public WorkflowPublishResult(
            long workflowVersionId,
            long agentId,
            int versionNo,
            WorkflowVersionStatus status,
            Long sourceWorkflowVersionId,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.workflowVersionId = workflowVersionId;
        this.agentId = agentId;
        this.versionNo = versionNo;
        this.status = status;
        this.sourceWorkflowVersionId = sourceWorkflowVersionId;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getWorkflowVersionId() {
        return workflowVersionId;
    }

    public long getAgentId() {
        return agentId;
    }

    public int getVersionNo() {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
