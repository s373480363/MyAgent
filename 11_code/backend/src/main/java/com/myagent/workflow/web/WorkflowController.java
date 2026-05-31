package com.myagent.workflow.web;

import com.myagent.common.api.PageResponse;
import com.myagent.workflow.application.WorkflowApplicationService;
import com.myagent.workflow.application.command.CopyWorkflowDraftFromVersionCommand;
import com.myagent.workflow.application.command.PublishWorkflowDraftCommand;
import com.myagent.workflow.application.command.SaveWorkflowDraftCommand;
import com.myagent.workflow.application.command.ValidateWorkflowDraftCommand;
import com.myagent.workflow.application.query.GetWorkflowDraftQuery;
import com.myagent.workflow.application.query.GetWorkflowVersionQuery;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.web.dto.CopyWorkflowDraftFromVersionRequest;
import com.myagent.workflow.web.dto.PublishWorkflowDraftRequest;
import com.myagent.workflow.web.dto.SaveWorkflowDraftRequest;
import com.myagent.workflow.web.dto.WorkflowDraftApiResponse;
import com.myagent.workflow.web.dto.WorkflowPublishApiResponse;
import com.myagent.workflow.web.dto.WorkflowValidationApiResponse;
import com.myagent.workflow.web.dto.WorkflowVersionDetailApiResponse;
import com.myagent.workflow.web.dto.WorkflowVersionPageApiResponse;
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
 * 工作流草稿与版本管理 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/agents/{agentId}")
@Tag(name = "Workflows", description = "工作流草稿、发布和历史版本接口。")
public class WorkflowController {

    /**
     * 工作流应用服务。
     */
    private final WorkflowApplicationService workflowApplicationService;

    /**
     * 构造控制器。
     *
     * @param workflowApplicationService 工作流应用服务
     */
    public WorkflowController(WorkflowApplicationService workflowApplicationService) {
        this.workflowApplicationService = workflowApplicationService;
    }

    /**
     * 获取当前草稿。
     *
     * @param agentId Agent 主键
     * @return 当前草稿
     */
    @GetMapping("/workflow-draft")
    @Operation(summary = "获取当前草稿", description = "获取当前 Agent 的完整草稿版本。", operationId = "getWorkflowDraft")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowDraftApiResponse.class))
            )
    })
    public WorkflowDraftApiResponse getWorkflowDraft(@PathVariable @Min(1) long agentId) {
        return new WorkflowDraftApiResponse(
                true,
                workflowApplicationService.getWorkflowDraft(new GetWorkflowDraftQuery(agentId)),
                null
        );
    }

    /**
     * 保存当前草稿。
     *
     * @param agentId Agent 主键
     * @param request 保存请求
     * @return 新草稿
     */
    @PutMapping("/workflow-draft")
    @Operation(summary = "保存当前草稿", description = "保存当前草稿并生成新的不可变 DRAFT 版本。", operationId = "saveWorkflowDraft")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowDraftApiResponse.class))
            )
    })
    public WorkflowDraftApiResponse saveWorkflowDraft(
            @PathVariable @Min(1) long agentId,
            @Valid @RequestBody SaveWorkflowDraftRequest request
    ) {
        return new WorkflowDraftApiResponse(
                true,
                workflowApplicationService.saveWorkflowDraft(new SaveWorkflowDraftCommand(
                        agentId,
                        request.getNodes(),
                        request.getEdges(),
                        request.getRuntimeOptions()
                )),
                null
        );
    }

    /**
     * 从已有版本复制生成新草稿。
     *
     * @param agentId Agent 主键
     * @param request 复制请求
     * @return 新草稿
     */
    @PostMapping("/workflow-draft/copy-from-version")
    @Operation(summary = "从已有版本复制生成新草稿", description = "显式从历史版本或已持久化版本复制生成新的草稿版本。", operationId = "copyWorkflowDraftFromVersion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowDraftApiResponse.class))
            )
    })
    public WorkflowDraftApiResponse copyWorkflowDraftFromVersion(
            @PathVariable @Min(1) long agentId,
            @Valid @RequestBody CopyWorkflowDraftFromVersionRequest request
    ) {
        return new WorkflowDraftApiResponse(
                true,
                workflowApplicationService.copyWorkflowDraftFromVersion(new CopyWorkflowDraftFromVersionCommand(
                        agentId,
                        request.getSourceWorkflowVersionId()
                )),
                null
        );
    }

    /**
     * 校验当前草稿。
     *
     * @param agentId Agent 主键
     * @return 校验结果
     */
    @PostMapping("/workflow-draft/validate")
    @Operation(summary = "校验当前草稿", description = "按完整发布规则校验当前草稿。", operationId = "validateWorkflowDraft")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowValidationApiResponse.class))
            )
    })
    public WorkflowValidationApiResponse validateWorkflowDraft(@PathVariable @Min(1) long agentId) {
        return new WorkflowValidationApiResponse(
                true,
                workflowApplicationService.validateWorkflowDraft(new ValidateWorkflowDraftCommand(agentId)),
                null
        );
    }

    /**
     * 发布当前草稿。
     *
     * @param agentId Agent 主键
     * @param request 发布请求
     * @return 发布结果
     */
    @PostMapping("/workflow-draft/publish")
    @Operation(summary = "发布当前草稿", description = "校验通过后发布当前草稿，生成新的不可变 PUBLISHED 版本。", operationId = "publishWorkflowDraft")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowPublishApiResponse.class))
            )
    })
    public WorkflowPublishApiResponse publishWorkflowDraft(
            @PathVariable @Min(1) long agentId,
            @Valid @RequestBody(required = false) PublishWorkflowDraftRequest request
    ) {
        return new WorkflowPublishApiResponse(
                true,
                workflowApplicationService.publishWorkflowDraft(new PublishWorkflowDraftCommand(
                        agentId,
                        request == null ? null : request.getPublishMessage()
                )),
                null
        );
    }

    /**
     * 分页查询工作流版本。
     *
     * @param agentId Agent 主键
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping("/workflow-versions")
    @Operation(summary = "查询工作流版本列表", description = "分页查询当前 Agent 的草稿、发布和历史版本摘要。", operationId = "listWorkflowVersions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowVersionPageApiResponse.class))
            )
    })
    public WorkflowVersionPageApiResponse listWorkflowVersions(
            @PathVariable @Min(1) long agentId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) WorkflowVersionStatus status
    ) {
        return new WorkflowVersionPageApiResponse(
                true,
                PageResponse.from(workflowApplicationService.listWorkflowVersions(
                        new ListWorkflowVersionsQuery(agentId, page, pageSize, status)
                )),
                null
        );
    }

    /**
     * 查询工作流版本详情。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 版本详情
     */
    @GetMapping("/workflow-versions/{workflowVersionId}")
    @Operation(summary = "查询工作流版本详情", description = "查询指定工作流版本的只读快照详情。", operationId = "getWorkflowVersion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = WorkflowVersionDetailApiResponse.class))
            )
    })
    public WorkflowVersionDetailApiResponse getWorkflowVersion(
            @PathVariable @Min(1) long agentId,
            @PathVariable @Min(1) long workflowVersionId
    ) {
        return new WorkflowVersionDetailApiResponse(
                true,
                workflowApplicationService.getWorkflowVersion(new GetWorkflowVersionQuery(agentId, workflowVersionId)),
                null
        );
    }
}
