package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 默认工作流编译器。
 */
@Component
public class DefaultWorkflowCompiler implements WorkflowCompiler {

    /**
     * LangGraph4j 起始节点。
     */
    private static final String LANGGRAPH_START = "__START__";

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
        CompiledWorkflow metadata = new CompiledWorkflow(
                snapshot,
                Map.copyOf(nodesById),
                outgoingEdgesByNodeId,
                startNode,
                null
        );
        return new CompiledWorkflow(
                snapshot,
                Map.copyOf(nodesById),
                outgoingEdgesByNodeId,
                startNode,
                buildExecutionGraph(metadata)
        );
    }

    /**
     * 构造 LangGraph4j 执行图。
     *
     * @param compiledWorkflow 编译后的工作流元数据
     * @return LangGraph4j 执行图
     */
    private StateGraph<AgentState> buildExecutionGraph(CompiledWorkflow compiledWorkflow) {
        try {
            StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
            for (WorkflowNodeDefinition node : compiledWorkflow.nodesById().values()) {
                graph.addNode(node.getNodeId(), node_async(state -> executionContext(state).executeGraphNode(state, node)));
                graph.addConditionalEdges(
                        node.getNodeId(),
                        edge_async(state -> state.value(WorkflowGraphStateKeys.NEXT_NODE_ID)
                                .map(String.class::cast)
                                .orElse(WorkflowGraphStateKeys.LANGGRAPH_END)),
                        routeMap(compiledWorkflow, node)
                );
            }
            graph.addEdge(LANGGRAPH_START, compiledWorkflow.startNode().getNodeId());
            return graph;
        } catch (Exception exception) {
            if (exception instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(ErrorCode.WORKFLOW_VALIDATION_FAILED, "LangGraph4j 图编译失败：" + exception.getMessage());
        }
    }

    /**
     * 构造 LangGraph4j 条件边路由表。
     *
     * @param compiledWorkflow 编译后的工作流
     * @param node 当前节点
     * @return 路由表
     */
    private Map<String, String> routeMap(CompiledWorkflow compiledWorkflow, WorkflowNodeDefinition node) {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put(WorkflowGraphStateKeys.LANGGRAPH_END, WorkflowGraphStateKeys.LANGGRAPH_END);
        for (WorkflowEdgeDefinition edge : compiledWorkflow.getOutgoingEdges(node.getNodeId())) {
            routes.put(edge.getTargetNodeId(), edge.getTargetNodeId());
        }
        return routes;
    }

    /**
     * 读取运行期执行回调。
     *
     * @param state 图状态
     * @return 运行期执行回调
     */
    private WorkflowGraphExecutionContext executionContext(AgentState state) {
        String executionContextId = state.value(WorkflowGraphStateKeys.EXECUTION_CONTEXT_ID)
                .map(String.class::cast)
                .orElseThrow(() -> new BizException(ErrorCode.NODE_EXECUTION_FAILED, "工作流图状态缺少运行期执行标识。"));
        return WorkflowGraphExecutionRegistry.required(executionContextId);
    }
}
