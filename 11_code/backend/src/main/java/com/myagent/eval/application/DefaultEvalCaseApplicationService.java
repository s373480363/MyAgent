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
     * 用例编号时间格式。
     */
    private static final DateTimeFormatter CASE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

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
     * NodeRun 仓储。
     */
    private final NodeRunRepository nodeRunRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * 正式用例校验服务。
     */
    private final EvalCaseFormalValidationService formalValidationService;

    /**
     * 构造默认验收用例应用服务。
     *
     * @param objectMapper JSON 对象映射器
     * @param evalSuiteRepository EvalSuite 仓储
     * @param evalCaseRepository EvalCase 仓储
     * @param agentRunRepository AgentRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param formalValidationService 正式用例校验服务
     */
    public DefaultEvalCaseApplicationService(
            ObjectMapper objectMapper,
            EvalSuiteRepository evalSuiteRepository,
            EvalCaseRepository evalCaseRepository,
            AgentRunRepository agentRunRepository,
            NodeRunRepository nodeRunRepository,
            WorkflowVersionRepository workflowVersionRepository,
            EvalCaseFormalValidationService formalValidationService
    ) {
        this.objectMapper = objectMapper;
        this.evalSuiteRepository = evalSuiteRepository;
        this.evalCaseRepository = evalCaseRepository;
        this.agentRunRepository = agentRunRepository;
        this.nodeRunRepository = nodeRunRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.formalValidationService = formalValidationService;
    }

    @Override
    public PageResult<EvalCaseResult> listCases(ListEvalCasesQuery query) {
        requiredSuite(query.suiteId());
        return evalCaseRepository.list(query).map(this::toCaseResult);
    }

    @Override
    @Transactional
    public EvalCaseResult createCase(CreateEvalCaseCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        requireSuiteNotArchived(suite);
        EvalCaseRecord record = new EvalCaseRecord(
                0L,
                suite.id(),
                requiredText(command.caseNo(), "验收用例编号不能为空。"),
                requiredText(command.title(), "验收用例标题不能为空。"),
                nullToObject(command.input()),
                command.referenceAnswer(),
                nullToArray(command.assertions()),
                nullToObject(command.scoreRule()),
                command.critical(),
                EvalCaseConfirmStatus.USER_CREATED,
                null,
                null,
                null,
                null,
                command.description() == null ? "" : command.description(),
                null,
                null
        );
        validateFormalEvalCase(suite, record);
        return toCaseResult(evalCaseRepository.insert(record));
    }

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
        EvalCaseRecord record = evalCaseRepository.insert(new EvalCaseRecord(
                0L,
                suite.id(),
                newCaseNo(),
                requiredText(command.title(), "验收用例标题不能为空。"),
                nullToObject(nodeRun.inputJson()),
                nodeRun.outputJson(),
                objectMapper.createArrayNode(),
                objectMapper.createObjectNode(),
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

    @Override
    public EvalCaseResult getCase(long suiteId, long caseId) {
        requiredSuite(suiteId);
        return toCaseResult(requiredCase(suiteId, caseId));
    }

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
                command.referenceAnswer(),
                nullToArray(command.assertions()),
                nullToObject(command.scoreRule()),
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
        if (isFormalCase(updated)) {
            validateFormalEvalCase(suite, updated);
        }
        return toCaseResult(updated);
    }

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

    @Override
    @Transactional
    public EvalCaseResult archiveCase(long suiteId, long caseId) {
        requiredSuite(suiteId);
        requiredCase(suiteId, caseId);
        return toCaseResult(evalCaseRepository.updateConfirmStatus(suiteId, caseId, EvalCaseConfirmStatus.ARCHIVED));
    }

    /**
     * 转换用例详情。
     *
     * @param record 用例记录
     * @return 用例详情
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
                record.referenceAnswerJson(),
                record.assertionsJson(),
                record.scoreRuleJson(),
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
     * 校验从 NodeRun 生成 EvalCase 时的来源一致性。
     *
     * @param suite 验收套件
     * @param nodeRun 来源 NodeRun
     * @param sourceRun 来源 AgentRun
     */
    private void validateNodeRunSourceForSuite(EvalSuiteRecord suite, NodeRunRecord nodeRun, AgentRunRecord sourceRun) {
        if (sourceRun.agentId() != suite.agentId()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 所属 Agent 与验收套件绑定的 Agent 不一致。");
        }
        if (sourceRun.workflowVersionId() != suite.workflowVersionId()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 所属工作流版本与验收套件绑定的工作流版本不一致。");
        }
        if (!suite.nodeId().equals(nodeRun.nodeId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 所属节点与验收套件目标节点不一致。");
        }
        if (nodeRun.status() != RunStatus.SUCCESS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "只有 SUCCESS 状态的 NodeRun 可以生成验收用例。");
        }
        if (nodeRun.outputJson() == null || nodeRun.outputJson().isNull() || nodeRun.outputJson().isMissingNode()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun 输出不能为空，无法复制为验收参考答案。");
        }
    }

    /**
     * 结合套件目标节点校验正式 EvalCase。
     *
     * @param suite EvalSuite 记录
     * @param record EvalCase 记录
     */
    private void validateFormalEvalCase(EvalSuiteRecord suite, EvalCaseRecord record) {
        formalValidationService.validateFormalEvalCase(record);
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(suite.workflowVersionId());
        formalValidationService.validateFormalEvalCase(record, requiredEvalNode(workflowVersion, suite.nodeId()));
    }

    /**
     * 判断用例是否属于正式运行候选。
     *
     * @param record EvalCase 记录
     * @return 属于正式候选时返回 true
     */
    private boolean isFormalCase(EvalCaseRecord record) {
        return record.confirmStatus() == EvalCaseConfirmStatus.USER_CREATED
                || record.confirmStatus() == EvalCaseConfirmStatus.USER_CONFIRMED;
    }

    /**
     * 查找用例并校验所属套件。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例记录
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
     * 查找套件。
     *
     * @param suiteId 套件主键
     * @return 套件记录
     */
    private EvalSuiteRecord requiredSuite(long suiteId) {
        return evalSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定验收套件不存在。"));
    }

    /**
     * 查找工作流版本。
     *
     * @param workflowVersionId 工作流版本主键
     * @return 工作流版本
     */
    private WorkflowVersionRecord requiredWorkflowVersion(long workflowVersionId) {
        return workflowVersionRepository.findById(workflowVersionId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工作流版本不存在。"));
    }

    /**
     * 要求套件未归档。
     *
     * @param suite 套件记录
     */
    private void requireSuiteNotArchived(EvalSuiteRecord suite) {
        if (suite.status() == EvalSuiteStatus.ARCHIVED) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "已归档验收套件不可修改。");
        }
    }

    /**
     * 查找并校验被验收节点。
     *
     * @param workflowVersion 工作流版本
     * @param nodeId 节点标识
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
     * 将空值转换为空对象。
     *
     * @param json 输入 JSON
     * @return JSON 对象
     */
    private JsonNode nullToObject(JsonNode json) {
        return json == null || json.isNull() ? objectMapper.createObjectNode() : json;
    }

    /**
     * 将空值转换为空数组。
     *
     * @param json 输入 JSON
     * @return JSON 数组
     */
    private JsonNode nullToArray(JsonNode json) {
        return json == null || json.isNull() ? objectMapper.createArrayNode() : json;
    }

    /**
     * 要求文本非空。
     *
     * @param value 文本
     * @param message 错误消息
     * @return 文本
     */
    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value;
    }

    /**
     * 生成用例编号。
     *
     * @return 用例编号
     */
    private String newCaseNo() {
        return "case_" + CASE_NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
