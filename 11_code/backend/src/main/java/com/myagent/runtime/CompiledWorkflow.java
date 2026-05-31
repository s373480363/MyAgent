package com.myagent.runtime;

import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;

import java.util.List;
import java.util.Map;

/**
 * 编译后的工作流。
 *
 * @param snapshot 原始版本快照
 * @param nodesById 节点索引
 * @param outgoingEdgesByNodeId 出边索引
 * @param startNode 开始节点
 */
public record CompiledWorkflow(
        WorkflowVersionSnapshot snapshot,
        Map<String, WorkflowNodeDefinition> nodesById,
        Map<String, List<WorkflowEdgeDefinition>> outgoingEdgesByNodeId,
        WorkflowNodeDefinition startNode
) {

    /**
     * 按节点标识查询节点。
     *
     * @param nodeId 节点标识
     * @return 节点定义
     */
    public WorkflowNodeDefinition getNode(String nodeId) {
        return nodesById.get(nodeId);
    }

    /**
     * 查询节点出边。
     *
     * @param nodeId 节点标识
     * @return 出边列表
     */
    public List<WorkflowEdgeDefinition> getOutgoingEdges(String nodeId) {
        return outgoingEdgesByNodeId.getOrDefault(nodeId, List.of());
    }
}
