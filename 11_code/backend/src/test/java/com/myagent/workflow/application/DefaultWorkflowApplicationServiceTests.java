package com.myagent.workflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.application.query.ListAgentsQuery;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.page.PageResult;
import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.externalagent.application.query.ListExternalAgentsQuery;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.method.application.query.ListJavaMethodsQuery;
import com.myagent.method.repository.JavaMethodRecord;
import com.myagent.method.repository.JavaMethodRepository;
import com.myagent.schema.application.SchemaApplicationService;
import com.myagent.schema.application.command.CreateSchemaCommand;
import com.myagent.schema.application.command.CreateSchemaVersionCommand;
import com.myagent.schema.application.command.UpdateSchemaDraftCommand;
import com.myagent.schema.application.query.GetSchemaQuery;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.application.result.SchemaDetailResult;
import com.myagent.schema.application.result.SchemaListItemResult;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.settings.domain.SettingValueType;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import com.myagent.tool.application.query.ListToolsQuery;
import com.myagent.tool.repository.ToolRecord;
import com.myagent.tool.repository.ToolRepository;
import com.myagent.workflow.application.command.CopyWorkflowDraftFromVersionCommand;
import com.myagent.workflow.application.command.PublishWorkflowDraftCommand;
import com.myagent.workflow.application.command.SaveWorkflowDraftCommand;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.application.result.WorkflowDraftResult;
import com.myagent.workflow.application.result.WorkflowPublishResult;
import com.myagent.workflow.domain.ReferencedSchemaVersion;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowEdgeType;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowSchemaRef;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionHistorySummaryRecord;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import com.myagent.workflow.validation.DefaultWorkflowDraftValidationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 工作流应用服务测试。
 */
class DefaultWorkflowApplicationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 保存草稿时必须补齐 runtimeOptions 缺省值，并稳定派生去重后的 Schema 引用快照。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void saveWorkflowDraftNormalizesRuntimeOptionsAndDerivesSchemaSnapshots() throws Exception {
        InMemoryAgentRepository agentRepository = new InMemoryAgentRepository();
        InMemoryWorkflowVersionRepository workflowRepository = new InMemoryWorkflowVersionRepository();
        InMemorySchemaRepository schemaRepository = new InMemorySchemaRepository();
        RecordingSchemaApplicationService schemaApplicationService = new RecordingSchemaApplicationService(schemaRepository);
        InMemorySystemSettingRepository systemSettingRepository = new InMemorySystemSettingRepository();
        systemSettingRepository.upsert(new SystemSettingRecord(
                PlatformSettingsResolver.DEFAULT_MAX_AGENT_CALL_DEPTH_KEY,
                "7",
                SettingValueType.NUMBER,
                "",
                true,
                Instant.now()
        ));
        AgentRecord agent = agentRepository.insert(new AgentRecord(
                0L,
                "summary-agent",
                "摘要 Agent",
                "",
                EnableStatus.ENABLED,
                "默认提示词",
                "gpt-4.1-mini",
                null,
                600,
                30,
                null,
                null,
                null,
                null
        ));
        schemaRepository.insert(schemaRecord("z-output", 1, 101L));
        schemaRepository.insert(schemaRecord("a-input", 2, 102L));

        DefaultWorkflowApplicationService service = newWorkflowService(
                agentRepository,
                workflowRepository,
                schemaRepository,
                schemaApplicationService,
                systemSettingRepository
        );

        WorkflowDraftResult result = service.saveWorkflowDraft(new SaveWorkflowDraftCommand(
                agent.id(),
                List.of(
                        startNode("node-start", "a-input", 2),
                        endNode("node-end", "z-output", 1),
                        llmNodeWithOutput("node-llm", "z-output", 1)
                ),
                List.of(edge("edge-1", "node-start", "node-llm"), edge("edge-2", "node-llm", "node-end")),
                OBJECT_MAPPER.readTree("""
                        {
                          "maxSteps": 45
                        }
                        """)
        ));

