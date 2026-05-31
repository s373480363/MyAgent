package com.myagent.run.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.eval.repository.EvalRunRepository;
import com.myagent.run.application.command.RunDebugAgentCommand;
import com.myagent.run.application.command.RunPublishedAgentCommand;
import com.myagent.run.application.query.GetRunDetailQuery;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.application.result.ChildRunResult;
import com.myagent.run.application.result.NodeRunResult;
import com.myagent.run.application.result.RunAgentResult;
import com.myagent.run.application.result.RunDetailResult;
import com.myagent.run.application.result.RunErrorResult;
import com.myagent.run.application.result.RunListItemResult;
import com.myagent.run.application.result.RunResult;
import com.myagent.run.application.result.RunWorkflowVersionResult;
import com.myagent.run.application.result.TraceEventResult;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.repository.AgentMessageRecord;
import com.myagent.run.repository.AgentMessageRepository;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.run.repository.RunTraceEventRecord;
import com.myagent.run.repository.TraceEventRepository;
import com.myagent.runtime.WorkflowRuntimeEngine;
import com.myagent.runtime.WorkflowRuntimeResult;
import com.myagent.runtime.WorkflowVersionSnapshot;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 默认运行应用服务。
 */
@Service
public class DefaultRunApplicationService implements RunApplicationService {

    /**
     * 运行编号时间格式。
     */
    private static final DateTimeFormatter RUN_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

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
     * NodeRun 仓储。
     */
    private final NodeRunRepository nodeRunRepository;

    /**
     * TraceEvent 仓储。
     */
    private final TraceEventRepository traceEventRepository;

    /**
     * AgentMessage 仓储。
     */
    private final AgentMessageRepository agentMessageRepository;

    /**
     * EvalRun 仓储。
     */
    private final EvalRunRepository evalRunRepository;

    /**
     * 工作流运行引擎。
     */
    private final WorkflowRuntimeEngine workflowRuntimeEngine;

    /**
     * 构造运行应用服务。
     *
     * @param objectMapper JSON 对象映射器
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param agentRunRepository AgentRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @param traceEventRepository TraceEvent 仓储
     * @param agentMessageRepository AgentMessage 仓储
     * @param evalRunRepository EvalRun 仓储
     * @param workflowRuntimeEngine 工作流运行引擎
     */
    public DefaultRunApplicationService(
            ObjectMapper objectMapper,
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            AgentRunRepository agentRunRepository,
            NodeRunRepository nodeRunRepository,
            TraceEventRepository traceEventRepository,
            AgentMessageRepository agentMessageRepository,
            EvalRunRepository evalRunRepository,
            WorkflowRuntimeEngine workflowRuntimeEngine
    ) {
        this.objectMapper = objectMapper;
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.agentRunRepository = agentRunRepository;
        this.nodeRunRepository = nodeRunRepository;
        this.traceEventRepository = traceEventRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.evalRunRepository = evalRunRepository;
        this.workflowRuntimeEngine = workflowRuntimeEngine;
    }

