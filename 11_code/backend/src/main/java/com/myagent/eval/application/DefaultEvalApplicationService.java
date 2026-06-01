package com.myagent.eval.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.eval.application.command.CreateEvalCaseCommand;
import com.myagent.eval.application.command.CreateEvalCaseFromNodeRunCommand;
import com.myagent.eval.application.command.CreateEvalSuiteCommand;
import com.myagent.eval.application.command.RunEvalSuiteCommand;
import com.myagent.eval.application.command.UpdateEvalCaseCommand;
import com.myagent.eval.application.command.UpdateEvalSuiteCommand;
import com.myagent.eval.application.query.GetEvalRunQuery;
import com.myagent.eval.application.query.ListEvalCasesQuery;
import com.myagent.eval.application.query.ListEvalRunHistoryQuery;
import com.myagent.eval.application.query.ListEvalRunResultsQuery;
import com.myagent.eval.application.query.ListEvalRunsQuery;
import com.myagent.eval.application.query.ListEvalSuitesQuery;
import com.myagent.eval.application.result.EvalAgentSummaryResult;
import com.myagent.eval.application.result.EvalAssertionResultItem;
import com.myagent.eval.application.result.EvalCaseResult;
import com.myagent.eval.application.result.EvalFailureSummaryResult;
import com.myagent.eval.application.result.EvalNodeSummaryResult;
import com.myagent.eval.application.result.EvalRunDetailResult;
import com.myagent.eval.application.result.EvalRunHistoryComparisonResult;
import com.myagent.eval.application.result.EvalRunHistoryItemResult;
import com.myagent.eval.application.result.EvalRunListItemResult;
import com.myagent.eval.application.result.EvalRunResult;
import com.myagent.eval.application.result.EvalRunResultItemResult;
import com.myagent.eval.application.result.EvalSuiteListItemResult;
import com.myagent.eval.application.result.EvalSuiteResult;
import com.myagent.eval.application.result.EvalSuiteSummaryResult;
import com.myagent.eval.application.result.EvalWorkflowVersionSummaryResult;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.domain.EvalSuiteStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.eval.repository.EvalCaseRepository;
import com.myagent.eval.repository.EvalCaseResultJoinedRecord;
import com.myagent.eval.repository.EvalCaseResultRecord;
import com.myagent.eval.repository.EvalCaseResultRepository;
import com.myagent.eval.repository.EvalRunRecord;
import com.myagent.eval.repository.EvalRunRepository;
import com.myagent.eval.repository.EvalSuiteRecord;
import com.myagent.eval.repository.EvalSuiteRepository;
import com.myagent.run.application.result.RunErrorResult;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.TraceEventType;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.runtime.NodeExecutionCommand;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutionRunner;
import com.myagent.runtime.NodeRunFinishRecord;
import com.myagent.runtime.NodeRunStartRecord;
import com.myagent.runtime.NodeRunStartResult;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.runtime.TraceWriter;
import com.myagent.runtime.WorkflowContext;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import com.myagent.workflow.repository.WorkflowVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 默认节点验收应用服务。
 */
@Service
public class DefaultEvalApplicationService implements EvalApplicationService {

    /**
     * 编号时间格式。
     */
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    /**
     * V1 正式支持的确定性断言类型。
     */
    private static final Set<String> SUPPORTED_ASSERTION_TYPES = Set.of(
            "JSON_PATH_EXISTS",
            "JSON_PATH_EQUALS",
            "FIELD_EQUALS",
            "JSON_PATH_CONTAINS",
            "CONTAINS",
            "JSON_PATH_NOT_CONTAINS",
            "NOT_CONTAINS",
            "JSON_PATH_REGEX",
            "REGEX_MATCH",
            "JSON_PATH_NUMBER_RANGE",
            "NUMERIC_RANGE",
            "JSON_PATH_IN",
            "ENUM",
            "SCHEMA_VALIDATION"
    );

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
     * EvalRun 仓储。
     */
    private final EvalRunRepository evalRunRepository;

    /**
     * EvalCaseResult 仓储。
     */
    private final EvalCaseResultRepository evalCaseResultRepository;

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 工作流版本仓储。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * AgentRun 仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * NodeRun 仓储。
     */
    private final NodeRunRepository nodeRunRepository;

    /**
     * Trace 写入器。
     */
    private final TraceWriter traceWriter;

