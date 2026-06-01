package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.domain.RunNoGenerator;
import com.myagent.run.domain.TraceEventType;
import com.myagent.run.repository.AgentMessageRecord;
import com.myagent.run.repository.AgentMessageRepository;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.runtime.ActiveChildRunRegistry;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.RunLimitContext;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.runtime.WorkflowRuntimeEngine;
import com.myagent.runtime.WorkflowRuntimeResult;
import com.myagent.runtime.WorkflowVersionSnapshot;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * AGENT_CALL 节点执行器。
 */
@Component
public class AgentCallNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * AgentRun 仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * AgentMessage 仓储。
     */
    private final AgentMessageRepository agentMessageRepository;

    /**
     * 工作流运行引擎延迟提供器。
     */
    private final ObjectProvider<WorkflowRuntimeEngine> workflowRuntimeEngineProvider;

    /**
     * 活跃子运行登记表。
     */
    private final ActiveChildRunRegistry activeChildRunRegistry;

    /**
     * 运行编号生成器。
     */
    private final RunNoGenerator runNoGenerator;

    /**
     * 构造 AGENT_CALL 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param agentRunRepository AgentRun 仓储
     * @param agentMessageRepository AgentMessage 仓储
     * @param workflowRuntimeEngineProvider 工作流运行引擎延迟提供器
     * @param activeChildRunRegistry 活跃子运行登记表
     * @param runNoGenerator 运行编号生成器
     */
    public AgentCallNodeExecutor(
            ObjectMapper objectMapper,
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            AgentRunRepository agentRunRepository,
            AgentMessageRepository agentMessageRepository,
            ObjectProvider<WorkflowRuntimeEngine> workflowRuntimeEngineProvider,
            ActiveChildRunRegistry activeChildRunRegistry,
            RunNoGenerator runNoGenerator
    ) {
        super(objectMapper);
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.agentRunRepository = agentRunRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.workflowRuntimeEngineProvider = workflowRuntimeEngineProvider;
        this.activeChildRunRegistry = activeChildRunRegistry;
        this.runNoGenerator = runNoGenerator;
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.AGENT_CALL;
    }

    /**
     * 执行 AGENT_CALL 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = extractInput(context);
        String targetAgentKey = readRequiredConfigText(context.nodeDefinition().getConfig(), "targetAgentKey", "内部 Agent 调用节点缺少 targetAgentKey。");
        if (targetAgentKey.equals(context.agentDefinition().agentKey())) {
            throw new BizException(ErrorCode.TARGET_AGENT_NOT_PUBLISHED, "AGENT_CALL 节点不能直接调用当前 Agent 自己。");
        }
        AgentRecord targetAgent = agentRepository.findByAgentKey(targetAgentKey)
                .orElseThrow(() -> new BizException(ErrorCode.TARGET_AGENT_NOT_PUBLISHED, "目标 Agent 不存在：" + targetAgentKey));
        if (targetAgent.status() != EnableStatus.ENABLED || targetAgent.currentPublishedWorkflowVersionId() == null) {
            throw new BizException(ErrorCode.TARGET_AGENT_NOT_PUBLISHED, "目标 Agent 未启用或尚未发布：" + targetAgentKey);
        }
        int childDepth = currentCallDepth(context.agentRunDbId()) + 1;
        context.runtimeLimitGuard().checkCallDepth(new RunLimitContext(
                context.agentRunDbId(),
                context.agentRunNo(),
                null,
                null,
                0,
                childDepth,
                null,
                context.runtimeOptions()
        ));

        WorkflowVersionRecord targetWorkflowVersion = workflowVersionRepository.findById(targetAgent.currentPublishedWorkflowVersionId())
                .orElseThrow(() -> new BizException(ErrorCode.TARGET_AGENT_NOT_PUBLISHED, "目标 Agent 当前发布版本不存在：" + targetAgentKey));
        AgentRunRecord childRun = createChildRun(context, targetAgent, targetWorkflowVersion, input);
        activeChildRunRegistry.register(context.nodeRunDbId(), childRun);
        try {
            WorkflowRuntimeResult childResult = workflowRuntimeEngineProvider.getObject().execute(
                    childRun.id(),
                    childRun.runNo(),
                    targetAgent,
                    toSnapshot(targetAgent, targetWorkflowVersion),
                    input
            );
            if (Thread.currentThread().isInterrupted()) {
                String reason = "父节点被中断，级联取消子运行。";
                agentRunRepository.cancelActiveRun(
                        childRun.id(),
                        ErrorCode.RUN_CANCELED.getCode(),
                        reason,
                        elapsedMillis(startedAt)
                );
                WorkflowRuntimeResult canceledResult = new WorkflowRuntimeResult(
                        RunStatus.CANCELED,
                        null,
                        ErrorCode.RUN_CANCELED.getCode(),
                        reason,
                        null,
                        elapsedMillis(startedAt)
                );
                writeTrace(context, targetAgentKey, childRun.runNo(), childDepth, canceledResult, objectMapper.nullNode());
                return NodeExecutionResult.failure(
                        RunStatus.CANCELED,
                        ErrorCode.RUN_CANCELED.getCode(),
                        reason,
                        elapsedMillis(startedAt)
                );
            }
            if (!activeChildRunRegistry.isActive(childRun.id())) {
                WorkflowRuntimeResult canceledResult = new WorkflowRuntimeResult(
                        RunStatus.CANCELED,
                        null,
                        ErrorCode.RUN_CANCELED.getCode(),
                        "父运行已取消，子运行级联取消。",
                        null,
                        elapsedMillis(startedAt)
                );
                writeTrace(context, targetAgentKey, childRun.runNo(), childDepth, canceledResult, objectMapper.nullNode());
                return NodeExecutionResult.failure(
                        RunStatus.CANCELED,
                        ErrorCode.RUN_CANCELED.getCode(),
                        canceledResult.errorMessage(),
                        elapsedMillis(startedAt)
                );
            }
            agentRunRepository.finishRun(
                    childRun.id(),
                    childResult.status(),
                    childResult.outputJson(),
                    childResult.errorCode(),
                    childResult.errorMessage(),
                    childResult.durationMs()
            );
            agentMessageRepository.insert(new AgentMessageRecord(
                    0L,
                    context.agentRunDbId(),
                    childRun.id(),
                    context.agentDefinition().id(),
                    targetAgent.id(),
                    input,
                    childResult.outputJson(),
                    childSummary(targetAgentKey, childRun.runNo(), childResult),
                    null
            ));
            JsonNode output = childResult.outputJson() == null ? objectMapper.nullNode() : childResult.outputJson();
            writeTrace(context, targetAgentKey, childRun.runNo(), childDepth, childResult, output);
            if (childResult.status() != RunStatus.SUCCESS) {
                return NodeExecutionResult.failure(
                        childResult.status(),
                        childResult.errorCode(),
                        childResult.errorMessage(),
                        childResult.errorDetails(),
                        elapsedMillis(startedAt)
                );
            }
            validateOutputSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), com.myagent.schema.validation.ValidationStage.NODE_OUTPUT);
            return NodeExecutionResult.success(output, elapsedMillis(startedAt));
        } catch (RuntimeException exception) {
            if (Thread.currentThread().isInterrupted()) {
                agentRunRepository.cancelActiveRun(
                        childRun.id(),
                        ErrorCode.RUN_CANCELED.getCode(),
                        "父节点被中断，级联取消子运行。",
                        elapsedMillis(startedAt)
                );
            }
            throw exception;
        } finally {
            activeChildRunRegistry.unregister(context.nodeRunDbId(), childRun.id());
        }
    }

    /**
     * 创建子运行记录。
     *
     * @param context 父节点执行上下文
     * @param targetAgent 目标 Agent
     * @param targetWorkflowVersion 目标工作流版本
     * @param input 子运行输入
     * @return 子运行记录
     */
    private AgentRunRecord createChildRun(
            NodeExecutionContext context,
            AgentRecord targetAgent,
            WorkflowVersionRecord targetWorkflowVersion,
            JsonNode input
    ) {
        AgentRunRecord childRun = agentRunRepository.insert(new AgentRunRecord(
                0L,
                runNoGenerator.nextRunNo(),
                targetAgent.id(),
                targetAgent.agentKey(),
                targetWorkflowVersion.id(),
                context.agentRunDbId(),
                RunType.AGENT_CALL,
                input == null ? objectMapper.createObjectNode() : input,
                null,
                RunStatus.PENDING,
                null,
                "",
                null,
                null,
                null
        ));
        agentRunRepository.markRunning(childRun.id());
        return childRun;
    }

    /**
     * 转换为目标工作流版本快照。
     *
     * @param agent Agent 主数据
     * @param workflowVersion 工作流版本
     * @return 版本快照
     */
    private WorkflowVersionSnapshot toSnapshot(AgentRecord agent, WorkflowVersionRecord workflowVersion) {
        return new WorkflowVersionSnapshot(
                workflowVersion.id(),
                agent.id(),
                agent.agentKey(),
                agent.name(),
                workflowVersion.versionNo(),
                workflowVersion.status(),
                workflowVersion.nodes(),
                workflowVersion.edges(),
                workflowVersion.runtimeOptions(),
                workflowVersion.referencedSchemaVersions()
        );
    }

    /**
     * 写入父运行上的 AGENT_CALL Trace。
     *
     * @param context 父节点执行上下文
     * @param targetAgentKey 目标 Agent 标识
     * @param childRunNo 子运行编号
     * @param childDepth 子调用深度
     * @param childResult 子运行结果
     */
    private void writeTrace(
            NodeExecutionContext context,
            String targetAgentKey,
            String childRunNo,
            int childDepth,
            WorkflowRuntimeResult childResult,
            JsonNode output
    ) {
        ObjectNode detail = objectMapper.createObjectNode()
                .put("targetAgentKey", targetAgentKey)
                .put("childRunId", childRunNo)
                .put("callDepth", childDepth)
                .put("status", childResult.status().name())
                .put("durationMs", childResult.durationMs());
        if (childResult.errorCode() != null) {
            detail.put("errorCode", childResult.errorCode());
        }
        if (childResult.errorMessage() != null) {
            detail.put("errorMessage", childResult.errorMessage());
        }
        detail.set("output", output);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.AGENT_CALL,
                childResult.status() == RunStatus.SUCCESS
                        ? "内部 Agent 调用成功：" + targetAgentKey
                        : "内部 Agent 调用失败：" + targetAgentKey,
                detail
        ));
    }

    /**
     * 计算当前调用深度。
     *
     * @param runDbId 当前运行主键
     * @return 当前运行已有的父级 AGENT_CALL 深度
     */
    private int currentCallDepth(long runDbId) {
        int depth = 0;
        Long cursor = runDbId;
        while (cursor != null) {
            AgentRunRecord run = agentRunRepository.findById(cursor).orElse(null);
            if (run == null || run.parentRunId() == null) {
                return depth;
            }
            depth++;
            cursor = run.parentRunId();
        }
        return depth;
    }

    /**
     * 构造 AgentMessage 摘要。
     *
     * @param targetAgentKey 目标 Agent 标识
     * @param childRunNo 子运行编号
     * @param childResult 子运行结果
     * @return 摘要
     */
    private String childSummary(String targetAgentKey, String childRunNo, WorkflowRuntimeResult childResult) {
        if (childResult.status() == RunStatus.SUCCESS) {
            return "调用 " + targetAgentKey + " 成功，子运行 " + childRunNo + "。";
        }
        return "调用 " + targetAgentKey + " 失败，子运行 " + childRunNo + "，原因：" + childResult.errorMessage();
    }

    /**
     * 读取必填文本配置。
     *
     * @param config 节点配置
     * @param fieldName 字段名
     * @param message 错误消息
     * @return 文本值
     */
    private String readRequiredConfigText(JsonNode config, String fieldName, String message) {
        if (config != null && config.hasNonNull(fieldName) && config.get(fieldName).isTextual()
                && !config.get(fieldName).asText().isBlank()) {
            return config.get(fieldName).asText();
        }
        throw new BizException(ErrorCode.TARGET_AGENT_NOT_PUBLISHED, message);
    }

    /**
     * 计算耗时。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
