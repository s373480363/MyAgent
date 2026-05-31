package com.myagent.eval.web;

import com.myagent.common.api.PageResponse;
import com.myagent.eval.application.EvalApplicationService;
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
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.domain.EvalSuiteStatus;
import com.myagent.eval.web.dto.CreateEvalCaseFromNodeRunRequest;
import com.myagent.eval.web.dto.CreateEvalCaseRequest;
import com.myagent.eval.web.dto.CreateEvalSuiteRequest;
import com.myagent.eval.web.dto.EvalCaseApiResponse;
import com.myagent.eval.web.dto.EvalCasePageApiResponse;
import com.myagent.eval.web.dto.EvalRunApiResponse;
import com.myagent.eval.web.dto.EvalRunDetailApiResponse;
import com.myagent.eval.web.dto.EvalRunHistoryPageApiResponse;
import com.myagent.eval.web.dto.EvalRunPageApiResponse;
import com.myagent.eval.web.dto.EvalRunResultPageApiResponse;
import com.myagent.eval.web.dto.EvalSuiteApiResponse;
import com.myagent.eval.web.dto.EvalSuitePageApiResponse;
import com.myagent.eval.web.dto.RunEvalSuiteRequest;
import com.myagent.eval.web.dto.UpdateEvalCaseRequest;
import com.myagent.eval.web.dto.UpdateEvalSuiteRequest;
import com.myagent.run.domain.RunStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 节点验收 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "Evals", description = "节点验收接口。")
public class EvalController {

    /**
     * 节点验收应用服务。
     */
    private final EvalApplicationService evalApplicationService;

    /**
     * 构造节点验收控制器。
     *
     * @param evalApplicationService 节点验收应用服务
     */
    public EvalController(EvalApplicationService evalApplicationService) {
        this.evalApplicationService = evalApplicationService;
    }

