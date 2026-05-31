package com.myagent.externalagent.web;

import com.myagent.common.api.PageResponse;
import com.myagent.common.domain.EnableStatus;
import com.myagent.externalagent.application.ExternalAgentApplicationService;
import com.myagent.externalagent.application.command.ChangeExternalAgentStatusCommand;
import com.myagent.externalagent.application.command.CreateExternalAgentCommand;
import com.myagent.externalagent.application.command.TestExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentSecretsCommand;
import com.myagent.externalagent.application.query.GetExternalAgentQuery;
import com.myagent.externalagent.application.query.ListExternalAgentsQuery;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.web.dto.ChangeExternalAgentStatusRequest;
import com.myagent.externalagent.web.dto.CreateExternalAgentRequest;
import com.myagent.externalagent.web.dto.ExternalAgentDetailApiResponse;
import com.myagent.externalagent.web.dto.ExternalAgentPageApiResponse;
import com.myagent.externalagent.web.dto.ExternalAgentTestApiResponse;
import com.myagent.externalagent.web.dto.TestExternalAgentRequest;
import com.myagent.externalagent.web.dto.UpdateExternalAgentRequest;
import com.myagent.externalagent.web.dto.UpdateExternalAgentSecretsRequest;
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

/**
 * 外部 Agent REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/external-agents")
@Tag(name = "ExternalAgents", description = "外部 Agent 目录接口。")
public class ExternalAgentController {

    /**
     * 外部 Agent 应用服务。
     */
    private final ExternalAgentApplicationService externalAgentApplicationService;

    /**
     * 构造外部 Agent 控制器。
     *
     * @param externalAgentApplicationService 外部 Agent 应用服务
     */
    public ExternalAgentController(ExternalAgentApplicationService externalAgentApplicationService) {
        this.externalAgentApplicationService = externalAgentApplicationService;
    }

    /**
     * 查询外部 Agent 列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 关键词
     * @param status 状态
     * @param adapterType 类型
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询外部 Agent 列表", description = "按分页、关键词、状态和类型筛选外部 Agent。", operationId = "listExternalAgents")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentPageApiResponse.class))
            )
    })
    public ExternalAgentPageApiResponse listExternalAgents(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EnableStatus status,
            @RequestParam(required = false) ExternalAgentType adapterType
    ) {
        return new ExternalAgentPageApiResponse(
                true,
                PageResponse.from(externalAgentApplicationService.listExternalAgents(
                        new ListExternalAgentsQuery(page, pageSize, keyword, status, adapterType)
                )),
                null
        );
    }

    /**
     * 查询外部 Agent 详情。
     *
     * @param adapterId 外部 Agent 主键
     * @return 详情结果
     */
    @GetMapping("/{adapterId}")
    @Operation(summary = "查询外部 Agent 详情", description = "查询外部 Agent 详情并按规则脱敏敏感 header。", operationId = "getExternalAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentDetailApiResponse.class))
            )
    })
    public ExternalAgentDetailApiResponse getExternalAgent(@PathVariable @Min(1) long adapterId) {
        return new ExternalAgentDetailApiResponse(
                true,
                externalAgentApplicationService.getExternalAgent(new GetExternalAgentQuery(adapterId)),
                null
        );
    }

    /**
     * 创建外部 Agent。
     *
     * @param request 创建请求
     * @return 详情结果
     */
    @PostMapping
    @Operation(summary = "创建外部 Agent", description = "创建 CUSTOM_CLI 或 CUSTOM_HTTP 外部 Agent。", operationId = "createExternalAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentDetailApiResponse.class))
            )
    })
    public ExternalAgentDetailApiResponse createExternalAgent(@Valid @RequestBody CreateExternalAgentRequest request) {
        return new ExternalAgentDetailApiResponse(
                true,
                externalAgentApplicationService.createExternalAgent(new CreateExternalAgentCommand(
                        request.getAdapterKey(),
                        request.getAdapterType(),
                        request.getName(),
                        request.getDescription(),
                        request.getCommandJson(),
                        request.getSecretHeaders() == null ? null : request.getSecretHeaders().stream()
                                .map(item -> new CreateExternalAgentCommand.SecretHeaderItem(item.getHeaderName(), item.getSecretValue()))
                                .toList(),
                        request.getWorkingDirectory(),
                        request.getTimeoutSeconds(),
                        request.getCaptureStdout(),
                        request.getCaptureStderr(),
                        request.getCaptureGitDiff(),
                        request.getOutputSchemaId()
                )),
                null
        );
    }

    /**
     * 更新外部 Agent。
     *
     * @param adapterId 外部 Agent 主键
     * @param request 更新请求
     * @return 更新后的详情
     */
    @PutMapping("/{adapterId}")
    @Operation(summary = "更新外部 Agent", description = "只更新非敏感字段和敏感 header 定义，不更新 secret 值。", operationId = "updateExternalAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentDetailApiResponse.class))
            )
    })
    public ExternalAgentDetailApiResponse updateExternalAgent(
            @PathVariable @Min(1) long adapterId,
            @Valid @RequestBody UpdateExternalAgentRequest request
    ) {
        return new ExternalAgentDetailApiResponse(
                true,
                externalAgentApplicationService.updateExternalAgent(new UpdateExternalAgentCommand(
                        adapterId,
                        request.getName(),
                        request.getDescription(),
                        request.getCommandJson(),
                        request.getSecretHeaders() == null ? null : request.getSecretHeaders().stream()
                                .map(item -> new UpdateExternalAgentCommand.SecretHeaderItem(item.getHeaderName(), item.getSecretValue()))
                                .toList(),
                        request.getWorkingDirectory(),
                        request.getTimeoutSeconds(),
                        request.getCaptureStdout(),
                        request.getCaptureStderr(),
                        request.getCaptureGitDiff(),
                        request.getOutputSchemaId()
                )),
                null
        );
    }

    /**
     * 单独更新敏感 secret。
     *
     * @param adapterId 外部 Agent 主键
     * @param request 更新请求
     * @return 更新后的详情
     */
    @PutMapping("/{adapterId}/secrets")
    @Operation(summary = "更新外部 Agent 敏感 secret", description = "覆盖写入、显式清空并保留未出现的旧 secret。", operationId = "updateExternalAgentSecrets")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentDetailApiResponse.class))
            )
    })
    public ExternalAgentDetailApiResponse updateExternalAgentSecrets(
            @PathVariable @Min(1) long adapterId,
            @Valid @RequestBody UpdateExternalAgentSecretsRequest request
    ) {
        return new ExternalAgentDetailApiResponse(
                true,
                externalAgentApplicationService.updateExternalAgentSecrets(new UpdateExternalAgentSecretsCommand(
                        adapterId,
                        request.getItems() == null ? null : request.getItems().stream()
                                .map(item -> new UpdateExternalAgentSecretsCommand.Item(item.getHeaderName(), item.getSecretValue()))
                                .toList(),
                        request.getClearHeaderNames()
                )),
                null
        );
    }

    /**
     * 更新外部 Agent 状态。
     *
     * @param adapterId 外部 Agent 主键
     * @param request 状态更新请求
     * @return 更新后的详情
     */
    @PutMapping("/{adapterId}/status")
    @Operation(summary = "更新外部 Agent 状态", description = "启用或停用外部 Agent。", operationId = "changeExternalAgentStatus")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentDetailApiResponse.class))
            )
    })
    public ExternalAgentDetailApiResponse changeExternalAgentStatus(
            @PathVariable @Min(1) long adapterId,
            @Valid @RequestBody ChangeExternalAgentStatusRequest request
    ) {
        externalAgentApplicationService.changeExternalAgentStatus(new ChangeExternalAgentStatusCommand(adapterId, request.getStatus()));
        return new ExternalAgentDetailApiResponse(
                true,
                externalAgentApplicationService.getExternalAgent(new GetExternalAgentQuery(adapterId)),
                null
        );
    }

    /**
     * 测试外部 Agent。
     *
     * @param adapterId 外部 Agent 主键
     * @param request 测试请求
     * @return 测试结果
     */
    @PostMapping("/{adapterId}/test")
    @Operation(summary = "测试外部 Agent", description = "发起轻量测试，不创建正式 AgentRun。", operationId = "testExternalAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ExternalAgentTestApiResponse.class))
            )
    })
    public ExternalAgentTestApiResponse testExternalAgent(
            @PathVariable @Min(1) long adapterId,
            @Valid @RequestBody TestExternalAgentRequest request
    ) {
        return new ExternalAgentTestApiResponse(
                true,
                externalAgentApplicationService.testExternalAgent(new TestExternalAgentCommand(
                        adapterId,
                        request.getPrompt(),
                        request.getInput()
                )),
                null
        );
    }
}
