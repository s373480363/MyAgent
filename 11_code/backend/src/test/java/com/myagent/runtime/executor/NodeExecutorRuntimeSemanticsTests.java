package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.page.PageResult;
import com.myagent.externalagent.application.ExternalAgentCommandJsonCodec;
import com.myagent.externalagent.application.ExternalAgentTestExecutor;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.repository.AgentMessageRecord;
import com.myagent.run.repository.AgentMessageRepository;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.runtime.DefaultMappingService;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeRunFinishRecord;
import com.myagent.runtime.NodeRunStartRecord;
import com.myagent.runtime.NodeRunStartResult;
import com.myagent.runtime.RunLimitContext;
import com.myagent.runtime.RuntimeLimitGuard;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.runtime.TraceWriter;
import com.myagent.runtime.WorkflowContext;
import com.myagent.runtime.WorkflowRuntimeEngine;
import com.myagent.runtime.WorkflowRuntimeResult;
import com.myagent.runtime.WorkflowVersionSnapshot;
import com.myagent.schema.validation.SchemaReference;
import com.myagent.schema.validation.SchemaValidationResult;
import com.myagent.schema.validation.SchemaValidationService;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 节点执行器运行语义测试。
 */
class NodeExecutorRuntimeSemanticsTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * EXTERNAL_AGENT 必须在真正调用前拦截未配置的敏感 header。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void externalAgentRejectsMissingSecretBeforeInvocation() throws Exception {
        ExternalAgentRepository repository = mock(ExternalAgentRepository.class);
        ExternalAgentTestExecutor testExecutor = mock(ExternalAgentTestExecutor.class);
        ExternalAgentNodeExecutor executor = new ExternalAgentNodeExecutor(
                objectMapper,
                repository,
                new ExternalAgentCommandJsonCodec(objectMapper),
                testExecutor
        );
        when(repository.findByAdapterKey("http-agent")).thenReturn(Optional.of(new ExternalAgentRecord(
                1L,
                "http-agent",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                objectMapper.readTree("""
                        {
                          "method": "POST",
                          "url": "http://127.0.0.1:18081/run",
                          "headers": {},
                          "bodyTemplate": {},
                          "resultSource": { "type": "HTTP_BODY_JSON" },
                          "secretHeaderNames": ["Authorization"],
                          "secretHeaderValues": {}
                        }
                        """),
                "",
                30,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                Instant.now(),
                Instant.now()
        )));

