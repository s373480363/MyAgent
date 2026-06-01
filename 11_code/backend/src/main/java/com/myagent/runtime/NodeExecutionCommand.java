package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;

import java.time.Instant;
import java.util.List;

/**
 * 节点执行协调命令。
 *
 * @param agentRunDbId AgentRun 数据库主键
 * @param agentRunNo AgentRun 对外编号
 * @param agent Agent 主数据
 * @param workflowVersionId 工作流版本主键
 * @param runtimeOptions 工作流版本运行约束
 * @param node 节点定义
 * @param outgoingEdges 节点出边
 * @param workflowContext 工作流上下文
 * @param resolvedInputJson 已解析节点输入；为空时由节点 inputMapping 解析
 * @param traceWriter Trace 写入器
 * @param runStartedAt 运行开始时间
 */
public record NodeExecutionCommand(
        long agentRunDbId,
        String agentRunNo,
        AgentRecord agent,
        long workflowVersionId,
        WorkflowRuntimeOptions runtimeOptions,
        WorkflowNodeDefinition node,
        List<WorkflowEdgeDefinition> outgoingEdges,
        WorkflowContext workflowContext,
        JsonNode resolvedInputJson,
        TraceWriter traceWriter,
        Instant runStartedAt
) {
}
