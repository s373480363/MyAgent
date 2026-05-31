package com.myagent.workflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.schema.application.SchemaApplicationService;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.workflow.application.command.CopyWorkflowDraftFromVersionCommand;
import com.myagent.workflow.application.command.PublishWorkflowDraftCommand;
import com.myagent.workflow.application.command.SaveWorkflowDraftCommand;
import com.myagent.workflow.application.command.ValidateWorkflowDraftCommand;
import com.myagent.workflow.application.query.GetWorkflowDraftQuery;
import com.myagent.workflow.application.query.GetWorkflowVersionQuery;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.application.result.WorkflowDraftResult;
import com.myagent.workflow.application.result.WorkflowPublishResult;
import com.myagent.workflow.application.result.WorkflowValidationIssueResult;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import com.myagent.workflow.application.result.WorkflowVersionListItemResult;
import com.myagent.workflow.application.result.WorkflowVersionResult;
import com.myagent.workflow.application.result.WorkflowVersionSummaryResult;
import com.myagent.workflow.domain.ReferencedSchemaVersion;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowSchemaRef;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import com.myagent.workflow.validation.WorkflowDraftValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作流应用服务默认实现。
 */
@Service
public class DefaultWorkflowApplicationService implements WorkflowApplicationService {

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * Schema 仓储。
     */
    private final SchemaRepository schemaRepository;

    /**
     * Schema 应用服务。
     */
    private final SchemaApplicationService schemaApplicationService;

    /**
     * 平台设置读取器。
     */
    private final PlatformSettingsResolver platformSettingsResolver;

    /**
     * 工作流校验服务。
     */
    private final WorkflowDraftValidationService workflowDraftValidationService;

    /**
     * 构造工作流应用服务。
     *
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param schemaRepository Schema 仓储
     * @param schemaApplicationService Schema 应用服务
     * @param platformSettingsResolver 平台设置读取器
     * @param workflowDraftValidationService 工作流校验服务
     */
    public DefaultWorkflowApplicationService(
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            SchemaRepository schemaRepository,
            SchemaApplicationService schemaApplicationService,
            PlatformSettingsResolver platformSettingsResolver,
            WorkflowDraftValidationService workflowDraftValidationService
    ) {
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.schemaRepository = schemaRepository;
        this.schemaApplicationService = schemaApplicationService;
        this.platformSettingsResolver = platformSettingsResolver;
        this.workflowDraftValidationService = workflowDraftValidationService;
    }

