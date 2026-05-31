package com.myagent.agent.web;

import com.myagent.agent.application.AgentApplicationService;
import com.myagent.agent.application.command.ChangeAgentStatusCommand;
import com.myagent.agent.application.command.CreateAgentCommand;
import com.myagent.agent.application.command.UpdateAgentCommand;
import com.myagent.agent.application.query.GetAgentDetailQuery;
import com.myagent.agent.application.query.ListAgentsQuery;
import com.myagent.agent.web.dto.AgentDetailApiResponse;
import com.myagent.agent.web.dto.AgentPageApiResponse;
import com.myagent.agent.web.dto.ChangeAgentStatusRequest;
import com.myagent.agent.web.dto.CreateAgentRequest;
import com.myagent.agent.web.dto.UpdateAgentRequest;
import com.myagent.common.api.PageResponse;
import com.myagent.common.domain.EnableStatus;
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
 * Agent 管理 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agents", description = "Agent 管理接口。")
public class AgentController {

    /**
     * Agent 应用服务。
     */
    private final AgentApplicationService agentApplicationService;

    /**
     * 构造 Agent 控制器。
     *
     * @param agentApplicationService Agent 应用服务
     */
    public AgentController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    /**
     * 查询 Agent 列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 关键词
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询 Agent 列表", description = "按分页、状态和关键词查询 Agent。", operationId = "listAgents")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = AgentPageApiResponse.class))
            )
    })
    public AgentPageApiResponse listAgents(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EnableStatus status
    ) {
        return new AgentPageApiResponse(
                true,
                PageResponse.from(agentApplicationService.listAgents(new ListAgentsQuery(page, pageSize, keyword, status))),
                null
        );
    }

    /**
     * 创建 Agent。
     *
     * @param request 创建请求
     * @return Agent 详情
     */
    @PostMapping
    @Operation(summary = "创建 Agent", description = "创建 Agent 基础信息并初始化首个草稿版本。", operationId = "createAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = AgentDetailApiResponse.class))
            )
    })
    public AgentDetailApiResponse createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return new AgentDetailApiResponse(
                true,
                agentApplicationService.createAgent(new CreateAgentCommand(
                        request.getAgentKey(),
                        request.getName(),
                        request.getDescription(),
                        request.getSystemPrompt(),
                        request.getDefaultModel(),
                        request.getTemperature(),
                        request.getTimeoutSeconds(),
                        request.getMaxSteps()
                )),
                null
        );
    }

    /**
     * 更新 Agent。
     *
     * @param agentId Agent 主键
     * @param request 更新请求
     * @return Agent 详情
     */
    @PutMapping("/{agentId}")
    @Operation(summary = "更新 Agent", description = "更新 Agent 基础信息和默认配置。", operationId = "updateAgent")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = AgentDetailApiResponse.class))
            )
    })
    public AgentDetailApiResponse updateAgent(
            @PathVariable @Min(1) long agentId,
            @Valid @RequestBody UpdateAgentRequest request
    ) {
        return new AgentDetailApiResponse(
                true,
                agentApplicationService.updateAgent(new UpdateAgentCommand(
                        agentId,
                        request.getName(),
                        request.getDescription(),
                        request.getSystemPrompt(),
                        request.getDefaultModel(),
                        request.getTemperature(),
                        request.getTimeoutSeconds(),
                        request.getMaxSteps()
                )),
                null
        );
    }

    /**
     * 修改 Agent 状态。
     *
     * @param agentId Agent 主键
     * @param request 状态更新请求
     * @return 最新 Agent 详情
     */
    @PutMapping("/{agentId}/status")
    @Operation(summary = "修改 Agent 状态", description = "启用或停用 Agent。", operationId = "changeAgentStatus")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = AgentDetailApiResponse.class))
            )
    })
    public AgentDetailApiResponse changeAgentStatus(
            @PathVariable @Min(1) long agentId,
            @Valid @RequestBody ChangeAgentStatusRequest request
    ) {
        agentApplicationService.changeAgentStatus(new ChangeAgentStatusCommand(agentId, request.getStatus()));
        return new AgentDetailApiResponse(
                true,
                agentApplicationService.getAgentDetail(new GetAgentDetailQuery(agentId)),
                null
        );
    }

    /**
     * 查询 Agent 详情。
     *
     * @param agentId Agent 主键
     * @return Agent 详情
     */
    @GetMapping("/{agentId}")
    @Operation(summary = "查询 Agent 详情", description = "查询 Agent 基础信息、当前草稿、当前发布和历史版本入口摘要。", operationId = "getAgentDetail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = AgentDetailApiResponse.class))
            )
    })
    public AgentDetailApiResponse getAgentDetail(@PathVariable @Min(1) long agentId) {
        return new AgentDetailApiResponse(
                true,
                agentApplicationService.getAgentDetail(new GetAgentDetailQuery(agentId)),
                null
        );
    }
}
