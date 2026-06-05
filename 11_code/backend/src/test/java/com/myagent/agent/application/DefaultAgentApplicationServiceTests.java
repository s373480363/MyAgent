package com.myagent.agent.application;

import com.myagent.agent.application.command.CreateAgentCommand;
import com.myagent.agent.application.query.ListAgentsQuery;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import com.myagent.settings.domain.SettingValueType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionHistorySummaryRecord;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agent 应用服务测试。
 */
class DefaultAgentApplicationServiceTests {

    /**
     * 创建 Agent 时必须初始化首个草稿版本，并将 Agent 默认值物化进 runtimeOptions。
     */
    @Test
    void createAgentInitializesFirstDraftWithNormalizedRuntimeOptions() {
        InMemoryAgentRepository agentRepository = new InMemoryAgentRepository();
        InMemoryWorkflowVersionRepository workflowVersionRepository = new InMemoryWorkflowVersionRepository();
        DefaultAgentApplicationService service = new DefaultAgentApplicationService(
                agentRepository,
                workflowVersionRepository,
                new PlatformSettingsResolver(new InMemorySystemSettingRepository(), settingsProperties(5))
        );

        var detail = service.createAgent(new CreateAgentCommand(
                "summary-agent",
                "摘要 Agent",
                "描述",
                "你是摘要助手",
                null,
                new BigDecimal("0.2"),
                900,
                40
        ));

        assertThat(detail.getAgentId()).isEqualTo(1L);
        assertThat(detail.getAgentKey()).isEqualTo("summary-agent");
        assertThat(detail.getDefaultModelOfferingKey()).isNull();
        assertThat(detail.getCurrentDraftWorkflow()).isNotNull();
        assertThat(detail.getCurrentDraftWorkflow().getVersionNo()).isEqualTo(1);
        assertThat(detail.getCurrentPublishedWorkflow()).isNull();
        assertThat(detail.getHistoryVersionSummary().getTotal()).isZero();

        WorkflowVersionRecord draft = workflowVersionRepository.findCurrentDraft(detail.getAgentId()).orElseThrow();
        assertThat(draft.status()).isEqualTo(WorkflowVersionStatus.DRAFT);
        assertThat(draft.runtimeOptions().getTimeoutSeconds()).isEqualTo(900);
        assertThat(draft.runtimeOptions().getMaxSteps()).isEqualTo(40);
        assertThat(draft.runtimeOptions().getMaxAgentCallDepth()).isEqualTo(5);
    }

    /**
     * 构造启动配置。
     *
     * @param defaultMaxAgentCallDepth 默认最大 Agent 调用深度
     * @return 启动配置
     */
    private MyAgentSettingsProperties settingsProperties(int defaultMaxAgentCallDepth) {
        MyAgentSettingsProperties properties = new MyAgentSettingsProperties();
        properties.getRuntime().setDefaultAgentTimeoutSeconds(600);
        properties.getRuntime().setDefaultMaxSteps(30);
        properties.getRuntime().setDefaultMaxAgentCallDepth(defaultMaxAgentCallDepth);
        return properties;
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
                    record.defaultModelOfferingKey(),
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
            if (!records.containsKey(record.id())) {
                return 0;
            }
            records.put(record.id(), new AgentRecord(
                    record.id(),
                    record.agentKey(),
                    record.name(),
                    record.description(),
                    record.status(),
                    record.systemPrompt(),
                    record.defaultModelOfferingKey(),
                    record.temperature(),
                    record.timeoutSeconds(),
                    record.maxSteps(),
                    record.currentDraftWorkflowVersionId(),
                    record.currentPublishedWorkflowVersionId(),
                    records.get(record.id()).createdAt(),
                    Instant.now()
            ));
            return 1;
        }

        @Override
        public int updateStatus(long agentId, EnableStatus status) {
            AgentRecord existing = records.get(agentId);
            if (existing == null) {
                return 0;
            }
            records.put(agentId, new AgentRecord(
                    existing.id(),
                    existing.agentKey(),
                    existing.name(),
                    existing.description(),
                    status,
                    existing.systemPrompt(),
                    existing.defaultModelOfferingKey(),
                    existing.temperature(),
                    existing.timeoutSeconds(),
                    existing.maxSteps(),
                    existing.currentDraftWorkflowVersionId(),
                    existing.currentPublishedWorkflowVersionId(),
                    existing.createdAt(),
                    Instant.now()
            ));
            return 1;
        }

        @Override
        public int updateCurrentDraftWorkflowVersionId(long agentId, Long workflowVersionId) {
            AgentRecord existing = records.get(agentId);
            records.put(agentId, new AgentRecord(
                    existing.id(),
                    existing.agentKey(),
                    existing.name(),
                    existing.description(),
                    existing.status(),
                    existing.systemPrompt(),
                    existing.defaultModelOfferingKey(),
                    existing.temperature(),
                    existing.timeoutSeconds(),
                    existing.maxSteps(),
                    workflowVersionId,
                    existing.currentPublishedWorkflowVersionId(),
                    existing.createdAt(),
                    Instant.now()
            ));
            return 1;
        }

        @Override
        public int updateCurrentPublishedWorkflowVersionId(long agentId, Long workflowVersionId) {
            AgentRecord existing = records.get(agentId);
            records.put(agentId, new AgentRecord(
                    existing.id(),
                    existing.agentKey(),
                    existing.name(),
                    existing.description(),
                    existing.status(),
                    existing.systemPrompt(),
                    existing.defaultModelOfferingKey(),
                    existing.temperature(),
                    existing.timeoutSeconds(),
                    existing.maxSteps(),
                    existing.currentDraftWorkflowVersionId(),
                    workflowVersionId,
                    existing.createdAt(),
                    Instant.now()
            ));
            return 1;
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
                    .findFirst();
        }

        @Override
        public Optional<WorkflowVersionRecord> findCurrentPublished(long agentId) {
            return records.values().stream()
                    .filter(record -> record.agentId() == agentId && record.status() == WorkflowVersionStatus.PUBLISHED)
                    .findFirst();
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
            if (existing == null) {
                return 0;
            }
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
        public PageResult<WorkflowVersionRecord> listWorkflowVersions(com.myagent.workflow.application.query.ListWorkflowVersionsQuery query) {
            List<WorkflowVersionRecord> items = records.values().stream()
                    .filter(record -> record.agentId() == query.agentId())
                    .toList();
            return PageResult.of(items, query.page(), query.pageSize(), items.size());
        }

        @Override
        public WorkflowVersionHistorySummaryRecord summarizeHistory(long agentId) {
            List<WorkflowVersionRecord> historyRecords = records.values().stream()
                    .filter(record -> record.agentId() == agentId && record.status() == WorkflowVersionStatus.HISTORY)
                    .sorted(java.util.Comparator.comparingInt(WorkflowVersionRecord::versionNo).reversed())
                    .toList();
            if (historyRecords.isEmpty()) {
                return new WorkflowVersionHistorySummaryRecord(0L, null, null, null);
            }
            WorkflowVersionRecord latest = historyRecords.getFirst();
            return new WorkflowVersionHistorySummaryRecord(
                    historyRecords.size(),
                    latest.id(),
                    latest.versionNo(),
                    latest.publishedAt()
            );
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
}
