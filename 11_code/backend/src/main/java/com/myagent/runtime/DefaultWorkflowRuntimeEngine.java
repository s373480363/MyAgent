package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.TraceEventType;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 默认工作流运行引擎。
 */
@Component
public class DefaultWorkflowRuntimeEngine implements WorkflowRuntimeEngine {

    /**
     * LangGraph4j 起始节点。
     */
    private static final String LANGGRAPH_START = "__START__";

    /**
     * LangGraph4j 结束节点。
     */
    private static final String LANGGRAPH_END = "__END__";

    /**
     * 图状态中的工作流上下文键。
     */
    private static final String STATE_WORKFLOW_CONTEXT = "workflowContext";

    /**
     * 图状态中的当前执行步数键。
     */
    private static final String STATE_CURRENT_STEP = "currentStep";

    /**
     * 图状态中的下一节点标识键。
     */
    private static final String STATE_NEXT_NODE_ID = "nextNodeId";

    /**
     * 图状态中的运行结果键。
     */
    private static final String STATE_RUNTIME_RESULT = "runtimeResult";

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 工作流编译器。
     */
    private final WorkflowCompiler workflowCompiler;

    /**
     * Trace 写入器。
     */
    private final TraceWriter traceWriter;

    /**
     * 映射服务。
     */
    private final MappingService mappingService;

    /**
     * 运行限制守卫。
     */
    private final RuntimeLimitGuard runtimeLimitGuard;

    /**
     * 节点执行协调器。
     */
    private final NodeExecutionRunner nodeExecutionRunner;

    /**
     * 构造运行引擎。
     *
     * @param objectMapper JSON 对象映射器
     * @param workflowCompiler 工作流编译器
     * @param traceWriter Trace 写入器
     * @param mappingService 映射服务
     * @param runtimeLimitGuard 运行限制守卫
     * @param nodeExecutionRunner 节点执行协调器
     */
    public DefaultWorkflowRuntimeEngine(
            ObjectMapper objectMapper,
            WorkflowCompiler workflowCompiler,
            TraceWriter traceWriter,
            MappingService mappingService,
            RuntimeLimitGuard runtimeLimitGuard,
            NodeExecutionRunner nodeExecutionRunner
    ) {
        this.objectMapper = objectMapper;
        this.workflowCompiler = workflowCompiler;
        this.traceWriter = traceWriter;
        this.mappingService = mappingService;
        this.runtimeLimitGuard = runtimeLimitGuard;
        this.nodeExecutionRunner = nodeExecutionRunner;
    }

    /**
     * 执行工作流版本快照。
     *
     * @param agentRunDbId AgentRun 数据库主键
     * @param agentRunNo AgentRun 对外编号
     * @param agent Agent 主数据
     * @param snapshot 工作流版本快照
     * @param input 输入 JSON
     * @return 执行结果
     */
    @Override
    public WorkflowRuntimeResult execute(
            long agentRunDbId,
            String agentRunNo,
            AgentRecord agent,
            WorkflowVersionSnapshot snapshot,
            JsonNode input
    ) {
        long startedAtNanos = System.nanoTime();
        Instant startedAt = Instant.now();
        WorkflowContext workflowContext = new WorkflowContext(objectMapper, input);
        try {
            CompiledWorkflow compiledWorkflow = workflowCompiler.compile(snapshot);
            StateGraph<AgentState> graph = buildExecutionGraph(
                    compiledWorkflow,
                    agentRunDbId,
                    agentRunNo,
                    agent,
                    snapshot,
                    startedAt,
                    startedAtNanos
            );
            Map<String, Object> initialState = new LinkedHashMap<>();
            initialState.put(STATE_WORKFLOW_CONTEXT, workflowContext);
            initialState.put(STATE_CURRENT_STEP, 0);
            initialState.put(STATE_NEXT_NODE_ID, compiledWorkflow.startNode().getNodeId());
            return graph.compile()
                    .invoke(initialState)
                    .flatMap(state -> state.value(STATE_RUNTIME_RESULT))
                    .map(WorkflowRuntimeResult.class::cast)
                    .orElseGet(() -> finishFailure(
                            agentRunDbId,
                            ErrorCode.NODE_EXECUTION_FAILED.getCode(),
                            "工作流未产生运行结果。",
                            null,
                            startedAtNanos
                    ));
        } catch (BizException exception) {
            return finishFailure(
                    agentRunDbId,
                    exception.getErrorCode().getCode(),
                    exception.getMessage(),
                    exception.getDetails(),
                    startedAtNanos
            );
        } catch (Exception exception) {
            return finishFailure(
                    agentRunDbId,
                    ErrorCode.NODE_EXECUTION_FAILED.getCode(),
                    "工作流执行失败：" + exception.getMessage(),
                    null,
                    startedAtNanos
            );
        }
    }

