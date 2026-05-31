package com.myagent.workflow.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;

import java.util.List;

/**
 * 保存工作流草稿命令。
 *
 * @param agentId Agent 主键
 * @param nodes 节点定义
 * @param edges 边定义
 * @param runtimeOptions 原始运行约束对象
 */
public record SaveWorkflowDraftCommand(
        long agentId,
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowEdgeDefinition> edges,
        JsonNode runtimeOptions
) {
}
