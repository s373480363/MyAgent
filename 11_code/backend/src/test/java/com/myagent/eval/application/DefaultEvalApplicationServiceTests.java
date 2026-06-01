package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.eval.application.command.CreateEvalCaseCommand;
import com.myagent.eval.application.command.CreateEvalCaseFromNodeRunCommand;
import com.myagent.eval.application.command.RunEvalSuiteCommand;
import com.myagent.eval.application.result.EvalCaseResult;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.domain.EvalSuiteStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.eval.repository.EvalCaseRepository;
import com.myagent.eval.repository.EvalCaseResultRepository;
import com.myagent.eval.repository.EvalRunRepository;
import com.myagent.eval.repository.EvalSuiteRecord;
import com.myagent.eval.repository.EvalSuiteRepository;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.runtime.NodeExecutionRunner;
import com.myagent.runtime.TraceWriter;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowSchemaRef;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 节点验收应用服务测试。
 */
class DefaultEvalApplicationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 正常 NodeRun 可以生成 AI_DRAFT_PENDING 验收用例。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createCaseFromNodeRunCreatesPendingDraftCaseForConsistentSource() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        AgentRunRepository runRepository = mock(AgentRunRepository.class);
        NodeRunRepository nodeRunRepository = mock(NodeRunRepository.class);
        DefaultEvalApplicationService service = service(suiteRepository(suite(1L, 10L, "llm")), caseRepository, runRepository, nodeRunRepository);
        AgentRunRecord sourceRun = agentRun(100L, 1L, 10L);
        NodeRunRecord nodeRun = nodeRun(200L, 100L, "llm", RunStatus.SUCCESS, OBJECT_MAPPER.readTree("""
                {
                  "summary": "ok"
                }
                """));
        when(runRepository.findById(100L)).thenReturn(Optional.of(sourceRun));
        when(nodeRunRepository.findById(200L)).thenReturn(Optional.of(nodeRun));
        when(caseRepository.insert(any(EvalCaseRecord.class))).thenAnswer(invocation -> withCaseId(invocation.getArgument(0)));

        EvalCaseResult result = service.createCaseFromNodeRun(new CreateEvalCaseFromNodeRunCommand(
                200L,
                300L,
                "来源用例",
                "从运行生成"
        ));

        assertThat(result.confirmStatus()).isEqualTo(EvalCaseConfirmStatus.AI_DRAFT_PENDING);
        assertThat(result.input().path("question").asText()).isEqualTo("hello");
        assertThat(result.referenceAnswer().path("summary").asText()).isEqualTo("ok");
        assertThat(result.sourceRunId()).isEqualTo("run-100");
        assertThat(result.sourceNodeRunId()).isEqualTo(200L);
        assertThat(result.sourceWorkflowVersionId()).isEqualTo(10L);
        assertThat(result.sourceNodeId()).isEqualTo("llm");
    }

    /**
     * NodeRun 所属 Agent 与 EvalSuite 不一致时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createCaseFromNodeRunRejectsAgentMismatch() throws Exception {
        assertCreateCaseFromNodeRunRejected(
                suite(1L, 10L, "llm"),
                agentRun(100L, 2L, 10L),
                nodeRun(200L, 100L, "llm", RunStatus.SUCCESS, output()),
                "Agent 不一致"
        );
    }

    /**
     * NodeRun 所属工作流版本与 EvalSuite 不一致时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createCaseFromNodeRunRejectsWorkflowVersionMismatch() throws Exception {
        assertCreateCaseFromNodeRunRejected(
                suite(1L, 10L, "llm"),
                agentRun(100L, 1L, 11L),
                nodeRun(200L, 100L, "llm", RunStatus.SUCCESS, output()),
                "工作流版本不一致"
        );
    }

    /**
     * NodeRun 所属节点与 EvalSuite 目标节点不一致时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createCaseFromNodeRunRejectsNodeIdMismatch() throws Exception {
        assertCreateCaseFromNodeRunRejected(
                suite(1L, 10L, "llm"),
                agentRun(100L, 1L, 10L),
                nodeRun(200L, 100L, "review", RunStatus.SUCCESS, output()),
                "目标节点不一致"
        );
    }

    /**
     * NodeRun 非 SUCCESS 时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createCaseFromNodeRunRejectsNonSuccessNodeRun() throws Exception {
        assertCreateCaseFromNodeRunRejected(
                suite(1L, 10L, "llm"),
                agentRun(100L, 1L, 10L),
                nodeRun(200L, 100L, "llm", RunStatus.FAILED, output()),
                "SUCCESS 状态"
        );
    }

    /**
     * NodeRun 输出为空时必须失败。
     */
    @Test
    void createCaseFromNodeRunRejectsEmptyOutput() {
        assertCreateCaseFromNodeRunRejected(
                suite(1L, 10L, "llm"),
                agentRun(100L, 1L, 10L),
                nodeRun(200L, 100L, "llm", RunStatus.SUCCESS, null),
                "输出不能为空"
        );
    }

    /**
     * 正式 EvalRun 不允许包含未确认用例。
     */
    @Test
    void runSuiteRejectsIncludeUnconfirmed() {
        DefaultEvalApplicationService service = service(
                suiteRepository(new EvalSuiteRecord(
                        300L,
                        1L,
                        10L,
                        "llm",
                        "套件",
                        "",
                        BigDecimal.ZERO,
                        EvalSuiteStatus.CONFIRMED,
                        Instant.now(),
                        Instant.now()
                )),
                mock(EvalCaseRepository.class),
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class)
        );

        assertThatThrownBy(() -> service.runSuite(new RunEvalSuiteCommand(300L, List.of(), true)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许包含未确认用例");
    }

    /**
     * 用户创建正式 EvalCase 时必须配置非空断言数组。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createCaseRejectsEmptyAssertions() throws Exception {
        DefaultEvalApplicationService service = service(
                suiteRepository(suite(1L, 10L, "llm")),
                mock(EvalCaseRepository.class),
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class)
        );

        assertThatThrownBy(() -> service.createCase(new CreateEvalCaseCommand(
                300L,
                "CASE-001",
                "空断言用例",
                OBJECT_MAPPER.readTree("{\"question\":\"hello\"}"),
                OBJECT_MAPPER.readTree("{\"summary\":\"ok\"}"),
                OBJECT_MAPPER.createArrayNode(),
                OBJECT_MAPPER.createObjectNode(),
                false,
                ""
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("断言不能为空");
    }

    /**
     * AI 草稿用例没有断言时不能被确认。
     */
    @Test
    void confirmCaseRejectsEmptyAssertions() {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        when(caseRepository.findById(400L)).thenReturn(Optional.of(evalCase(
                EvalCaseConfirmStatus.AI_DRAFT_PENDING,
                OBJECT_MAPPER.createArrayNode()
        )));
        DefaultEvalApplicationService service = service(
                suiteRepository(suite(1L, 10L, "llm")),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class)
        );

        assertThatThrownBy(() -> service.confirmCase(300L, 400L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("断言不能为空");
    }

    /**
     * SCHEMA_VALIDATION 断言要求目标节点配置 outputSchema。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void confirmCaseRejectsSchemaValidationAssertionWithoutOutputSchema() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        when(caseRepository.findById(400L)).thenReturn(Optional.of(evalCase(
                EvalCaseConfirmStatus.AI_DRAFT_PENDING,
                OBJECT_MAPPER.readTree("[{\"type\":\"SCHEMA_VALIDATION\"}]")
        )));
        when(workflowVersionRepository.findById(10L)).thenReturn(Optional.of(workflowVersion(false)));
        DefaultEvalApplicationService service = service(
                suiteRepository(suite(1L, 10L, "llm")),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class),
                mock(AgentRepository.class),
                workflowVersionRepository,
                mock(EvalRunLifecycleService.class)
        );

        assertThatThrownBy(() -> service.confirmCase(300L, 400L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("要求目标节点配置 outputSchema");
    }

    /**
     * 确认套件前必须校验所有正式用例具备确定性断言。
     */
    @Test
    void confirmSuiteRejectsFormalCaseWithEmptyAssertions() {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        when(caseRepository.listRunnableCases(300L, null)).thenReturn(List.of(evalCase(
                EvalCaseConfirmStatus.USER_CREATED,
                OBJECT_MAPPER.createArrayNode()
        )));
        DefaultEvalApplicationService service = service(
                suiteRepository(suite(1L, 10L, "llm")),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class)
        );

        assertThatThrownBy(() -> service.confirmSuite(300L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("断言不能为空");
    }

    /**
     * 正式 EvalRun 执行前必须再次防御性拒绝非法断言类型，且不能创建运行记录。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void runSuiteRejectsUnsupportedAssertionTypeBeforeCreatingRun() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        when(caseRepository.listRunnableCases(300L, List.of())).thenReturn(List.of(evalCase(
                EvalCaseConfirmStatus.USER_CONFIRMED,
                OBJECT_MAPPER.readTree("[{\"type\":\"UNSUPPORTED_ASSERTION\"}]")
        )));
        AgentRepository agentRepository = mock(AgentRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        EvalRunLifecycleService lifecycleService = mock(EvalRunLifecycleService.class);
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent()));
        when(workflowVersionRepository.findById(10L)).thenReturn(Optional.of(workflowVersion()));
        DefaultEvalApplicationService service = service(
                suiteRepository(new EvalSuiteRecord(
                        300L,
                        1L,
                        10L,
                        "llm",
                        "套件",
                        "",
                        BigDecimal.ZERO,
                        EvalSuiteStatus.CONFIRMED,
                        Instant.now(),
                        Instant.now()
                )),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class),
                agentRepository,
                workflowVersionRepository,
                lifecycleService
        );

        assertThatThrownBy(() -> service.runSuite(new RunEvalSuiteCommand(300L, List.of(), false)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持断言类型");
        verifyNoInteractions(lifecycleService);
    }

    /**
     * 断言从 NodeRun 生成 EvalCase 会失败。
     *
     * @param suite EvalSuite
     * @param sourceRun 来源运行
     * @param nodeRun 来源节点运行
     * @param message 错误消息片段
     */
    private void assertCreateCaseFromNodeRunRejected(
            EvalSuiteRecord suite,
            AgentRunRecord sourceRun,
            NodeRunRecord nodeRun,
            String message
    ) {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        AgentRunRepository runRepository = mock(AgentRunRepository.class);
        NodeRunRepository nodeRunRepository = mock(NodeRunRepository.class);
        DefaultEvalApplicationService service = service(suiteRepository(suite), caseRepository, runRepository, nodeRunRepository);
        when(runRepository.findById(sourceRun.id())).thenReturn(Optional.of(sourceRun));
        when(nodeRunRepository.findById(nodeRun.id())).thenReturn(Optional.of(nodeRun));

        assertThatThrownBy(() -> service.createCaseFromNodeRun(new CreateEvalCaseFromNodeRunCommand(
                nodeRun.id(),
                suite.id(),
                "来源用例",
                ""
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining(message);
    }

    /**
     * 构造应用服务。
     *
     * @param suiteRepository EvalSuite 仓储
     * @param caseRepository EvalCase 仓储
     * @param runRepository AgentRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @return 应用服务
     */
    private DefaultEvalApplicationService service(
            EvalSuiteRepository suiteRepository,
            EvalCaseRepository caseRepository,
            AgentRunRepository runRepository,
            NodeRunRepository nodeRunRepository
    ) {
        return service(
                suiteRepository,
                caseRepository,
                runRepository,
                nodeRunRepository,
                mock(AgentRepository.class),
                mock(WorkflowVersionRepository.class),
                mock(EvalRunLifecycleService.class)
        );
    }

    /**
     * 构造应用服务。
     *
     * @param suiteRepository EvalSuite 仓储
     * @param caseRepository EvalCase 仓储
     * @param runRepository AgentRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param lifecycleService EvalRun 生命周期服务
     * @return 应用服务
     */
    private DefaultEvalApplicationService service(
            EvalSuiteRepository suiteRepository,
            EvalCaseRepository caseRepository,
            AgentRunRepository runRepository,
            NodeRunRepository nodeRunRepository,
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            EvalRunLifecycleService lifecycleService
    ) {
        return new DefaultEvalApplicationService(
                OBJECT_MAPPER,
                suiteRepository,
                caseRepository,
                mock(EvalRunRepository.class),
                mock(EvalCaseResultRepository.class),
                agentRepository,
                workflowVersionRepository,
                runRepository,
                nodeRunRepository,
                mock(TraceWriter.class),
                mock(NodeExecutionRunner.class),
                mock(EvalAssertionEvaluator.class),
                mock(EvalScoreEvaluator.class),
                lifecycleService
        );
    }

    /**
     * 构造 EvalSuite 仓储。
     *
     * @param suite EvalSuite
     * @return EvalSuite 仓储
     */
    private EvalSuiteRepository suiteRepository(EvalSuiteRecord suite) {
        EvalSuiteRepository repository = mock(EvalSuiteRepository.class);
        when(repository.findById(suite.id())).thenReturn(Optional.of(suite));
        return repository;
    }

    /**
     * 构造 EvalSuite。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @param nodeId 节点标识
     * @return EvalSuite
     */
    private EvalSuiteRecord suite(long agentId, long workflowVersionId, String nodeId) {
        return new EvalSuiteRecord(
                300L,
                agentId,
                workflowVersionId,
                nodeId,
                "套件",
                "",
                BigDecimal.ZERO,
                EvalSuiteStatus.DRAFT,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造 AgentRun。
     *
     * @param runId 运行主键
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return AgentRun
     */
    private AgentRunRecord agentRun(long runId, long agentId, long workflowVersionId) {
        return new AgentRunRecord(
                runId,
                "run-" + runId,
                agentId,
                "agent",
                workflowVersionId,
                null,
                RunType.API,
                OBJECT_MAPPER.createObjectNode(),
                OBJECT_MAPPER.createObjectNode(),
                RunStatus.SUCCESS,
                "",
                "",
                Instant.now(),
                Instant.now(),
                10L
        );
    }

    /**
     * 构造 NodeRun。
     *
     * @param nodeRunId NodeRun 主键
     * @param runId AgentRun 主键
     * @param nodeId 节点标识
     * @param status 节点状态
     * @param outputJson 节点输出
     * @return NodeRun
     */
    private NodeRunRecord nodeRun(long nodeRunId, long runId, String nodeId, RunStatus status, JsonNode outputJson) {
        return new NodeRunRecord(
                nodeRunId,
                runId,
                nodeId,
                "节点",
                "LLM",
                OBJECT_MAPPER.createObjectNode().put("question", "hello"),
                outputJson,
                OBJECT_MAPPER.createObjectNode(),
                status,
                "",
                Instant.now(),
                Instant.now(),
                10L
        );
    }

    /**
     * 构造输出。
     *
     * @return 输出 JSON
     * @throws Exception JSON 构造失败时抛出
     */
    private JsonNode output() throws Exception {
        return OBJECT_MAPPER.readTree("""
                {
                  "summary": "ok"
                }
                """);
    }

    /**
     * 设置用例主键。
     *
     * @param record 原始用例
     * @return 带主键用例
     */
    private EvalCaseRecord withCaseId(EvalCaseRecord record) {
        return new EvalCaseRecord(
                400L,
                record.suiteId(),
                record.caseNo(),
                record.title(),
                record.inputJson(),
                record.referenceAnswerJson(),
                record.assertionsJson(),
                record.scoreRuleJson(),
                record.critical(),
                record.confirmStatus(),
                record.sourceAgentRunId(),
                record.sourceNodeRunId(),
                record.sourceWorkflowVersionId(),
                record.sourceNodeId(),
                record.description(),
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造 EvalCase。
     *
     * @param confirmStatus 确认状态
     * @param assertionsJson 断言配置
     * @return EvalCase
     */
    private EvalCaseRecord evalCase(EvalCaseConfirmStatus confirmStatus, JsonNode assertionsJson) {
        return new EvalCaseRecord(
                400L,
                300L,
                "CASE-001",
                "测试用例",
                OBJECT_MAPPER.createObjectNode().put("question", "hello"),
                OBJECT_MAPPER.createObjectNode().put("summary", "ok"),
                assertionsJson,
                OBJECT_MAPPER.createObjectNode(),
                false,
                confirmStatus,
                null,
                null,
                null,
                null,
                "",
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造 Agent。
     *
     * @return Agent
     */
    private AgentRecord agent() {
        return new AgentRecord(
                1L,
                "agent",
                "Agent",
                "",
                EnableStatus.ENABLED,
                "",
                "gpt-test",
                BigDecimal.ZERO,
                600,
                30,
                null,
                10L,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造工作流版本。
     *
     * @return 工作流版本
     * @throws Exception JSON 构造失败时抛出
     */
    private WorkflowVersionRecord workflowVersion() throws Exception {
        return workflowVersion(false);
    }

    /**
     * 构造工作流版本。
     *
     * @param withOutputSchema 目标节点是否配置输出 Schema
     * @return 工作流版本
     * @throws Exception JSON 构造失败时抛出
     */
    private WorkflowVersionRecord workflowVersion(boolean withOutputSchema) throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("llm");
        node.setName("LLM 节点");
        node.setType(WorkflowNodeType.LLM);
        node.setConfig(OBJECT_MAPPER.readTree("{\"userPromptTemplate\":\"输入 {inputJson}\"}"));
        if (withOutputSchema) {
            WorkflowSchemaRef outputSchemaRef = new WorkflowSchemaRef();
            outputSchemaRef.setSchemaKey("summary-output");
            outputSchemaRef.setVersion(1);
            node.setOutputSchemaRef(outputSchemaRef);
        }
        return new WorkflowVersionRecord(
                10L,
                1L,
                1,
                WorkflowVersionStatus.PUBLISHED,
                List.of(node),
                List.of(),
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of(),
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }
}
