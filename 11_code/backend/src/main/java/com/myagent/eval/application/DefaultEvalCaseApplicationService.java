package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.eval.application.command.CreateEvalCaseCommand;
import com.myagent.eval.application.command.CreateEvalCaseFromNodeRunCommand;
import com.myagent.eval.application.command.UpdateEvalCaseCommand;
import com.myagent.eval.application.query.ListEvalCasesQuery;
import com.myagent.eval.application.result.EvalCaseResult;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.domain.EvalSuiteStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.eval.repository.EvalCaseRepository;
import com.myagent.eval.repository.EvalSuiteRecord;
import com.myagent.eval.repository.EvalSuiteRepository;
import com.myagent.modelcatalog.application.ModelRouteResolver;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 默认验收用例应用服务。
 */
@Service
public class DefaultEvalCaseApplicationService implements EvalCaseApplicationService {

    /**
     * 验收用例编号时间片格式器。
     */
    private static final DateTimeFormatter CASE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 验收套件仓储。
     */
    private final EvalSuiteRepository evalSuiteRepository;

    /**
     * 验收用例仓储。
     */
    private final EvalCaseRepository evalCaseRepository;

    /**
     * AgentRun 仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * NodeRun 仓储。
     */
    private final NodeRunRepository nodeRunRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * 正式验收用例校验服务。
     */
    private final EvalCaseFormalValidationService formalValidationService;

    /**
     * 模型供应项路由解析器。
     */
    private final ModelRouteResolver modelRouteResolver;

    /**
     * 构造验收用例应用服务。
     *
     * @param objectMapper JSON 对象映射器
     * @param evalSuiteRepository 验收套件仓储
     * @param evalCaseRepository 验收用例仓储
     * @param agentRunRepository AgentRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param formalValidationService 正式验收用例校验服务
     * @param modelRouteResolver 模型供应项路由解析器
     */
    public DefaultEvalCaseApplicationService(
            ObjectMapper objectMapper,
            EvalSuiteRepository evalSuiteRepository,
            EvalCaseRepository evalCaseRepository,
            AgentRunRepository agentRunRepository,
            NodeRunRepository nodeRunRepository,
            WorkflowVersionRepository workflowVersionRepository,
            EvalCaseFormalValidationService formalValidationService,
            ModelRouteResolver modelRouteResolver
    ) {
        this.objectMapper = objectMapper;
        this.evalSuiteRepository = evalSuiteRepository;
        this.evalCaseRepository = evalCaseRepository;
        this.agentRunRepository = agentRunRepository;
        this.nodeRunRepository = nodeRunRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.formalValidationService = formalValidationService;
        this.modelRouteResolver = modelRouteResolver;
    }

    /**
     * 查询验收用例列表。
     *
     * @param query 分页查询条件
     * @return 验收用例分页结果
     */
    @Override
    public PageResult<EvalCaseResult> listCases(ListEvalCasesQuery query) {
        requiredSuite(query.suiteId());
        return evalCaseRepository.list(query).map(this::toCaseResult);
    }

    /**
     * 手工创建验收用例。
     *
     * @param command 创建命令
     * @return 创建后的验收用例
     */
    @Override
    @Transactional
    public EvalCaseResult createCase(CreateEvalCaseCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        requireSuiteNotArchived(suite);
        // 手工创建阶段只做基础结构保存，不在这里触发正式验收校验。
        EvalCaseRecord record = evalCaseRepository.insert(new EvalCaseRecord(
                0L,
                suite.id(),
                requiredText(command.caseNo(), "验收用例编号不能为空。"),
                requiredText(command.title(), "验收用例标题不能为空。"),
                nullToObject(command.input()),
                command.referenceSample(),
                normalizeJudgeRule(command.judgeRule()),
                nullToArray(command.hardChecks()),
                command.critical(),
                EvalCaseConfirmStatus.USER_CREATED,
                null,
                null,
                null,
                null,
                command.description() == null ? "" : command.description(),
                null,
                null
        ));
        return toCaseResult(record);
    }

    /**
     * 从 NodeRun 复制生成验收用例。
     *
     * @param command 创建命令
     * @return 创建后的验收用例
     */
    @Override
    @Transactional
    public EvalCaseResult createCaseFromNodeRun(CreateEvalCaseFromNodeRunCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        requireSuiteNotArchived(suite);
        NodeRunRecord nodeRun = nodeRunRepository.findById(command.nodeRunId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 NodeRun 不存在。"));
        AgentRunRecord sourceRun = agentRunRepository.findById(nodeRun.runId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "NodeRun 关联的运行不存在。"));
        validateNodeRunSourceForSuite(suite, nodeRun, sourceRun);
        // NodeRun 转用例时直接复制节点输入/输出，并落为 AI_DRAFT_PENDING 草稿。
        EvalCaseRecord record = evalCaseRepository.insert(new EvalCaseRecord(
                0L,
                suite.id(),
                newCaseNo(),
                requiredText(command.title(), "验收用例标题不能为空。"),
                nullToObject(nodeRun.inputJson()),
                nodeRun.outputJson(),
                "",
                objectMapper.createArrayNode(),
                false,
                EvalCaseConfirmStatus.AI_DRAFT_PENDING,
                sourceRun.id(),
                nodeRun.id(),
                sourceRun.workflowVersionId(),
                nodeRun.nodeId(),
                command.description() == null ? "" : command.description(),
                null,
                null
        ));
        return toCaseResult(record);
    }

