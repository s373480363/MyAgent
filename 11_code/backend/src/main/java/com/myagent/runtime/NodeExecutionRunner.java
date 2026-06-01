package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.TraceEventType;
import com.myagent.schema.validation.SchemaValidationError;
import com.myagent.schema.validation.SchemaValidationResult;
import com.myagent.schema.validation.SchemaValidationService;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 节点执行协调器。
 */
@Component
public class NodeExecutionRunner {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 节点执行器注册表。
     */
    private final NodeExecutorRegistry nodeExecutorRegistry;

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
     * 平台设置读取器。
     */
    private final PlatformSettingsResolver platformSettingsResolver;

    /**
     * 活跃子运行登记表。
     */
    private final ActiveChildRunRegistry activeChildRunRegistry;

    /**
     * 节点执行线程池。
     */
    private final ExecutorService nodeExecutionExecutorService;

    /**
     * 构造节点执行协调器。
     *
     * @param objectMapper JSON 对象映射器
     * @param nodeExecutorRegistry 节点执行器注册表
     * @param schemaValidationService Schema 校验服务
     * @param mappingService 映射服务
     * @param runtimeLimitGuard 运行限制守卫
     * @param platformSettingsResolver 平台设置读取器
     * @param activeChildRunRegistry 活跃子运行登记表
     * @param nodeExecutionExecutorService 节点执行线程池
     */
    public NodeExecutionRunner(
            ObjectMapper objectMapper,
            NodeExecutorRegistry nodeExecutorRegistry,
            SchemaValidationService schemaValidationService,
            MappingService mappingService,
            RuntimeLimitGuard runtimeLimitGuard,
            PlatformSettingsResolver platformSettingsResolver,
            ActiveChildRunRegistry activeChildRunRegistry,
            @Qualifier("nodeExecutionExecutorService") ExecutorService nodeExecutionExecutorService
    ) {
        this.objectMapper = objectMapper;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.schemaValidationService = schemaValidationService;
        this.mappingService = mappingService;
        this.runtimeLimitGuard = runtimeLimitGuard;
        this.platformSettingsResolver = platformSettingsResolver;
        this.activeChildRunRegistry = activeChildRunRegistry;
        this.nodeExecutionExecutorService = nodeExecutionExecutorService;
    }

