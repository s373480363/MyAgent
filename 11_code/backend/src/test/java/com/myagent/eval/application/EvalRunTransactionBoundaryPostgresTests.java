package com.myagent.eval.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.myagent.model.ModelInvocationResult;
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

    /**
     * PostgreSQL 测试容器。
     */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("myagent_eval_test")
                    .withUsername("myagent")
                    .withPassword("myagent");

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 节点验收应用服务。
     */
    private final EvalApplicationService evalApplicationService;

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * EvalSuite 仓储。
     */
    private final EvalSuiteRepository evalSuiteRepository;

    /**
     * EvalCase 仓储。
     */
    private final EvalCaseRepository evalCaseRepository;

    /**
     * AgentRun 仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * EvalRun 仓储。
     */
    private final EvalRunRepository evalRunRepository;

    /**
     * NodeRun 仓储。
     */
    private final NodeRunRepository nodeRunRepository;

    /**
     * TraceEvent 仓储。
     */
    private final TraceEventRepository traceEventRepository;

    /**
     * 构造集成测试。
     *
     * @param objectMapper JSON 对象映射器
     * @param evalApplicationService 节点验收应用服务
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param evalSuiteRepository EvalSuite 仓储
     * @param evalCaseRepository EvalCase 仓储
     * @param agentRunRepository AgentRun 仓储
     * @param evalRunRepository EvalRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @param traceEventRepository TraceEvent 仓储
     */
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

    /**
     * 注册 PostgreSQL 容器数据源。
     *
     * @param registry 动态配置注册器
     */
    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    /**
     * 正式 EvalRun 的父运行记录必须在线程池写 Trace 前已经提交。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void runSuiteCommitsParentRecordsBeforeWorkerThreadWritesTrace() throws Exception {
        AgentRecord agent = createAgent();
        WorkflowVersionRecord workflowVersion = createWorkflowVersion(agent);
        EvalSuiteRecord suite = createConfirmedSuite(agent, workflowVersion);
        createConfirmedCase(suite);

        EvalRunResult result = evalApplicationService.runSuite(new RunEvalSuiteCommand(suite.id(), List.of(), false));

        AgentRunRecord agentRun = agentRunRepository.findByRunNo(result.runId()).orElseThrow();
        EvalRunRecord evalRun = evalRunRepository.findByRunNo(result.evalRunId()).orElseThrow();
        List<NodeRunRecord> nodeRuns = nodeRunRepository.listByRunId(agentRun.id());
        List<RunTraceEventRecord> traceEvents = traceEventRepository.listByRunId(agentRun.id());

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(agentRun.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(evalRun.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(evalRun.agentRunId()).isEqualTo(agentRun.id());
        assertThat(nodeRuns).hasSize(1);
        assertThat(nodeRuns.getFirst().status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(traceEvents)
                .extracting(RunTraceEventRecord::eventType)
                .contains(TraceEventType.MODEL_REQUEST, TraceEventType.MODEL_RESPONSE, TraceEventType.EVAL_CASE_RESULT);
        assertTraceForeignKeys(traceEvents, TraceEventType.MODEL_REQUEST, agentRun.id(), nodeRuns.getFirst().id(), evalRun.id());
        assertTraceForeignKeys(traceEvents, TraceEventType.MODEL_RESPONSE, agentRun.id(), nodeRuns.getFirst().id(), evalRun.id());
    }

    /**
     * 创建 Agent 主数据。
     *
     * @return Agent 记录
     */
    private AgentRecord createAgent() {
        return agentRepository.insert(new AgentRecord(
                0L,
                "eval-agent-" + UUID.randomUUID().toString().substring(0, 8),
                "验收 Agent",
                "用于验证 EvalRun 事务边界。",
                EnableStatus.ENABLED,
                "你是测试模型。",
                "test-model",
                BigDecimal.ZERO,
                600,
                30,
                null,
                null,
                null,
                null
        ));
    }

    /**
     * 创建包含 LLM 目标节点的已发布工作流版本。
     *
     * @param agent Agent 记录
     * @return 工作流版本
     */
    private WorkflowVersionRecord createWorkflowVersion(AgentRecord agent) {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("llm_1");
        node.setType(WorkflowNodeType.LLM);
        node.setName("LLM 验收节点");
        node.setInputMapping(objectMapper.getNodeFactory().textNode("$.input"));
        node.setConfig(objectMapper.createObjectNode()
                .put("model", "test-model")
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

    /**
     * 创建已确认验收套件。
     *
     * @param agent Agent 记录
     * @param workflowVersion 工作流版本
     * @return 验收套件
     */
    private EvalSuiteRecord createConfirmedSuite(AgentRecord agent, WorkflowVersionRecord workflowVersion) {
        return evalSuiteRepository.insert(new EvalSuiteRecord(
                0L,
                agent.id(),
                workflowVersion.id(),
                "llm_1",
                "LLM 节点验收套件",
                "验证 Trace 外键链路。",
                BigDecimal.ZERO,
                EvalSuiteStatus.CONFIRMED,
                null,
                null
        ));
    }

    /**
     * 创建已确认验收用例。
     *
     * @param suite 验收套件
     * @return 验收用例
     * @throws Exception JSON 构造失败时抛出
     */
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
                objectMapper.readTree("""
                        [
                          {
                            "type": "JSON_PATH_EQUALS",
                            "path": "$.answer",
                            "expected": "ok"
                          }
                        ]
                        """),
                objectMapper.createObjectNode(),
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

    /**
     * 断言指定 Trace 事件带有完整外键。
     *
     * @param traceEvents Trace 事件列表
     * @param eventType 事件类型
     * @param agentRunId AgentRun 主键
     * @param nodeRunId NodeRun 主键
     * @param evalRunId EvalRun 主键
     */
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

    /**
     * 测试用模型网关配置。
     */
    @TestConfiguration
    static class TestModelGatewayConfiguration {

        /**
         * 构造不会外呼网络的模型网关。
         *
         * @param objectMapper JSON 对象映射器
         * @return 测试模型网关
         */
        @Bean
        @Primary
        OpenAiModelGateway testOpenAiModelGateway(ObjectMapper objectMapper) {
            return request -> new ModelInvocationResult(
                    objectMapper.createObjectNode().put("answer", "ok"),
                    "{\"answer\":\"ok\"}",
                    12L
            );
        }
    }
}
