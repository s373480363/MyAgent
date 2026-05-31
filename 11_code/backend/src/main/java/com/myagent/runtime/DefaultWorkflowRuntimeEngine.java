package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.TraceEventType;
import com.myagent.schema.validation.SchemaValidationService;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 默认工作流运行引擎。
 */
@Component
public class DefaultWorkflowRuntimeEngine implements WorkflowRuntimeEngine {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 工作流编译器。
     */
    private final WorkflowCompiler workflowCompiler;

    /**
     * 节点执行器注册表。
     */
    private final NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * Trace 写入器。
     */
    private final TraceWriter traceWriter;

    /**
     * Schema 校验服务。
     */
    private final SchemaValidationService schemaValidationService;

    /**
     * 映射服务。
     */
    private final MappingService mappingService;

    /**
     * 运行限制守卫。
     */
    private final RuntimeLimitGuard runtimeLimitGuard;

    /**
     * 构造运行引擎。
     *
     * @param objectMapper JSON 对象映射器
     * @param workflowCompiler 工作流编译器
     * @param nodeExecutorRegistry 节点执行器注册表
     * @param traceWriter Trace 写入器
     * @param schemaValidationService Schema 校验服务
     * @param mappingService 映射服务
     * @param runtimeLimitGuard 运行限制守卫
     */
    public DefaultWorkflowRuntimeEngine(
            ObjectMapper objectMapper,
            WorkflowCompiler workflowCompiler,
            NodeExecutorRegistry nodeExecutorRegistry,
            TraceWriter traceWriter,
            SchemaValidationService schemaValidationService,
            MappingService mappingService,
            RuntimeLimitGuard runtimeLimitGuard
    ) {
        this.objectMapper = objectMapper;
        this.workflowCompiler = workflowCompiler;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.traceWriter = traceWriter;
        this.schemaValidationService = schemaValidationService;
        this.mappingService = mappingService;
        this.runtimeLimitGuard = runtimeLimitGuard;
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
            WorkflowNodeDefinition currentNode = compiledWorkflow.startNode();
            int currentStep = 0;
            while (currentNode != null) {
                currentStep++;
                runtimeLimitGuard.checkStepLimit(new RunLimitContext(
                        agentRunDbId,
                        agentRunNo,
                        startedAt,
                        null,
                        currentStep,
                        0,
                        null,
                        snapshot.runtimeOptions()
                ));
                NodeExecutionResult result = executeNode(
                        agentRunDbId,
                        agentRunNo,
                        agent,
                        snapshot,
                        workflowContext,
                        compiledWorkflow,
                        currentNode
                );
                if (result.status() != RunStatus.SUCCESS) {
                    return finishWithResult(agentRunDbId, result, startedAtNanos);
                }
                workflowContext.putNodeOutput(currentNode.getNodeId(), result.outputJson());
                workflowContext.replaceRoot(mappingService.applyOutput(
                        workflowContext.root(),
                        currentNode.getOutputMapping(),
                        result.outputJson()
                ));
                if (currentNode.getType() == WorkflowNodeType.END) {
                    workflowContext.setOutput(result.outputJson());
                    return finishSuccess(agentRunDbId, workflowContext.output(), startedAtNanos);
                }
                currentNode = nextNode(compiledWorkflow, currentNode, result.selectedEdgeId());
            }
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "工作流未到达 END 节点。");
        } catch (BizException exception) {
            return finishFailure(agentRunDbId, exception.getErrorCode().getCode(), exception.getMessage(), startedAtNanos);
        } catch (Exception exception) {
            return finishFailure(agentRunDbId, ErrorCode.NODE_EXECUTION_FAILED.getCode(), "工作流执行失败：" + exception.getMessage(), startedAtNanos);
        }
    }

    /**
     * 执行单个节点。
     *
     * @param agentRunDbId AgentRun 数据库主键
     * @param agentRunNo AgentRun 对外编号
     * @param agent Agent 主数据
     * @param snapshot 工作流版本快照
     * @param workflowContext 工作流上下文
     * @param compiledWorkflow 编译后的工作流
     * @param node 节点定义
     * @return 节点执行结果
     */
    private NodeExecutionResult executeNode(
            long agentRunDbId,
            String agentRunNo,
            AgentRecord agent,
            WorkflowVersionSnapshot snapshot,
            WorkflowContext workflowContext,
            CompiledWorkflow compiledWorkflow,
            WorkflowNodeDefinition node
    ) {
        JsonNode inputJson = resolveNodeInput(workflowContext, node);
        NodeRunStartResult nodeRun = traceWriter.createNodeRun(new NodeRunStartRecord(
                agentRunDbId,
                agentRunNo,
                node.getNodeId(),
                node.getName(),
                node.getType(),
                inputJson
        ));
        long startedAtNanos = System.nanoTime();
        try {
            NodeExecutionContext context = new NodeExecutionContext(
                    agentRunDbId,
                    agentRunNo,
                    nodeRun.nodeRunDbId(),
                    agent,
                    snapshot.workflowVersionId(),
                    node,
                    compiledWorkflow.getOutgoingEdges(node.getNodeId()),
                    workflowContext,
                    snapshot.runtimeOptions(),
                    traceWriter,
                    schemaValidationService,
                    mappingService,
                    runtimeLimitGuard
            );
            NodeExecutionResult result = nodeExecutorRegistry.getExecutor(node.getType()).execute(context);
            traceWriter.finishNodeRun(new NodeRunFinishRecord(
                    nodeRun.nodeRunDbId(),
                    result.status(),
                    result.outputJson(),
                    null,
                    result.errorMessage(),
                    elapsedMillis(startedAtNanos)
            ));
            return result;
        } catch (BizException exception) {
            traceWriter.writeEvent(new TraceEventRecord(
                    agentRunDbId,
                    nodeRun.nodeRunDbId(),
                    null,
                    TraceEventType.NODE_ERROR,
                    exception.getMessage(),
                    objectMapper.createObjectNode()
                            .put("errorCode", exception.getErrorCode().getCode())
                            .put("errorMessage", exception.getMessage())
            ));
            traceWriter.finishNodeRun(new NodeRunFinishRecord(
                    nodeRun.nodeRunDbId(),
                    mapFailureStatus(exception),
                    null,
                    null,
                    exception.getMessage(),
                    elapsedMillis(startedAtNanos)
            ));
            return NodeExecutionResult.failure(
                    mapFailureStatus(exception),
                    exception.getErrorCode().getCode(),
                    exception.getMessage(),
                    elapsedMillis(startedAtNanos)
            );
        }
    }

    /**
     * 解析节点输入。
     *
     * @param workflowContext 工作流上下文
     * @param node 节点定义
     * @return 节点输入
     */
    private JsonNode resolveNodeInput(WorkflowContext workflowContext, WorkflowNodeDefinition node) {
        if (node.getType() == WorkflowNodeType.START) {
            return workflowContext.input();
        }
        return mappingService.extractInput(workflowContext.root(), node.getInputMapping());
    }

    /**
     * 选择下一个节点。
     *
     * @param compiledWorkflow 编译后的工作流
     * @param currentNode 当前节点
     * @param selectedEdgeId 命中的边标识
     * @return 下一个节点
     */
    private WorkflowNodeDefinition nextNode(
            CompiledWorkflow compiledWorkflow,
            WorkflowNodeDefinition currentNode,
            String selectedEdgeId
    ) {
        List<WorkflowEdgeDefinition> outgoingEdges = compiledWorkflow.getOutgoingEdges(currentNode.getNodeId());
        if (outgoingEdges.isEmpty()) {
            return null;
        }
        WorkflowEdgeDefinition selected = selectedEdgeId == null
                ? outgoingEdges.stream().min(Comparator.comparing(WorkflowEdgeDefinition::getEdgeId)).orElseThrow()
                : outgoingEdges.stream().filter(edge -> selectedEdgeId.equals(edge.getEdgeId())).findFirst().orElseThrow();
        return compiledWorkflow.getNode(selected.getTargetNodeId());
    }

    /**
     * 映射失败状态。
     *
     * @param exception 业务异常
     * @return 运行状态
     */
    private RunStatus mapFailureStatus(BizException exception) {
        return exception.getErrorCode() == ErrorCode.RUN_TIMEOUT ? RunStatus.TIMEOUT : RunStatus.FAILED;
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
        return new WorkflowRuntimeResult(RunStatus.SUCCESS, outputJson, null, null, elapsedMillis(startedAtNanos));
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
                elapsedMillis(startedAtNanos)
        );
    }

    /**
     * 构造异常失败结果。
     *
     * @param agentRunDbId AgentRun 主键
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param startedAtNanos 开始纳秒
     * @return 运行结果
     */
    private WorkflowRuntimeResult finishFailure(
            long agentRunDbId,
            String errorCode,
            String errorMessage,
            long startedAtNanos
    ) {
        traceWriter.writeEvent(new TraceEventRecord(
                agentRunDbId,
                null,
                null,
                TraceEventType.RUN_FINISHED,
                errorMessage,
                objectMapper.createObjectNode().put("errorCode", errorCode).put("errorMessage", errorMessage)
        ));
        RunStatus status = ErrorCode.RUN_TIMEOUT.getCode().equals(errorCode) ? RunStatus.TIMEOUT : RunStatus.FAILED;
        return new WorkflowRuntimeResult(status, null, errorCode, errorMessage, elapsedMillis(startedAtNanos));
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
