package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.eval.application.command.CreateEvalCaseFromNodeRunCommand;
import com.myagent.eval.application.command.RunEvalSuiteCommand;
import com.myagent.eval.application.result.EvalCaseResult;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.domain.EvalSuiteStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.eval.repository.EvalCaseRepository;
import com.myagent.eval.repository.EvalCaseResultRecord;
import com.myagent.eval.repository.EvalCaseResultRepository;
import com.myagent.eval.repository.EvalRunRecord;
import com.myagent.eval.repository.EvalRunRepository;
import com.myagent.eval.repository.EvalSuiteRecord;
import com.myagent.eval.repository.EvalSuiteRepository;
import com.myagent.modelcatalog.application.ModelRouteResolver;
import com.myagent.run.domain.RunNoGenerator;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.runtime.NodeExecutionResult;
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
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 节点验收应用服务测试。
 */
class DefaultEvalApplicationServiceTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void createCaseFromNodeRunCreatesPendingDraftCaseForConsistentSource() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        AgentRunRepository runRepository = mock(AgentRunRepository.class);
        NodeRunRepository nodeRunRepository = mock(NodeRunRepository.class);
        DefaultEvalCaseApplicationService service = caseService(
                suiteRepository(suite(1L, 10L, "llm", EvalSuiteStatus.DRAFT)),
                caseRepository,
                runRepository,
                nodeRunRepository
        );
        AgentRunRecord sourceRun = agentRun(100L, 1L, 10L);
        NodeRunRecord nodeRun = nodeRun(200L, 100L, "llm", RunStatus.SUCCESS, output());
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
        assertThat(result.referenceSample().path("summary").asText()).isEqualTo("ok");
        assertThat(result.judgeRule()).isBlank();
        assertThat(result.hardChecks().isArray()).isTrue();
        assertThat(result.sourceRunId()).isEqualTo("run-100");
        assertThat(result.sourceNodeRunId()).isEqualTo(200L);
        assertThat(result.sourceWorkflowVersionId()).isEqualTo(10L);
        assertThat(result.sourceNodeId()).isEqualTo("llm");
    }

    @Test
    void confirmCaseRejectsEmptyJudgeRule() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        when(caseRepository.findById(400L)).thenReturn(Optional.of(evalCase(
                EvalCaseConfirmStatus.AI_DRAFT_PENDING,
                "",
                OBJECT_MAPPER.createArrayNode()
        )));
        when(workflowVersionRepository.findById(10L)).thenReturn(Optional.of(workflowVersion()));
        DefaultEvalCaseApplicationService service = caseService(
                suiteRepository(suite(1L, 10L, "llm", EvalSuiteStatus.DRAFT)),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class),
                workflowVersionRepository
        );

        assertThatThrownBy(() -> service.confirmCase(300L, 400L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("judgeRule");
    }

    @Test
    void confirmCaseRejectsSchemaValidationHardCheckWithoutOutputSchema() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        when(caseRepository.findById(400L)).thenReturn(Optional.of(evalCase(
                EvalCaseConfirmStatus.AI_DRAFT_PENDING,
                "输出必须满足 schema。",
                OBJECT_MAPPER.readTree("[{\"type\":\"SCHEMA_VALIDATION\"}]")
        )));
        when(workflowVersionRepository.findById(10L)).thenReturn(Optional.of(workflowVersion(false)));
        DefaultEvalCaseApplicationService service = caseService(
                suiteRepository(suite(1L, 10L, "llm", EvalSuiteStatus.DRAFT)),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class),
                workflowVersionRepository
        );

        assertThatThrownBy(() -> service.confirmCase(300L, 400L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("outputSchema");
    }

    @Test
    void runSuiteRejectsUnsupportedHardCheckTypeBeforeCreatingRun() throws Exception {
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        when(caseRepository.listRunnableCases(300L, List.of())).thenReturn(List.of(evalCase(
                EvalCaseConfirmStatus.USER_CONFIRMED,
                "必须命中规则。",
                OBJECT_MAPPER.readTree("[{\"type\":\"UNSUPPORTED_HARD_CHECK\"}]")
        )));
        AgentRepository agentRepository = mock(AgentRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        EvalRunLifecycleService lifecycleService = mock(EvalRunLifecycleService.class);
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent()));
        when(workflowVersionRepository.findById(10L)).thenReturn(Optional.of(workflowVersion()));
        DefaultEvalApplicationService service = service(
                suiteRepository(suite(1L, 10L, "llm", EvalSuiteStatus.CONFIRMED)),
                caseRepository,
                mock(AgentRunRepository.class),
                mock(NodeRunRepository.class),
                agentRepository,
                workflowVersionRepository,
                lifecycleService
        );

        assertThatThrownBy(() -> service.runSuite(new RunEvalSuiteCommand(300L, List.of())))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("UNSUPPORTED_HARD_CHECK");
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void runSuiteSkipsJudgeWhenHardChecksFail() throws Exception {
        EvalSuiteRepository suiteRepository = suiteRepository(suite(1L, 10L, "llm", EvalSuiteStatus.CONFIRMED));
        EvalCaseRepository caseRepository = mock(EvalCaseRepository.class);
        EvalRunRepository evalRunRepository = mock(EvalRunRepository.class);
        EvalCaseResultRepository evalCaseResultRepository = mock(EvalCaseResultRepository.class);
        AgentRunRepository agentRunRepository = mock(AgentRunRepository.class);
        AgentRepository agentRepository = mock(AgentRepository.class);
        WorkflowVersionRepository workflowVersionRepository = mock(WorkflowVersionRepository.class);
        TraceWriter traceWriter = mock(TraceWriter.class);
        NodeExecutionRunner nodeExecutionRunner = mock(NodeExecutionRunner.class);
        EvalHardCheckEvaluator hardCheckEvaluator = mock(EvalHardCheckEvaluator.class);
        EvalJudgeEvaluator judgeEvaluator = mock(EvalJudgeEvaluator.class);
        EvalRunLifecycleService lifecycleService = mock(EvalRunLifecycleService.class);

        EvalCaseRecord evalCase = evalCase(
                EvalCaseConfirmStatus.USER_CONFIRMED,
                "judge rule",
                OBJECT_MAPPER.readTree("[{\"type\":\"JSON_PATH_EXISTS\",\"path\":\"$.summary\"}]")
        );
        JsonNode hardCheckResults = OBJECT_MAPPER.readTree("""
                [
                  {
                    "type": "JSON_PATH_EXISTS",
                    "passed": false,
                    "message": "$.summary missing",
                    "path": "$.summary",
                    "expected": "field exists",
                    "actual": null,
                    "details": {}
                  }
                ]
                """);

        when(caseRepository.listRunnableCases(300L, List.of())).thenReturn(List.of(evalCase));
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent()));
        when(workflowVersionRepository.findById(10L)).thenReturn(Optional.of(workflowVersion()));
        when(evalCaseResultRepository.countCriticalFailures(600L)).thenReturn(0L);
        when(nodeExecutionRunner.execute(any())).thenReturn(new NodeExecutionResult(
                RunStatus.SUCCESS,
                900L,
                output(),
                OBJECT_MAPPER.createObjectNode(),
                null,
                null,
                "",
                List.of(),
                15L
        ));
        when(hardCheckEvaluator.evaluate(any(), any(), any())).thenReturn(new EvalHardCheckEvaluation(
                false,
                hardCheckResults,
                "$.summary missing"
        ));

        AgentRunRecord agentRun = agentRun(500L, 1L, 10L);
        EvalRunRecord runningEvalRun = new EvalRunRecord(
                600L,
                "eval-run-600",
                300L,
                1L,
                10L,
                "llm",
                agentRun.id(),
                RunStatus.RUNNING,
                0,
                0,
                0,
                BigDecimal.ZERO,
                "",
                "",
                Instant.now(),
                null,
                null
        );
        EvalRunRecord finishedEvalRun = new EvalRunRecord(
                600L,
                "eval-run-600",
                300L,
                1L,
                10L,
                "llm",
                agentRun.id(),
                RunStatus.FAILED,
                1,
                0,
                1,
                BigDecimal.ZERO,
                "failed",
                "\u9a8c\u6536\u5224\u5b9a\u672a\u5168\u90e8\u901a\u8fc7\u3002",
                Instant.now(),
                Instant.now(),
                15L
        );
        when(lifecycleService.createEvalAgentRun(anyString(), any(), any(), any(), any())).thenReturn(agentRun);
        when(lifecycleService.createEvalRun(anyString(), any(), any(), any(), any())).thenReturn(runningEvalRun);
        when(lifecycleService.finishEvalRun(
                anyLong(),
                any(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(),
                anyString(),
                anyString(),
                anyLong()
        )).thenReturn(finishedEvalRun);

        DefaultEvalApplicationService service = service(
                suiteRepository,
                caseRepository,
                evalRunRepository,
                evalCaseResultRepository,
                agentRunRepository,
                agentRepository,
                workflowVersionRepository,
                traceWriter,
                nodeExecutionRunner,
                hardCheckEvaluator,
                judgeEvaluator,
                lifecycleService
        );

        var result = service.runSuite(new RunEvalSuiteCommand(300L, List.of()));

        assertThat(result.status()).isEqualTo(RunStatus.FAILED);
        assertThat(result.passedCaseCount()).isZero();
        assertThat(result.failedCaseCount()).isEqualTo(1);
        verifyNoInteractions(judgeEvaluator);

        ArgumentCaptor<EvalCaseResultRecord> resultCaptor = ArgumentCaptor.forClass(EvalCaseResultRecord.class);
        verify(lifecycleService).insertEvalCaseResult(resultCaptor.capture());
        EvalCaseResultRecord caseResult = resultCaptor.getValue();
        assertThat(caseResult.passed()).isFalse();
        assertThat(caseResult.hardCheckResultJson()).isEqualTo(hardCheckResults);
        assertThat(caseResult.judgeResultJson()).isNull();
        assertThat(caseResult.judgeRawText()).isNull();
        assertThat(caseResult.judgeModelOfferingKey()).isNull();
        assertThat(caseResult.judgePromptVersion()).isNull();
        assertThat(caseResult.errorMessage()).isEqualTo("hardChecks \u672a\u901a\u8fc7\uff0c\u5df2\u8df3\u8fc7 judge LLM\u3002");
        verify(lifecycleService).finishEvalAgentRun(
                eq(agentRun.id()),
                eq(RunStatus.FAILED),
                any(),
                eq("EVAL_JUDGE_RULE_FAILED"),
                anyString(),
                anyLong()
        );
    }

    private DefaultEvalCaseApplicationService caseService(
            EvalSuiteRepository suiteRepository,
            EvalCaseRepository caseRepository,
            AgentRunRepository runRepository,
            NodeRunRepository nodeRunRepository
    ) {
        return caseService(
                suiteRepository,
                caseRepository,
                runRepository,
                nodeRunRepository,
                mock(WorkflowVersionRepository.class)
        );
    }

    private DefaultEvalCaseApplicationService caseService(
            EvalSuiteRepository suiteRepository,
            EvalCaseRepository caseRepository,
            AgentRunRepository runRepository,
            NodeRunRepository nodeRunRepository,
            WorkflowVersionRepository workflowVersionRepository
    ) {
        return new DefaultEvalCaseApplicationService(
                OBJECT_MAPPER,
                suiteRepository,
                caseRepository,
                runRepository,
                nodeRunRepository,
                workflowVersionRepository,
                new EvalCaseFormalValidationService(),
                mock(ModelRouteResolver.class)
        );
    }

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
                mock(com.myagent.eval.repository.EvalRunRepository.class),
                mock(com.myagent.eval.repository.EvalCaseResultRepository.class),
                agentRepository,
                workflowVersionRepository,
                runRepository,
                mock(TraceWriter.class),
                mock(NodeExecutionRunner.class),
                mock(EvalHardCheckEvaluator.class),
                mock(EvalJudgeEvaluator.class),
                lifecycleService,
                new EvalCaseFormalValidationService(),
                mock(ModelRouteResolver.class),
                new RunNoGenerator()
        );
    }

    private DefaultEvalApplicationService service(
            EvalSuiteRepository suiteRepository,
            EvalCaseRepository caseRepository,
            EvalRunRepository evalRunRepository,
            EvalCaseResultRepository evalCaseResultRepository,
            AgentRunRepository runRepository,
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            TraceWriter traceWriter,
            NodeExecutionRunner nodeExecutionRunner,
            EvalHardCheckEvaluator hardCheckEvaluator,
            EvalJudgeEvaluator judgeEvaluator,
            EvalRunLifecycleService lifecycleService
    ) {
        return new DefaultEvalApplicationService(
                OBJECT_MAPPER,
                suiteRepository,
                caseRepository,
                evalRunRepository,
                evalCaseResultRepository,
                agentRepository,
                workflowVersionRepository,
                runRepository,
                traceWriter,
                nodeExecutionRunner,
                hardCheckEvaluator,
                judgeEvaluator,
                lifecycleService,
                new EvalCaseFormalValidationService(),
                mock(ModelRouteResolver.class),
                new RunNoGenerator()
        );
    }

    private EvalSuiteRepository suiteRepository(EvalSuiteRecord suite) {
        EvalSuiteRepository repository = mock(EvalSuiteRepository.class);
        when(repository.findById(suite.id())).thenReturn(Optional.of(suite));
        return repository;
    }

    private EvalSuiteRecord suite(long agentId, long workflowVersionId, String nodeId, EvalSuiteStatus status) {
        return new EvalSuiteRecord(
                300L,
                agentId,
                workflowVersionId,
                nodeId,
                "套件",
                "",
                "judge-model",
                BigDecimal.ZERO,
                BigDecimal.valueOf(80),
                status,
                Instant.now(),
                Instant.now()
        );
    }

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

    private JsonNode output() throws Exception {
        return OBJECT_MAPPER.readTree("""
                {
                  "summary": "ok"
                }
                """);
    }

    private EvalCaseRecord withCaseId(EvalCaseRecord record) {
        return new EvalCaseRecord(
                400L,
                record.suiteId(),
                record.caseNo(),
                record.title(),
                record.inputJson(),
                record.referenceSampleJson(),
                record.judgeRuleText(),
                record.hardChecksJson(),
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

    private EvalCaseRecord evalCase(EvalCaseConfirmStatus confirmStatus, String judgeRule, JsonNode hardChecksJson) {
        return new EvalCaseRecord(
                400L,
                300L,
                "CASE-001",
                "测试用例",
                OBJECT_MAPPER.createObjectNode().put("question", "hello"),
                OBJECT_MAPPER.createObjectNode().put("summary", "ok"),
                judgeRule,
                hardChecksJson,
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

    private WorkflowVersionRecord workflowVersion() throws Exception {
        return workflowVersion(true);
    }

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
