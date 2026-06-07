package com.myagent.eval.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.eval.application.command.RunEvalSuiteCommand;
import com.myagent.eval.application.result.EvalRunResult;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.domain.EvalSuiteStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.eval.repository.EvalCaseRepository;
import com.myagent.eval.repository.EvalRunRecord;
import com.myagent.eval.repository.EvalRunRepository;
import com.myagent.eval.repository.EvalSuiteRecord;
import com.myagent.eval.repository.EvalSuiteRepository;
import com.myagent.model.ModelInvocationRequest;
import com.myagent.model.ModelInvocationResult;
import com.myagent.model.ModelRequestTracePayload;
import com.myagent.model.OpenAiModelGateway;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.TraceEventType;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.run.repository.RunTraceEventRecord;
import com.myagent.run.repository.TraceEventRepository;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EvalRun 事务边界 PostgreSQL 集成测试。
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class EvalRunTransactionBoundaryPostgresTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("myagent_eval_test")
                    .withUsername("myagent")
                    .withPassword("myagent");

    private final ObjectMapper objectMapper;
    private final EvalApplicationService evalApplicationService;
    private final AgentRepository agentRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final EvalSuiteRepository evalSuiteRepository;
    private final EvalCaseRepository evalCaseRepository;
    private final AgentRunRepository agentRunRepository;
    private final EvalRunRepository evalRunRepository;
    private final NodeRunRepository nodeRunRepository;
    private final TraceEventRepository traceEventRepository;

    @Autowired
    EvalRunTransactionBoundaryPostgresTests(
            ObjectMapper objectMapper,
            EvalApplicationService evalApplicationService,
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            EvalSuiteRepository evalSuiteRepository,
            EvalCaseRepository evalCaseRepository,
            AgentRunRepository agentRunRepository,
            EvalRunRepository evalRunRepository,
            NodeRunRepository nodeRunRepository,
            TraceEventRepository traceEventRepository
    ) {
        this.objectMapper = objectMapper;
        this.evalApplicationService = evalApplicationService;
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.evalSuiteRepository = evalSuiteRepository;
        this.evalCaseRepository = evalCaseRepository;
        this.agentRunRepository = agentRunRepository;
        this.evalRunRepository = evalRunRepository;
        this.nodeRunRepository = nodeRunRepository;
        this.traceEventRepository = traceEventRepository;
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void runSuiteCommitsParentRecordsBeforeWorkerThreadWritesTrace() throws Exception {
        AgentRecord agent = createAgent();
        WorkflowVersionRecord workflowVersion = createWorkflowVersion(agent);
        EvalSuiteRecord suite = createConfirmedSuite(agent, workflowVersion);
        createConfirmedCase(suite);

        EvalRunResult result = evalApplicationService.runSuite(new RunEvalSuiteCommand(suite.id(), List.of()));

        AgentRunRecord agentRun = agentRunRepository.findByRunNo(result.runId()).orElseThrow();
        EvalRunRecord evalRun = evalRunRepository.findByRunNo(result.evalRunId()).orElseThrow();
        List<NodeRunRecord> nodeRuns = nodeRunRepository.listByRunId(agentRun.id());
        List<RunTraceEventRecord> traceEvents = traceEventRepository.listByRunId(agentRun.id());

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(agentRun.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(evalRun.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(evalRun.agentRunId()).isEqualTo(agentRun.id());
        assertThat(nodeRuns).hasSize(1);
        assertThat(nodeRuns.get(0).status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(traceEvents)
                .extracting(RunTraceEventRecord::eventType)
                .contains(TraceEventType.MODEL_REQUEST, TraceEventType.MODEL_RESPONSE, TraceEventType.EVAL_CASE_RESULT);
        assertTraceForeignKeys(traceEvents, TraceEventType.MODEL_REQUEST, agentRun.id(), nodeRuns.get(0).id(), evalRun.id());
        assertTraceForeignKeys(traceEvents, TraceEventType.MODEL_RESPONSE, agentRun.id(), nodeRuns.get(0).id(), evalRun.id());
    }

    private AgentRecord createAgent() {
        return agentRepository.insert(new AgentRecord(
                0L,
                "eval-agent-" + UUID.randomUUID().toString().substring(0, 8),
                "验收 Agent",
                "用于验证 EvalRun 事务边界。",
                EnableStatus.ENABLED,
                "你是测试模型。",
                null,
                BigDecimal.ZERO,
                600,
                30,
                null,
                null,
                null,
                null
        ));
    }

    private WorkflowVersionRecord createWorkflowVersion(AgentRecord agent) {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("llm_1");
        node.setType(WorkflowNodeType.LLM);
        node.setName("LLM 验收节点");
        node.setInputMapping(objectMapper.getNodeFactory().textNode("$.input"));
        node.setConfig(objectMapper.createObjectNode()
                .put("modelOfferingKey", "test-model")
                .put("userPromptTemplate", "请根据输入返回 JSON：{inputJson}")
                .put("temperature", 0));
        WorkflowVersionRecord workflowVersion = workflowVersionRepository.insert(new WorkflowVersionRecord(
                0L,
                agent.id(),
                1,
                WorkflowVersionStatus.PUBLISHED,
                List.of(node),
                List.of(),
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of(),
                null,
                Instant.now(),
                null,
                null
        ));
        agentRepository.updateCurrentPublishedWorkflowVersionId(agent.id(), workflowVersion.id());
        return workflowVersion;
    }

    private EvalSuiteRecord createConfirmedSuite(AgentRecord agent, WorkflowVersionRecord workflowVersion) {
        return evalSuiteRepository.insert(new EvalSuiteRecord(
                0L,
                agent.id(),
                workflowVersion.id(),
                "llm_1",
                "LLM 节点验收套件",
                "验证 Trace 外键链路。",
                "test-model",
                BigDecimal.ZERO,
                BigDecimal.valueOf(100),
                EvalSuiteStatus.CONFIRMED,
                null,
                null
        ));
    }

    private EvalCaseRecord createConfirmedCase(EvalSuiteRecord suite) throws Exception {
        return evalCaseRepository.insert(new EvalCaseRecord(
                0L,
                suite.id(),
                "CASE-0001",
                "模型返回 ok",
                objectMapper.readTree("""
                        {
                          "question": "hello"
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "answer": "ok"
                        }
                        """),
                "请判断 answer 是否等于 ok，并给出通过结论。",
                objectMapper.createArrayNode(),
                true,
                EvalCaseConfirmStatus.USER_CONFIRMED,
                null,
                null,
                null,
                null,
                "正式用例",
                null,
                null
        ));
    }

    private void assertTraceForeignKeys(
            List<RunTraceEventRecord> traceEvents,
            TraceEventType eventType,
            long agentRunId,
            long nodeRunId,
            long evalRunId
    ) {
        RunTraceEventRecord event = traceEvents.stream()
                .filter(traceEvent -> traceEvent.eventType() == eventType)
                .findFirst()
                .orElseThrow();
        assertThat(event.runId()).isEqualTo(agentRunId);
        assertThat(event.nodeRunId()).isEqualTo(nodeRunId);
        assertThat(event.evalRunId()).isEqualTo(evalRunId);
    }

    @TestConfiguration
    static class TestModelGatewayConfiguration {

        @Bean
        @Primary
        OpenAiModelGateway testOpenAiModelGateway(ObjectMapper objectMapper) {
            return new OpenAiModelGateway() {
                @Override
                public ModelRequestTracePayload resolveRequestTracePayload(ModelInvocationRequest request) {
                    return new ModelRequestTracePayload(
                            "openai",
                            "OpenAI",
                            request.modelOfferingKey(),
                            "gpt_4_1_mini",
                            "gpt-4.1-mini",
                            BigDecimal.ZERO,
                            request.structuredOutput()
                    );
                }

                @Override
                public ModelInvocationResult invoke(ModelInvocationRequest request) {
                    if (request.systemPrompt() != null && request.systemPrompt().contains("judge")) {
                        ObjectNode judgeResult = objectMapper.createObjectNode();
                        judgeResult.put("passed", true);
                        judgeResult.put("score", 100);
                        judgeResult.put("reason", "ok");
                        judgeResult.set("criteriaResults", objectMapper.createArrayNode());
                        return new ModelInvocationResult(
                                judgeResult,
                                "{\"passed\":true,\"score\":100,\"reason\":\"ok\",\"criteriaResults\":[]}",
                                12L
                        );
                    }
                    return new ModelInvocationResult(
                            objectMapper.createObjectNode().put("answer", "ok"),
                            "{\"answer\":\"ok\"}",
                            12L
                    );
                }
            };
        }
    }
}
