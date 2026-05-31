package com.myagent.workflow.repository;

import com.myagent.workflow.domain.ReferencedSchemaVersion;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;

import java.time.Instant;
import java.util.List;

/**
 * 工作流版本持久化记录。
 *
 * @param id 主键
 * @param agentId Agent 主键
 * @param versionNo 版本号
 * @param status 状态
 * @param nodes 节点定义
 * @param edges 边定义
 * @param runtimeOptions 运行约束快照
 * @param referencedSchemaVersions Schema 引用快照
 * @param sourceWorkflowVersionId 来源版本主键
 * @param publishedAt 发布时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record WorkflowVersionRecord(
        long id,
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
}
