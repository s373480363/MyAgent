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
 * Eval case application service.
 */
@Service
public class DefaultEvalCaseApplicationService implements EvalCaseApplicationService {

    private static final DateTimeFormatter CASE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final ObjectMapper objectMapper;
    private final EvalSuiteRepository evalSuiteRepository;
    private final EvalCaseRepository evalCaseRepository;
    private final AgentRunRepository agentRunRepository;
    private final NodeRunRepository nodeRunRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final EvalCaseFormalValidationService formalValidationService;
    private final ModelRouteResolver modelRouteResolver;

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
        EvalCaseRecord record = evalCaseRepository.insert(new EvalCaseRecord(
                0L,
                suite.id(),
                requiredText(command.caseNo(), "\u9a8c\u6536\u7528\u4f8b\u7f16\u53f7\u4e0d\u80fd\u4e3a\u7a7a\u3002"),
                requiredText(command.title(), "\u9a8c\u6536\u7528\u4f8b\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a\u3002"),
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

    @Override
    @Transactional
    public EvalCaseResult createCaseFromNodeRun(CreateEvalCaseFromNodeRunCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        requireSuiteNotArchived(suite);
        NodeRunRecord nodeRun = nodeRunRepository.findById(command.nodeRunId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "\u6307\u5b9a NodeRun \u4e0d\u5b58\u5728\u3002"));
        AgentRunRecord sourceRun = agentRunRepository.findById(nodeRun.runId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "NodeRun \u5173\u8054\u7684\u8fd0\u884c\u4e0d\u5b58\u5728\u3002"));
        validateNodeRunSourceForSuite(suite, nodeRun, sourceRun);
        EvalCaseRecord record = evalCaseRepository.insert(new EvalCaseRecord(
                0L,
                suite.id(),
                newCaseNo(),
                requiredText(command.title(), "\u9a8c\u6536\u7528\u4f8b\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a\u3002"),
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
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "\u5df2\u5f52\u6863\u9a8c\u6536\u7528\u4f8b\u4e0d\u53ef\u66f4\u65b0\u3002");
        }
        EvalCaseRecord updated = evalCaseRepository.update(new EvalCaseRecord(
                command.caseId(),
                command.suiteId(),
                current.caseNo(),
                requiredText(command.title(), "\u9a8c\u6536\u7528\u4f8b\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a\u3002"),
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
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "\u5df2\u5f52\u6863\u9a8c\u6536\u7528\u4f8b\u4e0d\u53ef\u786e\u8ba4\u3002");
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

    private void validateNodeRunSourceForSuite(EvalSuiteRecord suite, NodeRunRecord nodeRun, AgentRunRecord sourceRun) {
        if (sourceRun.agentId() != suite.agentId()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun \u6240\u5c5e Agent \u4e0e\u9a8c\u6536\u5957\u4ef6\u7ed1\u5b9a\u7684 Agent \u4e0d\u4e00\u81f4\u3002");
        }
        if (sourceRun.workflowVersionId() != suite.workflowVersionId()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "NodeRun \u6240\u5c5e\u5de5\u4f5c\u6d41\u7248\u672c\u4e0e\u9a8c\u6536\u5957\u4ef6\u7ed1\u5b9a\u7684\u5de5\u4f5c\u6d41\u7248\u672c\u4e0d\u4e00\u81f4\u3002"
            );
        }
        if (!suite.nodeId().equals(nodeRun.nodeId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun \u6240\u5c5e\u8282\u70b9\u4e0e\u9a8c\u6536\u5957\u4ef6\u76ee\u6807\u8282\u70b9\u4e0d\u4e00\u81f4\u3002");
        }
        if (nodeRun.status() != RunStatus.SUCCESS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "\u53ea\u6709 SUCCESS \u72b6\u6001\u7684 NodeRun \u53ef\u4ee5\u751f\u6210\u9a8c\u6536\u7528\u4f8b\u3002");
        }
        if (nodeRun.outputJson() == null || nodeRun.outputJson().isNull() || nodeRun.outputJson().isMissingNode()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "NodeRun \u8f93\u51fa\u4e0d\u80fd\u4e3a\u7a7a\uff0c\u65e0\u6cd5\u590d\u5236\u4e3a\u53c2\u8003\u6837\u4f8b\u3002");
        }
    }