    /**
     * 运行当前发布版本 Agent。
     *
     * @param command 正式运行命令
     * @return 运行结果
     */
    @Override
    public RunResult runPublishedAgent(RunPublishedAgentCommand command) {
        AgentRecord agent = agentRepository.findByAgentKey(command.agentKey())
                .orElseThrow(() -> new BizException(ErrorCode.AGENT_NOT_FOUND, "指定 Agent 不存在。"));
        if (agent.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.AGENT_DISABLED, "Agent 已停用，不能运行。");
        }
        if (agent.currentPublishedWorkflowVersionId() == null) {
            throw new BizException(ErrorCode.TARGET_AGENT_NOT_PUBLISHED, "Agent 尚未发布工作流版本。");
        }
        WorkflowVersionRecord workflowVersion = workflowVersionRepository.findById(agent.currentPublishedWorkflowVersionId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "当前发布版本不存在。"));
        return execute(agent, workflowVersion, RunType.API, null, command.input());
    }

    /**
     * 调试运行 Agent。
     *
     * @param command 调试运行命令
     * @return 运行结果
     */
    @Override
    public RunResult runDebugAgent(RunDebugAgentCommand command) {
        AgentRecord agent = agentRepository.findById(command.agentId())
                .orElseThrow(() -> new BizException(ErrorCode.AGENT_NOT_FOUND, "指定 Agent 不存在。"));
        long workflowVersionId = command.workflowVersionId() == null
                ? requiredDraftWorkflowVersionId(agent)
                : command.workflowVersionId();
        WorkflowVersionRecord workflowVersion = workflowVersionRepository.findById(workflowVersionId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工作流版本不存在。"));
        if (workflowVersion.agentId() != agent.id()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工作流版本不属于当前 Agent。");
        }
        return execute(agent, workflowVersion, RunType.DEBUG, null, command.input());
    }

    /**
     * 查询运行列表。
     *
     * @param query 查询条件
     * @return 分页运行列表
     */
    @Override
    public PageResult<RunListItemResult> listRuns(ListRunsQuery query) {
        return agentRunRepository.listRuns(query).map(this::toListItemResult);
    }

    /**
     * 查询运行详情。
     *
     * @param query 查询条件
     * @return 运行详情
     */
    @Override
    public RunDetailResult getRunDetail(GetRunDetailQuery query) {
        AgentRunRecord run = agentRunRepository.findByRunNo(query.runId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定运行不存在。"));
        AgentRecord agent = agentRepository.findById(run.agentId())
                .orElseThrow(() -> new BizException(ErrorCode.AGENT_NOT_FOUND, "运行关联的 Agent 不存在。"));
        WorkflowVersionRecord workflowVersion = workflowVersionRepository.findById(run.workflowVersionId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "运行关联的工作流版本不存在。"));
        String parentRunNo = run.parentRunId() == null
                ? null
                : agentRunRepository.findById(run.parentRunId()).map(AgentRunRecord::runNo).orElse(null);
        return new RunDetailResult(
                run.runNo(),
                new RunAgentResult(agent.id(), agent.agentKey(), agent.name()),
                new RunWorkflowVersionResult(workflowVersion.id(), workflowVersion.versionNo(), workflowVersion.status()),
                run.runType(),
                run.status(),
                parentRunNo,
                run.runType() == RunType.EVAL
                        ? evalRunRepository.findByAgentRunId(run.id()).map(com.myagent.eval.repository.EvalRunRecord::runNo).orElse(null)
                        : null,
                run.inputJson(),
                run.outputJson(),
                toError(run),
                nodeRunRepository.listByRunId(run.id()).stream().map(this::toNodeRunResult).toList(),
                traceEventRepository.listByRunId(run.id()).stream().map(this::toTraceEventResult).toList(),
                agentMessageRepository.listByParentRunId(run.id()).stream().map(this::toChildRunResult).toList(),
                run.startedAt(),
                run.finishedAt(),
                run.durationMs()
        );
    }

    /**
     * 执行工作流。
     *
     * @param agent Agent 主数据
     * @param workflowVersion 工作流版本
     * @param runType 运行类型
     * @param parentRunId 父运行主键
     * @param input 输入 JSON
     * @return 运行结果
     */
    private RunResult execute(
            AgentRecord agent,
            WorkflowVersionRecord workflowVersion,
            RunType runType,
            Long parentRunId,
            JsonNode input
    ) {
        AgentRunRecord run = agentRunRepository.insert(new AgentRunRecord(
                0L,
                newRunNo("run"),
                agent.id(),
                agent.agentKey(),
                workflowVersion.id(),
                parentRunId,
                runType,
                input == null ? objectMapper.createObjectNode() : input,
                null,
                RunStatus.PENDING,
                "",
                null,
                null,
                null
        ));
        agentRunRepository.markRunning(run.id());
        WorkflowRuntimeResult runtimeResult = workflowRuntimeEngine.execute(
                run.id(),
                run.runNo(),
                agent,
                toSnapshot(agent, workflowVersion),
                input == null ? objectMapper.createObjectNode() : input
        );
        agentRunRepository.finishRun(
                run.id(),
                runtimeResult.status(),
                runtimeResult.outputJson(),
                runtimeResult.errorMessage(),
                runtimeResult.durationMs()
        );
        return new RunResult(
                run.runNo(),
                agent.agentKey(),
                workflowVersion.id(),
                runtimeResult.status(),
                runtimeResult.outputJson(),
                runtimeResult.errorCode() == null ? null : new RunErrorResult(runtimeResult.errorCode(), runtimeResult.errorMessage()),
                runtimeResult.durationMs()
        );
    }

    /**
     * 转换为工作流版本快照。
     *
     * @param agent Agent 主数据
     * @param workflowVersion 工作流版本
     * @return 工作流版本快照
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
     * 返回当前草稿版本主键。
     *
     * @param agent Agent 主数据
     * @return 草稿版本主键
     */
    private long requiredDraftWorkflowVersionId(AgentRecord agent) {
        if (agent.currentDraftWorkflowVersionId() == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "当前 Agent 暂无草稿版本。");
        }
        return agent.currentDraftWorkflowVersionId();
    }

    /**
     * 构造运行列表项。
     *
     * @param run 运行记录
     * @return 运行列表项
     */
    private RunListItemResult toListItemResult(AgentRunRecord run) {
        AgentRecord agent = agentRepository.findById(run.agentId()).orElse(null);
        return new RunListItemResult(
                run.runNo(),
                run.agentId(),
                run.agentKey(),
                agent == null ? "" : agent.name(),
                run.runType(),
                run.status(),
                run.workflowVersionId(),
                run.startedAt(),
                run.finishedAt(),
                run.durationMs()
        );
    }

    /**
     * 构造节点运行结果。
     *
     * @param nodeRun 节点运行记录
     * @return 节点运行结果
     */
    private NodeRunResult toNodeRunResult(NodeRunRecord nodeRun) {
        return new NodeRunResult(
                nodeRun.id(),
                nodeRun.nodeId(),
                nodeRun.nodeName(),
                nodeRun.nodeType(),
                nodeRun.inputJson(),
                nodeRun.outputJson(),
                nodeRun.schemaValidationResultJson(),
                nodeRun.status(),
                nodeRun.errorMessage(),
                nodeRun.startedAt(),
                nodeRun.finishedAt(),
                nodeRun.durationMs()
        );
    }

    /**
     * 构造 Trace 事件结果。
     *
     * @param traceEvent Trace 事件记录
     * @return Trace 事件结果
     */
    private TraceEventResult toTraceEventResult(RunTraceEventRecord traceEvent) {
        return new TraceEventResult(
                traceEvent.id(),
                traceEvent.nodeRunId(),
                traceEvent.evalRunId(),
                traceEvent.eventType(),
                traceEvent.summary(),
                traceEvent.detailJson(),
                traceEvent.eventTime()
        );
    }

    /**
     * 构造子运行结果。
     *
     * @param message Agent 消息记录
     * @return 子运行结果
     */
    private ChildRunResult toChildRunResult(AgentMessageRecord message) {
        String childRunNo = agentRunRepository.findById(message.childRunId()).map(AgentRunRecord::runNo).orElse(null);
        return new ChildRunResult(childRunNo, message.summary());
    }

    /**
     * 构造错误结果。
     *
     * @param run 运行记录
     * @return 错误结果
     */
    private RunErrorResult toError(AgentRunRecord run) {
        if (run.errorMessage() == null || run.errorMessage().isBlank()) {
            return null;
        }
        return new RunErrorResult(run.status().name(), run.errorMessage());
    }

    /**
     * 生成运行编号。
     *
     * @param prefix 编号前缀
     * @return 运行编号
     */
    private String newRunNo(String prefix) {
        return prefix + "_" + RUN_NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
