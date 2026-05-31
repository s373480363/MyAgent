package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认工作流编译器。
 */
@Component
public class DefaultWorkflowCompiler implements WorkflowCompiler {

    /**
     * 编译工作流版本快照。
     *
     * @param snapshot 工作流版本快照
     * @return 编译后的工作流
     */
    @Override
    public CompiledWorkflow compile(WorkflowVersionSnapshot snapshot) {
        if (snapshot == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "工作流版本快照不能为空。");
        }
        Map<String, WorkflowNodeDefinition> nodesById = new LinkedHashMap<>();
        WorkflowNodeDefinition startNode = null;
        for (WorkflowNodeDefinition node : snapshot.nodes()) {
            if (node == null || node.getNodeId() == null || node.getNodeId().isBlank()) {
                throw new BizException(ErrorCode.WORKFLOW_VALIDATION_FAILED, "工作流存在无效节点。");
            }
            if (nodesById.put(node.getNodeId(), node) != null) {
                throw new BizException(ErrorCode.WORKFLOW_VALIDATION_FAILED, "工作流节点标识重复。");
            }
            if (node.getType() == WorkflowNodeType.START) {
                if (startNode != null) {
                    throw new BizException(ErrorCode.WORKFLOW_VALIDATION_FAILED, "工作流只能存在一个 START 节点。");
                }
                startNode = node;
            }
        }
        if (startNode == null) {
            throw new BizException(ErrorCode.WORKFLOW_VALIDATION_FAILED, "工作流缺少 START 节点。");
        }

        Map<String, List<WorkflowEdgeDefinition>> outgoingEdgesByNodeId = snapshot.edges().stream()
                .filter(edge -> edge != null && edge.getSourceNodeId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        WorkflowEdgeDefinition::getSourceNodeId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        return new CompiledWorkflow(snapshot, Map.copyOf(nodesById), outgoingEdgesByNodeId, startNode);
    }
}
