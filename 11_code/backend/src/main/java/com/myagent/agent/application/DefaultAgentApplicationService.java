package com.myagent.agent.application;

import com.myagent.agent.application.command.ChangeAgentStatusCommand;
import com.myagent.agent.application.command.CreateAgentCommand;
import com.myagent.agent.application.command.UpdateAgentCommand;
import com.myagent.agent.application.query.GetAgentDetailQuery;
import com.myagent.agent.application.query.ListAgentsQuery;
import com.myagent.agent.application.result.AgentDetailResult;
import com.myagent.agent.application.result.AgentListItemResult;
import com.myagent.agent.application.result.HistoryVersionSummaryResult;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.workflow.application.DefaultWorkflowApplicationService;
import com.myagent.workflow.application.result.WorkflowVersionSummaryResult;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionHistorySummaryRecord;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Agent 应用服务默认实现。
 */
@Service
public class DefaultAgentApplicationService implements AgentApplicationService {

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * 平台设置读取器。
     */
    private final PlatformSettingsResolver platformSettingsResolver;

    /**
     * 构造 Agent 应用服务。
     *
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param platformSettingsResolver 平台设置读取器
     */
    public DefaultAgentApplicationService(
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            PlatformSettingsResolver platformSettingsResolver
    ) {
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.platformSettingsResolver = platformSettingsResolver;
    }

    /**
     * 创建 Agent。
     *
     * @param command 创建命令
     * @return Agent 详情
     */
    @Override
    @Transactional
    public AgentDetailResult createAgent(CreateAgentCommand command) {
        validateCreateCommand(command);
        if (agentRepository.findByAgentKey(command.agentKey()).isPresent()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "agentKey 已存在，请使用新的 Agent 标识。");
        }

        AgentRecord inserted = agentRepository.insert(new AgentRecord(
                0L,
                command.agentKey().trim(),
                command.name().trim(),
                normalizedText(command.description()),
                EnableStatus.ENABLED,
                normalizedText(command.systemPrompt()),
                normalizedDefaultModel(command.defaultModel(), null),
                normalizedTemperature(command.temperature()),
                normalizedTimeoutSeconds(command.timeoutSeconds(), null),
                normalizedMaxSteps(command.maxSteps(), null),
                null,
                null,
                null,
                null
        ));