    /**
     * 查询验收套件列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @param nodeId 节点标识
     * @param status 套件状态
     * @param keyword 关键词
     * @return 套件列表
     */
    @GetMapping("/eval-suites")
    @Operation(summary = "查询验收套件列表", operationId = "listEvalSuites")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = EvalSuitePageApiResponse.class))
            )
    })
    public EvalSuitePageApiResponse listSuites(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) Long workflowVersionId,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) EvalSuiteStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return new EvalSuitePageApiResponse(true, PageResponse.from(evalApplicationService.listSuites(
                new ListEvalSuitesQuery(page, pageSize, agentId, workflowVersionId, nodeId, status, keyword)
        )), null);
    }

    /**
     * 创建验收套件。
     *
     * @param request 创建请求
     * @return 套件详情
     */
    @PostMapping("/eval-suites")
    @Operation(summary = "创建验收套件", operationId = "createEvalSuite")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = EvalSuiteApiResponse.class))
            )
    })
    public EvalSuiteApiResponse createSuite(@Valid @RequestBody CreateEvalSuiteRequest request) {
        return new EvalSuiteApiResponse(true, evalApplicationService.createSuite(new CreateEvalSuiteCommand(
                request.agentId(),
                request.workflowVersionId(),
                request.nodeId(),
                request.name(),
                request.goal(),
                request.passThreshold()
        )), null);
    }

    /**
     * 更新验收套件。
     *
     * @param suiteId 套件主键
     * @param request 更新请求
     * @return 套件详情
     */
    @PutMapping("/eval-suites/{suiteId}")
    @Operation(summary = "更新验收套件", operationId = "updateEvalSuite")
    public EvalSuiteApiResponse updateSuite(
            @PathVariable @Min(1) long suiteId,
            @Valid @RequestBody UpdateEvalSuiteRequest request
    ) {
        return new EvalSuiteApiResponse(true, evalApplicationService.updateSuite(
                new UpdateEvalSuiteCommand(suiteId, request.name(), request.goal(), request.passThreshold())
        ), null);
    }

    /**
     * 确认验收套件。
     *
     * @param suiteId 套件主键
     * @return 套件详情
     */
    @PutMapping("/eval-suites/{suiteId}/confirm")
    @Operation(summary = "确认验收套件", operationId = "confirmEvalSuite")
    public EvalSuiteApiResponse confirmSuite(@PathVariable @Min(1) long suiteId) {
        return new EvalSuiteApiResponse(true, evalApplicationService.confirmSuite(suiteId), null);
    }

    /**
     * 归档验收套件。
     *
     * @param suiteId 套件主键
     * @return 套件详情
     */
    @PutMapping("/eval-suites/{suiteId}/archive")
    @Operation(summary = "归档验收套件", operationId = "archiveEvalSuite")
    public EvalSuiteApiResponse archiveSuite(@PathVariable @Min(1) long suiteId) {
        return new EvalSuiteApiResponse(true, evalApplicationService.archiveSuite(suiteId), null);
    }

    /**
     * 查询验收用例列表。
     *
     * @param suiteId 套件主键
     * @param page 页码
     * @param pageSize 每页条数
     * @param confirmStatus 确认状态
     * @param critical 是否关键用例
     * @param keyword 关键词
     * @return 用例列表
     */
    @GetMapping("/eval-suites/{suiteId}/cases")
    @Operation(summary = "查询验收用例列表", operationId = "listEvalCases")
    public EvalCasePageApiResponse listCases(
            @PathVariable @Min(1) long suiteId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) EvalCaseConfirmStatus confirmStatus,
            @RequestParam(required = false) Boolean critical,
            @RequestParam(required = false) String keyword
    ) {
        return new EvalCasePageApiResponse(true, PageResponse.from(evalApplicationService.listCases(
                new ListEvalCasesQuery(suiteId, page, pageSize, confirmStatus, critical, keyword)
        )), null);
    }

    /**
     * 创建验收用例。
     *
     * @param suiteId 套件主键
     * @param request 创建请求
     * @return 用例详情
     */
    @PostMapping("/eval-suites/{suiteId}/cases")
    @Operation(summary = "创建验收用例", operationId = "createEvalCase")
    public EvalCaseApiResponse createCase(
            @PathVariable @Min(1) long suiteId,
            @Valid @RequestBody CreateEvalCaseRequest request
    ) {
        return new EvalCaseApiResponse(true, evalApplicationService.createCase(new CreateEvalCaseCommand(
                suiteId,
                request.caseNo(),
                request.title(),
                request.input(),
                request.referenceAnswer(),
                request.assertions(),
                request.scoreRule(),
                request.critical(),
                request.description()
        )), null);
    }

    /**
     * 查询验收用例详情。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    @GetMapping("/eval-suites/{suiteId}/cases/{caseId}")
    @Operation(summary = "查询验收用例详情", operationId = "getEvalCase")
    public EvalCaseApiResponse getCase(
            @PathVariable @Min(1) long suiteId,
            @PathVariable @Min(1) long caseId
    ) {
        return new EvalCaseApiResponse(true, evalApplicationService.getCase(suiteId, caseId), null);
    }

    /**
     * 更新验收用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @param request 更新请求
     * @return 用例详情
     */
    @PutMapping("/eval-suites/{suiteId}/cases/{caseId}")
    @Operation(summary = "更新验收用例", operationId = "updateEvalCase")
    public EvalCaseApiResponse updateCase(
            @PathVariable @Min(1) long suiteId,
            @PathVariable @Min(1) long caseId,
            @Valid @RequestBody UpdateEvalCaseRequest request
    ) {
        return new EvalCaseApiResponse(true, evalApplicationService.updateCase(new UpdateEvalCaseCommand(
                suiteId,
                caseId,
                request.title(),
                request.input(),
                request.referenceAnswer(),
                request.assertions(),
                request.scoreRule(),
                request.critical(),
                request.description()
        )), null);
    }

    /**
     * 确认验收用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    @PutMapping("/eval-suites/{suiteId}/cases/{caseId}/confirm")
    @Operation(summary = "确认验收用例", operationId = "confirmEvalCase")
    public EvalCaseApiResponse confirmCase(
            @PathVariable @Min(1) long suiteId,
            @PathVariable @Min(1) long caseId
    ) {
        return new EvalCaseApiResponse(true, evalApplicationService.confirmCase(suiteId, caseId), null);
    }

    /**
     * 归档验收用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    @PutMapping("/eval-suites/{suiteId}/cases/{caseId}/archive")
    @Operation(summary = "归档验收用例", operationId = "archiveEvalCase")
    public EvalCaseApiResponse archiveCase(
            @PathVariable @Min(1) long suiteId,
            @PathVariable @Min(1) long caseId
    ) {
        return new EvalCaseApiResponse(true, evalApplicationService.archiveCase(suiteId, caseId), null);
    }

    /**
     * 从 NodeRun 创建验收用例。
     *
     * @param nodeRunId NodeRun 数据库主键
     * @param request 创建请求
     * @return 用例详情
     */
    @PostMapping("/node-runs/{nodeRunId}/eval-cases")
    @Operation(summary = "从 NodeRun 创建验收用例", description = "nodeRunId 必须使用 node_run.id。", operationId = "createEvalCaseFromNodeRun")
    public EvalCaseApiResponse createCaseFromNodeRun(
            @PathVariable @Min(1) long nodeRunId,
            @Valid @RequestBody CreateEvalCaseFromNodeRunRequest request
    ) {
        return new EvalCaseApiResponse(true, evalApplicationService.createCaseFromNodeRun(
                new CreateEvalCaseFromNodeRunCommand(nodeRunId, request.suiteId(), request.title(), request.description())
        ), null);
    }

    /**
     * 运行验收套件。
     *
     * @param suiteId 套件主键
     * @param request 运行请求
     * @return 运行结果
     */
    @PostMapping("/eval-suites/{suiteId}/runs")
    @Operation(summary = "运行验收套件", operationId = "runEvalSuite")
    public EvalRunApiResponse runSuite(
            @PathVariable @Min(1) long suiteId,
            @Valid @RequestBody RunEvalSuiteRequest request
    ) {
        return new EvalRunApiResponse(true, evalApplicationService.runSuite(new RunEvalSuiteCommand(
                suiteId,
                request.caseIds(),
                request.includeUnconfirmed()
        )), null);
    }

    /**
     * 查询验收运行列表。
     *
     * @param suiteId 套件主键
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 状态
     * @param startedAtFrom 开始时间下界
     * @param startedAtTo 开始时间上界
     * @return 运行列表
     */
    @GetMapping("/eval-suites/{suiteId}/runs")
    @Operation(summary = "查询验收运行列表", operationId = "listEvalRuns")
    public EvalRunPageApiResponse listRuns(
            @PathVariable @Min(1) long suiteId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) Instant startedAtFrom,
            @RequestParam(required = false) Instant startedAtTo
    ) {
        return new EvalRunPageApiResponse(true, PageResponse.from(evalApplicationService.listRuns(
                new ListEvalRunsQuery(suiteId, page, pageSize, status, startedAtFrom, startedAtTo)
        )), null);
    }

    /**
     * 查询验收运行详情。
     *
     * @param evalRunId 对外验收运行编号
     * @return 运行详情
     */
    @GetMapping("/eval-runs/{evalRunId}")
    @Operation(summary = "查询验收运行详情", operationId = "getEvalRun")
    public EvalRunDetailApiResponse getRun(@PathVariable String evalRunId) {
        return new EvalRunDetailApiResponse(true, evalApplicationService.getRun(new GetEvalRunQuery(evalRunId)), null);
    }

    /**
     * 查询验收运行结果明细。
     *
     * @param evalRunId 对外验收运行编号
     * @param page 页码
     * @param pageSize 每页条数
     * @param passed 是否通过
     * @param critical 是否关键用例
     * @param keyword 关键词
     * @return 结果明细
     */
    @GetMapping("/eval-runs/{evalRunId}/results")
    @Operation(summary = "查询验收运行结果明细", operationId = "listEvalRunResults")
    public EvalRunResultPageApiResponse listRunResults(
            @PathVariable String evalRunId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) Boolean passed,
            @RequestParam(required = false) Boolean critical,
            @RequestParam(required = false) String keyword
    ) {
        return new EvalRunResultPageApiResponse(true, PageResponse.from(evalApplicationService.listRunResults(
                new ListEvalRunResultsQuery(evalRunId, page, pageSize, passed, critical, keyword)
        )), null);
    }

    /**
     * 查询验收历史对比。
     *
     * @param suiteId 套件主键
     * @param page 页码
     * @param pageSize 每页条数
     * @return 历史对比
     */
    @GetMapping("/eval-suites/{suiteId}/run-history")
    @Operation(summary = "查询验收历史对比", operationId = "listEvalRunHistory")
    public EvalRunHistoryPageApiResponse listRunHistory(
            @PathVariable @Min(1) long suiteId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize
    ) {
        return new EvalRunHistoryPageApiResponse(true, PageResponse.from(evalApplicationService.listRunHistory(
                new ListEvalRunHistoryQuery(suiteId, page, pageSize)
        )), null);
    }
}
