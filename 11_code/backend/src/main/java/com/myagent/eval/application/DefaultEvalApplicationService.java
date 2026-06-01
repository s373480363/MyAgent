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
import com.myagent.eval.application.command.CreateEvalSuiteCommand;
import com.myagent.eval.application.command.RunEvalSuiteCommand;
import com.myagent.eval.application.command.UpdateEvalSuiteCommand;
import com.myagent.eval.application.query.GetEvalRunQuery;
import com.myagent.eval.application.query.ListEvalRunHistoryQuery;
import com.myagent.eval.application.query.ListEvalRunResultsQuery;
import com.myagent.eval.application.query.ListEvalRunsQuery;
import com.myagent.eval.application.query.ListEvalSuitesQuery;
import com.myagent.eval.application.result.EvalAgentSummaryResult;
import com.myagent.eval.application.result.EvalAssertionResultItem;
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
import com.myagent.run.domain.RunNoGenerator;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.TraceEventType;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 默认节点验收应用服务。
 */
@Service
public class DefaultEvalApplicationService implements EvalApplicationService {

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
     * 正式用例校验服务。
     */
    private final EvalCaseFormalValidationService formalValidationService;

    /**
     * 运行编号生成器。
     */
    private final RunNoGenerator runNoGenerator;

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
     * @param traceWriter Trace 写入器
     * @param nodeExecutionRunner 节点执行协调器
     * @param assertionEvaluator 验收断言执行器
     * @param scoreEvaluator 验收评分执行器
     * @param evalRunLifecycleService EvalRun 生命周期服务
     * @param formalValidationService 正式用例校验服务
     * @param runNoGenerator 运行编号生成器
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
            TraceWriter traceWriter,
            NodeExecutionRunner nodeExecutionRunner,
            EvalAssertionEvaluator assertionEvaluator,
            EvalScoreEvaluator scoreEvaluator,
            EvalRunLifecycleService evalRunLifecycleService,
            EvalCaseFormalValidationService formalValidationService,
            RunNoGenerator runNoGenerator
    ) {
        this.objectMapper = objectMapper;
        this.evalSuiteRepository = evalSuiteRepository;
        this.evalCaseRepository = evalCaseRepository;
        this.evalRunRepository = evalRunRepository;
        this.evalCaseResultRepository = evalCaseResultRepository;
        this.agentRepository = agentRepository;
        this.workflowVersionRepository = workflowVersionRepository;
        this.agentRunRepository = agentRunRepository;
        this.traceWriter = traceWriter;
        this.nodeExecutionRunner = nodeExecutionRunner;
        this.assertionEvaluator = assertionEvaluator;
        this.scoreEvaluator = scoreEvaluator;
        this.evalRunLifecycleService = evalRunLifecycleService;
        this.formalValidationService = formalValidationService;
        this.runNoGenerator = runNoGenerator;
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
        formalCases.forEach(formalValidationService::validateFormalEvalCase);
        WorkflowVersionRecord workflowVersion = requiredWorkflowVersion(suite.workflowVersionId());
        WorkflowNodeDefinition node = requiredEvalNode(workflowVersion, suite.nodeId());
        formalCases.forEach(record -> formalValidationService.validateFormalEvalCase(record, node));
        return toSuiteResult(evalSuiteRepository.updateStatus(suiteId, EvalSuiteStatus.CONFIRMED));
    }

    @Override
    @Transactional
    public EvalSuiteResult archiveSuite(long suiteId) {
        requiredSuite(suiteId);
        return toSuiteResult(evalSuiteRepository.updateStatus(suiteId, EvalSuiteStatus.ARCHIVED));
    }

    @Override
    public EvalRunResult runSuite(RunEvalSuiteCommand command) {
        // 正式 EvalRun 只接收已确认套件和正式用例，未确认 AI 草稿不允许进入质量统计。
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
        // 创建运行记录前先完成断言合法性校验，避免无效用例污染 AgentRun/EvalRun 审计链路。
        cases.forEach(record -> formalValidationService.validateFormalEvalCase(record, node));
        long startedAtNanos = System.nanoTime();
        // AgentRun 与 EvalRun 通过生命周期服务独立提交，确保线程池内写 Trace 时外键已经可见。
        AgentRunRecord agentRun = evalRunLifecycleService.createEvalAgentRun(runNoGenerator.nextRunNo(), agent, workflowVersion, suite, cases);
        EvalRunRecord evalRun = null;
        int passed = 0;
        int failed = 0;
        try {
            evalRun = evalRunLifecycleService.createEvalRun(runNoGenerator.nextEvalRunNo(), suite, agent, workflowVersion, agentRun);
            for (EvalCaseRecord evalCase : cases) {
                // 单用例执行复用 NodeExecutionRunner，保持 Eval 与普通运行一致的 NodeRun、Trace、Schema 和超时语义。
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
            // 汇总只基于确定性断言结果；LLM 评分只进入 scoreResult，不覆盖正式通过率。
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
            // 异常路径同样显式写入 EvalRun 与 AgentRun 终态，避免运行记录长期停留在 RUNNING。
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