    private void validateFormalEvalCase(EvalSuiteRecord suite, EvalCaseRecord record) {
        validateSuiteJudgeConfiguration(suite);
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(suite.workflowVersionId());
        WorkflowNodeDefinition node = requiredEvalNode(workflowVersion, suite.nodeId());
        formalValidationService.validateFormalEvalCase(record, node);
    }

    private boolean isFormalCase(EvalCaseRecord record) {
        return record.confirmStatus() == EvalCaseConfirmStatus.USER_CONFIRMED;
    }

    private void validateSuiteJudgeConfiguration(EvalSuiteRecord suite) {
        String judgeModelOfferingKey = requiredText(
                suite.judgeModelOfferingKey(),
                "\u786e\u8ba4\u9a8c\u6536\u7528\u4f8b\u524d\u5fc5\u987b\u5148\u4e3a\u9a8c\u6536\u5957\u4ef6\u914d\u7f6e judge \u6a21\u578b\u4f9b\u5e94\u9879\u3002"
        );
        modelRouteResolver.validatePublishable(judgeModelOfferingKey);
    }

    private EvalCaseRecord requiredCase(long suiteId, long caseId) {
        EvalCaseRecord record = evalCaseRepository.findById(caseId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "\u6307\u5b9a\u9a8c\u6536\u7528\u4f8b\u4e0d\u5b58\u5728\u3002"));
        if (record.suiteId() != suiteId) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "\u6307\u5b9a\u9a8c\u6536\u7528\u4f8b\u4e0d\u5c5e\u4e8e\u5f53\u524d\u5957\u4ef6\u3002");
        }
        return record;
    }

    private EvalSuiteRecord requiredSuite(long suiteId) {
        return evalSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "\u6307\u5b9a\u9a8c\u6536\u5957\u4ef6\u4e0d\u5b58\u5728\u3002"));
    }

    private WorkflowVersionRecord requiredWorkflowVersion(long workflowVersionId) {
        return workflowVersionRepository.findById(workflowVersionId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "\u6307\u5b9a\u5de5\u4f5c\u6d41\u7248\u672c\u4e0d\u5b58\u5728\u3002"));
    }

    private void requireSuiteNotArchived(EvalSuiteRecord suite) {
        if (suite.status() == EvalSuiteStatus.ARCHIVED) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "\u5df2\u5f52\u6863\u9a8c\u6536\u5957\u4ef6\u4e0d\u53ef\u4fee\u6539\u3002");
        }
    }

    private WorkflowNodeDefinition requiredEvalNode(WorkflowVersionRecord workflowVersion, String nodeId) {
        WorkflowNodeDefinition node = workflowVersion.nodes().stream()
                .filter(candidate -> nodeId.equals(candidate.getNodeId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "\u5de5\u4f5c\u6d41\u7248\u672c\u4e2d\u4e0d\u5b58\u5728\u6307\u5b9a\u8282\u70b9\u3002"));
        if (node.getType() != WorkflowNodeType.LLM
                && node.getType() != WorkflowNodeType.REVIEW
                && node.getType() != WorkflowNodeType.SUMMARY) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "\u8282\u70b9\u9a8c\u6536\u4ec5\u652f\u6301 LLM\u3001REVIEW \u548c SUMMARY \u8282\u70b9\u3002");
        }
        return node;
    }

    private JsonNode nullToObject(JsonNode json) {
        return json == null || json.isNull() ? objectMapper.createObjectNode() : json;
    }

    private JsonNode nullToArray(JsonNode json) {
        return json == null || json.isNull() ? objectMapper.createArrayNode() : json;
    }

    private String normalizeJudgeRule(String judgeRule) {
        return judgeRule == null ? "" : judgeRule;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value;
    }

    private String newCaseNo() {
        return "case_" + CASE_NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