    /**
     * 节点执行协调器。
     */
    private final NodeExecutionRunner nodeExecutionRunner;

    /**
     * 验收断言执行器。
     */
    private final EvalAssertionEvaluator assertionEvaluator;

    /**
     * 验收评分执行器。
     */
    private final EvalScoreEvaluator scoreEvaluator;

    /**
     * EvalRun 生命周期服务。
     */
    private final EvalRunLifecycleService evalRunLifecycleService;

    /**
     * 构造节点验收应用服务。
     *
     * @param objectMapper JSON 对象映射器
     * @param evalSuiteRepository EvalSuite 仓储
     * @param evalCaseRepository EvalCase 仓储
     * @param evalRunRepository EvalRun 仓储
     * @param evalCaseResultRepository EvalCaseResult 仓储
     * @param agentRepository Agent 仓储
     * @param workflowVersionRepository 工作流版本仓储
     * @param agentRunRepository AgentRun 仓储
     * @param nodeRunRepository NodeRun 仓储
     * @param traceWriter Trace 写入器
     * @param nodeExecutionRunner 节点执行协调器
     * @param assertionEvaluator 验收断言执行器
     * @param scoreEvaluator 验收评分执行器
     * @param evalRunLifecycleService EvalRun 生命周期服务
     */
    public DefaultEvalApplicationService(
            ObjectMapper objectMapper,
            EvalSuiteRepository evalSuiteRepository,
            EvalCaseRepository evalCaseRepository,
            EvalRunRepository evalRunRepository,
            EvalCaseResultRepository evalCaseResultRepository,
            AgentRepository agentRepository,
            WorkflowVersionRepository workflowVersionRepository,
            AgentRunRepository agentRunRepository,
            NodeRunRepository nodeRunRepository,
            TraceWriter traceWriter,
            NodeExecutionRunner nodeExecutionRunner,
            EvalAssertionEvaluator assertionEvaluator,
            EvalScoreEvaluator scoreEvaluator,
            EvalRunLifecycleService evalRunLifecycleService
    ) {
        this.objectMapper = objectMapper;
        this.evalSuiteRepository = evalSuiteRepository;
        this.evalCaseRepository = evalCaseRepository;
        this.evalRunRepository = evalRunRepository;
        this.evalCaseResultRepository = evalCaseResultRepository;
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.agentRunRepository = agentRunRepository;
        this.nodeRunRepository = nodeRunRepository;
        this.traceWriter = traceWriter;
        this.nodeExecutionRunner = nodeExecutionRunner;
        this.assertionEvaluator = assertionEvaluator;
        this.scoreEvaluator = scoreEvaluator;
        this.evalRunLifecycleService = evalRunLifecycleService;
    }

    @Override
    public PageResult<EvalSuiteListItemResult> listSuites(ListEvalSuitesQuery query) {
        return evalSuiteRepository.list(query).map(this::toSuiteListItem);
    }

