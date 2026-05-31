package com.myagent.workflow.application.result;

import com.myagent.workflow.domain.ReferencedSchemaVersion;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * 当前草稿详情结果。
 */
@Schema(name = "WorkflowDraftResult", description = "当前草稿详情结果。")
public final class WorkflowDraftResult {

    /**
     * 工作流版本主键。
     */
    @Schema(description = "工作流版本主键。", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long workflowVersionId;

    /**
     * Agent 主键。
     */
    @Schema(description = "所属 Agent 主键。", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long agentId;

    /**
     * 版本号。
     */
    @Schema(description = "版本号。", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int versionNo;

    /**
     * 版本状态。
     */
    @Schema(description = "版本状态。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final WorkflowVersionStatus status;

    /**
     * 节点列表。
     */
    @Schema(description = "节点列表。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<WorkflowNodeDefinition> nodes;

    /**
     * 边列表。
     */
    @Schema(description = "边列表。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<WorkflowEdgeDefinition> edges;

    /**
     * 运行约束。
     */
    @Schema(description = "运行约束。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final WorkflowRuntimeOptions runtimeOptions;

    /**
     * Schema 引用快照。
     */
    @Schema(description = "Schema 引用快照。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<ReferencedSchemaVersion> referencedSchemaVersions;

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
     * 构造草稿详情。
     *
     * @param workflowVersionId 工作流版本主键
     * @param agentId Agent 主键
     * @param versionNo 版本号
     * @param status 版本状态
     * @param nodes 节点列表
     * @param edges 边列表
     * @param runtimeOptions 运行约束
     * @param referencedSchemaVersions Schema 引用快照
     * @param sourceWorkflowVersionId 来源版本主键
     * @param publishedAt 发布时间
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public WorkflowDraftResult(
            long workflowVersionId,
            long agentId,
            int versionNo,
            WorkflowVersionStatus status,
            List<WorkflowNodeDefinition> nodes,
            List<WorkflowEdgeDefinition> edges,
            WorkflowRuntimeOptions runtimeOptions,
            List<ReferencedSchemaVersion> referencedSchemaVersions,
            Long sourceWorkflowVersionId,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.workflowVersionId = workflowVersionId;
        this.agentId = agentId;
        this.versionNo = versionNo;
        this.status = status;
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
        this.edges = edges == null ? List.of() : List.copyOf(edges);
        this.runtimeOptions = runtimeOptions;
        this.referencedSchemaVersions = referencedSchemaVersions == null ? List.of() : List.copyOf(referencedSchemaVersions);
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

    public List<WorkflowNodeDefinition> getNodes() {
        return nodes;
    }

    public List<WorkflowEdgeDefinition> getEdges() {
        return edges;
    }

    public WorkflowRuntimeOptions getRuntimeOptions() {
        return runtimeOptions;
    }

    public List<ReferencedSchemaVersion> getReferencedSchemaVersions() {
        return referencedSchemaVersions;
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