    /**
     * 执行单个节点并负责节点级运行记录与 Trace 语义。
     *
     * @param command 节点执行命令
     * @return 节点执行结果
     */
    public NodeExecutionResult execute(NodeExecutionCommand command) {
        // Eval 传入已解析节点输入时必须跳过 inputMapping，避免从历史 NodeRun 生成的用例被二次映射。
        JsonNode inputJson = command.resolvedInputJson() == null
                ? resolveNodeInput(command.workflowContext(), command.node())
                : command.resolvedInputJson();
        WorkflowNodeDefinition executionNode = command.resolvedInputJson() == null
                ? command.node()
                : withoutInputMapping(command.node());
        WorkflowContext executionContext = command.resolvedInputJson() == null
                ? command.workflowContext()
                : new WorkflowContext(objectMapper, inputJson);
        // NodeRun 先创建，后续 Schema、模型调用和错误 Trace 都以这个节点运行记录为审计锚点。
        NodeRunStartResult nodeRun = command.traceWriter().createNodeRun(new NodeRunStartRecord(
                command.agentRunDbId(),
                command.agentRunNo(),
                command.node().getNodeId(),
                command.node().getName(),
                command.node().getType(),
                inputJson
        ));
        long startedAtNanos = System.nanoTime();
        Instant nodeStartedAt = Instant.now();
        Integer nodeTimeoutSeconds = resolveNodeTimeoutSeconds(command.node());
        List<SchemaValidationRecord> schemaValidationResults = new ArrayList<>();
        try {
            NodeExecutionContext context = new NodeExecutionContext(
                    command.agentRunDbId(),
                    command.agentRunNo(),
                    nodeRun.nodeRunDbId(),
                    command.agent(),
                    command.workflowVersionId(),
                    executionNode,
                    command.outgoingEdges(),
                    executionContext,
                    command.runtimeOptions(),
                    command.traceWriter(),
                    schemaValidationService,
                    schemaValidationResults,
                    mappingService,
                    runtimeLimitGuard
            );
            // 节点执行前后都检查节点超时；真实阻塞调用由 executeNodeWithTimeout 负责用 Future 包住。
            RunLimitContext beforeNodeContext = new RunLimitContext(
                    command.agentRunDbId(),
                    command.agentRunNo(),
                    null,
                    nodeStartedAt,
                    0,
                    0,
                    nodeTimeoutSeconds,
                    command.runtimeOptions()
            );
            runtimeLimitGuard.checkNodeTimeout(beforeNodeContext);
            Long effectiveTimeoutMillis = resolveEffectiveTimeoutMillis(command.runStartedAt(), command.runtimeOptions(), nodeTimeoutSeconds);
            NodeExecutionResult result = executeNodeWithTimeout(context, effectiveTimeoutMillis);
            runtimeLimitGuard.checkNodeTimeout(beforeNodeContext);
            JsonNode schemaValidationResultJson = schemaValidationResultsJson(schemaValidationResults);
            // 成功或业务失败都要把 Schema 校验汇总写回 NodeRun，运行详情才能直接回放字段级原因。
            command.traceWriter().finishNodeRun(new NodeRunFinishRecord(
                    nodeRun.nodeRunDbId(),
                    result.status(),
                    result.outputJson(),
                    schemaValidationResultJson,
                    result.errorMessage(),
                    elapsedMillis(startedAtNanos)
            ));
            return result.withNodeRunMetadata(nodeRun.nodeRunDbId(), schemaValidationResultJson);
        } catch (BizException exception) {
            JsonNode outputJson = exception instanceof NodeOutputSchemaValidationException outputException
                    ? outputException.getOutputJson()
                    : null;
            JsonNode schemaValidationResultJson = schemaValidationResultsJson(schemaValidationResults);
            command.traceWriter().writeEvent(new TraceEventRecord(
                    command.agentRunDbId(),
                    nodeRun.nodeRunDbId(),
                    null,
                    TraceEventType.NODE_ERROR,
                    exception.getMessage(),
                    nodeErrorDetail(exception)
            ));
            command.traceWriter().finishNodeRun(new NodeRunFinishRecord(
                    nodeRun.nodeRunDbId(),
                    mapFailureStatus(exception),
                    outputJson,
                    schemaValidationResultJson,
                    exception.getMessage(),
                    elapsedMillis(startedAtNanos)
            ));
            if (outputJson != null) {
                return NodeExecutionResult.failureWithOutput(
                        mapFailureStatus(exception),
                        outputJson,
                        exception.getErrorCode().getCode(),
                        exception.getMessage(),
                        exception.getDetails(),
                        elapsedMillis(startedAtNanos)
                ).withNodeRunMetadata(nodeRun.nodeRunDbId(), schemaValidationResultJson);
            }
            return NodeExecutionResult.failure(
                    mapFailureStatus(exception),
                    exception.getErrorCode().getCode(),
                    exception.getMessage(),
                    exception.getDetails(),
                    elapsedMillis(startedAtNanos)
            ).withNodeRunMetadata(nodeRun.nodeRunDbId(), schemaValidationResultJson);
        } catch (RuntimeException exception) {
            JsonNode schemaValidationResultJson = schemaValidationResultsJson(schemaValidationResults);
            if (Thread.currentThread().isInterrupted()) {
                activeChildRunRegistry.cancelActiveChildren(nodeRun.nodeRunDbId(), "父节点被中断，级联取消子运行。");
            }
            command.traceWriter().writeEvent(new TraceEventRecord(
                    command.agentRunDbId(),
                    nodeRun.nodeRunDbId(),
                    null,
                    TraceEventType.NODE_ERROR,
                    exception.getMessage(),
                    objectMapper.createObjectNode()
                            .put("errorCode", ErrorCode.NODE_EXECUTION_FAILED.getCode())
                            .put("errorMessage", exception.getMessage())
            ));
            command.traceWriter().finishNodeRun(new NodeRunFinishRecord(
                    nodeRun.nodeRunDbId(),
                    RunStatus.FAILED,
                    null,
                    schemaValidationResultJson,
                    exception.getMessage(),
                    elapsedMillis(startedAtNanos)
            ));
            return NodeExecutionResult.failure(
                    RunStatus.FAILED,
                    ErrorCode.NODE_EXECUTION_FAILED.getCode(),
                    exception.getMessage(),
                    elapsedMillis(startedAtNanos)
            ).withNodeRunMetadata(nodeRun.nodeRunDbId(), schemaValidationResultJson);
        }
    }