    /**
     * 获取当前草稿。
     *
     * @param query 查询条件
     * @return 当前草稿
     */
    @Override
    public WorkflowDraftResult getWorkflowDraft(GetWorkflowDraftQuery query) {
        AgentRecord agent = getRequiredAgent(query.agentId());
        WorkflowVersionRecord draft = workflowVersionRepository.findCurrentDraft(agent.id())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "当前 Agent 暂无草稿版本。"));
        return toDraftResult(draft);
    }

    /**
     * 保存当前草稿。
     *
     * @param command 保存命令
     * @return 新草稿
     */
    @Override
    @Transactional
    public WorkflowDraftResult saveWorkflowDraft(SaveWorkflowDraftCommand command) {
        AgentRecord agent = getRequiredAgent(command.agentId());
        WorkflowRuntimeOptions runtimeOptions = normalizeRuntimeOptions(command.runtimeOptions(), agent);
        List<WorkflowNodeDefinition> nodes = immutableNodes(command.nodes());
        List<WorkflowEdgeDefinition> edges = immutableEdges(command.edges());
        List<ReferencedSchemaVersion> referencedSchemaVersions = deriveReferencedSchemaVersions(nodes);

        WorkflowVersionRecord currentDraft = workflowVersionRepository.findCurrentDraft(agent.id()).orElse(null);
        if (currentDraft != null) {
            workflowVersionRepository.updateStatus(currentDraft.id(), WorkflowVersionStatus.HISTORY);
        }

        int nextVersionNo = workflowVersionRepository.findMaxVersionNo(agent.id()) + 1;
        WorkflowVersionRecord inserted = workflowVersionRepository.insert(new WorkflowVersionRecord(
                0L,
                agent.id(),
                nextVersionNo,
                WorkflowVersionStatus.DRAFT,
                nodes,
                edges,
                runtimeOptions,
                referencedSchemaVersions,
                currentDraft == null ? null : currentDraft.id(),
                null,
                null,
                null
        ));
        agentRepository.updateCurrentDraftWorkflowVersionId(agent.id(), inserted.id());
        return toDraftResult(inserted);
    }

    /**
     * 从已有版本复制生成新草稿。
     *
     * @param command 复制命令
     * @return 新草稿
     */
    @Override
    @Transactional
    public WorkflowDraftResult copyWorkflowDraftFromVersion(CopyWorkflowDraftFromVersionCommand command) {
        AgentRecord agent = getRequiredAgent(command.agentId());
        WorkflowVersionRecord source = getRequiredWorkflowVersion(agent.id(), command.sourceWorkflowVersionId());
        WorkflowVersionRecord currentDraft = workflowVersionRepository.findCurrentDraft(agent.id()).orElse(null);
        if (currentDraft != null) {
            workflowVersionRepository.updateStatus(currentDraft.id(), WorkflowVersionStatus.HISTORY);
        }
        int nextVersionNo = workflowVersionRepository.findMaxVersionNo(agent.id()) + 1;
        WorkflowVersionRecord inserted = workflowVersionRepository.insert(new WorkflowVersionRecord(
                0L,
                agent.id(),
                nextVersionNo,
                WorkflowVersionStatus.DRAFT,
                immutableNodes(source.nodes()),
                immutableEdges(source.edges()),
                source.runtimeOptions(),
                source.referencedSchemaVersions(),
                source.id(),
                null,
                null,
                null
        ));
        agentRepository.updateCurrentDraftWorkflowVersionId(agent.id(), inserted.id());
        return toDraftResult(inserted);
    }

    /**
     * 校验当前草稿。
     *
     * @param command 校验命令
     * @return 校验结果
     */
    @Override
    public WorkflowValidationResult validateWorkflowDraft(ValidateWorkflowDraftCommand command) {
        AgentRecord agent = getRequiredAgent(command.agentId());
        WorkflowVersionRecord draft = workflowVersionRepository.findCurrentDraft(agent.id())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "当前 Agent 暂无草稿版本。"));
        return workflowDraftValidationService.validate(agent, draft);
    }

    /**
     * 发布当前草稿。
     *
     * @param command 发布命令
     * @return 发布结果
     */
    @Override
    @Transactional
    public WorkflowPublishResult publishWorkflowDraft(PublishWorkflowDraftCommand command) {
        AgentRecord agent = getRequiredAgent(command.agentId());
        WorkflowVersionRecord draft = workflowVersionRepository.findCurrentDraft(agent.id())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "当前 Agent 暂无草稿版本。"));
        WorkflowValidationResult validationResult = workflowDraftValidationService.validate(agent, draft);
        ensureValid(validationResult);

        WorkflowVersionRecord currentPublished = workflowVersionRepository.findCurrentPublished(agent.id()).orElse(null);
        if (currentPublished != null) {
            workflowVersionRepository.updateStatus(currentPublished.id(), WorkflowVersionStatus.HISTORY);
        }

        int nextVersionNo = workflowVersionRepository.findMaxVersionNo(agent.id()) + 1;
        Instant publishedAt = Instant.now();
        WorkflowVersionRecord published = workflowVersionRepository.insert(new WorkflowVersionRecord(
                0L,
                agent.id(),
                nextVersionNo,
                WorkflowVersionStatus.PUBLISHED,
                immutableNodes(draft.nodes()),
                immutableEdges(draft.edges()),
                draft.runtimeOptions(),
                draft.referencedSchemaVersions(),
                draft.id(),
                publishedAt,
                null,
                null
        ));
        agentRepository.updateCurrentPublishedWorkflowVersionId(agent.id(), published.id());
        for (ReferencedSchemaVersion referencedSchemaVersion : published.referencedSchemaVersions()) {
            schemaApplicationService.lockSchemaVersion(referencedSchemaVersion.getSchemaId());
        }
        return toPublishResult(published);
    }

    /**
     * 分页查询工作流版本。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<WorkflowVersionListItemResult> listWorkflowVersions(ListWorkflowVersionsQuery query) {
        getRequiredAgent(query.agentId());
        return workflowVersionRepository.listWorkflowVersions(query).map(this::toListItemResult);
    }

    /**
     * 查询工作流版本详情。
     *
     * @param query 查询条件
     * @return 版本详情
     */
    @Override
    public WorkflowVersionResult getWorkflowVersion(GetWorkflowVersionQuery query) {
        getRequiredAgent(query.agentId());
        return toVersionResult(getRequiredWorkflowVersion(query.agentId(), query.workflowVersionId()));
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
     * 获取属于指定 Agent 的工作流版本。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 工作流版本
     */
    private WorkflowVersionRecord getRequiredWorkflowVersion(long agentId, long workflowVersionId) {
        WorkflowVersionRecord workflowVersion = workflowVersionRepository.findById(workflowVersionId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工作流版本不存在。"));
        if (workflowVersion.agentId() != agentId) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工作流版本不属于当前 Agent。");
        }
        return workflowVersion;
    }

    /**
     * 归一化运行约束。
     *
     * @param runtimeOptionsNode 原始运行约束对象
     * @param agent Agent 主数据
     * @return 完整运行约束
     */
    private WorkflowRuntimeOptions normalizeRuntimeOptions(JsonNode runtimeOptionsNode, AgentRecord agent) {
        JsonNode objectNode = runtimeOptionsNode;
        if (objectNode == null || objectNode.isNull()) {
            objectNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        }
        if (!objectNode.isObject()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "runtimeOptions 必须是 JSON 对象。");
        }

        Set<String> allowedFields = Set.of("timeoutSeconds", "maxSteps", "maxAgentCallDepth");
        objectNode.fieldNames().forEachRemaining(fieldName -> {
            if (!allowedFields.contains(fieldName)) {
                throw new BizException(
                        ErrorCode.INVALID_ARGUMENT,
                        "runtimeOptions 存在未定义字段。",
                        List.of(new ApiError.Detail("$.runtimeOptions." + fieldName, "not_allowed", "runtimeOptions 不允许出现未定义字段。", null, fieldName, null))
                );
            }
        });

        int timeoutSeconds = readPositiveInteger(objectNode, "timeoutSeconds", agent.timeoutSeconds());
        int maxSteps = readPositiveInteger(objectNode, "maxSteps", agent.maxSteps());
        int maxAgentCallDepth = readPositiveInteger(
                objectNode,
                "maxAgentCallDepth",
                platformSettingsResolver.resolveDefaultMaxAgentCallDepth()
        );
        return new WorkflowRuntimeOptions(timeoutSeconds, maxSteps, maxAgentCallDepth);
    }

    /**
     * 读取正整数配置。
     *
     * @param objectNode JSON 对象
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 解析后的正整数
     */
    private int readPositiveInteger(JsonNode objectNode, String fieldName, int defaultValue) {
        JsonNode valueNode = objectNode.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        if (!valueNode.canConvertToInt()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    fieldName + " 必须是正整数。",
                    List.of(new ApiError.Detail("$.runtimeOptions." + fieldName, "invalid_type", fieldName + " 必须是正整数。", null, valueNode.toString(), null))
            );
        }
        int value = valueNode.intValue();
        if (value <= 0) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    fieldName + " 必须大于 0。",
                    List.of(new ApiError.Detail("$.runtimeOptions." + fieldName, "invalid_value", fieldName + " 必须大于 0。", null, String.valueOf(value), null))
            );
        }
        return value;
    }

    /**
     * 根据节点定义派生 Schema 引用快照。
     *
     * @param nodes 节点定义
     * @return Schema 引用快照
     */
    private List<ReferencedSchemaVersion> deriveReferencedSchemaVersions(List<WorkflowNodeDefinition> nodes) {
        Map<String, ReferencedSchemaVersion> references = new LinkedHashMap<>();
        for (WorkflowNodeDefinition node : nodes) {
            if (node == null) {
                continue;
            }
            collectSchemaReference(node.getInputSchemaRef(), references);
            collectSchemaReference(node.getOutputSchemaRef(), references);
        }
        return references.values().stream()
                .sorted(Comparator.comparing(ReferencedSchemaVersion::getSchemaKey).thenComparingInt(ReferencedSchemaVersion::getVersion))
                .toList();
    }

    /**
     * 收集单个 Schema 引用。
     *
     * @param schemaRef Schema 引用
     * @param references 引用聚合映射
     */
    private void collectSchemaReference(WorkflowSchemaRef schemaRef, Map<String, ReferencedSchemaVersion> references) {
        if (schemaRef == null) {
            return;
        }
        if (schemaRef.getSchemaKey() == null || schemaRef.getSchemaKey().isBlank() || schemaRef.getVersion() == null || schemaRef.getVersion() <= 0) {
            throw new BizException(ErrorCode.WORKFLOW_VALIDATION_FAILED, "节点上的 Schema 引用格式不正确。");
        }
        SchemaRecord schemaRecord = schemaRepository.findByKeyAndVersion(schemaRef.getSchemaKey(), schemaRef.getVersion())
                .orElseThrow(() -> new BizException(
                        ErrorCode.WORKFLOW_VALIDATION_FAILED,
                        "节点引用的 Schema 版本不存在。",
                        List.of(new ApiError.Detail(
                                "$.nodes[*].schemaRef",
                                "not_found",
                                "节点引用的 Schema 版本不存在。",
                                null,
                                schemaRef.getSchemaKey() + ":" + schemaRef.getVersion(),
                                null
                        ))
                ));
        String uniqueKey = schemaRecord.getSchemaKey() + "#" + schemaRecord.getVersion();
        references.putIfAbsent(uniqueKey, new ReferencedSchemaVersion(
                schemaRecord.getId(),
                schemaRecord.getSchemaKey(),
                schemaRecord.getVersion()
        ));
    }

    /**
     * 确保校验通过。
     *
     * @param validationResult 校验结果
     */
    private void ensureValid(WorkflowValidationResult validationResult) {
        if (validationResult.isValid()) {
            return;
        }
        List<ApiError.Detail> details = new ArrayList<>();
        for (WorkflowValidationIssueResult error : validationResult.getErrors()) {
            details.addAll(error.getDetails());
        }
        throw new BizException(
                ErrorCode.WORKFLOW_VALIDATION_FAILED,
                "工作流校验失败，请修正后重试。",
                details
        );
    }

    /**
     * 转换为草稿结果。
     *
     * @param record 工作流版本记录
     * @return 草稿结果
     */
    private WorkflowDraftResult toDraftResult(WorkflowVersionRecord record) {
        return new WorkflowDraftResult(
                record.id(),
                record.agentId(),
                record.versionNo(),
                record.status(),
                immutableNodes(record.nodes()),
                immutableEdges(record.edges()),
                record.runtimeOptions(),
                record.referencedSchemaVersions() == null ? List.of() : List.copyOf(record.referencedSchemaVersions()),
                record.sourceWorkflowVersionId(),
                record.publishedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 转换为版本详情结果。
     *
     * @param record 工作流版本记录
     * @return 版本详情结果
     */
    private WorkflowVersionResult toVersionResult(WorkflowVersionRecord record) {
        return new WorkflowVersionResult(
                record.id(),
                record.agentId(),
                record.versionNo(),
                record.status(),
                immutableNodes(record.nodes()),
                immutableEdges(record.edges()),
                record.runtimeOptions(),
                record.referencedSchemaVersions() == null ? List.of() : List.copyOf(record.referencedSchemaVersions()),
                record.sourceWorkflowVersionId(),
                record.publishedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 转换为列表项。
     *
     * @param record 工作流版本记录
     * @return 列表项
     */
    private WorkflowVersionListItemResult toListItemResult(WorkflowVersionRecord record) {
        return new WorkflowVersionListItemResult(
                record.id(),
                record.versionNo(),
                record.status(),
                record.sourceWorkflowVersionId(),
                record.publishedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 转换为发布结果。
     *
     * @param record 工作流版本记录
     * @return 发布结果
     */
    private WorkflowPublishResult toPublishResult(WorkflowVersionRecord record) {
        return new WorkflowPublishResult(
                record.id(),
                record.agentId(),
                record.versionNo(),
                record.status(),
                record.sourceWorkflowVersionId(),
                record.publishedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 转换为不可变节点列表。
     *
     * @param nodes 节点列表
     * @return 不可变节点列表
     */
    private List<WorkflowNodeDefinition> immutableNodes(List<WorkflowNodeDefinition> nodes) {
        return nodes == null ? List.of() : List.copyOf(nodes);
    }

    /**
     * 转换为不可变边列表。
     *
     * @param edges 边列表
     * @return 不可变边列表
     */
    private List<WorkflowEdgeDefinition> immutableEdges(List<WorkflowEdgeDefinition> edges) {
        return edges == null ? List.of() : List.copyOf(edges);
    }

    /**
     * 供 Agent 详情聚合使用的版本摘要转换。
     *
     * @param record 工作流版本记录
     * @return 版本摘要
     */
    public static WorkflowVersionSummaryResult toSummaryResult(WorkflowVersionRecord record) {
        if (record == null) {
            return null;
        }
        return new WorkflowVersionSummaryResult(
                record.id(),
                record.versionNo(),
                record.status(),
                record.sourceWorkflowVersionId(),
                record.publishedAt(),
                record.updatedAt(),
                record.createdAt()
        );
    }
}
