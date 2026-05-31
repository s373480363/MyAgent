package com.myagent.runtime;

import com.myagent.workflow.domain.ReferencedSchemaVersion;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;

import java.util.List;

/**
 * 工作流版本运行快照。
 *
 * @param workflowVersionId 工作流版本主键
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param agentName Agent 名称
 * @param versionNo 版本号
 * @param status 版本状态
 * @param nodes 节点定义
 * @param edges 边定义
 * @param runtimeOptions 已持久化运行约束
 * @param referencedSchemaVersions 已持久化 Schema 引用快照
 */
public record WorkflowVersionSnapshot(
        long workflowVersionId,
        long agentId,
        String agentKey,
        String agentName,
        int versionNo,
        WorkflowVersionStatus status,
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowEdgeDefinition> edges,
        WorkflowRuntimeOptions runtimeOptions,
        List<ReferencedSchemaVersion> referencedSchemaVersions
) {
}