        assertThatThrownBy(() -> executor.execute(context(WorkflowNodeType.EXTERNAL_AGENT, "{\"adapterKey\":\"http-agent\"}")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("尚未配置密钥");
    }

    /**
     * 外部 Agent 未提取到结构化 outputJson 时必须退化为文本业务输出。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void externalAgentFallsBackToTextSummaryAsBusinessOutput() throws Exception {
        ExternalAgentRepository repository = mock(ExternalAgentRepository.class);
        ExternalAgentTestExecutor testExecutor = mock(ExternalAgentTestExecutor.class);
        ExternalAgentNodeExecutor executor = new ExternalAgentNodeExecutor(
                objectMapper,
                repository,
                new ExternalAgentCommandJsonCodec(objectMapper),
                testExecutor
        );
        when(repository.findByAdapterKey("cli-agent")).thenReturn(Optional.of(new ExternalAgentRecord(
                1L,
                "cli-agent",
                ExternalAgentType.CUSTOM_CLI,
                "CLI Agent",
                "",
                objectMapper.readTree("""
                        {
                          "command": "agent",
                          "arguments": ["run", "{prompt}"],
                          "resultSource": { "type": "STDOUT_JSON" },
                          "environment": {}
                        }
                        """),
                "",
                30,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                Instant.now(),
                Instant.now()
        )));
        when(testExecutor.execute(any(), anyString(), any())).thenReturn(new ExternalAgentTestResult(
                true,
                "SUCCESS",
                0,
                null,
                "纯文本摘要",
                "",
                null,
                "纯文本摘要",
                null,
                10
        ));

        NodeExecutionResult result = executor.execute(context(WorkflowNodeType.EXTERNAL_AGENT, "{\"adapterKey\":\"cli-agent\"}"));

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.outputJson().asText()).isEqualTo("纯文本摘要");
    }

    /**
     * AGENT_CALL 必须创建子 AgentRun 并写入 AgentMessage。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void agentCallCreatesChildRunAndMessage() throws Exception {
        AgentRepository agentRepository = mock(AgentRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        AgentRunRepository agentRunRepository = new InMemoryAgentRunRepository();
        AgentMessageRepository agentMessageRepository = new InMemoryAgentMessageRepository();
        WorkflowRuntimeEngine runtimeEngine = mock(WorkflowRuntimeEngine.class);
        ObjectProvider<WorkflowRuntimeEngine> provider = objectProvider(runtimeEngine);
        AgentCallNodeExecutor executor = new AgentCallNodeExecutor(
                objectMapper,
                agentRepository,
                workflowVersionRepository,
                agentRunRepository,
                agentMessageRepository,
                provider
        );
        AgentRecord targetAgent = agent(2L, "target-agent", 20L);
        WorkflowVersionRecord targetVersion = workflowVersion(20L, targetAgent.id());
        when(agentRepository.findByAgentKey("target-agent")).thenReturn(Optional.of(targetAgent));
        when(workflowVersionRepository.findById(20L)).thenReturn(Optional.of(targetVersion));
        when(runtimeEngine.execute(any(Long.class), anyString(), any(), any(WorkflowVersionSnapshot.class), any()))
                .thenReturn(new WorkflowRuntimeResult(
                        RunStatus.SUCCESS,
                        objectMapper.readTree("{\"answer\":\"完成\"}"),
                        null,
                        null,
                        12
                ));

        NodeExecutionResult result = executor.execute(context(WorkflowNodeType.AGENT_CALL, "{\"targetAgentKey\":\"target-agent\"}"));

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.outputJson().get("answer").asText()).isEqualTo("完成");
        assertThat(agentRunRepository.findByRunNo("run_00000000000000_00000001")).isPresent();
        assertThat(agentMessageRepository.listByParentRunId(100L)).hasSize(1);
        verify(runtimeEngine).execute(any(Long.class), anyString(), any(), any(WorkflowVersionSnapshot.class), any());
    }

    /**
     * 构造节点执行上下文。
     *
     * @param nodeType 节点类型
     * @param configJson 配置 JSON
     * @return 节点执行上下文
     * @throws Exception JSON 解析失败时抛出
     */
    private NodeExecutionContext context(WorkflowNodeType nodeType, String configJson) throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("node-1");
        node.setName("测试节点");
        node.setType(nodeType);
        node.setConfig(objectMapper.readTree(configJson));
        return new NodeExecutionContext(
                100L,
                "run-parent",
                200L,
                agent(1L, "source-agent", 10L),
                10L,
                node,
                List.of(),
                new WorkflowContext(objectMapper, objectMapper.readTree("{\"question\":\"你好\"}")),
                new WorkflowRuntimeOptions(600, 30, 3),
                new CollectingTraceWriter(),
                validSchemaValidationService(),
                new DefaultMappingService(objectMapper),
                new NoopRuntimeLimitGuard()
        );
    }

