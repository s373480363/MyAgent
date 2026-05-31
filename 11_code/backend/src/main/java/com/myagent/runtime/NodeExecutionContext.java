package com.myagent.runtime;

import com.myagent.agent.repository.AgentRecord;
import com.myagent.schema.validation.SchemaValidationService;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;

import java.util.List;

/**
 * 节点执行上下文。
 *
 * @param agentRunDbId AgentRun 数据库主键
 * @param agentRunNo AgentRun 对外编号
 * @param nodeRunDbId NodeRun 数据库主键
 * @param agentDefinition Agent 主数据
 * @param workflowVersionId 工作流版本主键
 * @param nodeDefinition 节点定义
 * @param outgoingEdges 节点出边
 * @param workflowContext 工作流上下文
 * @param runtimeOptions 工作流版本运行约束快照
 * @param traceWriter Trace 写入器
 * @param schemaValidationService Schema 校验服务
 * @param mappingService 映射服务
 * @param runtimeLimitGuard 运行限制守卫
 */
public record NodeExecutionContext(
        long agentRunDbId,
        String agentRunNo,
        long nodeRunDbId,
        AgentRecord agentDefinition,
        long workflowVersionId,
        WorkflowNodeDefinition nodeDefinition,
        List<WorkflowEdgeDefinition> outgoingEdges,
        WorkflowContext workflowContext,
        WorkflowRuntimeOptions runtimeOptions,
        TraceWriter traceWriter,
        SchemaValidationService schemaValidationService,
        MappingService mappingService,
        RuntimeLimitGuard runtimeLimitGuard
) {
}