    @Override
    @Transactional
    public EvalSuiteResult createSuite(CreateEvalSuiteCommand command) {
        AgentRecord agent = requiredAgent(command.agentId());
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(command.workflowVersionId());
        if (workflowVersion.agentId() != agent.id()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "验收套件绑定的工作流版本不属于指定 Agent。");
        }
        requiredEvalNode(workflowVersion, command.nodeId());
        EvalSuiteRecord record = evalSuiteRepository.insert(new EvalSuiteRecord(
                0L,
                command.agentId(),
                command.workflowVersionId(),
                command.nodeId(),
                requiredText(command.name(), "验收套件名称不能为空。"),
                command.goal() == null ? "" : command.goal(),
                normalizeThreshold(command.passThreshold()),
                EvalSuiteStatus.DRAFT,
                null,
                null
        ));
        return toSuiteResult(record);
    }

    @Override
    @Transactional
    public EvalSuiteResult updateSuite(UpdateEvalSuiteCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        requireSuiteDraft(suite);
        return toSuiteResult(evalSuiteRepository.update(
                command.suiteId(),
                requiredText(command.name(), "验收套件名称不能为空。"),
                command.goal() == null ? "" : command.goal(),
                normalizeThreshold(command.passThreshold())
        ));
    }

    @Override
    @Transactional
    public EvalSuiteResult confirmSuite(long suiteId) {
        EvalSuiteRecord suite = requiredSuite(suiteId);
        requireSuiteDraft(suite);
        List<EvalCaseRecord> formalCases = evalCaseRepository.listRunnableCases(suiteId, null);
        if (formalCases.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "确认验收套件前至少需要一个正式用例。");
        }
        formalCases.forEach(record -> validateFormalEvalCase(suite, record));
        return toSuiteResult(evalSuiteRepository.updateStatus(suiteId, EvalSuiteStatus.CONFIRMED));
    }

    @Override
    @Transactional
    public EvalSuiteResult archiveSuite(long suiteId) {
        requiredSuite(suiteId);
        return toSuiteResult(evalSuiteRepository.updateStatus(suiteId, EvalSuiteStatus.ARCHIVED));
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
        EvalCaseRecord record = requiredCase(suiteId, caseId);
        return toCaseResult(record);
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

    @Override
    public EvalRunResult runSuite(RunEvalSuiteCommand command) {
        EvalSuiteRecord suite = requiredSuite(command.suiteId());
        if (suite.status() != EvalSuiteStatus.CONFIRMED) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "只有已确认验收套件可以执行正式验收。");
        }
        if (command.includeUnconfirmed()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "V1 正式验收不允许包含未确认用例。");
        }
        AgentRecord agent = requiredAgent(suite.agentId());
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(suite.workflowVersionId());
        WorkflowNodeDefinition node = requiredEvalNode(workflowVersion, suite.nodeId());
        List<EvalCaseRecord> cases = evalCaseRepository.listRunnableCases(
                suite.id(),
                command.caseIds()
        );
        if (cases.isEmpty()) {
            throw new BizException(ErrorCode.EVAL_CASE_UNCONFIRMED, "没有可执行的验收用例。");
        }
        cases.forEach(record -> validateFormalEvalCase(record, node));
        long startedAtNanos = System.nanoTime();
        AgentRunRecord agentRun = evalRunLifecycleService.createEvalAgentRun(newRunNo("run"), agent, workflowVersion, suite, cases);
        EvalRunRecord evalRun = null;
        int passed = 0;
        int failed = 0;
        try {
            evalRun = evalRunLifecycleService.createEvalRun(newRunNo("eval"), suite, agent, workflowVersion, agentRun);
            for (EvalCaseRecord evalCase : cases) {
                EvalCaseExecution execution = executeCase(agentRun, evalRun, agent, workflowVersion, node, evalCase);
                boolean casePassed = execution.passed();
                if (casePassed) {
                    passed++;
                } else {
                    failed++;
                }
                evalRunLifecycleService.insertEvalCaseResult(new EvalCaseResultRecord(
                        0L,
                        evalRun.id(),
                        evalCase.id(),
                        execution.outputJson(),
                        execution.assertionResults(),
                        execution.scoreResult(),
                        casePassed,
                        execution.errorMessage(),
                        execution.durationMs(),
                        null
                ));
                traceWriter.writeEvent(new TraceEventRecord(
                        agentRun.id(),
                        execution.nodeRunDbId(),
                        evalRun.id(),
                        TraceEventType.EVAL_CASE_RESULT,
                        casePassed ? "验收用例通过：" + evalCase.caseNo() : "验收用例失败：" + evalCase.caseNo(),
                        objectMapper.createObjectNode()
                                .put("caseId", evalCase.id())
                                .put("caseNo", evalCase.caseNo())
                                .put("passed", casePassed)
                                .put("errorMessage", execution.errorMessage())
                ));
            }
            BigDecimal passRate = percent(passed, cases.size());
            long criticalFailed = evalCaseResultRepository.countCriticalFailures(evalRun.id());
            RunStatus status = passRate.compareTo(suite.passThreshold()) >= 0 && criticalFailed == 0
                    ? RunStatus.SUCCESS
                    : RunStatus.FAILED;
            String summary = buildSummary(status, criticalFailed, passed, failed, passRate);
            long durationMs = elapsedMillis(startedAtNanos);
            EvalRunRecord finishedEvalRun = evalRunLifecycleService.finishEvalRun(
                    evalRun.id(),
                    status,
                    cases.size(),
                    passed,
                    failed,
                    passRate,
                    summary,
                    status == RunStatus.SUCCESS ? "" : "验收断言未全部通过。",
                    durationMs
            );
            evalRunLifecycleService.finishEvalAgentRun(
                    agentRun.id(),
                    status,
                    objectMapper.createObjectNode()
                            .put("evalRunId", finishedEvalRun.runNo())
                            .put("passRate", passRate)
                            .put("summary", summary),
                    status == RunStatus.SUCCESS ? null : ErrorCode.EVAL_ASSERTION_FAILED.getCode(),
                    status == RunStatus.SUCCESS ? "" : "验收断言未全部通过。",
                    durationMs
            );
            return new EvalRunResult(
                    finishedEvalRun.runNo(),
                    agentRun.runNo(),
                    suite.id(),
                    status,
                    passRate,
                    cases.size(),
                    passed,
                    failed,
                    summary
            );
        } catch (RuntimeException exception) {
            markFailedEvalRun(agentRun, evalRun, cases, passed, failed, startedAtNanos, exception);
            throw exception;
        }
    }

    @Override
    public PageResult<EvalRunListItemResult> listRuns(ListEvalRunsQuery query) {
        requiredSuite(query.suiteId());
        return evalRunRepository.listBySuite(query).map(this::toRunListItem);
    }

    @Override
    public EvalRunDetailResult getRun(GetEvalRunQuery query) {
        EvalRunRecord run = requiredEvalRun(query.evalRunId());
        EvalSuiteRecord suite = requiredSuite(run.suiteId());
        AgentRecord agent = requiredAgent(run.agentId());
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(run.workflowVersionId());
        WorkflowNodeDefinition node = findNode(workflowVersion, run.nodeId());
        AgentRunRecord agentRun = requiredAgentRun(run.agentRunId());
        long criticalFailed = evalCaseResultRepository.countCriticalFailures(run.id());
        List<EvalFailureSummaryResult> failures = evalCaseResultRepository.listFailed(run.id()).stream()
                .map(this::toFailureSummary)
                .toList();
        return new EvalRunDetailResult(
                run.runNo(),
                agentRun.runNo(),
                new EvalSuiteSummaryResult(suite.id(), suite.name()),
                new EvalAgentSummaryResult(agent.id(), agent.agentKey(), agent.name()),
                new EvalWorkflowVersionSummaryResult(workflowVersion.id(), workflowVersion.versionNo()),
                new EvalNodeSummaryResult(run.nodeId(), node == null ? "" : node.getName(), node == null ? "" : node.getType().name()),
                run.status(),
                suite.passThreshold(),
                run.passRate(),
                run.totalCaseCount(),
                run.passedCaseCount(),
                run.failedCaseCount(),
                criticalFailed,
                run.summary(),
                run.errorMessage() == null || run.errorMessage().isBlank()
                        ? null
                        : new RunErrorResult(ErrorCode.EVAL_ASSERTION_FAILED.getCode(), run.errorMessage(), null),
                run.startedAt(),
                run.finishedAt(),
                run.durationMs(),
                historyComparison(run),
                failures
        );
    }

    @Override
    public PageResult<EvalRunResultItemResult> listRunResults(ListEvalRunResultsQuery query) {
        EvalRunRecord run = requiredEvalRun(query.evalRunId());
        return evalCaseResultRepository.listByEvalRun(run.id(), query).map(this::toRunResultItem);
    }

    @Override
    public PageResult<EvalRunHistoryItemResult> listRunHistory(ListEvalRunHistoryQuery query) {
        requiredSuite(query.suiteId());
        return evalRunRepository.listHistory(query.suiteId(), query.page(), query.pageSize())
                .map(this::toHistoryItem);
    }

    /**
     * 执行单条验收用例。
     *
     * @param agentRun AgentRun 记录
     * @param evalRun EvalRun 记录
     * @param agent Agent 主数据
     * @param workflowVersion 工作流版本
     * @param node 被验收节点
     * @param evalCase 验收用例
     * @return 用例执行结果
     */
    private EvalCaseExecution executeCase(
            AgentRunRecord agentRun,
            EvalRunRecord evalRun,
            AgentRecord agent,
            WorkflowVersionRecord workflowVersion,
            WorkflowNodeDefinition node,
            EvalCaseRecord evalCase
    ) {
        TraceWriter evalTraceWriter = new EvalTraceWriter(traceWriter, evalRun.id());
        NodeExecutionResult nodeResult = nodeExecutionRunner.execute(new NodeExecutionCommand(
                agentRun.id(),
                agentRun.runNo(),
                agent,
                workflowVersion.id(),
                workflowVersion.runtimeOptions(),
                node,
                List.of(),
                new WorkflowContext(objectMapper, evalCase.inputJson()),
                evalCase.inputJson(),
                evalTraceWriter,
                agentRun.startedAt() == null ? Instant.now() : agentRun.startedAt()
        ));
        if (nodeResult.status() != RunStatus.SUCCESS) {
            return EvalCaseExecution.failed(
                    nodeResult.nodeRunDbId(),
                    nodeResult.outputJson(),
                    objectMapper.createArrayNode(),
                    nodeResult.errorMessage(),
                    nodeResult.durationMs()
            );
        }
        EvalAssertionEvaluation assertion = assertionEvaluator.evaluate(
                nodeResult.outputJson(),
                evalCase.assertionsJson(),
                nodeResult.schemaValidationResultJson()
        );
        JsonNode scoreResult = scoreEvaluator.evaluate(new EvalScoreRequest(
                evalCase.scoreRuleJson(),
                agent,
                node,
                evalCase.inputJson(),
                evalCase.referenceAnswerJson(),
                nodeResult.outputJson(),
                assertion.assertionResults(),
                assertion.passed()
        ));
        return new EvalCaseExecution(
                nodeResult.nodeRunDbId(),
                nodeResult.outputJson(),
                assertion.assertionResults(),
                scoreResult,
                assertion.passed(),
                assertion.errorMessage(),
                nodeResult.durationMs()
        );
    }

    /**
     * 异常中止时显式提交 EvalRun 与 AgentRun 终态。
     *
     * @param agentRun AgentRun 记录
     * @param evalRun EvalRun 记录
     * @param cases 本次计划执行的用例
     * @param passed 已通过数
     * @param failed 已失败数
     * @param startedAtNanos 开始纳秒时间
     * @param exception 原始异常
     */
    private void markFailedEvalRun(
            AgentRunRecord agentRun,
            EvalRunRecord evalRun,
            List<EvalCaseRecord> cases,
            int passed,
            int failed,
            long startedAtNanos,
            RuntimeException exception
    ) {
        RunStatus status = failureStatus(exception);
        String errorCode = failureErrorCode(exception);
        String errorMessage = failureMessage(exception);
        int failedCount = Math.max(failed, cases.size() - passed);
        BigDecimal passRate = percent(passed, cases.size());
        String summary = "验收运行异常中止：" + errorMessage;
        long durationMs = elapsedMillis(startedAtNanos);
        try {
            if (evalRun != null) {
                evalRunLifecycleService.finishEvalRun(
                        evalRun.id(),
                        status,
                        cases.size(),
                        passed,
                        failedCount,
                        passRate,
                        summary,
                        errorMessage,
                        durationMs
                );
            }
            evalRunLifecycleService.finishEvalAgentRun(
                    agentRun.id(),
                    status,
                    objectMapper.createObjectNode()
                            .put("evalRunId", evalRun == null ? "" : evalRun.runNo())
                            .put("passRate", passRate)
                            .put("summary", summary),
                    errorCode,
                    errorMessage,
                    durationMs
            );
        } catch (RuntimeException finishException) {
            exception.addSuppressed(finishException);
        }
    }

    /**
     * 根据异常映射运行终态。
     *
     * @param exception 原始异常
     * @return 运行终态
     */
    private RunStatus failureStatus(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            if (bizException.getErrorCode() == ErrorCode.RUN_TIMEOUT) {
                return RunStatus.TIMEOUT;
            }
            if (bizException.getErrorCode() == ErrorCode.RUN_CANCELED) {
                return RunStatus.CANCELED;
            }
        }
        return RunStatus.FAILED;
    }

    /**
     * 根据异常解析错误码。
     *
     * @param exception 原始异常
     * @return 错误码
     */
    private String failureErrorCode(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            return bizException.getErrorCode().getCode();
        }
        return ErrorCode.INTERNAL_ERROR.getCode();
    }

    /**
     * 根据异常解析中文错误消息。
     *
     * @param exception 原始异常
     * @return 错误消息
     */
    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "验收运行异常中止。";
        }
        return message;
    }

    /**
     * 构造历史对比。
     *
     * @param run 当前运行
     * @return 历史对比
     */
    private EvalRunHistoryComparisonResult historyComparison(EvalRunRecord run) {
        return evalRunRepository.findPrevious(run.suiteId(), run.id())
                .map(previous -> new EvalRunHistoryComparisonResult(
                        previous.runNo(),
                        agentRunRepository.findById(previous.agentRunId()).map(AgentRunRecord::runNo).orElse(null),
                        previous.passRate(),
                        run.passRate().subtract(previous.passRate()),
                        run.passedCaseCount() - previous.passedCaseCount(),
                        run.failedCaseCount() - previous.failedCaseCount()
                ))
                .orElse(null);
    }

    /**
     * 转换历史项。
     *
     * @param run 验收运行
     * @return 历史项
     */
    private EvalRunHistoryItemResult toHistoryItem(EvalRunRecord run) {
        EvalRunHistoryComparisonResult comparison = historyComparison(run);
        return new EvalRunHistoryItemResult(
                run.runNo(),
                agentRunRepository.findById(run.agentRunId()).map(AgentRunRecord::runNo).orElse(null),
                run.status(),
                run.passRate(),
                run.totalCaseCount(),
                run.passedCaseCount(),
                run.failedCaseCount(),
                evalCaseResultRepository.countCriticalFailures(run.id()),
                run.startedAt(),
                run.finishedAt(),
                run.durationMs(),
                comparison == null ? BigDecimal.ZERO : comparison.passRateDelta(),
                comparison == null ? 0 : comparison.passedCaseCountDelta(),
                comparison == null ? 0 : comparison.failedCaseCountDelta()
        );
    }

    /**
     * 转换结果明细项。
     *
     * @param joined 联表记录
     * @return 明细项
     */
    private EvalRunResultItemResult toRunResultItem(EvalCaseResultJoinedRecord joined) {
        return new EvalRunResultItemResult(
                joined.evalCaseId(),
                joined.caseNo(),
                joined.title(),
                joined.confirmStatus(),
                joined.critical(),
                joined.passed(),
                joined.inputJson(),
                joined.referenceAnswerJson(),
                joined.outputJson(),
                toAssertionResultItems(joined.assertionResultJson()),
                emptyObjectAsNull(joined.scoreResultJson()),
                joined.errorMessage(),
                joined.durationMs()
        );
    }

    /**
     * 转换断言结果数组。
     *
     * @param assertionResultJson 断言结果 JSON
     * @return 断言结果列表
     */
    private List<EvalAssertionResultItem> toAssertionResultItems(JsonNode assertionResultJson) {
        if (assertionResultJson == null || !assertionResultJson.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(assertionResultJson, new TypeReference<>() {
        });
    }

    /**
     * 空对象转空值。
     *
     * @param json JSON 节点
     * @return JSON 节点或 null
     */
    private JsonNode emptyObjectAsNull(JsonNode json) {
        return json == null || json.isNull() || json.isMissingNode() || json.isEmpty() ? null : json;
    }

    /**
     * 转换失败摘要。
     *
     * @param joined 联表记录
     * @return 失败摘要
     */
    private EvalFailureSummaryResult toFailureSummary(EvalCaseResultJoinedRecord joined) {
        return new EvalFailureSummaryResult(
                joined.evalCaseId(),
                joined.caseNo(),
                joined.title(),
                joined.critical(),
                joined.errorMessage()
        );
    }

    /**
     * 转换运行列表项。
     *
     * @param record 运行记录
     * @return 列表项
     */
    private EvalRunListItemResult toRunListItem(EvalRunRecord record) {
        return new EvalRunListItemResult(
                record.runNo(),
                agentRunRepository.findById(record.agentRunId()).map(AgentRunRecord::runNo).orElse(null),
                record.suiteId(),
                record.workflowVersionId(),
                record.nodeId(),
                record.status(),
                record.passRate(),
                record.totalCaseCount(),
                record.passedCaseCount(),
                record.failedCaseCount(),
                record.startedAt(),
                record.finishedAt(),
                record.durationMs()
        );
    }

    /**
     * 转换套件列表项。
     *
     * @param record 套件记录
     * @return 列表项
     */
    private EvalSuiteListItemResult toSuiteListItem(EvalSuiteRecord record) {
        return new EvalSuiteListItemResult(
                record.id(),
                record.agentId(),
                record.workflowVersionId(),
                record.nodeId(),
                record.name(),
                record.goal(),
                record.passThreshold(),
                record.status(),
                record.updatedAt()
        );
    }

    /**
     * 转换套件详情。
     *
     * @param record 套件记录
     * @return 套件详情
     */
    private EvalSuiteResult toSuiteResult(EvalSuiteRecord record) {
        return new EvalSuiteResult(
                record.id(),
                record.agentId(),
                record.workflowVersionId(),
                record.nodeId(),
                record.name(),
                record.goal(),
                record.passThreshold(),
                record.status(),
                record.createdAt(),
                record.updatedAt()
        );
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
     * 校验正式 EvalCase 是否具备可执行验收标准。
     *
     * @param record EvalCase 记录
     */
    private void validateFormalEvalCase(EvalCaseRecord record) {
        JsonNode assertions = record.assertionsJson();
        if (assertions == null || assertions.isNull() || assertions.isMissingNode() || !assertions.isArray()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例必须配置断言数组：" + record.caseNo());
        }
        if (assertions.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例断言不能为空：" + record.caseNo());
        }
        for (int index = 0; index < assertions.size(); index++) {
            JsonNode assertion = assertions.get(index);
            if (assertion == null || !assertion.isObject()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例第 " + (index + 1) + " 条断言必须是对象：" + record.caseNo());
            }
            JsonNode typeNode = assertion.get("type");
            if (typeNode == null || !typeNode.isTextual() || typeNode.asText().isBlank()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例第 " + (index + 1) + " 条断言缺少 type：" + record.caseNo());
            }
            String type = typeNode.asText();
            if (!SUPPORTED_ASSERTION_TYPES.contains(type)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例不支持断言类型：" + type);
            }
        }
    }

    /**
     * 结合套件目标节点校验正式 EvalCase。
     *
     * @param suite EvalSuite 记录
     * @param record EvalCase 记录
     */
    private void validateFormalEvalCase(EvalSuiteRecord suite, EvalCaseRecord record) {
        validateFormalEvalCase(record);
        if (!containsSchemaValidationAssertion(record.assertionsJson())) {
            return;
        }
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(suite.workflowVersionId());
        validateFormalEvalCase(record, requiredEvalNode(workflowVersion, suite.nodeId()));
    }

    /**
     * 结合目标节点校验正式 EvalCase。
     *
     * @param record EvalCase 记录
     * @param node 目标节点
     */
    private void validateFormalEvalCase(EvalCaseRecord record, WorkflowNodeDefinition node) {
        validateFormalEvalCase(record);
        if (containsSchemaValidationAssertion(record.assertionsJson()) && node.getOutputSchemaRef() == null) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "SCHEMA_VALIDATION 断言要求目标节点配置 outputSchema：" + record.caseNo()
            );
        }
    }

    /**
     * 判断断言列表是否包含 Schema 校验断言。
     *
     * @param assertions 断言配置
     * @return 包含 Schema 校验断言时返回 true
     */
    private boolean containsSchemaValidationAssertion(JsonNode assertions) {
        if (assertions == null || !assertions.isArray()) {
            return false;
        }
        for (JsonNode assertion : assertions) {
            if ("SCHEMA_VALIDATION".equals(assertion.path("type").asText())) {
                return true;
            }
        }
        return false;
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
     * 查找 EvalRun。
     *
     * @param evalRunId 对外运行编号
     * @return EvalRun
     */
    private EvalRunRecord requiredEvalRun(String evalRunId) {
        return evalRunRepository.findByRunNo(evalRunId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定验收运行不存在。"));
    }

    /**
     * 查找 AgentRun。
     *
     * @param agentRunId AgentRun 主键
     * @return AgentRun
     */
    private AgentRunRecord requiredAgentRun(long agentRunId) {
        return agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "验收关联运行不存在。"));
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
     * 查找 Agent。
     *
     * @param agentId Agent 主键
     * @return Agent 记录
     */
    private AgentRecord requiredAgent(long agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new BizException(ErrorCode.AGENT_NOT_FOUND, "指定 Agent 不存在。"));
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
     * 要求套件为草稿态。
     *
     * @param suite 套件记录
     */
    private void requireSuiteDraft(EvalSuiteRecord suite) {
        if (suite.status() != EvalSuiteStatus.DRAFT) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "只有草稿态验收套件允许执行该操作。");
        }
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
        WorkflowNodeDefinition node = findNode(workflowVersion, nodeId);
        if (node == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "工作流版本中不存在指定节点。");
        }
        if (node.getType() != WorkflowNodeType.LLM
                && node.getType() != WorkflowNodeType.REVIEW
                && node.getType() != WorkflowNodeType.SUMMARY) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "节点验收仅支持 LLM、REVIEW 和 SUMMARY 节点。");
        }
        return node;
    }

    /**
     * 查找节点。
     *
     * @param workflowVersion 工作流版本
     * @param nodeId 节点标识
     * @return 节点定义
     */
    private WorkflowNodeDefinition findNode(WorkflowVersionRecord workflowVersion, String nodeId) {
        return workflowVersion.nodes().stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 规范化通过率阈值。
     *
     * @param passThreshold 阈值
     * @return 阈值
     */
    private BigDecimal normalizeThreshold(BigDecimal passThreshold) {
        BigDecimal value = passThreshold == null ? BigDecimal.ZERO : passThreshold;
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "通过率阈值必须在 0 到 100 之间。");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算通过率。
     *
     * @param passed 通过数
     * @param total 总数
     * @return 百分比
     */
    private BigDecimal percent(int passed, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(passed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /**
     * 构造验收摘要。
     *
     * @param status 运行状态
     * @param criticalFailed 关键失败数
     * @param passed 通过数
     * @param failed 失败数
     * @param passRate 通过率
     * @return 中文摘要
     */
    private String buildSummary(RunStatus status, long criticalFailed, int passed, int failed, BigDecimal passRate) {
        if (status == RunStatus.SUCCESS) {
            return "验收通过，通过率 " + passRate + "%，通过 " + passed + " 条，失败 " + failed + " 条。";
        }
        if (criticalFailed > 0) {
            return "存在 " + criticalFailed + " 条关键用例失败。";
        }
        return "验收未通过，通过率 " + passRate + "%，通过 " + passed + " 条，失败 " + failed + " 条。";
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
     * 生成运行编号。
     *
     * @param prefix 前缀
     * @return 编号
     */
    private String newRunNo(String prefix) {
        return prefix + "_" + NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成用例编号。
     *
     * @return 用例编号
     */
    private String newCaseNo() {
        return "case_" + NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 计算耗时毫秒。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    /**
     * Eval Trace 写入器包装，确保验收 TraceEvent 同时写入 run_id 与 eval_run_id。
     */
    private static final class EvalTraceWriter implements TraceWriter {

        /**
         * 委托写入器。
         */
        private final TraceWriter delegate;

        /**
         * EvalRun 数据库主键。
         */
        private final long evalRunDbId;

        /**
         * 构造包装器。
         *
         * @param delegate 委托写入器
         * @param evalRunDbId EvalRun 数据库主键
         */
        private EvalTraceWriter(TraceWriter delegate, long evalRunDbId) {
            this.delegate = delegate;
            this.evalRunDbId = evalRunDbId;
        }

        @Override
        public NodeRunStartResult createNodeRun(NodeRunStartRecord record) {
            return delegate.createNodeRun(record);
        }

        @Override
        public void finishNodeRun(NodeRunFinishRecord record) {
            delegate.finishNodeRun(record);
        }

        @Override
        public void writeEvent(TraceEventRecord record) {
            delegate.writeEvent(new TraceEventRecord(
                    record.agentRunDbId(),
                    record.nodeRunDbId(),
                    record.evalRunDbId() == null ? evalRunDbId : record.evalRunDbId(),
                    record.eventType(),
                    record.summary(),
                    record.detailJson()
            ));
        }
    }

    /**
     * 单条用例执行结果。
     *
     * @param nodeRunDbId NodeRun 主键
     * @param outputJson 输出 JSON
     * @param assertionResults 断言结果
     * @param scoreResult 评分结果
     * @param passed 是否通过
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     */
    private record EvalCaseExecution(
            long nodeRunDbId,
            JsonNode outputJson,
            JsonNode assertionResults,
            JsonNode scoreResult,
            boolean passed,
            String errorMessage,
            Long durationMs
    ) {

        /**
         * 构造失败结果。
         *
         * @param nodeRunDbId NodeRun 主键
         * @param outputJson 输出 JSON
         * @param assertionResults 断言结果
         * @param errorMessage 错误消息
         * @param durationMs 耗时毫秒
         * @return 执行结果
         */
        private static EvalCaseExecution failed(
                long nodeRunDbId,
                JsonNode outputJson,
                JsonNode assertionResults,
                String errorMessage,
                Long durationMs
        ) {
            return new EvalCaseExecution(nodeRunDbId, outputJson, assertionResults, null, false, errorMessage, durationMs);
        }
    }
}