    /**
     * 构造 Agent 记录。
     *
     * @param id 主键
     * @param agentKey 业务标识
     * @param publishedVersionId 发布版本主键
     * @return Agent 记录
     */
    private AgentRecord agent(long id, String agentKey, Long publishedVersionId) {
        return new AgentRecord(
                id,
                agentKey,
                agentKey,
                "",
                EnableStatus.ENABLED,
                "",
                "gpt-4.1-mini",
                BigDecimal.ZERO,
                600,
                30,
                null,
                publishedVersionId,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造工作流版本记录。
     *
     * @param id 主键
     * @param agentId Agent 主键
     * @return 工作流版本记录
     */
    private WorkflowVersionRecord workflowVersion(long id, long agentId) {
        return new WorkflowVersionRecord(
                id,
                agentId,
                1,
                WorkflowVersionStatus.PUBLISHED,
                List.of(),
                List.of(),
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of(),
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 返回始终通过的 Schema 校验服务。
     *
     * @return Schema 校验服务
     */
    private SchemaValidationService validSchemaValidationService() {
        return new SchemaValidationService() {
            @Override
            public SchemaValidationResult validate(JsonNode payload, SchemaReference schemaRef, ValidationStage stage) {
                return SchemaValidationResult.valid("test", 1);
            }
        };
    }

    /**
     * 构造 ObjectProvider。
     *
     * @param runtimeEngine 运行引擎
     * @return ObjectProvider
     */
    private ObjectProvider<WorkflowRuntimeEngine> objectProvider(WorkflowRuntimeEngine runtimeEngine) {
        return new ObjectProvider<>() {
            @Override
            public WorkflowRuntimeEngine getObject(Object... args) {
                return runtimeEngine;
            }

            @Override
            public WorkflowRuntimeEngine getIfAvailable() {
                return runtimeEngine;
            }

            @Override
            public WorkflowRuntimeEngine getIfUnique() {
                return runtimeEngine;
            }

            @Override
            public WorkflowRuntimeEngine getObject() {
                return runtimeEngine;
            }
        };
    }

    /**
     * 收集 Trace 的测试写入器。
     */
    private static final class CollectingTraceWriter implements TraceWriter {

        /**
         * Trace 事件列表。
         */
        private final List<TraceEventRecord> events = new ArrayList<>();

        @Override
        public NodeRunStartResult createNodeRun(NodeRunStartRecord record) {
            return new NodeRunStartResult(200L, record.agentRunDbId(), record.agentRunNo(), record.nodeId(), Instant.now());
        }

        @Override
        public void finishNodeRun(NodeRunFinishRecord record) {
        }

        @Override
        public void writeEvent(TraceEventRecord record) {
            events.add(record);
        }
    }

    /**
     * 不做限制的运行守卫。
     */
    private static final class NoopRuntimeLimitGuard implements RuntimeLimitGuard {

        @Override
        public void checkRunTimeout(RunLimitContext context) {
        }

        @Override
        public void checkNodeTimeout(RunLimitContext context) {
        }

        @Override
        public void checkStepLimit(RunLimitContext context) {
        }

        @Override
        public void checkCallDepth(RunLimitContext context) {
        }
    }

    /**
     * 内存 AgentRun 仓储。
     */
    private static final class InMemoryAgentRunRepository implements AgentRunRepository {

        /**
         * 子运行记录引用。
         */
        private final AtomicReference<AgentRunRecord> inserted = new AtomicReference<>();

        @Override
        public AgentRunRecord insert(AgentRunRecord record) {
            AgentRunRecord insertedRecord = new AgentRunRecord(
                    101L,
                    "run_00000000000000_00000001",
                    record.agentId(),
                    record.agentKey(),
                    record.workflowVersionId(),
                    record.parentRunId(),
                    record.runType(),
                    record.inputJson(),
                    record.outputJson(),
                    record.status(),
                    record.errorMessage(),
                    Instant.now(),
                    null,
                    null
            );
            inserted.set(insertedRecord);
            return insertedRecord;
        }

        @Override
        public Optional<AgentRunRecord> findByRunNo(String runNo) {
            return Optional.ofNullable(inserted.get()).filter(record -> record.runNo().equals(runNo));
        }

        @Override
        public Optional<AgentRunRecord> findById(long runId) {
            if (runId == 100L) {
                return Optional.of(new AgentRunRecord(
                        100L,
                        "run-parent",
                        1L,
                        "source-agent",
                        10L,
                        null,
                        RunType.API,
                        null,
                        null,
                        RunStatus.RUNNING,
                        "",
                        Instant.now(),
                        null,
                        null
                ));
            }
            return Optional.ofNullable(inserted.get()).filter(record -> record.id() == runId);
        }

        @Override
        public PageResult<AgentRunRecord> listRuns(ListRunsQuery query) {
            return PageResult.of(List.of(), query.page(), query.pageSize(), 0);
        }

        @Override
        public int finishRun(long runId, RunStatus status, JsonNode outputJson, String errorMessage, long durationMs) {
            AgentRunRecord current = inserted.get();
            inserted.set(new AgentRunRecord(
                    current.id(),
                    current.runNo(),
                    current.agentId(),
                    current.agentKey(),
                    current.workflowVersionId(),
                    current.parentRunId(),
                    current.runType(),
                    current.inputJson(),
                    outputJson,
                    status,
                    errorMessage,
                    current.startedAt(),
                    Instant.now(),
                    durationMs
            ));
            return 1;
        }

        @Override
        public int markRunning(long runId) {
            return 1;
        }
    }

    /**
     * 内存 AgentMessage 仓储。
     */
    private static final class InMemoryAgentMessageRepository implements AgentMessageRepository {

        /**
         * 消息列表。
         */
        private final List<AgentMessageRecord> messages = new ArrayList<>();

        @Override
        public AgentMessageRecord insert(AgentMessageRecord record) {
            AgentMessageRecord inserted = new AgentMessageRecord(
                    messages.size() + 1L,
                    record.parentRunId(),
                    record.childRunId(),
                    record.sourceAgentId(),
                    record.targetAgentId(),
                    record.inputJson(),
                    record.outputJson(),
                    record.summary(),
                    Instant.now()
            );
            messages.add(inserted);
            return inserted;
        }

        @Override
        public List<AgentMessageRecord> listByParentRunId(long parentRunId) {
            return messages.stream().filter(message -> message.parentRunId() == parentRunId).toList();
        }
    }

}