        assertThat(result.getStatus()).isEqualTo(WorkflowVersionStatus.DRAFT);
        assertThat(result.getRuntimeOptions().getTimeoutSeconds()).isEqualTo(600);
        assertThat(result.getRuntimeOptions().getMaxSteps()).isEqualTo(45);
        assertThat(result.getRuntimeOptions().getMaxAgentCallDepth()).isEqualTo(7);
        assertThat(result.getReferencedSchemaVersions())
                .extracting(ReferencedSchemaVersion::getSchemaKey, ReferencedSchemaVersion::getVersion)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("a-input", 2),
                        org.assertj.core.groups.Tuple.tuple("z-output", 1)
                );
    }

    /**
     * runtimeOptions 出现未知字段时必须直接拒绝，不能把未知字段原样落库。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void saveWorkflowDraftRejectsUnknownRuntimeOptionsField() throws Exception {
        InMemoryAgentRepository agentRepository = new InMemoryAgentRepository();
        InMemoryWorkflowVersionRepository workflowRepository = new InMemoryWorkflowVersionRepository();
        InMemorySchemaRepository schemaRepository = new InMemorySchemaRepository();
        RecordingSchemaApplicationService schemaApplicationService = new RecordingSchemaApplicationService(schemaRepository);
        AgentRecord agent = agentRepository.insert(new AgentRecord(
                0L, "summary-agent", "摘要 Agent", "", EnableStatus.ENABLED, "",
                "gpt-4.1-mini", null, 600, 30, null, null, null, null
        ));
        schemaRepository.insert(schemaRecord("agent-input", 1, 1L));
        schemaRepository.insert(schemaRecord("agent-output", 1, 2L));

        DefaultWorkflowApplicationService service = newWorkflowService(
                agentRepository,
                workflowRepository,
                schemaRepository,
                schemaApplicationService,
                new InMemorySystemSettingRepository()
        );

        assertThatThrownBy(() -> service.saveWorkflowDraft(new SaveWorkflowDraftCommand(
                agent.id(),
                List.of(startNode("start", "agent-input", 1), endNode("end", "agent-output", 1)),
                List.of(edge("edge-1", "start", "end")),
                OBJECT_MAPPER.readTree("""
                        {
                          "timeoutSeconds": 100,
                          "unknownField": 1
                        }
                        """)
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("未定义字段");
    }

    /**
     * 发布校验必须拒绝旧 prompt/promptTemplate 字段，不能因为正式字段存在而保留双轨配置。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void publishWorkflowDraftRejectsDeprecatedPromptFields() throws Exception {
        InMemoryAgentRepository agentRepository = new InMemoryAgentRepository();
        InMemoryWorkflowVersionRepository workflowRepository = new InMemoryWorkflowVersionRepository();
        InMemorySchemaRepository schemaRepository = new InMemorySchemaRepository();
        RecordingSchemaApplicationService schemaApplicationService = new RecordingSchemaApplicationService(schemaRepository);
        AgentRecord agent = agentRepository.insert(new AgentRecord(
                0L, "summary-agent", "摘要 Agent", "", EnableStatus.ENABLED, "",
                "gpt-4.1-mini", null, 600, 30, null, null, null, null
        ));
        schemaRepository.insert(schemaRecord("agent-input", 1, 1L));
        schemaRepository.insert(schemaRecord("agent-output", 1, 2L));

        DefaultWorkflowApplicationService service = newWorkflowService(
                agentRepository,
                workflowRepository,
                schemaRepository,
                schemaApplicationService,
                new InMemorySystemSettingRepository()
        );
        service.saveWorkflowDraft(new SaveWorkflowDraftCommand(
                agent.id(),
                List.of(
                        startNode("start", "agent-input", 1),
                        llmNodeWithDeprecatedPrompt("llm", "agent-output", 1),
                        endNode("end", "agent-output", 1)
                ),
                List.of(edge("edge-1", "start", "llm"), edge("edge-2", "llm", "end")),
                OBJECT_MAPPER.readTree("{}")
        ));

        assertThatThrownBy(() -> service.publishWorkflowDraft(new PublishWorkflowDraftCommand(agent.id(), "发布")))
                .isInstanceOf(BizException.class)
                .satisfies(exception -> assertThat(((BizException) exception).getDetails())
                        .anySatisfy(detail -> assertThat(detail.getMessage()).contains("userPromptTemplate/systemPromptTemplate")));
    }

    /**
     * 复制草稿和发布草稿时必须原样复用已持久化 runtimeOptions，不允许回头读取 Agent 默认值重算。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void copyAndPublishKeepPersistedRuntimeOptionsTruth() throws Exception {
        InMemoryAgentRepository agentRepository = new InMemoryAgentRepository();
        InMemoryWorkflowVersionRepository workflowRepository = new InMemoryWorkflowVersionRepository();
        InMemorySchemaRepository schemaRepository = new InMemorySchemaRepository();
        RecordingSchemaApplicationService schemaApplicationService = new RecordingSchemaApplicationService(schemaRepository);
        InMemorySystemSettingRepository systemSettingRepository = new InMemorySystemSettingRepository();
        AgentRecord agent = agentRepository.insert(new AgentRecord(
                0L, "summary-agent", "摘要 Agent", "默认提示词", EnableStatus.ENABLED, "默认提示词",
                "gpt-4.1-mini", null, 999, 88, null, null, null, null
        ));
        schemaRepository.insert(schemaRecord("agent-input", 1, 1L));
        schemaRepository.insert(schemaRecord("agent-output", 1, 2L));

        WorkflowVersionRecord oldDraft = workflowRepository.insert(new WorkflowVersionRecord(
                0L, agent.id(), 1, WorkflowVersionStatus.DRAFT,
                List.of(startNode("start", "agent-input", 1), endNode("end", "agent-output", 1)),
                List.of(edge("edge-1", "start", "end")),
                new WorkflowRuntimeOptions(111, 11, 1),
                List.of(
                        new ReferencedSchemaVersion(1L, "agent-input", 1),
                        new ReferencedSchemaVersion(2L, "agent-output", 1)
                ),
                null,
                null,
                null,
                null
        ));
        WorkflowVersionRecord sourceVersion = workflowRepository.insert(new WorkflowVersionRecord(
                0L, agent.id(), 2, WorkflowVersionStatus.HISTORY,
                List.of(startNode("start", "agent-input", 1), endNode("end", "agent-output", 1)),
                List.of(edge("edge-1", "start", "end")),
                new WorkflowRuntimeOptions(321, 12, 4),
                List.of(
                        new ReferencedSchemaVersion(1L, "agent-input", 1),
                        new ReferencedSchemaVersion(2L, "agent-output", 1)
                ),
                null,
                null,
                null,
                null
        ));
        WorkflowVersionRecord currentPublished = workflowRepository.insert(new WorkflowVersionRecord(
                0L, agent.id(), 3, WorkflowVersionStatus.PUBLISHED,
                List.of(startNode("start", "agent-input", 1), endNode("end", "agent-output", 1)),
                List.of(edge("edge-1", "start", "end")),
                new WorkflowRuntimeOptions(200, 20, 2),
                List.of(
                        new ReferencedSchemaVersion(1L, "agent-input", 1),
                        new ReferencedSchemaVersion(2L, "agent-output", 1)
                ),
                null,
                Instant.now(),
                null,
                null
        ));
        agentRepository.updateCurrentDraftWorkflowVersionId(agent.id(), oldDraft.id());
        agentRepository.updateCurrentPublishedWorkflowVersionId(agent.id(), currentPublished.id());
        agentRepository.replaceTimeoutAndMaxSteps(agent.id(), 5000, 500);

        DefaultWorkflowApplicationService service = newWorkflowService(
                agentRepository,
                workflowRepository,
                schemaRepository,
                schemaApplicationService,
                systemSettingRepository
        );

        WorkflowDraftResult copiedDraft = service.copyWorkflowDraftFromVersion(new CopyWorkflowDraftFromVersionCommand(
                agent.id(),
                sourceVersion.id()
        ));
        assertThat(copiedDraft.getRuntimeOptions().getTimeoutSeconds()).isEqualTo(321);
        assertThat(copiedDraft.getRuntimeOptions().getMaxSteps()).isEqualTo(12);
        assertThat(copiedDraft.getRuntimeOptions().getMaxAgentCallDepth()).isEqualTo(4);
        assertThat(copiedDraft.getSourceWorkflowVersionId()).isEqualTo(sourceVersion.id());
        assertThat(workflowRepository.findById(oldDraft.id()).orElseThrow().status()).isEqualTo(WorkflowVersionStatus.HISTORY);

        WorkflowPublishResult publishResult = service.publishWorkflowDraft(new PublishWorkflowDraftCommand(agent.id(), "发布"));
        WorkflowVersionRecord published = workflowRepository.findById(publishResult.getWorkflowVersionId()).orElseThrow();
        AgentRecord updatedAgent = agentRepository.findById(agent.id()).orElseThrow();
        assertThat(published.runtimeOptions().getTimeoutSeconds()).isEqualTo(321);
        assertThat(published.runtimeOptions().getMaxSteps()).isEqualTo(12);
        assertThat(published.runtimeOptions().getMaxAgentCallDepth()).isEqualTo(4);
        assertThat(updatedAgent.currentDraftWorkflowVersionId()).isEqualTo(copiedDraft.getWorkflowVersionId());
        assertThat(updatedAgent.currentPublishedWorkflowVersionId()).isEqualTo(publishResult.getWorkflowVersionId());
        assertThat(workflowRepository.findById(currentPublished.id()).orElseThrow().status()).isEqualTo(WorkflowVersionStatus.HISTORY);
        assertThat(schemaApplicationService.lockedSchemaIds).containsExactlyInAnyOrder(1L, 2L);
    }

    /**
     * 创建工作流应用服务。
     *
     * @param agentRepository Agent 仓储
     * @param workflowRepository 工作流仓储
     * @param schemaRepository Schema 仓储
     * @param schemaApplicationService Schema 应用服务
     * @param systemSettingRepository 系统设置仓储
     * @return 工作流应用服务
     */
    private DefaultWorkflowApplicationService newWorkflowService(
            InMemoryAgentRepository agentRepository,
            InMemoryWorkflowVersionRepository workflowRepository,
            InMemorySchemaRepository schemaRepository,
            RecordingSchemaApplicationService schemaApplicationService,
            InMemorySystemSettingRepository systemSettingRepository
    ) {
        return new DefaultWorkflowApplicationService(
                agentRepository,
                workflowRepository,
                schemaRepository,
                schemaApplicationService,
                new PlatformSettingsResolver(systemSettingRepository, settingsProperties()),
                new DefaultWorkflowDraftValidationService(
                        schemaRepository,
                        new EmptyJavaMethodRepository(),
                        new EmptyToolRepository(),
                        new EmptyExternalAgentRepository(),
                        agentRepository,
                        new com.myagent.workflow.validation.WorkflowMappingValidationService(schemaRepository)
                )
        );
    }

    /**
     * 构造启动配置。
     *
     * @return 启动配置
     */
    private MyAgentSettingsProperties settingsProperties() {
        MyAgentSettingsProperties properties = new MyAgentSettingsProperties();
        properties.getOpenai().setDefaultModel("gpt-4.1-mini");
        properties.getRuntime().setDefaultAgentTimeoutSeconds(600);
        properties.getRuntime().setDefaultMaxSteps(30);
        properties.getRuntime().setDefaultMaxAgentCallDepth(3);
        return properties;
    }

    /**
     * 构造 Schema 记录。
     *
     * @param schemaKey Schema 业务键
     * @param version 版本号
     * @param id 主键
     * @return Schema 记录
     *
     * @throws Exception JSON 构造失败时抛出
     */
    private SchemaRecord schemaRecord(String schemaKey, int version, long id) throws Exception {
        return new SchemaRecord(
                id,
                schemaKey,
                version,
                schemaKey,
                "",
                OBJECT_MAPPER.readTree("""
                        {
                          "type": "object"
                        }
                        """),
                "",
                SchemaCreatedFrom.USER_CREATED,
                SchemaStatus.ACTIVE,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造开始节点。
     *
     * @param nodeId 节点标识
     * @param schemaKey 输入 Schema 业务键
     * @param version 输入 Schema 版本号
     * @return 开始节点
     */
    private WorkflowNodeDefinition startNode(String nodeId, String schemaKey, int version) {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId(nodeId);
        node.setType(WorkflowNodeType.START);
        node.setName("开始");
        WorkflowSchemaRef schemaRef = new WorkflowSchemaRef();
        schemaRef.setSchemaKey(schemaKey);
        schemaRef.setVersion(version);
        node.setInputSchemaRef(schemaRef);
        return node;
    }

    /**
     * 构造结束节点。
     *
     * @param nodeId 节点标识
     * @param schemaKey 输出 Schema 业务键
     * @param version 输出 Schema 版本号
     * @return 结束节点
     */
    private WorkflowNodeDefinition endNode(String nodeId, String schemaKey, int version) {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId(nodeId);
        node.setType(WorkflowNodeType.END);
        node.setName("结束");
        WorkflowSchemaRef schemaRef = new WorkflowSchemaRef();
        schemaRef.setSchemaKey(schemaKey);
        schemaRef.setVersion(version);
        node.setOutputSchemaRef(schemaRef);
        return node;
    }

    /**
     * 构造 LLM 节点。
     *
     * @param nodeId 节点标识
     * @param schemaKey 输出 Schema 业务键
     * @param version 输出 Schema 版本号
     * @return LLM 节点
     *
     * @throws Exception JSON 构造失败时抛出
     */
    private WorkflowNodeDefinition llmNodeWithOutput(String nodeId, String schemaKey, int version) throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId(nodeId);
        node.setType(WorkflowNodeType.LLM);
        node.setName("LLM");
        node.setConfig(OBJECT_MAPPER.readTree("""
                {
                  "userPromptTemplate": "请总结输入 {inputJson}"
                }
                """));
        WorkflowSchemaRef schemaRef = new WorkflowSchemaRef();
        schemaRef.setSchemaKey(schemaKey);
        schemaRef.setVersion(version);
        node.setOutputSchemaRef(schemaRef);
        return node;
    }

    /**
     * 构造带旧提示词字段的 LLM 节点。
     *
     * @param nodeId 节点标识
     * @param schemaKey 输出 Schema 业务键
     * @param version 输出 Schema 版本号
     * @return LLM 节点
     * @throws Exception JSON 构造失败时抛出
     */
    private WorkflowNodeDefinition llmNodeWithDeprecatedPrompt(String nodeId, String schemaKey, int version) throws Exception {
        WorkflowNodeDefinition node = llmNodeWithOutput(nodeId, schemaKey, version);
        node.setConfig(OBJECT_MAPPER.readTree("""
                {
                  "userPromptTemplate": "请总结输入 {inputJson}",
                  "prompt": {}
                }
                """));
        return node;
    }

    /**
     * 构造普通边。
     *
     * @param edgeId 边标识
     * @param sourceNodeId 源节点标识
     * @param targetNodeId 目标节点标识
     * @return 边定义
     */
    private WorkflowEdgeDefinition edge(String edgeId, String sourceNodeId, String targetNodeId) {
        WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition();
        edge.setEdgeId(edgeId);
        edge.setSourceNodeId(sourceNodeId);
        edge.setTargetNodeId(targetNodeId);
        edge.setType(WorkflowEdgeType.NORMAL);
        return edge;
    }

    /**
     * 内存 Agent 仓储。
     */
    private static final class InMemoryAgentRepository implements AgentRepository {

        /**
         * Agent 数据。
         */
        private final Map<Long, AgentRecord> records = new LinkedHashMap<>();

        /**
         * 下一个主键。
         */
        private long nextId = 1L;

        @Override
        public PageResult<AgentRecord> listAgents(ListAgentsQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        @Override
        public Optional<AgentRecord> findById(long agentId) {
            return Optional.ofNullable(records.get(agentId));
        }

        @Override
        public Optional<AgentRecord> findByAgentKey(String agentKey) {
            return records.values().stream().filter(record -> record.agentKey().equals(agentKey)).findFirst();
        }

        @Override
        public AgentRecord insert(AgentRecord record) {
            long id = nextId++;
            Instant now = Instant.now();
            AgentRecord inserted = new AgentRecord(
                    id,
                    record.agentKey(),
                    record.name(),
                    record.description(),
                    record.status(),
                    record.systemPrompt(),
                    record.defaultModel(),
                    record.temperature(),
                    record.timeoutSeconds(),
                    record.maxSteps(),
                    record.currentDraftWorkflowVersionId(),
                    record.currentPublishedWorkflowVersionId(),
                    now,
                    now
            );
            records.put(id, inserted);
            return inserted;
        }

        @Override
        public int update(AgentRecord record) {
            records.put(record.id(), record);
            return 1;
        }

        @Override
        public int updateStatus(long agentId, EnableStatus status) {
            AgentRecord existing = records.get(agentId);
            if (existing == null) {
                return 0;
            }
            records.put(agentId, new AgentRecord(
                    existing.id(), existing.agentKey(), existing.name(), existing.description(),
                    status, existing.systemPrompt(), existing.defaultModel(), existing.temperature(),
                    existing.timeoutSeconds(), existing.maxSteps(), existing.currentDraftWorkflowVersionId(),
                    existing.currentPublishedWorkflowVersionId(), existing.createdAt(), Instant.now()
            ));
            return 1;
        }

        @Override
        public int updateCurrentDraftWorkflowVersionId(long agentId, Long workflowVersionId) {
            AgentRecord existing = records.get(agentId);
            records.put(agentId, new AgentRecord(
                    existing.id(), existing.agentKey(), existing.name(), existing.description(),
                    existing.status(), existing.systemPrompt(), existing.defaultModel(), existing.temperature(),
                    existing.timeoutSeconds(), existing.maxSteps(), workflowVersionId,
                    existing.currentPublishedWorkflowVersionId(), existing.createdAt(), Instant.now()
            ));
            return 1;
        }

        @Override
        public int updateCurrentPublishedWorkflowVersionId(long agentId, Long workflowVersionId) {
            AgentRecord existing = records.get(agentId);
            records.put(agentId, new AgentRecord(
                    existing.id(), existing.agentKey(), existing.name(), existing.description(),
                    existing.status(), existing.systemPrompt(), existing.defaultModel(), existing.temperature(),
                    existing.timeoutSeconds(), existing.maxSteps(), existing.currentDraftWorkflowVersionId(),
                    workflowVersionId, existing.createdAt(), Instant.now()
            ));
            return 1;
        }

        /**
         * 直接替换 Agent 默认超时和最大步数，用于验证发布不回头读取 Agent 默认值。
         *
         * @param agentId Agent 主键
         * @param timeoutSeconds 新总超时
         * @param maxSteps 新最大步数
         */
        void replaceTimeoutAndMaxSteps(long agentId, int timeoutSeconds, int maxSteps) {
            AgentRecord existing = records.get(agentId);
            records.put(agentId, new AgentRecord(
                    existing.id(), existing.agentKey(), existing.name(), existing.description(),
                    existing.status(), existing.systemPrompt(), existing.defaultModel(), existing.temperature(),
                    timeoutSeconds, maxSteps, existing.currentDraftWorkflowVersionId(),
                    existing.currentPublishedWorkflowVersionId(), existing.createdAt(), Instant.now()
            ));
        }
    }

    /**
     * 内存工作流版本仓储。
     */
    private static final class InMemoryWorkflowVersionRepository implements WorkflowVersionRepository {

        /**
         * 工作流版本数据。
         */
        private final Map<Long, WorkflowVersionRecord> records = new LinkedHashMap<>();

        /**
         * 下一个主键。
         */
        private long nextId = 1L;

        @Override
        public Optional<WorkflowVersionRecord> findById(long workflowVersionId) {
            return Optional.ofNullable(records.get(workflowVersionId));
        }

        @Override
        public Optional<WorkflowVersionRecord> findCurrentDraft(long agentId) {
            return records.values().stream()
                    .filter(record -> record.agentId() == agentId && record.status() == WorkflowVersionStatus.DRAFT)
                    .max(Comparator.comparingInt(WorkflowVersionRecord::versionNo));
        }

        @Override
        public Optional<WorkflowVersionRecord> findCurrentPublished(long agentId) {
            return records.values().stream()
                    .filter(record -> record.agentId() == agentId && record.status() == WorkflowVersionStatus.PUBLISHED)
                    .max(Comparator.comparingInt(WorkflowVersionRecord::versionNo));
        }

        @Override
        public int findMaxVersionNo(long agentId) {
            return records.values().stream()
                    .filter(record -> record.agentId() == agentId)
                    .mapToInt(WorkflowVersionRecord::versionNo)
                    .max()
                    .orElse(0);
        }

        @Override
        public WorkflowVersionRecord insert(WorkflowVersionRecord record) {
            long id = nextId++;
            Instant now = Instant.now();
            WorkflowVersionRecord inserted = new WorkflowVersionRecord(
                    id,
                    record.agentId(),
                    record.versionNo(),
                    record.status(),
                    record.nodes(),
                    record.edges(),
                    record.runtimeOptions(),
                    record.referencedSchemaVersions(),
                    record.sourceWorkflowVersionId(),
                    record.publishedAt(),
                    now,
                    now
            );
            records.put(id, inserted);
            return inserted;
        }

        @Override
        public int updateStatus(long workflowVersionId, WorkflowVersionStatus status) {
            WorkflowVersionRecord existing = records.get(workflowVersionId);
            records.put(workflowVersionId, new WorkflowVersionRecord(
                    existing.id(),
                    existing.agentId(),
                    existing.versionNo(),
                    status,
                    existing.nodes(),
                    existing.edges(),
                    existing.runtimeOptions(),
                    existing.referencedSchemaVersions(),
                    existing.sourceWorkflowVersionId(),
                    existing.publishedAt(),
                    existing.createdAt(),
                    Instant.now()
            ));
            return 1;
        }

        @Override
        public PageResult<WorkflowVersionRecord> listWorkflowVersions(ListWorkflowVersionsQuery query) {
            List<WorkflowVersionRecord> items = records.values().stream()
                    .filter(record -> record.agentId() == query.agentId())
                    .filter(record -> query.status() == null || record.status() == query.status())
                    .sorted(Comparator.comparingInt(WorkflowVersionRecord::versionNo).reversed())
                    .toList();
            return PageResult.of(items, query.page(), query.pageSize(), items.size());
        }

        @Override
        public WorkflowVersionHistorySummaryRecord summarizeHistory(long agentId) {
            List<WorkflowVersionRecord> historyRecords = records.values().stream()
                    .filter(record -> record.agentId() == agentId && record.status() == WorkflowVersionStatus.HISTORY)
                    .sorted(Comparator.comparingInt(WorkflowVersionRecord::versionNo).reversed())
                    .toList();
            if (historyRecords.isEmpty()) {
                return new WorkflowVersionHistorySummaryRecord(0L, null, null, null);
            }
            WorkflowVersionRecord latest = historyRecords.getFirst();
            return new WorkflowVersionHistorySummaryRecord(historyRecords.size(), latest.id(), latest.versionNo(), latest.publishedAt());
        }
    }

    /**
     * 内存 Schema 仓储。
     */
    private static final class InMemorySchemaRepository implements SchemaRepository {

        /**
         * Schema 数据。
         */
        private final Map<Long, SchemaRecord> records = new LinkedHashMap<>();

        @Override
        public PageResult<SchemaRecord> listSchemas(ListSchemasQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        @Override
        public Optional<SchemaRecord> findById(long schemaId) {
            return Optional.ofNullable(records.get(schemaId));
        }

        @Override
        public Optional<SchemaRecord> findByKeyAndVersion(String schemaKey, int version) {
            return records.values().stream()
                    .filter(record -> record.getSchemaKey().equals(schemaKey) && record.getVersion() == version)
                    .findFirst();
        }

        @Override
        public int findMaxVersion(String schemaKey) {
            return records.values().stream()
                    .filter(record -> record.getSchemaKey().equals(schemaKey))
                    .mapToInt(SchemaRecord::getVersion)
                    .max()
                    .orElse(0);
        }

        @Override
        public SchemaRecord insert(SchemaRecord record) {
            records.put(record.getId(), record);
            return record;
        }

        @Override
        public int updateDraft(SchemaRecord record) {
            records.put(record.getId(), record);
            return 1;
        }

        @Override
        public int lockSchemaVersion(long schemaId) {
            SchemaRecord existing = records.get(schemaId);
            records.put(schemaId, new SchemaRecord(
                    existing.getId(),
                    existing.getSchemaKey(),
                    existing.getVersion(),
                    existing.getName(),
                    existing.getDescription(),
                    existing.getJsonSchema(),
                    existing.getJavaType(),
                    existing.getCreatedFrom(),
                    existing.getStatus(),
                    true,
                    existing.getCreatedAt(),
                    Instant.now()
            ));
            return 1;
        }
    }

    /**
     * 记录锁定行为的 Schema 应用服务桩。
     */
    private static final class RecordingSchemaApplicationService implements SchemaApplicationService {

        /**
         * Schema 仓储。
         */
        private final InMemorySchemaRepository schemaRepository;

        /**
         * 已锁定的 Schema 主键。
         */
        private final java.util.List<Long> lockedSchemaIds = new java.util.ArrayList<>();

        /**
         * 构造应用服务桩。
         *
         * @param schemaRepository Schema 仓储
         */
        private RecordingSchemaApplicationService(InMemorySchemaRepository schemaRepository) {
            this.schemaRepository = schemaRepository;
        }

        @Override
        public PageResult<SchemaListItemResult> listSchemas(ListSchemasQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SchemaDetailResult createSchema(CreateSchemaCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SchemaDetailResult updateSchemaDraft(UpdateSchemaDraftCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SchemaDetailResult getSchema(GetSchemaQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SchemaDetailResult createSchemaVersion(CreateSchemaVersionCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lockSchemaVersion(long schemaId) {
            lockedSchemaIds.add(schemaId);
            schemaRepository.lockSchemaVersion(schemaId);
        }
    }

    /**
     * 内存系统设置仓储。
     */
    private static final class InMemorySystemSettingRepository implements SystemSettingRepository {

        /**
         * 系统设置覆盖值。
         */
        private final Map<String, SystemSettingRecord> records = new LinkedHashMap<>();

        @Override
        public Map<String, SystemSettingRecord> findByKeys(List<String> settingKeys) {
            Map<String, SystemSettingRecord> result = new LinkedHashMap<>();
            for (String settingKey : settingKeys) {
                if (records.containsKey(settingKey)) {
                    result.put(settingKey, records.get(settingKey));
                }
            }
            return result;
        }

        @Override
        public void upsert(SystemSettingRecord record) {
            records.put(record.settingKey(), record);
        }
    }

    /**
     * 空 Java 方法仓储。
     */
    private static final class EmptyJavaMethodRepository implements JavaMethodRepository {

        @Override
        public PageResult<JavaMethodRecord> listJavaMethods(ListJavaMethodsQuery query) {
            return PageResult.empty(query.page(), query.pageSize());
        }

        @Override
        public Optional<JavaMethodRecord> findById(long methodId) {
            return Optional.empty();
        }

        @Override
        public Optional<JavaMethodRecord> findByMethodKey(String methodKey) {
            return Optional.empty();
        }
    }

    /**
     * 空工具仓储。
     */
    private static final class EmptyToolRepository implements ToolRepository {

        @Override
        public PageResult<ToolRecord> listTools(ListToolsQuery query) {
            return PageResult.empty(query.page(), query.pageSize());
        }

        @Override
        public Optional<ToolRecord> findById(long toolId) {
            return Optional.empty();
        }

        @Override
        public Optional<ToolRecord> findByToolKey(String toolKey) {
            return Optional.empty();
        }
    }

    /**
     * 空外部 Agent 仓储。
     */
    private static final class EmptyExternalAgentRepository implements ExternalAgentRepository {

        @Override
        public PageResult<ExternalAgentRecord> listExternalAgents(ListExternalAgentsQuery query) {
            return PageResult.empty(query.page(), query.pageSize());
        }

        @Override
        public Optional<ExternalAgentRecord> findById(long adapterId) {
            return Optional.empty();
        }

        @Override
        public Optional<ExternalAgentRecord> findByAdapterKey(String adapterKey) {
            return Optional.empty();
        }

        @Override
        public ExternalAgentRecord insert(ExternalAgentRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(ExternalAgentRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(long adapterId, EnableStatus status) {
            throw new UnsupportedOperationException();
        }
    }
}