    /**
     * 构造 LangGraph4j 执行图。
     *
     * @param compiledWorkflow 编译后的工作流
     * @param agentRunDbId AgentRun 数据库主键
     * @param agentRunNo AgentRun 对外编号
     * @param agent Agent 主数据
     * @param snapshot 工作流版本快照
     * @param startedAt 运行开始时间
     * @param startedAtNanos 运行开始纳秒
     * @return LangGraph4j 执行图
     */
    private StateGraph<AgentState> buildExecutionGraph(
            CompiledWorkflow compiledWorkflow,
            long agentRunDbId,
            String agentRunNo,
            AgentRecord agent,
            WorkflowVersionSnapshot snapshot,
            Instant startedAt,
            long startedAtNanos
    ) {
        try {
            StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
            for (WorkflowNodeDefinition node : compiledWorkflow.nodesById().values()) {
                graph.addNode(node.getNodeId(), node_async(state -> executeGraphNode(
                        state,
                        compiledWorkflow,
                        agentRunDbId,
                        agentRunNo,
                        agent,
                        snapshot,
                        node,
                        startedAt,
                        startedAtNanos
                )));
                graph.addConditionalEdges(
                        node.getNodeId(),
                        edge_async(state -> state.value(STATE_NEXT_NODE_ID, LANGGRAPH_END)),
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
     * 执行 LangGraph4j 单个图节点。
     *
     * @param state 图状态
     * @param compiledWorkflow 编译后的工作流
     * @param agentRunDbId AgentRun 数据库主键
     * @param agentRunNo AgentRun 对外编号
     * @param agent Agent 主数据
     * @param snapshot 工作流版本快照
     * @param currentNode 当前节点
     * @param startedAt 运行开始时间
     * @param startedAtNanos 运行开始纳秒
     * @return 状态更新
     */
    private Map<String, Object> executeGraphNode(
            AgentState state,
            CompiledWorkflow compiledWorkflow,
            long agentRunDbId,
            String agentRunNo,
            AgentRecord agent,
            WorkflowVersionSnapshot snapshot,
            WorkflowNodeDefinition currentNode,
            Instant startedAt,
            long startedAtNanos
    ) {
        WorkflowContext workflowContext = state.value(STATE_WORKFLOW_CONTEXT)
                .map(WorkflowContext.class::cast)
                .orElseThrow(() -> new BizException(ErrorCode.NODE_EXECUTION_FAILED, "工作流图状态缺少运行上下文。"));
        int currentStep = state.<Integer>value(STATE_CURRENT_STEP, 0) + 1;
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(STATE_WORKFLOW_CONTEXT, workflowContext);
        updates.put(STATE_CURRENT_STEP, currentStep);
        try {
            RunLimitContext runLimitContext = new RunLimitContext(
                    agentRunDbId,
                    agentRunNo,
                    startedAt,
                    null,
                    currentStep,
                    0,
                    null,
                    snapshot.runtimeOptions()
            );
            runtimeLimitGuard.checkRunTimeout(runLimitContext);
            runtimeLimitGuard.checkStepLimit(runLimitContext);
            NodeExecutionResult result = nodeExecutionRunner.execute(new NodeExecutionCommand(
                    agentRunDbId,
                    agentRunNo,
                    agent,
                    snapshot.workflowVersionId(),
                    snapshot.runtimeOptions(),
                    currentNode,
                    compiledWorkflow.getOutgoingEdges(currentNode.getNodeId()),
                    workflowContext,
                    null,
                    traceWriter,
                    startedAt
            ));
            runtimeLimitGuard.checkRunTimeout(runLimitContext);
            if (result.status() != RunStatus.SUCCESS) {
                updates.put(STATE_RUNTIME_RESULT, finishWithResult(agentRunDbId, result, startedAtNanos));
                updates.put(STATE_NEXT_NODE_ID, LANGGRAPH_END);
                return updates;
            }
            workflowContext.putNodeOutput(currentNode.getNodeId(), result.outputJson());
            workflowContext.replaceRoot(mappingService.applyOutput(
                    workflowContext.root(),
                    currentNode.getOutputMapping(),
                    result.outputJson()
            ));
            if (currentNode.getType() == WorkflowNodeType.END) {
                workflowContext.setOutput(result.outputJson());
                updates.put(STATE_RUNTIME_RESULT, finishSuccess(agentRunDbId, workflowContext.output(), startedAtNanos));
                updates.put(STATE_NEXT_NODE_ID, LANGGRAPH_END);
                return updates;
            }
            updates.put(STATE_NEXT_NODE_ID, nextNodeId(compiledWorkflow, currentNode, result.selectedEdgeId()));
            return updates;
        } catch (BizException exception) {
            updates.put(STATE_RUNTIME_RESULT, finishFailure(
                    agentRunDbId,
                    exception.getErrorCode().getCode(),
                    exception.getMessage(),
                    exception.getDetails(),
                    startedAtNanos
            ));
            updates.put(STATE_NEXT_NODE_ID, LANGGRAPH_END);
            return updates;
        }
    }

    /**
     * 选择下一个节点标识。
     *
     * @param compiledWorkflow 编译后的工作流
     * @param currentNode 当前节点
     * @param selectedEdgeId 命中的边标识
     * @return 下一个节点标识
     */
    private String nextNodeId(CompiledWorkflow compiledWorkflow, WorkflowNodeDefinition currentNode, String selectedEdgeId) {
        List<WorkflowEdgeDefinition> outgoingEdges = compiledWorkflow.getOutgoingEdges(currentNode.getNodeId());
        if (outgoingEdges.isEmpty()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "工作流未到达 END 节点。");
        }
        WorkflowEdgeDefinition selected = selectedEdgeId == null
                ? outgoingEdges.stream().min(Comparator.comparing(WorkflowEdgeDefinition::getEdgeId)).orElseThrow()
                : outgoingEdges.stream().filter(edge -> selectedEdgeId.equals(edge.getEdgeId())).findFirst().orElseThrow();
        return selected.getTargetNodeId();
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
        routes.put(LANGGRAPH_END, LANGGRAPH_END);
        for (WorkflowEdgeDefinition edge : compiledWorkflow.getOutgoingEdges(node.getNodeId())) {
            routes.put(edge.getTargetNodeId(), edge.getTargetNodeId());
        }
        return routes;
    }

    /**
     * 构造成功结果。
     *
     * @param agentRunDbId AgentRun 主键
     * @param outputJson 输出 JSON
     * @param startedAtNanos 开始纳秒
     * @return 运行结果
     */
    private WorkflowRuntimeResult finishSuccess(long agentRunDbId, JsonNode outputJson, long startedAtNanos) {
        traceWriter.writeEvent(new TraceEventRecord(
                agentRunDbId,
                null,
                null,
                TraceEventType.RUN_FINISHED,
                "运行成功完成。",
                outputJson
        ));
        return new WorkflowRuntimeResult(RunStatus.SUCCESS, outputJson, null, null, null, elapsedMillis(startedAtNanos));
    }

    /**
     * 构造节点失败结果。
     *
     * @param agentRunDbId AgentRun 主键
     * @param result 节点执行结果
     * @param startedAtNanos 开始纳秒
     * @return 运行结果
     */
    private WorkflowRuntimeResult finishWithResult(long agentRunDbId, NodeExecutionResult result, long startedAtNanos) {
        traceWriter.writeEvent(new TraceEventRecord(
                agentRunDbId,
                null,
                null,
                TraceEventType.RUN_FINISHED,
                result.errorMessage(),
                objectMapper.valueToTree(result)
        ));
        return new WorkflowRuntimeResult(
                result.status(),
                null,
                result.errorCode(),
                result.errorMessage(),
                result.errorDetails(),
                elapsedMillis(startedAtNanos)
        );
    }

    /**
     * 构造异常失败结果。
     *
     * @param agentRunDbId AgentRun 主键
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param errorDetails 字段级错误明细
     * @param startedAtNanos 开始纳秒
     * @return 运行结果
     */
    private WorkflowRuntimeResult finishFailure(
            long agentRunDbId,
            String errorCode,
            String errorMessage,
            List<ApiError.Detail> errorDetails,
            long startedAtNanos
    ) {
        traceWriter.writeEvent(new TraceEventRecord(
                agentRunDbId,
                null,
                null,
                TraceEventType.RUN_FINISHED,
                errorMessage,
                objectMapper.createObjectNode()
                        .put("errorCode", errorCode)
                        .put("errorMessage", errorMessage)
                        .set("details", objectMapper.valueToTree(errorDetails))
        ));
        RunStatus status = ErrorCode.RUN_TIMEOUT.getCode().equals(errorCode) ? RunStatus.TIMEOUT : RunStatus.FAILED;
        return new WorkflowRuntimeResult(status, null, errorCode, errorMessage, errorDetails, elapsedMillis(startedAtNanos));
    }

    /**
     * 计算耗时毫秒。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
