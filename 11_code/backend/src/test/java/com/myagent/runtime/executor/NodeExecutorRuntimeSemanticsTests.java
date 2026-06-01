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
import com.myagent.method.repository.JavaMethodRecord;
import com.myagent.method.runtime.JavaMethodDescriptor;
import com.myagent.method.runtime.JavaMethodInvoker;
import com.myagent.method.runtime.JavaMethodRegistry;
import com.myagent.model.ModelInvocationResult;
import com.myagent.model.OpenAiModelGateway;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.domain.RunNoGenerator;
import com.myagent.run.domain.TraceEventType;
import com.myagent.run.repository.AgentMessageRecord;
import com.myagent.run.repository.AgentMessageRepository;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.runtime.ActiveChildRunRegistry;
import com.myagent.runtime.DefaultMappingService;
import com.myagent.runtime.DefaultRuntimeLimitGuard;
import com.myagent.runtime.NodeExecutionCommand;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutionRunner;
import com.myagent.runtime.NodeExecutorRegistry;
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
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.schema.validation.SchemaReference;
import com.myagent.schema.validation.SchemaValidationError;
import com.myagent.schema.validation.SchemaValidationResult;
import com.myagent.schema.validation.SchemaValidationService;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.tool.repository.ToolRecord;
import com.myagent.tool.runtime.ToolDescriptor;
import com.myagent.tool.runtime.ToolExecutionRequest;
import com.myagent.tool.runtime.ToolExecutor;
import com.myagent.tool.runtime.ToolRegistry;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowEdgeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowSchemaRef;
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
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
     * LLM 类节点必须只读取正式提示词模板字段。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void llmUsesFormalPromptTemplates() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        LlmNodeExecutor executor = new LlmNodeExecutor(objectMapper, modelGateway);
        when(modelGateway.invoke(any())).thenReturn(new ModelInvocationResult(
                objectMapper.getNodeFactory().textNode("完成"),
                "完成",
                5
        ));

        NodeExecutionResult result = executor.execute(context(
                WorkflowNodeType.LLM,
                """
                        {
                          "model": "gpt-test",
                          "temperature": 0.5,
                          "systemPromptTemplate": "系统 {agentKey}",
                          "userPromptTemplate": "输入 {inputJson}"
                        }
                        """
        ));

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        verify(modelGateway).invoke(argThat(request ->
                "gpt-test".equals(request.model())
                        && request.systemPrompt().contains("source-agent")
                        && request.userPrompt().contains("\"question\":\"你好\"")
                        && request.temperature().compareTo(new BigDecimal("0.5")) == 0
        ));
    }

    /**
     * REVIEW 输出 Schema 失败时也必须先保留 MODEL_RESPONSE，再写 SCHEMA_VALIDATION。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void reviewWritesModelResponseBeforeSchemaValidationFailure() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        ReviewNodeExecutor executor = new ReviewNodeExecutor(objectMapper, modelGateway);
        when(modelGateway.invoke(any())).thenReturn(new ModelInvocationResult(
                objectMapper.readTree("{\"unexpected\":true}"),
                "{\"unexpected\":true}",
                5
        ));
        CollectingTraceWriter traceWriter = new CollectingTraceWriter();
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("review-node");
        node.setName("审核节点");
        node.setType(WorkflowNodeType.REVIEW);
        node.setConfig(objectMapper.readTree("""
                {
                  "userPromptTemplate": "审核 {inputJson}"
                }
                """));
        WorkflowSchemaRef outputSchemaRef = new WorkflowSchemaRef();
        outputSchemaRef.setSchemaKey("review-output");
        outputSchemaRef.setVersion(1);
        node.setOutputSchemaRef(outputSchemaRef);
        SchemaValidationService invalidSchemaService = (payload, schemaRef, stage) -> SchemaValidationResult.invalid(
                "review-output",
                1,
                List.of(new SchemaValidationError("$.summary", "required", "summary 字段缺失。"))
        );

        assertThatThrownBy(() -> executor.execute(new NodeExecutionContext(
                100L,
                "run-review",
                200L,
                agent(1L, "source-agent", 10L),
                10L,
                node,
                List.of(),
                new WorkflowContext(objectMapper, objectMapper.readTree("{\"question\":\"你好\"}")),
                new WorkflowRuntimeOptions(600, 30, 3),
                traceWriter,
                invalidSchemaService,
                new ArrayList<>(),
                new DefaultMappingService(objectMapper),
                new NoopRuntimeLimitGuard()
        ))).isInstanceOf(BizException.class);

        assertThat(traceWriter.events)
                .extracting(TraceEventRecord::eventType)
                .containsSubsequence(TraceEventType.MODEL_RESPONSE, TraceEventType.SCHEMA_VALIDATION);
    }

    /**
     * outputSchema 失败时 NodeRun 必须保留模型已产生输出，同时状态仍为失败。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void nodeRunnerPreservesProducedOutputWhenOutputSchemaFails() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        when(modelGateway.invoke(any())).thenReturn(new ModelInvocationResult(
                objectMapper.readTree("{\"unexpected\":true}"),
                "{\"unexpected\":true}",
                5
        ));
        LlmNodeExecutor executor = new LlmNodeExecutor(objectMapper, modelGateway);
        WorkflowNodeDefinition node = modelNode(WorkflowNodeType.LLM);
        node.setNodeId("llm-output-schema");
        node.setTimeoutSeconds(5);
        WorkflowSchemaRef outputSchemaRef = new WorkflowSchemaRef();
        outputSchemaRef.setSchemaKey("llm-output");
        outputSchemaRef.setVersion(1);
        node.setOutputSchemaRef(outputSchemaRef);
        SchemaValidationService invalidOutputSchemaService = (payload, schemaRef, stage) -> {
            if (stage == ValidationStage.NODE_OUTPUT) {
                return SchemaValidationResult.invalid(
                        "llm-output",
                        1,
                        List.of(new SchemaValidationError("$.summary", "required", "summary 字段缺失。"))
                );
            }
            return SchemaValidationResult.valid("llm-input", 1);
        };
        NodeExecutorRegistry registry = mock(NodeExecutorRegistry.class);
        when(registry.getExecutor(WorkflowNodeType.LLM)).thenReturn(executor);
        CollectingTraceWriter traceWriter = new CollectingTraceWriter();
        NodeExecutionRunner runner = new NodeExecutionRunner(
                objectMapper,
                registry,
                invalidOutputSchemaService,
                new DefaultMappingService(objectMapper),
                new DefaultRuntimeLimitGuard(),
                mock(PlatformSettingsResolver.class),
                new ActiveChildRunRegistry(mock(AgentRunRepository.class)),
                Executors.newCachedThreadPool()
        );

        NodeExecutionResult result = runner.execute(new NodeExecutionCommand(
                100L,
                "run-schema-failure",
                agent(1L, "source-agent", 10L),
                10L,
                new WorkflowRuntimeOptions(600, 30, 3),
                node,
                List.of(),
                new WorkflowContext(objectMapper, objectMapper.readTree("{\"question\":\"你好\"}")),
                null,
                traceWriter,
                Instant.now()
        ));

        assertThat(result.status()).isEqualTo(RunStatus.FAILED);
        assertThat(result.outputJson().path("unexpected").asBoolean()).isTrue();
        assertThat(result.schemaValidationResultJson().path("valid").asBoolean()).isFalse();
        assertThat(result.schemaValidationResultJson().path("results").get(0).path("stage").asText()).isEqualTo("NODE_OUTPUT");
        assertThat(traceWriter.finished)
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.status()).isEqualTo(RunStatus.FAILED);
                    assertThat(record.outputJson().path("unexpected").asBoolean()).isTrue();
                    assertThat(record.schemaValidationResultJson().path("valid").asBoolean()).isFalse();
                    assertThat(record.errorMessage()).contains("summary 字段缺失");
                });
        assertThat(traceWriter.events)
                .extracting(TraceEventRecord::eventType)
                .containsSubsequence(
                        TraceEventType.MODEL_RESPONSE,
                        TraceEventType.SCHEMA_VALIDATION,
                        TraceEventType.NODE_ERROR
                );
    }

    /**
     * REVIEW 输入 Schema 失败时不能调用模型。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void reviewValidatesInputSchemaBeforeModelInvocation() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        ReviewNodeExecutor executor = new ReviewNodeExecutor(objectMapper, modelGateway);
        WorkflowNodeDefinition node = modelNode(WorkflowNodeType.REVIEW);
        WorkflowSchemaRef inputSchemaRef = new WorkflowSchemaRef();
        inputSchemaRef.setSchemaKey("review-input");
        inputSchemaRef.setVersion(1);
        node.setInputSchemaRef(inputSchemaRef);
        SchemaValidationService invalidInputSchemaService = (payload, schemaRef, stage) -> SchemaValidationResult.invalid(
                "review-input",
                1,
                List.of(new SchemaValidationError("$.question", "required", "question 字段缺失。"))
        );

        assertThatThrownBy(() -> executor.execute(context(node, invalidInputSchemaService)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("节点 Schema 校验失败");
        verifyNoInteractions(modelGateway);
    }

    /**
     * SUMMARY 输入 Schema 失败时不能调用模型。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void summaryValidatesInputSchemaBeforeModelInvocation() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        SummaryNodeExecutor executor = new SummaryNodeExecutor(objectMapper, modelGateway);
        WorkflowNodeDefinition node = modelNode(WorkflowNodeType.SUMMARY);
        WorkflowSchemaRef inputSchemaRef = new WorkflowSchemaRef();
        inputSchemaRef.setSchemaKey("summary-input");
        inputSchemaRef.setVersion(1);
        node.setInputSchemaRef(inputSchemaRef);
        SchemaValidationService invalidInputSchemaService = (payload, schemaRef, stage) -> SchemaValidationResult.invalid(
                "summary-input",
                1,
                List.of(new SchemaValidationError("$.content", "required", "content 字段缺失。"))
        );

        assertThatThrownBy(() -> executor.execute(context(node, invalidInputSchemaService)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("节点 Schema 校验失败");
        verifyNoInteractions(modelGateway);
    }

    /**
     * JAVA_METHOD 节点必须通过注册目录和调用边界执行。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void javaMethodNodeUsesRegistryAndInvoker() throws Exception {
        JavaMethodRegistry registry = mock(JavaMethodRegistry.class);
        JavaMethodInvoker invoker = mock(JavaMethodInvoker.class);
        JavaMethodNodeExecutor executor = new JavaMethodNodeExecutor(objectMapper, registry, invoker);
        JavaMethodDescriptor descriptor = new JavaMethodDescriptor(
                javaMethodRecord("method.ok", EnableStatus.ENABLED),
                this,
                NodeExecutorRuntimeSemanticsTests.class.getDeclaredMethod("registeredMethod")
        );
        when(registry.getEnabledMethod("method.ok")).thenReturn(descriptor);
        when(invoker.invoke(eq(descriptor), any())).thenReturn(objectMapper.readTree("{\"ok\":true}"));

        NodeExecutionResult result = executor.execute(context(WorkflowNodeType.JAVA_METHOD, "{\"methodKey\":\"method.ok\"}"));

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.outputJson().get("ok").asBoolean()).isTrue();
        verify(registry).getEnabledMethod("method.ok");
        verify(invoker).invoke(eq(descriptor), any());
    }

    /**
     * TOOL 节点必须通过注册目录和工具执行器执行。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void toolNodeUsesRegistryAndExecutor() throws Exception {
        ToolRegistry registry = mock(ToolRegistry.class);
        ToolExecutor toolExecutor = mock(ToolExecutor.class);
        ToolNodeExecutor executor = new ToolNodeExecutor(objectMapper, registry);
        ToolDescriptor descriptor = new ToolDescriptor(toolRecord("tool.echo", "ECHO", EnableStatus.ENABLED), toolExecutor);
        when(registry.getEnabledTool("tool.echo")).thenReturn(descriptor);
        when(toolExecutor.execute(any(ToolExecutionRequest.class))).thenReturn(objectMapper.readTree("{\"echo\":true}"));

        NodeExecutionResult result = executor.execute(context(WorkflowNodeType.TOOL, "{\"toolKey\":\"tool.echo\"}"));

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.outputJson().get("echo").asBoolean()).isTrue();
        verify(registry).getEnabledTool("tool.echo");
        verify(toolExecutor).execute(any(ToolExecutionRequest.class));
    }

    /**
     * CONDITION 正常求值但未命中显式分支时才允许走默认分支。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void conditionUsesDefaultOnlyWhenEvaluationCompletesWithoutMatch() throws Exception {
        ConditionNodeExecutor executor = new ConditionNodeExecutor(objectMapper);
        NodeExecutionResult result = executor.execute(context(
                WorkflowNodeType.CONDITION,
                "{}",
                List.of(
                        edge("edge-high", false, "{\"left\":\"$.input.score\",\"operator\":\"GREATER_THAN\",\"right\":90,\"valueType\":\"NUMBER\"}"),
                        edge("edge-default", true, null)
                ),
                objectMapper.readTree("{\"score\":70}")
        ));

        assertThat(result.selectedEdgeId()).isEqualTo("edge-default");
    }

    /**
     * CONDITION 字段路径缺失时必须失败，不能用默认分支掩盖错误。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void conditionFailsFastWhenPathIsMissing() throws Exception {
        ConditionNodeExecutor executor = new ConditionNodeExecutor(objectMapper);

        assertThatThrownBy(() -> executor.execute(context(
                WorkflowNodeType.CONDITION,
                "{}",
                List.of(
                        edge("edge-missing", false, "{\"left\":\"$.input.missing\",\"operator\":\"EQUALS\",\"right\":\"x\",\"valueType\":\"STRING\"}"),
                        edge("edge-default", true, null)
                ),
                objectMapper.readTree("{\"score\":70}")
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("映射读取失败");
    }

    /**
     * CONDITION 应支持 IN 数组匹配。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void conditionSupportsInOperator() throws Exception {
        ConditionNodeExecutor executor = new ConditionNodeExecutor(objectMapper);

        NodeExecutionResult result = executor.execute(context(
                WorkflowNodeType.CONDITION,
                "{}",
                List.of(
                        edge("edge-hit", false, "{\"left\":\"$.input.status\",\"operator\":\"IN\",\"right\":[\"PAID\",\"DONE\"],\"valueType\":\"STRING\"}"),
                        edge("edge-default", true, null)
                ),
                objectMapper.readTree("{\"status\":\"DONE\"}")
        ));

        assertThat(result.selectedEdgeId()).isEqualTo("edge-hit");
    }

    /**
     * CONDITION 应支持 NOT_IN 数组排除。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void conditionSupportsNotInOperator() throws Exception {
        ConditionNodeExecutor executor = new ConditionNodeExecutor(objectMapper);

        NodeExecutionResult result = executor.execute(context(
                WorkflowNodeType.CONDITION,
                "{}",
                List.of(
                        edge("edge-hit", false, "{\"left\":\"$.input.status\",\"operator\":\"NOT_IN\",\"right\":[\"CANCELED\",\"FAILED\"],\"valueType\":\"STRING\"}"),
                        edge("edge-default", true, null)
                ),
                objectMapper.readTree("{\"status\":\"DONE\"}")
        ));

        assertThat(result.selectedEdgeId()).isEqualTo("edge-hit");
    }

    /**
     * CONDITION 应支持 NOT_CONTAINS 字符串排除。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void conditionSupportsNotContainsOperator() throws Exception {
        ConditionNodeExecutor executor = new ConditionNodeExecutor(objectMapper);

        NodeExecutionResult result = executor.execute(context(
                WorkflowNodeType.CONDITION,
                "{}",
                List.of(
                        edge("edge-hit", false, "{\"left\":\"$.input.summary\",\"operator\":\"NOT_CONTAINS\",\"right\":\"失败\",\"valueType\":\"STRING\"}"),
                        edge("edge-default", true, null)
                ),
                objectMapper.readTree("{\"summary\":\"处理成功\"}")
        ));

        assertThat(result.selectedEdgeId()).isEqualTo("edge-hit");
    }

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
                provider,
                new ActiveChildRunRegistry(agentRunRepository),
                new RunNoGenerator()
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
     * 父 AGENT_CALL 节点超时时，活跃子运行必须被级联取消。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void agentCallCancelsChildRunWhenParentNodeTimesOut() throws Exception {
        AgentRepository agentRepository = mock(AgentRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        InMemoryAgentRunRepository agentRunRepository = new InMemoryAgentRunRepository();
        AgentMessageRepository agentMessageRepository = new InMemoryAgentMessageRepository();
        WorkflowRuntimeEngine runtimeEngine = mock(WorkflowRuntimeEngine.class);
        ObjectProvider<WorkflowRuntimeEngine> provider = objectProvider(runtimeEngine);
        ActiveChildRunRegistry childRunRegistry = new ActiveChildRunRegistry(agentRunRepository);
        AgentCallNodeExecutor executor = new AgentCallNodeExecutor(
                objectMapper,
                agentRepository,
                workflowVersionRepository,
                agentRunRepository,
                agentMessageRepository,
                provider,
                childRunRegistry,
                new RunNoGenerator()
        );
        AgentRecord targetAgent = agent(2L, "target-agent", 20L);
        WorkflowVersionRecord targetVersion = workflowVersion(20L, targetAgent.id());
        when(agentRepository.findByAgentKey("target-agent")).thenReturn(Optional.of(targetAgent));
        when(workflowVersionRepository.findById(20L)).thenReturn(Optional.of(targetVersion));
        when(runtimeEngine.execute(any(Long.class), anyString(), any(), any(WorkflowVersionSnapshot.class), any()))
                .thenAnswer(invocation -> {
                    try {
                        Thread.sleep(2_000);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    return new WorkflowRuntimeResult(RunStatus.SUCCESS, objectMapper.createObjectNode(), null, null, null, 2_000);
                });
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("agent-call");
        node.setName("调用子 Agent");
        node.setType(WorkflowNodeType.AGENT_CALL);
        node.setTimeoutSeconds(1);
        node.setConfig(objectMapper.readTree("{\"targetAgentKey\":\"target-agent\"}"));
        NodeExecutorRegistry registry = mock(NodeExecutorRegistry.class);
        when(registry.getExecutor(WorkflowNodeType.AGENT_CALL)).thenReturn(executor);
        NodeExecutionRunner runner = nodeExecutionRunner(registry, childRunRegistry);

        NodeExecutionResult result = runner.execute(new NodeExecutionCommand(
                100L,
                "run-parent",
                agent(1L, "source-agent", 10L),
                10L,
                new WorkflowRuntimeOptions(600, 30, 3),
                node,
                List.of(),
                new WorkflowContext(objectMapper, objectMapper.readTree("{\"question\":\"你好\"}")),
                null,
                new CollectingTraceWriter(),
                Instant.now()
        ));

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(agentRunRepository.findByRunNo("run_00000000000000_00000001"))
                .get()
                .extracting(AgentRunRecord::status)
                .isEqualTo(RunStatus.CANCELED);
    }

    /**
     * Eval 已解析节点输入执行时不能再次套用节点 inputMapping。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void nodeRunnerUsesResolvedInputWithoutApplyingInputMappingAgain() throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("eval-node");
        node.setName("验收节点");
        node.setType(WorkflowNodeType.LLM);
        node.setTimeoutSeconds(5);
        node.setInputMapping(objectMapper.getNodeFactory().textNode("$.input.payload"));
        NodeExecutorRegistry registry = mock(NodeExecutorRegistry.class);
        when(registry.getExecutor(WorkflowNodeType.LLM)).thenReturn(context -> NodeExecutionResult.success(
                context.mappingService().extractInput(context.workflowContext().root(), context.nodeDefinition().getInputMapping()),
                1
        ));
        NodeExecutionRunner runner = nodeExecutionRunner(registry, new ActiveChildRunRegistry(mock(AgentRunRepository.class)));

        NodeExecutionResult result = runner.execute(new NodeExecutionCommand(
                100L,
                "run-eval",
                agent(1L, "source-agent", 10L),
                10L,
                new WorkflowRuntimeOptions(600, 30, 3),
                node,
                List.of(),
                new WorkflowContext(objectMapper, objectMapper.readTree("{\"input\":{\"payload\":\"wrong-root\"}}")),
                objectMapper.readTree("{\"payload\":\"from-eval-case\"}"),
                new CollectingTraceWriter(),
                Instant.now()
        ));

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.outputJson().get("payload").asText()).isEqualTo("from-eval-case");
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
        return context(nodeType, configJson, List.of(), objectMapper.readTree("{\"question\":\"你好\"}"));
    }

    /**
     * 构造模型类节点。
     *
     * @param nodeType 节点类型
     * @return 工作流节点定义
     * @throws Exception JSON 解析失败时抛出
     */
    private WorkflowNodeDefinition modelNode(WorkflowNodeType nodeType) throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("model-node");
        node.setName("模型节点");
        node.setType(nodeType);
        node.setConfig(objectMapper.readTree("{\"userPromptTemplate\":\"输入 {inputJson}\"}"));
        return node;
    }

    /**
     * 构造节点执行上下文。
     *
     * @param node 节点定义
     * @param schemaValidationService Schema 校验服务
     * @return 节点执行上下文
     * @throws Exception JSON 解析失败时抛出
     */
    private NodeExecutionContext context(
            WorkflowNodeDefinition node,
            SchemaValidationService schemaValidationService
    ) throws Exception {
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
                schemaValidationService,
                new ArrayList<>(),
                new DefaultMappingService(objectMapper),
                new NoopRuntimeLimitGuard()
        );
    }

    /**
     * 构造节点执行上下文。
     *
     * @param nodeType 节点类型
     * @param configJson 配置 JSON
     * @param outgoingEdges 出边列表
     * @param input 输入 JSON
     * @return 节点执行上下文
     * @throws Exception JSON 解析失败时抛出
     */
    private NodeExecutionContext context(
            WorkflowNodeType nodeType,
            String configJson,
            List<WorkflowEdgeDefinition> outgoingEdges,
            JsonNode input
    ) throws Exception {
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
                outgoingEdges,
                new WorkflowContext(objectMapper, input),
                new WorkflowRuntimeOptions(600, 30, 3),
                new CollectingTraceWriter(),
                validSchemaValidationService(),
                new ArrayList<>(),
                new DefaultMappingService(objectMapper),
                new NoopRuntimeLimitGuard()
        );
    }

    /**
     * 构造测试边。
     *
     * @param edgeId 边标识
     * @param isDefault 是否默认边
     * @param conditionJson 条件 JSON
     * @return 工作流边
     * @throws Exception JSON 解析失败时抛出
     */
    private WorkflowEdgeDefinition edge(String edgeId, boolean isDefault, String conditionJson) throws Exception {
        WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition();
        edge.setEdgeId(edgeId);
        edge.setSourceNodeId("node-1");
        edge.setTargetNodeId("next");
        edge.setType(isDefault ? WorkflowEdgeType.DEFAULT : WorkflowEdgeType.CONDITION);
        edge.setIsDefault(isDefault);
        edge.setCondition(conditionJson == null ? null : objectMapper.readTree(conditionJson));
        return edge;
    }

    /**
     * 测试用注册方法。
     *
     * @return 测试输出
     */
    private String registeredMethod() {
        return "ok";
    }

    /**
     * 构造 Java 方法记录。
     *
     * @param methodKey 方法标识
     * @param status 启停状态
     * @return Java 方法记录
     */
    private JavaMethodRecord javaMethodRecord(String methodKey, EnableStatus status) {
        return new JavaMethodRecord(
                1L,
                methodKey,
                "测试方法",
                "",
                "testBean",
                "registeredMethod",
                1L,
                2L,
                status,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造工具记录。
     *
     * @param toolKey 工具标识
     * @param executorType 执行器类型
     * @param status 启停状态
     * @return 工具记录
     */
    private ToolRecord toolRecord(String toolKey, String executorType, EnableStatus status) {
        return new ToolRecord(
                1L,
                toolKey,
                "测试工具",
                "",
                1L,
                2L,
                executorType,
                objectMapper.createObjectNode(),
                status,
                Instant.now(),
                Instant.now()
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
     * 构造节点执行协调器。
     *
     * @param registry 节点执行器注册表
     * @param childRunRegistry 活跃子运行登记表
     * @return 节点执行协调器
     */
    private NodeExecutionRunner nodeExecutionRunner(
            NodeExecutorRegistry registry,
            ActiveChildRunRegistry childRunRegistry
    ) {
        return new NodeExecutionRunner(
                objectMapper,
                registry,
                validSchemaValidationService(),
                new DefaultMappingService(objectMapper),
                new DefaultRuntimeLimitGuard(),
                mock(PlatformSettingsResolver.class),
                childRunRegistry,
                Executors.newCachedThreadPool()
        );
    }

    /**
     * 收集 Trace 的测试写入器。
     */
    private static final class CollectingTraceWriter implements TraceWriter {

        /**
         * Trace 事件列表。
         */
        private final List<TraceEventRecord> events = new ArrayList<>();

        /**
         * NodeRun 完成记录列表。
         */
        private final List<NodeRunFinishRecord> finished = new ArrayList<>();

        @Override
        public NodeRunStartResult createNodeRun(NodeRunStartRecord record) {
            return new NodeRunStartResult(200L, record.agentRunDbId(), record.agentRunNo(), record.nodeId(), Instant.now());
        }

        @Override
        public void finishNodeRun(NodeRunFinishRecord record) {
            finished.add(record);
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
                    record.errorCode(),
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
                        null,
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
        public int finishRun(long runId, RunStatus status, JsonNode outputJson, String errorCode, String errorMessage, long durationMs) {
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
                    errorCode,
                    errorMessage,
                    current.startedAt(),
                    Instant.now(),
                    durationMs
            ));
            return 1;
        }

        @Override
        public int cancelActiveRun(long runId, String errorCode, String errorMessage, long durationMs) {
            AgentRunRecord current = inserted.get();
            if (current == null || current.id() != runId
                    || (current.status() != RunStatus.PENDING && current.status() != RunStatus.RUNNING)) {
                return 0;
            }
            inserted.set(new AgentRunRecord(
                    current.id(),
                    current.runNo(),
                    current.agentId(),
                    current.agentKey(),
                    current.workflowVersionId(),
                    current.parentRunId(),
                    current.runType(),
                    current.inputJson(),
                    current.outputJson(),
                    RunStatus.CANCELED,
                    errorCode,
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