    /**
     * 使用运行总超时和节点级超时包装节点执行器。
     *
     * @param context 节点执行上下文
     * @param effectiveTimeoutMillis 实际超时毫秒数
     * @return 节点执行结果
     */
    private NodeExecutionResult executeNodeWithTimeout(NodeExecutionContext context, Long effectiveTimeoutMillis) {
        if (effectiveTimeoutMillis == null) {
            return nodeExecutorRegistry.getExecutor(context.nodeDefinition().getType()).execute(context);
        }
        Future<NodeExecutionResult> future = nodeExecutionExecutorService.submit(
                () -> nodeExecutorRegistry.getExecutor(context.nodeDefinition().getType()).execute(context)
        );
        try {
            return future.get(effectiveTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            activeChildRunRegistry.cancelActiveChildren(context.nodeRunDbId(), "父节点超时，级联取消子运行。");
            throw new BizException(ErrorCode.RUN_TIMEOUT, "节点执行超过运行或节点超时。");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            activeChildRunRegistry.cancelActiveChildren(context.nodeRunDbId(), "父节点被中断，级联取消子运行。");
            throw new BizException(ErrorCode.RUN_TIMEOUT, "节点执行被中断。");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof BizException bizException) {
                if (bizException.getErrorCode() == ErrorCode.RUN_TIMEOUT) {
                    activeChildRunRegistry.cancelActiveChildren(context.nodeRunDbId(), "父节点超时，级联取消子运行。");
                }
                throw bizException;
            }
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "节点执行失败：" + cause.getMessage());
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
     * 复制节点定义并把 inputMapping 收口到已解析输入本身，供 Eval 节点执行使用。
     *
     * @param node 原节点定义
     * @return 使用已解析输入的节点定义
     */
    private WorkflowNodeDefinition withoutInputMapping(WorkflowNodeDefinition node) {
        WorkflowNodeDefinition copy = new WorkflowNodeDefinition();
        copy.setNodeId(node.getNodeId());
        copy.setType(node.getType());
        copy.setName(node.getName());
        copy.setDescription(node.getDescription());
        copy.setInputSchemaRef(node.getInputSchemaRef());
        copy.setOutputSchemaRef(node.getOutputSchemaRef());
        copy.setInputMapping(objectMapper.getNodeFactory().textNode("$.input"));
        copy.setOutputMapping(node.getOutputMapping());
        copy.setTimeoutSeconds(node.getTimeoutSeconds());
        copy.setFailurePolicy(node.getFailurePolicy());
        copy.setConfig(node.getConfig());
        copy.setUi(node.getUi());
        return copy;
    }

    /**
     * 解析节点级超时秒数。
     *
     * @param node 节点定义
     * @return 节点级超时秒数；无需节点超时时返回 null
     */
    private Integer resolveNodeTimeoutSeconds(WorkflowNodeDefinition node) {
        if (node.getTimeoutSeconds() != null) {
            return node.getTimeoutSeconds();
        }
        return switch (node.getType()) {
            case LLM, REVIEW, SUMMARY -> platformSettingsResolver.resolveDefaultLlmTimeoutSeconds();
            case JAVA_METHOD, TOOL -> platformSettingsResolver.resolveDefaultJavaMethodTimeoutSeconds();
            case AGENT_CALL -> platformSettingsResolver.resolveDefaultAgentTimeoutSeconds();
            case EXTERNAL_AGENT -> platformSettingsResolver.resolveDefaultExternalAgentTimeoutSeconds();
            default -> null;
        };
    }

    /**
     * 计算节点执行的实际超时，运行总超时和节点级超时取更小值。
     *
     * @param runStartedAt 运行开始时间
     * @param runtimeOptions 工作流版本运行约束
     * @param nodeTimeoutSeconds 节点级超时秒数
     * @return 实际超时毫秒数；没有超时限制时返回 null
     */
    private Long resolveEffectiveTimeoutMillis(
            Instant runStartedAt,
            WorkflowRuntimeOptions runtimeOptions,
            Integer nodeTimeoutSeconds
    ) {
        Long nodeTimeoutMillis = nodeTimeoutSeconds == null ? null : TimeUnit.SECONDS.toMillis(nodeTimeoutSeconds);
        if (runtimeOptions == null || runStartedAt == null) {
            return nodeTimeoutMillis;
        }
        long runTimeoutMillis = TimeUnit.SECONDS.toMillis(runtimeOptions.getTimeoutSeconds());
        long elapsedMillis = Duration.between(runStartedAt, Instant.now()).toMillis();
        long remainingMillis = runTimeoutMillis - elapsedMillis;
        if (remainingMillis <= 0) {
            throw new BizException(ErrorCode.RUN_TIMEOUT, "运行超过工作流版本总超时。");
        }
        return nodeTimeoutMillis == null ? remainingMillis : Math.min(nodeTimeoutMillis, remainingMillis);
    }

    /**
     * 聚合节点 Schema 校验结果。
     *
     * @param results 校验结果列表
     * @return 可落库 JSON
     */
    private JsonNode schemaValidationResultsJson(List<SchemaValidationRecord> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        ObjectNode aggregate = objectMapper.createObjectNode();
        aggregate.put("valid", results.stream().map(SchemaValidationRecord::result).allMatch(SchemaValidationResult::isValid));
        ArrayNode items = aggregate.putArray("results");
        results.forEach(record -> items.add(schemaValidationResultItem(record)));
        return aggregate;
    }

    /**
     * 构造单条 Schema 校验结果 JSON。
     *
     * @param record Schema 校验记录
     * @return 校验结果 JSON
     */
    private ObjectNode schemaValidationResultItem(SchemaValidationRecord record) {
        SchemaValidationResult result = record.result();
        ObjectNode item = objectMapper.createObjectNode()
                .put("stage", record.stage().getCode())
                .put("valid", result.isValid());
        if (result.getSchemaKey() != null) {
            item.put("schemaKey", result.getSchemaKey());
        }
        if (result.getSchemaVersion() != null) {
            item.put("version", result.getSchemaVersion());
        }
        ArrayNode errors = item.putArray("errors");
        for (SchemaValidationError error : result.getErrors()) {
            errors.add(objectMapper.createObjectNode()
                    .put("path", error.getPath())
                    .put("keyword", error.getKeyword())
                    .put("message", error.getMessage()));
        }
        return item;
    }

    /**
     * 构造节点错误 Trace 详情。
     *
     * @param exception 业务异常
     * @return Trace 详情
     */
    private ObjectNode nodeErrorDetail(BizException exception) {
        ObjectNode detail = objectMapper.createObjectNode()
                .put("errorCode", exception.getErrorCode().getCode())
                .put("errorMessage", exception.getMessage());
        List<ApiError.Detail> details = exception.getDetails();
        if (details != null && !details.isEmpty()) {
            detail.set("details", objectMapper.valueToTree(details));
        }
        return detail;
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
     * 计算耗时毫秒。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