        WorkflowVersionRecord initialDraft = workflowVersionRepository.insert(new WorkflowVersionRecord(
                0L,
                inserted.id(),
                1,
                WorkflowVersionStatus.DRAFT,
                java.util.List.of(),
                java.util.List.of(),
                new WorkflowRuntimeOptions(
                        inserted.timeoutSeconds(),
                        inserted.maxSteps(),
                        platformSettingsResolver.resolveDefaultMaxAgentCallDepth()
                ),
                java.util.List.of(),
                null,
                null,
                null,
                null
        ));
        agentRepository.updateCurrentDraftWorkflowVersionId(inserted.id(), initialDraft.id());
        return getAgentDetail(new GetAgentDetailQuery(inserted.id()));
    }

    /**
     * 更新 Agent。
     *
     * @param command 更新命令
     * @return Agent 详情
     */
    @Override
    @Transactional
    public AgentDetailResult updateAgent(UpdateAgentCommand command) {
        AgentRecord existing = getRequiredAgent(command.agentId());
        validateUpdateCommand(command);
        AgentRecord updated = new AgentRecord(
                existing.id(),
                existing.agentKey(),
                command.name().trim(),
                normalizedText(command.description()),
                existing.status(),
                normalizedTextOrExisting(command.systemPrompt(), existing.systemPrompt()),
                normalizedDefaultModel(command.defaultModel(), existing.defaultModel()),
                normalizedTemperature(command.temperature()),
                normalizedTimeoutSeconds(command.timeoutSeconds(), existing.timeoutSeconds()),
                normalizedMaxSteps(command.maxSteps(), existing.maxSteps()),
                existing.currentDraftWorkflowVersionId(),
                existing.currentPublishedWorkflowVersionId(),
                existing.createdAt(),
                existing.updatedAt()
        );
        int affected = agentRepository.update(updated);
        if (affected == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Agent 不存在。");
        }
        return getAgentDetail(new GetAgentDetailQuery(existing.id()));
    }

    /**
     * 修改 Agent 状态。
     *
     * @param command 状态修改命令
     */
    @Override
    @Transactional
    public void changeAgentStatus(ChangeAgentStatusCommand command) {
        if (command.status() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "status 不能为空。");
        }
        getRequiredAgent(command.agentId());
        int affected = agentRepository.updateStatus(command.agentId(), command.status());
        if (affected == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Agent 不存在。");
        }
    }

    /**
     * 分页查询 Agent 列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<AgentListItemResult> listAgents(ListAgentsQuery query) {
        return agentRepository.listAgents(query).map(this::toListItemResult);
    }

    /**
     * 查询 Agent 详情。
     *
     * @param query 查询条件
     * @return Agent 详情
     */
    @Override
    public AgentDetailResult getAgentDetail(GetAgentDetailQuery query) {
        AgentRecord agent = getRequiredAgent(query.agentId());
        WorkflowVersionSummaryResult currentDraftWorkflow = agent.currentDraftWorkflowVersionId() == null
                ? null
                : DefaultWorkflowApplicationService.toSummaryResult(
                        workflowVersionRepository.findById(agent.currentDraftWorkflowVersionId()).orElse(null)
                );
        WorkflowVersionSummaryResult currentPublishedWorkflow = agent.currentPublishedWorkflowVersionId() == null
                ? null
                : DefaultWorkflowApplicationService.toSummaryResult(
                        workflowVersionRepository.findById(agent.currentPublishedWorkflowVersionId()).orElse(null)
                );
        WorkflowVersionHistorySummaryRecord historySummary = workflowVersionRepository.summarizeHistory(agent.id());
        return new AgentDetailResult(
                agent.id(),
                agent.agentKey(),
                agent.name(),
                agent.description(),
                agent.status(),
                agent.systemPrompt(),
                agent.defaultModel(),
                agent.temperature(),
                agent.timeoutSeconds(),
                agent.maxSteps(),
                currentDraftWorkflow,
                currentPublishedWorkflow,
                new HistoryVersionSummaryResult(
                        historySummary.total(),
                        historySummary.latestWorkflowVersionId(),
                        historySummary.latestVersionNo(),
                        historySummary.latestPublishedAt()
                ),
                agent.updatedAt()
        );
    }

    /**
     * 校验创建命令。
     *
     * @param command 创建命令
     */
    private void validateCreateCommand(CreateAgentCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "创建 Agent 请求不能为空。");
        }
        if (command.agentKey() == null || command.agentKey().isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "agentKey 不能为空。");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "name 不能为空。");
        }
        normalizedTemperature(command.temperature());
        normalizedTimeoutSeconds(command.timeoutSeconds(), null);
        normalizedMaxSteps(command.maxSteps(), null);
    }

    /**
     * 校验更新命令。
     *
     * @param command 更新命令
     */
    private void validateUpdateCommand(UpdateAgentCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "更新 Agent 请求不能为空。");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "name 不能为空。");
        }
        normalizedTemperature(command.temperature());
        normalizedTimeoutSeconds(command.timeoutSeconds(), null);
        normalizedMaxSteps(command.maxSteps(), null);
    }

    /**
     * 获取指定 Agent。
     *
     * @param agentId Agent 主键
     * @return Agent 记录
     */
    private AgentRecord getRequiredAgent(long agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new BizException(ErrorCode.AGENT_NOT_FOUND, "指定 Agent 不存在。"));
    }

    /**
     * 规范化默认模型。
     *
     * @param requestedValue 请求值
     * @param existingValue 现有值
     * @return 规范化结果
     */
    private String normalizedDefaultModel(String requestedValue, String existingValue) {
        if (requestedValue == null) {
            return existingValue == null ? platformSettingsResolver.resolveDefaultModel() : existingValue;
        }
        if (requestedValue.isBlank()) {
            return platformSettingsResolver.resolveDefaultModel();
        }
        return requestedValue.trim();
    }

    /**
     * 规范化温度。
     *
     * @param temperature 温度
     * @return 规范化结果
     */
    private BigDecimal normalizedTemperature(BigDecimal temperature) {
        if (temperature == null) {
            return null;
        }
        if (temperature.compareTo(BigDecimal.ZERO) < 0 || temperature.compareTo(new BigDecimal("2")) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "temperature 必须在 0 到 2 之间。");
        }
        return temperature;
    }

    /**
     * 规范化总超时。
     *
     * @param requestedValue 请求值
     * @param existingValue 现有值
     * @return 规范化结果
     */
    private int normalizedTimeoutSeconds(Integer requestedValue, Integer existingValue) {
        if (requestedValue == null) {
            if (existingValue != null) {
                return existingValue;
            }
            return platformSettingsResolver.resolveDefaultAgentTimeoutSeconds();
        }
        if (requestedValue <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "timeoutSeconds 必须大于 0。");
        }
        return requestedValue;
    }

    /**
     * 规范化最大步数。
     *
     * @param requestedValue 请求值
     * @param existingValue 现有值
     * @return 规范化结果
     */
    private int normalizedMaxSteps(Integer requestedValue, Integer existingValue) {
        if (requestedValue == null) {
            if (existingValue != null) {
                return existingValue;
            }
            return platformSettingsResolver.resolveDefaultMaxSteps();
        }
        if (requestedValue <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "maxSteps 必须大于 0。");
        }
        return requestedValue;
    }

    /**
     * 规范化普通文本。
     *
     * @param value 原始值
     * @return 规范化结果
     */
    private String normalizedText(String value) {
        return value == null ? "" : value;
    }

    /**
     * 在更新场景中规范化文本。
     *
     * @param requestedValue 请求值
     * @param existingValue 现有值
     * @return 规范化结果
     */
    private String normalizedTextOrExisting(String requestedValue, String existingValue) {
        return requestedValue == null ? existingValue : requestedValue;
    }

    /**
     * 转换为列表项。
     *
     * @param record Agent 记录
     * @return 列表项
     */
    private AgentListItemResult toListItemResult(AgentRecord record) {
        return new AgentListItemResult(
                record.id(),
                record.agentKey(),
                record.name(),
                record.description(),
                record.status(),
                record.currentDraftWorkflowVersionId(),
                record.currentPublishedWorkflowVersionId(),
                record.updatedAt()
        );
    }
}