    /**
     * 查询单个验收用例详情。
     *
     * @param suiteId 验收套件主键
     * @param caseId 验收用例主键
     * @return 验收用例详情
     */
    @Override
    public EvalCaseResult getCase(long suiteId, long caseId) {
        requiredSuite(suiteId);
        return toCaseResult(requiredCase(suiteId, caseId));
    }

    /**
     * 更新验收用例。
     *
     * @param command 更新命令
     * @return 更新后的验收用例
     */
    @Override
    @Transactional
    public EvalCaseResult updateCase(UpdateEvalCaseCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        requireSuiteNotArchived(suite);
        EvalCaseRecord current = requiredCase(command.suiteId(), command.caseId());
        if (current.confirmStatus() == EvalCaseConfirmStatus.ARCHIVED) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "已归档验收用例不可更新。");
        }
        EvalCaseRecord updated = evalCaseRepository.update(new EvalCaseRecord(
                command.caseId(),
                command.suiteId(),
                current.caseNo(),
                requiredText(command.title(), "验收用例标题不能为空。"),
                nullToObject(command.input()),
                command.referenceSample(),
                normalizeJudgeRule(command.judgeRule()),
                nullToArray(command.hardChecks()),
                command.critical(),
                current.confirmStatus(),
                current.sourceAgentRunId(),
                current.sourceNodeRunId(),
                current.sourceWorkflowVersionId(),
                current.sourceNodeId(),
                command.description() == null ? "" : command.description(),
                current.createdAt(),
                current.updatedAt()
        ));
        // 只有正式状态用例更新后才需要立即重新校验契约。
        if (isFormalCase(updated)) {
            validateFormalEvalCase(suite, updated);
        }
        return toCaseResult(updated);
    }

    /**
     * 确认验收用例，使其进入可运行正式状态。
     *
     * @param suiteId 验收套件主键
     * @param caseId 验收用例主键
     * @return 确认后的验收用例
     */
    @Override
    @Transactional
    public EvalCaseResult confirmCase(long suiteId, long caseId) {
        EvalSuiteRecord suite = requiredSuite(suiteId);
        EvalCaseRecord current = requiredCase(suiteId, caseId);
        if (current.confirmStatus() == EvalCaseConfirmStatus.ARCHIVED) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "已归档验收用例不可确认。");
        }
        validateFormalEvalCase(suite, current);
        return toCaseResult(evalCaseRepository.updateConfirmStatus(suiteId, caseId, EvalCaseConfirmStatus.USER_CONFIRMED));
    }

    /**
     * 归档验收用例。
     *
     * @param suiteId 验收套件主键
     * @param caseId 验收用例主键
     * @return 归档后的验收用例
     */
    @Override
    @Transactional
    public EvalCaseResult archiveCase(long suiteId, long caseId) {
        requiredSuite(suiteId);
        requiredCase(suiteId, caseId);
        return toCaseResult(evalCaseRepository.updateConfirmStatus(suiteId, caseId, EvalCaseConfirmStatus.ARCHIVED));
    }

    /**
     * 转换验收用例结果对象。
     *
     * @param record 验收用例记录
     * @return 返回结果
     */
    private EvalCaseResult toCaseResult(EvalCaseRecord record) {
        String sourceRunId = record.sourceAgentRunId() == null
                ? null
                : agentRunRepository.findById(record.sourceAgentRunId()).map(AgentRunRecord::runNo).orElse(null);
        return new EvalCaseResult(
                record.id(),
                record.suiteId(),
                record.caseNo(),
                record.title(),
                record.inputJson(),
                record.referenceSampleJson(),
                record.judgeRuleText(),
                record.hardChecksJson(),
                record.critical(),
                record.confirmStatus(),
                sourceRunId,
                record.sourceNodeRunId(),
                record.sourceWorkflowVersionId(),
                record.sourceNodeId(),
                record.description(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 校验 NodeRun 是否可以复制到当前验收套件。
     *
     * @param suite 验收套件
     * @param nodeRun NodeRun 记录
     * @param sourceRun AgentRun 记录
     */
    private void validateNodeRunSourceForSuite(EvalSuiteRecord suite, NodeRunRecord nodeRun, AgentRunRecord sourceRun) {
        if (sourceRun.agentId() != suite.agentId()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 所属 Agent 与验收套件绑定的 Agent 不一致。");
        }
        if (sourceRun.workflowVersionId() != suite.workflowVersionId()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "NodeRun 所属工作流版本与验收套件绑定的工作流版本不一致。"
            );
        }
        if (!suite.nodeId().equals(nodeRun.nodeId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 所属节点与验收套件目标节点不一致。");
        }
        if (nodeRun.status() != RunStatus.SUCCESS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "只有 SUCCESS 状态的 NodeRun 可以生成验收用例。");
        }
        if (nodeRun.outputJson() == null || nodeRun.outputJson().isNull() || nodeRun.outputJson().isMissingNode()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 输出不能为空，无法复制为参考样例。");
        }
    }

    /**
     * 执行正式验收用例校验。
     *
     * @param suite 验收套件
     * @param record 验收用例记录
     */
    private void validateFormalEvalCase(EvalSuiteRecord suite, EvalCaseRecord record) {
        validateSuiteJudgeConfiguration(suite);
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(suite.workflowVersionId());
        WorkflowNodeDefinition node = requiredEvalNode(workflowVersion, suite.nodeId());
        formalValidationService.validateFormalEvalCase(record, node);
    }

    /**
     * 判断验收用例是否已经处于正式状态。
     *
     * @param record 验收用例记录
     * @return 是否正式状态
     */
    private boolean isFormalCase(EvalCaseRecord record) {
        return record.confirmStatus() == EvalCaseConfirmStatus.USER_CONFIRMED;
    }

    /**
     * 校验验收套件级别的 judge 配置。
     *
     * @param suite 验收套件
     */
    private void validateSuiteJudgeConfiguration(EvalSuiteRecord suite) {
        String judgeModelOfferingKey = requiredText(
                suite.judgeModelOfferingKey(),
                "确认验收用例前必须先为验收套件配置 judge 模型供应项。"
        );
        modelRouteResolver.validatePublishable(judgeModelOfferingKey);
    }

    /**
     * 查询并校验验收用例归属。
     *
     * @param suiteId 验收套件主键
     * @param caseId 验收用例主键
     * @return 验收用例记录
     */
    private EvalCaseRecord requiredCase(long suiteId, long caseId) {
        EvalCaseRecord record = evalCaseRepository.findById(caseId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定验收用例不存在。"));
        if (record.suiteId() != suiteId) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定验收用例不属于当前套件。");
        }
        return record;
    }

    /**
     * 查询验收套件。
     *
     * @param suiteId 验收套件主键
     * @return 验收套件记录
     */
    private EvalSuiteRecord requiredSuite(long suiteId) {
        return evalSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定验收套件不存在。"));
    }

    /**
     * 查询工作流版本。
     *
     * @param workflowVersionId 工作流版本主键
     * @return 工作流版本记录
     */
    private WorkflowVersionRecord requiredWorkflowVersion(long workflowVersionId) {
        return workflowVersionRepository.findById(workflowVersionId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工作流版本不存在。"));
    }

    /**
     * 要求验收套件未归档。
     *
     * @param suite 验收套件
     */
    private void requireSuiteNotArchived(EvalSuiteRecord suite) {
        if (suite.status() == EvalSuiteStatus.ARCHIVED) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "已归档验收套件不可修改。");
        }
    }

    /**
     * 查询并校验当前验收目标节点。
     *
     * @param workflowVersion 工作流版本
     * @param nodeId 节点编号
     * @return 节点定义
     */
    private WorkflowNodeDefinition requiredEvalNode(WorkflowVersionRecord workflowVersion, String nodeId) {
        WorkflowNodeDefinition node = workflowVersion.nodes().stream()
                .filter(candidate -> nodeId.equals(candidate.getNodeId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "工作流版本中不存在指定节点。"));
        if (node.getType() != WorkflowNodeType.LLM
                && node.getType() != WorkflowNodeType.REVIEW
                && node.getType() != WorkflowNodeType.SUMMARY) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "节点验收仅支持 LLM、REVIEW 和 SUMMARY 节点。");
        }
        return node;
    }

    /**
     * 将空输入规范化为空对象。
     *
     * @param json 原始 JSON
     * @return 非空对象节点
     */
    private JsonNode nullToObject(JsonNode json) {
        return json == null || json.isNull() ? objectMapper.createObjectNode() : json;
    }

    /**
     * 将空 hardChecks 规范化为空数组。
     *
     * @param json 原始 JSON
     * @return 非空数组节点
     */
    private JsonNode nullToArray(JsonNode json) {
        return json == null || json.isNull() ? objectMapper.createArrayNode() : json;
    }

    /**
     * 规范化 judgeRule。
     *
     * @param judgeRule 原始 judgeRule
     * @return 非 null 的 judgeRule
     */
    private String normalizeJudgeRule(String judgeRule) {
        return judgeRule == null ? "" : judgeRule;
    }

    /**
     * 校验必填文本。
     *
     * @param value 原始文本
     * @param message 失败消息
     * @return 非空文本
     */
    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value;
    }

    /**
     * 生成新的验收用例编号。
     *
     * @return 验收用例编号
     */
    private String newCaseNo() {
        return "case_" + CASE_NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
