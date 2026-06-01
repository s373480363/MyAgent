package com.myagent.workflow.repository;

import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * WorkflowVersion 当前版本指针 PostgreSQL 集成测试。
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class WorkflowVersionCurrentPointerPostgresTests {

    /**
     * PostgreSQL 测试容器。
     */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("myagent_workflow_pointer_test")
                    .withUsername("myagent")
                    .withPassword("myagent");

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * 构造当前版本指针测试。
     *
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     */
    @Autowired
    WorkflowVersionCurrentPointerPostgresTests(
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository
    ) {
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
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
     * 当前草稿必须按 Agent 草稿指针读取。
     */
    @Test
    void findCurrentDraftUsesAgentDraftPointer() {
        AgentRecord agent = createAgent();
        WorkflowVersionRecord draft = createWorkflowVersion(agent, 1, WorkflowVersionStatus.DRAFT);
        WorkflowVersionRecord history = createWorkflowVersion(agent, 2, WorkflowVersionStatus.HISTORY);
        agentRepository.updateCurrentDraftWorkflowVersionId(agent.id(), draft.id());

        assertThat(workflowVersionRepository.findCurrentDraft(agent.id()))
                .get()
                .extracting(WorkflowVersionRecord::id)
                .isEqualTo(draft.id());
        assertThat(workflowVersionRepository.findCurrentDraft(agent.id()))
                .get()
                .extracting(WorkflowVersionRecord::id)
                .isNotEqualTo(history.id());
    }

    /**
     * 草稿指针为空时不得按 DRAFT 状态回退。
     */
    @Test
    void findCurrentDraftDoesNotFallbackToDraftStatusWhenPointerIsNull() {
        AgentRecord agent = createAgent();
        createWorkflowVersion(agent, 1, WorkflowVersionStatus.DRAFT);

        assertThat(workflowVersionRepository.findCurrentDraft(agent.id())).isEmpty();
    }

    /**
     * 当前发布必须按 Agent 发布指针读取。
     */
    @Test
    void findCurrentPublishedUsesAgentPublishedPointer() {
        AgentRecord agent = createAgent();
        WorkflowVersionRecord published = createWorkflowVersion(agent, 1, WorkflowVersionStatus.PUBLISHED);
        WorkflowVersionRecord history = createWorkflowVersion(agent, 2, WorkflowVersionStatus.HISTORY);
        agentRepository.updateCurrentPublishedWorkflowVersionId(agent.id(), published.id());

        assertThat(workflowVersionRepository.findCurrentPublished(agent.id()))
                .get()
                .extracting(WorkflowVersionRecord::id)
                .isEqualTo(published.id());
        assertThat(workflowVersionRepository.findCurrentPublished(agent.id()))
                .get()
                .extracting(WorkflowVersionRecord::id)
                .isNotEqualTo(history.id());
    }

    /**
     * 发布指针为空时不得按 PUBLISHED 状态回退。
     */
    @Test
    void findCurrentPublishedDoesNotFallbackToPublishedStatusWhenPointerIsNull() {
        AgentRecord agent = createAgent();
        createWorkflowVersion(agent, 1, WorkflowVersionStatus.PUBLISHED);

        assertThat(workflowVersionRepository.findCurrentPublished(agent.id())).isEmpty();
    }

    /**
     * 创建 Agent 主数据。
     *
     * @return Agent 记录
     */
    private AgentRecord createAgent() {
        return agentRepository.insert(new AgentRecord(
                0L,
                "workflow-pointer-agent-" + UUID.randomUUID().toString().substring(0, 8),
                "工作流指针 Agent",
                "用于验证当前工作流版本指针。",
                EnableStatus.ENABLED,
                "",
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
     * 创建工作流版本。
     *
     * @param agent Agent 记录
     * @param versionNo 版本号
     * @param status 版本状态
     * @return 工作流版本
     */
    private WorkflowVersionRecord createWorkflowVersion(
            AgentRecord agent,
            int versionNo,
            WorkflowVersionStatus status
    ) {
        return workflowVersionRepository.insert(new WorkflowVersionRecord(
                0L,
                agent.id(),
                versionNo,
                status,
                List.of(),
                List.of(),
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of(),
                null,
                status == WorkflowVersionStatus.PUBLISHED ? Instant.now() : null,
                null,
                null
        ));
    }
}
