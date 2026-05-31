package com.myagent.run.web;

import com.myagent.common.api.PageResponse;
import com.myagent.run.application.RunApplicationService;
import com.myagent.run.application.command.RunDebugAgentCommand;
import com.myagent.run.application.command.RunPublishedAgentCommand;
import com.myagent.run.application.query.GetRunDetailQuery;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.web.dto.DebugRunRequest;
import com.myagent.run.web.dto.RunApiResponse;
import com.myagent.run.web.dto.RunDetailApiResponse;
import com.myagent.run.web.dto.RunPageApiResponse;
import com.myagent.run.web.dto.RunRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 运行 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "Runs", description = "运行与调试接口。")
public class RunController {

    /**
     * 运行应用服务。
     */
    private final RunApplicationService runApplicationService;

    /**
     * 构造运行控制器。
     *
     * @param runApplicationService 运行应用服务
     */
    public RunController(RunApplicationService runApplicationService) {
        this.runApplicationService = runApplicationService;
    }

    /**
     * 正式运行 Agent。
     *
     * @param agentKey Agent 业务标识
     * @param request 运行请求
     * @return 运行结果
     */
    @PostMapping("/agents/{agentKey}/runs")
    @Operation(summary = "正式运行 Agent", description = "同步运行 Agent 当前发布版本。", operationId = "runPublishedAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = RunApiResponse.class))
            )
    })
    public RunApiResponse runPublishedAgent(
            @PathVariable String agentKey,
            @Valid @RequestBody RunRequest request
    ) {
        return new RunApiResponse(
                true,
                runApplicationService.runPublishedAgent(new RunPublishedAgentCommand(agentKey, request.getInput())),
                null
        );
    }

    /**
     * 调试运行 Agent。
     *
     * @param agentId Agent 主键
     * @param request 调试请求
     * @return 运行结果
     */
    @PostMapping("/agents/{agentId}/debug-runs")
    @Operation(summary = "调试运行 Agent", description = "同步运行 Agent 当前草稿或指定版本。", operationId = "runDebugAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = RunApiResponse.class))
            )
    })
    public RunApiResponse runDebugAgent(
            @PathVariable @Min(1) long agentId,
            @Valid @RequestBody DebugRunRequest request
    ) {
        return new RunApiResponse(
                true,
                runApplicationService.runDebugAgent(new RunDebugAgentCommand(agentId, request.getWorkflowVersionId(), request.getInput())),
                null
        );
    }

    /**
     * 查询运行列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param agentId Agent 主键
     * @param agentKey Agent 业务标识
     * @param runType 运行类型
     * @param status 运行状态
     * @param keyword 关键词
     * @param startedAtFrom 开始时间下界
     * @param startedAtTo 开始时间上界
     * @return 运行列表
     */
    @GetMapping("/runs")
    @Operation(summary = "查询运行列表", description = "默认只返回 API、DEBUG、AGENT_CALL，显式 runType=EVAL 时返回验收运行。", operationId = "listRuns")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = RunPageApiResponse.class))
            )
    })
    public RunPageApiResponse listRuns(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String agentKey,
            @RequestParam(required = false) RunType runType,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Instant startedAtFrom,
            @RequestParam(required = false) Instant startedAtTo
    ) {
        return new RunPageApiResponse(
                true,
                PageResponse.from(runApplicationService.listRuns(new ListRunsQuery(
                        page,
                        pageSize,
                        agentId,
                        agentKey,
                        runType,
                        status,
                        keyword,
                        startedAtFrom,
                        startedAtTo
                ))),
                null
        );
    }

    /**
     * 查询运行详情。
     *
     * @param runId 对外运行编号
     * @return 运行详情
     */
    @GetMapping("/runs/{runId}")
    @Operation(summary = "查询运行详情", description = "返回 RunDetailDto，包含 nodeRuns 和 traceEvents 全文数组。", operationId = "getRunDetail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = RunDetailApiResponse.class))
            )
    })
    public RunDetailApiResponse getRunDetail(@PathVariable String runId) {
        return new RunDetailApiResponse(true, runApplicationService.getRunDetail(new GetRunDetailQuery(runId)), null);
    }
}
